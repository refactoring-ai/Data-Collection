package refactoringml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.QuarkusApplication;
import refactoringml.util.FileUtils;

public class DataCollector implements QuarkusApplication {
    private static final Logger log = Logger.getLogger(DataCollector.class);

    private static final int DEFAULT_THREAD_COUNT_REDUCE = 1;

    @ConfigProperty(name = "repositories.path")
    Optional<Path> repositoriesPath;

    @ConfigProperty(name = "storage.path", defaultValue = "storage")
    Path storagePath;

    @ConfigProperty(name = "input.csv.file", defaultValue = "input.csv")
    Path inputCsvFile;

    @ConfigProperty(name = "failed.projects.file", defaultValue = "failedProjects.txt")
    Path failedProjectsFile;

    @ConfigProperty(name = "store.full.souce.code", defaultValue = "false")
    boolean storeFullSourceCode;

    @ConfigProperty(name = "thread.count")
    Optional<Integer> threadCount;

    @Inject
    AppBean appBean;

    @Override
    public int run(String... args) throws Exception {
        // By default use amount of threads -1 so we can still use the underlying
        // machine without everything slowing down. If the machine only has 1 thread use
        // 1.
        var threadCountResolved = threadCount.orElse(Runtime.getRuntime().availableProcessors() > 1
                ? Runtime.getRuntime().availableProcessors() - DEFAULT_THREAD_COUNT_REDUCE
                : 1);
        ThreadPoolExecutor tp = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCountResolved);
        tp.setCorePoolSize(threadCountResolved);
        List<RepoProcesser> tasks;
        Path repositoriesPathResolved = repositoriesPath.orElse(Files.createTempDirectory(null));
        try (Stream<String> stream = Files.lines(inputCsvFile)) {
            tasks = stream.map(line -> processorFromLine(line, repositoriesPathResolved)).collect(Collectors.toList());
        }
        tp.invokeAll(tasks);
        tp.shutdown();
        return 0;
    }

    private RepoProcesser processorFromLine(String line, Path repositoriesPath) {
        return new RepoProcesser(line, appBean, repositoriesPath);
    }

    private class RepoProcesser implements Callable<Void> {

        private final String repoInfoLine;
        private final AppBean appBean;
        private final Path repositoriesPath;

        /**
         * @param storagePath
         * @param storeFullSourceCode
         * @param db
         * @param repoInfoLine
         */
        public RepoProcesser(String repoInfoLine, AppBean appBean, Path repositoriesPath) {
            this.repoInfoLine = repoInfoLine;
            this.appBean = appBean;
            this.repositoriesPath = repositoriesPath;
        }

        @Override
        public Void call() throws Exception {
            processRepository();
            log.infof("Done with %s", repoInfoLine);
            return null;

        }

        private void processRepository() throws IOException {

            // try
            // Files.createDirectories(failedProjectsFile);
            if (!Files.exists(failedProjectsFile)) {
                Files.createFile(failedProjectsFile);
            }

            String[] repoInfoSplit = repoInfoLine.split(",");
            String dataset = repoInfoSplit[2];
            String gitUrl = repoInfoSplit[1];
            String projectInfo = gitUrl + ", " + dataset;

            File file = failedProjectsFile.toFile();
            FileUtils.appendToFile(file, projectInfo + "\n");
            try {
                appBean.run(dataset, gitUrl, storagePath, repositoriesPath, storeFullSourceCode);
            } catch (org.eclipse.jgit.api.errors.TransportException te) {
                log.errorf(te, "Could not clone project %s", gitUrl);
                storeFailedProject(gitUrl, "Repository not available", file, te);
            } catch (Exception e) {
                log.errorf(e, "%s while processing %s", e.getClass().getCanonicalName(), gitUrl);
                storeFailedProject(gitUrl, e.getClass().getCanonicalName(), file, e);
            }
            FileUtils.removeFromFile(file, projectInfo);
        }

        private void storeFailedProject(String gitUrl, String failureReason, File failedProjectsFile,
                Exception exception) throws IOException {
            String failedProject = gitUrl + ", " + failureReason + ", " + exception.toString() + "\n";
            FileUtils.appendToFile(failedProjectsFile, failedProject);
        }
    }
}

package refactoringml;

import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FileUtils.appendToFile;
import static refactoringml.util.FileUtils.cleanOldTempDir;
import static refactoringml.util.FileUtils.removeFromFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;

import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.HibernateConfig.ConnectionPoolType;
import refactoringml.db.HibernateConfig.DatabaseEngine;
import refactoringml.util.PropertiesUtils;

public class RunImportCsv {
	private static final Logger log = LogManager.getLogger(RunImportCsv.class);

	public static void main(String[] args) throws Exception {
		RunImportCsv runner = new RunImportCsv();
		Args argv = new Args();
		JCommander.newBuilder().addObject(argv).args(args).build();
		cleanOldTempDir();
		runner.run(argv, "/tmp/repos");
	}

	private static class Args {

		@Parameter(names = { "-h", "--db-host" }, description = "Host IP or address of database")
		private String databaseHost = "127.0.0.1";

		@Parameter(names = { "-P", "--db-port" }, description = "Port of database")
		private Short databasePort;

		@Parameter(names = { "-u", "--db-user" }, description = "Database user")
		private String databaseUser = System.getProperty("user.name");

		@Parameter(names = { "-p", "--db-password" }, description = "Database password", password = true)
		private String databasePassword = "pass";

		@Parameter(names = { "-n", "--db-name" }, description = "Name of the database to use")
		private String databaseName = "refactoringdb";

		// TODO use converter into Enum directly instead of valueOf later on based on
		// string to construct enum.
		@Parameter(names = { "-d",
				"--db-engine" }, description = "The database engine being used, currently supported values are MYSQL, MARIADB or POSTGRESQL")
		private String databaseEngine = "MARIADB";

		// TODO use converter into Enum directly instead of valueOf later on based on
		// string to construct enum.
		@Parameter(names = { "-c",
				"--connection-pool" }, description = "The connection pooling library used. currently supported values are MYSQL, MARIADB or POSTGRESQL")
		private String connectionPoolType = "HIKARICP";

		@Parameter(names = { "-b", "--hbm2ddl" }, description = "The hibernate.hbm2ddl.auto in the database connection")
		private String hbm2ddlStrategy = "create";

		@Parameter(names = { "-s", "--store-source-code" }, description = "Store the source code of the metrics")
		private boolean storeSourceCode = false;

		@Parameter(names = { "-t",
				"thread-amount" }, description = "Amount of threads to run in parallel. Will default to amount of cores available")
		private int runWithCores = Runtime.getRuntime().availableProcessors();

		@Parameter(required = true, description = "Input csv file")
		private String inputCsvFile;

	}

	private void run(Args args, String repoLocation) throws InterruptedException, IOException {
		ThreadPoolExecutor tp = (ThreadPoolExecutor) Executors.newFixedThreadPool(args.runWithCores);
		tp.setCorePoolSize(args.runWithCores);
		SessionFactory sf = HibernateConfig.getSessionFactory(args.databaseHost, Optional.ofNullable(args.databasePort),
				args.databaseName, args.databaseUser, args.databasePassword, args.hbm2ddlStrategy,
				DatabaseEngine.valueOf(args.databaseEngine), ConnectionPoolType.valueOf(args.connectionPoolType));
		try {
			try (Stream<String> stream = Files.lines(Paths.get(args.inputCsvFile))) {
				List<RepoProcesser> tasks = stream.map(line -> new RepoProcesser(repoLocation, args.storeSourceCode,
						new Database(sf.openSession()), line)).collect(Collectors.toList());
				tp.invokeAll(tasks);
			}
			tp.shutdown();
		} finally {
			sf.close();
		}

	}

	private static class RepoProcesser implements Callable<Void> {

		private final String storagePath;
		private final boolean storeFullSourceCode;
		private final Database db;
		private final String repoInfoLine;

		/**
		 * @param storagePath
		 * @param storeFullSourceCode
		 * @param db
		 * @param repoInfoLine
		 */
		public RepoProcesser(String storagePath, boolean storeFullSourceCode, Database db, String repoInfoLine) {
			this.storagePath = storagePath;
			this.storeFullSourceCode = storeFullSourceCode;
			this.db = db;
			this.repoInfoLine = repoInfoLine;
		}

		@Override
		public Void call() throws Exception {
			processRepository();
			log.info("Done with {}", repoInfoLine);
			return null;

		}

		private void processRepository() throws IOException {

			var failedProjectsFile = new File(enforceUnixPaths(PropertiesUtils.getProperty("failedProjectsFile")));
			failedProjectsFile.getParentFile().mkdirs();

			String[] repoInfoSplit = repoInfoLine.split(",");
			String dataset = repoInfoSplit[2];
			String gitUrl = repoInfoSplit[1];
			String projectInfo = gitUrl + ", " + dataset;
			appendToFile(failedProjectsFile, projectInfo + "\n");
			try {
				new App(dataset, gitUrl, storagePath, db, storeFullSourceCode).run();
			} catch (org.eclipse.jgit.api.errors.TransportException te) {
				storeFailedProject(gitUrl, "Repository not available", failedProjectsFile, te);
			} catch (Exception e) {
				log.error("{} while processing {}", e.getClass().getCanonicalName(), gitUrl, e);
				storeFailedProject(gitUrl, e.getClass().getCanonicalName(), failedProjectsFile, e);
			}
			removeFromFile(failedProjectsFile, projectInfo);
		}

		private void storeFailedProject(String gitUrl, String failureReason, File failedProjectsFile,
				Exception exception) throws IOException {
			String failedProject = gitUrl + ", " + failureReason + ", " + exception.toString() + "\n";
			appendToFile(failedProjectsFile, failedProject);
		}
	}

}

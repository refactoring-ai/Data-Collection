package refactoringml;

import static refactoringml.util.JGitUtils.calculateDiffEntries;
import static refactoringml.util.JGitUtils.createDiffFormatter;
import static refactoringml.util.JGitUtils.discoverMainBranch;
import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;
import static refactoringml.util.JGitUtils.getHead;
import static refactoringml.util.JGitUtils.getJGitRenames;
import static refactoringml.util.JGitUtils.getRefactoringMinerRenames;
import static refactoringml.util.LogUtils.createErrorState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import refactoringml.db.CommitMetaData;
import refactoringml.db.Project;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import refactoringml.util.CounterUtils;
import refactoringml.util.CounterUtils.CounterResult;
import refactoringml.util.JGitUtils;
import refactoringml.util.RefactoringUtils;

@ApplicationScoped
public class AppBean {

	@ConfigProperty(name = "git.username")
	Optional<String> gitUsername;

	@ConfigProperty(name = "git.password")
	Optional<String> gitPassword;

	// TODO needed for tests --- refactor tests to define it more stable
	/**
	 * @param stableCommitThresholds the stableCommitThresholds to set
	 */
	public void setStableCommitThresholds(String stableCommitThresholds) {
		this.stableCommitThresholds = stableCommitThresholds;
	}

	@ConfigProperty(name = "stable.commit.thresholds", defaultValue = "20,50")
	String stableCommitThresholds;

	Logger log = Logger.getLogger(AppBean.class);

	public void run(String dataset, String gitUrl, Path storagePath, Path repositoriesPath, boolean storeFullSourceCode)
			throws GitAPIException, IOException {
		run(dataset, gitUrl, storagePath, repositoriesPath, null, null, storeFullSourceCode);
	}

	public Project run(String datasetName, String gitUrl, Path filesStoragePath, Path repositoriesPath,
			String firstCommitToProcess, String lastCommitToProcess, boolean storeFullSourceCode)
			throws GitAPIException, IOException {
		filesStoragePath = filesStoragePath.resolve(extractProjectNameFromGitUrl(gitUrl)); // add
		// project
		// as
		// subfolder

		// creates a temp dir to store the project
		Path clonePath = (JGitUtils.isLocal(gitUrl) ? Paths.get(gitUrl)
				: repositoriesPath.resolve(extractProjectNameFromGitUrl(gitUrl)));

		// do not run if the project is already in the database
		if (Project.projectExists(gitUrl)) {
			String message = String.format("Project %s already in the database", gitUrl);
			throw new IllegalArgumentException(message);
		}

		long startProjectTime = System.currentTimeMillis();
		// creates the directory in the storage
		if (storeFullSourceCode) {
			Files.createDirectories(filesStoragePath);
		}

		Git git = initGitRepository(clonePath, gitUrl);
		String mainBranch = discoverMainBranch(git);

		Project project = initProject(clonePath, git, gitUrl, datasetName);
		log.debug("Created project for analysis: " + project.toString());
		persistAndFlushInTransaction(project);

		// get all necessary objects to analyze the commits
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		PMDatabase pmDatabase = new PMDatabase();
		var repository = git.getRepository();
		final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, repository,
				filesStoragePath.toString(), storeFullSourceCode);
		final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, repository, pmDatabase,
				filesStoragePath);

		// get all commits in the repo, and to each commit with a refactoring, extract
		// the metrics
		try (RevWalk walk = JGitUtils.getReverseWalk(repository, mainBranch);
				var diffFormatter = JGitUtils.createDiffFormatter(repository)) {
			RevCommit currentCommit = walk.next();
			log.info("Start mining project " + gitUrl + "(clone at " + clonePath + ")");

			boolean firstCommitFound = firstCommitToProcess == null;
			int commitNumber = 1;
			// we only analyze commits that have one parent or the first commit with 0
			// parents
			for (boolean endFound = false; currentCommit != null && !endFound; currentCommit = walk.next()) {
				String commitHash = currentCommit.getId().getName();

				// only start the analysis once the firstCommitHash was found
				firstCommitFound = firstCommitFound || commitHash.equals(firstCommitToProcess);
				if (!firstCommitFound)
					continue;

				// did we find the last commit to process?
				// if so, process it and then stop
				if (commitHash.equals(lastCommitToProcess))
					endFound = true;

				// i.e., ignore merge commits
				if (currentCommit.getParentCount() > 1)
					continue;

				try {
					processCommit(currentCommit, commitNumber, miner, refactoringAnalyzer, processMetrics, git, project,
							diffFormatter);
				} catch (Exception e) {
					log.errorf(e, "Could not process commit %s on project %s", currentCommit, project);
				}
				commitNumber += 1;
			}
		}
		// set finished data
		// note that if this process crashes, finished date will be equals to null in
		// the database

		project.finishedDate = Calendar.getInstance();
		// project.exceptionsCount = exceptionsCount;
		mergeInTransaction(project);

		logProjectStatistics(startProjectTime, project, gitUrl);
		return project;
	}

	private boolean isFirst(RevCommit commit) {
		return commit.getParentCount() == 0;
	}

	// Initialize the git repository for this run, by downloading it
	// Returns the jgit repository object and the git object
	private Git initGitRepository(Path clonePath, String gitUrl) throws GitAPIException, IOException {
		try {
			return Git.open(clonePath.resolve(".git").toFile());
		} catch (RepositoryNotFoundException rnfe) {
			CloneCommand command = Git.cloneRepository().setCloneAllBranches(true).setDirectory(clonePath.toFile())
					.setURI(gitUrl);
			if (gitUsername.isPresent() && gitPassword.isPresent()) {
				command.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(gitUsername.get(), gitPassword.get()));
			}
			return command.call();
		}
	}

	// Initialize the project object for this run
	private Project initProject(Path clonePath, Git git, String gitUrl, String datasetName)
			throws GitAPIException, IOException {
		CounterResult counterResult = CounterUtils.countProductionAndTestFiles(clonePath);
		createDiffFormatter(git.getRepository());
		long projectSize = -1;
		try {
			projectSize = FileUtils.sizeOfDirectory(clonePath.toFile());
		} catch (IllegalArgumentException e) {
			log.info("For project: " + gitUrl + " the project size could not be determined.", e);
		}
		int numberOfCommits = JGitUtils.numberOfCommits(git);
		String lastCommitHash = getHead(git);
		String projectName = extractProjectNameFromGitUrl(gitUrl);
		return new Project(datasetName, gitUrl, projectName, Calendar.getInstance(), numberOfCommits,
				stableCommitThresholds, lastCommitHash, counterResult, projectSize);
	}

	private void processCommit(RevCommit currentCommit, int commitNumber, GitHistoryRefactoringMiner miner,
			RefactoringAnalyzer refactoringAnalyzer, ProcessMetricsCollector processMetrics, Git git, Project project,
			DiffFormatter diffFormatter) {
		long startCommitTime = System.currentTimeMillis();
		String commitHash = currentCommit.getId().getName();
		try {
			processCommitTransaction(currentCommit, commitNumber, miner, refactoringAnalyzer, processMetrics,
					commitHash, git, project, diffFormatter);
		} catch (RuntimeException e) {
			project.exceptionsCount++;
			log.warn("Unhandled exception when collecting commit data for commit: " + commitHash
					+ createErrorState(commitHash, project), e);
		} catch (MissingObjectException moe) {
			project.exceptionsCount++;
			log.debug("Missing commit", moe);
		} catch (IOException ioe) {
			project.exceptionsCount++;
			log.warn("Not caught IOE ocurred", ioe);
		}
		long elapsedCommitTime = System.currentTimeMillis() - startCommitTime;
		log.debug("Processing commit " + commitHash + " took " + elapsedCommitTime + " milliseconds.");
	}

	@Transactional
	@TransactionConfiguration(timeout = Integer.MAX_VALUE)
	public void processCommitTransaction(RevCommit currentCommit, int commitNumber, GitHistoryRefactoringMiner miner,
			RefactoringAnalyzer refactoringAnalyzer, ProcessMetricsCollector processMetrics, String commitHash, Git git,
			Project project, DiffFormatter diffFormatter) throws IOException {
		var handler = new RefactoringHandlerImpl(git, project);
		// stores all the ck metrics for the current commit
		List<RefactoringCommit> allRefactoringCommits = new ArrayList<>();
		// stores the commit meta data
		CommitMetaData superCommitMetaData = new CommitMetaData(currentCommit, commitNumber, project);
		List<DiffEntry> entries = calculateDiffEntries(currentCommit, diffFormatter);
		// Note that we only run it if the commit has a parent, i.e, skip the first
		// commit of the repo
		if (!isFirst(currentCommit)) {
			long startTimeRMiner = System.currentTimeMillis();
			miner.detectAtCommit(git.getRepository(), commitHash, handler, 120);
			log.debug("Refactoring miner took " + (System.currentTimeMillis() - startTimeRMiner)
					+ " milliseconds to mine the commit: " + commitHash);

			// if timeout has happened, refactoringsToProcess and commitIdToProcess will be
			// null
			if (handler.isRefactoringToProcess()) {
				// remove all not studied refactorings from the list
				handler.refactoringsToProcess = handler.refactoringsToProcess.stream()
						.filter(RefactoringUtils::isStudied).collect(Collectors.toList());
			}
			// check if refactoring miner detected a refactoring we study
			if (handler.isRefactoringToProcess() && !handler.refactoringsToProcess.isEmpty()) {
				allRefactoringCommits = refactoringAnalyzer.collectCommitData(currentCommit, superCommitMetaData,
						handler.refactoringsToProcess, entries);
			}
			// else if (handler.isRefactoringToProcess()) {
			// // timeout happened, so count it as an exception
			// log.debug("Refactoring Miner did not find any refactorings for commit: " +
			// commitHash
			// + createErrorState(commitHash, project));
			// } else {
			// // timeout happened, so count it as an exception
			// log.error("Refactoring Miner timed out for commit: " + commitHash
			// + createErrorState(commitHash, project));
			// project.exceptionsCount++;
			// }
		}

		// collect the process metrics for the current commit
		Set<ImmutablePair<String, String>> refactoringRenames = getRefactoringMinerRenames(
				handler.refactoringsToProcess);
		Set<ImmutablePair<String, String>> jGitRenames = getJGitRenames(entries);
		processMetrics.collectMetrics(currentCommit, superCommitMetaData, allRefactoringCommits, entries,
				refactoringRenames, jGitRenames, diffFormatter);
		long startTimeTransaction = System.currentTimeMillis();
		log.debug("Committing the transaction for commit " + commitHash + " took "
				+ (System.currentTimeMillis() - startTimeTransaction) + " milliseconds.");
	}

	private static final Set<RefactoringType> REFACTORINGMINER_1_TYPES = Set.of(RefactoringType.EXTRACT_OPERATION,
			RefactoringType.RENAME_CLASS, RefactoringType.MOVE_ATTRIBUTE, RefactoringType.RENAME_METHOD,
			RefactoringType.INLINE_OPERATION, RefactoringType.MOVE_OPERATION, RefactoringType.PULL_UP_OPERATION,
			RefactoringType.MOVE_CLASS, RefactoringType.MOVE_RENAME_CLASS, RefactoringType.MOVE_SOURCE_FOLDER,
			RefactoringType.PULL_UP_ATTRIBUTE, RefactoringType.PUSH_DOWN_ATTRIBUTE, RefactoringType.PUSH_DOWN_OPERATION,
			RefactoringType.EXTRACT_INTERFACE, RefactoringType.EXTRACT_SUPERCLASS, RefactoringType.MERGE_OPERATION,
			RefactoringType.EXTRACT_AND_MOVE_OPERATION, RefactoringType.CONVERT_ANONYMOUS_CLASS_TO_TYPE,
			RefactoringType.INTRODUCE_POLYMORPHISM, RefactoringType.RENAME_PACKAGE);

	private class RefactoringHandlerImpl extends RefactoringHandler {

		private String commitIdToProcess;
		private List<Refactoring> refactoringsToProcess;
		private final Git git;
		private final Project project;

		/**
		 * @param git
		 * @param project
		 */
		private RefactoringHandlerImpl(Git git, Project project) {
			this.git = git;
			this.project = project;
		}

		private boolean isRefactoringToProcess() {
			if (refactoringsToProcess == null || commitIdToProcess == null) {
				return false;
			}

			refactoringsToProcess = refactoringsToProcess.stream()
					// .filter(r -> REFACTORINGMINER_1_TYPES.contains(r.getRefactoringType()))
					.collect(Collectors.toList());
			return !refactoringsToProcess.isEmpty();
		}

		@Override
		public void handle(String commitId, List<Refactoring> refactorings) {
			commitIdToProcess = commitId;
			refactoringsToProcess = refactorings;
		}

		@Override
		public void handleException(String commitId, Exception e) {
			project.exceptionsCount++;
			log.error("RefactoringMiner could not handle commit: " + commitId + createErrorState(commitId, project), e);
			resetGitRepo();
		}

		private void resetGitRepo() {
			try {
				git.reset().setMode(ResetCommand.ResetType.HARD).call();
			} catch (GitAPIException e1) {
				log.error("Reset failed for repository: " + project.gitUrl + " after a commit couldn't be handled."
						+ createErrorState("UNK", project), e1);
			}
		}

	}

	// Log the project statistics after the run
	@Transactional
	public void logProjectStatistics(long startProjectTime, Project project, String gitUrl) {
		double elapsedTime = (System.currentTimeMillis() - startProjectTime) / 1000.0 / 60.0;
		StringBuilder statistics = new StringBuilder("Finished mining " + gitUrl + " in " + elapsedTime + " minutes");

		long stableInstancesCount = StableCommit.countForProject(project);
		long refactoringInstancesCount = RefactoringCommit.countForProject(project);
		statistics.append("\nFound ").append(refactoringInstancesCount).append(" refactoring- and ")
				.append(stableInstancesCount).append(" stable instances in the project.");
		for (int threshold : project.commitCountThresholdsInt) {
			stableInstancesCount = StableCommit.countForProjectAndThreshold(project, threshold);
			statistics.append("\n\t\tFound ").append(stableInstancesCount)
					.append(" stable instances in the project with threshold: ").append(threshold);
		}
		statistics.append("\n").append(project.toString());
		log.info(statistics);
	}

	@Transactional
	public void persistAndFlushInTransaction(Project project) {
		project.persistAndFlush();
	}

	@Transactional
	public void mergeInTransaction(Project project) {
		project.merge();
	}
}

package refactoringml;

import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.FileUtils.newDir;
import static refactoringml.util.JGitUtils.calculateDiffEntries;
import static refactoringml.util.JGitUtils.createDiffFormatter;
import static refactoringml.util.JGitUtils.discoverMainBranch;
import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;
import static refactoringml.util.JGitUtils.getHead;
import static refactoringml.util.JGitUtils.getJGitRenames;
import static refactoringml.util.JGitUtils.getRefactoringMinerRenames;
import static refactoringml.util.JGitUtils.numberOfCommits;
import static refactoringml.util.LogUtils.createErrorState;
import static refactoringml.util.PropertiesUtils.getProperty;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.hibernate.Transaction;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import refactoringml.db.CommitMetaData;
import refactoringml.db.Database;
import refactoringml.db.Project;
import refactoringml.db.RefactoringCommit;
import refactoringml.util.Counter;
import refactoringml.util.Counter.CounterResult;
import refactoringml.util.JGitUtils;
import refactoringml.util.LogUtils;
import refactoringml.util.RefactoringUtils;

public class App {
	private static final Logger log = LogManager.getLogger(App.class);
	private String currentTempDir;

	// url of the project to analyze
	private String gitUrl;
	// if source code storage is activated it is stored here
	private String filesStoragePath;
	// handles the logic with the MYSQL db
	private Database db;
	// which commit to start processing? (mostly for testing purposes)
	private String firstCommitToProcess;
	// the last commit to process on the selected branch
	private String lastCommitToProcess;
	// Do you want to save the affected source code for each commit?
	private boolean storeFullSourceCode;
	// name of the dataset
	private String datasetName;
	// number of unhandled exceptions encountered during runtime, @WARN quite
	// unreliable
	private int exceptionsCount = 0;
	// timeout in seconds for the refactoring miner
	private int refactoringMinerTimeout;
	// current commitId processed by the RefactoringMiner
	private String commitIdToProcess;
	// all by RefactoringMiner detected refactorings for the current commit
	private List<Refactoring> refactoringsToProcess;
	// the git repository is cloned to this path, to analyze it there
	private String clonePath;
	// main branch of the current repository, this one will be analyzed
	private String mainBranch;
	// Metrics about the current project
	private Project project;
	// JGit repository object for the current run
	private Repository repository;

	public App(String datasetName, String gitUrl, String filesStoragePath, Database db, boolean storeFullSourceCode) {
		this(datasetName, gitUrl, filesStoragePath, db, null, storeFullSourceCode);
	}

	public App(String datasetName, String gitUrl, String filesStoragePath, Database db, String lastCommitToProcess,
			boolean storeFullSourceCode) {
		this(datasetName, gitUrl, filesStoragePath, db, null, lastCommitToProcess, storeFullSourceCode);
	}

	public App(String datasetName, String gitUrl, String filesStoragePath, Database db, String firstCommitToProcess,
			String lastCommitToProcess, boolean storeFullSourceCode) {

		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.filesStoragePath = enforceUnixPaths(filesStoragePath + extractProjectNameFromGitUrl(gitUrl)); // add
																											// project
																											// as
																											// subfolder
		this.db = db;
		this.firstCommitToProcess = firstCommitToProcess;
		this.lastCommitToProcess = lastCommitToProcess;
		this.storeFullSourceCode = storeFullSourceCode;

		// creates a temp dir to store the project
		currentTempDir = createTmpDir();
		clonePath = (Project.isLocal(gitUrl) ? gitUrl : currentTempDir + "repo").trim();
		this.refactoringMinerTimeout = Integer.parseInt(getProperty("timeoutRefactoringMiner"));
	}

	public Project run() throws Exception {
		Transaction t = db.beginTransaction();
		// do not run if the project is already in the database
		try {
			if (db.projectExists(gitUrl)) {
				String message = String.format("Project %s already in the database", gitUrl);
				throw new IllegalArgumentException(message);
			}

			long startProjectTime = System.currentTimeMillis();
			// creates the directory in the storage
			if (storeFullSourceCode) {
				newDir(filesStoragePath);
			}

			try {
				Git git = initGitRepository();
				project = initProject(git);
				log.debug("Created project for analysis: {}", project);
				db.save(project);

				// get all necessary objects to analyze the commits
				GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
				RefactoringHandler handler = getRefactoringHandler(git);
				PMDatabase pmDatabase = new PMDatabase();
				final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repository,
						pmDatabase, filesStoragePath, storeFullSourceCode);
				final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repository,
						pmDatabase, filesStoragePath);

				// get all commits in the repo, and to each commit with a refactoring, extract
				// the metrics
				RevWalk walk = JGitUtils.getReverseWalk(repository, mainBranch);
				RevCommit currentCommit = walk.next();
				log.info("Start mining project {} (clone at {})", gitUrl, clonePath);

				boolean firstCommitFound = firstCommitToProcess == null;
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

					processCommit(currentCommit, miner, handler, refactoringAnalyzer, processMetrics);
				}
				walk.close();

				// set finished data
				// note that if this process crashes, finished date will be equals to null in
				// the database
				project.setFinishedDate(Calendar.getInstance());
				project.setExceptions(exceptionsCount);
				db.update(project);

				logProjectStatistics(startProjectTime);
				t.commit();
				return project;
			}

			finally {
				// delete the tmp dir that stores the project
				FileUtils.deleteDirectory(new File(currentTempDir));
			}
		} catch (IOException | GitAPIException e) {
			log.error(e);
			t.rollback();
			throw e;
		} finally {
			db.close();
		}
	}

	private boolean isFirst(RevCommit commit) {
		return commit.getParentCount() == 0;
	}

	// Initialize the git repository for this run, by downloading it
	// Returns the jgit repository object and the git object
	private Git initGitRepository() throws Exception {
		GitService gitService = new GitServiceImpl();
		repository = gitService.cloneIfNotExists(clonePath, gitUrl);
		final Git git = Git.open(new File(lastSlashDir(clonePath) + ".git"));
		// identifies the main branch of that repo
		mainBranch = discoverMainBranch(git);
		return git;
	}

	// Initialize the project object for this run
	private Project initProject(Git git) throws GitAPIException, IOException {
		CounterResult counterResult = Counter.countProductionAndTestFiles(clonePath);
		createDiffFormatter(repository);
		long projectSize = -1;
		try {
			projectSize = FileUtils.sizeOfDirectory(new File(clonePath));
		} catch (IllegalArgumentException e) {
			log.info("For project: {} the project size could not be determined.", gitUrl, e);
		}
		int numberOfCommits = numberOfCommits(git);
		String lastCommitHash = getHead(git);
		String projectName = extractProjectNameFromGitUrl(gitUrl);
		return new Project(datasetName, gitUrl, projectName, Calendar.getInstance(), numberOfCommits,
				getProperty("stableCommitThresholds"), lastCommitHash, counterResult, projectSize);
	}

	private void processCommit(RevCommit currentCommit, GitHistoryRefactoringMiner miner, RefactoringHandler handler,
			RefactoringAnalyzer refactoringAnalyzer, ProcessMetricsCollector processMetrics) throws SQLException {
		long startCommitTime = System.currentTimeMillis();
		String commitHash = currentCommit.getId().getName();

		try {

			refactoringsToProcess = null;
			commitIdToProcess = null;

			// stores all the ck metrics for the current commit
			List<RefactoringCommit> allRefactoringCommits = new ArrayList<>();
			// stores the commit meta data
			CommitMetaData superCommitMetaData = new CommitMetaData(currentCommit, project);
			List<DiffEntry> entries = calculateDiffEntries(currentCommit);
			// Note that we only run it if the commit has a parent, i.e, skip the first
			// commit of the repo
			if (!isFirst(currentCommit)) {
				long startTimeRMiner = System.currentTimeMillis();
				miner.detectAtCommit(repository, commitHash, handler, refactoringMinerTimeout);
				log.debug("Refactoring miner took {} milliseconds to mine the commit: {}",
						(System.currentTimeMillis() - startTimeRMiner), commitHash);

				// if timeout has happened, refactoringsToProcess and commitIdToProcess will be
				// null
				boolean thereIsRefactoringToProcess = refactoringsToProcess != null && commitIdToProcess != null;
				if (thereIsRefactoringToProcess)
					// remove all not studied refactorings from the list
					refactoringsToProcess = refactoringsToProcess.stream().filter(RefactoringUtils::isStudied)
							.collect(Collectors.toList());

				// check if refactoring miner detected a refactoring we study
				if (thereIsRefactoringToProcess && !refactoringsToProcess.isEmpty()) {
					allRefactoringCommits = refactoringAnalyzer.collectCommitData(currentCommit, superCommitMetaData,
							refactoringsToProcess, entries);
				} else if (thereIsRefactoringToProcess) {
					String errorState = LogUtils.createErrorState(commitHash, project);
					// timeout happened, so count it as an exception
					log.debug("Refactoring Miner did not find any refactorings for commit: {} {}", commitHash,
							errorState);
				} else {
					String errorState = createErrorState(commitHash, project);
					// timeout happened, so count it as an exception
					log.error("Refactoring Miner timed out for commit: {} {}", commitHash, errorState);
					exceptionsCount++;
				}
			}

			// collect the process metrics for the current commit
			Set<ImmutablePair<String, String>> refactoringRenames = getRefactoringMinerRenames(refactoringsToProcess);
			Set<ImmutablePair<String, String>> jGitRenames = getJGitRenames(entries);
			processMetrics.collectMetrics(currentCommit, superCommitMetaData, allRefactoringCommits, entries,
					refactoringRenames, jGitRenames);
			long startTimeTransaction = System.currentTimeMillis();
			log.debug("Committing the transaction for commit {} took {} milliseconds.", commitHash,
					(System.currentTimeMillis() - startTimeTransaction));
		} catch (Exception e) {
			// are not supported by mariadb
			exceptionsCount++;
			log.error("Unhandled exception when collecting commit data for commit: {} {}", commitHash,
					createErrorState(commitHash, project), e);

		}
		long elapsedCommitTime = System.currentTimeMillis() - startCommitTime;
		log.debug("Processing commit {} took {} milliseconds.", commitHash, elapsedCommitTime);
	}

	// Log the project statistics after the run
	private void logProjectStatistics(long startProjectTime) {
		double elapsedTime = (System.currentTimeMillis() - startProjectTime) / 1000.0 / 60.0;
		StringBuilder statistics = new StringBuilder("Finished mining " + gitUrl + " in " + elapsedTime + " minutes");

		long stableInstancesCount = db.findAllStableCommits(project.getId());
		long refactoringInstancesCount = db.findAllRefactoringCommits(project.getId());
		statistics.append("\nFound ").append(refactoringInstancesCount).append(" refactoring- and ")
				.append(stableInstancesCount).append(" stable instances in the project.");
		for (int level : project.getCommitCountThresholds()) {
			stableInstancesCount = db.findAllStableCommits(project.getId(), level);
			statistics.append("\n\t\tFound ").append(stableInstancesCount)
					.append(" stable instances in the project with threshold: ").append(level);
		}
		statistics.append("\n").append(project.toString());
		log.info(statistics);
	}

	private RefactoringHandler getRefactoringHandler(Git git) {
		return new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				commitIdToProcess = commitId;
				refactoringsToProcess = refactorings;
			}

			@Override
			public void handleException(String commitId, Exception e) {
				exceptionsCount++;
				log.error("RefactoringMiner could not handle commit: " + commitId + createErrorState(commitId, project),
						e);
				resetGitRepo();
			}

			private void resetGitRepo() {
				try {
					git.reset().setMode(ResetCommand.ResetType.HARD).call();
				} catch (GitAPIException e1) {
					log.error("Reset failed for repository: " + gitUrl + " after a commit couldn't be handled."
							+ createErrorState("UNK", project), e1);
				}
			}
		};
	}
}
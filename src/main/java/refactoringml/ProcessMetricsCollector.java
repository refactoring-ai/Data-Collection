package refactoringml;

import static refactoringml.util.CKUtils.cleanDollarSign;
import static refactoringml.util.CKUtils.extractClassMetrics;
import static refactoringml.util.CKUtils.extractMethodMetrics;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FileUtils.cleanTempDir;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.FileUtils.writeFile;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.LogUtils.createErrorState;
import static refactoringml.util.RefactoringUtils.calculateLinesAdded;
import static refactoringml.util.RefactoringUtils.calculateLinesDeleted;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.mauricioaniche.ck.CKMethodResult;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.logging.Logger;

import refactoringml.db.ClassMetric;
import refactoringml.db.CommitMetaData;
import refactoringml.db.FieldMetric;
import refactoringml.db.MethodMetric;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.Project;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import refactoringml.db.VariableMetric;
import refactoringml.util.CKUtils;
import refactoringml.util.LogUtils;
import refactoringml.util.RefactoringUtils;
import refactoringml.util.RefactoringUtils.Level;

public class ProcessMetricsCollector {
	private Project project;
	private Repository repository;
	private Path fileStoragePath;
	private PMDatabase pmDatabase;

	private static final Logger log = Logger.getLogger(ProcessMetricsCollector.class);

	public ProcessMetricsCollector(Project project, Repository repository, PMDatabase pmDatabase,
			Path fileStoragePath) {
		this.project = project;
		this.repository = repository;
		this.fileStoragePath = fileStoragePath;
		this.pmDatabase = pmDatabase;
	}

	// if this commit contained a refactoring, then collect its process metrics for
	// all affected class files,
	// otherwise only update the file process metrics
	public void collectMetrics(RevCommit commit, CommitMetaData superCommitMetaData,
			List<RefactoringCommit> allRefactoringCommits, List<DiffEntry> entries,
			Set<ImmutablePair<String, String>> refactoringRenames, Set<ImmutablePair<String, String>> jGitRenames,
			DiffFormatter diffFormatter, int cKTimeoutInSeconds) throws IOException {
		collectProcessMetricsOfRefactoredCommit(superCommitMetaData, allRefactoringCommits);

		processRenames(refactoringRenames, jGitRenames, superCommitMetaData);

		// we go now change by change in the commit to update the process metrics there
		// Also if a stable instance is found it is stored with the metrics in the DB
		collectProcessMetricsOfStableCommits(commit, superCommitMetaData, entries, diffFormatter, cKTimeoutInSeconds);
	}

	// Collect the ProcessMetrics of the RefactoringCommit before this commit
	// happened and update the database entry with it
	private void collectProcessMetricsOfRefactoredCommit(CommitMetaData superCommitMetaData,
			List<RefactoringCommit> allRefactoringCommits) {
		for (RefactoringCommit refactoringCommit : allRefactoringCommits) {
			String fileName = refactoringCommit.filePath;
			ProcessMetricTracker currentProcessMetricsTracker = pmDatabase.find(fileName);

			ProcessMetrics dbProcessMetrics = currentProcessMetricsTracker != null
					? new ProcessMetrics(currentProcessMetricsTracker.getCurrentProcessMetrics())
					: new ProcessMetrics(0, 0, 0, 0, 0, superCommitMetaData.project);

			refactoringCommit.processMetrics = dbProcessMetrics;
			refactoringCommit.merge();

			pmDatabase.reportRefactoring(fileName, superCommitMetaData);
		}
	}

	// update the process metrics for all renames that were missed by
	// RefactoringMiner
	private void processRenames(Set<ImmutablePair<String, String>> refactoringRenames,
			Set<ImmutablePair<String, String>> jGitRenames, CommitMetaData superCommitMetadata) {
		// get all renames detected by RefactoringMiner
		if (refactoringRenames != null) {
			for (ImmutablePair<String, String> rename : refactoringRenames) {
				// check if the class file name was changed, not only the class name
				if (!rename.left.equals(rename.right)) {
					pmDatabase.renameFile(rename.left, rename.right, superCommitMetadata);
					// hotfix for the case in which we rename a file but missed the refactoring
					if (pmDatabase.find(rename.right).getCommitCountThreshold() > 0)
						pmDatabase.reportRefactoring(rename.right, superCommitMetadata);
					log.debug("Renamed " + rename.left + " to " + rename.right + " in PMDatabase.");
				}
			}
		}

		// process the renames missed by refactoringminer
		if (jGitRenames != null) {
			// get all renames missed by RefactoringMiner
			if (refactoringRenames != null)
				jGitRenames.removeAll(refactoringRenames);
			if (!jGitRenames.isEmpty()) {
				log.debug("Refactoringminer missed these refactorings: " + jGitRenames
						+ LogUtils.createErrorState(superCommitMetadata.commitId, project));
				// update the missed renames in the PM database
				for (ImmutablePair<String, String> rename : jGitRenames) {
					log.debug("Renamed " + rename.left + " to " + rename.right + " in PMDatabase.");
					pmDatabase.renameFile(rename.left, rename.right, superCommitMetadata);
					pmDatabase.reportRefactoring(rename.right, superCommitMetadata);
				}
			}
		}
	}

	// Update the process metrics of all affected class files:
	// Reset the PMTracker for all class files, that were refactored on this commit
	// Increase the PMTracker for all class files, that were not refactored but
	// changed on this commit
	private void collectProcessMetricsOfStableCommits(RevCommit commit, CommitMetaData superCommitMetaData,
			List<DiffEntry> entries, DiffFormatter diffFormatter, int cKTimeoutInSeconds) throws IOException {
		for (DiffEntry entry : entries) {
			String fileName = enforceUnixPaths(entry.getNewPath());

			// do not collect these numbers if not a java file (save some memory)
			if (!refactoringml.util.FileUtils.isJavaFile(fileName))
				continue;

			// if the class was deleted, we remove it from our pmDatabase
			// this is a TTV as we can't correctly trace all renames and etc. But this
			// doesn't affect the overall result,
			// as this is basically exceptional when compared to thousands of commits and
			// changes.
			if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
				String oldFileName = enforceUnixPaths(entry.getOldPath());
				pmDatabase.removeFile(oldFileName);
				log.debug("Deleted " + oldFileName + " from PMDatabase.");
				continue;
			}

			// collect number of lines deleted and added in that file
			List<Edit> editList = diffFormatter.toFileHeader(entry).toEditList();
			int linesDeleted = calculateLinesDeleted(editList);
			int linesAdded = calculateLinesAdded(editList);

			// we increase the counter here. This means a class will go to the 'non
			// refactored' bucket
			// only after we see it X times (and not involved in a refactoring, otherwise,
			// counters are resetted).
			ProcessMetricTracker pmTracker = pmDatabase.reportChanges(fileName, superCommitMetaData,
					commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

			// The last commit passed the stability threshold for this class file
			if (pmTracker.calculateStability(project.commitCountThresholdsInt)) {
				outputNonRefactoredClass(pmTracker, cKTimeoutInSeconds);

				// we then reset the counter, and start again.
				// it is ok to use the same class more than once, as metrics as well as
				// its source code will/may change, and thus, they are a different instance.
				if (pmTracker.getCommitCountThreshold() == project.maxCommitThreshold) {
					log.debug("Reset pmTracker for class " + pmTracker.getFileName() + " with threshold: "
							+ pmTracker.getCommitCountThreshold() + " because it is the max threshold("
							+ project.maxCommitThreshold + ").");
					pmTracker.resetCounter(new CommitMetaData(commit, superCommitMetaData.commitNumber, project));
				}
			}
		}
	}

	// Store the refactoring instances in the DB
	private void outputNonRefactoredClass(ProcessMetricTracker pmTracker, int cKTimeoutInSeconds) throws IOException {
		String tempDir = null;
		try {
			String commitBackThen = pmTracker.getBaseCommitMetaData().commitId;
			log.debug("Class " + pmTracker.getFileName()
					+ " is an example of a not refactored instance with the stable commit: " + commitBackThen);

			// we extract the source code from back then (as that's the one that never
			// deserved a refactoring)
			String sourceCodeBackThen = readFileFromGit(repository, commitBackThen, pmTracker.getFileName());
			// create a temp dir to store the source code files and run CK there
			tempDir = createTmpDir();

			// we save it in the permanent storage...
			writeFile(fileStoragePath + pmTracker.getFileName() + "/" + "not-refactored/" + pmTracker.getFileName(),
					sourceCodeBackThen);
			// ... as well as in the temp one, so that we can calculate the CK metrics
			writeFile(tempDir + pmTracker.getFileName(), sourceCodeBackThen);
			var baseCommitData = pmTracker.getBaseCommitMetaData();
			if (baseCommitData.id == null) {
				pmTracker.getBaseCommitMetaData().persist();
			}

			CommitMetaData commitMetaData = CommitMetaData.findById(pmTracker.getBaseCommitMetaData().id);
			List<StableCommit> stableCommits = codeMetrics(commitMetaData, tempDir, pmTracker.getCommitCountThreshold(),
					cKTimeoutInSeconds);

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			if (!stableCommits.isEmpty()) {
				// don't store duplicate entries of the same process metrics
				ProcessMetrics processMetrics = new ProcessMetrics(pmTracker.getBaseProcessMetrics());
				processMetrics.persist();
				for (StableCommit stableCommit : stableCommits) {
					stableCommit.processMetrics = processMetrics;
					stableCommit.persist();
				}
			}

		} catch (Exception e) {
			log.error(e.getClass().getCanonicalName() + " while processing stable process metrics."
					+ createErrorState(pmTracker.getBaseCommitMetaData().commitId, project), e);
		} finally {
			cleanTempDir(tempDir);
		}
	}

	// TODO: Fix this, as it generates many duplicates
	private List<StableCommit> codeMetrics(CommitMetaData commitMetaData, String tempDir, int commitThreshold,
			int cKTimeoutInSeconds) throws InterruptedException {
		Preconditions.checkNotNull(commitMetaData);
		Preconditions.checkNotNull(project);

		List<StableCommit> stableCommits = new ArrayList<>();

		CKUtils.calculate(tempDir, commitMetaData.commitId, project.gitUrl, ck -> {
			String cleanedCkClassName = cleanDollarSign(ck.getClassName());
			ClassMetric classMetric = extractClassMetrics(ck, project);

			Set<CKMethodResult> methods = ck.getMethods();
			for (CKMethodResult ckMethodResult : methods) {
				MethodMetric methodMetrics = extractMethodMetrics(ckMethodResult, project);

				Set<Map.Entry<String, Integer>> variables = ckMethodResult.getVariablesUsage().entrySet();
				for (Map.Entry<String, Integer> entry : variables) {
					VariableMetric variableMetric = new VariableMetric(entry.getKey(), entry.getValue(), project);

					StableCommit stableCommitV = new StableCommit(project, commitMetaData,
							enforceUnixPaths(ck.getFile()).replace(tempDir, ""), cleanedCkClassName, classMetric,
							methodMetrics, variableMetric, null, RefactoringUtils.Level.VARIABLE.ordinal(),
							commitThreshold);

					stableCommits.add(stableCommitV);
				}

				StableCommit stableCommitM = new StableCommit(project, commitMetaData,
						enforceUnixPaths(ck.getFile()).replace(tempDir, ""), cleanedCkClassName, classMetric,
						methodMetrics, null, null, RefactoringUtils.Level.METHOD.ordinal(), commitThreshold);
				stableCommits.add(stableCommitM);
			}

			Set<String> fields = ck.getMethods().stream().flatMap(x -> x.getFieldUsage().keySet().stream())
					.collect(Collectors.toSet());
			for (String field : fields) {
				int totalAppearances = ck.getMethods().stream()
						.map(x -> x.getFieldUsage().get(field) == null ? 0 : x.getFieldUsage().get(field))
						.mapToInt(Integer::intValue).sum();

				FieldMetric fieldMetrics = new FieldMetric(field, totalAppearances, project);

				StableCommit stableCommitF = new StableCommit(project, commitMetaData,
						enforceUnixPaths(ck.getFile()).replace(tempDir, ""), cleanedCkClassName, classMetric, null,
						null, fieldMetrics, Level.ATTRIBUTE.ordinal(), commitThreshold);

				stableCommits.add(stableCommitF);
			}

			StableCommit stableCommit = new StableCommit(project, commitMetaData,
					enforceUnixPaths(ck.getFile()).replace(tempDir, ""), cleanedCkClassName, classMetric, null, null,
					null, RefactoringUtils.Level.CLASS.ordinal(), commitThreshold);
			stableCommits.add(stableCommit);
		}, cKTimeoutInSeconds);

		return stableCommits;
	}
}
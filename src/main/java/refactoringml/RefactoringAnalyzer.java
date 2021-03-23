package refactoringml;

import static refactoringml.util.CKUtils.cleanDollarSign;
import static refactoringml.util.CKUtils.extractClassMetrics;
import static refactoringml.util.CKUtils.extractMethodMetrics;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FilePathUtils.fileNameOnly;
import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.FileUtils.cleanTempDir;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.FileUtils.fileDoesNotExist;
import static refactoringml.util.FileUtils.writeFile;
import static refactoringml.util.JGitUtils.getMapWithOldAndNewFiles;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.LogUtils.createErrorState;
import static refactoringml.util.LogUtils.createRefactoringErrorState;
import static refactoringml.util.LogUtils.shortSummary;
import static refactoringml.util.RefactoringUtils.getClassAliases;
import static refactoringml.util.RefactoringUtils.getRefactoredMethod;
import static refactoringml.util.RefactoringUtils.getRefactoredVariableOrAttribute;
import static refactoringml.util.RefactoringUtils.isAnonymousClass;
import static refactoringml.util.RefactoringUtils.isAttributeLevelRefactoring;
import static refactoringml.util.RefactoringUtils.isClassRename;
import static refactoringml.util.RefactoringUtils.isMethodLevelRefactoring;
import static refactoringml.util.RefactoringUtils.isVariableLevelRefactoring;
import static refactoringml.util.RefactoringUtils.refactoredFilesAndClasses;
import static refactoringml.util.RefactoringUtils.refactoringTypeInNumber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.mauricioaniche.ck.CKMethodResult;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.logging.Logger;
import org.refactoringminer.api.Refactoring;

import refactoringml.db.ClassMetric;
import refactoringml.db.CommitMetaData;
import refactoringml.db.FieldMetric;
import refactoringml.db.MethodMetric;
import refactoringml.db.Project;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.VariableMetric;
import refactoringml.util.CKUtils;
import refactoringml.util.RefactoringUtils;

public class RefactoringAnalyzer {
	private String tempDir;
	private Project project;
	private Repository repository;
	private boolean storeFullSourceCode;
	private String fileStorageDir;

	private static final Logger log = Logger.getLogger(RefactoringAnalyzer.class);

	public RefactoringAnalyzer(Project project, Repository repository, String fileStorageDir,
			boolean storeFullSourceCode) {
		this.project = project;
		this.repository = repository;
		this.storeFullSourceCode = storeFullSourceCode;
		this.tempDir = null;
		this.fileStorageDir = lastSlashDir(fileStorageDir);
	}

	public List<RefactoringCommit> collectCommitData(RevCommit commit, CommitMetaData superCommitMetaData,
			List<Refactoring> refactoringsToProcess, List<DiffEntry> entries) {
		List<RefactoringCommit> allRefactorings = new ArrayList<>();
		boolean persistedCommitMetaData = false;

		try {
			// get the map between new path -> old path
			Map<String, String> filesMap = getMapWithOldAndNewFiles(entries);

			// get the map between class names
			Map<String, String> classAliases = getClassAliases(refactoringsToProcess);
			// Iterate over all Refactorings found for this commit
			for (Refactoring refactoring : refactoringsToProcess) {
				String refactoringSummary = refactoring.toString().trim();
				log.debug("Process Commit [" + commit.getId().getName() + "] with Refactoring: [" + refactoringSummary
						+ "]");

				// loop over all refactored classes, multiple classes can be refactored by the
				// same refactoring, e.g. Extract Interface Refactoring
				for (ImmutablePair<String, String> pair : refactoredFilesAndClasses(refactoring,
						refactoring.getInvolvedClassesBeforeRefactoring())) {
					// get the name of the file before the refactoring
					// if the one returned by RMiner exists in the map, we use the one in the map
					// instead
					String refactoredClassFile = enforceUnixPaths(pair.getLeft());
					// ignore the filename from JGit for move source dirs etc, because it is
					// unreliable (#133)
					if (!isClassRename(refactoring) && filesMap.containsKey(refactoredClassFile))
						refactoredClassFile = enforceUnixPaths(filesMap.get(refactoredClassFile));

					/**
					 * Sometimes, RMiner finds refactorings in newly introduced classes. Often, as
					 * part of larger refactorings. (See
					 * https://github.com/tsantalis/RefactoringMiner/issues/89 for a better
					 * understanding) We can't use those, as we need a file in the previous commit
					 * to collect metrics. Thus, we skip this refactoring.
					 */
					if (fileDoesNotExist(refactoredClassFile)) {
						log.info("Refactoring in a newly introduced file, which we skip: " + pair.getLeft()
								+ ", commit = " + superCommitMetaData + ", refactoring = "
								+ shortSummary(refactoringSummary));
						continue;
					}

					/**
					 * Now, we get the name of the class that was refactored. However, we skip
					 * refactorings in anonymous classes. For us, it's pretty hard to get the code
					 * metrics for those (as CK 0.6.0 doesn't return good names for anonymous
					 * classes). (If the heuristic fail, it's not a problem, as later we won't be
					 * able to find code metrics for it; so we will never store "bad data")
					 */
					String refactoredClassNameFromRMiner = pair.getRight();
					if (isAnonymousClass(refactoredClassNameFromRMiner)) {
						log.info("Refactoring in an anonymous class, which we skip: " + refactoredClassNameFromRMiner
								+ ", commit = " + superCommitMetaData + ", refactoring = "
								+ shortSummary(refactoringSummary));
						continue;
					}
					if (!persistedCommitMetaData) {
						persistedCommitMetaData = true;
						superCommitMetaData.persist();
					}
					ImmutablePair<String, String> refactoredClassName = new ImmutablePair<>(
							refactoredClassNameFromRMiner, classAliases.get(refactoredClassNameFromRMiner));

					// build the full RefactoringCommit object
					RefactoringCommit refactoringCommit = buildRefactoringCommitObject(superCommitMetaData, refactoring,
							refactoringSummary, refactoredClassName, refactoredClassFile);

					if (refactoringCommit != null) {
						// mark it for the process metrics collection
						allRefactorings.add(refactoringCommit);

						if (storeFullSourceCode)
							storeSourceCode(refactoringCommit.id, refactoring, commit);
					} else {
						log.debug("RefactoringCommit instance was not created for the class: " + refactoredClassName
								+ " and the refactoring type: " + refactoring.getName() + " on commit "
								+ commit.getName());
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to collect commit data for a refactored commit."
					+ createErrorState(superCommitMetaData.commitId, project), e);
		}

		return allRefactorings;
	}

	protected RefactoringCommit buildRefactoringCommitObject(CommitMetaData superCommitMetaData,
			Refactoring refactoring, String refactoringSummary, ImmutablePair<String, String> refactoredClassNames,
			String fileName) throws InterruptedException {
		String parentCommitId = superCommitMetaData.parentCommitId;

		try {
			/**
			 * Now, we get the contents of the file in the previous version, which we use to
			 * extract the features.
			 */
			String sourceCodeInPreviousVersion = readFileFromGit(repository, parentCommitId, fileName);
			tempDir = createTmpDir();
			writeFile(tempDir + "/" + fileName, sourceCodeInPreviousVersion);

			RefactoringCommit refactoringCommit = calculateCkMetrics(refactoredClassNames, superCommitMetaData,
					refactoring, refactoringSummary);
			cleanTempDir(tempDir);

			return refactoringCommit;
		} catch (IOException e) {
			/**
			 * We could not open the file in the previous commit. This should not happen.
			 */
			log.error(
					"Could not find (previous) version of " + fileName + " in parent commit " + parentCommitId
							+ createRefactoringErrorState(superCommitMetaData.commitId, project, refactoringSummary),
					e);

			return null;
		}
	}

	private void storeSourceCode(long id, Refactoring refactoring, RevCommit currentCommit) throws IOException {

		RevCommit commitParent = currentCommit.getParent(0);

		// for the before refactoring, we get its source code in the previous commit
		for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesBeforeRefactoring()) {
			String fileName = pair.getLeft();

			try {
				String sourceCode = readFileFromGit(repository, commitParent, fileName);
				writeFile(fileStorageDir + id + "/before/" + fileNameOnly(fileName), sourceCode);
			} catch (Exception e) {
				log.error("Could not write raw source code for file before refactoring, id=" + id + ", file name="
						+ fileName
						+ createRefactoringErrorState(currentCommit.getName(), project, refactoring.toString().trim()),
						e);
			}
		}

		// for the after refactoring, we get its source code in the current commit
		for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesAfterRefactoring()) {
			String fileName = pair.getLeft();

			try {
				String sourceCode = readFileFromGit(repository, currentCommit, fileName);
				writeFile(fileStorageDir + id + "/after/" + fileNameOnly(fileName), sourceCode);
			} catch (Exception e) {
				log.error("Could not write raw source code for file after refactoring, id=" + id + ", file name="
						+ fileName
						+ createRefactoringErrorState(currentCommit.getName(), project, refactoring.toString().trim()),
						e);
			}
		}
	}

	private RefactoringCommit calculateCkMetrics(ImmutablePair<String, String> refactoredClasses,
			CommitMetaData commitMetaData, Refactoring refactoring, String refactoringSummary)
			throws InterruptedException {
		final List<RefactoringCommit> refactorings = new ArrayList<>();
		CKUtils.calculate(tempDir, commitMetaData.commitId, project.gitUrl, ck -> {
			String cleanedCkClassName = cleanDollarSign(ck.getClassName());

			// Ignore all subclass callbacks from CK, that are not relevant in this case
			if (!cleanedCkClassName.equals(refactoredClasses.getLeft())
					&& !cleanedCkClassName.equals(refactoredClasses.getRight())) {
				return;
			}
			// collect the class level metrics
			ClassMetric classMetric = extractClassMetrics(ck, project);
			MethodMetric methodMetrics = null;
			VariableMetric variableMetrics = null;

			// if it's a method or a variable-level refactoring, collect the data
			if (isMethodLevelRefactoring(refactoring) || isVariableLevelRefactoring(refactoring)) {
				String fullRefactoredMethod = CKUtils
						.simplifyFullMethodName(RefactoringUtils.fullMethodName(getRefactoredMethod(refactoring)));

				Optional<CKMethodResult> ckMethod = ck.getMethods().stream().filter(
						x -> CKUtils.simplifyFullMethodName(x.getMethodName()).equalsIgnoreCase(fullRefactoredMethod))
						.findFirst();

				if (!ckMethod.isPresent()) {
					// for some reason we did not find the method, let's remove it from the
					// refactorings.
					String methods = ck.getMethods().stream()
							.map(x -> CKUtils.simplifyFullMethodName(x.getMethodName()))
							.reduce("", (a, b) -> a + ", " + b);
					log.error("CK did not find the refactored method: " + fullRefactoredMethod
							+ " for the refactoring type: " + refactoring.getName() + " on class "
							+ refactoredClasses.getLeft() + "/" + refactoredClasses.getRight()
							+ "\nAll methods found by CK: " + methods
							+ createRefactoringErrorState(commitMetaData.commitId, project, refactoringSummary));
					return;
				} else {
					CKMethodResult ckMethodResult = ckMethod.get();
					methodMetrics = extractMethodMetrics(ckMethodResult, project);

					if (isVariableLevelRefactoring(refactoring)) {
						String refactoredVariable = getRefactoredVariableOrAttribute(refactoring);

						Integer appearances = ckMethodResult.getVariablesUsage().get(refactoredVariable);
						if (appearances == null) {
							// if we couldn't find the variable, for any reason, give it a -1, so we can
							// filter it
							// out later
							appearances = -1;
						}
						variableMetrics = new VariableMetric(refactoredVariable, appearances, project);
					}
				}
			}

			// finally, if it's a field refactoring, we then only have class + field
			FieldMetric fieldMetrics = null;
			if (isAttributeLevelRefactoring(refactoring)) {
				String refactoredField = getRefactoredVariableOrAttribute(refactoring);

				int totalAppearances = ck.getMethods().stream()
						.map(x -> x.getFieldUsage().get(refactoredField) == null ? 0
								: x.getFieldUsage().get(refactoredField))
						.mapToInt(Integer::intValue).sum();

				fieldMetrics = new FieldMetric(refactoredField, totalAppearances, project);
			}

			// assemble the final object
			RefactoringCommit refactoringCommit = new RefactoringCommit(project, commitMetaData,
					enforceUnixPaths(ck.getFile()).replace(tempDir, ""), cleanedCkClassName,
					refactoring.getRefactoringType().getDisplayName(), refactoringTypeInNumber(refactoring),
					refactoringSummary, classMetric, methodMetrics, variableMetrics, fieldMetrics);
			refactorings.add(refactoringCommit);
		});

		/**
		 * It is possible that we did not find the class among the results of CK.
		 * Possible causes are:
		 *
		 * 1) Anonymous classes. It can be really hard to match anonymous classes from
		 * CK with the ones from RMiner. See this issue in CK:
		 * https://github.com/mauricioaniche/ck/issues/54.
		 */
		if (refactorings.isEmpty()) {
			log.error("CK did not find class " + refactoredClasses.getLeft() + "/" + refactoredClasses.getRight()
					+ createRefactoringErrorState(commitMetaData.commitId, project, refactoringSummary));
		} else {
			for (RefactoringCommit refactoringCommit : refactorings) {
				refactoringCommit.persist();
			}
		}

		return refactorings.isEmpty() ? null : refactorings.get(0);
	}
}
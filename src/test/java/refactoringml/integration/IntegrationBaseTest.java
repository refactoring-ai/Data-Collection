package refactoringml.integration;

import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.refactoringminer.util.GitServiceImpl;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.panache.common.Parameters;
import refactoringml.AppBean;
import refactoringml.db.CommitMetaData;
import refactoringml.db.Instance;
import refactoringml.db.Project;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;

public abstract class IntegrationBaseTest {

	protected Path outputDir;
	protected String tmpDir;
	protected Project project;

	private List<RefactoringCommit> refactoringCommits;
	private List<StableCommit> stableCommits;

	@Inject
	EntityManager session;

	@Inject
	AppBean appBean;

	protected final boolean drop() {
		return true;
	}

	protected boolean storeSourceCode() {
		return false;
	}

	protected String getLastCommit() {
		return null;
	}

	protected String getFirstCommit() {
		return null;
	}

	protected abstract String getRepo();

	protected String getStableCommitThreshold() {
		return "50";
	};

	/*
	 * Test Behavior
	 */
	@BeforeAll
	protected void runApp() throws Exception {
		outputDir = Files.createTempDirectory(null);
		tmpDir = createTmpDir();

		String projectName = extractProjectNameFromGitUrl(getRepo());
		Path repoLocalDir = Files.createTempDirectory(null).resolve(projectName);
		boolean projectAlreadyCloned = Files.exists(repoLocalDir);
		if (!projectAlreadyCloned)
			new GitServiceImpl().cloneIfNotExists(repoLocalDir.toString(), getRepo());

		deleteProject(projectName);

		appBean.setStableCommitThresholds(getStableCommitThreshold());
		// set the stableCommitThreshold in the PMDatabase to test various configs
		project = appBean.run("integration-test-dataset", repoLocalDir.toString(), outputDir,
				Files.createTempDirectory(null), getFirstCommit(), getLastCommit(), storeSourceCode());
	}

	@AfterAll
	protected void afterApp() throws IOException {
		FileUtils.deleteDirectory(new File(tmpDir));
		FileUtils.deleteDirectory(outputDir.toFile());
	}

	@Transactional
	private List<Long> getIds(String metricsName, List<Long> projectIds) {

		List<Long> ids = (List<Long>) session.createQuery(
				String.format("SELECT r.%s.id FROM RefactoringCommit r WHERE r.project.id IN :projectIds", metricsName))
				.setParameter("projectIds", projectIds).getResultList();
		ids.addAll(
				(List<Long>) session
						.createQuery(String.format(
								"SELECT s.%s.id FROM StableCommit s WHERE s.project.id IN :projectIds", metricsName))
						.setParameter("projectIds", projectIds).getResultList());
		return ids;
	}

	@Transactional
	private void deleteMetrics(String tableName, List<Long> ids) {
		if (!ids.isEmpty()) {
			session.createQuery(String.format("DELETE FROM %s WHERE id IN :ids", tableName)).setParameter("ids", ids)
					.executeUpdate();
		}
	}

	@Transactional
	public void deleteProject(String repository) {
		List<Project> projects = Project.list("projectName", repository);

		// List<Project> projects = (List<Project>) session.createQuery("FROM Project
		// WHERE projectName = :projectName")
		// .setParameter("projectName", repository).list();
		List<Long> projectIds = projects.stream().map(project -> project.id).collect(Collectors.toList());

		if (!projectIds.isEmpty()) {
			List<Long> metaData = getIds("commitMetaData", projectIds);
			List<Long> classMetrics = getIds("classMetrics", projectIds);
			List<Long> methodMetrics = getIds("methodMetrics", projectIds);
			List<Long> variableMetrics = getIds("variableMetrics", projectIds);
			List<Long> fieldMetrics = getIds("fieldMetrics", projectIds);
			List<Long> processMetrics = getIds("processMetrics", projectIds);

			Panache.executeUpdate("DELETE FROM RefactoringCommit WHERE project.id IN :projectIds",
					Parameters.with("projectIds", projectIds));
			Panache.executeUpdate("DELETE FROM StableCommit WHERE project.id IN :projectIds",
					Parameters.with("projectIds", projectIds));

			deleteMetrics("ClassMetric", classMetrics);
			deleteMetrics("MethodMetric", methodMetrics);
			deleteMetrics("CommitMetaData", metaData);
			deleteMetrics("VariableMetric", variableMetrics);
			deleteMetrics("FieldMetric", fieldMetrics);
			deleteMetrics("ProcessMetrics", processMetrics);
			projects.stream().forEach(session::remove);

		}

		// System.out.println("Could not delete the project before starting the test");

	}

	// Get all RefactoringCommits from the DB as a List, use this instead of a
	// custom query
	@Transactional
	protected List<RefactoringCommit> getRefactoringCommits() {

		if (refactoringCommits != null)
			return refactoringCommits;

		refactoringCommits = session
				.createQuery("From RefactoringCommit where project = :project AND isValid = TRUE order by id asc")
				.setParameter("project", project).getResultList();
		return refactoringCommits;
	}

	// Get all StableCommits from the DB as a List, use this instead of a custom
	// query
	@Transactional
	protected List<StableCommit> getStableCommits() {

		// if (stableCommits != null)
		// return stableCommits;

		stableCommits = session.createQuery("From StableCommit where project = :project order by id asc")
				.setParameter("project", project).getResultList();
		return stableCommits;
	}

	// Filter all commitInstances with the given commitHash
	protected List<? extends Instance> filterCommit(List<? extends Instance> commitList, String commitId) {
		return commitList.stream().filter(commit -> commit.getCommit().equals(commitId)).collect(Collectors.toList());
	}

	// Test if all refactoring commits where found
	protected void assertRefactoring(List<RefactoringCommit> refactoringCommitList, String commit, String refactoring,
			int qty) {
		List<RefactoringCommit> inCommit = (List<RefactoringCommit>) filterCommit(refactoringCommitList, commit);

		long count = inCommit.stream().filter(refactoringCommit -> refactoringCommit.refactoring.equals(refactoring))
				.count();
		Assertions.assertEquals(qty, count);
	}

	// Test if all stable commits where detected
	protected void assertStableCommit(List<StableCommit> stableCommitList, String... commits) {
		Set<String> stableCommits = stableCommitList.stream().map(x -> x.getCommit()).collect(Collectors.toSet());
		Set<String> assertCommits = Set.of(commits);

		Assertions.assertEquals(commits.length, stableCommits.size());
		Assertions.assertEquals(stableCommits, assertCommits);
	}

	protected void assertMetaDataRefactoring(String commit, String commitMessage, String refactoringSummary,
			String commitUrl, int commitNumber, String parentCommit) {
		RefactoringCommit refactoringCommit = (RefactoringCommit) filterCommit(getRefactoringCommits(), commit).get(0);

		Assertions.assertEquals(refactoringSummary, refactoringCommit.refactoringSummary);
		assertMetaData(refactoringCommit.commitMetaData, commitUrl, commitNumber, parentCommit, commitMessage);
	}

	protected void assertMetaDataStable(String commit, String commitUrl, int commitNumber, String parentCommit,
			String commitMessage) {
		StableCommit stableCommit = (StableCommit) filterCommit(getStableCommits(), commit).get(0);

		assertMetaData(stableCommit.commitMetaData, commitUrl, commitNumber, parentCommit, commitMessage);
	}

	private void assertMetaData(CommitMetaData commitMetaData, String commitUrl, int commitNumber, String parentCommit,
			String commitMessage) {
		// TODO this doesnt work since the temp dir is in the url and is generated
		// separately from this test, do not generate dir in these tests to fix
		// Assertions.assertEquals(commitUrl, commitMetaData.commitUrl);
		Assertions.assertEquals(parentCommit, commitMetaData.parentCommitId);
		Assertions.assertEquals(commitMessage, commitMetaData.commitMessage);
		Assertions.assertEquals(commitNumber, commitMetaData.commitNumber);
	}

	protected void assertProcessMetrics(Instance instance, String truth) {
		Assertions.assertEquals(truth, instance.processMetrics.toString());
	}

	protected void assertInnerClass(List<? extends Instance> commitList, String commitId, String className, int qty) {
		List<? extends Instance> filteredList = filterCommit(commitList, commitId).stream()
				.filter(commit -> commit.classMetrics.isInnerClass && commit.getClassName().contains(className))
				.collect(Collectors.toList());
		Assertions.assertEquals(qty, filteredList.size());
	}

	// Test if the project metrics are computed correctly
	protected void assertProjectMetrics(int javaFilesCount, int productionFilesCount, int testFilesCount,
			int javaLocCount, int productionLocCount, int testLocCount) {
		Assertions.assertEquals(productionFilesCount, project.numberOfProductionFiles);
		Assertions.assertEquals(testFilesCount, project.numberOfTestFiles);
		Assertions.assertEquals(javaFilesCount, project.numberOfProductionFiles + project.numberOfTestFiles);

		Assertions.assertEquals(testLocCount, project.testLoc);
		Assertions.assertEquals(productionLocCount, project.productionLoc);
		Assertions.assertEquals(javaLocCount, project.javaLoc);
	}

	// Test if all Refactorings were classified
	@Test
	public void refactoringLevel() {
		List<RefactoringCommit> refactoringCommitsNoLevel = getRefactoringCommits().stream()
				.filter(refactoringCommit -> refactoringCommit.level < 0).collect(Collectors.toList());
		Assertions.assertEquals(0, refactoringCommitsNoLevel.size());
	}

	// @Test
	// public void checkExceptions() throws FileNotFoundException {
	// Assertions.assertFalse(
	// refactoringml.util.FileUtils.readFile("./logs/data-collection_test_ERROR.log").contains("Exception:
	// "));
	// }

	// @Test
	// public void checkCKMethodNotFound() throws FileNotFoundException {
	// Assertions.assertFalse(refactoringml.util.FileUtils.readFile("./logs/data-collection_test_ERROR.log")
	// .contains("CK did not find the refactored method:"));
	// }

	@Test
	@Transactional
	public void monitorDuplicateStableInstances() {

		String query = "SELECT COUNT(*) FROM (SELECT DISTINCT s.className, s.filePath, s.isTest, s.level, s.commitThreshold, s.classMetrics_id, s.commitMetaData_id, s.fieldMetrics_id, s.methodMetrics_id, s.processMetrics_id, s.project_id, s.variableMetrics_id From StableCommit s where s.project_id = "
				+ project.id + ") t";
		Object result = session.createNativeQuery(query).getSingleResult();
		int uniqueStableCommits = Integer.parseInt(result.toString());
		Assertions.assertEquals(uniqueStableCommits, getStableCommits().size());
	}

	@Test
	@Transactional
	public void monitorDuplicateRefactoringInstances() {

		String query = "SELECT COUNT(*) FROM (SELECT DISTINCT s.refactoring, s.refactoringSummary, s.className, s.filePath, s.isTest, s.level, s.classMetrics_id, s.commitMetaData_id, s.fieldMetrics_id, s.methodMetrics_id, s.processMetrics_id, s.project_id, s.variableMetrics_id From RefactoringCommit s where s.isValid = TRUE AND s.project_id = "
				+ project.id + ") t";
		Object result = session.createNativeQuery(query).getSingleResult();
		int uniqueRefactoringCommits = Integer.parseInt(result.toString());
		Assertions.assertEquals(uniqueRefactoringCommits, getRefactoringCommits().size());
	}

	// Test if we have invalid or redundant commit-metadata collected
	@Test
	@Transactional
	public void relevantCommitMetaData() {

		List<String> allRelevantCommitIds = session
				.createQuery(
						"SELECT DISTINCT r.commitMetaData.commitId FROM RefactoringCommit r WHERE r.isValid = TRUE")
				.getResultList();
		allRelevantCommitIds.addAll(
				session.createQuery("SELECT DISTINCT s.commitMetaData.commitId FROM StableCommit s").getResultList());
		allRelevantCommitIds = allRelevantCommitIds.stream().distinct().collect(Collectors.toList());
		List<String> allCommitMetaDatas = session.createQuery("SELECT DISTINCT c.commitId From CommitMetaData c")
				.getResultList();

		Assertions.assertEquals(allRelevantCommitIds.size(), allCommitMetaDatas.size());
	}
}
package refactoringml.integration.toyprojects;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import refactoringml.db.RefactoringCommit;
import refactoringml.integration.IntegrationBaseTest;

// tests related to PR #144: https://github.com/refactoring-ai/predicting-refactoring-ml/issues/144
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class R7ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r7.git";
	}

	@Test
	void t1() {
		List<RefactoringCommit> refactorings = getRefactoringCommits();
		Assertions.assertEquals(6, refactorings.size());

		Assertions.assertTrue(refactorings.stream().anyMatch(x -> x.refactoring.equals("Extract Interface")));
	}

	@Test
	public void MoveRefactoring() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();

		String moveRefactoring1 = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		assertRefactoring(refactoringCommitList, moveRefactoring1, "Move Class", 1);

		String moveRefactoring2 = "9cc7c77cfd38210eb44a67adda0545c0b6655017";
		assertRefactoring(refactoringCommitList, moveRefactoring2, "Move Class", 1);

		String moveRefactoring3 = "a0f7fbbfb859a23027705d2fdb12291795752736";
		assertRefactoring(refactoringCommitList, moveRefactoring3, "Move Class", 1);
	}

	@Test
	public void methodInvocations() {
		String commit = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		RefactoringCommit lastCommit = refactoringCommitList.stream()
				.filter(refactoringCommit -> refactoringCommit.getCommit().equals(commit)
						&& refactoringCommit.filePath.endsWith("A.java")
						&& refactoringCommit.refactoring.equals("Extract Method"))
				.findFirst().get();
		Assertions.assertEquals(1, lastCommit.methodMetrics.methodInvocationsQty);
		Assertions.assertEquals(0, lastCommit.methodMetrics.methodInvocationsLocalQty);
		Assertions.assertEquals(0, lastCommit.methodMetrics.methodInvocationsIndirectLocalQty);
	}

	@Test
	public void ProcessMetrics() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		String renameVariable = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		RefactoringCommit renameCommit = (RefactoringCommit) filterCommit(refactoringCommitList, renameVariable).get(0);
		assertProcessMetrics(renameCommit,
				"ProcessMetrics{qtyOfCommits=1, linesAdded=14, linesDeleted=0, qtyOfAuthors=1, qtyMinorAuthors=0, qtyMajorAuthors=1, authorOwnership=1.0, bugFixCount=0, refactoringsInvolved=0}");

		String extractInterface = "a0f7fbbfb859a23027705d2fdb12291795752736";
		RefactoringCommit extractCommit = (RefactoringCommit) filterCommit(refactoringCommitList, extractInterface)
				.get(0);
		assertProcessMetrics(extractCommit,
				"ProcessMetrics{qtyOfCommits=4, linesAdded=55, linesDeleted=1, qtyOfAuthors=1, qtyMinorAuthors=0, qtyMajorAuthors=1, authorOwnership=1.0, bugFixCount=0, refactoringsInvolved=4}");
	}
}
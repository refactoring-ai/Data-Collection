package refactoringml.integration.toyprojects;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import refactoringml.db.RefactoringCommit;
import refactoringml.integration.IntegrationBaseTest;

// tests related to PR #144: https://github.com/refactoring-ai/predicting-refactoring-ml/issues/144
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class R8ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r8.git";
	}

	@Test
	void t1() {
		List<RefactoringCommit> refactorings = getRefactoringCommits();
		Assertions.assertEquals(1, refactorings.size());

		Assertions.assertEquals("Rename Variable", refactorings.get(0).refactoring);

		// TODO: we need some assertion here that makes sure the exception is not thrown. Ideas?
	}
}
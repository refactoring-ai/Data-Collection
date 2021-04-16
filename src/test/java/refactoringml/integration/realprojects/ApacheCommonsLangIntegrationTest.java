package refactoringml.integration.realprojects;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import refactoringml.integration.IntegrationBaseTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest

public class ApacheCommonsLangIntegrationTest extends IntegrationBaseTest {
    @Override
    protected String getLastCommit() {
        return "2ea44b2adae8da8e3e7f55cc226479f9431feda9";
    }

    @Override
    protected String getRepo() {
        return "https://www.github.com/apache/commons-lang.git";
    }

    @Override
    protected String getStableCommitThreshold() {
        return "10";
    };

    // this test checks the Rename Method that has happened in
    // #5e7d64d6b2719afb1e5f4785d80d24ac5a19a782,
    // method isSet
    @Test
    public void t1() {
        // manually verified
        RefactoringCommit instance1 = getRefactoringCommits().stream()
                .filter(commit -> commit.getCommit().equals("5e7d64d6b2719afb1e5f4785d80d24ac5a19a782")
                        && commit.refactoring.equals("Extract Method")
                        && commit.methodMetrics.getFullMethodName().equals("isSameDay/2[Date,Date]"))
                .collect(Collectors.toList()).get(0);

        Assertions.assertNotNull(instance1);

        Assertions.assertEquals("isSameDay/2[Date,Date]", instance1.methodMetrics.getFullMethodName());
        Assertions.assertEquals(2, instance1.methodMetrics.methodVariablesQty);
        Assertions.assertEquals(1, instance1.methodMetrics.methodMaxNestedBlocks);
        Assertions.assertEquals(1, instance1.methodMetrics.methodReturnQty);
        Assertions.assertEquals(0, instance1.methodMetrics.methodTryCatchQty);
    }

    // this test follows the
    // src/java/org/apache/commons/lang/builder/HashCodeBuilder.java file

    @Test
    public void t2() {
        List<StableCommit> stableCommitList = getStableCommits().stream()
                .filter(commit -> commit.filePath
                        .equals("src/java/org/apache/commons/lang/builder/HashCodeBuilder.java") && commit.level == 1)
                .collect(Collectors.toList());
        // it has been through 9 different refactorings
        List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(
                commit -> commit.filePath.equals("src/java/org/apache/commons/lang/builder/HashCodeBuilder.java"))
                .collect(Collectors.toList());

        Assertions.assertEquals(2, stableCommitList.size());
        Assertions.assertEquals(20, refactoringCommitList.size());

        Assertions.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", stableCommitList.get(0).getCommit());

        // then, it was refactored two times (in commit
        // 5c40090fecdacd9366bba7e3e29d94f213cf2633)
        Assertions.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", refactoringCommitList.get(0).getCommit());
        Assertions.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", refactoringCommitList.get(1).getCommit());

        // It appears 3 times
        Assertions.assertEquals("379d1bcac32d75e6c7f32661b2203f930f9989df", stableCommitList.get(1).getCommit());
        // Assertions.assertEquals("d3c425d6f1281d9387f5b80836ce855bc168453d", stableCommitList.get(2).getCommit());
        // Assertions.assertEquals("3ed99652c84339375f1e6b99bd9c7f71d565e023", stableCommitList.get(3).getCommit());
    }

    // check the number of test and production files as well as their LOC
    @Test
    void projectMetrics() {
        assertProjectMetrics(403, 216, 187, 85053, 29836, 55217);
    }
}
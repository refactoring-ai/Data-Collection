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

// TODO check this case, after RM updates the results change
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApacheCommonsCSVIntegrationTest extends IntegrationBaseTest {
    @Override
    protected String getLastCommit() {
        return "70092bb303af69b09bf3978b24c1faa87c909e3c";
    }

    @Override
    protected String getRepo() {
        return "https://www.github.com/apache/commons-csv.git";
    }

    @Override
    protected String getStableCommitThreshold() {
        return "50";
    };

    // this test checks the Rename Parameter that has happened in
    // #b58168683d01149a568734df21568ffcc41105fe,
    // method isSet
    @Test
    public void randomRefactoring() {
        // manually verified
        List<RefactoringCommit> commits = getRefactoringCommits();
        commits = commits.stream()
                .filter(commit -> commit.getCommit().equals("b58168683d01149a568734df21568ffcc41105fe")
                        && commit.refactoring.equals("Rename Parameter")
                        && commit.methodMetrics.getFullMethodName().equals("isSet/1[int]"))
                .collect(Collectors.toList());
        RefactoringCommit instance1 = commits.get(0);
        Assertions.assertNotNull(instance1);

        Assertions.assertEquals(0, instance1.methodMetrics.methodVariablesQty);
        Assertions.assertEquals(0, instance1.methodMetrics.methodMaxNestedBlocks);
        Assertions.assertEquals(1, instance1.methodMetrics.methodReturnQty);
        Assertions.assertEquals(0, instance1.methodMetrics.methodTryCatchQty);
    }

    // this test follows the src/main/java/org/apache/commons/csv/CSVFormat.java
    // file
    @Test
    public void refactoringsCSVFormat() {
        String fileName = "src/main/java/org/apache/commons/csv/CSVFormat.java";
        List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream()
                .filter(commit -> commit.filePath.equals(fileName)).collect(Collectors.toList());

        // refactoring miner detects precisely 145 refactorings in this file
        Assertions.assertEquals(145, refactoringCommitList.size());

        assertRefactoring(refactoringCommitList, "56ca5858db4765112dca44e5deeda0ac181a6766", "Extract Class", 1);
        assertRefactoring(refactoringCommitList, "6a34b823c807325bc251ef43c66c307adcd947b8", "Extract Class", 1);
        assertRefactoring(refactoringCommitList, "50e2719bb646870dc08dd71f2bc2314ce46def76", "Extract Method", 4);
        assertRefactoring(refactoringCommitList, "7c770e0b53235e90a216ae3b214048b765cda0c0", "Inline Method", 1);
        assertRefactoring(refactoringCommitList, "6a34b823c807325bc251ef43c66c307adcd947b8", "Move Method", 7);
        assertRefactoring(refactoringCommitList, "75f39a81a77b3680c21cd3f810da62ebbe9944b8", "Move Method", 9);
        assertRefactoring(refactoringCommitList, "c0d91d205d5494dc402dab13edcc89679aecd547", "Move Method", 1);
        assertRefactoring(refactoringCommitList, "040c2606eb7e2cfe60e4bbcbf2928f1e971ce4b4", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "0dbb499888e5e17322d08802222f2453bf5621a6", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "322fad25ad96da607a3045a19702a55381a426e7", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "38741a48c692ae2fc13cd2445e77ace6ecea1156", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "4695d73e3b1974454d55a30be2b1c3a4bddbf3db", "Rename Method", 4);
        assertRefactoring(refactoringCommitList, "5a0894f9e0ee9f4703b8db3f200ff4a507bf043b", "Rename Method", 2);
        assertRefactoring(refactoringCommitList, "67bbc35289bb3435eae0bd6f20cc6b15280e66e0", "Rename Method", 4);
        assertRefactoring(refactoringCommitList, "67d150adc88b806e52470d110a438d9107e72ed5", "Rename Method", 2);
        assertRefactoring(refactoringCommitList, "6e57364216b78bca031f764b8d0a46494ba27b46", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "73ec29f75f1dd6f0c52e9c310dc4f8414346f49a", "Rename Method", 3);
        assertRefactoring(refactoringCommitList, "75f39a81a77b3680c21cd3f810da62ebbe9944b8", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "7ac5dd3ec633d64603bb836d0576f34a51f93f08", "Rename Method", 2);
        // assertRefactoring(refactoringCommitList, "9fb2b4f2b100c8d5a769532ee26973832c2a61c0", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "a72c71f5cc6431890f82707a2782325be6747dd1", "Rename Method", 2);
        assertRefactoring(refactoringCommitList, "aa0762d538c52f4384f629bb799769f5f85c1c9e", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "ecea0c35993b2428e0a938933896329c413de40e", "Rename Method", 1);
        assertRefactoring(refactoringCommitList, "f51f89828df4d763148362e312e310435864ba96", "Rename Method", 5);
        assertRefactoring(refactoringCommitList, "f9a3162037f7e82ce6927bbe944b7d61349f8c11", "Rename Method", 9);
        assertRefactoring(refactoringCommitList, "040c2606eb7e2cfe60e4bbcbf2928f1e971ce4b4", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "38741a48c692ae2fc13cd2445e77ace6ecea1156", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "5a0894f9e0ee9f4703b8db3f200ff4a507bf043b", "Rename Parameter", 3);
        assertRefactoring(refactoringCommitList, "67bbc35289bb3435eae0bd6f20cc6b15280e66e0", "Rename Parameter", 2);
        assertRefactoring(refactoringCommitList, "73ec29f75f1dd6f0c52e9c310dc4f8414346f49a", "Rename Parameter", 2);
        assertRefactoring(refactoringCommitList, "939a8a04eb9391fb29f5e22594a4fd988c89fd57", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "a72c71f5cc6431890f82707a2782325be6747dd1", "Rename Parameter", 2);
        assertRefactoring(refactoringCommitList, "aa0762d538c52f4384f629bb799769f5f85c1c9e", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "afc9de71bd8bb51dfc7ab12df2aeb7a38b709ef2", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "ecea0c35993b2428e0a938933896329c413de40e", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "f80c5bd0ad8ed0739f43a2ed6392d94bbceae1c9", "Rename Parameter", 1);
    }

    // this test follows the src/main/java/org/apache/commons/csv/CSVFormat.java
    // file
    @Test
    public void stableCSVFormat() {
        String fileName = "src/main/java/org/apache/commons/csv/CSVFormat.java";
        List<StableCommit> stableCommitListLevel2 = getStableCommits().stream()
                .filter(commit -> commit.filePath.equals(fileName) && commit.level == 2).collect(Collectors.toList());

        Assertions.assertEquals(43, stableCommitListLevel2.size());

        List<StableCommit> stableCommitListLevel3 = getStableCommits().stream()
                .filter(commit -> commit.filePath.equals(fileName) && commit.level == 3).collect(Collectors.toList());

        Assertions.assertEquals(43, stableCommitListLevel3.size());

        assertStableCommit(stableCommitListLevel2, "67d150adc88b806e52470d110a438d9107e72ed5");

        // also manually validated
        Assertions.assertEquals(5, stableCommitListLevel2.get(0).classMetrics.classNumberOfPublicFields);
        Assertions.assertEquals(39, stableCommitListLevel2.get(0).classMetrics.classNumberOfPublicMethods);
        Assertions.assertEquals(13, stableCommitListLevel2.get(0).classMetrics.classNumberOfPrivateFields);
        Assertions.assertEquals(4, stableCommitListLevel2.get(0).classMetrics.classNumberOfPrivateMethods);

        // also manually validated
        Assertions.assertEquals(215, stableCommitListLevel2.get(0).processMetrics.qtyOfCommits);

        // in refactorings_CSVFormat, we see that there are 82 refactorings in total.
        // after this commit, there was just one more refactoring. Thus, 81 refactorings
        Assertions.assertEquals(186, stableCommitListLevel2.get(0).processMetrics.refactoringsInvolved);
    }

    @Test
    public void refactoringsCSVStrategy() {
        String fileName = "src/main/java/org/apache/commons/csv/CSVStrategy.java";
        // and 10 with the old name 'CSVStrategy.java'
        List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream()
                .filter(commit -> commit.filePath.equals(fileName)).distinct().collect(Collectors.toList());

        assertRefactoring(refactoringCommitList, "42476f4b08fe4b075aa36f688f0801857f3635d9", "Rename Method", 5);
        // assertRefactoring(refactoringCommitList, "42476f4b08fe4b075aa36f688f0801857f3635d9", "Rename Parameter", 4);
        // assertRefactoring(refactoringCommitList, "43b777b9829141a3eb417ebf3ce49c8444884af0", "Inline Method", 2); TODO changed due to updated RM
        assertRefactoring(refactoringCommitList, "43b777b9829141a3eb417ebf3ce49c8444884af0", "Rename Parameter", 1);
        assertRefactoring(refactoringCommitList, "cb99634ab3d6143dffc90938fc68e15c7f9d25b8", "Rename Class", 1);
        assertRefactoring(refactoringCommitList, "cb99634ab3d6143dffc90938fc68e15c7f9d25b8", "Rename Variable", 9);

        Assertions.assertEquals(40, refactoringCommitList.size());
    }

    @Test
    public void stableCSVStrategy() {
        String fileName = "src/main/java/org/apache/commons/csv/CSVStrategy.java";
        List<StableCommit> stableCommitList = getStableCommits().stream()
                .filter(commit -> commit.filePath.equals(fileName)).collect(Collectors.toList());
        Assertions.assertEquals(0, stableCommitList.size());
    }

    // check the number of test and production files as well as their LOC
    @Test
    public void projectMetrics() {
        assertProjectMetrics(35, 11, 24, 7240, 1855, 5385);
    }
}
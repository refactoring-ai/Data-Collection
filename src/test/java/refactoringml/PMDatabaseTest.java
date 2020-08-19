package refactoringml;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import refactoringml.db.CommitMetaData;

//Test the PMDatabase class
//Closely linked to the
class PMDatabaseTest {
    @Test
    void constructor() {
        Map<String, ProcessMetricTracker> database = new HashMap<>();
        PMDatabase pmDatabase = new PMDatabase();

        String expected = "PMDatabase{" + "database=" + database.toString() + "}";
        Assert.assertEquals(expected, pmDatabase.toString());
    }

    // Test the case sensitivity of class fileNames
    // Be aware: .Java and .java are equal for java
    @Ignore
    @Test
    void caseSensitivity() {
        PMDatabase pmDatabase = new PMDatabase();

        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        pmDatabase.reportChanges("A.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        Assert.assertNotEquals(pmDatabase.find("a.Java"), pmDatabase.find("A.Java"));

        pmDatabase.reportChanges("a.java", new CommitMetaData("#2", "n", "n", "0"), "R", 1, 1);
        Assert.assertEquals(pmDatabase.find("a.Java"), pmDatabase.find("a.java"));
    }

    @Test
    void reportChanges() {
        PMDatabase pmDatabase = new PMDatabase();
        Assert.assertEquals(0, pmDatabase.findStableInstances(List.of(10, 25)).size());

        // test if a new pm tracker is created with the right values
        ProcessMetricTracker aTracker = pmDatabase.reportChanges("a.Java",
                new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(aTracker, pmDatabase.find("a.Java"));
        Assert.assertFalse(aTracker.calculateStability(List.of(10, 25)));
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertNotNull(pmDatabase.find("a.Java").getBaseProcessMetrics());
        Assert.assertNotNull(pmDatabase.find("a.Java").getCurrentProcessMetrics());

        // test if another new pm tracker is created with the right values
        pmDatabase.reportChanges("A.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotEquals(pmDatabase.find("a.Java"), pmDatabase.find("A.Java"));
        Assert.assertNotNull(pmDatabase.find("A.Java"));
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
        Assert.assertNotNull(pmDatabase.find("A.Java").getBaseProcessMetrics());
        Assert.assertNotNull(pmDatabase.find("A.Java").getCurrentProcessMetrics());

        // test if an existing pmTracker is updated correctly
        pmDatabase.reportChanges("a.Java", new CommitMetaData("#2", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(2, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertNotEquals(pmDatabase.find("a.Java").getBaseProcessMetrics().qtyOfCommits,
                pmDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
    }

    @Test
    void reportRefactoring() {
        PMDatabase pmDatabase = new PMDatabase();

        pmDatabase.reportRefactoring("a.Java", new CommitMetaData("#1", "null", "null", "0"));
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(0, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(pmDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(1, pmDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("#2", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        pmDatabase.reportChanges("A.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());

        pmDatabase.reportRefactoring("a.Java", new CommitMetaData("#3", "null", "null", "0"));
        Assert.assertEquals(0, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(pmDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assert.assertEquals(2, pmDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(2, pmDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
    }

    // take care of case sensitivity
    @Test
    void removeFile1() {
        PMDatabase pmDatabase = new PMDatabase();
        Map<String, ProcessMetricTracker> database = new HashMap<>();

        pmDatabase.removeFile("a.Java");
        String expected = "PMDatabase{" + "database=" + database.toString() + "}";
        Assert.assertEquals(expected, pmDatabase.toString());

        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmDatabase.find("a.Java"));

        pmDatabase.removeFile("a.Java");
        Assert.assertNull(pmDatabase.find("a.Java"));
    }

    // take care of case sensitivity
    @Test
    void renameFile() {
        PMDatabase pmDatabase = new PMDatabase();

        ProcessMetricTracker oldPMTracker = pmDatabase.renameFile("a.Java", "A.Java",
                new CommitMetaData("1", "null", "null", "0"));
        Assert.assertNull(oldPMTracker);
        Assert.assertNotNull(pmDatabase.find("A.Java"));
        Assert.assertNull(pmDatabase.find("a.Java"));
        Assert.assertEquals("A.Java", pmDatabase.find("A.Java").getFileName());

        pmDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("A.Java"));
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
        Assert.assertEquals(0, pmDatabase.find("A.Java").getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, pmDatabase.find("A.Java").getCurrentProcessMetrics().linesAdded);

        oldPMTracker = pmDatabase.renameFile("A.Java", "B.Java", new CommitMetaData("1", "null", "null", "0"));
        Assert.assertNotNull(oldPMTracker);
        Assert.assertEquals("A.Java", oldPMTracker.getFileName());
        Assert.assertEquals(1, oldPMTracker.getCommitCounter());
        Assert.assertEquals(0, oldPMTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, oldPMTracker.getCurrentProcessMetrics().linesAdded);

        pmDatabase.reportChanges("B.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("B.Java"));
        Assert.assertEquals("B.Java", pmDatabase.find("B.Java").getFileName());
        Assert.assertEquals(2, pmDatabase.find("B.Java").getCommitCounter());
        Assert.assertEquals(2, pmDatabase.find("B.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(40, pmDatabase.find("B.Java").getCurrentProcessMetrics().linesDeleted);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(20, pmDatabase.find("a.Java").getCurrentProcessMetrics().linesDeleted);
    }

    // take care of case sensitivity
    @Test
    void renameFile2() {
        PMDatabase pmDatabase = new PMDatabase();
        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmDatabase.renameFile("a.Java", "A.Java", new CommitMetaData("1", "null", "null", "0"));

        ProcessMetricTracker pmTracker = pmDatabase.find("A.Java");
        Assert.assertNotNull(pmTracker);
        Assert.assertEquals(10, pmTracker.getCurrentProcessMetrics().linesAdded);
        Assert.assertEquals(0, pmTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals("#1", pmTracker.getBaseCommitMetaData().getCommitId());
    }

    @Test
    void renameFile3() {
        PMDatabase pmDatabase = new PMDatabase();
        CommitMetaData commitMetaData = new CommitMetaData("1", "null", "null", "0");
        assertThrows(IllegalArgumentException.class, () -> pmDatabase.renameFile("a.Java", "a.Java", commitMetaData));
    }

    // take care of renamed files
    @Test
    void removeFile2() {
        PMDatabase pmDatabase = new PMDatabase();
        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmDatabase.renameFile("a.Java", "A.Java", new CommitMetaData("1", "null", "null", "0"));

        ProcessMetricTracker pmTracker = pmDatabase.removeFile("a.Java");
        Assert.assertNull(pmTracker);
        pmTracker = pmDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmTracker);
    }

    @Test
    void isStable1() {
        PMDatabase pm = new PMDatabase();
        List<Integer> stableCommitCounts = List.of(10, 25);

        for (int i = 0; i < 9; i++) {
            ProcessMetricTracker aTracker = pm.reportChanges("a.Java",
                    new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
            ProcessMetricTracker bTracker = pm.reportChanges("b.Java",
                    new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertFalse(aTracker.calculateStability(stableCommitCounts));
            Assert.assertFalse(bTracker.calculateStability(stableCommitCounts));
            Assert.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
            Assert.assertEquals(i + 1, pm.find("b.Java").getCommitCounter());
        }

        ProcessMetricTracker bTracker = pm.reportChanges("b.Java", new CommitMetaData("#10", "null", "null", "0"),
                "Rafael", 10, 20);
        Assert.assertEquals(pm.find("b.Java"), bTracker);
        Assert.assertEquals(10, pm.find("b.Java").getCommitCounter());
        Assert.assertTrue(bTracker.calculateStability(stableCommitCounts));

        for (int i = 0; i < 14; i++) {
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i + 11), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("#10", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assert.assertEquals(10, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());

        pm.reportChanges("b.Java", new CommitMetaData("#25", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pm.findStableInstances(stableCommitCounts).size());
        Assert.assertEquals(25,
                pm.findStableInstances(stableCommitCounts).stream()
                        .filter(pmTracker -> pmTracker.getFileName().equals("b.Java")).collect(Collectors.toList())
                        .get(0).getCommitCountThreshold());

        pm.reportRefactoring("b.Java", new CommitMetaData("#26", "null", "null", "0"));
        Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());

        Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assert.assertEquals(10, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());
    }

    @Test
    void isStable2() {
        PMDatabase pm = new PMDatabase();
        List<Integer> stableCommitCounts = List.of(10, 25);

        for (int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0"), "Rafael", 10, 20);
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
        }

        pm.reportRefactoring("a.Java", new CommitMetaData("#" + (10), "null", "null", "0"));
        Assert.assertEquals(0, pm.find("a.Java").getCommitCounter());
        Assert.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());

        for (int i = 0; i < 2; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 10), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
            Assert.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
        }

        pm.reportChanges("b.Java", new CommitMetaData("#" + (9), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());

        for (int i = 0; i < 7; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("#" + (20), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pm.findStableInstances(stableCommitCounts).size());
    }

    @Test
    void multipleKs1() {
        PMDatabase pm = new PMDatabase();
        List<Integer> stableCommitCounts = List.of(10, 25);

        for (int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
        }
        pm.reportChanges("a.Java", new CommitMetaData("#" + (10), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assert.assertEquals(10, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());

        pm.reportChanges("a.Java", new CommitMetaData("#" + (11), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());

        for (int i = 0; i < 14; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0"), "Rafael", 10, 20);
        }
        Assert.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assert.assertEquals(25, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());

        pm.reportRefactoring("a.Java", new CommitMetaData("#" + (26), "null", "null", "0"));
        Assert.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
    }
}
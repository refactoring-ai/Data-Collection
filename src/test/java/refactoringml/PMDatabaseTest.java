package refactoringml;

import org.junit.jupiter.api.Assertions;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import refactoringml.db.CommitMetaData;
import refactoringml.db.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Test the PMDatabase class
//Closely linked to the
public class PMDatabaseTest {
    Project project = new Project();


    @Test
    public void constructor() {
        Map<String, ProcessMetricTracker> database = new HashMap<>();
        PMDatabase pmDatabase = new PMDatabase();

        String expected = "PMDatabase{" + "database=" + database.toString() + "}";
        Assertions.assertEquals(expected, pmDatabase.toString());
    }

    // Test the case sensitivity of class fileNames
    // Be aware: .Java and .java are equal for java
    // @Ignore
    // @Test
    // void caseSensitivity() {
    //     PMDatabase pmDatabase = new PMDatabase();
    //     pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "n", "n", "0", project), "R", 1, 1);
    //     pmDatabase.reportChanges("A.Java", new CommitMetaData("#1", "n", "n", "0", project), "R", 1, 1);
    //     Assertions.assertNotEquals(pmDatabase.find("a.Java"), pmDatabase.find("A.Java"));

    //     pmDatabase.reportChanges("a.java", new CommitMetaData("#2", "n", "n", "0", project), "R", 1, 1);
    //     Assertions.assertEquals(pmDatabase.find("a.Java"), pmDatabase.find("a.java"));
    // }

    @Test
    public void reportChanges() {
        PMDatabase pmDatabase = new PMDatabase();
        Assertions.assertEquals(0, pmDatabase.findStableInstances(List.of(10, 25)).size());

        // test if a new pm tracker is created with the right values
        ProcessMetricTracker aTracker = pmDatabase.reportChanges("a.Java",
                new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertNotNull(pmDatabase.find("a.Java"));
        Assertions.assertEquals(aTracker, pmDatabase.find("a.Java"));
        Assertions.assertFalse(aTracker.calculateStability(List.of(10, 25)));
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assertions.assertNotNull(pmDatabase.find("a.Java").getBaseProcessMetrics());
        Assertions.assertNotNull(pmDatabase.find("a.Java").getCurrentProcessMetrics());

        // test if another new pm tracker is created with the right values
        pmDatabase.reportChanges("A.Java", new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertNotEquals(pmDatabase.find("a.Java"), pmDatabase.find("A.Java"));
        Assertions.assertNotNull(pmDatabase.find("A.Java"));
        Assertions.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
        Assertions.assertNotNull(pmDatabase.find("A.Java").getBaseProcessMetrics());
        Assertions.assertNotNull(pmDatabase.find("A.Java").getCurrentProcessMetrics());

        // test if an existing pmTracker is updated correctly
        pmDatabase.reportChanges("a.Java", new CommitMetaData("#2", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertNotNull(pmDatabase.find("a.Java"));
        Assertions.assertEquals(2, pmDatabase.find("a.Java").getCommitCounter());
        Assertions.assertNotEquals(pmDatabase.find("a.Java").getBaseProcessMetrics().qtyOfCommits,
                pmDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
    }

    @Test
    public void reportRefactoring() {
        PMDatabase pmDatabase = new PMDatabase();

        pmDatabase.reportRefactoring("a.Java", new CommitMetaData("#1", "null", "null", "0", project));
        Assertions.assertNotNull(pmDatabase.find("a.Java"));
        Assertions.assertEquals(0, pmDatabase.find("a.Java").getCommitCounter());
        Assertions.assertEquals(pmDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("#2", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        pmDatabase.reportChanges("A.Java", new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assertions.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());

        pmDatabase.reportRefactoring("a.Java", new CommitMetaData("#3", "null", "null", "0", project));
        Assertions.assertEquals(0, pmDatabase.find("a.Java").getCommitCounter());
        Assertions.assertEquals(pmDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assertions.assertEquals(2, pmDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assertions.assertEquals(2, pmDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);
        Assertions.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
    }

    // take care of case sensitivity
    @Test
    public void removeFile1() {
        PMDatabase pmDatabase = new PMDatabase();
        Map<String, ProcessMetricTracker> database = new HashMap<>();

        pmDatabase.removeFile("a.Java");
        String expected = "PMDatabase{" + "database=" + database.toString() + "}";
        Assertions.assertEquals(expected, pmDatabase.toString());

        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
        pmDatabase.removeFile("A.Java");
        Assertions.assertNotNull(pmDatabase.find("a.Java"));

        pmDatabase.removeFile("a.Java");
        Assertions.assertNull(pmDatabase.find("a.Java"));
    }

    // take care of case sensitivity
    @Test
    public void renameFile() {
        PMDatabase pmDatabase = new PMDatabase();

        ProcessMetricTracker oldPMTracker = pmDatabase.renameFile("a.Java", "A.Java",
                new CommitMetaData("1", "null", "null", "0", project));
        Assertions.assertNull(oldPMTracker);
        Assertions.assertNotNull(pmDatabase.find("A.Java"));
        Assertions.assertNull(pmDatabase.find("a.Java"));
        Assertions.assertEquals("A.Java", pmDatabase.find("A.Java").getFileName());

        pmDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertNotNull(pmDatabase.find("A.Java"));
        Assertions.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
        Assertions.assertEquals(0, pmDatabase.find("A.Java").getBaseProcessMetrics().linesAdded);
        Assertions.assertEquals(10, pmDatabase.find("A.Java").getCurrentProcessMetrics().linesAdded);

        oldPMTracker = pmDatabase.renameFile("A.Java", "B.Java", new CommitMetaData("1", "null", "null", "0", project));
        Assertions.assertNotNull(oldPMTracker);
        Assertions.assertEquals("A.Java", oldPMTracker.getFileName());
        Assertions.assertEquals(1, oldPMTracker.getCommitCounter());
        Assertions.assertEquals(0, oldPMTracker.getBaseProcessMetrics().linesAdded);
        Assertions.assertEquals(10, oldPMTracker.getCurrentProcessMetrics().linesAdded);

        pmDatabase.reportChanges("B.Java", new CommitMetaData("1", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertNotNull(pmDatabase.find("B.Java"));
        Assertions.assertEquals("B.Java", pmDatabase.find("B.Java").getFileName());
        Assertions.assertEquals(2, pmDatabase.find("B.Java").getCommitCounter());
        Assertions.assertEquals(2, pmDatabase.find("B.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assertions.assertEquals(40, pmDatabase.find("B.Java").getCurrentProcessMetrics().linesDeleted);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertNotNull(pmDatabase.find("a.Java"));
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assertions.assertEquals(1, pmDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assertions.assertEquals(20, pmDatabase.find("a.Java").getCurrentProcessMetrics().linesDeleted);
    }

    // take care of case sensitivity
    @Test
    public void renameFile2() {
        PMDatabase pmDatabase = new PMDatabase();
        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
        pmDatabase.renameFile("a.Java", "A.Java", new CommitMetaData("1", "null", "null", "0", project));

        ProcessMetricTracker pmTracker = pmDatabase.find("A.Java");
        Assertions.assertNotNull(pmTracker);
        Assertions.assertEquals(10, pmTracker.getCurrentProcessMetrics().linesAdded);
        Assertions.assertEquals(0, pmTracker.getBaseProcessMetrics().linesAdded);
        Assertions.assertEquals("#1", pmTracker.getBaseCommitMetaData().commitId);
    }

    // @Test(IllegalArgumentException.class)
    // public void renameFile3() {
    //     PMDatabase pmDatabase = new PMDatabase();
    //     pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
    //     pmDatabase.renameFile("a.Java", "a.Java", new CommitMetaData("1", "null", "null", "0", project));
    // }

    // take care of renamed files
    @Test
    public void removeFile2() {
        PMDatabase pmDatabase = new PMDatabase();
        pmDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0", project), "Rafael", 10, 20);
        pmDatabase.renameFile("a.Java", "A.Java", new CommitMetaData("1", "null", "null", "0", project));

        ProcessMetricTracker pmTracker = pmDatabase.removeFile("a.Java");
        Assertions.assertNull(pmTracker);
        pmTracker = pmDatabase.removeFile("A.Java");
        Assertions.assertNotNull(pmTracker);
    }

    @Test
    public void isStable1() {
        PMDatabase pm = new PMDatabase();
        List<Integer> stableCommitCounts = List.of(10, 25);

        for (int i = 0; i < 9; i++) {
            ProcessMetricTracker aTracker = pm.reportChanges("a.Java",
                    new CommitMetaData("#" + (i), "null", "null", "0", project), "Rafael", 10, 20);
            ProcessMetricTracker bTracker = pm.reportChanges("b.Java",
                    new CommitMetaData("#" + (i), "null", "null", "0", project), "Rafael", 10, 20);
            Assertions.assertFalse(aTracker.calculateStability(stableCommitCounts));
            Assertions.assertFalse(bTracker.calculateStability(stableCommitCounts));
            Assertions.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
            Assertions.assertEquals(i + 1, pm.find("b.Java").getCommitCounter());
        }

        ProcessMetricTracker bTracker = pm.reportChanges("b.Java", new CommitMetaData("#10", "null", "null", "0", project),
                "Rafael", 10, 20);
        Assertions.assertEquals(pm.find("b.Java"), bTracker);
        Assertions.assertEquals(10, pm.find("b.Java").getCommitCounter());
        Assertions.assertTrue(bTracker.calculateStability(stableCommitCounts));

        for (int i = 0; i < 14; i++) {
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i + 11), "null", "null", "0", project), "Rafael", 10, 20);
            Assertions.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("#10", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assertions.assertEquals(10, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());

        pm.reportChanges("b.Java", new CommitMetaData("#25", "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(2, pm.findStableInstances(stableCommitCounts).size());
        Assertions.assertEquals(25,
                pm.findStableInstances(stableCommitCounts).stream()
                        .filter(pmTracker -> pmTracker.getFileName().equals("b.Java")).collect(Collectors.toList())
                        .get(0).getCommitCountThreshold());

        pm.reportRefactoring("b.Java", new CommitMetaData("#26", "null", "null", "0", project));
        Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());

        Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assertions.assertEquals(10, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());
    }

    @Test
    public void isStable2() {
        PMDatabase pm = new PMDatabase();
        List<Integer> stableCommitCounts = List.of(10, 25);

        for (int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0", project), "Rafael", 10, 20);
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0", project), "Rafael", 10, 20);
            Assertions.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
        }

        pm.reportRefactoring("a.Java", new CommitMetaData("#" + (10), "null", "null", "0", project));
        Assertions.assertEquals(0, pm.find("a.Java").getCommitCounter());
        Assertions.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());

        for (int i = 0; i < 2; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 10), "null", "null", "0", project), "Rafael", 10, 20);
            Assertions.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
            Assertions.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
        }

        pm.reportChanges("b.Java", new CommitMetaData("#" + (9), "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());

        for (int i = 0; i < 7; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0", project), "Rafael", 10, 20);
            Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("#" + (20), "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(2, pm.findStableInstances(stableCommitCounts).size());
    }

    @Test
    public void multipleKs1() {
        PMDatabase pm = new PMDatabase();
        List<Integer> stableCommitCounts = List.of(10, 25);

        for (int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i), "null", "null", "0", project), "Rafael", 10, 20);
        }
        pm.reportChanges("a.Java", new CommitMetaData("#" + (10), "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assertions.assertEquals(10, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());

        pm.reportChanges("a.Java", new CommitMetaData("#" + (11), "null", "null", "0", project), "Rafael", 10, 20);
        Assertions.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());

        for (int i = 0; i < 14; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0", project), "Rafael", 10, 20);
        }
        Assertions.assertEquals(1, pm.findStableInstances(stableCommitCounts).size());
        Assertions.assertEquals(25, pm.findStableInstances(stableCommitCounts).get(0).getCommitCountThreshold());

        pm.reportRefactoring("a.Java", new CommitMetaData("#" + (26), "null", "null", "0", project));
        Assertions.assertEquals(0, pm.findStableInstances(stableCommitCounts).size());
    }
}
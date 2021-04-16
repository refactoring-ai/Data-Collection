package refactoringml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import refactoringml.db.CommitMetaData;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.Project;

import java.util.List;
import java.util.Random;

//Tests the ProcessMetricTracker and ProcessMetrics classes
//Closely linked to PMDatabaseTest
public class ProcessMetricTrackerTest {
	// Test if the constructor of the ProcessMetricTracker works as intended
	@Test
	public void constructor() {
		CommitMetaData commitMetaData = new CommitMetaData();
		commitMetaData.project = new Project();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		// check if the the commit meta data is still refering to the same object, we
		// want to reduce the memory usage, as commitmetadata is stable
		Assertions.assertFalse(pm.getBaseProcessMetrics() == pm.getCurrentProcessMetrics());
		// Check if the two pm metrics are not accidently refering to the same object,
		// because process metrics are not stable
		Assertions.assertTrue(pm.getBaseCommitMetaData() == commitMetaData);

		ProcessMetrics baseProcessMetrics = new ProcessMetrics(0, 0, 0, 0, 0, null);
		// Assertions if the process metrics are "empty" at the beginning of the
		// tracking process
		Assertions.assertEquals(baseProcessMetrics.toString(), pm.getBaseProcessMetrics().toString());
		Assertions.assertEquals(baseProcessMetrics.toString(), pm.getCurrentProcessMetrics().toString());
	}

	// test if the ProcessMetricTracker reportCommit function behaves as expected
	@Test
	public void reportCommit() {
		CommitMetaData commitMetaData = new CommitMetaData();
		commitMetaData.project = new Project();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		for (int i = 0; i < 21; i++) {
			pm.reportCommit("commit #" + i, "Mauricio", 1, 1);
		}
		pm.reportCommit("Bug fix in commit #" + 22, "Jan", 10, 5);

		// no refactoring was done on a.Java, thus the base commit process metrics
		// should be empty
		ProcessMetrics baseProcessMetrics = new ProcessMetrics(0, 0, 0, 0, 0, null);
		Assertions.assertEquals(baseProcessMetrics.toString(), pm.getBaseProcessMetrics().toString());

		// 22 commits affected a.Java, thus the current process metrics should look like
		// this:
		ProcessMetrics currentProcessMetrics = new ProcessMetrics(22, 31, 26, 1, 0, null);
		for (int i = 0; i < 21; i++) {
			currentProcessMetrics.updateAuthorCommits("Mauricio");
		}
		currentProcessMetrics.updateAuthorCommits("Jan");
		Assertions.assertEquals(currentProcessMetrics.toString(), pm.getCurrentProcessMetrics().toString());

		// Assertions if the tracker is counting the commits since the last refactoring
		// correct
		Assertions.assertEquals(22, pm.getCommitCounter());
	}

	// test if the ProcessMetricTracker reset function behaves as expected
	@Test
	public void resetClassFile() {
		CommitMetaData commitMetaData = new CommitMetaData();
		commitMetaData.project = new Project();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		for (int i = 0; i < 21; i++) {
			pm.reportCommit("commit #" + i, "Mauricio", 1, 1);
		}
		pm.reportCommit("Bug fix in commit #" + 22, "Jan", 10, 5);

		// Reset the tracker counter, because a refactoring happened in this commit
		CommitMetaData refactoringCommitMetaData = new CommitMetaData();
		pm.resetCounter(refactoringCommitMetaData);

		// Assertions if the new commit metadata is the same object as the one in the
		// tracker after the reset
		Assertions.assertTrue(refactoringCommitMetaData == pm.getBaseCommitMetaData());

		// Assertions if the current processmetrics were deep copied to the base process
		// metrics
		Assertions.assertFalse(pm.getBaseProcessMetrics() == pm.getCurrentProcessMetrics());
		Assertions.assertEquals(pm.getCurrentProcessMetrics().toString(), pm.getBaseProcessMetrics().toString());

		// Assertions if the values of the deep base process metrics are correct
		// 22 commits affected a.Java, thus the current process metrics should look like
		// this:
		ProcessMetrics currentProcessMetrics = new ProcessMetrics(22, 31, 26, 1, 1, null);
		for (int i = 0; i < 21; i++) {
			currentProcessMetrics.updateAuthorCommits("Mauricio");
		}
		currentProcessMetrics.updateAuthorCommits("Jan");
		Assertions.assertEquals(currentProcessMetrics.toString(), pm.getCurrentProcessMetrics().toString());

		// Assertions if the tracker is counting the commits since the last refactoring
		// correct
		Assertions.assertEquals(0, pm.getCommitCounter());
	}

	@Test
	public void authorOwnership() {
		// TODO: What commit message should I use here?
		var cm = new CommitMetaData();
		cm.project = new Project();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", cm);

		for (int i = 0; i < 90; i++) {
			pm.reportCommit("commit", "Mauricio", 10, 20);
		}

		for (int i = 0; i < 6; i++) {
			pm.reportCommit("commit", "Diogo", 10, 20);
		}

		for (int i = 0; i < 4; i++) {
			pm.reportCommit("commit", "Rafael", 10, 20);
		}

		Assertions.assertEquals(3, pm.getCurrentProcessMetrics().qtyOfAuthors(), 0.0001);
		Assertions.assertEquals(100, pm.getCurrentProcessMetrics().qtyOfCommits, 0.0001);
		Assertions.assertEquals(0.90, pm.getCurrentProcessMetrics().authorOwnership(), 0.0001);
		Assertions.assertEquals(2, pm.getCurrentProcessMetrics().qtyMajorAuthors());
		Assertions.assertEquals(1, pm.getCurrentProcessMetrics().qtyMinorAuthors());
		Assertions.assertEquals(0, pm.getCurrentProcessMetrics().bugFixCount);

		// Reset the tracker counter, because a refactoring happened in this commit
		CommitMetaData refactoringCommitMetaData = new CommitMetaData();
		pm.resetCounter(refactoringCommitMetaData);

		pm.reportCommit("bug fix in commit", "Michael", 10, 20);

		Assertions.assertEquals(3, pm.getBaseProcessMetrics().qtyOfAuthors(), 0.0001);
		Assertions.assertEquals(100, pm.getBaseProcessMetrics().qtyOfCommits, 0.0001);
		Assertions.assertEquals(0.90, pm.getBaseProcessMetrics().authorOwnership(), 0.0001);
		Assertions.assertEquals(2, pm.getBaseProcessMetrics().qtyMajorAuthors());
		Assertions.assertEquals(1, pm.getBaseProcessMetrics().qtyMinorAuthors());
		Assertions.assertEquals(0, pm.getBaseProcessMetrics().bugFixCount);

		Assertions.assertEquals(4, pm.getCurrentProcessMetrics().qtyOfAuthors(), 0.0001);
		Assertions.assertEquals(101, pm.getCurrentProcessMetrics().qtyOfCommits, 0.0001);
		Assertions.assertEquals(0.8910891089108911, pm.getCurrentProcessMetrics().authorOwnership(), 0.0001);
		Assertions.assertEquals(2, pm.getCurrentProcessMetrics().qtyMajorAuthors());
		Assertions.assertEquals(2, pm.getCurrentProcessMetrics().qtyMinorAuthors());
		Assertions.assertEquals(1, pm.getCurrentProcessMetrics().bugFixCount);
	}

	@Test
	public void countBugFixes() {
		int qtyKeywords = ProcessMetricTracker.bugKeywords.length;
		Random rnd = new Random();
		var commitMetaData = new CommitMetaData();
		commitMetaData.project = new Project();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		pm.reportCommit("bug fix here", "Rafael", 10, 20);

		int qty = 1;
		for (int i = 0; i < 500; i++) {
			String keywordHere = "";
			if (rnd.nextBoolean()) {
				keywordHere = ProcessMetricTracker.bugKeywords[rnd.nextInt(qtyKeywords - 1)];
				qty++;
			}

			pm.reportCommit("bla bla " + (keywordHere) + "ble ble", "Rafael", 10, 20);
		}

		Assertions.assertEquals(qty, pm.getCurrentProcessMetrics().bugFixCount);
	}

	@Test
	public void countQtyOfCommits() {
		var project = new Project();
		PMDatabase pmDatabase = new PMDatabase();

		for (int i = 0; i < 20; i++) {
			pmDatabase.reportChanges("a.Java", new CommitMetaData("#" + i, "n", "n", "0", project), "R", 1, 1);
		}
		pmDatabase.reportRefactoring("a.Java", new CommitMetaData("#21", "n", "n", "0", project));

		ProcessMetricTracker pmTracker = pmDatabase.find("a.Java");
		Assertions.assertEquals(20, pmTracker.getCurrentProcessMetrics().qtyOfCommits);
		Assertions.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesDeleted);
		Assertions.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesAdded);

		pmTracker = pmDatabase.find("a.Java");
		Assertions.assertEquals(20, pmTracker.getCurrentProcessMetrics().qtyOfCommits);
		Assertions.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesDeleted);
		Assertions.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesAdded);
	}

	@Test
	void calculateStability() {
		var cm = new CommitMetaData();
		cm.project = new Project();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", cm);

		List<Integer> stableCommitCounts = List.of(10, 25);

		for (int i = 0; i < 9; i++) {
			pm.reportCommit("commit #" + i, "Mauricio", 1, 1);
			Assertions.assertFalse(pm.calculateStability(stableCommitCounts));
			Assertions.assertFalse(pm.calculateStability(stableCommitCounts));
		}
		pm.reportCommit("commit #10", "Mauricio", 1, 1);
		Assertions.assertTrue(pm.calculateStability(stableCommitCounts));
		Assertions.assertTrue(pm.calculateStability(stableCommitCounts));

		for (int i = 0; i < 14; i++) {
			pm.reportCommit("commit #" + (i + 10), "Mauricio", 1, 1);
			Assertions.assertFalse(pm.calculateStability(stableCommitCounts));
		}

		pm.reportCommit("commit #25", "Mauricio", 1, 1);
		Assertions.assertTrue(pm.calculateStability(stableCommitCounts));
	}
}
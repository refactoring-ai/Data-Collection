package refactoringml;

import refactoringml.db.CommitMetaData;
import refactoringml.db.ProcessMetrics;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

//Tests the ProcessMetricTracker and ProcessMetrics classes
//Closely linked to PMDatabaseTest
class ProcessMetricTrackerTest {
	//Test if the constructor of the ProcessMetricTracker works as intended
	@Test
	public void constructor(){
		CommitMetaData commitMetaData = new CommitMetaData();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		//check if the the commit meta data is still refering to the same object, we want to reduce the memory usage, as commitmetadata is stable
		Assert.assertFalse(pm.getBaseProcessMetrics() == pm.getCurrentProcessMetrics());
		//Check if the two pm metrics are not accidently refering to the same object, because process metrics are not stable
		Assert.assertTrue(pm.getBaseCommitMetaData() == commitMetaData);

		ProcessMetrics baseProcessMetrics = new ProcessMetrics(0, 0, 0, 0, 0);
		//Assert if the process metrics are "empty" at the beginning of the tracking process
		Assert.assertEquals(baseProcessMetrics.toString(), pm.getBaseProcessMetrics().toString());
		Assert.assertEquals(baseProcessMetrics.toString(), pm.getCurrentProcessMetrics().toString());
	}

	//test if the ProcessMetricTracker reportCommit function behaves as expected
	@Test
	void reportCommit(){
		CommitMetaData commitMetaData = new CommitMetaData();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		for(int i = 0; i < 21; i++) {
			pm.reportCommit("commit #" + i,"Mauricio", 1, 1);
		}
		pm.reportCommit("Bug fix in commit #" + 22, "Jan", 10, 5);

		//no refactoring was done on a.Java, thus the base commit process metrics should be empty
		ProcessMetrics baseProcessMetrics = new ProcessMetrics(0, 0, 0, 0, 0);
		Assert.assertEquals(baseProcessMetrics.toString(), pm.getBaseProcessMetrics().toString());

		//22 commits affected a.Java, thus the current process metrics should look like this:
		ProcessMetrics currentProcessMetrics = new ProcessMetrics(22, 31, 26, 1, 0);
		for(int i = 0; i < 21; i++) {
			currentProcessMetrics.updateAuthorCommits("Mauricio");
		}
		currentProcessMetrics.updateAuthorCommits("Jan");
		Assert.assertEquals(currentProcessMetrics.toString(), pm.getCurrentProcessMetrics().toString());

		//Assert if the tracker is counting the commits since the last refactoring correct
		Assert.assertEquals(22, pm.getCommitCounter());
	}

	//test if the ProcessMetricTracker reset function behaves as expected
	@Test
	void resetClassFile(){
		CommitMetaData commitMetaData = new CommitMetaData();
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", commitMetaData);

		for(int i = 0; i < 21; i++) {
			pm.reportCommit("commit #" + i,"Mauricio", 1, 1);
		}
		pm.reportCommit("Bug fix in commit #" + 22, "Jan", 10, 5);

		//Reset the tracker counter, because a refactoring happened in this commit
		CommitMetaData refactoringCommitMetaData = new CommitMetaData();
		pm.resetCounter(refactoringCommitMetaData);

		//Assert if the new commit metadata is the same object as the one in the tracker after the reset
		Assert.assertTrue(refactoringCommitMetaData == pm.getBaseCommitMetaData());

		//Assert if the current processmetrics were deep copied to the base process metrics
		Assert.assertFalse(pm.getBaseProcessMetrics() == pm.getCurrentProcessMetrics());
		Assert.assertEquals(pm.getCurrentProcessMetrics().toString(), pm.getBaseProcessMetrics().toString());

		//Assert if the values of the deep base process metrics are correct
		//22 commits affected a.Java, thus the current process metrics should look like this:
		ProcessMetrics currentProcessMetrics = new ProcessMetrics(22, 31, 26, 1, 1);
		for(int i = 0; i < 21; i++) {
			currentProcessMetrics.updateAuthorCommits("Mauricio");
		}
		currentProcessMetrics.updateAuthorCommits("Jan");
		Assert.assertEquals(currentProcessMetrics.toString(), pm.getCurrentProcessMetrics().toString());

		//Assert if the tracker is counting the commits since the last refactoring correct
		Assert.assertEquals(0, pm.getCommitCounter());
	}

	@Test
	void authorOwnership() {
		//TODO: What commit message should I use here?
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", new CommitMetaData());

		for(int i = 0; i < 90; i++) {
			pm.reportCommit("commit","Mauricio", 10, 20);
		}

		for(int i = 0; i < 6; i++) {
			pm.reportCommit("commit","Diogo", 10, 20);
		}

		for(int i = 0; i < 4; i++) {
			pm.reportCommit("commit","Rafael", 10, 20);
		}

		Assert.assertEquals(3, pm.getCurrentProcessMetrics().qtyOfAuthors(), 0.0001);
		Assert.assertEquals(100, pm.getCurrentProcessMetrics().qtyOfCommits, 0.0001);
		Assert.assertEquals(0.90, pm.getCurrentProcessMetrics().authorOwnership(), 0.0001);
		Assert.assertEquals(2, pm.getCurrentProcessMetrics().qtyMajorAuthors());
		Assert.assertEquals(1, pm.getCurrentProcessMetrics().qtyMinorAuthors());
		Assert.assertEquals(0, pm.getCurrentProcessMetrics().bugFixCount);

		//Reset the tracker counter, because a refactoring happened in this commit
		CommitMetaData refactoringCommitMetaData = new CommitMetaData();
		pm.resetCounter(refactoringCommitMetaData);

		pm.reportCommit("bug fix in commit","Michael", 10, 20);

		Assert.assertEquals(3, pm.getBaseProcessMetrics().qtyOfAuthors(), 0.0001);
		Assert.assertEquals(100, pm.getBaseProcessMetrics().qtyOfCommits, 0.0001);
		Assert.assertEquals(0.90, pm.getBaseProcessMetrics().authorOwnership(), 0.0001);
		Assert.assertEquals(2, pm.getBaseProcessMetrics().qtyMajorAuthors());
		Assert.assertEquals(1, pm.getBaseProcessMetrics().qtyMinorAuthors());
		Assert.assertEquals(0, pm.getBaseProcessMetrics().bugFixCount);

		Assert.assertEquals(4, pm.getCurrentProcessMetrics().qtyOfAuthors(), 0.0001);
		Assert.assertEquals(101, pm.getCurrentProcessMetrics().qtyOfCommits, 0.0001);
		Assert.assertEquals(0.8910891089108911, pm.getCurrentProcessMetrics().authorOwnership(), 0.0001);
		Assert.assertEquals(2, pm.getCurrentProcessMetrics().qtyMajorAuthors());
		Assert.assertEquals(2, pm.getCurrentProcessMetrics().qtyMinorAuthors());
		Assert.assertEquals(1, pm.getCurrentProcessMetrics().bugFixCount);
	}

	@Test
	void countBugFixes() {
		int qtyKeywords = ProcessMetricTracker.bugKeywords.length;
		Random rnd = new Random();

		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", new CommitMetaData());

		pm.reportCommit( "bug fix here","Rafael", 10, 20);

		int qty = 1;
		for(int i = 0; i < 500; i++) {
			String keywordHere = "";
			if(rnd.nextBoolean()) {
				keywordHere = ProcessMetricTracker.bugKeywords[rnd.nextInt(qtyKeywords - 1)];
				qty++;
			}

			pm.reportCommit("bla bla " + (keywordHere) + "ble ble","Rafael", 10, 20);
		}

		Assert.assertEquals(qty, pm.getCurrentProcessMetrics().bugFixCount);
	}

	@Test
	void countQtyOfCommits(){
		PMDatabase pmDatabase = new PMDatabase();

		for(int i = 0; i < 20; i++) {
			pmDatabase.reportChanges("a.Java", new CommitMetaData("#" + i, "n", "n", "0"), "R", 1, 1);
		}
		pmDatabase.reportRefactoring("a.Java", new CommitMetaData("#21", "n", "n", "0"));

		ProcessMetricTracker pmTracker = pmDatabase.find("a.Java");
		Assert.assertEquals(20, pmTracker.getCurrentProcessMetrics().qtyOfCommits);
		Assert.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesDeleted);
		Assert.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesAdded);

		pmTracker = pmDatabase.find("a.Java");
		Assert.assertEquals(20, pmTracker.getCurrentProcessMetrics().qtyOfCommits);
		Assert.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesDeleted);
		Assert.assertEquals(20, pmTracker.getCurrentProcessMetrics().linesAdded);
	}

	@Test
	void calculateStability(){
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", new CommitMetaData());
		List<Integer> stableCommitCounts = List.of(10, 25);

		for(int i = 0; i < 9; i++) {
			pm.reportCommit("commit #" + i,"Mauricio", 1, 1);
			Assert.assertFalse(pm.calculateStability(stableCommitCounts));
			Assert.assertFalse(pm.calculateStability(stableCommitCounts));
		}
		pm.reportCommit("commit #10","Mauricio", 1, 1);
		Assert.assertTrue(pm.calculateStability(stableCommitCounts));
		Assert.assertTrue(pm.calculateStability(stableCommitCounts));

		for(int i = 0; i < 14; i++) {
			pm.reportCommit("commit #" + (i + 10),"Mauricio", 1, 1);
			Assert.assertFalse(pm.calculateStability(stableCommitCounts));
		}

		pm.reportCommit("commit #25","Mauricio", 1, 1);
		Assert.assertTrue(pm.calculateStability(stableCommitCounts));
	}
}
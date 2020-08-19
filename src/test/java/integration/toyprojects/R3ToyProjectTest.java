package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R3ToyProjectTest extends IntegrationBaseTest {
	@Override
	protected String getStableCommitThreshold() {return List.of(3, 5, 6).toString();}

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r3.git";
	}

	// This test helped to check if refactoring in subclasses are working.
	//Push Up Attribute not working see e3e605f2d76b5e8a4d85ba0d586103834822ea40
	//I tried to create a new class named Cat. Cat and Dog had the same field "region". Then I push it up to AnimalSuper.
	// However the Pull Up Attribute in commit 556cf904bc didnt work.
	// commit 892ffd8486daaedb5c92a548a23b87753393ce16 should show two refactoring -- Rename Class and Push Down Method
	@Test
	void refactorings() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		Assert.assertEquals(17, refactoringCommitList.size());

		assertRefactoring(refactoringCommitList, "074881da657ed0a11527cb8b14bba12e4111c704", "Rename Class", 1);
		assertRefactoring(refactoringCommitList, "d025fed92a7253953a148f7264de28a85bc9af4e", "Rename Method", 2);
		assertRefactoring(refactoringCommitList, "061febd820977f2b00c4926634f09908cc5b8b08", "Rename Parameter", 2);
		assertRefactoring(refactoringCommitList, "376304b51193e5fade802be2cbd7523d6a5ba664", "Move And Rename Class", 1);
		assertRefactoring(refactoringCommitList, "24b55774f386aefdc69f7753132310d53759e2e3", "Move Class", 1);
		assertRefactoring(refactoringCommitList, "4881103961cfac2afb7139c29eb10536b42bc3cd", "Move Class", 1);
		assertRefactoring(refactoringCommitList, "07cae36026215cefeac784c4213b2d46eb63de53", "Rename Parameter", 1);
		assertRefactoring(refactoringCommitList, "8fc1ff2f53a617082767b8dd0af60b978dfc6e67", "Move Class", 1);
		assertRefactoring(refactoringCommitList, "cf2ef5a3de59923ac000a4fe3ceeb8778229b293", "Pull Up Method", 2);
		assertRefactoring(refactoringCommitList, "892ffd8486daaedb5c92a548a23b87753393ce16", "Rename Class", 1);
		assertRefactoring(refactoringCommitList, "892ffd8486daaedb5c92a548a23b87753393ce16", "Move Method", 1);

		for (RefactoringCommit refactoringCommit : refactoringCommitList){
			Assertions.assertFalse(refactoringCommit.getClassMetrics().isInnerClass());
		}
	}

	@Test
	void commitCount(){
		List<Integer> qtyOfCommitsDogList = getRefactoringCommits().stream()
				.filter(refactoring -> refactoring.getClassName().equals("Dog"))
				.map(refactoring -> refactoring.getProcessMetrics().qtyOfCommits)
				.distinct().collect(Collectors.toList());

		//commit counts of the dog class file at the six refactorings we detect
		List<Integer> refactoringCommitCountsDog = List.of(2, 4, 12, 13, 15, 17);
		for (int qtyOfCommits : refactoringCommitCountsDog){
			Assert.assertTrue(qtyOfCommitsDogList.contains(qtyOfCommits));
		}
	}

	//Test if various commits are found correctly, requires the multiple Ks feature to work correct
	@Test
	void stableCommits() {
		//TODO: Why are the stableCommits multiples of three? Classes are always added three times instead of once
		List<StableCommit> stableCommitList = getStableCommits();

		//TODO: reasonable tests here
		String lastRefactoring = "061febd820977f2b00c4926634f09908cc5b8b08";
		List<StableCommit> filteredList = (List<StableCommit>) filterCommit(stableCommitList, lastRefactoring);
		//TODO: Activate to test multipleKs
		//Assert.assertEquals(3, filteredList.size());
		//Assert.assertEquals(3, filteredList.get(0).getCommitThreshold());

		assertMetaDataStable(
				lastRefactoring,
				"@local/repos/toyrepo-r3/" + lastRefactoring,
				5,
				"0e094a734239b1bcc6d6bce1436200c0e45b1e8d",
				"rename");

		//AnimalSuper has 5 commits and Dog has 6 commits
		//TODO: test process metrics
	}

	//Test if all refactorings with multiple Ks are detected correctly.
	@Test
	void stableThresholds() {
		List<StableCommit> highestStabilityThreshold = getStableCommits().stream().filter(commit ->
				commit.getCommitThreshold() >= 50).collect(Collectors.toList());
		Assert.assertEquals(0, highestStabilityThreshold.size());

		//Manually Verified
		List<StableCommit> stableCommitLow = getStableCommits().stream().filter(commit ->
				commit.getCommitThreshold() == 3).collect(Collectors.toList());
		Assert.assertEquals(3, stableCommitLow.size());

		List<StableCommit> stableCommitsMedium = getStableCommits().stream().filter(commit ->
				commit.getCommitThreshold() == 5).collect(Collectors.toList());
		Assert.assertEquals(1, stableCommitsMedium.size());

		List<StableCommit> stableCommitsHigh = getStableCommits().stream().filter(commit ->
				commit.getCommitThreshold() == 6).collect(Collectors.toList());
		Assert.assertEquals(1, stableCommitsHigh.size());

		Assert.assertEquals(5, getStableCommits().size());

		String lastRefactoring = "061febd820977f2b00c4926634f09908cc5b8b08";
		List<StableCommit> filteredList = (List<StableCommit>) filterCommit(getStableCommits(), lastRefactoring);
		Assert.assertEquals(3, filteredList.get(0).getCommitThreshold());
	}

	@Test
	void commitMetaData1(){
		String commit = "376304b51193e5fade802be2cbd7523d6a5ba664";
		assertMetaDataRefactoring(
				commit,
				"Move and Rename Class testing",
				"Move And Rename Class\tAnimal moved and renamed to inheritance.superinfo.AnimalSuper",
				"@local/repos/toyrepo-r3/" + commit,
				6,
				"061febd820977f2b00c4926634f09908cc5b8b08");
	}

	@Test
	void commitMetaData2(){
		String commit = "061febd820977f2b00c4926634f09908cc5b8b08";
		assertMetaDataRefactoring(
				commit,
				"rename",
				"Rename Parameter\tinfo : String to noise : String in method package abstract sound(noise String) : void in class Animal",
				"@local/repos/toyrepo-r3/" + commit,
				5,
				"0e094a734239b1bcc6d6bce1436200c0e45b1e8d");
	}
	@Test
	void projectMetrics() {
		assertProjectMetrics(3, 3, 0, 23, 23, 0);
	}
}
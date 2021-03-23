package refactoringml.integration.realprojects;

import refactoringml.integration.IntegrationBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class ApacheCommonsCliIntegrationTest extends IntegrationBaseTest {
	@Override
	protected String getStableCommitThreshold() {return "10";};

	@Override
	protected String getRepo() {
		return "https://github.com/apache/commons-cli.git";
	}

	@Override
	protected String getLastCommit() {
		return "c12260c05ebcd27558265395df79b8b049534fbd";
	}

	//Test the isInnerClass boolean for both RefactoringCommit and StableCommit .
	@Test
	public void isInnerClassRefactoring(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit -> commit.getClassName().equals("org.apache.commons.cli.HelpFormatter")||
				commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());

		Assertions.assertEquals(26, refactoringCommitList.size());
		List<RefactoringCommit> areInnerClassesInRefactorings = refactoringCommitList.stream().filter(commit ->
				commit.classMetrics.isInnerClass
						&& commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
		List<RefactoringCommit> areNotInnerClassesInRefactorings = refactoringCommitList.stream().filter(commit -> !commit.classMetrics.isInnerClass).collect(Collectors.toList());

		Assertions.assertEquals(1, areInnerClassesInRefactorings.size());
		Assertions.assertEquals(25, areNotInnerClassesInRefactorings.size());
	}

	@Test
	public void isInnerClassStable(){
		List<StableCommit> areInnerClassesInStable = getStableCommits().stream().filter(commit ->
				commit.classMetrics.isInnerClass).collect(Collectors.toList());
		Assertions.assertEquals(65, areInnerClassesInStable.size());

		List<StableCommit> areNotInnerClassesInStable = getStableCommits().stream().filter(commit ->
				!commit.classMetrics.isInnerClass).collect(Collectors.toList());
		Assertions.assertEquals(2195, areNotInnerClassesInStable.size());

		Assertions.assertEquals(2260, getStableCommits().size());
	}

	@Test
	public void isInnerClassStable2(){
		List<StableCommit> areInnerClassesInStableLevel2 = getStableCommits().stream().filter(commit ->
				commit.classMetrics.isInnerClass && commit.level == 2).collect(Collectors.toList());
		Assertions.assertEquals(22, areInnerClassesInStableLevel2.size());

		List<StableCommit> areInnerClassesInStableLevel3 = getStableCommits().stream().filter(commit ->
				commit.classMetrics.isInnerClass && commit.level == 3).collect(Collectors.toList());
		Assertions.assertEquals(27, areInnerClassesInStableLevel3.size());

		List<StableCommit> areNotInnerClassesInStableLevel2 = getStableCommits().stream().filter(commit ->
				!commit.classMetrics.isInnerClass && commit.level == 2).collect(Collectors.toList());
		Assertions.assertEquals(610, areNotInnerClassesInStableLevel2.size());

		List<StableCommit> areNotInnerClassesInStableLevel3 = getStableCommits().stream().filter(commit ->
				!commit.classMetrics.isInnerClass && commit.level == 3).collect(Collectors.toList());
		Assertions.assertEquals(1381, areNotInnerClassesInStableLevel3.size());
	}

	@Test
	public void commitCount(){
		String helpFormatterClass = "org.apache.commons.cli.HelpFormatter";
		String extractMethod = "c7127329dad2c5d6284532da09ddc0fdefd67436";
		RefactoringCommit extractMethodRefactoring = (RefactoringCommit) filterCommit(getRefactoringCommits(), extractMethod).stream()
				.filter(refactoringCommit -> refactoringCommit.getClassName().equals(helpFormatterClass)).collect(Collectors.toList()).get(0);
		Assertions.assertEquals(4, extractMethodRefactoring.processMetrics.qtyOfCommits);

		String maven2Commit = "1596f3bbe57986361da4ac1a23634dd5b00d10df";
		RefactoringCommit maven2Refactoring = (RefactoringCommit) filterCommit(getRefactoringCommits(), maven2Commit).stream()
				.filter(refactoringCommit -> refactoringCommit.getClassName().equals(helpFormatterClass)).collect(Collectors.toList()).get(0);
		Assertions.assertEquals(42, maven2Refactoring.processMetrics.qtyOfCommits);

		String renameVariableCommit = "3936da9d3fe37bcd20dd37216d82608e5917be07";
		RefactoringCommit renameVariablRefactoring = (RefactoringCommit) filterCommit(getRefactoringCommits(), renameVariableCommit).stream()
				.filter(refactoringCommit -> refactoringCommit.getClassName().equals(helpFormatterClass)).collect(Collectors.toList()).get(0);
		Assertions.assertEquals(56, renameVariablRefactoring.processMetrics.qtyOfCommits);
	}

	//Test if the inner classes are tracked and marked correctly, with all details
	//This test requires multipleKs to work
	@Test
	public void isInnerClassDetails1() {
		assertInnerClass(
				getStableCommits(),
				"38ab386d9d86c6cacea817954064bb25fba312aa",
				"org.apache.commons.cli.Option.Builder",
				35);
	}

	@Test
	public void isInnerClassDetails2() {
		//org.apache.commons.cli.HelpFormatter:
		//Refactoring at:
		// 8f8639f6a2606f45c130d7d9b65248248fc431c1
		// in total xx commits till the next refactoring:
		// 1596f3bbe57986361da4ac1a23634dd5b00d10df
		assertInnerClass(
				getStableCommits(),
				"8f95e4a724350f9f80429c2af1c3ac9bb2b2c2db",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				6);
		assertInnerClass(
				getStableCommits(),
				"51f4ee70a4f5a8a921557b2a53413fb19c52b918",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				6);
		assertInnerClass(
				getStableCommits(),
				"3936da9d3fe37bcd20dd37216d82608e5917be07",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				4);
	}

	@Test
	public void isInnerClassDetails3() {
		assertInnerClass(
				getStableCommits(),
				"cd745ecf52fb2fe8fed1c67fc9149e4be11a73f0",
				"org.apache.commons.cli.OptionTest.TestOption",
				7);
		assertInnerClass(
				getStableCommits(),
				"cd745ecf52fb2fe8fed1c67fc9149e4be11a73f0",
				"org.apache.commons.cli.OptionTest.DefaultOption",
				7);
	}

	@Test
	public void commitMetaDataRefactoring(){
		String renameCommit = "04490af06faa8fd1be15da88172beb32218dd336";
		assertMetaDataRefactoring(
				renameCommit,
				"bug #11457: implemented fix, javadoc added to Option\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129803 13f79535-47bb-0310-9956-ffa450edef68",
				"Extract Variable\tkey : String in method package setOpt(opt Option) : void from class org.apache.commons.cli.CommandLine",
				"@local/repos/commons-cli/" + renameCommit,
				37,
				"469e71799a438ccb2d0925e50d4bb9dce37cdba2");

		String moveCommit = "347bbeb8f98a49744501ac50850457ba8751d545";
		assertMetaDataRefactoring(
				moveCommit,
				"refactored the option string handling, added property support for options with an argument value\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129846 13f79535-47bb-0310-9956-ffa450edef68",
				"Rename Parameter\topts : Options to options : Options in method public parse(options Options, arguments String[], stopAtNonOption boolean) : CommandLine in class org.apache.commons.cli.Parser",
				"@local/repos/commons-cli/" + moveCommit,
				76,
				"3b8e3de5b7599a6165d48103f94f3a830361188d");
	}

	@Test
	public void commitMetaDataStable(){
		String stableCommit1 = "aae50c585ec3ac33c6a9af792e80378904a73195";
		assertMetaDataStable(
				stableCommit1,
				"@local/repos/commons-cli/" + stableCommit1,
				2,
				"4868ac5e7c2afd428de74a6dcbec07dc6541a1ea",
				"moved cli over from the sandbox to commons proper\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129767 13f79535-47bb-0310-9956-ffa450edef68");

		String stableCommit2 = "745d1a535c9cf45d24455afc150b808981c8e0df";
		assertMetaDataStable(
				stableCommit2,
				"@local/repos/commons-cli/" + stableCommit2,
				39,
				"dde69934d7f0bee13e4cd1fc99a7d60ce95a0c78",
				"javadoc updates\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129805 13f79535-47bb-0310-9956-ffa450edef68");
	}

	// this test checks the Extract Method that has happened in #269eae18a911f792895d0402f5dd4e7913410523,
	// method getParsedOptionValue
	@Test
	public void refactoringDetected1() {
		RefactoringCommit instance1 = getRefactoringCommits().stream().filter(commit ->
				commit.getCommit().equals("269eae18a911f792895d0402f5dd4e7913410523") &&
						commit.refactoring.equals("Extract Method") &&
						commit.methodMetrics.getFullMethodName().equals("getParsedOptionValue/1[String]")
		).collect(Collectors.toList()).get(0);

		Assertions.assertNotNull(instance1);

		Assertions.assertEquals("getParsedOptionValue/1[String]", instance1.methodMetrics.getFullMethodName());
		Assertions.assertEquals(2, instance1.methodMetrics.methodVariablesQty);
		Assertions.assertEquals(1, instance1.methodMetrics.methodMaxNestedBlocks);
		Assertions.assertEquals(2, instance1.methodMetrics.methodReturnQty);
		Assertions.assertEquals(0, instance1.methodMetrics.methodTryCatchQty);
	}

	// this test follows the src/java/org/apache/commons/cli/Option.java file
	// This test helped us to understand that we should not delete
	// RefactoringCommit where variableAppearances = -1, as this happens in newly introduced variables.
	@Test
	public void refactoringOptionClass() {
		// it has been through 9 different refactorings
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit ->
				commit.filePath.equals("src/java/org/apache/commons/cli/Option.java")).collect(Collectors.toList());

		Assertions.assertEquals(22, refactoringCommitList.size());
		assertRefactoring(refactoringCommitList, "04490af06faa8fd1be15da88172beb32218dd336", "Extract Variable", 1);
		assertRefactoring(refactoringCommitList, "347bbeb8f98a49744501ac50850457ba8751d545", "Extract Class", 1);
		assertRefactoring(refactoringCommitList, "347bbeb8f98a49744501ac50850457ba8751d545", "Move Method", 3);
		assertRefactoring(refactoringCommitList, "5470bcaa9d75d73fb9c687fa13e12d642c75984f", "Extract Method", 2);
		assertRefactoring(refactoringCommitList, "97744806d59820b096fb502b1d51ca54b5d0921d", "Rename Method", 1);
		assertRefactoring(refactoringCommitList, "bfe6bd8634895645aa71d6a6dc668545297d7413", "Rename Parameter", 1);
	}

	@Test
	public void stableOptionClass(){
		List<StableCommit> stableCommitList = getStableCommits().stream().filter(commit ->
				commit.filePath.equals("src/java/org/apache/commons/cli/Option.java")).collect(Collectors.toList());

		assertStableCommit(stableCommitList, "5470bcaa9d75d73fb9c687fa13e12d642c75984f");
	}

	@Test
	public void stableCommitsClasses(){
		List<String> noListUnique = getStableCommits().stream().map(instance -> instance.getClassName()).distinct().collect(Collectors.toList());
		Assertions.assertEquals(43, noListUnique.size());
	}

	// test if test files are marked as tests, and production files are not
	@Test
	public void isTest() {
		// it has been through 9 different refactorings
		List<String> refactoringCommitsTests = getRefactoringCommits().stream().filter(refactoring -> refactoring.isTest).map(
				refactoringCommit -> refactoringCommit.getClassName()).distinct().collect(Collectors.toList());
		List<String> refactoringCommitsNoTests = getRefactoringCommits().stream().filter(refactoring -> !refactoring.isTest).map(
				refactoringCommit -> refactoringCommit.getClassName()).distinct().collect(Collectors.toList());
		Assertions.assertEquals(47, refactoringCommitsTests.size());
		Assertions.assertEquals(42, refactoringCommitsNoTests.size());

		List<String> stableCommitListTests =  getStableCommits().stream().filter(stable -> stable.isTest).map(
				refactoringCommit -> refactoringCommit.getClassName()).distinct().collect(Collectors.toList());
		List<String> stableCommitListNoTests =  getStableCommits().stream().filter(stable -> !stable.isTest).map(
				refactoringCommit -> refactoringCommit.getClassName()).distinct().collect(Collectors.toList());
		Assertions.assertEquals(16, stableCommitListTests.size());
		Assertions.assertEquals(27, stableCommitListNoTests.size());
	}

	@Test
	public void projectMetrics() {
		assertProjectMetrics(52, 23, 29, 7078, 2793, 4285);
	}
}
package refactoringml;

import static refactoringml.util.FileUtils.isTestFile;

import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import refactoringml.util.FilePathUtils;
import refactoringml.util.FileUtils;

public class UtilsTest {
	@Test
	public void classFromFileName() {
		Assertions.assertEquals("File", FilePathUtils.classFromFileName("/some/dir/File.java"));
		Assertions.assertEquals("File", FilePathUtils.classFromFileName("c:\\some\\dir\\File.java"));
		Assertions.assertEquals("File", FilePathUtils.classFromFileName("/File.java"));
	}

	@Test
	public void classFromFullName() {
		Assertions.assertEquals("File", FilePathUtils.classFromFullName(".some.pack.File"));
		Assertions.assertEquals("File", FilePathUtils.classFromFullName("File"));
	}

	@Test
	public void isJavaFile(){
		for(int i = 0; i < 100; i++){
			String randomString = generateRandomString();

			Assertions.assertTrue(FileUtils.isJavaFile(randomString + ".Java"));
			Assertions.assertTrue(FileUtils.isJavaFile(randomString + ".java"));

			Assertions.assertFalse(FileUtils.isJavaFile(randomString + "Java"));
			Assertions.assertFalse(FileUtils.isJavaFile(randomString + ".Jav"));
			Assertions.assertFalse(FileUtils.isJavaFile(randomString + ".Java.a"));
			Assertions.assertFalse(FileUtils.isJavaFile(randomString + "-Java"));
		}
	}

	@Test
	public void isTest(){
		for(int i = 0; i < 100; i++){
			String randomString = generateRandomString();

			Assertions.assertTrue(isTestFile("Test" + randomString + ".java"));
			Assertions.assertTrue(isTestFile(randomString + "/Test" + randomString + ".java"));
			//assert ant test conventions
			Assertions.assertTrue(isTestFile(randomString + "test.java"));
			Assertions.assertTrue(isTestFile(randomString + "Test.java"));

			Assertions.assertFalse(isTestFile(randomString + "*est.java"));
			Assertions.assertFalse(isTestFile(randomString + "Test.jav"));

			//assert maven and gradle test conventions
			Assertions.assertTrue(isTestFile("/test/" + randomString + ".java"));

			Assertions.assertFalse(isTestFile("/test/" + randomString + ".jav"));
			Assertions.assertFalse(isTestFile("src/" + randomString + ".java"));
		}
	}

	@Test
	public void onlyFileName() {
		Assertions.assertEquals("File.java", FilePathUtils.fileNameOnly("/some/dir/File.java"));
		Assertions.assertEquals("File.java", FilePathUtils.fileNameOnly("File.java"));
		Assertions.assertEquals("File.java", FilePathUtils.fileNameOnly("/File.java"));
	}


	private String generateRandomString(){
		Random rnd = new Random();

		int length = rnd.ints(0, 10).findFirst().getAsInt();
		boolean useLetters = rnd.nextBoolean();
		boolean useNumbers = rnd.nextBoolean();
		return RandomStringUtils.random(length, useLetters, useNumbers);
	}
}
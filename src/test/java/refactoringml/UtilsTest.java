package refactoringml;

import static refactoringml.util.FileUtils.isTestFile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import refactoringml.util.FilePathUtils;
import refactoringml.util.FileUtils;

class UtilsTest {
	@Test
	void classFromFileName() {
		Assertions.assertEquals("File", FilePathUtils.classFromFileName("/some/dir/File.java"));
		Assertions.assertEquals("File", FilePathUtils.classFromFileName("c:\\some\\dir\\File.java"));
		Assertions.assertEquals("File", FilePathUtils.classFromFileName("/File.java"));
	}

	@Test
	void classFromFullName() {
		Assertions.assertEquals("File", FilePathUtils.classFromFullName(".some.pack.File"));
		Assertions.assertEquals("File", FilePathUtils.classFromFullName("File"));
	}

	@Test
	void isJavaFile() {
		String fileName = "foobar";

		Assertions.assertTrue(FileUtils.isJavaFile(fileName + ".Java"));
		Assertions.assertTrue(FileUtils.isJavaFile(fileName + ".java"));

		Assertions.assertFalse(FileUtils.isJavaFile(fileName + "Java"));
		Assertions.assertFalse(FileUtils.isJavaFile(fileName + ".Jav"));
		Assertions.assertFalse(FileUtils.isJavaFile(fileName + ".Java.a"));
		Assertions.assertFalse(FileUtils.isJavaFile(fileName + "-Java"));

	}

	@Test
	void isTest() {
		String fileName = "foobar";

		Assertions.assertTrue(isTestFile("Test" + fileName + ".java"));
		Assertions.assertTrue(isTestFile(fileName + "/Test" + fileName + ".java"));
		// assert ant test conventions
		Assertions.assertTrue(isTestFile(fileName + "test.java"));
		Assertions.assertTrue(isTestFile(fileName + "Test.java"));

		Assertions.assertFalse(isTestFile(fileName + "*est.java"));
		Assertions.assertFalse(isTestFile(fileName + "Test.jav"));

		// assert maven and gradle test conventions
		Assertions.assertTrue(isTestFile("/test/" + fileName + ".java"));

		Assertions.assertFalse(isTestFile("/test/" + fileName + ".jav"));
		Assertions.assertFalse(isTestFile("src/" + fileName + ".java"));

	}

	@Test
	void onlyFileName() {
		Assertions.assertEquals("File.java", FilePathUtils.fileNameOnly("/some/dir/File.java"));
		Assertions.assertEquals("File.java", FilePathUtils.fileNameOnly("File.java"));
		Assertions.assertEquals("File.java", FilePathUtils.fileNameOnly("/File.java"));
	}
}
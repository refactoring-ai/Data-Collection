package refactoringml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import refactoringml.util.JGitUtils;

public class JGitUtilsTest {

	@Test
	public void extractProjectNameFromHttpUrl() {
		Assertions.assertEquals("commons-collections", JGitUtils.extractProjectNameFromGitUrl("https://www.github.com/apache/commons-collections.git"));
	}

	@Test
	public void extractProjectNameFromGitSSHUrl() {
		Assertions.assertEquals("commons-collections", JGitUtils.extractProjectNameFromGitUrl("git@github.com:apache/commons-collections.git"));
	}
}

package refactoringml;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import refactoringml.util.JGitUtils;

class JGitUtilsTest {

	@Test
	void extractProjectNameFromHttpUrl() {
		Assert.assertEquals("commons-collections", JGitUtils.extractProjectNameFromGitUrl("https://www.github.com/apache/commons-collections.git"));
	}

	@Test
	void extractProjectNameFromGitSSHUrl() {
		Assert.assertEquals("commons-collections", JGitUtils.extractProjectNameFromGitUrl("git@github.com:apache/commons-collections.git"));
	}
}

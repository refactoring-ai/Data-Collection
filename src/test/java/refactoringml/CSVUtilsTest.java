package refactoringml;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import refactoringml.util.CSVUtils;

class CSVUtilsTest {

	@Test
	void escape() {
		Assert.assertEquals("\"method[a,b,c,d]\"", CSVUtils.escape("method[a,b,c,d]"));
		Assert.assertEquals("\"a,b,c,d,\"\"e\"\"\"", CSVUtils.escape("a,b,c,d,\"e\""));
	}
}

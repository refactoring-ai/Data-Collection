package refactoringml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import refactoringml.util.CSVUtils;

public class CSVUtilsTest {

	@Test
	public void escape() {
		Assertions.assertEquals("\"method[a,b,c,d]\"", CSVUtils.escape("method[a,b,c,d]"));
		Assertions.assertEquals("\"a,b,c,d,\"\"e\"\"\"", CSVUtils.escape("a,b,c,d,\"e\""));
	}
}

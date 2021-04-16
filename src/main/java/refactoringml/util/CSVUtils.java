package refactoringml.util;

public class CSVUtils {

	private CSVUtils() {
		
	}

	public static String escape(String toEscape) {
		return "\"" + toEscape.replace("\"", "\"\"") + "\"";
	}

}

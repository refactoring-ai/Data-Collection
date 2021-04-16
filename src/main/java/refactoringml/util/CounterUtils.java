package refactoringml.util;

import static refactoringml.util.FileUtils.isTestFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

// TODO Add constructor to CK LOC calculator and call the ck version.
public class CounterUtils {

	private static final Logger log = Logger.getLogger(CounterUtils.class);

	/**
	 *
	 * @param line
	 * @return This method returns true if there is any valid source code in the
	 *         given input line. It does not worry if comment has begun or not. This
	 *         method will work only if we are sure that comment has not already
	 *         begun previously. Hence, this method should be called only after
	 *         {@link #commentBegan(String)} is called
	 */
	private static boolean isSourceCodeLine(String line) {
		boolean isSourceCodeLine = false;
		line = line.trim();
		if ("".equals(line) || line.startsWith("//")) {
			return isSourceCodeLine;
		}
		if (line.length() == 1) {
			return true;
		}
		int index = line.indexOf("/*");
		if (index != 0) {
			return true;
		} else {
			while (line.length() > 0) {
				line = line.substring(index + 2);
				int endCommentPosition = line.indexOf("*/");
				if (endCommentPosition < 0) {
					return false;
				}
				if (endCommentPosition == line.length() - 2) {
					return false;
				} else {
					String subString = line.substring(endCommentPosition + 2).trim();
					if ("".equals(subString) || subString.indexOf("//") == 0) {
						return false;
					} else {
						if (subString.startsWith("/*")) {
							line = subString;
							continue;
						}
						return true;
					}
				}

			}
		}
		return isSourceCodeLine;
	}

	/**
	 *
	 * @param line
	 * @return This method checks if in the given line a comment has begun and has
	 *         not ended
	 */
	private static boolean commentBegan(String line) {
		// If line = /* */, this method will return false
		// If line = /* */ /*, this method will return true
		int index = line.indexOf("/*");
		if (index < 0) {
			return false;
		}
		int quoteStartIndex = line.indexOf("\"");
		if (quoteStartIndex != -1 && quoteStartIndex < index) {
			while (quoteStartIndex > -1) {
				line = line.substring(quoteStartIndex + 1);
				int quoteEndIndex = line.indexOf("\"");
				line = line.substring(quoteEndIndex + 1);
				quoteStartIndex = line.indexOf("\"");
			}
			return commentBegan(line);
		}
		return !commentEnded(line.substring(index + 2));
	}

	/**
	 *
	 * @param line
	 * @return This method checks if in the given line a comment has ended and no
	 *         new comment has not begun
	 */
	private static boolean commentEnded(String line) {
		// If line = */ /* , this method will return false
		// If line = */ /* */, this method will return true
		int index = line.indexOf("*/");
		if (index < 0) {
			return false;
		} else {
			String subString = line.substring(index + 2).trim();
			if ("".equals(subString) || subString.startsWith("//")) {
				return true;
			}
			if (commentBegan(subString)) {
				return false;
			} else {
				return true;
			}
		}
	}

	private static int getNumberOfLines(BufferedReader bReader) throws IOException {
		int count = 0;
		boolean commentBegan = false;
		String line = null;

		while ((line = bReader.readLine()) != null) {
			line = line.trim();
			if ("".equals(line) || line.startsWith("//")) {
				continue;
			}
			if (commentBegan) {
				if (commentEnded(line)) {
					line = line.substring(line.indexOf("*/") + 2).trim();
					commentBegan = false;
					if ("".equals(line) || line.startsWith("//")) {
						continue;
					}
				} else
					continue;
			}
			if (isSourceCodeLine(line)) {
				count++;
			}
			if (commentBegan(line)) {
				commentBegan = true;
			}
		}
		return count;
	}

	public static int calculate(String sourceCode) {
		try {
			InputStream is = IOUtils.toInputStream(sourceCode);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			return getNumberOfLines(reader);
		} catch (IOException e) {
			log.error("Error when counting lines", e);
			return 0;
		}
	}

	private CounterUtils() {

	}

	public static CounterResult countProductionAndTestFiles(Path srcPath) {
		var res = new CounterResult();
		String[] allFiles = FileUtils.getAllJavaFiles(srcPath);

		List<String> productionFiles = Arrays.stream(allFiles).filter(path -> !isTestFile(path))
				.collect(Collectors.toList());
		List<String> testFiles = Arrays.stream(allFiles).filter(path -> isTestFile(path)).collect(Collectors.toList());

		Long productionLoc = productionFiles.stream().map(path -> countLines(Paths.get(path), res)).reduce(0L,
				(a, b) -> a + b);
		Long testLoc = testFiles.stream().map(path -> countLines(Paths.get(path), res)).reduce(0L, (a, b) -> a + b);
		res.setCounterResultProperties(productionFiles.size(), testFiles.size(), productionLoc, testLoc);
		return res;
	}

	private static long countLines(Path filePath, CounterResult counterResult) {
		try {
			var contents = Files.readString(filePath);
			return (long) calculate(contents);
		} catch (MalformedInputException mie) {
			counterResult.incrementExceptionsCount();
			log.warnf(mie, "File with path %s has unmappable sequence", filePath);
			return 0;
		} catch (Exception e) {
			counterResult.incrementExceptionsCount();
			throw new RuntimeException(e);
		}
	}

	public static class CounterResult {
		private long qtyOfProductionFiles;
		private long qtyOfTestFiles;

		private long locProductionFiles;
		private long locTestFiles;
		private int exceptionsCount;

		public CounterResult() {

		}

		public void setCounterResultProperties(long qtyOfProductionFiles, long qtyOfTestFiles, long locProductionFiles,
				long locTestFiles) {
			this.qtyOfProductionFiles = qtyOfProductionFiles;
			this.qtyOfTestFiles = qtyOfTestFiles;
			this.locProductionFiles = locProductionFiles;
			this.locTestFiles = locTestFiles;
		}

		public long getQtyOfProductionFiles() {
			return qtyOfProductionFiles;
		}

		public long getQtyOfTestFiles() {
			return qtyOfTestFiles;
		}

		public long getLocProductionFiles() {
			return locProductionFiles;
		}

		public long getLocTestFiles() {
			return locTestFiles;
		}

		/**
		 * @return the exceptionsCount
		 */
		public int getExceptionsCount() {
			return exceptionsCount;
		}

		public void incrementExceptionsCount() {
			exceptionsCount++;
		}

	}
}
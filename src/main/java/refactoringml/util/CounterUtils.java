package refactoringml.util;

import static refactoringml.util.FileUtils.isTestFile;

import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.mauricioaniche.ck.util.LOCCalculator;

import org.jboss.logging.Logger;

public class CounterUtils {

	private static final Logger log = Logger.getLogger(CounterUtils.class);

	private CounterUtils() {

	}

	public static CounterResult countProductionAndTestFiles(Path srcPath) {
		var res = new CounterResult();
		String[] allFiles = FileUtils.getAllJavaFiles(srcPath);

		List<String> productionFiles = Arrays.stream(allFiles).filter(path -> !isTestFile(path))
				.collect(Collectors.toList());
		List<String> testFiles = Arrays.stream(allFiles).filter(path -> isTestFile(path)).collect(Collectors.toList());

		Long productionLoc = productionFiles.stream().map(path -> countLines(Paths.get(path),res)).reduce(0L,
				(a, b) -> a + b);
		Long testLoc = testFiles.stream().map(path -> countLines(Paths.get(path),res)).reduce(0L, (a, b) -> a + b);
		res.setCounterResultProperties(productionFiles.size(), testFiles.size(), productionLoc, testLoc);
		return res;
	}

	private static long countLines(Path filePath, CounterResult counterResult) {
		try {
			var contents = Files.readString(filePath);
			return (long) LOCCalculator.calculate(contents);
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
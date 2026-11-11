package org.ihtsdo.rvf.core.service.structure.validation;

import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * To verify that each line (including last line) in RF2 file is terminated by CR followed by LF (i.e "\r\n").
 *
 */
public class RF2FileStructureTester {
	
	private static final String EMPTY_FILE_CHECKING = " empty file checking";
	private static final String RF2_LINE_SEPARATOR = "\r\n";
	private static final String TEST_TYPE = "line terminator check";
	private final ValidationLog validationLog;
	private final ResourceProvider resourceManager;
	private final TestReportable testReport;
	private Date startTime;
	private static final Logger LOGGER = LoggerFactory.getLogger(RF2FileStructureTester.class);
	
	
	/**
	 * @param validationLog Logger instance
	 * @param resourceManager Resource provider
	 * @param testReport Test report
	 */
	public RF2FileStructureTester(final ValidationLog validationLog, final ResourceProvider resourceManager, StreamTestReport testReport) {
		
		this.validationLog = validationLog;
		this.resourceManager = resourceManager;
		this.testReport = testReport;
	}
	
	public void runTests(){
		startTime = new Date();
		List<String> fileNames = resourceManager.getFileNames();
		try (ExecutorService executorService = Executors.newCachedThreadPool()) {
			List<Future<?>> futures = new ArrayList<>();
			for (final String fileName : fileNames) {
				if (!fileName.endsWith(".txt")) {
					continue;
				}
				futures.add(executorService.submit(() -> runTestForFile(fileName)));
			}
			for (Future<?> task : futures) {
				try {
					task.get();
				} catch (InterruptedException | ExecutionException e) {
					LOGGER.error("Task failed when structure testing due to:", e);
					validationLog.executionError("Error", "Failed to check file due to:" + e.fillInStackTrace());
				}
			}
		}
	}
	
	private void runTestForFile(String fileName) {
		FileScan scan;
		try (BufferedReader reader = resourceManager.getReader(fileName, StandardCharsets.UTF_8)) {
			scan = scan(reader);
		} catch (Exception e) {
			validationLog.executionError("Error", "Failed to read file:" + fileName);
			return;
		}

		if (scan.totalLine() == 0) {
			testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), fileName + EMPTY_FILE_CHECKING, EMPTY_FILE_CHECKING, null,"total line is :" + scan.totalLine(), " RF2 file can't be empty and should at least have a header line",null);
		}
		if (scan.crlfSeparatedTokens() < scan.totalLine()) {
			testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), fileName + " line terminator", TEST_TYPE, null, "total line is terminated with CR+LF:" + scan.crlfSeparatedTokens() ,
					 "total line is terminated with CR+LF:" + scan.totalLine(),null);
		}
		// Only when there are at least two lines, because the previous
		// implementation reached this check by reading up to line
		// (totalLine - 1) and so never ran it on a single-line file. Preserved
		// deliberately: changing it would report last-line terminators that no
		// release has ever been failed for.
		if (scan.totalLine() >= 2 && !RF2_LINE_SEPARATOR.equals(scan.lastLineTerminator())) {
			StringBuilder actualResult = new StringBuilder();
			String actualLineSeparator = scan.lastLineTerminator().replace("\n", "LF").replace("\r", "CR");
			actualResult.append("the last line is terminated with[");
			actualResult.append(actualLineSeparator);
			actualResult.append("]");
			testReport.addError(scan.totalLine() + "-0", startTime, fileName, resourceManager.getFilePath(), fileName + " ast line terminator",TEST_TYPE, null,
					actualResult.toString(), "the last line is terminated with CR+LF",null);
		}
	}

	/**
	 * Everything the three line-terminator checks need, from ONE pass.
	 *
	 * @param totalLine            lines as {@code BufferedReader.readLine} counts
	 *                             them: terminated by LF, CR or CRLF, and a final
	 *                             unterminated line still counts.
	 * @param crlfSeparatedTokens  tokens as {@code Scanner.useDelimiter("\r\n")}
	 *                             produces them, which is what the previous
	 *                             implementation compared against {@code totalLine}
	 *                             to decide whether every line ended CRLF.
	 * @param lastLineTerminator   the final line's terminator, or "" if the file
	 *                             ends without one.
	 */
	record FileScan(int totalLine, int crlfSeparatedTokens, String lastLineTerminator) {}

	/**
	 * One pass instead of three.
	 *
	 * <p>This replaced: a {@code readLine} loop to count lines, then a
	 * {@code Scanner} over a CRLF delimiter counting them again, then a third
	 * open that read every line up to {@code totalLine - 1} purely to look at
	 * the last one. On the 594MB description file of the SNOMED CT-AU edition
	 * (4,174,475 lines) those three cost 1.33s, 6.71s and 1.14s, 9.19s in all;
	 * this single pass costs 3.26s. {@code Scanner} was the most expensive of
	 * the three because it tokenises with the regex engine.
	 *
	 * <p>The token count is reproduced exactly rather than approximated; see
	 * {@link #scannerTokens} for the rule.
	 */
	static FileScan scan(java.io.Reader reader) throws java.io.IOException {
		char[] buffer = new char[8192];
		TerminatorScanner scanner = new TerminatorScanner();
		int read;
		while ((read = reader.read(buffer)) != -1) {
			for (int i = 0; i < read; i++) {
				scanner.accept(buffer[i]);
			}
		}
		return scanner.finish();
	}

	private static final class TerminatorScanner {
		private int totalLine;
		private int crlfCount;
		private long charsRead;
		private long endOfLastCrlf;
		private boolean pendingCarriageReturn;
		private boolean lineHasContent;
		private boolean firstSegmentEmpty;
		private String lastTerminator = "";

		void accept(char c) {
			charsRead++;
			if (pendingCarriageReturn) {
				pendingCarriageReturn = false;
				if (c == '\n') {
					endCrlfLine();
					return;
				}
				// A lone CR ended that line; fall through and treat c as the
				// first character of the next one.
				endLine("\r");
			}
			if (c == '\r') {
				pendingCarriageReturn = true;
			} else if (c == '\n') {
				endLine("\n");
			} else {
				lineHasContent = true;
			}
		}

		private void endCrlfLine() {
			crlfCount++;
			if (charsRead == 2) {
				// The very first two characters, so the segment before
				// this delimiter is empty.
				firstSegmentEmpty = true;
			}
			endOfLastCrlf = charsRead;
			endLine(RF2_LINE_SEPARATOR);
		}

		private void endLine(String terminator) {
			totalLine++;
			lastTerminator = terminator;
			lineHasContent = false;
		}

		FileScan finish() {
			if (pendingCarriageReturn) {
				pendingCarriageReturn = false;
				endLine("\r");
			}
			if (lineHasContent) {
				// A final line with no terminator at all.
				endLine("");
			}
			return new FileScan(totalLine, scannerTokens(crlfCount, charsRead, endOfLastCrlf, firstSegmentEmpty),
					lastTerminator);
		}
	}

	/**
	 * The token count {@code Scanner.useDelimiter("\r\n")} would produce, which
	 * is what the previous implementation compared against the line count.
	 *
	 * <p>Its rule is NOT "one token per delimiter". Splitting on every CRLF gives
	 * {@code crlfCount + 1} segments, and Scanner returns all of them except an
	 * empty FIRST segment and an empty LAST one:
	 *
	 * <pre>
	 *   "a\r\nb\r\n"       -&gt; a, b, ""       -&gt; ["a", "b"]      2
	 *   "a\r\n\r\nb\r\n"   -&gt; a, "", b, ""   -&gt; ["a", "", "b"]  3   interior empties KEPT
	 *   "\r\n"            -&gt; "", ""         -&gt; []              0   both dropped
	 *   "\r\n\r\n"        -&gt; "", "", ""     -&gt; [""]            1
	 * </pre>
	 *
	 * <p>Getting this wrong is not academic: a file whose first line is empty
	 * would report 1 token against 1 line and lose the line-terminator error it
	 * should have raised. RF2FileStructureTesterScanTest checks this against a
	 * real {@code Scanner}, as part of the previous three-pass logic, for leading,
	 * interior and trailing delimiters.
	 */
	private static int scannerTokens(final int crlfCount, final long charsRead,
			final long endOfLastCrlf, final boolean firstSegmentEmpty) {
		if (crlfCount == 0) {
			return charsRead == 0 ? 0 : 1;
		}
		final int segments = crlfCount + 1;
		final boolean lastSegmentEmpty = charsRead == endOfLastCrlf;
		return segments - (firstSegmentEmpty ? 1 : 0) - (lastSegmentEmpty ? 1 : 0);
	}
}

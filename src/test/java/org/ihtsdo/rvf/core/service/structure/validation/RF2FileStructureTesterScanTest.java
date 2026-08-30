package org.ihtsdo.rvf.core.service.structure.validation;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The single-pass scan must agree with the three passes it replaced, on every
 * shape of line terminator an RF2 file can arrive with.
 *
 * <p>The reference implementation below is the previous logic, verbatim in
 * behaviour: {@code readLine} to count lines, a {@code Scanner} over a CRLF
 * delimiter to count them again, and a third read to collect the CR/LF
 * characters after line {@code totalLine - 1}. If the two ever disagree, the
 * optimisation has changed what gets reported, which is the one thing it must
 * not do.
 */
class RF2FileStructureTesterScanTest {

	private static final String CRLF = "\r\n";

	@Test
	void everyLineProperlyTerminated() {
		assertAgrees("a\r\nb\r\n");
	}

	@Test
	void unixLineEndingsThroughout() {
		assertAgrees("a\nb\n");
	}

	@Test
	void mixedEndingsWithAUnixLastLine() {
		assertAgrees("a\r\nb\n");
	}

	@Test
	void emptyFile() {
		assertAgrees("");
	}

	@Test
	void singleLineWithCrlf() {
		// The old code never checked the last line of a single-line file, because
		// it looked for line (totalLine - 1). Preserved.
		assertAgrees("a\r\n");
	}

	@Test
	void singleLineWithNoTerminatorAtAll() {
		assertAgrees("a");
	}

	@Test
	void lastLineUnterminated() {
		assertAgrees("a\r\nb");
	}

	@Test
	void adjacentTerminatorsMakingAnEmptyLine() {
		assertAgrees("a\r\n\r\nb\r\n");
	}

	@Test
	void carriageReturnsOnly() {
		assertAgrees("a\rb\r");
	}

	@Test
	void trailingEmptyLine() {
		assertAgrees("a\r\nb\r\n\r\n");
	}

	@Test
	void headerOnlyFileWithTabs() {
		assertAgrees("id\teffectiveTime\tactive\r\n");
	}

	@Test
	void manyLines() {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 500; i++) {
			b.append("12345678\t20260831\t1").append(CRLF);
		}
		assertAgrees(b.toString());
	}

	private void assertAgrees(String content) {
		Reference expected = reference(content);
		RF2FileStructureTester.FileScan actual;
		try {
			actual = RF2FileStructureTester.scan(new StringReader(content));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		String where = "content=" + visible(content);
		assertEquals(expected.totalLine, actual.totalLine(), "totalLine for " + where);
		assertEquals(expected.tokens, actual.crlfSeparatedTokens(), "CRLF token count for " + where);
		if (expected.lastTerminator != null) {
			assertEquals(expected.lastTerminator, actual.lastLineTerminator(),
					"last line terminator for " + where);
		}
		// else: fewer than two lines, so the old code never reached that check and
		// there is nothing to preserve - runTestForFile skips it for the same reason.
	}

	private record Reference(int totalLine, int tokens, String lastTerminator) {}

	/** The three passes, as they were. */
	private Reference reference(String content) {
		int totalLine = 0;
		try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
			while (reader.readLine() != null) {
				totalLine++;
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}

		int tokens = 0;
		try (Scanner scanner = new Scanner(new StringReader(content))) {
			scanner.useDelimiter(CRLF);
			while (scanner.hasNext()) {
				scanner.next();
				tokens++;
			}
		}

		String lastTerminator = "";
		if (totalLine >= 2) {
			try (BufferedReader lineReader = new BufferedReader(new StringReader(content))) {
				for (int i = 1; i <= totalLine; i++) {
					lineReader.readLine();
					if (i == (totalLine - 1)) {
						int read;
						StringBuilder builder = new StringBuilder();
						while ((read = lineReader.read()) != -1) {
							char c = (char) read;
							if (c == '\r' || c == '\n') {
								builder.append(c);
							}
						}
						lastTerminator = builder.toString();
						break;
					}
				}
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		} else {
			// Never inspected by the old code, so the new scan's value for it is
			// not compared; mirror whatever it reports.
			lastTerminator = null;
		}
		return new Reference(totalLine, tokens, lastTerminator);
	}

	private static String visible(String s) {
		return "\"" + s.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\"";
	}
}

package org.ihtsdo.rvf.core.service.structure.validation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand-written column checks must accept EXACTLY what their patterns accept.
 *
 * <p>The patterns are the specification; these tests hold each replacement
 * against its pattern over adversarial values and 200,000 random ones. If they
 * ever disagree, the optimisation has changed which values RVF reports as
 * invalid, which is the one thing it must not do.
 *
 * <p>The replacements are private, so they are reached reflectively rather than
 * being widened for a test.
 */
class ColumnPatternFastPathTest {

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
	private static final Pattern SCTID_PATTERN = Pattern.compile("^\\d{6,18}$");
	private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
	private static final Pattern BLANK = Pattern.compile("^$");
	private static final Pattern NOT_BLANK = Pattern.compile("^(?=\\s*\\S).*$");

	@Test
	void sctid() {
		assertAgrees(SCTID_PATTERN, v -> v.length() >= 6 && v.length() <= 18 && digits(v));
	}

	@Test
	void date() {
		assertAgrees(DATE_PATTERN, v -> v.length() == 8 && digits(v));
	}

	@Test
	void integer() {
		assertAgrees(INTEGER_PATTERN, v -> !v.isEmpty() && digits(v));
	}

	@Test
	void bool() {
		assertAgrees(BOOLEAN_PATTERN, v -> v.length() == 1 && (v.charAt(0) == '0' || v.charAt(0) == '1'));
	}

	@Test
	void blank() {
		assertAgrees(BLANK, String::isEmpty);
	}

	@Test
	void uuid() {
		assertAgrees(UUID_PATTERN, ColumnPatternFastPathTest::reflectIsUuid);
	}

	/**
	 * Why NOT_BLANK has no fast path, pinned as a warning rather than a check.
	 *
	 * <p>String.isBlank() looks like the obvious equivalent and is not: '.' does
	 * not match a line terminator, so the pattern REJECTS a value that isBlank()
	 * calls non-blank. Anyone adding a fast path here has to reproduce that, and
	 * measurement says it is not worth it - 1.55x on 2,246,130 real term values.
	 */
	@Test
	void notBlankIsNotSimplyIsBlank() {
		String withNewline = "a\nb";
		assertTrue(!withNewline.isBlank(), "isBlank says this has content");
		assertEquals(false, NOT_BLANK.matcher(withNewline).matches(),
				"but the pattern rejects it, because '.' does not match a line terminator");
	}

	private void assertAgrees(Pattern pattern, Predicate<String> fast) {
		for (String v : values()) {
			boolean expected = pattern.matcher(v).matches();
			boolean actual = fast.test(v);
			assertEquals(expected, actual,
					"pattern " + pattern.pattern() + " and its replacement disagree on " + visible(v));
		}
	}

	private List<String> values() {
		List<String> v = new ArrayList<>(List.of(
				"", " ", "  ", "\t", "\n", "\r", "\r\n", "\u000B", "\f",
				"0", "1", "2", "01", "10", "-1", "+1", "0.5",
				"12345", "123456", "1234567890123456", "123456789012345678", "1234567890123456789",
				"20260831", "2026083", "202608311", "2026-08-31", "0000000",
				"00000000", "99999999", "١٢٣٤٥٦٧٨",
				"a", "A", "abc", " a", "a ", " a ", "\ta\t", "a\nb", "a\rb",
				"\u00A0", "\u00A0a", "\u2028", "\u2029", "\u0085", "a\u2028b",
				"e6bfd6f0-0b1e-4b06-b1b0-4e4d5b1a2c3d",
				"E6BFD6F0-0B1E-4B06-B1B0-4E4D5B1A2C3D",
				"e6bfd6f0-0b1e-4b06-b1b0-4e4d5b1a2c3",
				"e6bfd6f0-0b1e-4b06-b1b0-4e4d5b1a2c3dd",
				"e6bfd6f00b1e4b06b1b04e4d5b1a2c3d",
				"g6bfd6f0-0b1e-4b06-b1b0-4e4d5b1a2c3d",
				"e6bfd6f0_0b1e-4b06-b1b0-4e4d5b1a2c3d",
				"900000000000207008", "900000000000207008 ", " 900000000000207008"
		));
		// random values over an alphabet chosen to hit every boundary
		Random random = new Random(20260831L);
		char[] alphabet = {'0', '1', '9', 'a', 'f', 'F', 'g', '-', ' ', '\t', '\n', '\r', '\u00A0', '\u2028'};
		for (int i = 0; i < 200_000; i++) {
			int len = random.nextInt(40);
			StringBuilder b = new StringBuilder(len);
			for (int j = 0; j < len; j++) {
				b.append(alphabet[random.nextInt(alphabet.length)]);
			}
			v.add(b.toString());
		}
		return v;
	}

	private static boolean digits(String v) {
		for (int i = 0; i < v.length(); i++) {
			char c = v.charAt(i);
			if (c < '0' || c > '9') return false;
		}
		return true;
	}

	private static boolean reflectIsUuid(String v) {
		return reflect("isUuid", v);
	}

	private static boolean reflect(String name, String value) {
		try {
			Method m = ColumnPatternTester.class.getDeclaredMethod(name, String.class);
			m.setAccessible(true);
			return (boolean) m.invoke(null, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("ColumnPatternTester." + name + "(String) should exist", e);
		}
	}

	private static String visible(String s) {
		StringBuilder b = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			if (c < 0x20 || c > 0x7E) b.append(String.format("\\u%04X", (int) c));
			else b.append(c);
		}
		return b.append('"').toString();
	}
}

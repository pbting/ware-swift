package com.ware.swift.core;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class WareCoreSwiftStringUtils {
	/**
	 * A String for a space character.
	 *
	 * @since 3.2
	 */
	public static final String SPACE = " ";

	/**
	 * The empty String {@code ""}.
	 * @since 2.0
	 */
	public static final String EMPTY = "";

	/**
	 * A String for linefeed LF ("\n").
	 *
	 * @see <a href=
	 * "http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF:
	 * Escape Sequences for Character and String Literals</a>
	 * @since 3.2
	 */
	public static final String LF = "\n";

	/**
	 * A String for carriage return CR ("\r").
	 *
	 * @see <a href=
	 * "http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF:
	 * Escape Sequences for Character and String Literals</a>
	 * @since 3.2
	 */
	public static final String CR = "\r";

	/**
	 * Represents a failed index search.
	 * @since 2.1
	 */
	public static final int INDEX_NOT_FOUND = -1;

	/**
	 * <p>
	 * The maximum size to which the padding constant(s) can expand.
	 * </p>
	 */
	private static final int PAD_LIMIT = 8192;

	/**
	 * A regex pattern for recognizing blocks of whitespace characters. The apparent
	 * convolutedness of the pattern serves the purpose of ignoring "blocks" consisting of
	 * only a single space: the pattern is used only to normalize whitespace, condensing
	 * "blocks" down to a single space, thus matching the same would likely cause a great
	 * many noop replacements.
	 */
	private static final Pattern WHITESPACE_PATTERN = Pattern
			.compile("(?: |\\u00A0|\\s|[\\s&&[^ ]])\\s*");

	/**
	 * <p>
	 * {@code StringUtils} instances should NOT be constructed in standard programming.
	 * Instead, the class should be used as {@code StringUtils.trim(" foo ");}.
	 * </p>
	 *
	 * <p>
	 * This constructor is public to permit tools that require a JavaBean instance to
	 * operate.
	 * </p>
	 */
	public WareCoreSwiftStringUtils() {
		super();
	}

	public static boolean isEmpty(final CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	public static boolean isNotEmpty(final CharSequence cs) {
		return !WareCoreSwiftStringUtils.isEmpty(cs);
	}

	public static boolean isAnyEmpty(CharSequence... css) {
		if (css == null) {
			return true;
		}
		for (CharSequence cs : css) {
			if (isEmpty(cs)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNoneEmpty(CharSequence... css) {
		return !isAnyEmpty(css);
	}

	public static boolean isBlank(final CharSequence cs) {
		int strLen;
		if (cs == null || (strLen = cs.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if (Character.isWhitespace(cs.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNotBlank(final CharSequence cs) {
		return !WareCoreSwiftStringUtils.isBlank(cs);
	}

	public static boolean isAnyBlank(CharSequence... css) {
		if (css == null) {
			return true;
		}
		for (CharSequence cs : css) {
			if (isBlank(cs)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNoneBlank(CharSequence... css) {
		return !isAnyBlank(css);
	}

	public static String trim(final String str) {
		return str == null ? null : str.trim();
	}

	public static String trimToNull(final String str) {
		final String ts = trim(str);
		return isEmpty(ts) ? null : ts;
	}

	public static String trimToEmpty(final String str) {
		return str == null ? EMPTY : str.trim();
	}

	public static String strip(final String str) {
		return strip(str, null);
	}

	public static String stripToNull(String str) {
		if (str == null) {
			return null;
		}
		str = strip(str, null);
		return str.isEmpty() ? null : str;
	}

	public static String stripToEmpty(final String str) {
		return str == null ? EMPTY : strip(str, null);
	}

	public static String strip(String str, final String stripChars) {
		if (isEmpty(str)) {
			return str;
		}
		str = stripStart(str, stripChars);
		return stripEnd(str, stripChars);
	}

	public static String stripStart(final String str, final String stripChars) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return str;
		}
		int start = 0;
		if (stripChars == null) {
			while (start != strLen && Character.isWhitespace(str.charAt(start))) {
				start++;
			}
		}
		else if (stripChars.isEmpty()) {
			return str;
		}
		else {
			while (start != strLen
					&& stripChars.indexOf(str.charAt(start)) != INDEX_NOT_FOUND) {
				start++;
			}
		}
		return str.substring(start);
	}

	public static String stripEnd(final String str, final String stripChars) {
		int end;
		if (str == null || (end = str.length()) == 0) {
			return str;
		}

		if (stripChars == null) {
			while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
				end--;
			}
		}
		else if (stripChars.isEmpty()) {
			return str;
		}
		else {
			while (end != 0
					&& stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
				end--;
			}
		}
		return str.substring(0, end);
	}

	public static String[] stripAll(final String... strs) {
		return stripAll(strs, null);
	}

	public static String[] stripAll(final String[] strs, final String stripChars) {
		int strsLen;
		if (strs == null || (strsLen = strs.length) == 0) {
			return strs;
		}
		final String[] newArr = new String[strsLen];
		for (int i = 0; i < strsLen; i++) {
			newArr[i] = strip(strs[i], stripChars);
		}
		return newArr;
	}

	public static String stripAccents(final String input) {
		if (input == null) {
			return null;
		}
		final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$
		final String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
		// Note that this doesn't correctly remove ligatures...
		return pattern.matcher(decomposed).replaceAll("");//$NON-NLS-1$
	}

	public static boolean equals(final CharSequence cs1, final CharSequence cs2) {
		if (cs1 == cs2) {
			return true;
		}
		if (cs1 == null || cs2 == null) {
			return false;
		}
		if (cs1 instanceof String && cs2 instanceof String) {
			return cs1.equals(cs2);
		}
		return false;
	}
}

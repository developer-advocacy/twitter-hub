package com.joshlong.twitter.utils;

import org.springframework.util.Assert;

public abstract class StringUtils {

	public static String securityMask(String text) {
		return mask(text, '*');
	}

	private static String repeat(char maskChar, int count) {
		var n = new StringBuffer();
		n.append(String.valueOf(maskChar).repeat(Math.max(0, count)));
		return n.toString();
	}

	public static String mask(String text, char maskChar) {
		var show = .2;
		Assert.hasText(text, "the text to mask must be non empty!");
		var l = text.length();
		if (l == 1)
			return "" + maskChar;
		if (l <= 5)
			return repeat(maskChar, 3); // that way we dont have weirdness w/ odds
		var lettersToShowCount = (int) (show * ((double) l));
		if (lettersToShowCount % 2 == 1)
			lettersToShowCount += 1;
		var b = text.substring(0, lettersToShowCount);
		var m = repeat(maskChar, l - (2 * lettersToShowCount));
		var e = text.substring(text.length() - lettersToShowCount);
		return b + m + e;
	}

}

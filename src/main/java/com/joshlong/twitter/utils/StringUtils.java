package com.joshlong.twitter.utils;

import org.springframework.util.Assert;

public abstract class StringUtils {

	public static String securityMask(String text) {
		return mask(text, '*');
	}

	public static String mask(String text, char maskChar) {
		Assert.hasText(text, "the text to mask must be non empty!");
		var n = new StringBuffer();
		for (var i = 0; i < text.length(); i++)
			n.append(maskChar);
		return n.toString();
		// var len = text.length();
		// var quarter = ((double) len) * .25;
		// var size = text.length();
		// if (quarter > 0) {
		// size = (int) quarter;
		// }
		// // return the whole thing masked
		// var nm = new StringBuffer() ;
		// for (var i = 0 ; i < size ; i++ )
		// nm.append( maskChar)

	}

}

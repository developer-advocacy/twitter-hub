package com.joshlong.twitter.utils;

import org.springframework.util.Assert;

public abstract class TwitterUtils {

	public static String validateUsername(String twitterUsername) {
		Assert.hasText(twitterUsername, "the twitter username must not be empty");
		var cleanTwitterUsername = twitterUsername.toLowerCase();
		if (cleanTwitterUsername.startsWith("@"))
			cleanTwitterUsername = cleanTwitterUsername.substring(1);
		return cleanTwitterUsername;
	}

}

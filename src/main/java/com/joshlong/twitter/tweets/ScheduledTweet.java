package com.joshlong.twitter.tweets;

import java.util.Date;

public record ScheduledTweet(String username, String text,
		String media/* this is the base64 encoded version of the file */, Date scheduled, String clientId,
		String clientSecret, Date sent, String id) {
}

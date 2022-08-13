package com.joshlong.twitter.tweets;

import java.util.Date;

public record ScheduledTweet(String username, String jsonRequest, Date scheduled, String clientId, String clientSecret,
		Date sent, String id) {
}

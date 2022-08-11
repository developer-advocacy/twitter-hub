package com.joshlong.twitter.tweets;

import java.util.Date;

public record ScheduledTweet(String username, String jsonRequest, Date scheduled, String clientId, Date sent) {

	public ScheduledTweet(String username, String jsonRequest, Date scheduled, String clientId) {
		this(username, jsonRequest, scheduled, clientId, null);
	}

	public ScheduledTweet(String username, String jsonRequest, Date scheduled, String clientId, Date sent) {
		this.username = username;
		this.jsonRequest = jsonRequest;
		this.scheduled = scheduled;
		this.clientId = clientId;
		this.sent = sent;
	}
}

package com.joshlong.twitter.registrations;

import java.util.Date;

public record TwitterRegistration(String username, String accessToken, String refreshToken, Date lastUpdated) {

	public TwitterRegistration(String username, String accessToken, String refreshToken) {
		this(username, accessToken, refreshToken, null);
	}

	public TwitterRegistration(String username, String accessToken, String refreshToken, Date lastUpdated) {
		this.username = username;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.lastUpdated = lastUpdated;
	}
}

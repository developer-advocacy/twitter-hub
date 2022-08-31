package com.joshlong.twitter.registrations;

import java.util.Date;

public record TwitterRegistration(String username, String accessToken, String accessTokenSecret, Date lastUpdated) {

	public TwitterRegistration(String username, String accessToken, String accessTokenSecret) {
		this(username, accessToken, accessTokenSecret, null);
	}

}

package com.joshlong.twitter;

import com.joshlong.twitter.clients.Client;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twitter")
public record TwitterProperties(App app, Encryption encryption, Client[] clients) {

	public record Encryption(String password, String salt) {
	}

	public record App(String clientId, String clientSecret) {
	}

}

package com.joshlong.twitter;

import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@Slf4j
@SpringBootTest
class TwitterApiIntegrationTest {

	//
	private final TwitterApiIntegration integration;

	private final StreamBridge streamBridge;

	private final ClientService clients;

	private final TwitterRegistrationService registrations;

	//
	private final String secret = "1234";

	private final String clientId = "test-client";

	TwitterApiIntegrationTest(@Autowired TwitterRegistrationService registrationService,
			@Autowired StreamBridge streamBridge, @Autowired ClientService clients,
			@Autowired TwitterApiIntegration integration) {
		this.integration = integration;
		this.clients = clients;
		this.streamBridge = streamBridge;
		this.registrations = registrationService;
	}

	@Test
	void inAndOut() {
		var username = "springbuxman";
		var accessToken = "accessToken";
		var refreshToken = "refreshToken";
		var registered = this.registrations.register(username, accessToken, refreshToken);
		StepVerifier
				.create(registered).expectNextMatches(tr -> tr.username().equals(username)
						&& tr.accessToken().equals(accessToken) && tr.refreshToken().equals(refreshToken))
				.verifyComplete();
	}

	@Test
	void sendLiveTweet() {
		StepVerifier //
				.create(this.clients.register(this.clientId, this.secret)
						.then(this.clients.authenticate(this.clientId, this.secret)))//
				.expectNextMatches(c -> c.clientId().equals(this.clientId)) //
				.verifyComplete();
		var json = String.format("""
				{ "text" : "sending a tweet from a unit test at %s" }
				""", Instant.now().toString());
		var live = this.integration //
				.sendLiveTweet(this.clientId, this.secret, "bpmpass", json) //
				.switchIfEmpty(Mono.error(new IllegalStateException("couldn't send the tweet!")))//
				.doOnError(e -> log.error("oops! something happened! ", e));
		StepVerifier.create(live)
				.expectNextMatches(pt -> StringUtils.hasText(pt.id()) && StringUtils.hasText(pt.text()))
				.verifyComplete();

	}

}
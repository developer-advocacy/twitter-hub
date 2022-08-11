package com.joshlong.twitter;

import com.joshlong.twitter.clients.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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

	//
	private final String secret = "1234";

	private final String clientId = "test-client";

	TwitterApiIntegrationTest(@Autowired StreamBridge streamBridge, @Autowired ClientService clients,
			@Autowired TwitterApiIntegration integration) {
		this.integration = integration;
		this.clients = clients;
		this.streamBridge = streamBridge;
	}

	@BeforeEach
	void before() {
		StepVerifier //
				.create(this.clients.register(this.clientId, this.secret)
						.then(this.clients.authenticate(this.clientId, this.secret)))//
				.expectNextMatches(c -> c.clientId().equals(this.clientId)) //
				.verifyComplete();
	}

	@Test
	void directInvocation() {
		var json = String.format("""
				{ "text" : "function at %s" }
				""", Instant.now().toString());
		log.debug("sending " + json);
		var live = this.integration //
				.sendLiveTweet(this.clientId, this.secret, "bpmpass", json) //
				.switchIfEmpty(Mono.error(new IllegalStateException("couldn't send the tweet!")))//
				.doOnError(e -> log.error("oops! something happened! ", e));
		StepVerifier.create(live)
				.expectNextMatches(pt -> StringUtils.hasText(pt.id()) && StringUtils.hasText(pt.text()))
				.verifyComplete();

	}

}
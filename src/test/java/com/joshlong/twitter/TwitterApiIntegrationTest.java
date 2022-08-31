package com.joshlong.twitter;

import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@Slf4j
@SpringBootTest
class TwitterApiIntegrationTest {

	private final TwitterApiIntegration integration;

	private final StreamBridge streamBridge;

	private final ClientService clients;

	private final TwitterRegistrationService registrations;

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
	void createMedia() throws Exception {
		var imgResource = new ClassPathResource("/images/test.png");
		try (var in = imgResource.getInputStream()) {
			var bytes = FileCopyUtils.copyToByteArray(in);
			var encoded = Base64Utils.encodeToString(bytes);
			var request = new TwitterApiIntegration.Request("this is a test. can we send images? ", encoded);
			var tr = this.registrations.byUsername("bpmpass").flatMap(r -> this.integration.post(request, r));
			StepVerifier.create(tr).verifyComplete();
		}
	}

	@Test
	void inAndOut() {
		var username = "springbuxman";
		var accessToken = "accessToken";
		var accessTokenSecret = "accessTokenSecret";
		var registered = this.registrations.register(username, accessToken, accessTokenSecret);
		StepVerifier.create(registered)//
				.expectNextMatches(tr -> tr.username().equals(username)
						&& tr.accessTokenSecret().equals(accessTokenSecret) && tr.accessToken().equals(accessToken)) //
				.verifyComplete();
	}

	@Test
	void sendLiveTweet() {
		StepVerifier //
				.create(this.clients.register(this.clientId, this.secret)
						.then(this.clients.authenticate(this.clientId, this.secret)))//
				.expectNextMatches(c -> c.id().equals(this.clientId)) //
				.verifyComplete();
		var json = String.format("""
				{ "text" : "sending a tweet from a unit test at %s" }
				""", Instant.now().toString());
		var live = this.integration //
				.tweet(this.clientId, this.secret, "bpmpass", json) //
				.switchIfEmpty(Mono.error(new IllegalStateException("couldn't send the tweet!")))//
				.doOnError(e -> log.error("oops! something happened! ", e));
		StepVerifier.create(live)
				.expectNextMatches(pt -> StringUtils.hasText(pt.id()) && StringUtils.hasText(pt.text()))
				.verifyComplete();

	}

}
package com.joshlong.twitter;

import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
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

	// todo figure out how to do the uploads dance with twitter
	// https://developer.twitter.com/en/docs/twitter-api/v1/media/upload-media/api-reference/post-media-upload

	// @Test
	void createMedia() throws Exception {
		var imgResource = new ClassPathResource("/images/test.png");
		var bytes = (byte[]) null;
		var encoded = (String) null;
		try (var in = imgResource.getInputStream()) {
			bytes = FileCopyUtils.copyToByteArray(in);
			encoded = Base64Utils.encodeToString(bytes);
		}
		var http = WebClient.builder().build();
		var at = "TnprckpVZEpNQnhybi13Nl9NMVY0Z3pNdlEzcTFNdlBqXzhjMGZPY08zLVF0OjE2NjE3NjMzNTU2NTE6MTowOmF0OjE";
		var mediaUploadUrl = "https://upload.twitter.com/1.1/media/upload.json?media_category=tweet_image";
		/*
		 * var multipartBodyBuilder = new MultipartBodyBuilder(); multipartBodyBuilder
		 * .part("media_data", bytes) ; var build = multipartBodyBuilder.build();
		 */
		// bodyBuilder.part("profileImage",
		// ClassPathResource("test-image.jpg").file.readBytes())
		// .header("Content-Disposition", "form-data; name=profileImage;
		// filename=profile-image.jpg")
		var media = http.post().uri(mediaUploadUrl).contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData("media_data", encoded)).headers(h -> h.setBearerAuth(at))
				.retrieve().bodyToMono(String.class);
		StepVerifier.create(media.doOnNext(log::info)).verifyComplete();
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
				.expectNextMatches(c -> c.id().equals(this.clientId)) //
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
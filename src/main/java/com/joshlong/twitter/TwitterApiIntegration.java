package com.joshlong.twitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistration;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import com.joshlong.twitter.utils.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
class TwitterApiIntegration {

	private final WebClient http;

	private final ClientService clients;

	private final TwitterRegistrationService registrations;

	private final ObjectMapper objectMapper;

	private final TypeReference<Map<String, Object>> tr = new TypeReference<>() {
	};

	private final ParameterizedTypeReference<Map<String, Object>> ptr = new ParameterizedTypeReference<>() {
	};

	private final Map<String, Twitter> twitterCacheMap = new ConcurrentHashMap<>();

	private final String apiKey, apiKeySecret;

	TwitterApiIntegration(WebClient http, ClientService clients, TwitterRegistrationService registrations,
			ObjectMapper objectMapper, String apiKey, String apiKeySecret) {
		this.http = http;
		this.clients = clients;
		this.registrations = registrations;
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.apiKeySecret = apiKeySecret;
	}

	/**
	 * this method does the actual work of validating that we have a valid client making
	 * the request, that we have a fresh access token and refresh token, and that we then
	 * send the tweet out.
	 * @param clientId the OAuth clientId, <em>not</em> the {@code twitter_clients} client
	 * ID
	 * @param clientSecret the OAuth client secret
	 * @param twitterUsername the Twitter username under whose name we should send these
	 * tweets
	 * @param json the payload we'd like to send out (should comply with the <a href=
	 * "https://developer.twitter.com/en/docs/twitter-api/tweets/manage-tweets/api-reference/post-tweets">expectations
	 * of the Twitter HTTP API</a>}
	 */
	/* package private for testing */
	public Mono<TweetResponse> tweet(String clientId, String clientSecret, String twitterUsername, String json) {
		log.debug("trying to send the tweet: clientId: [" + clientId + "], clientSecret: ["
				+ StringUtils.securityMask(clientSecret) + "], twitterUsername: [@" + twitterUsername + "], json: ["
				+ json + "]");
		return this.clients //
				.authenticate(clientId, clientSecret)//
				.flatMap(c -> this.registrations.byUsername(twitterUsername))//
				.doOnNext(registration -> log.info("got a valid registration for @" + registration.username() + "."))//
				.flatMap(tr -> post(json, tr))//
				.doOnError(e -> log.error("oops!", e)) //
				.doOnNext(pt -> log.info("posted tweet: " + pt.toString()));
	}

	record Request(String text, String image) {

	}

	@SneakyThrows
	Mono<TweetResponse> post(String json, TwitterRegistration twitterRegistration) {
		var parse = objectMapper.readValue(json, Request.class);
		return post(parse, twitterRegistration);
	}

	@SneakyThrows
	Mono<TweetResponse> post(Request request, TwitterRegistration twitterRegistration) {

		var twitter = this.getTwitterInstance(this.apiKey, this.apiKeySecret, twitterRegistration.accessToken(),
				twitterRegistration.accessTokenSecret());

		Assert.hasText(request.text(), "the text of the request is null");

		var uuid = UUID.randomUUID().toString();
		var status = (Status) null;
		if (org.springframework.util.StringUtils.hasText(request.image())) {
			var decodedBytes = Base64Utils.decodeFromString(request.image());
			var byteArrayResource = new ByteArrayResource(decodedBytes);
			try (var in = byteArrayResource.getInputStream()) {
				status = twitter.updateStatus(new StatusUpdate(request.text()).media(uuid, in));
			}
		}
		else {
			status = twitter.updateStatus(new StatusUpdate(request.text()));
		}
		var twitterResult = new TweetResponse(Long.toString(status.getId()), status.getText());
		return Mono.just(twitterResult);
	}

	private Twitter getTwitterInstance(String apiKey, String apiKeySecret, String accessToken,
			String accessTokenSecret) {
		var key = ("" + apiKey + apiKeySecret + accessTokenSecret + accessToken);
		Assert.hasText(key, "the key must have text!");
		return this.twitterCacheMap.computeIfAbsent(key, k -> {
			var cb = new ConfigurationBuilder() //
					.setDebugEnabled(true) ///
					.setOAuthConsumerKey(apiKey)//
					.setOAuthConsumerSecret(apiKeySecret) //
					.setOAuthAccessToken(accessToken) //
					.setOAuthAccessTokenSecret(accessTokenSecret);
			var twitterFactory = new TwitterFactory(cb.build());
			return twitterFactory.getInstance();
		});
	}

}

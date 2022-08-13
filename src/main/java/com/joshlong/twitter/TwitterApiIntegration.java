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
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

@Slf4j
class TwitterApiIntegration {

	private final WebClient http;

	private final ClientService clients;

	private final TwitterRegistrationService registrations;

	private final ObjectMapper objectMapper;

	private final String oauthClientId, oauthClientSecret;

	private final TypeReference<Map<String, Object>> tr = new TypeReference<>() {
	};

	private final ParameterizedTypeReference<Map<String, Object>> ptr = new ParameterizedTypeReference<>() {
	};

	TwitterApiIntegration(String oauthClientId, String oauthClientSecret, WebClient http, ClientService clients,
			TwitterRegistrationService registrations, ObjectMapper objectMapper) {
		this.http = http;
		this.clients = clients;
		this.registrations = registrations;
		this.objectMapper = objectMapper;
		this.oauthClientId = oauthClientId;
		this.oauthClientSecret = oauthClientSecret;
	}

	/**
	 * Attempts to refresh the access token by calling the /token endpoint and then
	 * updates the registration with the new access token and refresh token
	 */
	/* package private for testing */
	private Mono<TwitterRegistration> refreshAccessToken(String twitterUsername, String refreshToken) {
		log.debug("the client id is [" + this.oauthClientId + "] and the refresh token is ["
				+ StringUtils.securityMask(refreshToken) + "]");
		return this.http //
				.post()//
				.uri("https://api.twitter.com/2/oauth2/token")//
				.body(BodyInserters.fromFormData("client_id", this.oauthClientId) //
						.with("grant_type", "refresh_token") //
						.with("refresh_token", refreshToken)//
				) //
				.headers(httpHeaders -> {
					httpHeaders.setContentType(MediaType.parseMediaType("application/x-www-form-urlencoded"));
					httpHeaders.setBasicAuth(this.oauthClientId, this.oauthClientSecret);
				})//
				.retrieve()//
				.bodyToMono(this.ptr)//
				.flatMap(map -> this.registrations.register(twitterUsername, (String) map.get("access_token"),
						(String) map.get("refresh_token")));
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
	 * @return
	 */
	/* package private for testing */
	Mono<TweetResponse> sendLiveTweet(String clientId, String clientSecret, String twitterUsername, String json) {
		var jsonRequest = json.trim();
		log.debug("trying to send the tweet: clientId: [" + clientId + "], clientSecret: ["
				+ StringUtils.securityMask(clientSecret) + "], twitterUsername: [@" + twitterUsername + "], json: ["
				+ jsonRequest + "]");
		var now = new Date();
		var freshnessWindow = 30 * 1000 * 60;
		var freshTwitterRegistration = this.registrations //
				.byUsername(twitterUsername) //
				.doOnNext(tr -> log.info("found a TwitterRegistration " + tr.toString())).flatMap(tr -> {//
					var tooOld = now.getTime() - freshnessWindow;
					var fresh = tr.lastUpdated().getTime() > tooOld;
					if (fresh) { // if it was updated in the last two hours, use it.
						log.info("the access token for @" + tr.username() + " was created recently (" + tr.lastUpdated()
								+ "). Using as-is");
						return Mono.just(tr);
					}
					else {
						// otherwise, refresh it
						log.info("the access token for @" + tr.username() + " was not created recently ("
								+ tr.lastUpdated() + "). Refreshing");
						return refreshAccessToken(tr.username(), tr.refreshToken());
					}
				}) //
				.doOnError(e -> log.error("oops!", e));
		return this.clients //
				.authenticate(clientId, clientSecret)//
				.flatMap(c -> freshTwitterRegistration)//
				.doOnNext(registration -> log.info("got a valid " + TwitterRegistration.class.getSimpleName()
						+ " with a fresh access code for @" + registration.username() + "."))//
				.flatMap(tr -> post(jsonRequest, tr.accessToken()))//
				.doOnNext(pt -> log.info("posted tweet: " + pt.toString()));

	}

	@SneakyThrows
	private TweetResponse from(String jsonReply) {
		var jsonDecoded = objectMapper.readValue(jsonReply, this.tr);
		var dataObject = jsonDecoded.get("data");
		if (dataObject instanceof Map<?, ?> map) {
			log.debug("the json received in reply is " + jsonDecoded);
			var id = (String) map.get("id");
			var text = (String) map.get("text");
			return new TweetResponse(id, text);
		}
		return null;
	}

	@SneakyThrows
	private Mono<TweetResponse> post(String jsonRequest, String accessToken) {
		Assert.hasText(jsonRequest, "the JSON must be non-empty");
		Assert.hasText(accessToken, "the accessToken must be non-empty");
		log.info("going to try posting with access token [" + StringUtils.securityMask(accessToken)
				+ "] and the following JSON body: " + jsonRequest);
		var map = this.objectMapper.readValue(jsonRequest, this.tr);
		return this.http.post() //
				.uri("https://api.twitter.com/2/tweets")//
				.body(Mono.just(map), this.ptr) //
				.headers(h -> h.setBearerAuth(accessToken)) //
				.retrieve()//
				.bodyToMono(String.class) //
				.mapNotNull(this::from) //
				.doOnError(exception -> log.error("got an error posting!", exception));
	}

}

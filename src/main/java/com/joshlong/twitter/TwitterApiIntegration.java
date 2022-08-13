package com.joshlong.twitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistration;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import com.joshlong.twitter.tweets.ScheduledTweetService;
import com.joshlong.twitter.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
		this.oauthClientId = oauthClientId.trim();
		this.oauthClientSecret = oauthClientSecret.trim();
	}

	/**
	 * Attempts to refresh the access token by calling the /token endpoint and then
	 * updates the registration with the new access token and refresh token
	 */
	/* package private for testing */
	private Mono<TwitterRegistration> refreshAccessToken(String twitterUsername, String refreshToken) {
		log.debug("the client id is [" + this.oauthClientId + "] and the refresh token is ["
				+ refreshToken.substring(0, 10) + "..." + "]");
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
	 * @param twitterUsername the Twitter username under whsse name we should send these
	 * tweets
	 * @param json the payload we'd like to send out (should comply with the <a href=
	 * "https://developer.twitter.com/en/docs/twitter-api/tweets/manage-tweets/api-reference/post-tweets">expectations
	 * of the Twitter HTTP API</a>}
	 * @return
	 */
	/* package private for testing */
	Mono<TweetResponse> sendLiveTweet(String clientId, String clientSecret, String twitterUsername, String json) {
		var jsonRequest = json.trim();
		log.debug("trying to send the tweet: clientId: [" + clientId + "], clientSecret: [" + clientSecret
				+ "], twitterUsername: [" + twitterUsername + "], json: [" + jsonRequest + "]");
		var now = new Date();
		var freshnessPeriodIs1H15M = 30 * 1000 * 60;
		var freshTR = registrations //
				.byUsername(twitterUsername) //
				.doOnNext(tr -> log.debug("authenticated " + tr.username())) //
				.flatMap(tr -> {//
					var tooOld = now.getTime() - freshnessPeriodIs1H15M;
					var fresh = tr.lastUpdated().getTime() > tooOld;
					if (fresh) { // if it was updated in the last two hours, use it.
						return Mono.just(tr);
					}
					else {
						// otherwise, refresh it
						return refreshAccessToken(tr.username(), tr.refreshToken());
					}
				}) //
				.doOnNext(tr -> log.debug("got a valid TwitterRegistration for " + tr.username())) //
				.doOnError(e -> log.error("oops!", e));
		return this.clients //
				.authenticate(clientId, clientSecret)//
				.flatMap(c -> freshTR)//
				.doOnNext(registration -> log.debug(
						"got a valid registration with a fresh access code for @" + registration.username() + "."))//
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
		log.debug("going to try posting with access token [" + accessToken.substring(0, 10) + "..."
				+ "] and the following JSON body: " + jsonRequest);
		var map = this.objectMapper.readValue(jsonRequest, this.tr);
		log.debug("here is the payload :" + map.toString());
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

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
class SchedulerConfiguration {

	private final TwitterApiIntegration integration;

	private final ScheduledTweetService service;

	private final TransactionalOperator operator;

	@Scheduled(timeUnit = TimeUnit.MINUTES, fixedDelay = 1)
	public void runPeriodically() {
		this.service //
				.due() //
				.doOnNext(st -> log.debug("new scheduled tweet: " + st)) //
				.flatMap(st -> this.operator.transactional(//
						this.integration//
								.sendLiveTweet(st.clientId(), st.clientSecret(), st.username(), st.jsonRequest())//
								.flatMap(x -> Mono.just(st))//
								.flatMap(s -> this.service.send(s, new Date())) //
				))//
				.subscribe(tr -> log.info("processed " + tr.toString()));
	}

	@Bean
	Function<Flux<Message<String>>, Flux<Void>> twitterRequests(ObjectMapper objectMapper,
			ScheduledTweetService service) {
		return sink -> sink.flatMap(message -> {
			var payload = parseJsonIntoTweetRequest(objectMapper, message.getPayload());
			return service
					.schedule(payload.twitterUsername(), payload.jsonRequest(), payload.scheduled(), payload.clientId(),
							payload.clientSecret(), null) //
					.then();
		});
	}

	@SneakyThrows
	private static TweetRequest parseJsonIntoTweetRequest(ObjectMapper objectMapper, String json) {
		var jn = objectMapper.readValue(json, JsonNode.class);
		return new TweetRequest(jn.get("clientId").textValue(), jn.get("clientSecret").textValue(),
				jn.get("twitterUsername").textValue(), jn.get("jsonRequest").textValue(),
				DateUtils.readIsoDateTime(jn.get("scheduled").textValue()));
	}

}

record TweetRequest(String clientId, String clientSecret, String twitterUsername, String jsonRequest, Date scheduled) {
}

record TweetResponse(String id, String text) {
}

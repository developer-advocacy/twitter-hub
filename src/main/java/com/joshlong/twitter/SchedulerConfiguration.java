package com.joshlong.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.tweets.ScheduledTweet;
import com.joshlong.twitter.tweets.ScheduledTweetService;
import com.joshlong.twitter.utils.Base64Utils;
import com.joshlong.twitter.utils.DateUtils;
import com.joshlong.twitter.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
class SchedulerConfiguration {

	private final TwitterApiIntegration integration;

	private final ScheduledTweetService service;

	private final TransactionalOperator operator;

	@Scheduled(timeUnit = TimeUnit.MINUTES, fixedDelay = 1)
	public void scheduledTweetsLoop() {
		this.service //
				.due() //
				.doOnNext(st -> log.info("attempting to send scheduled tweet: " + debugMap(st))) //
				.flatMap(st -> this.operator.transactional(//
						this.integration//
								.tweet(st.clientId(), st.clientSecret(), st.username(), st.text(),
										Base64Utils.decode(st.media()))//
								.flatMap(x -> Mono.just(st))//
								.flatMap(s -> this.service.schedule(s, new Date())) //
				))//
				.subscribe(st -> log.info("sent " + debugMap(st)));
	}

	@Bean
	Function<Flux<Message<String>>, Flux<Void>> twitterRequests(ObjectMapper objectMapper,
			ScheduledTweetService service) {
		return sink -> sink //
				.flatMap(message -> { //
					var unparsedPayload = message.getPayload();
					log.info("new payload: " + unparsedPayload);
					var payload = parseJsonIntoTweetRequest(objectMapper, message.getPayload());
					var scheduledTweet = new ScheduledTweet(payload.twitterUsername(), payload.text(), payload.media(),
							payload.scheduled(), payload.clientId(), payload.clientSecret(), null,
							UUID.randomUUID().toString());
					return service.schedule(scheduledTweet, null).then();
				});
	}

	@SneakyThrows
	private static TweetRequest parseJsonIntoTweetRequest(ObjectMapper objectMapper, String json) {
		var jn = objectMapper.readValue(json, JsonNode.class);
		var scheduledNode = jn.has("scheduled") ? jn.get("scheduled") : null;
		var scheduled = (null == scheduledNode) ? new Date() : DateUtils.readIsoDateTime(scheduledNode.textValue());

		return new TweetRequest(//
				jn.get("clientId").textValue(), //
				jn.get("clientSecret").textValue(), //
				jn.get("twitterUsername").textValue(), //
				jn.get("text").textValue(), //
				jn.get("media").textValue(), //
				scheduled);
	}

	private static Map<String, String> debugMap(ScheduledTweet st) {
		return Map.of( //
				"clientId", st.clientId(), //
				"twitter username", st.username(), //
				"text", st.text(), //
				"media",
				(org.springframework.util.StringUtils.hasText(st.media()) ? st.media() : "").length() + " characters", //
				"clientSecret", StringUtils.securityMask(st.clientSecret()) //
		);
	}

}

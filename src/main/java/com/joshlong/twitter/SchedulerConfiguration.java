package com.joshlong.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.tweets.ScheduledTweet;
import com.joshlong.twitter.tweets.ScheduledTweetService;
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
								.sendLiveTweet(st.clientId(), st.clientSecret(), st.username(), st.jsonRequest())//
								.flatMap(x -> Mono.just(st))//
								.flatMap(s -> this.service.send(s, new Date())) //
				))//
				.subscribe(st -> log.info("sent " + debugMap(st)));
	}

	@Bean
	Function<Flux<Message<String>>, Flux<Void>> twitterRequests(ObjectMapper objectMapper,
			ScheduledTweetService service) {
		return sink -> sink //
				.flatMap(message -> { //
					var payload = parseJsonIntoTweetRequest(objectMapper, message.getPayload());
					var scheduled = payload.scheduled();
					return service
							.schedule(payload.twitterUsername(), payload.jsonRequest(), scheduled, payload.clientId(),
									payload.clientSecret(), null) //
							.then();
				});
	}

	@SneakyThrows
	private static TweetRequest parseJsonIntoTweetRequest(ObjectMapper objectMapper, String json) {
		var jn = objectMapper.readValue(json, JsonNode.class);
		return new TweetRequest(//
				jn.get("clientId").textValue(), //
				jn.get("clientSecret").textValue(), //
				jn.get("twitterUsername").textValue(), //
				jn.get("jsonRequest").textValue(), //
				DateUtils.readIsoDateTime(jn.get("scheduled").textValue())//
		);
	}

	private static Map<String, String> debugMap(ScheduledTweet st) {
		return Map.of( //
				"clientId", st.clientId(), //
				"twitter username", st.username(), //
				"json", st.jsonRequest(), //
				"clientSecret", StringUtils.securityMask(st.clientSecret()) //
		);
	}

}

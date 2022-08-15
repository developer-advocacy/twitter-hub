package com.joshlong.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.scheduling.engine.ScheduleEvent;
import com.joshlong.scheduling.engine.SchedulingService;
import com.joshlong.twitter.tweets.ScheduledTweet;
import com.joshlong.twitter.tweets.ScheduledTweetService;
import com.joshlong.twitter.utils.DateUtils;
import com.joshlong.twitter.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
class SchedulerConfiguration {

	private final TwitterApiIntegration integration;

	private final ScheduledTweetService service;

	private final TransactionalOperator operator;

	private final SchedulingService schedulingService;

	// todo: force this to use the
	// https://github.com/developer-advocacy/bootiful-scheduler
	// @Scheduled(timeUnit = TimeUnit.MINUTES, fixedDelay = 1)

	@EventListener(ApplicationReadyEvent.class)
	public void ready() {
		registerUpcomingDates().subscribe();
	}

	@EventListener
	public void onScheduleEvent(ScheduleEvent scheduleEvent) {
		var message = String.format("""
				got a scheduled event @ %s. going to load all the tweets due up until this point.
				""", scheduleEvent.getSource());
		log.info(message);
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
							.thenMany(registerUpcomingDates()).then();
				});
	}

	/**
	 * this goes through all the as-yet unsent {@link ScheduledTweet tweets}, gets their
	 * dates, calculates a time that's at least one minute later, and then installs that
	 * time as a callback time for the SchedulerTrigger
	 */
	private Flux<Instant> registerUpcomingDates() {
		return this.service //
				.scheduleDates()//
				.map(Date::toInstant)//
				.map(i -> i.plus(1, TimeUnit.MINUTES.toChronoUnit())).collectList() //
				.flatMapMany(list -> {
					log.debug("registering " + list.size() + " upcoming dates: "
							+ list.stream().map(Instant::toString).collect(Collectors.joining(", ")));
					schedulingService.schedule(list);
					return Flux.fromIterable(list);
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

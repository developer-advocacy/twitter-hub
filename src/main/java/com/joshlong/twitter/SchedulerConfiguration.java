package com.joshlong.twitter;

import com.joshlong.twitter.tweets.ScheduledTweetService;
import com.joshlong.twitter.utils.Base64Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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
				.flatMap(st -> this.operator.transactional(//
						this.integration//
								.tweet(st.clientId(), st.clientSecret(), st.username(), st.text(),
										Base64Utils.decode(st.media()))//
								.flatMap(x -> Mono.just(st))//
								.flatMap(s -> this.service.schedule(s, new Date())) //
				))//
				.subscribe(st -> log.info("sent " + (st)));
	}

}

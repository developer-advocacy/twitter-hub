package com.joshlong.twitter.tweets;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface ScheduledTweetService {

	Flux<ScheduledTweet> unscheduled();

	Mono<ScheduledTweet> send(ScheduledTweet tweet, Date date);

	Mono<ScheduledTweet> schedule(String username, String jsonRequest, Date scheduled, String clientId, Date sent);

}

package com.joshlong.twitter.tweets;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface ScheduledTweetService {

	Flux<ScheduledTweet> due();

	Mono<ScheduledTweet> schedule(ScheduledTweet tweet, Date promoted);

}

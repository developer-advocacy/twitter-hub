package com.joshlong.twitter.tweets;

import com.joshlong.twitter.utils.DateUtils;
import com.joshlong.twitter.utils.TwitterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class SqlScheduledTweetService implements ScheduledTweetService {

	private final DatabaseClient dbc;

	private final Function<Map<String, Object>, ScheduledTweet> scheduledTweetMapper = new Function<>() {

		@Override
		public ScheduledTweet apply(Map<String, Object> stringObjectMap) {
			return new ScheduledTweet((String) stringObjectMap.get("username"),
					(String) stringObjectMap.get("json_request"),
					DateUtils.dateFromLocalDateTime(((LocalDateTime) stringObjectMap.get("scheduled"))),
					(String) stringObjectMap.get("client_id"),
					DateUtils.dateFromLocalDateTime((LocalDateTime) stringObjectMap.get("sent")));
		}

	};

	@Override
	public Flux<ScheduledTweet> unscheduled() {
		return this.dbc//
				.sql("  select * from twitter_scheduled_tweets where sent is null ") //
				.fetch()//
				.all()//
				.map(this.scheduledTweetMapper);
	}

	@Override
	public Mono<ScheduledTweet> send(ScheduledTweet tweet, Date sent) {
		Assert.notNull(sent, "you must provide the datetime at which point the ScheduledTweet was sent");
		return this.schedule(tweet.username(), tweet.jsonRequest(), tweet.scheduled(), tweet.clientId(), sent);
	}

	@Override
	public Mono<ScheduledTweet> schedule(String twitterUsername, String jsonRequest, Date scheduled, String clientId,
			Date sent) {
		log.debug("sent is " + (null == sent ? "" : "not") + " null");
		var minimumRunwayInMinutes = 5;
		var username = TwitterUtils.validateUsername(twitterUsername);
		var sql = """
				insert into twitter_scheduled_tweets (username,  json_request, scheduled, client_id, sent ) values
				( :username, :json_request, :scheduled, :client_id, :sent )
				on conflict on constraint twitter_scheduled_tweets_pkey
				do update set scheduled = excluded.scheduled , json_request = excluded.json_request , sent = excluded.sent
				""";
		Assert.hasText(jsonRequest, "you must provide a valid JSON fragment to send, per the Twitter API docs");
		Assert.hasText(twitterUsername,
				"you must provide a valid Twitter username (e.g.: @SpringTipsLive, or springtipslive)");
		Assert.hasText(clientId, "you must provide a valid clientId matching a record in twitter_clients");
		Assert.state(
				scheduled != null
						&& scheduled.after(new Date(System.currentTimeMillis() + (60 + 1000 + minimumRunwayInMinutes))),
				() -> String.format(
						"you must provide a valid scheduled date that is non-null and at least %s minutes into the future",
						minimumRunwayInMinutes));
		var executeSpec = this.dbc//
				.sql(sql)//
				.bind("username", username) //
				.bind("json_request", jsonRequest.trim())//
				.bind("scheduled", scheduled)//
				.bind("client_id", clientId);
		executeSpec = (sent == null) ? executeSpec.bindNull("sent", Date.class) : executeSpec.bind("sent", sent);
		return executeSpec//
				.fetch()//
				.rowsUpdated()//
				.map(c -> new ScheduledTweet(username, jsonRequest, scheduled, clientId, sent));
	}

}

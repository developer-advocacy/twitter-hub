package com.joshlong.twitter.tweets;

import com.joshlong.twitter.utils.TwitterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.joshlong.twitter.utils.DateUtils.dateFromLocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class SqlScheduledTweetService implements ScheduledTweetService {

	private final DatabaseClient dbc;

	private final TextEncryptor encryptor;

	private final Function<Map<String, Object>, ScheduledTweet> scheduledTweetMapper = new Function<>() {
		@Override
		public ScheduledTweet apply(Map<String, Object> record) {
			return new ScheduledTweet(//
					(String) record.get("username"), //
					(String) record.get("json_request"), //
					dateFromLocalDateTime(((LocalDateTime) record.get("scheduled"))), //
					(String) record.get("client_id"), //
					encryptor.decrypt((String) record.get("client_secret")), //
					record.containsKey("date") ? dateFromLocalDateTime((LocalDateTime) record.get("sent")) : null, //
					(String) record.get("id") //
			);
		}
	};//

	@Override
	public Flux<ScheduledTweet> due() {
		var sql = """
				SELECT
				    st.*
				FROM
				    twitter_scheduled_tweets st
				WHERE
				    st.sent is null
				AND
				    st.scheduled <= (select NOW() )
				""";
		return this.dbc.sql(sql).fetch().all().map(this.scheduledTweetMapper);
	}

	@Override
	public Mono<ScheduledTweet> send(ScheduledTweet tweet, Date sent) {
		Assert.notNull(sent, "you must provide the datetime at which point the ScheduledTweet was sent");
		return this.schedule(tweet.username(), tweet.jsonRequest(), tweet.scheduled(), tweet.clientId(),
				tweet.clientSecret(), sent);
	}

	@Override
	public Mono<ScheduledTweet> schedule(String twitterUsername, String jsonRequest, Date scheduled, String clientId,
			String clientSecret, Date sent) {
		var id = UUID.randomUUID().toString();
		var username = TwitterUtils.validateUsername(twitterUsername);
		var sql = """
				insert into twitter_scheduled_tweets (
				    id,
				    username,
				    json_request,
				    scheduled,
				    client_id,
				    client_secret,
				    sent
				)
				values (
				    :id,
				    :username,
				    :json_request,
				    :scheduled,
				    :client_id,
				    :client_secret,
				    :sent
				)
				on conflict on constraint twitter_scheduled_tweets_pkey
				do update set
				    scheduled = excluded.scheduled ,
				    json_request = excluded.json_request ,
				    sent = excluded.sent
				""";
		Assert.hasText(jsonRequest, "you must provide a valid JSON fragment to send, per the Twitter API docs");
		Assert.hasText(twitterUsername,
				"you must provide a valid Twitter username (e.g.: @SpringTipsLive, or springtipslive)");
		Assert.hasText(clientId, "you must provide a valid clientId matching a record in twitter_clients");
		var executeSpec = this.dbc//
				.sql(sql)//
				.bind("username", username) //
				.bind("json_request", jsonRequest.trim())//
				.bind("scheduled", scheduled)//
				.bind("id", id)//
				.bind("client_secret", this.encryptor.encrypt(clientSecret))//
				.bind("client_id", clientId);
		executeSpec = (sent == null) ? executeSpec.bindNull("sent", Date.class) : executeSpec.bind("sent", sent);
		var findByIdSql = """
				SELECT
				    st.*
				FROM
				    twitter_scheduled_tweets st
				WHERE st.id = :id
				""";
		return executeSpec//
				.fetch()//
				.rowsUpdated()//
				.flatMapMany(c -> dbc //
						.sql(findByIdSql)//
						.bind("id", id) //
						.fetch() //
						.all() //
						.map(this.scheduledTweetMapper)//
				) //
				.singleOrEmpty();

	}

}

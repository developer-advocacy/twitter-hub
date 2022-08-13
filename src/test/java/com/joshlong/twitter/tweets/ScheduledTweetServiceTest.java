package com.joshlong.twitter.tweets;

import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@SpringBootTest
class ScheduledTweetServiceTest {

	private final ScheduledTweetService scs;

	private final DatabaseClient dbc;

	private final ClientService clients;

	private final TwitterRegistrationService registrations;

	ScheduledTweetServiceTest(@Autowired DatabaseClient databaseClient,
			@Autowired TwitterRegistrationService registration, @Autowired ClientService clients,
			@Autowired ScheduledTweetService scs) {
		this.scs = scs;
		this.dbc = databaseClient;
		this.clients = clients;
		this.registrations = registration;
	}

	@BeforeEach
	void before() {
		var reset = this.dbc.sql("truncate table twitter_scheduled_tweets").fetch().rowsUpdated();
		StepVerifier.create(reset).expectNextMatches(c1 -> true).verifyComplete();
		StepVerifier.create(this.registrations.register("@tEST_twitter_userNAME", "at", "rt"))
				.expectNextMatches(tr -> tr.username().equals("test_twitter_username")).verifyComplete();
		StepVerifier.create(this.clients.register("test_client", "pw"))
				.expectNextMatches(c -> c.clientId().equals("test_client")).verifyComplete();
	}

	@Test
	void unscheduled() {
		var json = """
				{ "text" : "Hi, Spring fans!" }
				""";
		var tomorrow = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
		var scheduled = this.scs.schedule("@TEST_TWITTER_USERNAME", json, tomorrow, "test_client", "test_client_secret",
				null);
		var unscheduled = this.scs.due();
		var match = unscheduled.filter(st -> st.scheduled().equals(tomorrow) && st.clientId().equals("test_client")
				&& st.clientSecret().equals("test_client_secret") && st.username().equals("test_twitter_username")
				&& st.jsonRequest().equals(json.trim()));
		StepVerifier.create(scheduled.thenMany(match))//
				.expectNextCount(1)//
				.verifyComplete();
		var unscheduledAfterSending = this.scs
				.send(new ScheduledTweet("@TEST_TWITTER_USernAME", json, tomorrow, "test_client", null, null, "242232"),
						new Date())
				.thenMany(this.scs.due());
		StepVerifier.create(unscheduledAfterSending).expectComplete().verify();
	}

}
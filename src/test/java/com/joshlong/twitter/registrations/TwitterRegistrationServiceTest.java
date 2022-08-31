package com.joshlong.twitter.registrations;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.StringUtils;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@SpringBootTest
class TwitterRegistrationServiceTest {

	private final DatabaseClient databaseClient;

	private final TwitterRegistrationService repository;

	TwitterRegistrationServiceTest(@Autowired DatabaseClient databaseClient,
			@Autowired TwitterRegistrationService repository) {
		this.databaseClient = databaseClient;
		this.repository = repository;
	}

	@BeforeEach
	void before() {
		StepVerifier//
				.create(this.databaseClient//
						.sql("delete from twitter_accounts where username = :username")//
						.bind("username", "test")//
						.fetch() //
						.rowsUpdated()//
				)//
				.expectNextMatches(c -> true) //
				.verifyComplete();
	}

	@Test
	void register() {
		log.debug("start");
		StepVerifier//
				.create(this.repository.register("@TesT", "at", "rt"))//
				.expectNextMatches(tr -> tr.accessToken().equals("at") && tr.username().equals("test")) //
				.verifyComplete();
		log.debug("basics");
		StepVerifier.create(this.repository.byUsername("test")) //
				.expectNextMatches(tr -> StringUtils.hasText(tr.username()) && StringUtils.hasText(tr.accessToken())) //
				.verifyComplete();
		log.debug("by username ");

		var date = this.databaseClient //
				.sql("select * from twitter_accounts where username = :username")//
				.bind("username", "test")//
				.fetch()//
				.all()//
				.map(map -> (LocalDateTime) map.get("created"));
		StepVerifier //
				.create(date) //
				.expectNextMatches(Objects::nonNull)//
				.verifyComplete();
		log.debug("by date ");

		StepVerifier//
				.create(this.repository.register("@TEST", "at1", "rt1"))//
				.expectNextMatches(tr -> tr.accessToken().equals("at1") && tr.username().equals("test")) //
				.verifyComplete();
		var count = this.databaseClient//
				.sql("select count(*) as count from twitter_accounts where username = :username")//
				.bind("username", "test")//
				.fetch()//
				.all()//
				.map(map -> (Number) map.get("count")) //
				.map(Number::intValue);
		StepVerifier.create(count).expectNextMatches(c -> c == 1).verifyComplete();
		log.debug("by count ");

	}

}
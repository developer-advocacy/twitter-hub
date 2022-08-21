package com.joshlong.twitter.clients;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.test.StepVerifier;

@Slf4j
@SpringBootTest
class ClientServiceTest {

	private final DatabaseClient databaseClient;

	private final PasswordEncoder passwordEncoder;

	private final ClientService clientService;

	ClientServiceTest(@Autowired DatabaseClient databaseClient, @Autowired PasswordEncoder passwordEncoder,
			@Autowired ClientService clientService) {
		this.databaseClient = databaseClient;
		this.passwordEncoder = passwordEncoder;
		this.clientService = clientService;
	}

	@BeforeEach
	void before() {
		var reset = this.databaseClient //
				.sql("delete from twitter_scheduled_tweets; ") //
				.fetch()//
				.rowsUpdated() //
				.then(this.databaseClient.sql("delete from twitter_clients; ") //
						.fetch() //
						.rowsUpdated() //
				) //
				.then();
		StepVerifier.create(reset).verifyComplete();
	}

	@Test
	void clients() {
		log.info("encoded pw: " + this.passwordEncoder.encode("pw"));
		log.info("encoded pw1: " + this.passwordEncoder.encode("pw1"));
		var query = this.clientService.clients()//
				.filter(c -> c.id().equals("client") && c.secret().equals(this.passwordEncoder.encode("pw"))) //
				.count();
		StepVerifier.create(this.clientService.register("client", "pw").then(query)).expectNextCount(1)
				.verifyComplete();
		var update = this.clientService //
				.register("client", "pw1")//
				.thenMany(this.clientService.authenticate("client", "pw1")) //
				.count();
		StepVerifier.create(update) //
				.expectNext(1L) //
				.verifyComplete();
		var authenticated = this.clientService //
				.authenticate("client", "pw1");
		StepVerifier //
				.create(authenticated.filter(c -> c.id().equals("client"))) //
				.expectNextMatches(c -> c.id().equalsIgnoreCase("client")) //
				.verifyComplete();

	}

}
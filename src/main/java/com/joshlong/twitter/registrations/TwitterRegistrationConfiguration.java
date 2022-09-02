package com.joshlong.twitter.registrations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
class TwitterRegistrationConfiguration {

	@Bean
	ApplicationRunner registrationRunner(TwitterRegistrationService registrations,
			@Value("${debug:true}") boolean debug) {
		return args -> {
			if (debug) {
				registrations.registrations().subscribe(tr -> log.info("" + Map.of("username", tr.username(),
						"accessToken", (tr.accessToken()), "accessTokenSecret", (tr.accessTokenSecret()))));
			}
		};
	}

}

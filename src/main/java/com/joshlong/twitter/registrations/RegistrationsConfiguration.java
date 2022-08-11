package com.joshlong.twitter.registrations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
class RegistrationsConfiguration {

	@Bean
	ApplicationRunner registrationRunner(TwitterRegistrationService registrations,
			@Value("${debug:false}") boolean debug) {
		return args -> {
			if (debug) {
				registrations.registrations().subscribe(tr -> log.info(tr.toString()));
			}
		};
	}

}

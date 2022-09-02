package com.joshlong.twitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

/**
 * @author Josh Long
 */
@Slf4j
@EnableConfigurationProperties(TwitterProperties.class)
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	WebClient webClient(WebClient.Builder webClient) {
		return webClient.build();
	}

	@Bean
	TextEncryptor encryptor(TwitterProperties properties) {
		return Encryptors.delux(properties.encryption().password(), properties.encryption().salt());
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@EventListener
	public void liveness(AvailabilityChangeEvent<LivenessState> live) {
		log.info("liveness: " + live.toString());
	}

	@EventListener
	public void readiness(AvailabilityChangeEvent<ReadinessState> live) {
		log.info("readiness: " + live.toString());
	}

	@Bean
	WebFilter loggingWebFilter() {
		return (exchange, chain) -> {
			exchange.getResponse().getHeaders().forEach((k, v) -> log.info("header: " + k + "=" + v));
			return chain.filter(exchange);
		};
	}

	@Bean
	TwitterApiIntegration integration(TwitterProperties properties, ClientService clientService,
			TwitterRegistrationService registrations) {
		return new TwitterApiIntegration(clientService, registrations, properties.app().clientId(),
				properties.app().clientSecret());
	}

}

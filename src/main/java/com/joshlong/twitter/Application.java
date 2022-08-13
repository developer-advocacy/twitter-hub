package com.joshlong.twitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
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
	TwitterApiIntegration integration(
			@Value("${spring.security.oauth2.client.registration.twitter.client-id}") String clientId,
			@Value("${spring.security.oauth2.client.registration.twitter.client-secret}") String clientSecret,
			WebClient http, ClientService clientService, ObjectMapper om, TwitterRegistrationService registrations) {
		return new TwitterApiIntegration(clientId, clientSecret, http, clientService, registrations, om);
	}

}

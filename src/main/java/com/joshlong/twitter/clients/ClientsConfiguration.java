package com.joshlong.twitter.clients;

import com.joshlong.twitter.TwitterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@Configuration
class ClientsConfiguration {

	@Bean
	ApplicationRunner clientInitialization(@Value("${debug:false}") boolean debug, ClientService clientService,
			TwitterProperties properties) {
		return args -> {
			if ((properties.clients() != null && properties.clients().length > 0)) {
				Flux//
						.fromArray(properties.clients()) //
						.flatMap(c -> {
							var clientId = c.clientId();
							var secret = c.secret();
							var valueToPrintForSecret = debug ? secret : repeat('*', secret.length());
							return clientService //
									.register(clientId, secret) //
									.doOnNext(cc -> log.info(String.format("registering client %s with secret %s",
											clientId, valueToPrintForSecret)));
						})//
						.subscribe();

			} //
			else {
				log.warn("there are no clients to register. Returning.");
			}

		};
	}

	private static String repeat(char c, int times) {
		var chars = new char[times];
		Arrays.fill(chars, c);
		return new String(chars);
	}

}

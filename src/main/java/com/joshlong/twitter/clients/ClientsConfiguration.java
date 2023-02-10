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
			if (properties.clients() != null) {
				var length = properties.clients().length;
				if (length > 0) {
					log.info("need to register " + length + " client");
					Flux//
							.fromArray(properties.clients()) //
							.flatMap(c -> {
								var keep = 4;
								var clientId = c.id();
								var secret = c.secret();// todo
								var valueToPrintForClientId = debug ? clientId
										: clientId.substring(0, keep) + repeat('*', clientId.length() - keep);
								var valueToPrintForSecret = debug ? secret
										: secret.substring(0, keep) + repeat('*', secret.length() - keep);
								return clientService //
										.register(clientId, secret) //
										.doOnNext(cc -> log.info(String.format("registering client %s with secret %s",
												valueToPrintForClientId, valueToPrintForSecret)));
							})//
							.subscribe();
				} //

			}
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

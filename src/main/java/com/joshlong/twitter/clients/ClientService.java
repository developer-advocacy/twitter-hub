package com.joshlong.twitter.clients;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientService {

	Mono<Client> register(String clientId, String secret);

	Flux<Client> clients();

	Mono<Client> authenticate(String clientId, String secret);

}

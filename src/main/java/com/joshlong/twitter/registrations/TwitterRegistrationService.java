package com.joshlong.twitter.registrations;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TwitterRegistrationService {

	Flux<TwitterRegistration> registrations();

	Mono<TwitterRegistration> byUsername(String username);

	Mono<TwitterRegistration> register(String username, String accessToken, String accessTokenSecret);

}

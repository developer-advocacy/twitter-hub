package com.joshlong.twitter.registrations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Controller
@ResponseBody
@RequiredArgsConstructor
class TwitterRegistrationController {

	private final TwitterRegistrationService registrationService;

	@PostMapping(value = "/register", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	Mono<Void> register(@RequestBody Map<String, String> registration) {
		var username = registration.get("username");
		var at = registration.getOrDefault("accessToken", registration.getOrDefault("access_token", null));
		var ats = registration.getOrDefault("accessTokenSecret",
				registration.getOrDefault("access_token_secret", null));
		for (var s : new String[] { username, at, ats })
			Assert.hasText(s, "you have not provided all required values");
		log.debug("got a request to register: [" + registration + "]");
		var result = this.registrationService.register(username, at, ats);
		return result.flatMap(tr -> Mono.empty());

	}

}

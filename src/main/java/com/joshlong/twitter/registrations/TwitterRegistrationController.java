package com.joshlong.twitter.registrations;

import com.joshlong.twitter.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
class TwitterRegistrationController {

	@GetMapping("/register")
	ModelAndView register(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient twitter, Principal principal) {
		return new ModelAndView("registration", Map.of("username", SecurityUtils.extractTwitterUsername(principal)));
	}

}

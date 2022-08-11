package com.joshlong.twitter.utils;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.security.Principal;
import java.util.Map;

public abstract class SecurityUtils {

	public static String extractTwitterUsername(Principal principal) {

		if (principal instanceof OAuth2AuthenticationToken auth2AuthenticationToken) {
			var data = auth2AuthenticationToken.getPrincipal().getAttributes().get("data");
			if (data instanceof Map<?, ?> map) {
				var username = (String) map.get("username");
				return (TwitterUtils.validateUsername(username));
			}
		} //

		return null;
	}

}

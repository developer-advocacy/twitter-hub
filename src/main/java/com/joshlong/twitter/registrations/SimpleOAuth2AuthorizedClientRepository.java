package com.joshlong.twitter.registrations;

import com.joshlong.twitter.utils.TwitterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
class SimpleOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

	private final TwitterRegistrationService registrationService;

	private final Map<String, OAuth2AuthorizedClient> clients = new ConcurrentHashMap<>();

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal,
			HttpServletRequest request, HttpServletResponse response) {
		this.clients.put(authorizedClient.getClientRegistration().getRegistrationId(), authorizedClient);
		var refreshToken = authorizedClient.getRefreshToken();
		var accessToken = authorizedClient.getAccessToken();
		if (principal instanceof OAuth2AuthenticationToken auth2AuthenticationToken) {
			var data = auth2AuthenticationToken.getPrincipal().getAttributes().get("data");
			if (data instanceof Map<?, ?> map) {
				var username = (String) map.get("username");
				// todo could i turn this into something properly reactive? Maybe use
				// spring cloud function to reactively write this request to a queue and
				// then consume it in the same VM?
				this.registrationService
						.register(TwitterUtils.validateUsername(username), accessToken.getTokenValue(),
								Objects.requireNonNull(refreshToken).getTokenValue()) //
						.block(); // todo ugh
			}
		} //
		else {
			Assert.state(false, "how did we get to this point?");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
			Authentication principal, HttpServletRequest request) {
		return (T) this.clients.getOrDefault(clientRegistrationId, null);
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, Authentication principal,
			HttpServletRequest request, HttpServletResponse response) {
		if (!this.clients.isEmpty())
			this.clients.remove(clientRegistrationId);
	}

}

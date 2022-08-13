package com.joshlong.twitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;

/**
 * @author Rob Winch
 */
@Slf4j
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

	@Bean
	TextEncryptor encryptor(TwitterProperties properties) {
		return Encryptors.noOpText(); // Encryptors.delux(
		// properties.encryption().password(), properties.encryption().salt());
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	DefaultSecurityFilterChain springSecurity(HttpSecurity http, //
			OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository, //
			OAuth2AuthorizationRequestResolver authorizationResolver) throws Exception {
		http//
				.authorizeHttpRequests(requests -> requests //
						.mvcMatchers("/oauth2/authorization", "/register").authenticated() //
						.anyRequest().permitAll())//
				.oauth2Login(oauth2 -> oauth2.authorizedClientRepository(oAuth2AuthorizedClientRepository)
						.authorizationEndpoint(
								authorization -> authorization.authorizationRequestResolver(authorizationResolver)));
		return http.build();
	}

	@Bean
	OAuth2AuthorizationRequestResolver authorizationRequestResolver(
			ClientRegistrationRepository clientRegistrationRepository) {
		var authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
				"/oauth2/authorization");
		authorizationRequestResolver
				.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
		return authorizationRequestResolver;
	}

}

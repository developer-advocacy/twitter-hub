package com.joshlong.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.tweets.ScheduledTweet;
import com.joshlong.twitter.tweets.ScheduledTweetService;
import com.joshlong.twitter.utils.DateUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import java.util.Date;
import java.util.UUID;

@Configuration
@Slf4j
class InboundIntegrationConfiguration {

	@Bean
	IntegrationFlow inboundIntegrationFlow(ObjectMapper objectMapper, ScheduledTweetService service,
			ConnectionFactory connectionFactory) {
		return IntegrationFlows //
				.from(Amqp.inboundAdapter(connectionFactory, "twitter-requests-queue"))//
				.handle(String.class, (payload, headers) -> {
					twitterRequests(payload, objectMapper, service);
					return null;
				}) //
				.get();

	}

	@Bean
	Binding binding() {
		return BindingBuilder.bind(queue()).to(exchange()).with("twitter-requests").noargs();
	}

	@Bean
	Exchange exchange() {
		return ExchangeBuilder.directExchange("twitter-requests").durable(true).build();
	}

	@Bean
	Queue queue() {
		return QueueBuilder.durable("twitter-requests-queue").build();
	}

	private static void twitterRequests(String message, ObjectMapper objectMapper, ScheduledTweetService service) {
		var unparsedPayload = message;
		log.info("new payload: " + unparsedPayload);
		var payload = parseJsonIntoTweetRequest(objectMapper, message.getPayload());
		var scheduledTweet = new ScheduledTweet(payload.twitterUsername(), payload.text(), payload.media(),
				payload.scheduled(), payload.clientId(), payload.clientSecret(), null, UUID.randomUUID().toString());
		service.schedule(scheduledTweet, null).block();
	}

	@SneakyThrows
	private static TweetRequest parseJsonIntoTweetRequest(ObjectMapper objectMapper, String json) {
		var jn = objectMapper.readValue(json, JsonNode.class);
		var scheduledNode = jn.has("scheduled") ? jn.get("scheduled") : null;
		var scheduled = (null == scheduledNode) ? new Date() : DateUtils.readIsoDateTime(scheduledNode.textValue());
		return new TweetRequest(//
				jn.get("clientId").textValue(), //
				jn.get("clientSecret").textValue(), //
				jn.get("twitterUsername").textValue(), //
				jn.get("text").textValue(), //
				jn.get("media").textValue(), //
				scheduled);
	}

}

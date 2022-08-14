# Twitter Service 1.0

## Design

This service centralizes logic around tweeting for my various services. 

The application should allow a user to login to Twitter, thus preserving their access token and refresh token in our database. Spring Security is used here to allow the user to do login, to seed an access token and a refresh token. We'll need to handle storing those values (in an encrypted fashion) in a PostgreSQL database of some sort.

Ideally, we should have a PostgreSQL table  (`twitter_accounts`) that looks like this: 

```shell
------------------------------------------------------------------------
twitter_username    | access_token            | refresh_token
------------------------------------------------------------------------
starbuxman         | fejsfljfdosjd3hdkls...  | 6MToxOnJ0OjEfdkf..
springtipslive     | ewef3424fsds...         | dffgd64jkxvh93...
```

The service accepts new requests on a RabbitMQ exchange. Each request should contain:

* the JSON data of the request to send to the Twitter v2 `/v2/tweets` endpoint 
* the username (e.g.: `@starbuxman`)
* the datetime in ISO 8601 format specifying when the tweet should be sent 
* some sort of shared secret between the `twitter-service` and the client, sent perhaps as a header. This shared secret should have the effect of identifying the client making the request. 

If the shared secret is validated, then the data in the request is written to a database table (`twitter_scheduled_tweets`) that looks something like this:


```shell
-------------------------------------------------------------------------------------------------------------------------------- 
twitter_username    | json_request                | datetime                | client-id                   | datetime_sent
-------------------------------------------------------------------------------------------------------------------------------- 
starbuxman          | {"text": "Hello, world!"}   | 2019-09-07T-15:50+00	| youtube-promotion-service   | null
```

Finally, a scheduled background thread will run, let's say, every five minutes and find all the tweets scheduled to be sent between now and five minutes from now and send them. As each one is successfully sent, it must update the table, setting `datetime_sent` to a non-null ISO 8601 datetime. 

### Security 

We'll need a mechanism for adding clients to the system. For a first cut, perhaps we could just manually add clients and passwords to a `clients` table?


```shell
---------------------------------------------------  
client                        | secret 
--------------------------------------------------- 
youtube-promotion-service     | dffshhsdi3453oui
```

The passwords here are _not_ plaintext, and must be encrypted using a salt value that's provided at application startup time and kept only in memory.

The `access_token` and `refresh_token` values too must be encrypted using a salt value that's provided at application startup time and kept only in memory. Tweets must never be sent if:
* a request specifies a client that doesn't exist 
* if it fails to specify a client 
* if the client password - once encrypted - does not match the password stored in the database   

### Twitter 

We're using Spring Security's PKCE support to login and incept an access token and refresh token. We'll need to develop a simple Twitter client to support posting to a given HTTP endpoint with the right access token and gracefully handling the situation where we need to use the refresh token to source a new access token. Here's documentation on how to [incorporate the use of refresh tokens](https://developer.twitter.com/en/docs/authentication/oauth-2-0/authorization-code) in the Twitter API. It has a usable `curl` incantation.



## Getting Started

Go to  and get [a developer account setup](https://developer.twitter.com/en/portal/petition/essential/basic-info). After confirming your email, you can enter an `App Name`. Copy your `API Key` and `Secret` and save them. Visit the [dashboard](https://developer.twitter.com/en/portal/dashboard) and select the settings of your newly created app . Click `Set up` next to `Authentication not set up`.

* Enable `OAuth 2.0`.  
* Type of App: `Web App`.  
* Callback URI / Redirect URL: `http://localhost:8080/login/oauth2/code/twitter`
* Website URL: https://spring.io

Copy the client ID and client Secret and save them. They need to be plugged into this service's `application.yaml` file for the `spring.security.oauth2.client.registration.twitter.{client-id,client-secret}` 

## Sending a Request 

You'll need to send a valid message to the RabbitMQ exchange bound (by default, it's `twitter-requests`) to this application. (See `application.properties`)

The payload should look something like this: 

```json 
{"clientId":"test-client","clientSecret":"1234","twitterUsername":"bpmpass","jsonRequest":" { \"text\" : \"Hello rmq-sent message  2022-08-08T11:27:13.773743Z!\" }\n"}
```

If you're using Spring Cloud Stream then you might use code like this: 

```java

    Mono<Boolean> send() {
        var bindingNameForOutput = "twitterRequests-out-0";
        var twitterApiRequestJsonPayload = String.format("""
                 { "text" : "Hello HTTP controller-sent message  %s!" }
                """, Instant.now().toString());
        var payload = Map.of("clientId", "test-client", //
                "clientSecret", "1234", //
                "twitterUsername", "bpmpass", //
                "jsonRequest", twitterApiRequestJsonPayload //
        );
        var send = this.streamBridge.send(bindingNameForOutput, new GenericMessage<>(payload));
        return Mono.just(send);
    }

```

This assumes you've paired that Java code with appropriate configuration, which might look like this:

```properties 
spring.cloud.stream.bindings.twitterRequests-out-0.destination=twitter-requests
```

And that in turn assumes that `twitter-requests` is the name of the exchange you've specified to accept requests.

## Resources 

* [A Spring Security Issue](https://github.com/spring-projects/spring-security/issues/6548#issuecomment-1076200345)
* [Information on customizing the authorization grant](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html#_customizing_the_authorization_request)
* The docs say that the [resulting token is only good for two hours ](https://developer.twitter.com/en/docs/authentication/oauth-2-0/authorization-code).
* Apparently it's also possible [to use OAuth 1.0](https://developer.twitter.com/en/docs/authentication/oauth-1-0a), but uh.. why? 
* The [API endpoint to post Tweets](https://developer.twitter.com/en/docs/twitter-api/tweets/manage-tweets/api-reference/post-tweets)

## Inspecting it 

This application runs on the `joshlong-dot-com` Kubernetes cluster. You can login to that cluster and then use the following command to inspect the running objects: 

```shell
k get deployments/twitter ingress/twitter-ingress ManagedCertificate/twitter-certificate  service/twitter
``` 

## Resetting the DB

```sql
drop table twitter_scheduled_tweets, twitter_accounts, twitter_clients cascade 
```
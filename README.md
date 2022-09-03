# Twitter Gateway 0.0.1



## Design

This service centralizes logic around tweeting for my various services. 

The application should allow a user to login to Twitter, thus preserving their access token and refresh token in our database. Spring Security is used here to allow the user to do login, to seed an access token and a refresh token. We'll need to handle storing those values (in an encrypted fashion) in a PostgreSQL database of some sort.

Ideally, we should have a PostgreSQL table  (`twitter_accounts`) that looks like this: 

```shell
------------------------------------------------------------------------
twitter_username    | access_token            | access_token_secret
------------------------------------------------------------------------
starbuxman         | fejsfljfdosjd3hdkls...  | 6MToxOnJ0OjEfdkf..
springtipslive     | ewef3424fsds...         | dffgd64jkxvh93...
```

The service accepts new requests on a RabbitMQ exchange. Each request should contain:

* the JSON data of the request to send to the Twitter v1 endpoints
* the username (e.g.: `@starbuxman`)
* the datetime in ISO 8601 format specifying when the tweet should be sent 
* some sort of shared secret between the `twitter-gateway` and the client, sent perhaps as a header. This shared secret should have the effect of identifying the client making the request. 

If the shared secret is validated, then the data in the request is written to a database table (`twitter_scheduled_tweets`) that looks something like this:


```shell
-------------------------------------------------------------------------------------------------------------------------------- 
twitter_username    | tweet_text    | datetime             | client-id                 | tweet_media | ...
-------------------------------------------------------------------------------------------------------------------------------- 
starbuxman          | Hello, world! | 2019-09-07T-15:50+00 | youtube-promotion-service | null        | ... 
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

There's a ridiculous sitution in the Twitterverse at the moment (2022-08) where: 

* you can sign up for a developer account and get access to `Essentials` which limits you to only the v2 APIs  
* there are a number of APIs that don't have parity in the v2 APIs, so you have to use the v1 APIs. e.g.: tweeting with an uploaded image. 
* you _can_ acccess the v1 APIs if you have `Elevated` access, but that requires a tedious form that somebody at Twitter will have to read and agree for you 


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
{"clientId":"test-client","clientSecret":"1234","twitterUsername":"bpmpass","text":"Hello rmq-sent message  2022-08-08"}
```

I've extracted out all the logic around how to issue requests over RabbitMQ to be consumed by this gateway into [a separate module](https://github.com/developer-advocacy/twitter-gateway-client), that in turn has its own Spring Boot autoconfiguration. Bring in the dependency thusly: 

```xml
<dependency>
    <groupId>com.joshlong.twitter</groupId>
    <artifactId>twitter-gateway-client</artifactId>
    <version>0.0.2-SNAPSHOT</version>
</dependency>
```


## Seeding the `twitter_accounts` table. 

The gateway works by issuing tweets for you on behalf of certain Twitter accounts. You'll need to ensure that you have valid access tokens and access tokens secrets in the database. 

You'll need to 

* create an application on the Twitter dev portal. 
* gather the app consumer keys api key and secret 
* then use [this handy tool `tw-oob-oauth-cli`](https://github.com/smaeda-ks/tw-oob-oauth-cli) to - from the command line, drive an authentication attempt with twitter's PIN-based OAuth system. It'll then dump out the access token and access token secret on the command line. Note them for later, along with the handle of the Twitter account. You kick off the application by downloading the relevant `go` binary and then running: `tw-oob-oauth --consumer-key value --consumer-secret value`
* then issue a (possibly authenticated) POST request against the Twitter gateway's `/register` endpoint with something like the following JSON in the body:

```json
{ 
   "username" : ... , 
   "access_token" : ... , 
   "access_token_secret" : .. 
}
```

You might use a `curl` command like this: 

```shell
curl  -H"content-type: application/json" -XPOST  -d'<JSON>' https://yourhost:port/register  
```

Be sure to replace `<JSON>` with the contents of the JSON structure described above. 

This will _encrypt_ the sensitive values - the `acces_token` and `access_token_secre` - before writing them to the DB. Ensure that you then promptly delete any files where you may have stored that JSON or those values earlier. 

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

## Security 

This needs to be pretty secure, by its nature.  It uses OAuth and PKCE to establish the connection with Twitter for the accounts the service manages. 

All access tokens and refresh tokens, which may be used again at some point in the future, are symmetrically encrypted with Spring Security's `TextEncryptor`. Likewise, the `clientSecret` sent in the request on RabbitMQ to create a `ScheduledTweet` is also encrypted so that while it lives in the `twitter_scheduled_tweets` table, it's not in plain-text. This encryptor is symmetrical: you can decrypt the values assuming you have a `TextEncryptor` configured in exactly the same was it when you encrypted the values. This means using the exact same `salt` and `password`.  The `password` and `salt` come from configuration which you're expected to override (use Kubernetes `Secret`s and `ConfigMap`s, Hashicorp Vault, Spring Cloud Config Server, or straight up environment variables as appropriate in your deployment). 

The clients defined in `twitter_clients` have a client ID and a secret. The secret is assymetrically encoded using BCrypt and is written to the DB in that form. When we decrypt the value from the `twitter_scheduled_tweet`, we then use it as a _raw_ value to test whether it matches the BCrypt value for the client in the `twitter_clients` table. 

It might be worth exploring whether we could do the authentication at the time of the request (as opposed to when the Tweet is actually sent out, at some point in the future). This way, we wouldn't need to store the client secret (even encrypted) in a symmetric fashion that could in theory be reversed if somebody got hold of the `salt` and `password`.


## Resetting the DB

Because the secure values are extraordinarily sensitive, if you change anything, anywhere, and don't note the change, you'll lose access to your data. If you're developing the code locally, make sure to take care to reset judiciously, or you'll get phantom errors about cryptography and padding and keys and what all. There's no coming back from that. You just need to reset, like this:

```sql
drop table twitter_scheduled_tweets, twitter_accounts, twitter_clients cascade 
```

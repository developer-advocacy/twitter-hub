package com.joshlong.twitter.registrations;

import com.joshlong.twitter.utils.DateUtils;
import com.joshlong.twitter.utils.TwitterUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
class SqlTwitterRegistrationService implements TwitterRegistrationService {

	private final DatabaseClient dbc;

	private final TextEncryptor encryptor;

	private final Function<Map<String, Object>, TwitterRegistration> mapTwitterRegistrationFunction = //
			record -> new TwitterRegistration((String) record.get("username"),
					decrypt((String) record.get("access_token")), decrypt((String) record.get("access_token_secret")),
					DateUtils.dateFromLocalDateTime((LocalDateTime) record.get("updated")));

	SqlTwitterRegistrationService(DatabaseClient dbc, TextEncryptor encryptor) {
		this.dbc = dbc;
		this.encryptor = encryptor;
	}

	private String decrypt(String encryptedText) {
		try {
			return this.encryptor.decrypt(encryptedText);
		} //
		catch (Throwable e) {
			if (log.isDebugEnabled())
				log.error("got an error trying to decrypt " + encryptedText, e);
		}
		return null;
	}

	@Override
	public Flux<TwitterRegistration> registrations() {
		return this.dbc.sql("select * from twitter_accounts") //
				.fetch() //
				.all() //
				.map(this.mapTwitterRegistrationFunction);
	}

	@Override
	public Mono<TwitterRegistration> byUsername(String username) {
		Assert.hasText(username, "the username must not be null");
		return this.dbc//
				.sql("select * from twitter_accounts where username = :un") //
				.bind("un", TwitterUtils.validateUsername(username)) //
				.fetch()//
				.one()//
				.map(this.mapTwitterRegistrationFunction)//
				.switchIfEmpty(Mono.error(new IllegalStateException("can't find one!")))
				.doOnNext(tr -> log.debug("found: " + tr.toString()))//
				.doOnError(e -> log.error("can't find a twitter_account with username @" + username + ".", e));
	}

	@Override
	public Mono<TwitterRegistration> register(String username, String accessToken, String accessTokenSecret) {
		var sql = """
				insert into twitter_accounts(username, access_token,access_token_secret,  created, updated) values( :username, :at, :ats , :created, :updated )
				on conflict on constraint twitter_accounts_pkey
				do update SET
				    access_token = excluded.access_token,
				    updated = excluded.updated
				""";
		log.info("========================================");
		log.info("access token: [" + accessToken + "]");
		log.info("access token secret: [" + accessTokenSecret + "]");

		var tr = new TwitterRegistration(TwitterUtils.validateUsername(username), accessToken, accessTokenSecret);
		var ts = new Date();
		return this.dbc.sql(sql)//
				.bind("username", tr.username())//
				.bind("at", encryptor.encrypt(accessToken)) //
				.bind("ats", encryptor.encrypt(accessTokenSecret))//
				.bind("created", ts)//
				.bind("updated", ts)//
				.fetch() //
				.rowsUpdated() //
				.flatMap(count -> this.byUsername(tr.username()));
	}

}

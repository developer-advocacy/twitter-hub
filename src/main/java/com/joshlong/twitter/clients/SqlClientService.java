package com.joshlong.twitter.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
class SqlClientService implements ClientService {

	private final DatabaseClient dbc;

	private final PasswordEncoder passwordEncoder;

	private final Function<Map<String, Object>, Client> clientFunction = record -> {
		log.debug("processing record " + record);
		return new Client((String) record.get("client_id"), (String) record.get("secret"));
	};

	@Override
	public Mono<Client> register(String clientId, String rawSecret) {
		var secret = this.passwordEncoder.encode(rawSecret);
		var sql = """
				insert into twitter_clients( client_id, secret) values( :clientId, :secret)
				on conflict on constraint twitter_clients_pkey do update set  secret = excluded.secret
				""";
		return this.dbc.sql(sql)//
				.bind("clientId", clientId)//
				.bind("secret", secret)//
				.fetch()//
				.rowsUpdated()//
				.map(c -> new Client(clientId, secret));

	}

	@Override
	public Flux<Client> clients() {

		return this.dbc.sql("select  * from twitter_clients") //
				.fetch() //
				.all() //
				.map(this.clientFunction);
	}

	@Override
	public Mono<Client> authenticate(String clientId, String secret) {
		log.debug("trying to authenticate [" + clientId + "] and [" + secret + "]");
		return this.dbc //
				.sql("select * from twitter_clients where client_id = :cid  ")//
				.bind("cid", clientId)//
				.fetch()//
				.all()//
				.map(stringObjectMap -> {
					log.debug("result: " + stringObjectMap);
					return clientFunction.apply(stringObjectMap);
				})//
				.filter(c -> this.passwordEncoder.matches(secret, c.secret())) //
				.singleOrEmpty()//
				.switchIfEmpty(Mono.error(
						new IllegalStateException("could not find a valid client of the name '" + clientId + "'!")));

	}

}

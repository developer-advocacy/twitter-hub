package com.joshlong.twitter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Slf4j
class PasswordEncryptionTests {

	private final PasswordEncoder passwordEncoder;

	PasswordEncryptionTests(@Autowired PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Test
	void symmetric() {
		var encryptor = Encryptors.text("pw", KeyGenerators.string().generateKey());
		var message = "Hello, world!";
		var encrypted = encryptor.encrypt(message);
		var decrypted = encryptor.decrypt(encrypted);
		Assertions.assertEquals(encryptor.decrypt(encrypted), decrypted);
	}

	@Test
	void assymetric() {
		var encoded = this.passwordEncoder.encode("1234");
		log.info("encoded: " + encoded);
		var match = this.passwordEncoder.matches("1234", encoded);
		Assertions.assertTrue(match, "the passwords should match");
	}

}

package com.joshlong.twitter.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class StringUtilsTest {

	@Test
	void mask() {

		try {
			StringUtils.securityMask(null);
			Assertions.fail("you should never get here!");
		} //
		catch (IllegalArgumentException throwable) {
			// works as expected
		}
		Assertions.assertEquals(StringUtils.securityMask("a"), "*");
		Assertions.assertEquals(StringUtils.securityMask("123"), "***");
		Assertions.assertEquals(StringUtils.securityMask("12345"), "***");
		Assertions.assertEquals(StringUtils.securityMask("123456"), "12**56");
		Assertions.assertEquals(StringUtils.securityMask("1234567890"), "12******90");

	}

}
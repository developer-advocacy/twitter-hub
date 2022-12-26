package com.joshlong.twitter.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
class InstantUtilsTest {

	private final ZoneId gmt = ZoneId.of("GMT");

	@Test
	void parse8601() throws Exception {
		log(InstantUtils.parseIso8601String("2022-03-11T13:05:05.000+0000"));
		var instant = InstantUtils.parseIso8601String("2022-03-12T11:33:20.000+00");
		log(instant);
		var dateTime = from(instant);
		Assertions.assertEquals(dateTime.getMinute(), 33);
		Assertions.assertEquals(dateTime.getSecond(), 20);
		Assertions.assertEquals(dateTime.getHour(), 11);
		Assertions.assertEquals(dateTime.getYear(), 2022);
		Assertions.assertEquals(dateTime.getMonth().getValue(), 3);
		Assertions.assertEquals(dateTime.getDayOfMonth(), 12);
	}

	private LocalDateTime from(Instant instant) {
		return LocalDateTime.ofInstant(instant, gmt);
	}

	private void log(Instant instant) {
		var ldt = LocalDateTime.ofInstant(instant, gmt);
		var h = ldt.getHour();
		var m = ldt.getMinute();
		var s = ldt.getSecond();
		var y = ldt.getYear();
		var mo = ldt.getMonth().getValue();
		var d = ldt.getDayOfMonth();
		var ds = "" + y + "/" + mo + "/" + d;
		var ts = h + ":" + m + ":" + s;
		var result = ds + " " + ts;
		log.info(result);

	}

}
package com.joshlong.twitter.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalField;

@Slf4j
class DateUtilsTest {

	@Test
	void parseIsoDateTime() {
		var date = DateUtils.readIsoDateTime("2022-02-13T13:35:09.840Z");
		log.info(date.toString());
	}

}
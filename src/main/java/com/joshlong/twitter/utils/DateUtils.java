package com.joshlong.twitter.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public abstract class DateUtils {

	public static Date readIsoDateTime(String s) {
		var ta = DateTimeFormatter.ISO_INSTANT.parse(s);
		var i = Instant.from(ta);
		return Date.from(i);

	}

	public static Date dateFromLocalDateTime(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		var instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
		return Date.from(instant);
	}

}

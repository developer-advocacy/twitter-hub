package com.joshlong.twitter.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public abstract class DateUtils {

	public static java.util.Date readIsoDateTime(String s) {
		var ta = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(s);
		var i = LocalDateTime.from(ta).atZone(ZoneId.systemDefault()).toInstant();
		return Date.from(i);
	}

	public static Date dateFromLocalDateTime(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		var instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
		return Date.from(instant);
	}

}

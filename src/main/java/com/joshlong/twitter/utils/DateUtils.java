package com.joshlong.twitter.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public abstract class DateUtils {

	public static Date dateFromLocalDateTime(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		var instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
		return Date.from(instant);
	}

}

package com.joshlong.twitter.utils;

import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public abstract class Base64Utils {

	@SneakyThrows
	public static InputStream decode(String media) {
		if (org.springframework.util.StringUtils.hasText(media)) {
			var decoded = org.springframework.util.Base64Utils.decodeFromString(media);
			return new ByteArrayInputStream(decoded);
		}
		return null;
	}

	@SneakyThrows
	public static String encode(InputStream file) {
		if (null == file)
			return null;
		var inResource = new InputStreamResource(file);
		try (var f = inResource.getInputStream()) {
			var content = FileCopyUtils.copyToByteArray(f);
			return org.springframework.util.Base64Utils.encodeToString(content);
		}
	}

}

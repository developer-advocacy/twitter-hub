package com.joshlong.twitter.utils;

import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

public abstract class Base64Utils {

	@SneakyThrows
	public static InputStream decode(String media) {
		if (org.springframework.util.StringUtils.hasText(media)) {
			var decoded = decodeFromString(media);
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
			return encodeToString(content);
		}
	}

	private static String encodeToString(byte[] src) {
		if (src.length == 0) {
			return "";
		}
		return Base64.getEncoder().encodeToString(src);
	}

	private static byte[] decodeFromString(String src) {
		if (src.isEmpty()) {
			return new byte[0];
		}
		return Base64.getDecoder().decode(src);
	}

}

package com.joshlong.twitter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@ResponseBody
class DefaultHelloController {

	@GetMapping("/")
	Map<String, String> hello() {
		return Map.of("message", "Hello, world!");
	}

}

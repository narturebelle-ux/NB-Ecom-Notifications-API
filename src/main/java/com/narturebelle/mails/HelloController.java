package com.narturebelle.mails;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Connection check: confirms a request reached this API. */
@RestController
@CrossOrigin(originPatterns = "http://localhost:*")
public class HelloController {

    @GetMapping("/api/v1/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello Emails", "service", "NB-Ecom-Notifications-API");
    }
}

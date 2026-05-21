package com.exe101.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CorsOriginProperties {

    private final List<String> allowedOriginPatterns;

    public CorsOriginProperties(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://localhost:3000,http://localhost:5173,https://exe-fe-gold.vercel.app,https://pawply.site,https://www.pawply.site,https://exe-fe-pink.vercel.app,https://*.vercel.app}") String allowedOriginPatterns
    ) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList();
    }

    public String[] asArray() {
        return allowedOriginPatterns.toArray(String[]::new);
    }

    public List<String> asList() {
        return allowedOriginPatterns;
    }
}

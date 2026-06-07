package com.compass.digitalbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Account account) {

    public record Jwt(String secret, long expirationMs) {
    }

    public record Account(BigDecimal maxInitialBalance) {
    }
}

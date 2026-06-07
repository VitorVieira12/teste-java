package com.compass.digitalbank.security;

import com.compass.digitalbank.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("ZGlnaXRhbGJhbmtTdXBlclNlY3JldEtleUZvckp3dDIwMjQhIQ==", 3600000L),
                new AppProperties.Account(java.math.BigDecimal.valueOf(1000)));
        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken("alice", 1L);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractAccountId(token)).isEqualTo(1L);
        assertThat(jwtService.isTokenValid(token, new AuthenticatedAccount(1L, "alice", "pwd"))).isTrue();
    }
}

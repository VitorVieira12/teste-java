package com.compass.digitalbank.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long accountId
) {

    public static AuthResponse of(String token, long expiresInMs, Long accountId) {
        return new AuthResponse(token, "Bearer", expiresInMs, accountId);
    }
}

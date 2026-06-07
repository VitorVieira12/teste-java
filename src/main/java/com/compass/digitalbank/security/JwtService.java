package com.compass.digitalbank.security;

import com.compass.digitalbank.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(AppProperties appProperties) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(appProperties.jwt().secret()));
        this.expirationMs = appProperties.jwt().expirationMs();
    }

    public String generateToken(String username, Long accountId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("accountId", accountId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractAccountId(String token) {
        return extractAllClaims(token).get("accountId", Long.class);
    }

    public boolean isTokenValid(String token, AuthenticatedAccount account) {
        String username = extractUsername(token);
        return username.equals(account.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}

package com.owlexa.owlexabackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET = "owlexa-secret-key-owlexa-secret-key";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static final long ACCESS_TOKEN_EXPIRE_MS = 1000 * 60 * 15; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRE_MS = 1000 * 60 * 60 * 24 * 7; // 7 days

    // Generate accessToken
    public String generateAccessToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .claim("tokenType", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
    //Generate refreshToken
    public String generateRefreshToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("tokenType", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract Subject
    public String extractSubject(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    // Extract Role
    public String extractRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    // Extract Token type
    public String extractTokenType(String token) {
        Claims claims = getClaims(token);
        return claims.get("tokenType", String.class);
    }

    // Check if it's refresh token
    public boolean isRefreshToken(String token) {
        String tokenType = extractTokenType(token);
        
        if (tokenType.equals("refresh")) {
            return true;
        }

        return false;
    }

    // Decoding token and get Claims
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

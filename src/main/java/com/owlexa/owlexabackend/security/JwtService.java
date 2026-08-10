package com.owlexa.owlexabackend.security;

import com.owlexa.owlexabackend.entity.User;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
    private static final String HEADER = BASE64_ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;
    private final Clock clock;

    @Autowired
    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:28800}") long expirationSeconds
    ) {
        this(objectMapper, secret, expirationSeconds, Clock.systemUTC());
    }

    JwtService(ObjectMapper objectMapper, String secret, long expirationSeconds, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret phải có ít nhất 32 ký tự");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("phone", user.getPhoneNumber());
        claims.put("role", user.getRole().name());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", issuedAt.plusSeconds(expirationSeconds).getEpochSecond());

        String payload = BASE64_ENCODER.encodeToString(
                objectMapper.writeValueAsBytes(claims));
        String unsignedToken = HEADER + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public Long extractUserId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        byte[] suppliedSignature;
        byte[] expectedSignature;
        try {
            suppliedSignature = BASE64_DECODER.decode(parts[2]);
            expectedSignature = BASE64_DECODER.decode(sign(unsignedToken));
        } catch (IllegalArgumentException exception) {
            throw new InvalidTokenException();
        }

        if (!MessageDigest.isEqual(suppliedSignature, expectedSignature)) {
            throw new InvalidTokenException();
        }

        try {
            Map<String, Object> claims = objectMapper.readValue(
                    BASE64_DECODER.decode(parts[1]), new TypeReference<>() {});
            long expiration = ((Number) claims.get("exp")).longValue();
            if (clock.instant().getEpochSecond() >= expiration) {
                throw new InvalidTokenException();
            }
            return Long.valueOf(String.valueOf(claims.get("sub")));
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return BASE64_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể ký access token", exception);
        }
    }

    public static final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() {
            super("Access token không hợp lệ hoặc đã hết hạn");
        }
    }
}

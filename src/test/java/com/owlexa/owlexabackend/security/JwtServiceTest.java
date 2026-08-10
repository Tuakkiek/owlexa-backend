package com.owlexa.owlexabackend.security;

import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.entity.User;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "a-test-secret-that-is-longer-than-thirty-two-characters";

    @Test
    void generatesAndValidatesSignedToken() throws Exception {
        Instant now = Instant.parse("2026-08-09T10:00:00Z");
        JwtService jwtService = serviceAt(now, 3600);
        User admin = adminWithId(42L);

        String token = jwtService.generateAccessToken(admin);

        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void rejectsTamperedAndExpiredTokens() throws Exception {
        Instant now = Instant.parse("2026-08-09T10:00:00Z");
        String token = serviceAt(now, 60).generateAccessToken(adminWithId(42L));
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> serviceAt(now, 60).extractUserId(tampered))
                .isInstanceOf(JwtService.InvalidTokenException.class);
        assertThatThrownBy(() -> serviceAt(now.plusSeconds(61), 60).extractUserId(token))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }

    private JwtService serviceAt(Instant instant, long expirationSeconds) {
        return new JwtService(
                new ObjectMapper(),
                SECRET,
                expirationSeconds,
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private User adminWithId(Long id) throws Exception {
        User user = new User("0900000000", "Admin", null, "hash", RoleName.ADMIN);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}

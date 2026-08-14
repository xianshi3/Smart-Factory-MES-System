package com.mes.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils 单元测试：密钥校验、签发、解析、过期
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();
        setField(jwtUtils, "secret", "UnitTestJwtSecret-AtLeast-32-Characters-Long!");
        setField(jwtUtils, "expiration", 3600_000L);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = JwtUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void validateSecret_acceptsLongEnoughSecret() {
        assertDoesNotThrow(() -> jwtUtils.validateSecret());
    }

    @Test
    void validateSecret_rejectsShortSecret() throws Exception {
        setField(jwtUtils, "secret", "short");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jwtUtils.validateSecret());
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void generateAndParseToken_roundTrip() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        String token = jwtUtils.generateToken(42L, "admin", claims);

        Claims parsed = jwtUtils.parseToken(token);
        assertEquals("42", parsed.getSubject());
        assertEquals("admin", parsed.get("username"));
        assertEquals("ADMIN", parsed.get("role"));
        assertEquals(42L, jwtUtils.getUserId(token));
        assertFalse(jwtUtils.isTokenExpired(token));
        assertTrue(jwtUtils.getRemainingMillis(token) > 0);
    }

    @Test
    void parseToken_rejectsTamperedToken() {
        String token = jwtUtils.generateToken(1L, "user", null);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThrows(JwtException.class, () -> jwtUtils.parseToken(tampered));
    }

    @Test
    void parseToken_rejectsTokenSignedWithDifferentSecret() throws Exception {
        JwtUtils other = new JwtUtils();
        setField(other, "secret", "AnotherUnitTestSecret-AtLeast-32-Characters!");
        setField(other, "expiration", 3600_000L);
        String token = other.generateToken(1L, "user", null);
        assertThrows(JwtException.class, () -> jwtUtils.parseToken(token));
    }
}

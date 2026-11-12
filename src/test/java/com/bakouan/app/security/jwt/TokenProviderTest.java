package com.bakouan.app.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenProviderTest {
    @Test
    void tokensWithoutRolesAreValidAndRememberMeControlsLifetime() {
        TokenProvider provider = new TokenProvider(secret());
        var authentication = new UsernamePasswordAuthenticationToken("agent@bsic.bf", null, List.of());
        var token = provider.createToken(authentication, false);
        assertTrue(provider.validateToken(token.getToken()));
        assertTrue(provider.getAuthentication(token.getToken()).getAuthorities().isEmpty());
        assertEquals("agent@bsic.bf", provider.extractUsername(token.getToken()));
        assertEquals(86_400_000L, token.getExpiresIn());
        assertEquals(259_200_000L, provider.createToken(authentication, true).getExpiresIn());
    }

    @Test
    void rejectsInvalidExpiredAndNonExpiringTokens() {
        String secret = secret();
        TokenProvider provider = new TokenProvider(secret);
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        String expired = Jwts.builder().setSubject("agent").setExpiration(new Date(0))
                .signWith(key, SignatureAlgorithm.HS512).compact();
        String noExpiry = Jwts.builder().setSubject("agent")
                .signWith(key, SignatureAlgorithm.HS512).compact();
        assertFalse(provider.validateToken(expired));
        assertFalse(provider.validateToken(noExpiry));
        assertFalse(provider.validateToken("invalid"));
        assertFalse(provider.validateToken("invalid", "agent"));
    }

    @Test
    void rejectsMissingWeakOrMalformedSecretsAndOtherSigningKey() {
        assertThrows(IllegalArgumentException.class, () -> new TokenProvider(""));
        assertThrows(IllegalArgumentException.class, () -> new TokenProvider("invalid!"));
        assertThrows(IllegalArgumentException.class,
                () -> new TokenProvider(Base64.getEncoder().encodeToString(new byte[32])));
        TokenProvider provider = new TokenProvider(secret());
        String token = provider.createToken(new UsernamePasswordAuthenticationToken("agent", null, List.of()), false).getToken();
        assertFalse(new TokenProvider(secret()).validateToken(token));
    }

    private String secret() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
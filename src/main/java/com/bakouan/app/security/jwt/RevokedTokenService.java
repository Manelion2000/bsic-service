package com.bakouan.app.security.jwt;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RevokedTokenService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(final String token, final Date expirationDate) {
        if (token == null || expirationDate == null) {
            return;
        }
        Instant expirationInstant = expirationDate.toInstant();
        if (expirationInstant.isAfter(Instant.now())) {
            revokedTokens.put(token, expirationInstant);
        }
    }

    public boolean isRevoked(final String token) {
        if (token == null) {
            return false;
        }
        Instant expirationInstant = revokedTokens.get(token);
        if (expirationInstant == null) {
            return false;
        }
        if (!expirationInstant.isAfter(Instant.now())) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }
}

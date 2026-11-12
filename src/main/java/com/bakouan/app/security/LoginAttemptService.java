package com.bakouan.app.security;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private final int maxAttempt;
    private final Cache<String, Integer> attemptsCache;

    @Autowired
    public LoginAttemptService(@Value("${app.security.attempts.max:5}") final int maxAttempt,
                               @Value("${app.security.attempts.timeout:5}") final long timeoutMinutes) {
        this(maxAttempt, timeoutMinutes, Ticker.systemTicker());
    }

    LoginAttemptService(final int maxAttempt, final long timeoutMinutes, final Ticker ticker) {
        if (maxAttempt < 1 || timeoutMinutes < 1) {
            throw new IllegalArgumentException("Le seuil et le delai des tentatives doivent etre positifs.");
        }
        this.maxAttempt = maxAttempt;
        this.attemptsCache = CacheBuilder.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(timeoutMinutes, TimeUnit.MINUTES)
                .ticker(ticker)
                .build();
    }

    public void loginSucceeded(final String key) {
        attemptsCache.invalidate(key);
    }

    /** L'increment est atomique, y compris lors de requetes concurrentes. */
    public void loginFailed(final String key) {
        attemptsCache.asMap().compute(key,
                (ignored, attempts) -> attempts == null ? 1 : attempts >= maxAttempt ? maxAttempt : attempts + 1);
    }

    public boolean isBlocked(final String key) {
        Integer attempts = attemptsCache.getIfPresent(key);
        return attempts != null && attempts >= maxAttempt;
    }
}

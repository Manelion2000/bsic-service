package com.bakouan.app.security;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {
    @Test
    void blockExpiresAfterConfiguredTimeout() {
        AtomicLong nanos = new AtomicLong();
        LoginAttemptService service = new LoginAttemptService(2, 5, new Ticker() {
            @Override public long read() { return nanos.get(); }
        });
        service.loginFailed("client");
        service.loginFailed("client");
        assertTrue(service.isBlocked("client"));
        nanos.set(TimeUnit.MINUTES.toNanos(5));
        assertFalse(service.isBlocked("client"));
    }

    @Test
    void parallelFailuresCannotLoseIncrements() throws Exception {
        LoginAttemptService service = new LoginAttemptService(100, 5);
        var executor = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < 100; i++) {
                futures.add(executor.submit(() -> service.loginFailed("client")));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            assertTrue(service.isBlocked("client"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void successOnlyClearsTheMatchingClient() {
        LoginAttemptService service = new LoginAttemptService(1, 5);
        service.loginFailed("one");
        service.loginFailed("two");
        service.loginSucceeded("one");
        assertFalse(service.isBlocked("one"));
        assertTrue(service.isBlocked("two"));
    }
}
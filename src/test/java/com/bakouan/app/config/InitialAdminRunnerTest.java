package com.bakouan.app.config;

import com.bakouan.app.security.InitialAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitialAdminRunnerTest {
    @AfterEach void clearContext(){SecurityContextHolder.clearContext();}
    @Test void runnerUsesTechnicalAuditIdentityAndRestoresThePreviousContext() {
        var previous=SecurityContextHolder.createEmptyContext();
        previous.setAuthentication(new UsernamePasswordAuthenticationToken("previous-user",null,List.of()));
        SecurityContextHolder.setContext(previous);
        var service=mock(InitialAdminService.class);
        when(service.initialize()).thenAnswer(i->{
            var authentication=SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getName()).isEqualTo("bootstrap-admin");assertThat(authentication.getAuthorities()).isEmpty();
            assertThat(authentication.getCredentials()).isNull();return InitialAdminService.Result.CREATED;
        });
        new InitialAdminRunner(service).run(new DefaultApplicationArguments());
        verify(service).initialize();assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
    }
    @Test void failedInitializationDoesNotLeaveAnAuthenticationContextBehind() {
        var previous=SecurityContextHolder.getContext();var service=mock(InitialAdminService.class);
        when(service.initialize()).thenThrow(new IllegalStateException("Configuration manquante"));
        assertThatThrownBy(()->new InitialAdminRunner(service).run(new DefaultApplicationArguments())).hasMessage("Configuration manquante");
        assertThat(SecurityContextHolder.getContext()).isSameAs(previous);assertThat(previous.getAuthentication()).isNull();
    }
}

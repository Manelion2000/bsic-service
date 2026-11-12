package com.bakouan.app.config;

import com.bakouan.app.security.BaSecurityErrorHandler;
import com.bakouan.app.security.BaUserDetailsService;
import com.bakouan.app.security.jwt.RevokedTokenService;
import com.bakouan.app.security.jwt.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityConfigForProdTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles("prod");
        context.register(SecurityConfigForProd.class, BaAuthenticationConfig.class, TestBeans.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @AfterEach void close() { if (context != null) context.close(); }

    @Test
    void usersCannotBeListedOrCreatedAnonymously() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
        mvc.perform(post("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void activationRemainsProtected() throws Exception {
        mvc.perform(get("/api/users/user-id/activate")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginResetAndPublicDirectoryAreAvailableWithoutToken() throws Exception {
        mvc.perform(post("/api/authenticate")).andExpect(status().isOk());
        mvc.perform(put("/api/users/request-reset-password")).andExpect(status().isOk());
        mvc.perform(put("/api/users/complete-reset-password")).andExpect(status().isOk());
        mvc.perform(get("/api/public/features")).andExpect(status().isOk());
    }

    @Test
    void publicPathsDoNotPermitUnlistedHttpMethods() throws Exception {
        mvc.perform(get("/api/authenticate")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/public/features")).andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    static class TestBeans {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory() {
            return mock(jakarta.persistence.EntityManagerFactory.class);
        }
        @Bean BaUserDetailsService userDetailsService() { return mock(BaUserDetailsService.class); }
        @Bean RevokedTokenService revokedTokenService() { return new RevokedTokenService(); }
        @Bean TokenProvider tokenProvider() {
            byte[] secret = new byte[64];
            new SecureRandom().nextBytes(secret);
            return new TokenProvider(Base64.getEncoder().encodeToString(secret));
        }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean BaSecurityErrorHandler securityErrorHandler(ObjectMapper mapper) {
            return new BaSecurityErrorHandler(mapper);
        }
        @Bean RestTemplateBuilder restTemplateBuilder() { return new RestTemplateBuilder(); }
        @Bean RoutesController routesController() { return new RoutesController(); }
    }

    @RestController
    static class RoutesController {
        @RequestMapping({"/api/users", "/api/users/user-id/activate", "/api/authenticate",
                "/api/users/request-reset-password", "/api/users/complete-reset-password", "/api/public/features"})
        String ok() { return "ok"; }
    }
}

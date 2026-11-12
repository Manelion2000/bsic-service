package com.bakouan.app.security.jwt;

import com.bakouan.app.security.BaSecurityErrorHandler;
import com.bakouan.app.security.BaUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JWTFilterTest {
    private final TokenProvider tokens = mock(TokenProvider.class);
    private final BaUserDetailsService details = mock(BaUserDetailsService.class);
    private final RevokedTokenService revoked = mock(RevokedTokenService.class);
    private final JWTFilter filter = new JWTFilter(tokens, details, revoked,
            new BaSecurityErrorHandler(new ObjectMapper().findAndRegisterModules()));

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void usesCurrentRolesAndErasesCredentials() throws Exception {
        when(tokens.validateToken("token")).thenReturn(true);
        when(tokens.extractUsername("token")).thenReturn("agent");
        when(details.loadUserByUsername("agent")).thenReturn(
                new User("agent", "stored-hash", List.of(new SimpleGrantedAuthority("CURRENT_ROLE"))));
        filter.doFilter(request("/api/users"), new MockHttpServletResponse(), new MockFilterChain());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(List.of(new SimpleGrantedAuthority("CURRENT_ROLE")), List.copyOf(authentication.getAuthorities()));
        assertNull(((User) authentication.getPrincipal()).getPassword());
        verify(tokens, never()).getAuthentication(anyString());
    }

    @Test
    void inactiveAccountIsRejectedWithJson() throws Exception {
        when(tokens.validateToken("token")).thenReturn(true);
        when(tokens.extractUsername("token")).thenReturn("agent");
        when(details.loadUserByUsername("agent")).thenReturn(
                new User("agent", "hash", false, true, true, true, List.of()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/api/users"), response, chain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void revokedTokenIsRejectedBeforeLoadingAccount() throws Exception {
        when(revoked.isRevoked("token")).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/users"), response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        verifyNoInteractions(details, tokens);
    }

    @Test
    void oldTokenCannotPreventLogin() throws Exception {
        MockHttpServletRequest request = request("/api/authenticate");
        request.setMethod("POST");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
        verifyNoInteractions(tokens, details, revoked);
    }

    @Test
    void urlQueryTokenIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setServletPath("/api/users");
        request.addParameter("access_token", "token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokens, details, revoked);
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
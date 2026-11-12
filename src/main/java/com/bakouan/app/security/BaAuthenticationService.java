package com.bakouan.app.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BaAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final BaUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;
    private final ClientIpResolver clientIpResolver;
    private final HttpServletRequest request;

    public Authentication authenticate(final UsernamePasswordAuthenticationToken credentials) {
        String clientIp = clientIpResolver.resolve(request);
        checkAllowed(clientIp);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(credentials);
            authentication = userDetailsService.completeAuthentication(authentication);
        } catch (InternalAuthenticationServiceException ex) {
            // Une panne de base de donnees ne compte pas comme un mauvais mot de passe.
            throw ex;
        } catch (AuthenticationException ex) {
            loginAttemptService.loginFailed(clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Les informations de connexion sont invalides.");
        }
        loginAttemptService.loginSucceeded(clientIp);
        return authentication;
    }

    public void checkPasswordResetAllowed() {
        String key = "password-reset:" + clientIpResolver.resolve(request);
        checkAllowed(key);
        loginAttemptService.loginFailed(key);
    }

    private void checkAllowed(final String key) {
        if (loginAttemptService.isBlocked(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives. Veuillez reessayer dans quelques minutes.");
        }
    }
}

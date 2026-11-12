package com.bakouan.app.security;

import com.bakouan.app.dto.BaApiErrorDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BaSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(final HttpServletRequest request, final HttpServletResponse response,
                         final AuthenticationException exception) throws IOException {
        unauthorized(response);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
                       final AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Vous n'etes pas autorise a effectuer cette action.");
    }

    public void unauthorized(final HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Votre session est invalide ou expiree. Veuillez vous reconnecter.");
    }

    public void passwordChangeRequired(HttpServletResponse response)throws IOException {
        write(response,HttpStatus.FORBIDDEN,"Vous devez définir votre mot de passe personnel avant d'accéder aux modules.");
    }

    private void write(final HttpServletResponse response, final HttpStatus status,
                       final String message) throws IOException {
        response.setStatus(status.value());
        response.setHeader("Cache-Control","no-store");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new BaApiErrorDetail(status, message, null));
    }
}

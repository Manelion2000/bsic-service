package com.bakouan.app.security.jwt;

import com.bakouan.app.security.BaSecurityErrorHandler;
import com.bakouan.app.security.BaUserDetailsService;
import com.bakouan.app.security.CredentialUserDetails;
import com.bakouan.app.security.CredentialSession;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.bakouan.app.utils.BaConstants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JWTFilter extends GenericFilterBean {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_TOKEN = "access_token";

    private final TokenProvider tokenProvider;
    private final BaUserDetailsService userDetailsService;
    private final RevokedTokenService revokedTokenService;
    private final BaSecurityErrorHandler securityErrorHandler;

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // Un ancien token ne doit pas empecher une nouvelle connexion ou reinitialisation.
        if (isSessionEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String jwt = resolveToken(request);
        if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (revokedTokenService.isRevoked(jwt) || !tokenProvider.validateToken(jwt)) {
                reject(response);
                return;
            }
            UserDetails user;
            try {
                user = userDetailsService.loadUserByUsername(tokenProvider.extractUsername(jwt));
            } catch (UsernameNotFoundException | JwtException ex) {
                reject(response);
                return;
            }
            if (!user.isEnabled() || !user.isAccountNonLocked()
                    || !user.isAccountNonExpired() || !user.isCredentialsNonExpired()) {
                reject(response);
                return;
            }
            long version;
            boolean setup;
            try{version=tokenProvider.credentialVersion(jwt);setup=tokenProvider.passwordSetup(jwt);}
            catch(JwtException|IllegalArgumentException ex){reject(response);return;}
            if(user instanceof CredentialUserDetails details){
                if(version!=details.getCredentialVersion()||setup!=details.isPasswordChangeRequired()||(setup&&!details.isTemporaryPasswordUsed())){reject(response);return;}
            }else if(setup){reject(response);return;}
            if(setup&&!("PUT".equals(request.getMethod())&&request.getServletPath().equals(BaConstants.URL.BASE_URL+BaConstants.URL.USER+"/complete-initial-password"))){
                SecurityContextHolder.clearContext();securityErrorHandler.passwordChangeRequired(response);return;
            }
            // Utiliser les droits actuels, pas les roles figes lors de l'emission du JWT.
            var authentication = new UsernamePasswordAuthenticationToken(user, null, setup?List.of(new SimpleGrantedAuthority("PASSWORD_SETUP_ONLY")):user.getAuthorities());
            authentication.setDetails(new CredentialSession(version,setup));
            authentication.eraseCredentials();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private void reject(final HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        securityErrorHandler.unauthorized(response);
    }

    private boolean isSessionEndpoint(final HttpServletRequest request) {
        String path = request.getServletPath();
        String base = BaConstants.URL.BASE_URL;
        return ("POST".equals(request.getMethod())
                && (path.equals(base + BaConstants.URL.AUTHENTICATE)
                    || path.equals(base + BaConstants.URL.LOGOUT)))
                || ("PUT".equals(request.getMethod())
                && (path.equals(base + BaConstants.URL.USER + "/request-reset-password")
                    || path.equals(base + BaConstants.URL.USER + "/complete-reset-password")));
    }

    private String resolveToken(final HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // Les parametres d'URL sont journalises par les proxies et l'historique du navigateur.
        return null;
    }
}

package com.bakouan.app.security.jwt;

import com.bakouan.app.dto.BaJWTTokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import com.bakouan.app.security.CredentialSession;
import com.bakouan.app.security.CredentialUserDetails;
import com.bakouan.app.config.PasswordAccessProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Function;

@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final long TOKEN_VALIDITY_IN_MILLISECONDS = 86_400_000L;
    private static final long TOKEN_VALIDITY_IN_MILLISECONDS_FOR_REMEMBER_ME = 259_200_000L;
    private final Key key;
    private final PasswordAccessProperties passwordAccess;

    public TokenProvider(String secret){this(secret,new PasswordAccessProperties());}
    @Autowired
    public TokenProvider(@Value("${app.security.jwt.base64-secret:}") final String secret,PasswordAccessProperties passwordAccess) {
        this.passwordAccess=passwordAccess;
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Configurer BSIC_JWT_SECRET avec une cle Base64 d'au moins 64 octets.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("BSIC_JWT_SECRET doit contenir une cle Base64 valide.");
        }
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("BSIC_JWT_SECRET doit contenir au moins 64 octets.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public BaJWTTokenDto createToken(final Authentication authentication, final boolean rememberMe) {
        var session=authentication.getDetails() instanceof CredentialSession s?s:new CredentialSession(
                authentication.getPrincipal() instanceof CredentialUserDetails u?u.getCredentialVersion():0,false);
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.joining(","));
        long expiresIn = session.passwordSetup()?Math.max(1,Math.min(60,passwordAccess.getSetupSessionMinutes()))*60_000L:
                rememberMe ? TOKEN_VALIDITY_IN_MILLISECONDS_FOR_REMEMBER_ME : TOKEN_VALIDITY_IN_MILLISECONDS;
        Instant now = Instant.now();
        String token = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, session.passwordSetup()?"":authorities)
                .claim("cv",session.version())
                .claim("purpose",session.passwordSetup()?"PASSWORD_SETUP":"ACCESS")
                .setId(java.util.UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiresIn)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        return new BaJWTTokenDto(now, token, expiresIn,session.passwordSetup());
    }

    public Authentication getAuthentication(final String token) {
        Claims claims = parseToken(token);
        String roles = claims.get(AUTHORITIES_KEY, String.class);
        if("PASSWORD_SETUP".equals(claims.get("purpose",String.class)))roles="";
        var authorities = Arrays.stream(roles == null ? new String[0] : roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .distinct()
                .toList();
        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Claims parseToken(final String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long credentialVersion(String token){return extractClaim(token,c->{Object v=c.get("cv");if(v==null)return 0L;if(!(v instanceof Number n)||n.longValue()<0)throw new IllegalArgumentException("Version invalide");return n.longValue();});}
    public boolean passwordSetup(String token){return "PASSWORD_SETUP".equals(extractClaim(token,c->c.get("purpose",String.class)));}

    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseToken(token));
    }

    public Boolean validateToken(final String token, final String username) {
        try {
            return username != null && username.equals(extractUsername(token)) && validateToken(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Boolean validateToken(final String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject() != null && !claims.getSubject().isBlank()
                    && (claims.get("purpose")==null||"ACCESS".equals(claims.get("purpose"))||"PASSWORD_SETUP".equals(claims.get("purpose")))
                    && claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}

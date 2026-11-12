package com.bakouan.app.security;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaRole;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BaUserDetailsService implements UserDetailsService {

    private final BaUserRepository userRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    /** Charge le compte sans enregistrer une connexion ni modifier les tentatives. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String username) {
        BaUser user = getOneByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Les informations de connexion sont invalides."));
        var authorities = (user.getRoles() == null ? Stream.<BaRole>empty() : user.getRoles().stream())
                .filter(role -> role != null && role.getCode() != null && !role.getCode().isBlank())
                .map(role -> new SimpleGrantedAuthority(role.getCode().trim()))
                .distinct()
                .toList();

        return new CredentialUserDetails(user.getUsername().toLowerCase(Locale.ROOT),user.getPassword(),
                Boolean.TRUE.equals(user.getActivated()),!Boolean.TRUE.equals(user.getLocked()),authorities,
                user.getCredentialVersion(),user.isPasswordChangeRequired(),user.isTemporaryPasswordUsed());
    }

    @Transactional
    public Authentication completeAuthentication(Authentication authentication) {
        var user=userRepository.findLockedByUsername(authentication.getName(),EStatut.A)
                .orElseThrow(()->new BadCredentialsException("Connexion invalide."));
        if(entityManager!=null)entityManager.refresh(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        long version=authentication.getPrincipal() instanceof CredentialUserDetails details?details.getCredentialVersion():0;
        if(version!=user.getCredentialVersion()||!Boolean.TRUE.equals(user.getActivated())||Boolean.TRUE.equals(user.getLocked()))
            throw new BadCredentialsException("Connexion invalide.");
        boolean setup=user.isPasswordChangeRequired();
        if(setup&&(user.isTemporaryPasswordUsed()||user.getTemporaryPasswordExpiresAt()==null||!user.getTemporaryPasswordExpiresAt().isAfter(Instant.now())))
            throw new BadCredentialsException("Connexion invalide.");
        if(setup)user.setTemporaryPasswordUsed(true);
        user.setLastConnexionDate(ZonedDateTime.now());userRepository.saveAndFlush(user);
        var result=new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),null,
                setup?List.of(new SimpleGrantedAuthority("PASSWORD_SETUP_ONLY")):authentication.getAuthorities());
        result.setDetails(new CredentialSession(user.getCredentialVersion(),setup));result.eraseCredentials();return result;
    }

    @Transactional
    public void recordSuccessfulLogin(final String username) {
        BaUser user = getOneByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable."));
        user.setLastConnexionDate(ZonedDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<BaUser> getOneByUsername(final String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findOneByUsernameIgnoreCaseAndStatut(username.trim(), EStatut.A);
    }
}

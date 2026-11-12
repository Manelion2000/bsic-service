package com.bakouan.app.security;

import com.bakouan.app.config.PasswordAccessProperties;
import com.bakouan.app.dto.*;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.service.BaLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;

/** Accès temporaires, versions de session et changement personnel dans une transaction. */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountCredentialService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final BaUserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordAccessProperties properties;
    private final BaLogService audit;
    @PersistenceContext private EntityManager entityManager;

    /** Le secret n'est renvoyé qu'à la création ou à la réinitialisation autorisée. */
    public String prepareTemporary(BaUser user) {
        byte[] random = new byte[18];
        RANDOM.nextBytes(random);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        user.setPassword(encoder.encode(secret));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        user.setPasswordChangeRequired(true);
        user.setTemporaryPasswordUsed(false);
        int hours = Math.max(1, Math.min(168, properties.getTemporaryValidityHours()));
        user.setTemporaryPasswordExpiresAt(Instant.now().plus(Duration.ofHours(hours)));
        user.setResetKey(null);
        user.setResetDate(null);
        return secret;
    }

    public TemporaryCredentialDto receipt(BaUser user, String secret) {
        return new TemporaryCredentialDto(user.getUsername(), secret, user.getTemporaryPasswordExpiresAt(), user.getVersion());
    }

    /** Capacité calculée côté serveur, incluant le rôle hérité d'un profil actif. */
    public boolean canResetAccess(BaUser actor) {
        if (actor == null || actor.getStatut() != EStatut.A || !Boolean.TRUE.equals(actor.getActivated())
                || Boolean.TRUE.equals(actor.getLocked())) return false;
        var assigned = new ArrayList<>(actor.getRoles());
        if (actor.getProfil() != null && actor.getProfil().getStatut() == EStatut.A) {
            assigned.addAll(actor.getProfil().getRoles());
        }
        return assigned.stream().anyMatch(role -> role.getStatut() == EStatut.A
                && BaRolesConstants.BA_ADMIN.equalsIgnoreCase(role.getCode()));
    }

    public TemporaryCredentialDto reset(String id, PasswordAccessDtos.Reset input) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw error(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        if (!(authentication.getDetails() instanceof CredentialSession session) || session.passwordSetup()) {
            throw error(HttpStatus.FORBIDDEN, "Une session normale est requise.");
        }
        var actor = users.findOneByUsernameIgnoreCaseAndStatut(authentication.getName(), EStatut.A)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
        if (!canResetAccess(actor)) throw error(HttpStatus.FORBIDDEN, "Réinitialisation réservée à l'administrateur.");
        if (actor.getId().equals(id)) throw error(HttpStatus.BAD_REQUEST, "Pour votre compte, utilisez Mon mot de passe.");

        var user = users.findLockedById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        refresh(user);
        if (!user.getVersion().equals(input.version())) {
            throw error(HttpStatus.CONFLICT, "Le compte a changé. Rechargez sa fiche avant de réinitialiser.");
        }
        if (user.getStatut() != EStatut.A || !Boolean.TRUE.equals(user.getActivated()) || Boolean.TRUE.equals(user.getLocked())) {
            throw error(HttpStatus.CONFLICT, "Activez ou déverrouillez le compte par l'action prévue avant de réinitialiser son accès.");
        }
        String secret = prepareTemporary(user);
        users.saveAndFlush(user);
        var event = new BaLogDto(EAction.U, "Réinitialisation accès utilisateur : " + id);
        event.setDetails("motif=" + input.motif().trim());
        audit.log(event);
        return receipt(user, secret);
    }

    public void complete(PasswordAccessDtos.Complete input) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof CredentialSession session)
                || !session.passwordSetup()) {
            throw error(HttpStatus.FORBIDDEN, "Une session de changement initial est requise.");
        }
        var user = users.findLockedByUsername(authentication.getName(), EStatut.A)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Session expirée."));
        refresh(user);
        if (!user.isPasswordChangeRequired() || !user.isTemporaryPasswordUsed()
                || user.getCredentialVersion() != session.version()
                || !Boolean.TRUE.equals(user.getActivated()) || Boolean.TRUE.equals(user.getLocked())) {
            throw error(HttpStatus.UNAUTHORIZED, "Session expirée. Demandez un nouvel accès temporaire.");
        }
        replacePassword(user, input.nouveau(), input.confirmer());
        audit.log(new BaLogDto(EAction.U, "Définition du mot de passe personnel : " + user.getId()));
    }

    public void change(BaUpdatePasswordDto input) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof CredentialSession session)
                || session.passwordSetup()) throw error(HttpStatus.FORBIDDEN, "Une session normale est requise.");
        var user = users.findLockedByUsername(authentication.getName(), EStatut.A)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Session expirée."));
        refresh(user);
        if (user.getCredentialVersion() != session.version() || user.isPasswordChangeRequired()
                || !Boolean.TRUE.equals(user.getActivated()) || Boolean.TRUE.equals(user.getLocked())) {
            throw error(HttpStatus.UNAUTHORIZED, "Session expirée.");
        }
        if (input.getAncien() == null || !encoder.matches(input.getAncien(), user.getPassword())) {
            throw error(HttpStatus.BAD_REQUEST, "Votre mot de passe actuel est incorrect.");
        }
        replacePassword(user, input.getNouveau(), input.getConfirmer());
        audit.log(new BaLogDto(EAction.U, "Changement du mot de passe personnel : " + user.getId()));
    }

    private void replacePassword(BaUser user, String value, String confirm) {
        if (value == null || !value.equals(confirm)) {
            throw error(HttpStatus.BAD_REQUEST, "La confirmation ne correspond pas au nouveau mot de passe.");
        }
        if (value.isBlank() || value.length() < 12 || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw error(HttpStatus.BAD_REQUEST, "Choisissez un mot de passe de 12 caractères minimum, sans dépasser 72 octets UTF-8.");
        }
        if (encoder.matches(value, user.getPassword())) {
            throw error(HttpStatus.BAD_REQUEST, "Le nouveau mot de passe doit être différent du précédent.");
        }
        user.setPassword(encoder.encode(value));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordUsed(false);
        user.setTemporaryPasswordExpiresAt(null);
        user.setResetKey(null);
        user.setResetDate(null);
        users.saveAndFlush(user);
    }

    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private void refresh(BaUser user) {
        // OSIV peut avoir chargé le compte avant le verrou : relire son état après acquisition.
        if (entityManager != null) entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
    }
}

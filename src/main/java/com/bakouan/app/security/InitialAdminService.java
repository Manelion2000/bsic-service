package com.bakouan.app.security;

import com.bakouan.app.config.InitialAdminProperties;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaLog;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaLogRepository;
import com.bakouan.app.repositories.BaRoleRepository;
import com.bakouan.app.repositories.BaUserRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InitialAdminService {
    public enum Result { DISABLED, ALREADY_PRESENT, CREATED }

    private final InitialAdminProperties properties;
    private final BaUserRepository users;
    private final BaRoleRepository roles;
    private final BaLogRepository logs;
    private final PasswordEncoder encoder;
    private final Validator validator;

    @Transactional
    public Result initialize() {
        if (!properties.isEnabled()) return Result.DISABLED;

        // Le verrou de la ligne du rôle sérialise les premiers démarrages de plusieurs instances.
        var role = roles.findLockedByCode(BaRolesConstants.BA_ADMIN)
                .filter(r -> r.getStatut() == EStatut.A)
                .orElseThrow(() -> new IllegalStateException(
                        "Initialisation administrateur : le rôle BA_ADMIN actif est absent. Vérifiez les migrations."));

        // Inclure les profils et les comptes désactivés : le démarrage n'est pas un mécanisme de récupération.
        if (users.existsWithRoleCode(BaRolesConstants.BA_ADMIN)) return Result.ALREADY_PRESENT;

        String email = clean(properties.getEmail()).toLowerCase(Locale.ROOT);
        String telephone = clean(properties.getTelephone());
        var input = new BaUserDto();
        input.setUsername(email); input.setEmail(email); input.setTelephone(telephone);
        input.setNom(clean(properties.getNom())); input.setPrenom(clean(properties.getPrenom()));
        boolean validIdentity = java.util.stream.Stream.of("username", "email", "telephone", "nom", "prenom")
                .allMatch(field -> validator.validateProperty(input, field).isEmpty());
        if (email.length() > 50 || !validIdentity) {
            throw new IllegalStateException("Initialisation administrateur : vérifiez APP_BOOTSTRAP_ADMIN_EMAIL "
                    + "(e-mail de 50 caractères maximum), TELEPHONE, NOM et PRENOM. Le téléphone est obligatoire.");
        }
        String password = properties.getPassword();
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Initialisation administrateur : APP_BOOTSTRAP_ADMIN_PASSWORD est obligatoire "
                    + "et doit contenir au moins 12 caractères, sans dépasser 72 octets UTF-8.");
        }
        if (users.existsByUsernameIgnoreCase(email) || users.existsByEmailIgnoreCase(email)
                || users.existsByTelephoneIgnoreCase(telephone)) {
            throw new IllegalStateException("Initialisation administrateur : l'e-mail ou le téléphone est déjà utilisé. "
                    + "Aucun compte existant n'a été modifié. Choisissez des coordonnées distinctes.");
        }

        var account = new BaUser();
        account.setEmail(email); account.setTelephone(telephone);
        account.setNom(input.getNom()); account.setPrenom(input.getPrenom());
        account.setPassword(encoder.encode(password)); account.setActivated(true); account.setLocked(false);
        account.setPasswordChangeRequired(true);account.setTemporaryPasswordUsed(false);account.setCredentialVersion(1);
        account.setTemporaryPasswordExpiresAt(Instant.now().plus(java.time.Duration.ofHours(24)));
        account.setStatut(EStatut.A); account.getRoles().add(role);
        account.setCreatedBy("bootstrap-admin"); account.setCreatedDate(Instant.now());
        account = users.saveAndFlush(account);

        // Audit sans requête HTTP ni secret ; compte, rôle et trace sont validés dans la même transaction.
        var event = new BaLog();
        event.setAction(EAction.C); event.setSujet("Création de l'administrateur initial");
        event.setDetails("compte=" + account.getId() + ";role=BA_ADMIN;origine=DEPLOIEMENT_INITIAL");
        event.setIpAdresse("SYSTEM"); event.setCreatedBy("bootstrap-admin"); event.setCreatedDate(Instant.now());
        logs.saveAndFlush(event);
        return Result.CREATED;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}

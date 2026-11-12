package com.bakouan.app.security;

import com.bakouan.app.config.InitialAdminProperties;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaLog;
import com.bakouan.app.model.BaRole;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaLogRepository;
import com.bakouan.app.repositories.BaRoleRepository;
import com.bakouan.app.repositories.BaUserRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialAdminServiceTest {
    @Mock BaUserRepository users;
    @Mock BaRoleRepository roles;
    @Mock BaLogRepository logs;
    private InitialAdminProperties properties;
    private InitialAdminService service;
    private ValidatorFactory factory;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach void setup() {
        properties = new InitialAdminProperties();
        properties.setEnabled(true); properties.setTelephone("+22670001122"); properties.setPassword("Startup-test-Password");
        factory = Validation.buildDefaultValidatorFactory();
        service = new InitialAdminService(properties,users,roles,logs,encoder,factory.getValidator());
    }
    @AfterEach void cleanup() { factory.close(); }
    private void activeRole() {
        var role = new BaRole(); role.setId("admin-role"); role.setCode("BA_ADMIN"); role.setStatut(EStatut.A);
        when(roles.findLockedByCode("BA_ADMIN")).thenReturn(Optional.of(role));
    }
    @Test void disabledBootstrapDoesNotAccessDatabaseOrRequireSecrets() {
        properties.setEnabled(false);properties.setPassword(null);properties.setTelephone(null);
        assertThat(service.initialize()).isEqualTo(InitialAdminService.Result.DISABLED);
        verifyNoInteractions(users,roles,logs);
    }
    @Test void existingAdministratorSkipsInitializationWithoutRequiringPassword() {
        activeRole();when(users.existsWithRoleCode("BA_ADMIN")).thenReturn(true);properties.setPassword(null);properties.setTelephone(null);
        assertThat(service.initialize()).isEqualTo(InitialAdminService.Result.ALREADY_PRESENT);
        verify(users,never()).saveAndFlush(any());verifyNoInteractions(logs);
    }
    @Test void missingOrInactiveRoleIsReportedWithoutCreatingAnAccount() {
        when(roles.findLockedByCode("BA_ADMIN")).thenReturn(Optional.empty());
        assertThatThrownBy(service::initialize).isInstanceOf(IllegalStateException.class).hasMessageContaining("BA_ADMIN");
        var role=new BaRole();role.setStatut(EStatut.D);when(roles.findLockedByCode("BA_ADMIN")).thenReturn(Optional.of(role));
        assertThatThrownBy(service::initialize).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(users,logs);
    }
    @Test void createsAnActiveTechnicalAdministratorWithHashedPasswordAndAudit() {
        activeRole();properties.setEmail(" ADMIN@BSIC.BF ");properties.setPassword("  Startup-test-Password  ");
        when(users.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        assertThat(service.initialize()).isEqualTo(InitialAdminService.Result.CREATED);
        var account=ArgumentCaptor.forClass(BaUser.class);verify(users).saveAndFlush(account.capture());var user=account.getValue();
        assertThat(user.getUsername()).isEqualTo("admin@bsic.bf");assertThat(user.getActivated()).isTrue();assertThat(user.getLocked()).isFalse();
        assertThat(user.isPasswordChangeRequired()).isTrue();assertThat(user.isTemporaryPasswordUsed()).isFalse();
        assertThat(user.getCredentialVersion()).isEqualTo(1);assertThat(user.getTemporaryPasswordExpiresAt()).isAfter(java.time.Instant.now());
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo(properties.getPassword());assertThat(encoder.matches(properties.getPassword(),user.getPassword())).isTrue();
        assertThat(user.getRoles()).extracting(BaRole::getCode).containsExactly("BA_ADMIN");
        assertThat(user.getFonction()).isNull();assertThat(user.getDepartement()).isNull();assertThat(user.getService()).isNull();assertThat(user.getResetKey()).isNull();
        var event=ArgumentCaptor.forClass(BaLog.class);verify(logs).saveAndFlush(event.capture());
        assertThat(event.getValue().getDetails()).contains(user.getId(),"BA_ADMIN").doesNotContain(properties.getPassword(),user.getPassword());
    }
    @Test void missingShortAndOverlongUtf8PasswordsAreRejectedWithoutDisclosure() {
        activeRole();
        for(String password:new String[]{null,"", "too-short", "é".repeat(37)}) {
            properties.setPassword(password);
            var failure=catchThrowable(service::initialize);assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_BOOTSTRAP_ADMIN_PASSWORD");
            if(password!=null&&!password.isEmpty())assertThat(failure.getMessage()).doesNotContain(password);
        }
        verify(users,never()).saveAndFlush(any());verifyNoInteractions(logs);
    }
    @Test void invalidIdentityConfigurationIsRejectedBeforeStoringPassword() {
        activeRole();properties.setTelephone(null);assertThatThrownBy(service::initialize).hasMessageContaining("TELEPHONE");
        properties.setTelephone("+22670001122");properties.setEmail("invalid-email");assertThatThrownBy(service::initialize).hasMessageContaining("EMAIL");
        properties.setEmail("a".repeat(45)+"@bsic.bf");assertThatThrownBy(service::initialize).hasMessageContaining("50 caractères");
        verify(users,never()).saveAndFlush(any());verifyNoInteractions(logs);
    }
    @Test void doesNotGrantPrivilegesToAnExistingAccountWithSameIdentity() {
        activeRole();when(users.existsByUsernameIgnoreCase("admin@bsic.bf")).thenReturn(true);
        assertThatThrownBy(service::initialize).hasMessageContaining("déjà utilisé");
        verify(users,never()).saveAndFlush(any());verifyNoInteractions(logs);
    }
    @Test void duplicateTelephoneCannotBeOverwritten() {
        activeRole();when(users.existsByTelephoneIgnoreCase("+22670001122")).thenReturn(true);
        assertThatThrownBy(service::initialize).hasMessageContaining("déjà utilisé");
        verify(users,never()).saveAndFlush(any());verifyNoInteractions(logs);
    }
}

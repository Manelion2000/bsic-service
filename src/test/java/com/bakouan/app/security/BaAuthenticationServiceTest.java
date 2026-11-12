package com.bakouan.app.security;

import com.bakouan.app.config.BaAuthenticationConfig;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaAuthenticationServiceTest {
    @Mock private BaUserRepository repository;
    private BaAuthenticationService service;
    private BaUserDetailsService detailsService;
    private LoginAttemptService attempts;
    private BaUser user;

    @BeforeEach
    void setUp() {
        detailsService = new BaUserDetailsService(repository);
        attempts = new LoginAttemptService(5, 5);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        BaAuthenticationConfig config = new BaAuthenticationConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        service = new BaAuthenticationService(config.authenticationManager(
                config.authenticationProvider(detailsService, encoder)), detailsService, attempts,
                new ClientIpResolver(""), request);
        user = new BaUser();
        user.setUsername("agent@bsic.bf");
        user.setPassword(encoder.encode("correct-password"));
        user.setStatut(EStatut.A);
        user.setActivated(true);
    }

    @Test
    void failuresAreCountedAndCorrectPasswordCannotBypassBlock() {
        when(repository.findOneByUsernameIgnoreCaseAndStatut("agent@bsic.bf", EStatut.A))
                .thenReturn(Optional.of(user));
        for (int i = 0; i < 5; i++) {
            assertEquals(401, assertThrows(ResponseStatusException.class,
                    () -> login("wrong")).getStatusCode().value());
            assertNull(user.getLastConnexionDate());
        }
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> login("correct-password")).getStatusCode().value());
        assertNull(user.getLastConnexionDate());
    }

    @Test
    void successClearsFailuresRecordsDateAndErasesPassword() {
        when(repository.findLockedByUsername("agent@bsic.bf", EStatut.A)).thenReturn(Optional.of(user));
        when(repository.findOneByUsernameIgnoreCaseAndStatut("agent@bsic.bf", EStatut.A))
                .thenReturn(Optional.of(user));
        assertThrows(ResponseStatusException.class, () -> login("wrong"));
        Authentication authentication = login("correct-password");
        assertTrue(authentication.isAuthenticated());
        assertNotNull(user.getLastConnexionDate());
        assertNull(((UserDetails) authentication.getPrincipal()).getPassword());
        for (int i = 0; i < 4; i++) {
            assertThrows(ResponseStatusException.class, () -> login("wrong"));
        }
        assertFalse(attempts.isBlocked("192.0.2.10"));
    }

    @Test
    void unknownUserAndWrongPasswordReturnSameMessage() {
        when(repository.findOneByUsernameIgnoreCaseAndStatut(anyString(), eq(EStatut.A)))
                .thenAnswer(invocation -> "agent@bsic.bf".equals(invocation.getArgument(0))
                        ? Optional.of(user) : Optional.empty());
        ResponseStatusException wrong = assertThrows(ResponseStatusException.class, () -> login("wrong"));
        ResponseStatusException unknown = assertThrows(ResponseStatusException.class,
                () -> service.authenticate(new UsernamePasswordAuthenticationToken("unknown@bsic.bf", "wrong")));
        assertEquals(wrong.getReason(), unknown.getReason());
        assertEquals(wrong.getStatusCode(), unknown.getStatusCode());
    }

    @Test
    void disabledAndLockedAccountsCannotAuthenticate() {
        when(repository.findOneByUsernameIgnoreCaseAndStatut("agent@bsic.bf", EStatut.A))
                .thenReturn(Optional.of(user));
        user.setActivated(false);
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> login("correct-password")).getStatusCode().value());
        user.setActivated(true);
        user.setLocked(true);
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> login("correct-password")).getStatusCode().value());
        assertNull(user.getLastConnexionDate());
    }

    @Test
    void infrastructureFailureIsNotReportedAsBadCredentialsOrCounted() {
        when(repository.findOneByUsernameIgnoreCaseAndStatut(anyString(), eq(EStatut.A)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        assertThrows(InternalAuthenticationServiceException.class, () -> login("correct-password"));
        for (int i = 0; i < 4; i++) {
            attempts.loginFailed("192.0.2.10");
        }
        assertFalse(attempts.isBlocked("192.0.2.10"));
    }

    @Test
    void loadingDetailsDoesNotChangeLastLoginOrNormalizePassword() {
        when(repository.findOneByUsernameIgnoreCaseAndStatut("agent@bsic.bf", EStatut.A))
                .thenReturn(Optional.of(user));
        UserDetails details = detailsService.loadUserByUsername(" agent@bsic.bf ");
        assertEquals(user.getPassword(), details.getPassword());
        assertNull(user.getLastConnexionDate());
        verify(repository, never()).save(any());
    }

    private Authentication login(String password) {
        return service.authenticate(new UsernamePasswordAuthenticationToken("agent@bsic.bf", password));
    }
}

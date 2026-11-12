package com.bakouan.app.security;
import com.bakouan.app.dto.BaUpdatePasswordDto;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.service.BaMailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class BaPasswordResetTest {
    @Mock BaUserRepository repository;
    @Mock BaMailService mail;
    @InjectMocks BaUserService service;
    @Test void anonymousRecoveryIsNonDisclosingAndNeverSendsEmailOrModifiesAccounts(){
        for(String email:new String[]{"agent@bsic.bf","unknown@bsic.bf"}){
            var request=new BaUpdatePasswordDto();request.setEmail(email);
            assertDoesNotThrow(()->service.requestPasswordReset(request));
        }
        verifyNoInteractions(repository,mail);
    }
    @Test void legacyEmailCodeCannotChangePasswordOrReactivateAccount(){
        var request=new BaUpdatePasswordDto();request.setEmail("agent@bsic.bf");request.setResetKey("legacy-code");
        request.setNouveau("new-personal-password");request.setConfirmer("new-personal-password");
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.completResetPassword(request)).getStatusCode().value());
        verifyNoInteractions(repository,mail);
    }
    @Test void passwordDtosNeverPrintSecrets(){
        var request=new BaUpdatePasswordDto();request.setAncien("secret-old");request.setNouveau("secret-new");request.setConfirmer("secret-new");request.setResetKey("secret-code");
        assertFalse(request.toString().contains("secret-"));
        assertFalse(new com.bakouan.app.dto.PasswordAccessDtos.Complete("secret-new","secret-new").toString().contains("secret-new"));
    }
}

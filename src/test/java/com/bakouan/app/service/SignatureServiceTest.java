package com.bakouan.app.service;

import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignatureServiceTest {
    CongeSecurity security = mock(CongeSecurity.class);
    BaUserRepository users = mock(BaUserRepository.class);
    BaUserSignatureRepository personal = mock(BaUserSignatureRepository.class);
    BaActeSignatureRepository visas = mock(BaActeSignatureRepository.class);
    BaLogService logs = mock(BaLogService.class);
    SignatureService service = new SignatureService(security, users, personal, visas, new SignatureImageValidator(), logs);
    BaUser actor = new BaUser();
    @BeforeEach void setup() { actor.setId("me"); actor.setNom("Ouédraogo"); actor.setPrenom("Aminata"); actor.setFonction(EFonctionEmploye.DIRECTEUR); when(security.current()).thenReturn(actor); when(users.findLockedById("me")).thenReturn(Optional.of(actor)); }

    @Test void refusesSigningWithoutPersonalSignature() {
        assertThatThrownBy(() -> service.capture("CIRCUIT", "request", 0, actor, Instant.now())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Ma signature");
        verifyNoInteractions(visas);
    }
    @Test void snapshotsAreIndependentOfLaterImageAndIdentityChanges() throws Exception {
        BaUserSignature s = new BaUserSignature(); s.setContenu(SignatureTestImages.png(0)); s.setEmpreinte("a".repeat(64)); when(personal.findForSigning("me")).thenReturn(Optional.of(s));
        Instant now = Instant.now(); service.capture("CIRCUIT", "request", 2, actor, now);
        ArgumentCaptor<BaActeSignature> copy = ArgumentCaptor.forClass(BaActeSignature.class); verify(visas).save(copy.capture());
        byte[] before = copy.getValue().getContenu().clone(); s.getContenu()[0] = 0; actor.setNom("Autre nom"); service.remove();
        assertThat(copy.getValue().getContenu()).isEqualTo(before); assertThat(copy.getValue().getNomSignataire()).isEqualTo("Ouédraogo Aminata"); assertThat(copy.getValue().getDateSignature()).isEqualTo(now); verify(visas, never()).deleteById(any());
    }
    @Test void uploadUsesSessionOwnerAndNormalizesContent() throws Exception {
        service.upload(new MockMultipartFile("file", "image.png", "image/png", SignatureTestImages.png(0)));
        ArgumentCaptor<BaUserSignature> saved = ArgumentCaptor.forClass(BaUserSignature.class); verify(personal).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("me"); assertThat(saved.getValue().getEmpreinte()).hasSize(64); verify(users).findLockedById("me");
    }
    @Test void previewIsNormalizedWithoutChangingTheAccountOrArchivingAVisum() throws Exception {
        byte[] preview=service.preview(new MockMultipartFile("file","signature.png","image/png",SignatureTestImages.png(0)));
        assertThat(preview).startsWith(new byte[]{(byte)137,80,78,71,13,10,26,10});
        verify(security).current();verifyNoInteractions(personal,users,visas,logs);
    }
}

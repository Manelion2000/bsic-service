package com.bakouan.app.service;

import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.ERepriseServiceStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.service.impl.BaRepriseServiceManagerImpl;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaRepriseSignatureTest {
    BaRepriseServiceRepository reprises=mock(BaRepriseServiceRepository.class);
    BaUserRepository users=mock(BaUserRepository.class);
    BaUserService account=mock(BaUserService.class);
    RhValidationPolicy policy=mock(RhValidationPolicy.class);
    SignatureService signatures=mock(SignatureService.class);
    BaRepriseServiceManagerImpl service=new BaRepriseServiceManagerImpl(reprises,users,account,policy,signatures);
    BaRepriseService request=new BaRepriseService(); BaUser actor=new BaUser(), requester=new BaUser();
    @BeforeEach void setup(){actor.setId("signer");requester.setId("agent");request.setId("reprise");request.setDemandeur(requester);request.setStatutValidation(ERepriseServiceStatut.EN_ATTENTE);BaUserDto dto=new BaUserDto();dto.setId("signer");when(account.getUserInfoWithMoreDetails()).thenReturn(dto);when(users.findById("signer")).thenReturn(Optional.of(actor));when(reprises.findLockedById("reprise")).thenReturn(Optional.of(request));when(policy.canValidate(actor,requester)).thenReturn(true);when(reprises.save(any())).thenAnswer(i->i.getArgument(0));}
    @Test void approvalArchivesTheActualValidator(){service.valider("reprise",null);verify(signatures).capture(eq("REPRISE"),eq("reprise"),eq(0),eq(actor),any());assertThat(request.getSignataire()).isSameAs(actor);assertThat(request.getStatutValidation()).isEqualTo(ERepriseServiceStatut.VALIDEE);}
    @Test void missingSignaturePreventsSavingTheDecision(){doThrow(new ResponseStatusException(HttpStatus.CONFLICT,"Ma signature")).when(signatures).capture(anyString(),anyString(),anyInt(),any(),any());assertThatThrownBy(()->service.valider("reprise",null)).isInstanceOf(ResponseStatusException.class);verify(reprises,never()).save(any());}
    @Test void rejectionDoesNotApplyAnApprovalSignature(){service.rejeter("reprise","Reprise non constatée");verifyNoInteractions(signatures);assertThat(request.getStatutValidation()).isEqualTo(ERepriseServiceStatut.REJETEE);}
    @Test void unauthorizedActorCannotArchiveASignature(){when(policy.canValidate(actor,requester)).thenReturn(false);assertThatThrownBy(()->service.valider("reprise",null)).isInstanceOf(ResponseStatusException.class);verifyNoInteractions(signatures);verify(reprises,never()).save(any());}
}

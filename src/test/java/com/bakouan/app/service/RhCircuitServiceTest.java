package com.bakouan.app.service;

import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RhCircuitServiceTest {
    BaUserRepository users = mock(BaUserRepository.class);
    BaDgaPoleValidateurRepository assignments = mock(BaDgaPoleValidateurRepository.class);
    BaRhCircuitRepository circuits = mock(BaRhCircuitRepository.class);
    BaDepartement department = new BaDepartement();
    BaUser agent, chief, peer, director, operations, development, dg;
    RhCircuitService service;
    SignatureService signatures = mock(SignatureService.class);

    @Test void missingSignatureDoesNotAdvanceOrRecordAnApproval() {
        service = new RhCircuitService(users, assignments, circuits, true, "DEFINITIF", signatures);
        BaRhCircuit circuit = service.freeze(agent);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Ma signature"))
                .when(signatures).capture(eq("CIRCUIT"), eq(circuit.getId()), eq(0), eq(chief), any());
        assertThatThrownBy(() -> service.decide(circuit, chief, 0, true, null)).isInstanceOf(ResponseStatusException.class);
        assertThat(circuit.getHistorique()).isEmpty(); assertThat(circuit.getEtapeCourante()).isZero(); assertThat(circuit.getStatut()).isEqualTo("EN_ATTENTE");
    }

    @BeforeEach void setup() {
        department.setId("dept");
        agent=user("agent", EFonctionEmploye.AGENT); chief=user("chief", EFonctionEmploye.CHEF_SERVICE);
        peer=user("peer", EFonctionEmploye.CHEF_SERVICE); director=user("director", EFonctionEmploye.DIRECTEUR);
        operations=user("operations", EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT);
        development=user("development", EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT); dg=user("dg", EFonctionEmploye.DIRECTEUR_GENERAL);
        when(users.findByStatut(EStatut.A)).thenReturn(List.of(agent, chief, peer, director, operations, development, dg));
        when(assignments.findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut.A)).thenReturn(List.of(assignment(EDgaPole.OPERATIONS, operations), assignment(EDgaPole.DEVELOPPEMENT, development)));
        when(circuits.save(any())).thenAnswer(i -> i.getArgument(0));
        service=new RhCircuitService(users, assignments, circuits, true, "DEFINITIF", mock(SignatureService.class));
    }

    @Test void agentStartsWithAllDepartmentChiefsAndStrictOrder() {
        BaService own=new BaService(); own.setId("own-service"); own.setDepartement(department); agent.setService(own);
        BaService other=new BaService(); other.setId("other-service"); other.setDepartement(department); peer.setService(other);
        BaRhCircuit c=service.freeze(agent);
        assertThat(c.getEtapes()).extracting(e -> e.getType()).containsExactly("CHEF_SERVICE","DIRECTEUR","DGA","DG");
        assertThat(service.canDecide(c, peer)).isTrue();
        assertThat(service.canDecide(c, director)).isFalse();
        assertThat(service.canDecide(c, operations)).isFalse();
        assertThat(service.canDecide(c, agent)).isFalse();
        assertThat(service.decide(c,peer,0,true,"OK")).isEqualTo("EN_ATTENTE");
        assertThat(c.getEtapeCourante()).isEqualTo(1);
        assertThat(c.getEtapes().get(0).getIdSignataire()).isEqualTo("peer");
        assertThatThrownBy(() -> service.decide(c,chief,0,true,"duplicate")).isInstanceOf(ResponseStatusException.class);
        assertThat(c.getHistorique()).hasSize(1);
    }

    @Test void foreignChiefAndTechnicalAdminCannotSign() {
        BaRhCircuit c=service.freeze(agent);
        BaDepartement foreign=new BaDepartement(); foreign.setId("foreign");
        BaUser foreignChief=user("foreign-chief", EFonctionEmploye.CHEF_SERVICE); foreignChief.setDepartement(foreign);
        BaUser admin=user("admin", EFonctionEmploye.AGENT);
        BaRole role=new BaRole(); role.setCode("BA_ADMIN"); admin.getRoles().add(role);
        assertThat(service.canDecide(c,foreignChief)).isFalse(); assertThat(service.canDecide(c,admin)).isFalse();
    }

    @Test void chiefsStartAtDirectorAndDirectorsAtDga() {
        assertThat(service.freeze(chief).getEtapes()).extracting(e -> e.getType()).containsExactly("DIRECTEUR","DGA","DG");
        assertThat(service.freeze(director).getEtapes()).extracting(e -> e.getType()).containsExactly("DGA","DG");
    }

    @ParameterizedTest @EnumSource(value=EFonctionEmploye.class,names={"AGENT","CHEF_SERVICE","DIRECTEUR"})
    void oneDgaSufficesButDgMustApprove(EFonctionEmploye function) {
        BaUser requester=function==EFonctionEmploye.AGENT?agent:function==EFonctionEmploye.CHEF_SERVICE?chief:director;
        BaRhCircuit c=service.freeze(requester);
        if (function==EFonctionEmploye.AGENT) service.decide(c,peer,c.getEtapeCourante(),true,"");
        if (function!=EFonctionEmploye.DIRECTEUR) service.decide(c,director,c.getEtapeCourante(),true,"");
        int step=c.getEtapeCourante();
        assertThat(service.decide(c,operations,step,true,"")).isEqualTo("EN_ATTENTE");
        assertThat(service.canDecide(c,development)).isFalse();
        assertThatThrownBy(() -> service.decide(c,development,step,true,"duplicate")).isInstanceOf(ResponseStatusException.class);
        assertThat(service.decide(c,dg,c.getEtapeCourante(),true,"")).isEqualTo("VALIDEE");
    }

    @Test void eachDgaCanOnlyBeValidatedByTheOtherDga() {
        for (BaUser requester : List.of(operations, development)) {
            BaRhCircuit c=service.freeze(requester);
            BaUser other=requester==operations?development:operations;
            assertThat(c.getEtapes()).extracting(e -> e.getType()).containsExactly("AUTRE_DGA","DG");
            assertThat(service.canDecide(c,requester)).isFalse(); assertThat(service.canDecide(c,dg)).isFalse();
            assertThat(service.canDecide(c,other)).isTrue();
        }
    }

    @Test void missingOrUnavailableOtherDgaBlocksSubmission() {
        development.setLocked(true);
        assertThatThrownBy(() -> service.freeze(operations)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("autre DGA");
        when(assignments.findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut.A)).thenReturn(List.of(assignment(EDgaPole.OPERATIONS,operations)));
        assertThatThrownBy(() -> service.freeze(operations)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("autre DGA");
    }

    @Test void dgRequesterAndMissingOrganizationalFunctionAreBlocked() {
        assertThatThrownBy(() -> service.freeze(dg)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("pas defini");
        agent.setFonction(null);
        assertThatThrownBy(() -> service.freeze(agent)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("fonction organisationnelle");
    }

    @Test void blockedPreviewStillShowsApplicableStagesWithoutInventingApprovals() {
        development.setLocked(true);
        var preview=service.preview(operations);
        assertThat(preview.getBlocage()).contains("autre DGA");
        assertThat(preview.getEtapes()).extracting(e -> e.getType()).containsExactly("AUTRE_DGA","DG");
        assertThat(preview.getEtapes()).allSatisfy(e -> assertThat(e.getStatut()).isEqualTo("EN_ATTENTE"));
        assertThat(preview.isPeutDecider()).isFalse();
    }

    @Test void disabledDgFinalizesAtLastMandatoryStep() {
        service=new RhCircuitService(users,assignments,circuits,false,"DEFINITIF", mock(SignatureService.class));
        BaRhCircuit c=service.freeze(director);
        assertThat(c.getEtapes()).hasSize(1);
        assertThat(service.decide(c,development,0,true,"OK")).isEqualTo("VALIDEE");
    }

    @Test void unconfiguredRefusalBlocksAnyDecisionInAnAlternativeGroup() {
        service=new RhCircuitService(users,assignments,circuits,true,"NON_CONFIGURE", mock(SignatureService.class));
        BaRhCircuit c=service.freeze(agent);
        assertThatThrownBy(() -> service.decide(c,chief,0,true,"OK")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("refus");
        assertThat(c.getHistorique()).isEmpty();
    }

    @Test void bothExplicitRefusalPoliciesPreserveActualSigners() {
        BaRhCircuit definitive=service.freeze(agent);
        assertThat(service.decide(definitive,chief,0,false,"Motif")).isEqualTo("REJETEE");
        assertThat(service.canDecide(definitive,peer)).isFalse();
        service=new RhCircuitService(users,assignments,circuits,true,"AUTRES_VALIDATEURS", mock(SignatureService.class));
        BaRhCircuit alternative=service.freeze(agent);
        assertThat(service.decide(alternative,chief,0,false,"Motif")).isEqualTo("EN_ATTENTE");
        assertThat(service.canDecide(alternative,chief)).isFalse();
        assertThat(service.decide(alternative,peer,0,true,"OK")).isEqualTo("EN_ATTENTE");
        assertThat(alternative.getHistorique()).extracting(d -> d.idSignataire()).containsExactly("chief","peer");
        assertThat(alternative.getEtapes().get(0).getIdSignataire()).isEqualTo("peer");
    }

    @Test void frozenCircuitDoesNotChangeWithLaterServerConfiguration() {
        BaRhCircuit c=service.freeze(director);
        service=new RhCircuitService(users,assignments,circuits,false,"AUTRES_VALIDATEURS", mock(SignatureService.class));
        assertThat(service.decide(c,operations,0,true,"")).isEqualTo("EN_ATTENTE");
        assertThat(c.isDgObligatoire()).isTrue(); assertThat(c.getPolitiqueRefus()).isEqualTo("DEFINITIF");
    }

    @Test void missingDgPreviewRetainsRequiredStepAndGivesSpecificConfigurationHelp() {
        when(users.findByStatut(EStatut.A)).thenReturn(List.of(agent,chief,peer,director,operations,development));
        for(String code:List.of("ABSENCE","SORTIE")) {
            var preview=service.previewFor(code,agent);
            assertThat(preview.isDgObligatoire()).isTrue();assertThat(preview.getFonctionDemandeur()).isEqualTo("AGENT");
            assertThat(preview.getEtapeBloquee()).isEqualTo("DG");assertThat(preview.getBlocage()).contains("Gestion du DG","actif");
            assertThat(preview.getEtapes()).extracting(e->e.getType()).containsExactly("CHEF_SERVICE","DIRECTEUR","DGA","DG");
            assertThat(preview.getEtapes()).allSatisfy(e->assertThat(e.getStatut()).isEqualTo("EN_ATTENTE"));assertThat(preview.isPeutDecider()).isFalse();
            assertThatThrownBy(()->service.freezeFor(code,agent)).isInstanceOf(ResponseStatusException.class);
        }
        verify(circuits,never()).save(any());
    }
    @Test void technicalDgOrAdministratorRoleCannotSubstituteForTheDgFunction() {
        var role=new BaRole();role.setCode("BA_DG");director.getRoles().add(role);var adminRole=new BaRole();adminRole.setCode("BA_ADMIN");director.getRoles().add(adminRole);
        dg.setLocked(true);
        assertThat(service.previewFor("ABSENCE",director).getEtapeBloquee()).isEqualTo("DG");
    }
    @Test void configuringTheDgUnblocksTheSamePreviewWithoutRemovingTheStage() {
        dg.setActivated(false);assertThat(service.previewFor("SORTIE",agent).getEtapeBloquee()).isEqualTo("DG");
        dg.setActivated(true);lookupCandidates();var preview=service.previewFor("SORTIE",agent);
        assertThat(preview.getBlocage()).isNull();assertThat(preview.getEtapes()).hasSize(4);assertThat(preview.getEtapes().get(3).getValidateurs()).extracting(v->v.id()).containsExactly("dg");
    }
    @Test void disabledDgAllowsTheCircuitEvenWhenNoDgIsConfigured() {
        service=new RhCircuitService(users,assignments,circuits,false,"DEFINITIF", mock(SignatureService.class));dg.setActivated(false);lookupCandidates();
        var preview=service.previewFor("ABSENCE",agent);assertThat(preview.isDgObligatoire()).isFalse();assertThat(preview.getBlocage()).isNull();assertThat(preview.getEtapes()).extracting(e->e.getType()).containsExactly("CHEF_SERVICE","DIRECTEUR","DGA");
    }
    @Test void blockedPublishedPreviewUsesItsOwnDgSettingRatherThanGlobalDefault() {
        bindPublished(false);when(assignments.findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut.A)).thenReturn(List.of());
        var preview=service.previewFor("ABSENCE",agent);
        assertThat(preview.getPublicationId()).isEqualTo("published");assertThat(preview.isDgObligatoire()).isFalse();assertThat(preview.getEtapeBloquee()).isEqualTo("DGA");
        assertThat(preview.getEtapes()).extracting(e->e.getType()).containsExactly("CHEF_SERVICE","DIRECTEUR","DGA");
    }
    @Test void publishedRequiredDgOverridesDisabledGlobalDefaultInBlockedPreview() {
        service=new RhCircuitService(users,assignments,circuits,false,"DEFINITIF", mock(SignatureService.class));bindPublished(true);dg.setLocked(true);
        var preview=service.previewFor("ABSENCE",agent);assertThat(preview.isDgObligatoire()).isTrue();assertThat(preview.getPublicationId()).isEqualTo("published");assertThat(preview.getEtapeBloquee()).isEqualTo("DG");assertThat(preview.getEtapes()).hasSize(4);
    }
    private void bindPublished(boolean dgRequired) {
        var repository=mock(BaDemarcheRepository.class);org.springframework.test.util.ReflectionTestUtils.setField(service,"demarches",repository);
        var publication=new BaCircuitPublication();publication.setId("published");publication.setDefinition(new com.bakouan.app.dto.ProcessusDtos.Definition("HIERARCHIQUE","DEFINITIF",dgRequired,List.of()));
        var binding=new BaDemarche();binding.setActif(true);binding.setPublication(publication);when(repository.findByCode("ABSENCE")).thenReturn(Optional.of(binding));
    }
    private void lookupCandidates(){when(users.findById(anyString())).thenAnswer(i->List.of(agent,chief,peer,director,operations,development,dg).stream().filter(u->u.getId().equals(i.getArgument(0))).findFirst());}

    private BaUser user(String id, EFonctionEmploye function) {
        BaUser u=new BaUser(id); u.setFonction(function); u.setDepartement(department); u.setNom(id);
        u.setStatut(EStatut.A); u.setActivated(true); u.setLocked(false); return u;
    }
    private BaDgaPoleValidateur assignment(EDgaPole pole, BaUser user) {
        BaDgaPoleValidateur a=new BaDgaPoleValidateur(); a.setPole(pole); a.setDga(user); a.setDateDebut(LocalDate.now().minusDays(1)); return a;
    }
}

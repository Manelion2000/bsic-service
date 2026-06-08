package com.bakouan.app.service.impl;

import com.bakouan.app.dto.*;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.service.BaLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaBusinessServiceImplTest {

    @Mock
    private BaLogService logService;
    @Mock
    private BaServiceRepository serviceRepository;
    @Mock
    private BaDepartementRepository departementRepository;
    @Mock
    private BaAgenceRepository agenceRepository;
    @Mock
    private BaUserRepository userRepository;
    @Mock
    private BaPlateformeRepository plateformeRepository;
    //@Mock
            // private BaPlateformeEtapeConfigRepository plateformeEtapeConfigRepository;
    @Mock
    private BaEtapeDefinitionRepository etapeDefinitionRepository;
    @Mock
    private BaCircuitRepository circuitRepository;
    @Mock
    private BaCircuitEtapeRepository circuitEtapeRepository;
    @Mock
    private BaFicheHabilitationRepository ficheHabilitationRepository;
    @Mock
    private BaFicheHabilitationEtapeRepository ficheHabilitationEtapeRepository;
    @Mock
    private BaReunionRepository reunionRepository;
    @Mock
    private BaReunionParticipantRepository reunionParticipantRepository;
    @Mock
    private BaReunionActionRepository reunionActionRepository;
    @Mock
    private BaUserService userService;

    @InjectMocks
    private BaBusinessServiceImpl service;

    @Test
    void updateService_updatesFieldsAndDepartement() {
        BaService entity = new BaService();
        entity.setId("S1");
        entity.setCode("SVC1");
        entity.setNom("Service 1");
        when(serviceRepository.findById("S1")).thenReturn(Optional.of(entity));
        when(serviceRepository.existsByCode("SVC2")).thenReturn(false);
        BaDepartement departement = new BaDepartement();
        departement.setId("D1");
        departement.setNom("IT");
        when(departementRepository.findById("D1")).thenReturn(Optional.of(departement));
        when(serviceRepository.save(any(BaService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaServiceDto dto = new BaServiceDto();
        dto.setCode("SVC2");
        dto.setNom("Service 2");
        dto.setIdDepartement("D1");

        BaServiceDto result = service.updateService("S1", dto);

        assertEquals("SVC2", result.getCode());
        assertEquals("Service 2", result.getNom());
        assertEquals("D1", result.getIdDepartement());
    }

    @Test
    void updateDepartement_updatesFields() {
        BaDepartement entity = new BaDepartement();
        entity.setId("D1");
        entity.setCode("DEP1");
        entity.setNom("Old");
        when(departementRepository.findById("D1")).thenReturn(Optional.of(entity));
        when(departementRepository.existsByCode("DEP2")).thenReturn(false);
        when(departementRepository.save(any(BaDepartement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaDepartementDto dto = new BaDepartementDto();
        dto.setCode("DEP2");
        dto.setNom("IT");

        BaDepartementDto result = service.updateDepartement("D1", dto);

        assertEquals("DEP2", result.getCode());
        assertEquals("IT", result.getNom());
    }

    @Test
    void updateAgence_updatesFields() {
        BaAgence entity = new BaAgence();
        entity.setId("A1");
        entity.setCode("AG1");
        entity.setNom("Old");
        when(agenceRepository.findById("A1")).thenReturn(Optional.of(entity));
        when(agenceRepository.existsByCode("AG2")).thenReturn(false);
        when(agenceRepository.save(any(BaAgence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaAgenceDto dto = new BaAgenceDto();
        dto.setCode("AG2");
        dto.setNom("Agence 2");
        dto.setAdresse("Adresse");
        dto.setIdDepartement("D1");
        BaDepartement departement = new BaDepartement();
        departement.setId("D1");
        when(departementRepository.findById("D1")).thenReturn(Optional.of(departement));

        BaAgenceDto result = service.updateAgence("A1", dto);

        assertEquals("AG2", result.getCode());
        assertEquals("Agence 2", result.getNom());
        assertEquals("Adresse", result.getAdresse());
    }

    @Test
    void updatePlateforme_updatesFields() {
        BaPlateforme entity = new BaPlateforme();
        entity.setId("P1");
        entity.setCode("PL1");
        entity.setNom("Old");
        when(plateformeRepository.findById("P1")).thenReturn(Optional.of(entity));
        when(plateformeRepository.existsByCode("PL2")).thenReturn(false);
        when(plateformeRepository.save(any(BaPlateforme.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaPlateformeDto dto = new BaPlateformeDto();
        dto.setCode("PL2");
        dto.setNom("Plateforme 2");

        BaPlateformeDto result = service.updatePlateforme("P1", dto);

        assertEquals("PL2", result.getCode());
        assertEquals("Plateforme 2", result.getNom());
    }

    @Test
    void updateCircuit_updatesFieldsAndPlateforme() {
        BaCircuit entity = new BaCircuit();
        entity.setId("C1");
        entity.setCode("COD1");
        entity.setLibelle("Old");
        when(circuitRepository.findById("C1")).thenReturn(Optional.of(entity));
        when(circuitRepository.existsByCode("COD2")).thenReturn(false);
        BaPlateforme plateforme = new BaPlateforme();
        plateforme.setId("P1");
        plateforme.setNom("Plateforme 1");
        when(plateformeRepository.findById("P1")).thenReturn(Optional.of(plateforme));
        when(circuitRepository.save(any(BaCircuit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaCircuitDto dto = new BaCircuitDto();
        dto.setCode("COD2");
        dto.setLibelle("Circuit 2");
        dto.setDescription("Desc");
        dto.setIdPlateforme("P1");

        BaCircuitDto result = service.updateCircuit("C1", dto);

        assertEquals("COD2", result.getCode());
        assertEquals("Circuit 2", result.getLibelle());
        assertEquals("Desc", result.getDescription());
        assertEquals("P1", result.getIdPlateforme());
    }

    @Test
    void updateEtapeDefinition_updatesFieldsAndRelations() {
        BaEtapeDefinition entity = new BaEtapeDefinition();
        entity.setId("E1");
        when(etapeDefinitionRepository.findById("E1")).thenReturn(Optional.of(entity));
        BaDepartement departement = new BaDepartement();
        departement.setId("D1");
        departement.setNom("IT");
        when(departementRepository.findById("D1")).thenReturn(Optional.of(departement));
        when(etapeDefinitionRepository.save(any(BaEtapeDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaEtapeDefinitionDto dto = new BaEtapeDefinitionDto();
        dto.setType(EHabilitationEtapeType.DEPARTEMENT);
        dto.setIdDepartement("D1");
        dto.setFonctionRequise(EFonctionEmploye.AGENT);
        dto.setRoleCode("ROLE_IT");
        dto.setLibelle("Validation IT");

        BaEtapeDefinitionDto result = service.updateEtapeDefinition("E1", dto);

        assertEquals(EHabilitationEtapeType.DEPARTEMENT, result.getType());
        assertEquals("D1", result.getIdDepartement());
        assertEquals(EFonctionEmploye.AGENT, result.getFonctionRequise());
        assertEquals("ROLE_IT", result.getRoleCode());
        assertEquals("Validation IT", result.getLibelle());
    }

    @Test
    void updateReunion_updatesFields() {
        BaReunion entity = new BaReunion();
        entity.setId("R1");
        when(reunionRepository.findById("R1")).thenReturn(Optional.of(entity));
        when(reunionRepository.save(any(BaReunion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaReunionDto dto = new BaReunionDto();
        dto.setTitre("Titre");
        dto.setObjet("Objet");
        dto.setDateHeure(Instant.parse("2026-03-03T10:15:30Z"));
        dto.setProcesVerbalId("PV1");
        dto.setValideParDirecteur(Boolean.TRUE);

        BaReunionDto result = service.updateReunion("R1", dto);

        assertEquals("Titre", result.getTitre());
        assertEquals("Objet", result.getObjet());
        assertEquals("PV1", result.getProcesVerbalId());
        assertEquals(Boolean.TRUE, result.getValideParDirecteur());
        assertEquals(dto.getDateHeure(), result.getDateHeure());
    }

    @Test
    void updateReunionParticipant_updatesFields() {
        BaReunionParticipant entity = new BaReunionParticipant();
        entity.setId("RP1");
        when(reunionParticipantRepository.findById("RP1")).thenReturn(Optional.of(entity));
        BaReunion reunion = new BaReunion();
        reunion.setId("R1");
        when(reunionRepository.findById("R1")).thenReturn(Optional.of(reunion));
        BaUser employe = new BaUser();
        employe.setId("U1");
        when(userRepository.findById("U1")).thenReturn(Optional.of(employe));
        when(reunionParticipantRepository.save(any(BaReunionParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaReunionParticipantDto dto = new BaReunionParticipantDto();
        dto.setIdReunion("R1");
        dto.setIdEmploye("U1");
        dto.setParticipantExterne("Ext");

        BaReunionParticipantDto result = service.updateReunionParticipant("RP1", dto);

        assertEquals("R1", result.getIdReunion());
        assertEquals("U1", result.getIdEmploye());
        assertEquals("Ext", result.getParticipantExterne());
    }

    @Test
    void updateReunionAction_updatesFields() {
        BaReunionAction entity = new BaReunionAction();
        entity.setId("RA1");
        when(reunionActionRepository.findById("RA1")).thenReturn(Optional.of(entity));
        BaReunion reunion = new BaReunion();
        reunion.setId("R1");
        when(reunionRepository.findById("R1")).thenReturn(Optional.of(reunion));
        BaUser responsable = new BaUser();
        responsable.setId("U2");
        when(userRepository.findById("U2")).thenReturn(Optional.of(responsable));
        when(reunionActionRepository.save(any(BaReunionAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BaReunionActionDto dto = new BaReunionActionDto();
        dto.setIdReunion("R1");
        dto.setLibelle("Action");
        dto.setIdResponsable("U2");
        dto.setDateLimite(Instant.parse("2026-03-10T00:00:00Z"));
        dto.setTerminee(Boolean.TRUE);

        BaReunionActionDto result = service.updateReunionAction("RA1", dto);

        assertEquals("R1", result.getIdReunion());
        assertEquals("Action", result.getLibelle());
        assertEquals("U2", result.getIdResponsable());
        assertEquals(dto.getDateLimite(), result.getDateLimite());
        assertEquals(Boolean.TRUE, result.getTerminee());
    }

    @Test
    void getMyFicheValidations_returnsCurrentDepartmentStepForDirector() {
        BaUser directeur = buildUser("U-DIT", EFonctionEmploye.DIRECTEUR, "D-IT", null);
        BaUserDto current = new BaUserDto();
        current.setId(directeur.getId());
        when(userService.getUserInfoWithMoreDetails()).thenReturn(current);
        when(userRepository.findById(directeur.getId())).thenReturn(Optional.of(directeur));

        BaFicheHabilitation fiche = buildFiche("F1");
        BaFicheHabilitationEtape etapeRh = buildEtape("E-RH", fiche, 1, EHabilitationStatut.VALIDE,
                buildDefinition("DEF-RH", EHabilitationEtapeType.DEPARTEMENT, EFonctionEmploye.DIRECTEUR, "D-RH", null));
        BaFicheHabilitationEtape etapeDit = buildEtape("E-DIT", fiche, 2, EHabilitationStatut.EN_ATTENTE,
                buildDefinition("DEF-DIT", EHabilitationEtapeType.DEPARTEMENT, EFonctionEmploye.DIRECTEUR, "D-IT", null));
        when(ficheHabilitationRepository.findByStatut(com.bakouan.app.enums.EStatut.A)).thenReturn(List.of(fiche));
        when(ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre("F1")).thenReturn(List.of(etapeRh, etapeDit));

        org.springframework.data.domain.Page<BaFicheHabilitationDto> result =
                service.getMyFicheValidations(null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("F1", result.getContent().get(0).getId());
    }

    @Test
    void getMyFicheValidations_hidesDepartmentStepUntilItIsCurrent() {
        BaUser directeur = buildUser("U-DIT", EFonctionEmploye.DIRECTEUR, "D-IT", null);
        BaUserDto current = new BaUserDto();
        current.setId(directeur.getId());
        when(userService.getUserInfoWithMoreDetails()).thenReturn(current);
        when(userRepository.findById(directeur.getId())).thenReturn(Optional.of(directeur));

        BaFicheHabilitation fiche = buildFiche("F1");
        BaFicheHabilitationEtape etapeRh = buildEtape("E-RH", fiche, 1, EHabilitationStatut.EN_ATTENTE,
                buildDefinition("DEF-RH", EHabilitationEtapeType.DEPARTEMENT, EFonctionEmploye.DIRECTEUR, "D-RH", null));
        BaFicheHabilitationEtape etapeDit = buildEtape("E-DIT", fiche, 2, EHabilitationStatut.EN_ATTENTE,
                buildDefinition("DEF-DIT", EHabilitationEtapeType.DEPARTEMENT, EFonctionEmploye.DIRECTEUR, "D-IT", null));
        when(ficheHabilitationRepository.findByStatut(com.bakouan.app.enums.EStatut.A)).thenReturn(List.of(fiche));
        when(ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre("F1")).thenReturn(List.of(etapeRh, etapeDit));

        org.springframework.data.domain.Page<BaFicheHabilitationDto> result =
                service.getMyFicheValidations(null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getMyFicheValidations_usesDirectorForDepartmentStepEvenWithLegacyFunction() {
        BaUser directeur = buildUser("U-DIT", EFonctionEmploye.DIRECTEUR, "D-IT", null);
        BaUserDto current = new BaUserDto();
        current.setId(directeur.getId());
        when(userService.getUserInfoWithMoreDetails()).thenReturn(current);
        when(userRepository.findById(directeur.getId())).thenReturn(Optional.of(directeur));

        BaFicheHabilitation fiche = buildFiche("F1");
        BaEtapeDefinition definition = buildDefinition(
                "DEF-DIT",
                EHabilitationEtapeType.DEPARTEMENT,
                EFonctionEmploye.CHEF_SERVICE,
                "D-IT",
                null);
        BaFicheHabilitationEtape etapeDit = buildEtape("E-DIT", fiche, 1, EHabilitationStatut.EN_ATTENTE, definition);
        when(ficheHabilitationRepository.findByStatut(com.bakouan.app.enums.EStatut.A)).thenReturn(List.of(fiche));
        when(ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre("F1")).thenReturn(List.of(etapeDit));

        org.springframework.data.domain.Page<BaFicheHabilitationDto> result =
                service.getMyFicheValidations(null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("F1", result.getContent().get(0).getId());
    }

    @Test
    void updateEtapeStatus_allowsDepartmentDirectorOnCurrentStep() {
        BaUser directeur = buildUser("U-DIT", EFonctionEmploye.DIRECTEUR, "D-IT", null);
        BaUserDto current = new BaUserDto();
        current.setId(directeur.getId());
        when(userService.getUserInfoWithMoreDetails()).thenReturn(current);
        when(userRepository.findById(directeur.getId())).thenReturn(Optional.of(directeur));

        BaFicheHabilitation fiche = buildFiche("F1");
        BaFicheHabilitationEtape etapeDit = buildEtape("E-DIT", fiche, 1, EHabilitationStatut.EN_ATTENTE,
                buildDefinition("DEF-DIT", EHabilitationEtapeType.DEPARTEMENT, EFonctionEmploye.DIRECTEUR, "D-IT", null));
        when(ficheHabilitationEtapeRepository.findById("E-DIT")).thenReturn(Optional.of(etapeDit));
        when(ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre("F1")).thenReturn(List.of(etapeDit));
        when(ficheHabilitationEtapeRepository.save(any(BaFicheHabilitationEtape.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ficheHabilitationRepository.save(any(BaFicheHabilitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BaFicheHabilitationEtapeDto result = service.updateEtapeStatus("E-DIT", EHabilitationStatut.VALIDE, null);

        assertEquals(EHabilitationStatut.VALIDE, result.getStatutValidation());
        assertEquals(directeur.getId(), result.getIdValidateur());
        assertEquals(com.bakouan.app.enums.EHabilitationFicheStatut.VALIDEE, fiche.getStatutHabilitation());
    }

    @Test
    void initFicheEtapesFromCircuit_createsMissingDepartmentDefinition() {
        BaCircuit circuit = new BaCircuit();
        circuit.setId("C1");
        BaFicheHabilitation fiche = buildFiche("F1");
        fiche.setCircuit(circuit);

        BaDepartement departement = new BaDepartement();
        departement.setId("D-IT");
        departement.setNom("Departement IT");

        BaCircuitEtape circuitEtape = new BaCircuitEtape();
        circuitEtape.setId("CE1");
        circuitEtape.setCircuit(circuit);
        circuitEtape.setOrdre(1);
        circuitEtape.setType(EHabilitationEtapeType.DEPARTEMENT);
        circuitEtape.setDepartement(departement);

        BaUser directeur = buildUser("U-DIT", EFonctionEmploye.DIRECTEUR, "D-IT", null);

        when(ficheHabilitationRepository.findById("F1")).thenReturn(Optional.of(fiche));
        when(circuitEtapeRepository.findByCircuitIdOrderByOrdre("C1")).thenReturn(List.of(circuitEtape));
        when(ficheHabilitationEtapeRepository.findByFicheId("F1")).thenReturn(List.of());
        when(etapeDefinitionRepository.findByDepartementId("D-IT")).thenReturn(List.of());
        when(userRepository.findByFonctionAndDepartementIdAndStatut(
                EFonctionEmploye.DIRECTEUR, "D-IT", com.bakouan.app.enums.EStatut.A))
                .thenReturn(List.of(directeur));
        when(ficheHabilitationEtapeRepository.save(any(BaFicheHabilitationEtape.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre("F1")).thenAnswer(invocation -> {
            BaEtapeDefinition definition = buildDefinition(
                    "DEF-DIT",
                    EHabilitationEtapeType.DEPARTEMENT,
                    EFonctionEmploye.DIRECTEUR,
                    "D-IT",
                    null);
            BaFicheHabilitationEtape savedEtape = buildEtape(
                    "E-DIT",
                    fiche,
                    1,
                    EHabilitationStatut.EN_ATTENTE,
                    definition);
            savedEtape.setValidateur(directeur);
            return List.of(savedEtape);
        });

        List<BaFicheHabilitationEtapeDto> result = service.initFicheEtapesFromCircuit("F1");

        assertEquals(1, result.size());
        assertEquals("D-IT", result.get(0).getIdDepartement());
        assertEquals("U-DIT", result.get(0).getIdValidateur());
    }

    @Test
    void getMyFicheValidations_initializesExistingFicheWithoutEtapes() {
        BaUser directeur = buildUser("U-DIT", EFonctionEmploye.DIRECTEUR, "D-IT", null);
        BaUserDto current = new BaUserDto();
        current.setId(directeur.getId());
        when(userService.getUserInfoWithMoreDetails()).thenReturn(current);
        when(userRepository.findById(directeur.getId())).thenReturn(Optional.of(directeur));

        BaCircuit circuit = new BaCircuit();
        circuit.setId("C1");
        BaFicheHabilitation fiche = buildFiche("F1");
        fiche.setCircuit(circuit);

        BaDepartement departement = new BaDepartement();
        departement.setId("D-IT");
        departement.setNom("Departement IT");

        BaCircuitEtape circuitEtape = new BaCircuitEtape();
        circuitEtape.setId("CE1");
        circuitEtape.setCircuit(circuit);
        circuitEtape.setOrdre(1);
        circuitEtape.setType(EHabilitationEtapeType.DEPARTEMENT);
        circuitEtape.setDepartement(departement);

        BaEtapeDefinition definition = buildDefinition(
                "DEF-DIT",
                EHabilitationEtapeType.DEPARTEMENT,
                EFonctionEmploye.DIRECTEUR,
                "D-IT",
                null);
        BaFicheHabilitationEtape currentEtape = buildEtape(
                "E-DIT",
                fiche,
                1,
                EHabilitationStatut.EN_ATTENTE,
                definition);
        currentEtape.setValidateur(directeur);

        when(ficheHabilitationRepository.findByStatut(com.bakouan.app.enums.EStatut.A)).thenReturn(List.of(fiche));
        when(ficheHabilitationRepository.findById("F1")).thenReturn(Optional.of(fiche));
        when(circuitEtapeRepository.findByCircuitIdOrderByOrdre("C1")).thenReturn(List.of(circuitEtape));
        when(ficheHabilitationEtapeRepository.findByFicheId("F1")).thenReturn(List.of());
        when(ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre("F1"))
                .thenReturn(List.of())
                .thenReturn(List.of(currentEtape))
                .thenReturn(List.of(currentEtape));
        when(etapeDefinitionRepository.findByDepartementId("D-IT")).thenReturn(List.of(definition));
        when(userRepository.findByFonctionAndDepartementIdAndStatut(
                EFonctionEmploye.DIRECTEUR, "D-IT", com.bakouan.app.enums.EStatut.A))
                .thenReturn(List.of(directeur));
        when(ficheHabilitationEtapeRepository.save(any(BaFicheHabilitationEtape.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        org.springframework.data.domain.Page<BaFicheHabilitationDto> result =
                service.getMyFicheValidations(null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("F1", result.getContent().get(0).getId());
    }

    private BaFicheHabilitation buildFiche(final String id) {
        BaFicheHabilitation fiche = new BaFicheHabilitation();
        fiche.setId(id);
        fiche.setStatutHabilitation(com.bakouan.app.enums.EHabilitationFicheStatut.EN_COURS);
        return fiche;
    }

    private BaFicheHabilitationEtape buildEtape(final String id,
                                                final BaFicheHabilitation fiche,
                                                final Integer ordre,
                                                final EHabilitationStatut statut,
                                                final BaEtapeDefinition definition) {
        BaFicheHabilitationEtape etape = new BaFicheHabilitationEtape();
        etape.setId(id);
        etape.setFiche(fiche);
        etape.setOrdre(ordre);
        etape.setStatutValidation(statut);
        etape.setEtapeDefinition(definition);
        return etape;
    }

    private BaEtapeDefinition buildDefinition(final String id,
                                              final EHabilitationEtapeType type,
                                              final EFonctionEmploye fonction,
                                              final String departementId,
                                              final String serviceId) {
        BaEtapeDefinition definition = new BaEtapeDefinition();
        definition.setId(id);
        definition.setType(type);
        definition.setFonctionRequise(fonction);
        if (departementId != null) {
            BaDepartement departement = new BaDepartement();
            departement.setId(departementId);
            definition.setDepartement(departement);
        }
        if (serviceId != null) {
            BaService service = new BaService();
            service.setId(serviceId);
            definition.setService(service);
        }
        return definition;
    }

    private BaUser buildUser(final String id,
                             final EFonctionEmploye fonction,
                             final String departementId,
                             final String serviceId) {
        BaUser user = new BaUser();
        user.setId(id);
        user.setFonction(fonction);
        if (departementId != null) {
            BaDepartement departement = new BaDepartement();
            departement.setId(departementId);
            user.setDepartement(departement);
        }
        if (serviceId != null) {
            BaService service = new BaService();
            service.setId(serviceId);
            user.setService(service);
        }
        return user;
    }
}

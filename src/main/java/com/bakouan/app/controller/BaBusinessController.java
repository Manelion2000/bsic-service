package com.bakouan.app.controller;

import com.bakouan.app.dto.*;
import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.enums.EHabilitationFicheStatut;
import com.bakouan.app.service.BaBusinessService;
import com.bakouan.app.service.BaAutorisationSortieService;
import com.bakouan.app.service.BaRepriseServiceManager;
import com.bakouan.app.service.CircuitService;
import com.bakouan.app.utils.BaConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(BaConstants.URL.BASE_URL)
@Tag(name = "Annuaire", description = "Gestion de l'annuaire et des entites associees")
public class BaBusinessController {

    private final BaBusinessService businessService;
    private final CircuitService circuitService;
    private final BaAutorisationSortieService autorisationSortieService;
    private final BaRepriseServiceManager repriseServiceManager;

    @GetMapping(BaConstants.URL.PUBLIC + BaConstants.URL.ANNUAIRE + BaConstants.URL.SERVICE)
    public List<BaServiceDto> getPublicServices() {
        return businessService.getAllServices();
    }

    @GetMapping(BaConstants.URL.PUBLIC + BaConstants.URL.ANNUAIRE + BaConstants.URL.DEPARTEMENT)
    public List<BaDepartementDto> getPublicDepartements() {
        return businessService.getAllDepartements();
    }

    @GetMapping(BaConstants.URL.PUBLIC + BaConstants.URL.ANNUAIRE + BaConstants.URL.AGENCE)
    public List<BaAgenceDto> getPublicAgences() {
        return businessService.getAllAgences();
    }

    @Operation(summary = "Annuaire public", description = "Retourne la liste publique des agents avec les contacts professionnels uniquement.")
    @GetMapping(BaConstants.URL.PUBLIC + BaConstants.URL.ANNUAIRE + BaConstants.URL.ANNUAIRE_LIST)
    public List<BaPublicAnnuaireDepartementDto> getPublicAnnuaire(
            @Parameter(description = "Filtrer par departement") @RequestParam(value = "departementId", required = false) String departementId,
            @Parameter(description = "Filtrer par service") @RequestParam(value = "serviceId", required = false) String serviceId,
            @Parameter(description = "Filtrer par agence") @RequestParam(value = "agenceId", required = false) String agenceId) {
        return businessService.getPublicAnnuaire(departementId, serviceId, agenceId);
    }

    @GetMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.SERVICE)
    public List<BaServiceDto> getAllServices() {
        return businessService.getAllServices();
    }

    @PostMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.SERVICE)
    public ResponseEntity<BaServiceDto> createService(@RequestBody BaServiceDto dto) {
        return new ResponseEntity<>(businessService.createService(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.SERVICE + "/{id}")
    public ResponseEntity<BaServiceDto> updateService(@PathVariable String id, @RequestBody  BaServiceDto dto) {
        return ResponseEntity.ok(businessService.updateService(id, dto));
    }

    @GetMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.DEPARTEMENT)
    public List<BaDepartementDto> getAllDepartements() {
        return businessService.getAllDepartements();
    }

    @PostMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.DEPARTEMENT)
    public ResponseEntity<BaDepartementDto> createDepartement(@RequestBody @Valid BaDepartementDto dto) {
        return new ResponseEntity<>(businessService.createDepartement(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.DEPARTEMENT + "/{id}")
    public ResponseEntity<BaDepartementDto> updateDepartement(@PathVariable String id, @RequestBody @Valid BaDepartementDto dto) {
        return ResponseEntity.ok(businessService.updateDepartement(id, dto));
    }

    @GetMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.AGENCE)
    public List<BaAgenceDto> getAllAgences() {
        return businessService.getAllAgences();
    }

    @PostMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.AGENCE)
    public ResponseEntity<BaAgenceDto> createAgence(@RequestBody BaAgenceDto dto) {
        return new ResponseEntity<>(businessService.createAgence(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.AGENCE + "/{id}")
    public ResponseEntity<BaAgenceDto> updateAgence(@PathVariable String id, @RequestBody @jakarta.validation.Valid BaAgenceDto dto) {
        return ResponseEntity.ok(businessService.updateAgence(id, dto));
    }


    @Operation(summary = "Annuaire par departement", description = "Retourne la liste des agents groupee par departement.")
    @GetMapping(BaConstants.URL.ANNUAIRE + BaConstants.URL.ANNUAIRE_LIST)
    public List<BaAnnuaireDepartementDto> getAnnuaire(@Parameter(description = "Filtrer par departement") @RequestParam(value = "departementId", required = false) String departementId,
                                                      @Parameter(description = "Filtrer par service") @RequestParam(value = "serviceId", required = false) String serviceId,
                                                      @Parameter(description = "Filtrer par agence") @RequestParam(value = "agenceId", required = false) String agenceId) {
        return businessService.getAnnuaire(departementId, serviceId, agenceId);
    }

    @GetMapping(BaConstants.URL.HABILITATION + BaConstants.URL.PLATEFORME)
    public List<BaPlateformeDto> getAllPlateformes() {
        return businessService.getAllPlateformes();
    }

    @PostMapping(BaConstants.URL.HABILITATION + BaConstants.URL.PLATEFORME)
    public ResponseEntity<BaPlateformeDto> createPlateforme(@RequestBody BaPlateformeDto dto) {
        return new ResponseEntity<>(businessService.createPlateforme(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.HABILITATION + BaConstants.URL.PLATEFORME + "/{id}")
    public ResponseEntity<BaPlateformeDto> updatePlateforme(@PathVariable String id, @RequestBody @jakarta.validation.Valid BaPlateformeDto dto) {
        return ResponseEntity.ok(businessService.updatePlateforme(id, dto));
    }

    @PostMapping(BaConstants.URL.HABILITATION + BaConstants.URL.HABILITATION_ETAPES_DEF)
    public ResponseEntity<BaEtapeDefinitionDto> createEtapeDefinition(@RequestBody @Valid BaEtapeDefinitionDto dto) {
        return new ResponseEntity<>(businessService.createEtapeDefinition(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.HABILITATION + BaConstants.URL.HABILITATION_ETAPES_DEF + "/{id}")
    public ResponseEntity<BaEtapeDefinitionDto> updateEtapeDefinition(@PathVariable String id, @RequestBody @Valid BaEtapeDefinitionDto dto) {
        return ResponseEntity.ok(businessService.updateEtapeDefinition(id, dto));
    }

    @GetMapping(BaConstants.URL.HABILITATION + BaConstants.URL.HABILITATION_ETAPES_DEF)
    public List<BaEtapeDefinitionDto> getEtapeDefinitions(
            @RequestParam(value = "type", required = false) EHabilitationEtapeType type,
            @RequestParam(value = "departementId", required = false) String departementId,
            @RequestParam(value = "serviceId", required = false) String serviceId) {
        return businessService.getEtapeDefinitions(type, departementId, serviceId);
    }

    @GetMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION)
    public List<BaFicheHabilitationDto> getAllFichesHabilitation() {
        return businessService.getAllFichesHabilitation();
    }

    @GetMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION + "/{id}")
    public ResponseEntity<BaFicheHabilitationDto> getFicheHabilitationById(@PathVariable String id) {
        return ResponseEntity.ok(businessService.getFicheHabilitationById(id));
    }

    @PostMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION)
    public ResponseEntity<BaFicheHabilitationDto> createFicheHabilitation(@RequestBody BaFicheHabilitationDto dto) {
        return new ResponseEntity<>(businessService.createFicheHabilitation(dto), HttpStatus.CREATED);
    }

    @DeleteMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION + "/{id}")
    public ResponseEntity<Void> deleteFicheHabilitation(@PathVariable String id) {
        businessService.deleteFicheHabilitation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(BaConstants.URL.HABILITATION + BaConstants.URL.HABILITATION_MES_DEMANDES)
    public org.springframework.data.domain.Page<BaFicheHabilitationDto> getMyFicheDemandes(
            @RequestParam(value = "statut", required = false) EHabilitationFicheStatut statut,
            org.springframework.data.domain.Pageable pageable) {
        return businessService.getMyFicheDemandes(statut, pageable);
    }

    @GetMapping(BaConstants.URL.HABILITATION + BaConstants.URL.HABILITATION_MES_VALIDATIONS)
    public org.springframework.data.domain.Page<BaFicheHabilitationDto> getMyFicheValidations(
            @RequestParam(value = "statut", required = false) EHabilitationFicheStatut statut,
            org.springframework.data.domain.Pageable pageable) {
        return businessService.getMyFicheValidations(statut, pageable);
    }

    @PostMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION_ETAPES_INIT_CIRCUIT)
    public List<BaFicheHabilitationEtapeDto> initFicheEtapesFromCircuit(@PathVariable("id") String ficheId) {
        return businessService.initFicheEtapesFromCircuit(ficheId);
    }

    @PostMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION_ETAPE)
    public ResponseEntity<BaFicheHabilitationEtapeDto> addEtape(@RequestBody BaFicheHabilitationEtapeDto dto) {
        return new ResponseEntity<>(businessService.addEtape(dto), HttpStatus.CREATED);
    }

    @PutMapping(BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION_ETAPE + "/{id}/status")
    public ResponseEntity<BaFicheHabilitationEtapeDto> updateEtapeStatus(@PathVariable("id") String id,
                                                                         @RequestParam("statut") EHabilitationStatut statut,
                                                                         @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(businessService.updateEtapeStatus(id, statut, commentaire));
    }

    @GetMapping(BaConstants.URL.HABILITATION + "/comptes-plateforme")
    public List<BaHabilitationComptePlateformeDto> getComptesPlateforme(
            @RequestParam(value = "statutCreation", required = false)
            com.bakouan.app.enums.EHabilitationCompteStatut statutCreation) {
        return businessService.getComptesPlateforme(statutCreation);
    }

    @PutMapping(BaConstants.URL.HABILITATION + "/comptes-plateforme/{id}/demarrer")
    public ResponseEntity<BaHabilitationComptePlateformeDto> demarrerCreationCompte(
            @PathVariable String id,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(businessService.demarrerCreationCompte(id, commentaire));
    }

    @PutMapping(BaConstants.URL.HABILITATION + "/comptes-plateforme/{id}/marquer-cree")
    public ResponseEntity<BaHabilitationComptePlateformeDto> marquerCompteCree(
            @PathVariable String id,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(businessService.marquerCompteCree(id, commentaire));
    }

    @PostMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes")
    public ResponseEntity<BaReinitialisationCompteDto> createReinitialisationCompte(
            @RequestBody BaReinitialisationCompteRequestDto request) {
        return new ResponseEntity<>(businessService.createReinitialisationCompte(request), HttpStatus.CREATED);
    }

    @GetMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes/mes-demandes")
    public List<BaReinitialisationCompteDto> getMesReinitialisationsCompte() {
        return businessService.getMesReinitialisationsCompte();
    }

    @GetMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes/a-valider")
    public List<BaReinitialisationCompteDto> getReinitialisationsCompteAValider() {
        return businessService.getReinitialisationsCompteAValider();
    }

    @PutMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes/etapes/{id}/status")
    public ResponseEntity<BaReinitialisationCompteDto> updateReinitialisationEtapeStatus(
            @PathVariable("id") String id,
            @RequestParam("statut") EHabilitationStatut statut,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(businessService.updateReinitialisationEtapeStatus(id, statut, commentaire));
    }

    @GetMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes/a-traiter")
    public List<BaReinitialisationCompteDto> getReinitialisationsCompteATraiter(
            @RequestParam(value = "statutTraitement", required = false)
            com.bakouan.app.enums.EReinitialisationTraitementStatut statutTraitement) {
        return businessService.getReinitialisationsCompteATraiter(statutTraitement);
    }

    @PutMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes/traitements/{id}/demarrer")
    public ResponseEntity<BaReinitialisationCompteDto> demarrerTraitementReinitialisation(
            @PathVariable String id,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(businessService.demarrerTraitementReinitialisation(id, commentaire));
    }

    @PutMapping(BaConstants.URL.HABILITATION + "/reinitialisations-comptes/traitements/{id}/marquer-traitee")
    public ResponseEntity<BaReinitialisationCompteDto> marquerReinitialisationTraitee(
            @PathVariable String id,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(businessService.marquerReinitialisationTraitee(id, commentaire));
    }

    @GetMapping(BaConstants.URL.HABILITATION + "/statistiques/tableau-bord")
    public BaPlateformeDashboardStatsDto getPlateformeDashboardStats(@ModelAttribute BaPlateformeStatFilterDto filter) {
        return businessService.getPlateformeDashboardStats(filter);
    }

    @GetMapping(BaConstants.URL.HABILITATION + "/suivi-plateformes")
    public org.springframework.data.domain.Page<BaPlateformeUsageRowDto> getSuiviPlateformes(
            @ModelAttribute BaPlateformeStatFilterDto filter,
            org.springframework.data.domain.Pageable pageable) {
        return businessService.getSuiviPlateformes(filter, pageable);
    }

    @GetMapping(BaConstants.URL.REUNION)
    public List<BaReunionDto> getAllReunions() {
        return businessService.getAllReunions();
    }

    @PostMapping(BaConstants.URL.REUNION)
    public ResponseEntity<BaReunionDto> createReunion(@RequestBody BaReunionDto dto) {
        return new ResponseEntity<>(businessService.createReunion(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.REUNION + "/{id}")
    public ResponseEntity<BaReunionDto> updateReunion(@PathVariable String id, @RequestBody @jakarta.validation.Valid BaReunionDto dto) {
        return ResponseEntity.ok(businessService.updateReunion(id, dto));
    }

    @PostMapping(BaConstants.URL.REUNION_PARTICIPANT)
    public ResponseEntity<BaReunionParticipantDto> addReunionParticipant(@RequestBody BaReunionParticipantDto dto) {
        return new ResponseEntity<>(businessService.addReunionParticipant(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.REUNION_PARTICIPANT + "/{id}")
    public ResponseEntity<BaReunionParticipantDto> updateReunionParticipant(@PathVariable String id, @RequestBody @Valid BaReunionParticipantDto dto) {
        return ResponseEntity.ok(businessService.updateReunionParticipant(id, dto));
    }

    @PostMapping(BaConstants.URL.REUNION_ACTION)
    public ResponseEntity<BaReunionActionDto> addReunionAction(@RequestBody BaReunionActionDto dto) {
        return new ResponseEntity<>(businessService.addReunionAction(dto), HttpStatus.CREATED);
    }
    @PutMapping(BaConstants.URL.REUNION_ACTION + "/{id}")
    public ResponseEntity<BaReunionActionDto> updateReunionAction(@PathVariable String id, @RequestBody @Valid BaReunionActionDto dto) {
        return ResponseEntity.ok(businessService.updateReunionAction(id, dto));
    }

    // Circuit API fusionnee dans BaBusinessController
    @PostMapping(BaConstants.URL.CIRCUIT_V2)
    public ResponseEntity<CircuitResponseDto> createCircuitV2(@RequestBody CircuitRequestDto request) {
        return new ResponseEntity<>(circuitService.createCircuit(request), HttpStatus.CREATED);
    }

    @PutMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}")
    public ResponseEntity<CircuitResponseDto> updateCircuitV2(@PathVariable String id,
                                                              @RequestBody CircuitRequestDto request) {
        return ResponseEntity.ok(circuitService.updateCircuit(id, request));
    }

    @GetMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}")
    public ResponseEntity<CircuitResponseDto> getCircuitByIdV2(@PathVariable String id) {
        return ResponseEntity.ok(circuitService.getCircuitById(id));
    }

    @GetMapping(BaConstants.URL.CIRCUIT_V2)
    public List<CircuitResponseDto> getAllCircuitsV2() {
        return circuitService.getCircuits();
    }

    @GetMapping(BaConstants.URL.CIRCUIT_V2_ACTIFS)
    public List<CircuitResponseDto> getCircuitsActifsV2() {
        return circuitService.getCircuitsActifs();
    }

    @DeleteMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}")
    public ResponseEntity<Void> deleteCircuitV2(@PathVariable String id) {
        circuitService.deleteCircuit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}/activer")
    public ResponseEntity<CircuitResponseDto> activerCircuitV2(@PathVariable String id) {
        return ResponseEntity.ok(circuitService.activerCircuit(id));
    }

    @PostMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}/desactiver")
    public ResponseEntity<CircuitResponseDto> desactiverCircuitV2(@PathVariable String id) {
        return ResponseEntity.ok(circuitService.desactiverCircuit(id));
    }

    @PostMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}/dupliquer")
    public ResponseEntity<CircuitResponseDto> dupliquerCircuitV2(@PathVariable String id,
                                                                  @RequestParam String nouveauNom) {
        return new ResponseEntity<>(circuitService.dupliquerCircuit(id, nouveauNom), HttpStatus.CREATED);
    }

    @PostMapping(BaConstants.URL.CIRCUIT_V2 + "/{id}/tester")
    public ResponseEntity<Boolean> testerCircuitV2(@PathVariable String id) {
        return ResponseEntity.ok(circuitService.testerCircuit(id));
    }

    @GetMapping(BaConstants.URL.CIRCUIT_V2_ETAPES)
    public List<CircuitEtapeResponseDto> getCircuitEtapesV2(@PathVariable("id") String id) {
        return circuitService.getCircuitEtapes(id);
    }

    @PutMapping(BaConstants.URL.CIRCUIT_V2_ETAPES)
    public List<CircuitEtapeResponseDto> replaceCircuitEtapesV2(@PathVariable("id") String id,
                                                                 @RequestBody List<CircuitEtapeRequestDto> etapes) {
        return circuitService.replaceCircuitEtapes(id, etapes);
    }

    @PostMapping(BaConstants.URL.CIRCUIT_V2_ETAPES)
    public ResponseEntity<CircuitEtapeResponseDto> addCircuitEtapeV2(@PathVariable("id") String id,
                                                                      @RequestBody CircuitEtapeRequestDto etape) {
        return new ResponseEntity<>(circuitService.addCircuitEtape(id, etape), HttpStatus.CREATED);
    }

    @PutMapping(BaConstants.URL.CIRCUIT_V2_ETAPES + "/{etapeId}")
    public ResponseEntity<CircuitEtapeResponseDto> updateCircuitEtapeV2(@PathVariable("id") String id,
                                                                         @PathVariable String etapeId,
                                                                         @RequestBody CircuitEtapeRequestDto etape) {
        return ResponseEntity.ok(circuitService.updateCircuitEtape(id, etapeId, etape));
    }

    @DeleteMapping(BaConstants.URL.CIRCUIT_V2_ETAPES + "/{etapeId}")
    public ResponseEntity<Void> deleteCircuitEtapeV2(@PathVariable("id") String id,
                                                      @PathVariable String etapeId) {
        circuitService.deleteCircuitEtape(id, etapeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(BaConstants.URL.AUTORISATION_SORTIE)
    public ResponseEntity<BaAutorisationSortieDto> createDemandeSortie(@RequestBody BaAutorisationSortieRequestDto request) {
        return new ResponseEntity<>(autorisationSortieService.createDemande(request), HttpStatus.CREATED);
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE_MES_DEMANDES)
    public List<BaAutorisationSortieDto> getMesDemandesSortie() {
        return autorisationSortieService.getMesDemandes();
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE_A_VALIDER)
    public List<BaAutorisationSortieDto> getDemandesSortieAValider() {
        return autorisationSortieService.getDemandesAValider();
    }

    @PutMapping(BaConstants.URL.AUTORISATION_SORTIE + "/{id}/valider")
    public ResponseEntity<BaAutorisationSortieDto> validerDemandeSortie(@PathVariable String id,
                                                                         @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(autorisationSortieService.validerDemande(id, commentaire));
    }

    @PutMapping(BaConstants.URL.AUTORISATION_SORTIE + "/{id}/rejeter")
    public ResponseEntity<BaAutorisationSortieDto> rejeterDemandeSortie(@PathVariable String id,
                                                                         @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(autorisationSortieService.rejeterDemande(id, commentaire));
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE + "/statistiques")
    public BaRhDemandesStatsDto getStatistiquesAutorisations(@ModelAttribute BaRhDemandesFilterDto filter) {
        return autorisationSortieService.getStatistiques(filter);
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE + "/suivi")
    public org.springframework.data.domain.Page<BaAutorisationSortieSuiviDto> getSuiviAutorisations(
            @ModelAttribute BaRhDemandesFilterDto filter,
            org.springframework.data.domain.Pageable pageable) {
        return autorisationSortieService.getSuivi(filter, pageable);
    }

    @PostMapping(BaConstants.URL.REPRISE_SERVICE)
    public ResponseEntity<BaRepriseServiceDto> createDemandeReprise(@RequestBody BaRepriseServiceRequestDto request) {
        return new ResponseEntity<>(repriseServiceManager.createDemande(request), HttpStatus.CREATED);
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE_MES_DEMANDES)
    public List<BaRepriseServiceDto> getMesDemandesReprise() {
        return repriseServiceManager.getMesDemandes();
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE_A_VALIDER)
    public List<BaRepriseServiceDto> getDemandesRepriseAValider() {
        return repriseServiceManager.getDemandesAValider();
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE + "/{id}")
    public ResponseEntity<BaRepriseServiceDto> getDemandeReprise(@PathVariable String id) {
        return ResponseEntity.ok(repriseServiceManager.getById(id));
    }

    @PutMapping(BaConstants.URL.REPRISE_SERVICE + "/{id}/valider")
    public ResponseEntity<BaRepriseServiceDto> validerDemandeReprise(
            @PathVariable String id,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(repriseServiceManager.valider(id, commentaire));
    }

    @PutMapping(BaConstants.URL.REPRISE_SERVICE + "/{id}/rejeter")
    public ResponseEntity<BaRepriseServiceDto> rejeterDemandeReprise(
            @PathVariable String id,
            @RequestParam("commentaire") String commentaire) {
        return ResponseEntity.ok(repriseServiceManager.rejeter(id, commentaire));
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE + "/statistiques")
    public BaRhDemandesStatsDto getStatistiquesReprises(@ModelAttribute BaRhDemandesFilterDto filter) {
        return repriseServiceManager.getStatistiques(filter);
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE + "/suivi")
    public org.springframework.data.domain.Page<BaRepriseServiceSuiviDto> getSuiviReprises(
            @ModelAttribute BaRhDemandesFilterDto filter,
            org.springframework.data.domain.Pageable pageable) {
        return repriseServiceManager.getSuivi(filter, pageable);
    }
}

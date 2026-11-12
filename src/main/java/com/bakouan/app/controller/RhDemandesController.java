package com.bakouan.app.controller;

import com.bakouan.app.dto.BaAutorisationSortieDto;
import com.bakouan.app.dto.BaAutorisationSortieRequestDto;
import com.bakouan.app.dto.BaAutorisationSortieSuiviDto;
import com.bakouan.app.dto.BaRepriseServiceDto;
import com.bakouan.app.dto.BaRepriseServiceRequestDto;
import com.bakouan.app.dto.BaRepriseServiceSuiviDto;
import com.bakouan.app.dto.BaRhDemandesFilterDto;
import com.bakouan.app.dto.BaRhDemandesStatsDto;
import com.bakouan.app.service.BaAutorisationSortieService;
import com.bakouan.app.service.BaRepriseServiceManager;
import com.bakouan.app.utils.BaConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(BaConstants.URL.BASE_URL)
@Tag(name = "Demandes RH", description = "Autorisations de sortie et reprises de service")
public class RhDemandesController {

    private final BaAutorisationSortieService autorisationSortieService;
    private final BaRepriseServiceManager repriseServiceManager;
    private final com.bakouan.app.service.BaAutorisationAbsenceService absenceService;
    private final com.bakouan.app.service.RhCircuitService circuits;
    private final com.bakouan.app.security.BaUserService users;
    private final com.bakouan.app.repositories.BaUserRepository userRepository;

    @GetMapping("/rh/circuit")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.bakouan.app.dto.RhCircuitDto circuitApplicable(@RequestParam(defaultValue="SORTIE") String demarche) {
        if(!java.util.Set.of("SORTIE","ABSENCE").contains(demarche))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"D?marche RH inconnue.");
        return circuits.previewFor(demarche,userRepository.findById(users.getUserInfoWithMoreDetails().getId()).orElseThrow());
    }

    @PostMapping(BaConstants.URL.AUTORISATION_ABSENCE)
    public ResponseEntity<com.bakouan.app.dto.BaAutorisationAbsenceDto> createAbsence(@RequestBody @Valid com.bakouan.app.dto.BaAutorisationAbsenceRequestDto request) {
        return new ResponseEntity<>(absenceService.create(request), HttpStatus.CREATED);
    }

    @GetMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/mes-demandes")
    public List<com.bakouan.app.dto.BaAutorisationAbsenceDto> mesAbsences() { return absenceService.mesDemandes(); }

    @GetMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/a-valider")
    public List<com.bakouan.app.dto.BaAutorisationAbsenceDto> absencesAValider() { return absenceService.aValider(); }

    @GetMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/suivi")
    public List<com.bakouan.app.dto.BaAutorisationAbsenceDto> suiviAbsences(@RequestParam(required = false) LocalDate debut, @RequestParam(required = false) LocalDate fin) { return absenceService.suivi(debut, fin); }

    @GetMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/statistiques")
    public com.bakouan.app.dto.BaAbsenceStatsDto statsAbsences(@RequestParam(required = false) LocalDate debut, @RequestParam(required = false) LocalDate fin) { return absenceService.statistiques(debut, fin); }

    @PutMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/{id}/valider")
    public com.bakouan.app.dto.BaAutorisationAbsenceDto validerAbsence(@PathVariable String id, @RequestParam int etape, @RequestParam(required = false) String commentaire) { return absenceService.valider(id, etape, commentaire); }

    @PutMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/{id}/rejeter")
    public com.bakouan.app.dto.BaAutorisationAbsenceDto rejeterAbsence(@PathVariable String id, @RequestParam int etape, @RequestParam String commentaire) { return absenceService.rejeter(id, etape, commentaire); }

    @PutMapping(BaConstants.URL.AUTORISATION_ABSENCE + "/{id}/annuler")
    public com.bakouan.app.dto.BaAutorisationAbsenceDto annulerAbsence(@PathVariable String id) { return absenceService.annuler(id); }

    @PostMapping(BaConstants.URL.AUTORISATION_SORTIE)
    public ResponseEntity<BaAutorisationSortieDto> createAutorisation(
            @RequestBody @Valid final BaAutorisationSortieRequestDto request) {
        return new ResponseEntity<>(autorisationSortieService.createDemande(request), HttpStatus.CREATED);
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE_MES_DEMANDES)
    public List<BaAutorisationSortieDto> getMesAutorisations() {
        return autorisationSortieService.getMesDemandes();
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE_A_VALIDER)
    public List<BaAutorisationSortieDto> getAutorisationsAValider() {
        return autorisationSortieService.getDemandesAValider();
    }

    @PutMapping(BaConstants.URL.AUTORISATION_SORTIE + "/{id}/valider")
    public BaAutorisationSortieDto validerAutorisation(
            @PathVariable final String id,
            @RequestParam final int etape,
            @RequestParam(value = "commentaire", required = false) final String commentaire) {
        return autorisationSortieService.validerDemande(id, etape, commentaire);
    }

    @PutMapping(BaConstants.URL.AUTORISATION_SORTIE + "/{id}/rejeter")
    public BaAutorisationSortieDto rejeterAutorisation(
            @PathVariable final String id,
            @RequestParam final int etape,
            @RequestParam final String commentaire) {
        return autorisationSortieService.rejeterDemande(id, etape, commentaire);
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE + "/statistiques")
    public BaRhDemandesStatsDto getStatistiquesAutorisations(@ModelAttribute final BaRhDemandesFilterDto filter) {
        return autorisationSortieService.getStatistiques(filter);
    }

    @GetMapping(BaConstants.URL.AUTORISATION_SORTIE + "/suivi")
    public Page<BaAutorisationSortieSuiviDto> getSuiviAutorisations(
            @ModelAttribute final BaRhDemandesFilterDto filter, final Pageable pageable) {
        return autorisationSortieService.getSuivi(filter, pageable);
    }

    @PostMapping(BaConstants.URL.REPRISE_SERVICE)
    public ResponseEntity<BaRepriseServiceDto> createReprise(
            @RequestBody @Valid final BaRepriseServiceRequestDto request) {
        return new ResponseEntity<>(repriseServiceManager.createDemande(request), HttpStatus.CREATED);
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE_MES_DEMANDES)
    public List<BaRepriseServiceDto> getMesReprises() {
        return repriseServiceManager.getMesDemandes();
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE_A_VALIDER)
    public List<BaRepriseServiceDto> getReprisesAValider() {
        return repriseServiceManager.getDemandesAValider();
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE + "/{id}")
    public BaRepriseServiceDto getReprise(@PathVariable final String id) {
        return repriseServiceManager.getById(id);
    }

    @PutMapping(BaConstants.URL.REPRISE_SERVICE + "/{id}/valider")
    public BaRepriseServiceDto validerReprise(
            @PathVariable final String id,
            @RequestParam(value = "commentaire", required = false) final String commentaire) {
        return repriseServiceManager.valider(id, commentaire);
    }

    @PutMapping(BaConstants.URL.REPRISE_SERVICE + "/{id}/rejeter")
    public BaRepriseServiceDto rejeterReprise(
            @PathVariable final String id,
            @RequestParam final String commentaire) {
        return repriseServiceManager.rejeter(id, commentaire);
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE + "/statistiques")
    public BaRhDemandesStatsDto getStatistiquesReprises(@ModelAttribute final BaRhDemandesFilterDto filter) {
        return repriseServiceManager.getStatistiques(filter);
    }

    @GetMapping(BaConstants.URL.REPRISE_SERVICE + "/suivi")
    public Page<BaRepriseServiceSuiviDto> getSuiviReprises(
            @ModelAttribute final BaRhDemandesFilterDto filter, final Pageable pageable) {
        return repriseServiceManager.getSuivi(filter, pageable);
    }
}

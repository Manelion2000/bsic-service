package com.bakouan.app.service.impl;

import com.bakouan.app.dto.*;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.enums.ECircuitStatut;
import com.bakouan.app.enums.EDgaPole;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.enums.EHabilitationCompteStatut;
import com.bakouan.app.enums.EHabilitationFicheStatut;
import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.enums.EReinitialisationCompteStatut;
import com.bakouan.app.enums.EReinitialisationTraitementStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.enums.ETypePlateforme;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.security.BaRolesConstants;
import com.bakouan.app.service.BaBusinessService;
import com.bakouan.app.service.BaLogService;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.utils.BaConstants;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class BaBusinessServiceImpl implements BaBusinessService {

    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);
    private final BaLogService logService;
    private final BaUserService userService;

    private final BaServiceRepository serviceRepository;
    private final BaDepartementRepository departementRepository;
    private final BaAgenceRepository agenceRepository;
    private final BaUserRepository userRepository;


    private final BaPlateformeRepository plateformeRepository;
    private final BaCircuitRepository circuitRepository;
    private final BaCircuitEtapeRepository circuitEtapeRepository;
    private final BaFicheHabilitationRepository ficheHabilitationRepository;
    private final BaFicheHabilitationEtapeRepository ficheHabilitationEtapeRepository;
    private final BaFicheHabilitationPlateformeRepository ficheHabilitationPlateformeRepository;
    private final BaHabilitationComptePlateformeRepository habilitationComptePlateformeRepository;
    private final BaReinitialisationCompteRepository reinitialisationCompteRepository;
    private final BaReinitialisationCompteEtapeRepository reinitialisationCompteEtapeRepository;
    private final BaReinitialisationCompteTraitementRepository reinitialisationCompteTraitementRepository;
    private final BaValidationDelegationRepository validationDelegationRepository;
    private final BaDgaPoleValidateurRepository dgaPoleValidateurRepository;

    private final BaReunionRepository reunionRepository;
    private final BaReunionParticipantRepository reunionParticipantRepository;
    private final BaReunionActionRepository reunionActionRepository;


    /**
     * Retouner toutes les services
     * @return: une liste de service
     */
    @Override
    public List<BaServiceDto> getAllServices() {
        return serviceRepository.findByStatut(EStatut.A).stream().map(mapper::maps).collect(Collectors.toList());
    }

    @Override
    public BaServiceDto createService(final BaServiceDto dto) {
        logService.log(new BaLogDto(EAction.C, "Service " + dto.getCode()));
        if (serviceRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja un service avec ce code");
        }
        BaService entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        if (!BaUtils.isEmpty(dto.getIdDepartement())) {
            entity.setDepartement(departementRepository.findById(dto.getIdDepartement())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        }
        return mapper.maps(serviceRepository.save(entity));
    }

    @Override
    public BaServiceDto updateService(final String id, final BaServiceDto dto) {
        logService.log(new BaLogDto(EAction.U, "Service " + id));
        BaService entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable"));
        if (!BaUtils.isEmpty(dto.getCode()) && !dto.getCode().equalsIgnoreCase(entity.getCode())
                && serviceRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja un service avec ce code");
        }
        entity.setCode(dto.getCode());
        entity.setNom(dto.getNom());
        if (!BaUtils.isEmpty(dto.getIdDepartement())) {
            entity.setDepartement(departementRepository.findById(dto.getIdDepartement())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        } else {
            entity.setDepartement(null);
        }
        return mapper.maps(serviceRepository.save(entity));
    }

    @Override
    public List<BaDepartementDto> getAllDepartements() {
        return departementRepository.findByStatut(EStatut.A).stream()
                .map(this::toDepartementDto)
                .collect(Collectors.toList());
    }

    /**
     * FOnction de crÃ©ation de departement
     * @param dto donnees du departement
     * @return: un dto aprÃ¨s crÃ©ation du dÃ©partement
     */

    @Override
    public BaDepartementDto createDepartement(final BaDepartementDto dto) {
        logService.log(new BaLogDto(EAction.C, "Departement " + dto.getCode()));
        if (departementRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja un departement avec ce code");
        }
        BaDepartement entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        entity.setParentDepartement(resolveParentDepartement(dto.getIdParentDepartement(), null));
        entity.setDgaValidateur(resolveDgaValidateur(dto.getIdDgaValidateur()));
        return toDepartementDto(departementRepository.save(entity));
    }

    @Override
    public BaDepartementDto updateDepartement(final String id, final BaDepartementDto dto) {
        logService.log(new BaLogDto(EAction.U, "Departement " + id));
        BaDepartement entity = departementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable"));
        if (!BaUtils.isEmpty(dto.getCode()) && !dto.getCode().equalsIgnoreCase(entity.getCode())
                && departementRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja un departement avec ce code");
        }
        entity.setCode(dto.getCode());
        entity.setNom(dto.getNom());
        entity.setDgaPole(dto.getDgaPole());
        entity.setParentDepartement(resolveParentDepartement(dto.getIdParentDepartement(), id));
        entity.setDgaValidateur(resolveDgaValidateur(dto.getIdDgaValidateur()));
        return toDepartementDto(departementRepository.save(entity));
    }

    @Override
    public List<BaAgenceDto> getAllAgences() {
        return agenceRepository.findByStatut(EStatut.A).stream().map(mapper::maps).collect(Collectors.toList());
    }

    @Override
    public BaAgenceDto createAgence(final BaAgenceDto dto) {
        logService.log(new BaLogDto(EAction.C, "Agence " + dto.getCode()));
        if (agenceRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja une agence avec ce code");
        }
        if (BaUtils.isEmpty(dto.getIdDepartement())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement obligatoire pour une agence");
        }
        BaAgence entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        entity.setDepartement(departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        return mapper.maps(agenceRepository.save(entity));
    }

    @Override
    public BaAgenceDto updateAgence(final String id, final BaAgenceDto dto){
        logService.log(new BaLogDto(EAction.U, "Agence " + id));
        BaAgence entity=agenceRepository.findById(id).orElseThrow(
                ()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"agence introuvable"));
        if (!BaUtils.isEmpty(dto.getCode()) && !dto.getCode().equalsIgnoreCase(entity.getCode())
                && agenceRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja une agence avec ce code");
        }
        entity.setAdresse(dto.getAdresse());
        entity.setCode(dto.getCode());
        entity.setNom(dto.getNom());
        entity.setTelephone(dto.getTelephone());
        entity.setTelephone1(dto.getTelephone1());
        if (BaUtils.isEmpty(dto.getIdDepartement())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement obligatoire pour une agence");
        }
        entity.setDepartement(departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));

        return mapper.maps(agenceRepository.save(entity));
    }


    @Override
    public List<BaAnnuaireDepartementDto> getAnnuaire(final String departementId,
                                                      final String serviceId,
                                                      final String agenceId) {
        List<BaUser> employes = userRepository.findByStatut(EStatut.A);
        List<BaUser> filtered = employes.stream()
                .filter(employe -> BaUtils.isEmpty(departementId) ||
                        (employe.getDepartement() != null && departementId.equals(employe.getDepartement().getId())))
                .filter(employe -> BaUtils.isEmpty(serviceId) ||
                        (employe.getService() != null && serviceId.equals(employe.getService().getId())))
                .filter(employe -> BaUtils.isEmpty(agenceId) ||
                        (employe.getAgence() != null && agenceId.equals(employe.getAgence().getId())))
                .collect(Collectors.toList());

        List<BaUser> sorted = filtered.stream()
                .sorted((a, b) -> {
                    String depA = a.getDepartement() == null ? "ZZZ" : a.getDepartement().getNom();
                    String depB = b.getDepartement() == null ? "ZZZ" : b.getDepartement().getNom();
                    int cmp = depA.compareToIgnoreCase(depB);
                    if (cmp != 0) {
                        return cmp;
                    }
                    String nomA = a.getNom() == null ? "" : a.getNom();
                    String nomB = b.getNom() == null ? "" : b.getNom();
                    cmp = nomA.compareToIgnoreCase(nomB);
                    if (cmp != 0) {
                        return cmp;
                    }
                    String prenomA = a.getPrenom() == null ? "" : a.getPrenom();
                    String prenomB = b.getPrenom() == null ? "" : b.getPrenom();
                    return prenomA.compareToIgnoreCase(prenomB);
                })
                .collect(Collectors.toList());

        List<BaAnnuaireDepartementDto> result = new ArrayList<>();
        java.util.Map<String, BaAnnuaireDepartementDto> departements = new java.util.LinkedHashMap<>();

        for (BaUser employe : sorted) {
            String depId = employe.getDepartement() == null ? null : employe.getDepartement().getId();
            String depNom = employe.getDepartement() == null ? "Non renseigne" : employe.getDepartement().getNom();
            String depKey = depId == null ? "__NONE__" : depId;

            BaAnnuaireDepartementDto departementDto = departements.get(depKey);
            if (departementDto == null) {
                departementDto = new BaAnnuaireDepartementDto();
                departementDto.setId(depId);
                departementDto.setNom(depNom);
                departementDto.setAgents(new ArrayList<>());
                departements.put(depKey, departementDto);
            }

            BaAnnuaireAgentDto agentDto = new BaAnnuaireAgentDto();
            agentDto.setNom(employe.getNom());
            agentDto.setPrenom(employe.getPrenom());
            agentDto.setFonction(employe.getFonction() == null ? null : employe.getFonction().name());
            agentDto.setEmail(BaUtils.isEmpty(employe.getEmailPro()) ? employe.getEmail() : employe.getEmailPro());
            agentDto.setNumeroPoste(employe.getTelephoneFixe());
            agentDto.setTelephoneSecondaire(employe.getTelephoneMobile());
            agentDto.setIdService(employe.getService() == null ? null : employe.getService().getId());
            agentDto.setNomService(employe.getService() == null ? null : employe.getService().getNom());
            agentDto.setIdAgence(employe.getAgence() == null ? null : employe.getAgence().getId());
            agentDto.setNomAgence(employe.getAgence() == null ? null : employe.getAgence().getNom());
            departementDto.getAgents().add(agentDto);
        }

        result.addAll(departements.values());
        return result;
    }

    @Override
    public List<BaPublicAnnuaireDepartementDto> getPublicAnnuaire(final String departementId,
                                                                  final String serviceId,
                                                                  final String agenceId) {
        List<BaUser> employes = userRepository.findByStatut(EStatut.A);
        List<BaUser> filtered = employes.stream()
                .filter(employe -> BaUtils.isEmpty(departementId) ||
                        (employe.getDepartement() != null && departementId.equals(employe.getDepartement().getId())))
                .filter(employe -> BaUtils.isEmpty(serviceId) ||
                        (employe.getService() != null && serviceId.equals(employe.getService().getId())))
                .filter(employe -> BaUtils.isEmpty(agenceId) ||
                        (employe.getAgence() != null && agenceId.equals(employe.getAgence().getId())))
                .sorted((a, b) -> {
                    String depA = a.getDepartement() == null ? "ZZZ" : a.getDepartement().getNom();
                    String depB = b.getDepartement() == null ? "ZZZ" : b.getDepartement().getNom();
                    int cmp = depA.compareToIgnoreCase(depB);
                    if (cmp != 0) {
                        return cmp;
                    }
                    String nomA = a.getNom() == null ? "" : a.getNom();
                    String nomB = b.getNom() == null ? "" : b.getNom();
                    cmp = nomA.compareToIgnoreCase(nomB);
                    if (cmp != 0) {
                        return cmp;
                    }
                    String prenomA = a.getPrenom() == null ? "" : a.getPrenom();
                    String prenomB = b.getPrenom() == null ? "" : b.getPrenom();
                    return prenomA.compareToIgnoreCase(prenomB);
                })
                .collect(Collectors.toList());

        List<BaPublicAnnuaireDepartementDto> result = new ArrayList<>();
        Map<String, BaPublicAnnuaireDepartementDto> departements = new LinkedHashMap<>();

        for (BaUser employe : filtered) {
            String depId = employe.getDepartement() == null ? null : employe.getDepartement().getId();
            String depNom = employe.getDepartement() == null ? "Non renseigne" : employe.getDepartement().getNom();
            String depKey = depId == null ? "__NONE__" : depId;

            BaPublicAnnuaireDepartementDto departementDto = departements.get(depKey);
            if (departementDto == null) {
                departementDto = new BaPublicAnnuaireDepartementDto();
                departementDto.setId(depId);
                departementDto.setNom(depNom);
                departementDto.setAgents(new ArrayList<>());
                departements.put(depKey, departementDto);
            }

            BaPublicAnnuaireAgentDto agentDto = new BaPublicAnnuaireAgentDto();
            agentDto.setNom(employe.getNom());
            agentDto.setPrenom(employe.getPrenom());
            agentDto.setFonction(employe.getFonction() == null ? null : employe.getFonction().name());
            agentDto.setEmail(resolvePublicBsicEmail(employe));
            agentDto.setNumeroPoste(employe.getTelephoneFixe());
            agentDto.setIdService(employe.getService() == null ? null : employe.getService().getId());
            agentDto.setNomService(employe.getService() == null ? null : employe.getService().getNom());
            agentDto.setIdAgence(employe.getAgence() == null ? null : employe.getAgence().getId());
            agentDto.setNomAgence(employe.getAgence() == null ? null : employe.getAgence().getNom());
            departementDto.getAgents().add(agentDto);
        }

        result.addAll(departements.values());
        return result;
    }

    private String resolvePublicBsicEmail(final BaUser employe) {
        if (isBsicEmail(employe.getEmailPro())) {
            return employe.getEmailPro();
        }
        if (isBsicEmail(employe.getEmail())) {
            return employe.getEmail();
        }
        return null;
    }

    private boolean isBsicEmail(final String email) {
        return email != null && email.trim().toLowerCase().endsWith("@bsic.bf");
    }

    @Override
    public List<BaPlateformeDto> getAllPlateformes() {
        return plateformeRepository.findByStatut(EStatut.A).stream().map(mapper::maps).collect(Collectors.toList());
    }

    @Override
    public BaPlateformeDto createPlateforme(final BaPlateformeDto dto) {
        logService.log(new BaLogDto(EAction.C, "Plateforme " + dto.getCode()));
        if (plateformeRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja une plateforme avec ce code");
        }
        BaPlateforme entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        if (!BaUtils.isEmpty(dto.getIdCircuit())) {
            entity.setCircuit(circuitRepository.findById(dto.getIdCircuit())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit introuvable")));
        }
        return mapper.maps(plateformeRepository.save(entity));
    }

    @Override
    public BaPlateformeDto updatePlateforme(final String id, final BaPlateformeDto dto) {
        logService.log(new BaLogDto(EAction.U, "Plateforme " + id));
        BaPlateforme entity = plateformeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plateforme introuvable"));
        if (!BaUtils.isEmpty(dto.getCode()) && !dto.getCode().equalsIgnoreCase(entity.getCode())
                && plateformeRepository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il existe deja une plateforme avec ce code");
        }
        entity.setCode(dto.getCode());
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        entity.setTypePlateforme(dto.getTypePlateforme() == null ? ETypePlateforme.RESEAUX : dto.getTypePlateforme());
        if (!BaUtils.isEmpty(dto.getIdCircuit())) {
            entity.setCircuit(circuitRepository.findById(dto.getIdCircuit())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit introuvable")));
        } else {
            entity.setCircuit(null);
        }
        return mapper.maps(plateformeRepository.save(entity));
    }

    public List<BaFicheHabilitationDto> getAllFichesHabilitation() {
        return ficheHabilitationRepository.findByStatut(EStatut.A).stream()
                .map(this::toFicheDtoWithSignataires)
                .collect(Collectors.toList());
    }

    @Override
    public BaFicheHabilitationDto getFicheHabilitationById(final String id) {
        BaFicheHabilitation fiche = ficheHabilitationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiche introuvable"));
        return toFicheDtoWithSignataires(fiche);
    }

    @Override
    public BaFicheHabilitationDto createFicheHabilitation(final BaFicheHabilitationDto dto) {
        logService.log(new BaLogDto(EAction.C, "Fiche habilitation"));
        if (dto == null || BaUtils.isEmpty(dto.getIdEmploye())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employe manquant");
        }
        List<BaPlateforme> plateformes = resolvePlateformesForFiche(dto);
        BaCircuit circuit = resolveCircuitForPlateformes(plateformes);
        BaFicheHabilitation entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        BaUser employe = userRepository.findById(dto.getIdEmploye())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employe introuvable"));
        entity.setEmploye(employe);
        entity.setCircuit(circuit);
        if (entity.getFonctionEmploye() == null) {
            entity.setFonctionEmploye(employe.getFonction());
        }
        if (BaUtils.isEmpty(entity.getTelephoneMobileEmploye())) {
            entity.setTelephoneMobileEmploye(employe.getTelephoneMobile());
        }
        entity.setStatutHabilitation(EHabilitationFicheStatut.EN_COURS);
        BaFicheHabilitation saved = ficheHabilitationRepository.save(entity);
        saveFichePlateformes(saved, plateformes);

        initFicheEtapesFromCircuit(saved.getId());

        return toFicheDtoWithSignataires(saved);
    }

    @Override
    public void deleteFicheHabilitation(final String id) {
        logService.log(new BaLogDto(EAction.D, "Fiche habilitation " + id));
        BaFicheHabilitation fiche = ficheHabilitationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiche introuvable"));

        if (fiche.getStatut() == EStatut.D) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiche deja supprimee");
        }
        if (fiche.getStatutHabilitation() != EHabilitationFicheStatut.EN_COURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les fiches EN_COURS peuvent etre supprimees");
        }

        List<BaFicheHabilitationEtape> etapes = ficheHabilitationEtapeRepository.findByFicheId(id);
        etapes.forEach(etape -> etape.setStatut(EStatut.D));
        if (!etapes.isEmpty()) {
            ficheHabilitationEtapeRepository.saveAll(etapes);
        }

        fiche.setStatut(EStatut.D);
        ficheHabilitationRepository.save(fiche);
    }

    @Override
    public org.springframework.data.domain.Page<BaFicheHabilitationDto> getMyFicheDemandes(
            final EHabilitationFicheStatut statut,
            final org.springframework.data.domain.Pageable pageable) {
        BaUserDto currentUser = userService.getUserInfoWithMoreDetails();
        BaUser employe = resolveUserForCurrent(currentUser);
        org.springframework.data.domain.Page<BaFicheHabilitation> page = statut == null
                ? ficheHabilitationRepository.findByEmployeId(employe.getId(), pageable)
                : ficheHabilitationRepository.findByEmployeIdAndStatutHabilitation(employe.getId(), statut, pageable);
        return page.map(this::toFicheDtoWithSignataires);
    }

    @Override
    public org.springframework.data.domain.Page<BaFicheHabilitationDto> getMyFicheValidations(
            final EHabilitationFicheStatut statut,
            final org.springframework.data.domain.Pageable pageable) {
        BaUserDto currentUser = userService.getUserInfoWithMoreDetails();
        BaUser employe = resolveUserForCurrent(currentUser);
        List<BaFicheHabilitation> fiches = ficheHabilitationRepository.findByStatut(EStatut.A);
        List<BaFicheHabilitationDto> result = new ArrayList<>();
        for (BaFicheHabilitation fiche : fiches) {
            if (statut != null && fiche.getStatutHabilitation() != statut) {
                continue;
            }
            List<BaFicheHabilitationEtape> etapes = ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre(fiche.getId());
            if (etapes.isEmpty() && fiche.getCircuit() != null) {
                try {
                    initFicheEtapesFromCircuit(fiche.getId());
                    etapes = ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre(fiche.getId());
                } catch (ResponseStatusException ex) {
                    log.warn("Impossible d'initialiser les etapes de la fiche {}: {}", fiche.getId(), ex.getReason());
                }
            }
            BaFicheHabilitationEtape current = etapes.stream()
                    .filter(e -> e.getStatutValidation() == EHabilitationStatut.EN_ATTENTE)
                    .findFirst()
                    .orElse(null);
            if (current == null) {
                continue;
            }
            if (isUserAllowedForValidation(currentUser, employe, fiche, current)) {
                result.add(toFicheDtoWithSignataires(fiche));
            }
        }
        List<BaFicheHabilitationDto> sorted = sortFiches(result, pageable.getSort());
        return toPage(sorted, pageable);
    }

    @Override
    public List<BaFicheHabilitationEtapeDto> initFicheEtapesFromCircuit(final String ficheId) {
        if (BaUtils.isEmpty(ficheId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiche introuvable");
        }
        BaFicheHabilitation fiche = ficheHabilitationRepository.findById(ficheId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiche introuvable"));
        if (fiche.getCircuit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit introuvable");
        }
        List<BaCircuitEtape> configEtapes = circuitEtapeRepository
                .findByCircuitIdOrderByOrdre(fiche.getCircuit().getId());
        if (configEtapes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune etape configuree pour ce circuit.");
        }

        List<BaFicheHabilitationEtape> existing = ficheHabilitationEtapeRepository.findByFicheId(ficheId);

        for (BaCircuitEtape config : configEtapes) {
            boolean ordreAlreadyExists = existing.stream()
                    .anyMatch(e -> e.getOrdre() != null && e.getOrdre().equals(config.getOrdre()));
            if (ordreAlreadyExists) {
                continue;
            }
            BaFicheHabilitationEtape etape = new BaFicheHabilitationEtape();
            etape.setId(BaUtils.randomUUID());
            etape.setFiche(fiche);
            etape.setOrdre(config.getOrdre());
            etape.setStatutValidation(EHabilitationStatut.EN_ATTENTE);
            etape.setValidateur(resolveSignerForCircuitEtape(fiche, config));
            ficheHabilitationEtapeRepository.save(etape);
        }

        return ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre(ficheId)
                .stream()
                .map(etape -> toFicheEtapeDto(fiche, etape))
                .collect(Collectors.toList());
    }

    @Override
    public BaFicheHabilitationEtapeDto updateEtapeStatus(final String etapeId, final EHabilitationStatut statut, final String commentaire) {
        logService.log(new BaLogDto(EAction.U, "MAJ etape habilitation " + etapeId));
        BaFicheHabilitationEtape entity = ficheHabilitationEtapeRepository.findById(etapeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etape introuvable"));
        BaFicheHabilitation fiche = entity.getFiche();
        if (fiche.getStatutHabilitation() == EHabilitationFicheStatut.REJETEE
                || fiche.getStatutHabilitation() == EHabilitationFicheStatut.VALIDEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fiche est deja terminee");
        }
        List<BaFicheHabilitationEtape> etapes = ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre(fiche.getId());
        BaFicheHabilitationEtape current = etapes.stream()
                .filter(e -> e.getStatutValidation() == EHabilitationStatut.EN_ATTENTE)
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune etape en attente");
        }
        if (!current.getId().equals(entity.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette etape n'est pas l'etape courante");
        }
        BaUserDto currentUser = userService.getUserInfoWithMoreDetails();
        BaUser currentEmploye = resolveUserForCurrent(currentUser);
        if (!isUserAllowedForValidation(currentUser, currentEmploye, fiche, entity)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'etes pas autorise a valider cette etape");
        }
        if (statut == EHabilitationStatut.REJETE && BaUtils.isEmpty(commentaire)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de rejet est obligatoire");
        }
        if (statut == EHabilitationStatut.VALIDE) {
            entity.setCommentaire("Avis favorable");
        } else {
            entity.setCommentaire(commentaire);
        }
        entity.setValidateur(currentEmploye);
        entity.setDateValidation(java.time.ZonedDateTime.now());
        entity.setStatutValidation(statut);
        BaFicheHabilitationEtape saved = ficheHabilitationEtapeRepository.save(entity);
        if (statut == EHabilitationStatut.REJETE) {
            fiche.setStatutHabilitation(EHabilitationFicheStatut.REJETEE);
            fiche.setMotif(entity.getCommentaire());
            ficheHabilitationRepository.save(fiche);
        } else if (statut == EHabilitationStatut.VALIDE) {
            boolean allValidated = etapes.stream()
                    .allMatch(e -> e.getId().equals(entity.getId())
                            ? statut == EHabilitationStatut.VALIDE
                            : e.getStatutValidation() == EHabilitationStatut.VALIDE);
            if (allValidated) {
                fiche.setStatutHabilitation(EHabilitationFicheStatut.VALIDEE);
                ficheHabilitationRepository.save(fiche);
                ensureComptePlateformeForFiche(fiche);
            }
        }
        return mapper.maps(saved);
    }

    @Override
    public List<BaHabilitationComptePlateformeDto> getComptesPlateforme(
            final EHabilitationCompteStatut statutCreation) {
        List<BaHabilitationComptePlateforme> comptes = statutCreation == null
                ? habilitationComptePlateformeRepository.findByStatutOrderByCreatedDateDesc(EStatut.A)
                : habilitationComptePlateformeRepository.findByStatutAndStatutCreationOrderByCreatedDateDesc(
                        EStatut.A, statutCreation);
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        return comptes.stream()
                .filter(compte -> canCreateCompteForPlateforme(current, compte.getPlateforme()))
                .map(this::toComptePlateformeDto)
                .collect(Collectors.toList());
    }

    @Override
    public BaHabilitationComptePlateformeDto demarrerCreationCompte(final String id, final String commentaire) {
        BaHabilitationComptePlateforme compte = findComptePlateforme(id);
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        assertCanCreateCompteForPlateforme(current, compte.getPlateforme());
        if (compte.getStatutCreation() == EHabilitationCompteStatut.CREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte est deja cree.");
        }
        compte.setStatutCreation(EHabilitationCompteStatut.EN_COURS_CREATION);
        compte.setDateDebutCreation(java.time.ZonedDateTime.now());
        compte.setCreePar(current);
        compte.setCommentaireCreation(BaUtils.isEmpty(commentaire) ? compte.getCommentaireCreation() : commentaire.trim());
        return toComptePlateformeDto(habilitationComptePlateformeRepository.save(compte));
    }

    @Override
    public BaHabilitationComptePlateformeDto marquerCompteCree(final String id, final String commentaire) {
        BaHabilitationComptePlateforme compte = findComptePlateforme(id);
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        assertCanCreateCompteForPlateforme(current, compte.getPlateforme());
        if (compte.getDateDebutCreation() == null) {
            compte.setDateDebutCreation(java.time.ZonedDateTime.now());
        }
        compte.setStatutCreation(EHabilitationCompteStatut.CREE);
        compte.setDateCreation(java.time.ZonedDateTime.now());
        compte.setCreePar(current);
        compte.setCommentaireCreation(BaUtils.isEmpty(commentaire) ? compte.getCommentaireCreation() : commentaire.trim());
        return toComptePlateformeDto(habilitationComptePlateformeRepository.save(compte));
    }

    @Override
    public BaReinitialisationCompteDto createReinitialisationCompte(final BaReinitialisationCompteRequestDto request) {
        if (request == null || BaUtils.isEmpty(request.getIdPlateforme()) || BaUtils.isEmpty(request.getMotif())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plateforme et motif obligatoires.");
        }
        BaUser demandeur = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        BaPlateforme plateforme = plateformeRepository.findById(request.getIdPlateforme())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plateforme introuvable."));
        if (plateforme.getCircuit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun circuit n'est lie a cette plateforme.");
        }
        boolean compteCree = habilitationComptePlateformeRepository
                .findByStatutAndStatutCreationOrderByCreatedDateDesc(EStatut.A, EHabilitationCompteStatut.CREE)
                .stream()
                .anyMatch(c -> c.getDemandeur() != null && c.getPlateforme() != null
                        && demandeur.getId().equals(c.getDemandeur().getId())
                        && plateforme.getId().equals(c.getPlateforme().getId()));
        if (!compteCree) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le compte doit deja etre cree avant une reinitialisation.");
        }
        BaReinitialisationCompte demande = new BaReinitialisationCompte();
        demande.setId(BaUtils.randomUUID());
        demande.setDemandeur(demandeur);
        demande.setPlateforme(plateforme);
        demande.setCircuit(plateforme.getCircuit());
        demande.setMotif(request.getMotif().trim());
        demande.setStatutDemande(EReinitialisationCompteStatut.EN_COURS);
        BaReinitialisationCompte saved = reinitialisationCompteRepository.save(demande);
        initReinitialisationEtapes(saved);
        return toReinitialisationDto(saved);
    }

    @Override
    public List<BaReinitialisationCompteDto> getMesReinitialisationsCompte() {
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        return reinitialisationCompteRepository.findByDemandeurIdAndStatutOrderByCreatedDateDesc(current.getId(), EStatut.A)
                .stream().map(this::toReinitialisationDto).collect(Collectors.toList());
    }

    @Override
    public List<BaReinitialisationCompteDto> getReinitialisationsCompteAValider() {
        BaUserDto currentDto = userService.getUserInfoWithMoreDetails();
        BaUser current = resolveUserForCurrent(currentDto);
        return reinitialisationCompteRepository.findByStatutAndStatutDemandeOrderByCreatedDateDesc(EStatut.A, EReinitialisationCompteStatut.EN_COURS)
                .stream()
                .filter(d -> {
                    BaReinitialisationCompteEtape etape = currentReinitialisationEtape(d);
                    return etape != null && isUserAllowedForReinitialisationValidation(currentDto, current, etape);
                })
                .map(this::toReinitialisationDto)
                .collect(Collectors.toList());
    }

    @Override
    public BaReinitialisationCompteDto updateReinitialisationEtapeStatus(final String etapeId, final EHabilitationStatut statut, final String commentaire) {
        BaReinitialisationCompteEtape etape = reinitialisationCompteEtapeRepository.findById(etapeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etape introuvable."));
        BaReinitialisationCompte demande = etape.getDemande();
        if (demande.getStatutDemande() != EReinitialisationCompteStatut.EN_COURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La demande est deja traitee.");
        }
        BaReinitialisationCompteEtape current = currentReinitialisationEtape(demande);
        if (current == null || !current.getId().equals(etape.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette etape n'est pas l'etape courante.");
        }
        BaUserDto currentDto = userService.getUserInfoWithMoreDetails();
        BaUser currentUser = resolveUserForCurrent(currentDto);
        if (!isUserAllowedForReinitialisationValidation(currentDto, currentUser, etape)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'etes pas autorise a valider cette etape.");
        }
        if (statut == EHabilitationStatut.REJETE && BaUtils.isEmpty(commentaire)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de rejet est obligatoire.");
        }
        etape.setValidateur(currentUser);
        etape.setStatutValidation(statut);
        etape.setCommentaire(statut == EHabilitationStatut.VALIDE ? "Avis favorable" : commentaire.trim());
        etape.setDateValidation(java.time.ZonedDateTime.now());
        reinitialisationCompteEtapeRepository.save(etape);
        if (statut == EHabilitationStatut.REJETE) {
            demande.setStatutDemande(EReinitialisationCompteStatut.REJETEE);
            demande.setDateValidation(java.time.ZonedDateTime.now());
            reinitialisationCompteRepository.save(demande);
        } else if (allReinitialisationEtapesValidated(demande)) {
            demande.setStatutDemande(EReinitialisationCompteStatut.VALIDEE);
            demande.setDateValidation(java.time.ZonedDateTime.now());
            reinitialisationCompteRepository.save(demande);
            ensureReinitialisationTraitement(demande);
        }
        return toReinitialisationDto(demande);
    }

    @Override
    public List<BaReinitialisationCompteDto> getReinitialisationsCompteATraiter(final EReinitialisationTraitementStatut statutTraitement) {
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        List<BaReinitialisationCompteTraitement> traitements = statutTraitement == null
                ? reinitialisationCompteTraitementRepository.findByStatutOrderByCreatedDateDesc(EStatut.A)
                : reinitialisationCompteTraitementRepository.findByStatutAndStatutTraitementOrderByCreatedDateDesc(EStatut.A, statutTraitement);
        return traitements.stream()
                .filter(t -> canResetCompteForPlateforme(current, t.getDemande().getPlateforme()))
                .map(t -> toReinitialisationDto(t.getDemande()))
                .collect(Collectors.toList());
    }

    @Override
    public BaReinitialisationCompteDto demarrerTraitementReinitialisation(final String traitementId, final String commentaire) {
        BaReinitialisationCompteTraitement traitement = findReinitialisationTraitement(traitementId);
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        assertCanResetCompteForPlateforme(current, traitement.getDemande().getPlateforme());
        traitement.setStatutTraitement(EReinitialisationTraitementStatut.EN_COURS_TRAITEMENT);
        traitement.setDateDebutTraitement(java.time.ZonedDateTime.now());
        traitement.setTraitePar(current);
        traitement.setCommentaireTraitement(BaUtils.isEmpty(commentaire) ? traitement.getCommentaireTraitement() : commentaire.trim());
        reinitialisationCompteTraitementRepository.save(traitement);
        return toReinitialisationDto(traitement.getDemande());
    }

    @Override
    public BaReinitialisationCompteDto marquerReinitialisationTraitee(final String traitementId, final String commentaire) {
        BaReinitialisationCompteTraitement traitement = findReinitialisationTraitement(traitementId);
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        assertCanResetCompteForPlateforme(current, traitement.getDemande().getPlateforme());
        if (traitement.getDateDebutTraitement() == null) {
            traitement.setDateDebutTraitement(java.time.ZonedDateTime.now());
        }
        traitement.setStatutTraitement(EReinitialisationTraitementStatut.TRAITEE);
        traitement.setDateTraitement(java.time.ZonedDateTime.now());
        traitement.setTraitePar(current);
        traitement.setCommentaireTraitement(BaUtils.isEmpty(commentaire) ? traitement.getCommentaireTraitement() : commentaire.trim());
        reinitialisationCompteTraitementRepository.save(traitement);
        return toReinitialisationDto(traitement.getDemande());
    }

    @Override
    public BaPlateformeDashboardStatsDto getPlateformeDashboardStats(final BaPlateformeStatFilterDto filter) {
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        List<BaHabilitationComptePlateforme> comptes = habilitationComptePlateformeRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A)
                .stream()
                .filter(compte -> canViewPlateformeStats(current, compte.getPlateforme()))
                .filter(compte -> matchesCompteFilter(compte, filter))
                .collect(Collectors.toList());
        List<BaReinitialisationCompteTraitement> traitements = reinitialisationCompteTraitementRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A);
        Map<String, BaReinitialisationCompteTraitement> traitementByDemandeId = traitements.stream()
                .filter(t -> t.getDemande() != null)
                .collect(Collectors.toMap(t -> t.getDemande().getId(), t -> t, (a, b) -> a));
        List<BaReinitialisationCompte> reinitialisations = reinitialisationCompteRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A)
                .stream()
                .filter(demande -> canViewPlateformeStats(current, demande.getPlateforme()))
                .filter(demande -> matchesReinitialisationFilter(demande, traitementByDemandeId.get(demande.getId()), filter))
                .collect(Collectors.toList());

        BaPlateformeDashboardStatsDto stats = new BaPlateformeDashboardStatsDto();
        stats.setTotalComptes(comptes.size());
        stats.setComptesACreer(countComptesByStatut(comptes, EHabilitationCompteStatut.A_CREER));
        stats.setComptesEnCoursCreation(countComptesByStatut(comptes, EHabilitationCompteStatut.EN_COURS_CREATION));
        stats.setComptesCrees(countComptesByStatut(comptes, EHabilitationCompteStatut.CREE));
        stats.setTotalReinitialisations(reinitialisations.size());
        stats.setReinitialisationsEnCours(countReinitialisationsByStatut(reinitialisations, EReinitialisationCompteStatut.EN_COURS));
        stats.setReinitialisationsValidees(countReinitialisationsByStatut(reinitialisations, EReinitialisationCompteStatut.VALIDEE));
        stats.setReinitialisationsRejetees(countReinitialisationsByStatut(reinitialisations, EReinitialisationCompteStatut.REJETEE));
        List<BaReinitialisationCompteTraitement> filteredTraitements = reinitialisations.stream()
                .map(demande -> traitementByDemandeId.get(demande.getId()))
                .filter(t -> t != null)
                .collect(Collectors.toList());
        stats.setTraitementsATraiter(countTraitementsByStatut(filteredTraitements, EReinitialisationTraitementStatut.A_TRAITER));
        stats.setTraitementsEnCours(countTraitementsByStatut(filteredTraitements, EReinitialisationTraitementStatut.EN_COURS_TRAITEMENT));
        stats.setTraitementsTraites(countTraitementsByStatut(filteredTraitements, EReinitialisationTraitementStatut.TRAITEE));
        stats.setParTypePlateforme(toStatItems(comptes, c -> c.getPlateforme() == null || c.getPlateforme().getTypePlateforme() == null
                ? "NON_RENSEIGNE" : c.getPlateforme().getTypePlateforme().name(), c -> c.getPlateforme() == null || c.getPlateforme().getTypePlateforme() == null
                ? "Non renseigne" : c.getPlateforme().getTypePlateforme().name(), 10));
        stats.setParDepartement(toStatItems(comptes, c -> c.getDemandeur() == null || c.getDemandeur().getDepartement() == null
                ? "NON_RENSEIGNE" : c.getDemandeur().getDepartement().getId(), c -> c.getDemandeur() == null || c.getDemandeur().getDepartement() == null
                ? "Non renseigne" : c.getDemandeur().getDepartement().getNom(), 10));
        stats.setTopPlateformes(toStatItems(comptes, c -> c.getPlateforme() == null ? "NON_RENSEIGNE" : c.getPlateforme().getId(),
                c -> c.getPlateforme() == null ? "Non renseigne" : c.getPlateforme().getNom(), 10));
        stats.setTopCreateurs(toStatItems(comptes.stream().filter(c -> c.getCreePar() != null).collect(Collectors.toList()),
                c -> c.getCreePar().getId(), c -> fullName(c.getCreePar()), 10));
        stats.setTopTraiteurs(toStatItems(filteredTraitements.stream().filter(t -> t.getTraitePar() != null).collect(Collectors.toList()),
                t -> t.getTraitePar().getId(), t -> fullName(t.getTraitePar()), 10));
        return stats;
    }

    @Override
    public org.springframework.data.domain.Page<BaPlateformeUsageRowDto> getSuiviPlateformes(
            final BaPlateformeStatFilterDto filter,
            final org.springframework.data.domain.Pageable pageable) {
        BaUser current = resolveUserForCurrent(userService.getUserInfoWithMoreDetails());
        List<BaReinitialisationCompteTraitement> traitements = reinitialisationCompteTraitementRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A);
        Map<String, BaReinitialisationCompteTraitement> traitementByDemandeId = traitements.stream()
                .filter(t -> t.getDemande() != null)
                .collect(Collectors.toMap(t -> t.getDemande().getId(), t -> t, (a, b) -> a));
        List<BaReinitialisationCompte> reinitialisations = reinitialisationCompteRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A);

        List<BaPlateformeUsageRowDto> rows = habilitationComptePlateformeRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A)
                .stream()
                .filter(compte -> canViewPlateformeStats(current, compte.getPlateforme()))
                .filter(compte -> matchesCompteFilter(compte, filter))
                .map(compte -> toUsageRow(compte, reinitialisations, traitementByDemandeId, filter))
                .filter(row -> row != null)
                .sorted(Comparator.comparing(BaPlateformeUsageRowDto::getDateDemandeCompte,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        int total = rows.size();
        int start = Math.min((int) pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        return new org.springframework.data.domain.PageImpl<>(rows.subList(start, end), pageable, total);
    }

    @Override
    public List<BaReunionDto> getAllReunions() {
        return reunionRepository.findByStatut(EStatut.A).stream().map(mapper::maps).collect(Collectors.toList());
    }

    @Override
    public BaReunionDto createReunion(final BaReunionDto dto) {
        logService.log(new BaLogDto(EAction.C, "Reunion " + dto.getTitre()));
        BaReunion entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        entity.setDepartement(BaUtils.isEmpty(dto.getIdDepartement()) ? null
                : departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        entity.setAgence(BaUtils.isEmpty(dto.getIdAgence()) ? null
                : agenceRepository.findById(dto.getIdAgence())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable")));
        entity.setService(BaUtils.isEmpty(dto.getIdService()) ? null
                : serviceRepository.findById(dto.getIdService())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable")));
        return mapper.maps(reunionRepository.save(entity));
    }

    @Override
    public BaReunionDto updateReunion(final String id, final BaReunionDto dto) {
        logService.log(new BaLogDto(EAction.U, "Reunion " + id));
        BaReunion entity = reunionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable"));
        entity.setTitre(dto.getTitre());
        entity.setObjet(dto.getObjet());
        entity.setDateHeure(dto.getDateHeure());
        entity.setProcesVerbalId(dto.getProcesVerbalId());
        entity.setValideParDirecteur(dto.getValideParDirecteur());
        entity.setDepartement(BaUtils.isEmpty(dto.getIdDepartement()) ? null
                : departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        entity.setAgence(BaUtils.isEmpty(dto.getIdAgence()) ? null
                : agenceRepository.findById(dto.getIdAgence())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable")));
        entity.setService(BaUtils.isEmpty(dto.getIdService()) ? null
                : serviceRepository.findById(dto.getIdService())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable")));
        return mapper.maps(reunionRepository.save(entity));
    }

    @Override
    public BaReunionParticipantDto addReunionParticipant(final BaReunionParticipantDto dto) {
        logService.log(new BaLogDto(EAction.C, "Participant reunion"));
        BaReunionParticipant entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        entity.setReunion(reunionRepository.findById(dto.getIdReunion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable")));
        if (!BaUtils.isEmpty(dto.getIdEmploye())) {
            entity.setEmploye(userRepository.findById(dto.getIdEmploye())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employe introuvable")));
        }
        return mapper.maps(reunionParticipantRepository.save(entity));
    }

    @Override
    public BaReunionParticipantDto updateReunionParticipant(final String id, final BaReunionParticipantDto dto) {
        logService.log(new BaLogDto(EAction.U, "Participant reunion " + id));
        BaReunionParticipant entity = reunionParticipantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Participant introuvable"));
        if (BaUtils.isEmpty(dto.getIdReunion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable");
        }
        entity.setReunion(reunionRepository.findById(dto.getIdReunion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable")));
        if (!BaUtils.isEmpty(dto.getIdEmploye())) {
            entity.setEmploye(userRepository.findById(dto.getIdEmploye())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employe introuvable")));
        } else {
            entity.setEmploye(null);
        }
        entity.setParticipantExterne(dto.getParticipantExterne());
        return mapper.maps(reunionParticipantRepository.save(entity));
    }

    @Override
    public BaReunionActionDto addReunionAction(final BaReunionActionDto dto) {
        logService.log(new BaLogDto(EAction.C, "Action reunion"));
        BaReunionAction entity = mapper.maps(dto);
        entity.setId(BaUtils.randomUUID());
        entity.setReunion(reunionRepository.findById(dto.getIdReunion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable")));
        if (!BaUtils.isEmpty(dto.getIdResponsable())) {
            entity.setResponsable(userRepository.findById(dto.getIdResponsable())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsable introuvable")));
        }
        return mapper.maps(reunionActionRepository.save(entity));
    }

    @Override
    public BaReunionActionDto updateReunionAction(final String id, final BaReunionActionDto dto) {
        logService.log(new BaLogDto(EAction.U, "Action reunion " + id));
        BaReunionAction entity = reunionActionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action introuvable"));
        if (BaUtils.isEmpty(dto.getIdReunion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable");
        }
        entity.setReunion(reunionRepository.findById(dto.getIdReunion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reunion introuvable")));
        entity.setLibelle(dto.getLibelle());
        entity.setDateLimite(dto.getDateLimite());
        entity.setTerminee(dto.getTerminee());
        if (!BaUtils.isEmpty(dto.getIdResponsable())) {
            entity.setResponsable(userRepository.findById(dto.getIdResponsable())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsable introuvable")));
        } else {
            entity.setResponsable(null);
        }
        return mapper.maps(reunionActionRepository.save(entity));
    }

    @Override
    public List<BaValidationDelegationDto> getValidationDelegations() {
        return validationDelegationRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .map(this::toValidationDelegationDto)
                .collect(Collectors.toList());
    }

    @Override
    public BaValidationDelegationDto createValidationDelegation(final BaValidationDelegationDto dto) {
        logService.log(new BaLogDto(EAction.C, "Delegation validation"));
        BaValidationDelegation entity = new BaValidationDelegation();
        entity.setId(BaUtils.randomUUID());
        hydrateValidationDelegation(entity, dto);
        entity.setActif(dto.getActif() == null || dto.getActif());
        return toValidationDelegationDto(validationDelegationRepository.save(entity));
    }

    @Override
    public BaValidationDelegationDto updateValidationDelegation(final String id, final BaValidationDelegationDto dto) {
        logService.log(new BaLogDto(EAction.U, "Delegation validation " + id));
        BaValidationDelegation entity = validationDelegationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delegation introuvable"));
        hydrateValidationDelegation(entity, dto);
        entity.setActif(dto.getActif() == null || dto.getActif());
        return toValidationDelegationDto(validationDelegationRepository.save(entity));
    }

    @Override
    public void desactiverValidationDelegation(final String id) {
        logService.log(new BaLogDto(EAction.D, "Delegation validation " + id));
        BaValidationDelegation entity = validationDelegationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delegation introuvable"));
        entity.setActif(false);
        entity.setStatut(EStatut.D);
        validationDelegationRepository.save(entity);
    }

    @Override
    public List<BaDgaPoleValidateurDto> getDgaPoleValidateurs() {
        return dgaPoleValidateurRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .map(this::toDgaPoleValidateurDto)
                .collect(Collectors.toList());
    }

    @Override
    public BaDgaPoleValidateurDto createDgaPoleValidateur(final BaDgaPoleValidateurDto dto) {
        logService.log(new BaLogDto(EAction.C, "DGA pole validateur"));
        BaDgaPoleValidateur entity = new BaDgaPoleValidateur();
        entity.setId(BaUtils.randomUUID());
        hydrateDgaPoleValidateur(entity, dto);
        entity.setActif(dto.getActif() == null || dto.getActif());
        return toDgaPoleValidateurDto(dgaPoleValidateurRepository.save(entity));
    }

    @Override
    public BaDgaPoleValidateurDto updateDgaPoleValidateur(final String id, final BaDgaPoleValidateurDto dto) {
        logService.log(new BaLogDto(EAction.U, "DGA pole validateur " + id));
        BaDgaPoleValidateur entity = dgaPoleValidateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuration DGA introuvable"));
        hydrateDgaPoleValidateur(entity, dto);
        entity.setActif(dto.getActif() == null || dto.getActif());
        return toDgaPoleValidateurDto(dgaPoleValidateurRepository.save(entity));
    }

    @Override
    public void desactiverDgaPoleValidateur(final String id) {
        logService.log(new BaLogDto(EAction.D, "DGA pole validateur " + id));
        BaDgaPoleValidateur entity = dgaPoleValidateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuration DGA introuvable"));
        entity.setActif(false);
        entity.setStatut(EStatut.D);
        dgaPoleValidateurRepository.save(entity);
    }

    private void hydrateDgaPoleValidateur(final BaDgaPoleValidateur entity, final BaDgaPoleValidateurDto dto) {
        if (dto == null || dto.getPole() == null || BaUtils.isEmpty(dto.getIdDga()) || dto.getDateDebut() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole, DGA et date debut obligatoires");
        }
        if (dto.getDateFin() != null && dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periode DGA invalide");
        }
        BaUser dga = userRepository.findById(dto.getIdDga())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "DGA introuvable"));
        if (!hasRole(dga, "BA_DGA") && dga.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur doit etre DGA ou avoir le role BA_DGA");
        }
        entity.setPole(dto.getPole());
        entity.setDga(dga);
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
    }

    private BaDgaPoleValidateurDto toDgaPoleValidateurDto(final BaDgaPoleValidateur entity) {
        BaDgaPoleValidateurDto dto = new BaDgaPoleValidateurDto();
        dto.setId(entity.getId());
        dto.setPole(entity.getPole());
        dto.setIdDga(entity.getDga() == null ? null : entity.getDga().getId());
        dto.setNomDga(fullName(entity.getDga()));
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setActif(entity.getActif());
        return dto;
    }

    private void hydrateValidationDelegation(final BaValidationDelegation entity,
                                             final BaValidationDelegationDto dto) {
        if (dto == null || BaUtils.isEmpty(dto.getIdDelegue()) || BaUtils.isEmpty(dto.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delegue et role obligatoires");
        }
        if (dto.getDateDebut() == null || dto.getDateFin() == null || dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periode de delegation invalide");
        }
        entity.setDelegant(BaUtils.isEmpty(dto.getIdDelegant()) ? null : userRepository.findById(dto.getIdDelegant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delegant introuvable")));
        entity.setDelegue(userRepository.findById(dto.getIdDelegue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delegue introuvable")));
        entity.setRoleCode(normalizeRoleCode(dto.getRoleCode()));
        entity.setDepartement(BaUtils.isEmpty(dto.getIdDepartement()) ? null : departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        entity.setService(BaUtils.isEmpty(dto.getIdService()) ? null : serviceRepository.findById(dto.getIdService())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable")));
        entity.setAgence(BaUtils.isEmpty(dto.getIdAgence()) ? null : agenceRepository.findById(dto.getIdAgence())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable")));
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
        entity.setMotif(dto.getMotif());
    }

    private BaValidationDelegationDto toValidationDelegationDto(final BaValidationDelegation entity) {
        BaValidationDelegationDto dto = new BaValidationDelegationDto();
        dto.setId(entity.getId());
        dto.setIdDelegant(entity.getDelegant() == null ? null : entity.getDelegant().getId());
        dto.setNomDelegant(fullName(entity.getDelegant()));
        dto.setIdDelegue(entity.getDelegue() == null ? null : entity.getDelegue().getId());
        dto.setNomDelegue(fullName(entity.getDelegue()));
        dto.setRoleCode(entity.getRoleCode());
        dto.setIdDepartement(entity.getDepartement() == null ? null : entity.getDepartement().getId());
        dto.setNomDepartement(entity.getDepartement() == null ? null : entity.getDepartement().getNom());
        dto.setIdService(entity.getService() == null ? null : entity.getService().getId());
        dto.setNomService(entity.getService() == null ? null : entity.getService().getNom());
        dto.setIdAgence(entity.getAgence() == null ? null : entity.getAgence().getId());
        dto.setNomAgence(entity.getAgence() == null ? null : entity.getAgence().getNom());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setActif(entity.getActif());
        dto.setMotif(entity.getMotif());
        return dto;
    }

    private void hydrateEmployeRelations(final BaUser entity, final BaUserDto dto) {
        entity.setService(BaUtils.isEmpty(dto.getIdService()) ? null : serviceRepository.findById(dto.getIdService())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable")));
        entity.setDepartement(BaUtils.isEmpty(dto.getIdDepartement()) ? null : departementRepository.findById(dto.getIdDepartement())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departement introuvable")));
        entity.setAgence(BaUtils.isEmpty(dto.getIdAgence()) ? null : agenceRepository.findById(dto.getIdAgence())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable")));
        entity.setSuperieur(BaUtils.isEmpty(dto.getIdSuperieur()) ? null : userRepository.findById(dto.getIdSuperieur())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Superieur introuvable")));
    }

    private BaFicheHabilitationDto toFicheDtoWithSignataires(final BaFicheHabilitation fiche) {
        BaFicheHabilitationDto ficheDto = mapper.maps(fiche);
        List<BaPlateforme> plateformes = ficheHabilitationPlateformeRepository == null
                ? new ArrayList<>()
                : ficheHabilitationPlateformeRepository.findByFicheId(fiche.getId()).stream()
                .map(BaFicheHabilitationPlateforme::getPlateforme)
                .collect(Collectors.toList());
        if (plateformes.isEmpty() && fiche.getCircuit() != null && fiche.getCircuit().getPlateforme() != null) {
            plateformes = List.of(fiche.getCircuit().getPlateforme());
        }
        if (!plateformes.isEmpty()) {
            ficheDto.setIdsPlateformes(plateformes.stream().map(BaPlateforme::getId).collect(Collectors.toList()));
            ficheDto.setNomsPlateformes(plateformes.stream().map(BaPlateforme::getNom).collect(Collectors.toList()));
            ficheDto.setNomPlateforme(plateformes.stream().map(BaPlateforme::getNom).collect(Collectors.joining(", ")));
        } else if (BaUtils.isEmpty(ficheDto.getNomPlateforme()) && !BaUtils.isEmpty(ficheDto.getIdCircuit())) {
            plateformeRepository.findFirstByCircuitIdAndStatut(ficheDto.getIdCircuit(), EStatut.A).ifPresent(p -> {
                ficheDto.setNomPlateforme(p.getNom());
                ficheDto.setIdsPlateformes(List.of(p.getId()));
                ficheDto.setNomsPlateformes(List.of(p.getNom()));
            });
        }
        List<BaFicheHabilitationEtapeDto> etapes = ficheHabilitationEtapeRepository.findByFicheIdOrderByOrdre(fiche.getId())
                .stream()
                .map(etape -> toFicheEtapeDto(fiche, etape))
                .collect(Collectors.toList());
        ficheDto.setEtapes(etapes.isEmpty() ? new ArrayList<>() : etapes);
        return ficheDto;
    }

    private BaFicheHabilitationEtapeDto toFicheEtapeDto(final BaFicheHabilitation fiche,
                                                        final BaFicheHabilitationEtape etape) {
        BaFicheHabilitationEtape effectiveEtape = refreshPendingValidatorIfNeeded(fiche, etape);
        BaFicheHabilitationEtapeDto dto = mapper.maps(effectiveEtape);
        BaCircuitEtape config = resolveCircuitEtapeForFicheEtape(fiche, effectiveEtape);
        if (config != null) {
            dto.setTypeEtape(config.getType());
            dto.setIdDepartement(config.getDepartement() == null ? null : config.getDepartement().getId());
            dto.setNomDepartement(config.getDepartement() == null ? null : config.getDepartement().getNom());
            dto.setIdService(config.getService() == null ? null : config.getService().getId());
            dto.setNomService(config.getService() == null ? null : config.getService().getNom());
            dto.setFonctionRequise(parseFonction(config.getFonctionRequise()));
            dto.setIdRole(config.getRole() == null ? null : config.getRole().getId());
            dto.setRoleLibelle(config.getRole() == null ? null : config.getRole().getLibelle());
        }
        return dto;
    }

    private BaFicheHabilitationEtape refreshPendingValidatorIfNeeded(final BaFicheHabilitation fiche,
                                                                     final BaFicheHabilitationEtape etape) {
        if (fiche == null || etape == null || etape.getStatutValidation() != EHabilitationStatut.EN_ATTENTE) {
            return etape;
        }
        if (etape.getValidateur() != null && isActiveValidator(etape.getValidateur())) {
            return etape;
        }
        BaCircuitEtape config = resolveCircuitEtapeForFicheEtape(fiche, etape);
        if (config == null) {
            return etape;
        }
        try {
            BaUser validateur = resolveSignerForCircuitEtape(fiche, config);
            if (etape.getValidateur() == null || !validateur.getId().equals(etape.getValidateur().getId())) {
                etape.setValidateur(validateur);
                return ficheHabilitationEtapeRepository.save(etape);
            }
        } catch (ResponseStatusException ex) {
            log.warn("Impossible de recalculer le validateur de l'etape {}: {}", etape.getId(), ex.getReason());
            if (etape.getValidateur() != null && !isActiveValidator(etape.getValidateur())) {
                etape.setValidateur(null);
                return ficheHabilitationEtapeRepository.save(etape);
            }
        }
        return etape;
    }

    private void ensureComptePlateformeForFiche(final BaFicheHabilitation fiche) {
        if (fiche == null || fiche.getEmploye() == null) {
            log.warn("Creation compte plateforme ignoree pour fiche {}: plateforme ou demandeur manquant",
                    fiche == null ? null : fiche.getId());
            return;
        }
        List<BaPlateforme> plateformes = ficheHabilitationPlateformeRepository.findByFicheId(fiche.getId()).stream()
                .map(BaFicheHabilitationPlateforme::getPlateforme)
                .collect(Collectors.toList());
        if (plateformes.isEmpty() && fiche.getCircuit() != null && fiche.getCircuit().getPlateforme() != null) {
            plateformes = List.of(fiche.getCircuit().getPlateforme());
        }
        for (BaPlateforme plateforme : plateformes) {
            habilitationComptePlateformeRepository.findByFicheIdAndPlateformeId(fiche.getId(), plateforme.getId())
                    .orElseGet(() -> {
                        BaHabilitationComptePlateforme compte = new BaHabilitationComptePlateforme();
                        compte.setId(BaUtils.randomUUID());
                        compte.setFiche(fiche);
                        compte.setPlateforme(plateforme);
                        compte.setDemandeur(fiche.getEmploye());
                        compte.setStatutCreation(EHabilitationCompteStatut.A_CREER);
                        return habilitationComptePlateformeRepository.save(compte);
                    });
        }
    }

    private List<BaPlateforme> resolvePlateformesForFiche(final BaFicheHabilitationDto dto) {
        List<String> ids = dto.getIdsPlateformes() == null ? new ArrayList<>() : dto.getIdsPlateformes().stream()
                .filter(id -> !BaUtils.isEmpty(id))
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            if (!BaUtils.isEmpty(dto.getIdCircuit())) {
                plateformeRepository.findFirstByCircuitIdAndStatut(dto.getIdCircuit(), EStatut.A)
                        .ifPresent(p -> ids.add(p.getId()));
            }
        }
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selectionnez au moins une plateforme.");
        }
        List<BaPlateforme> plateformes = plateformeRepository.findByIdInAndStatut(ids, EStatut.A);
        if (plateformes.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une ou plusieurs plateformes sont introuvables.");
        }
        return plateformes;
    }

    private BaCircuit resolveCircuitForPlateformes(final List<BaPlateforme> plateformes) {
        String circuitId = null;
        for (BaPlateforme plateforme : plateformes) {
            if (plateforme.getCircuit() == null || BaUtils.isEmpty(plateforme.getCircuit().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Toutes les plateformes selectionnees doivent etre liees a un circuit.");
            }
            if (circuitId == null) {
                circuitId = plateforme.getCircuit().getId();
            } else if (!circuitId.equals(plateforme.getCircuit().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Les plateformes d'une meme fiche doivent utiliser le meme circuit.");
            }
        }
        return plateformes.get(0).getCircuit();
    }

    private void saveFichePlateformes(final BaFicheHabilitation fiche, final List<BaPlateforme> plateformes) {
        List<BaFicheHabilitationPlateforme> links = new ArrayList<>();
        for (BaPlateforme plateforme : plateformes) {
            BaFicheHabilitationPlateforme link = new BaFicheHabilitationPlateforme();
            link.setId(BaUtils.randomUUID());
            link.setFiche(fiche);
            link.setPlateforme(plateforme);
            links.add(link);
        }
        ficheHabilitationPlateformeRepository.saveAll(links);
    }

    private BaHabilitationComptePlateforme findComptePlateforme(final String id) {
        return habilitationComptePlateformeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte plateforme introuvable."));
    }

    private BaHabilitationComptePlateformeDto toComptePlateformeDto(final BaHabilitationComptePlateforme compte) {
        BaHabilitationComptePlateformeDto dto = new BaHabilitationComptePlateformeDto();
        dto.setId(compte.getId());
        dto.setIdFiche(compte.getFiche() == null ? null : compte.getFiche().getId());
        dto.setIdPlateforme(compte.getPlateforme() == null ? null : compte.getPlateforme().getId());
        dto.setNomPlateforme(compte.getPlateforme() == null ? null : compte.getPlateforme().getNom());
        dto.setTypePlateforme(compte.getPlateforme() == null ? null : compte.getPlateforme().getTypePlateforme());
        dto.setIdDemandeur(compte.getDemandeur() == null ? null : compte.getDemandeur().getId());
        dto.setMatriculeDemandeur(compte.getDemandeur() == null ? null : compte.getDemandeur().getMatricule());
        dto.setNomCompletDemandeur(compte.getDemandeur() == null ? null
                : compte.getDemandeur().getNom() + " " + compte.getDemandeur().getPrenom());
        dto.setNomServiceDemandeur(compte.getDemandeur() == null || compte.getDemandeur().getService() == null
                ? null : compte.getDemandeur().getService().getNom());
        dto.setNomDepartementDemandeur(compte.getDemandeur() == null || compte.getDemandeur().getDepartement() == null
                ? null : compte.getDemandeur().getDepartement().getNom());
        dto.setStatutCreation(compte.getStatutCreation());
        dto.setDateDebutCreation(compte.getDateDebutCreation());
        dto.setDateCreation(compte.getDateCreation());
        dto.setIdCreePar(compte.getCreePar() == null ? null : compte.getCreePar().getId());
        dto.setNomCompletCreePar(compte.getCreePar() == null ? null
                : compte.getCreePar().getNom() + " " + compte.getCreePar().getPrenom());
        dto.setCommentaireCreation(compte.getCommentaireCreation());
        dto.setCreatedDate(compte.getCreatedDate());
        return dto;
    }

    private void assertCanCreateCompteForPlateforme(final BaUser user, final BaPlateforme plateforme) {
        if (!canCreateCompteForPlateforme(user, plateforme)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'etes pas autorise a creer un compte pour ce type de plateforme.");
        }
    }

    private boolean canCreateCompteForPlateforme(final BaUser user, final BaPlateforme plateforme) {
        if (user == null || plateforme == null || plateforme.getTypePlateforme() == null) {
            return false;
        }
        if (hasRole(user, BaRolesConstants.BA_ADMIN)) {
            return true;
        }
        return switch (plateforme.getTypePlateforme()) {
            case RESEAUX -> hasRole(user, BaRolesConstants.HABILITATION_CREATION_COMPTE_RESEAUX);
            case CORE_BANKING -> hasRole(user, BaRolesConstants.HABILITATION_CREATION_COMPTE_CORE_BANKING);
        };
    }

    private boolean canViewPlateformeStats(final BaUser user, final BaPlateforme plateforme) {
        return canCreateCompteForPlateforme(user, plateforme) || canResetCompteForPlateforme(user, plateforme);
    }

    private boolean matchesCompteFilter(final BaHabilitationComptePlateforme compte,
                                        final BaPlateformeStatFilterDto filter) {
        if (compte == null) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        BaPlateforme plateforme = compte.getPlateforme();
        BaUser demandeur = compte.getDemandeur();
        if (!BaUtils.isEmpty(filter.getIdPlateforme())
                && (plateforme == null || !filter.getIdPlateforme().equals(plateforme.getId()))) {
            return false;
        }
        if (filter.getTypePlateforme() != null
                && (plateforme == null || plateforme.getTypePlateforme() != filter.getTypePlateforme())) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdDemandeur())
                && (demandeur == null || !filter.getIdDemandeur().equals(demandeur.getId()))) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdDepartement())
                && (demandeur == null || demandeur.getDepartement() == null
                || !filter.getIdDepartement().equals(demandeur.getDepartement().getId()))) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdService())
                && (demandeur == null || demandeur.getService() == null
                || !filter.getIdService().equals(demandeur.getService().getId()))) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdCreateurCompte())
                && (compte.getCreePar() == null || !filter.getIdCreateurCompte().equals(compte.getCreePar().getId()))) {
            return false;
        }
        if (filter.getStatutCreation() != null && compte.getStatutCreation() != filter.getStatutCreation()) {
            return false;
        }
        return isInstantInPeriod(compte.getCreatedDate(), filter);
    }

    private boolean matchesReinitialisationFilter(final BaReinitialisationCompte demande,
                                                 final BaReinitialisationCompteTraitement traitement,
                                                 final BaPlateformeStatFilterDto filter) {
        if (demande == null) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        BaPlateforme plateforme = demande.getPlateforme();
        BaUser demandeur = demande.getDemandeur();
        if (!BaUtils.isEmpty(filter.getIdPlateforme())
                && (plateforme == null || !filter.getIdPlateforme().equals(plateforme.getId()))) {
            return false;
        }
        if (filter.getTypePlateforme() != null
                && (plateforme == null || plateforme.getTypePlateforme() != filter.getTypePlateforme())) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdDemandeur())
                && (demandeur == null || !filter.getIdDemandeur().equals(demandeur.getId()))) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdDepartement())
                && (demandeur == null || demandeur.getDepartement() == null
                || !filter.getIdDepartement().equals(demandeur.getDepartement().getId()))) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdService())
                && (demandeur == null || demandeur.getService() == null
                || !filter.getIdService().equals(demandeur.getService().getId()))) {
            return false;
        }
        if (!BaUtils.isEmpty(filter.getIdTraitePar())
                && (traitement == null || traitement.getTraitePar() == null
                || !filter.getIdTraitePar().equals(traitement.getTraitePar().getId()))) {
            return false;
        }
        if (filter.getStatutReinitialisation() != null && demande.getStatutDemande() != filter.getStatutReinitialisation()) {
            return false;
        }
        if (filter.getStatutTraitement() != null
                && (traitement == null || traitement.getStatutTraitement() != filter.getStatutTraitement())) {
            return false;
        }
        return isInstantInPeriod(demande.getCreatedDate(), filter);
    }

    private boolean isInstantInPeriod(final java.time.Instant instant, final BaPlateformeStatFilterDto filter) {
        if (filter == null || (filter.getDateDebut() == null && filter.getDateFin() == null)) {
            return true;
        }
        if (instant == null) {
            return false;
        }
        java.time.LocalDate date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        if (filter.getDateDebut() != null && date.isBefore(filter.getDateDebut())) {
            return false;
        }
        return filter.getDateFin() == null || !date.isAfter(filter.getDateFin());
    }

    private long countComptesByStatut(final List<BaHabilitationComptePlateforme> comptes,
                                      final EHabilitationCompteStatut statut) {
        return comptes.stream().filter(compte -> compte.getStatutCreation() == statut).count();
    }

    private long countReinitialisationsByStatut(final List<BaReinitialisationCompte> demandes,
                                                final EReinitialisationCompteStatut statut) {
        return demandes.stream().filter(demande -> demande.getStatutDemande() == statut).count();
    }

    private long countTraitementsByStatut(final List<BaReinitialisationCompteTraitement> traitements,
                                          final EReinitialisationTraitementStatut statut) {
        return traitements.stream().filter(traitement -> traitement.getStatutTraitement() == statut).count();
    }

    private <T> List<BaStatItemDto> toStatItems(final List<T> source,
                                                final java.util.function.Function<T, String> idResolver,
                                                final java.util.function.Function<T, String> labelResolver,
                                                final int limit) {
        Map<String, Long> counts = source.stream()
                .collect(Collectors.groupingBy(idResolver, Collectors.counting()));
        Map<String, String> labels = source.stream()
                .collect(Collectors.toMap(idResolver, labelResolver, (a, b) -> a));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new BaStatItemDto(e.getKey(), labels.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    private BaPlateformeUsageRowDto toUsageRow(final BaHabilitationComptePlateforme compte,
                                               final List<BaReinitialisationCompte> allReinitialisations,
                                               final Map<String, BaReinitialisationCompteTraitement> traitementByDemandeId,
                                               final BaPlateformeStatFilterDto filter) {
        List<BaReinitialisationCompte> reinitialisations = allReinitialisations.stream()
                .filter(demande -> demande.getDemandeur() != null && compte.getDemandeur() != null
                        && demande.getDemandeur().getId().equals(compte.getDemandeur().getId()))
                .filter(demande -> demande.getPlateforme() != null && compte.getPlateforme() != null
                        && demande.getPlateforme().getId().equals(compte.getPlateforme().getId()))
                .filter(demande -> matchesReinitialisationFilter(demande, traitementByDemandeId.get(demande.getId()), filter))
                .collect(Collectors.toList());
        if (hasReinitialisationOnlyFilters(filter) && reinitialisations.isEmpty()) {
            return null;
        }
        BaReinitialisationCompte latest = reinitialisations.stream()
                .max(Comparator.comparing(BaReinitialisationCompte::getCreatedDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        BaReinitialisationCompteTraitement latestTraitement = latest == null ? null : traitementByDemandeId.get(latest.getId());
        BaUser demandeur = compte.getDemandeur();
        BaPlateforme plateforme = compte.getPlateforme();
        BaPlateformeUsageRowDto row = new BaPlateformeUsageRowDto();
        row.setIdCompte(compte.getId());
        row.setIdDemandeur(demandeur == null ? null : demandeur.getId());
        row.setNomDemandeur(fullName(demandeur));
        row.setMatriculeDemandeur(demandeur == null ? null : demandeur.getMatricule());
        row.setIdDepartement(demandeur == null || demandeur.getDepartement() == null ? null : demandeur.getDepartement().getId());
        row.setDepartement(demandeur == null || demandeur.getDepartement() == null ? null : demandeur.getDepartement().getNom());
        row.setIdService(demandeur == null || demandeur.getService() == null ? null : demandeur.getService().getId());
        row.setService(demandeur == null || demandeur.getService() == null ? null : demandeur.getService().getNom());
        row.setIdPlateforme(plateforme == null ? null : plateforme.getId());
        row.setPlateforme(plateforme == null ? null : plateforme.getNom());
        row.setTypePlateforme(plateforme == null ? null : plateforme.getTypePlateforme());
        row.setStatutCreation(compte.getStatutCreation());
        row.setDateDemandeCompte(compte.getCreatedDate());
        row.setDateCreationCompte(compte.getDateCreation());
        row.setIdCreePar(compte.getCreePar() == null ? null : compte.getCreePar().getId());
        row.setCreePar(fullName(compte.getCreePar()));
        row.setNombreReinitialisations(reinitialisations.size());
        row.setDerniereReinitialisation(latest == null ? null : latest.getCreatedDate());
        row.setDernierStatutReinitialisation(latest == null ? null : latest.getStatutDemande());
        row.setDernierStatutTraitement(latestTraitement == null ? null : latestTraitement.getStatutTraitement());
        row.setIdDernierTraitePar(latestTraitement == null || latestTraitement.getTraitePar() == null ? null : latestTraitement.getTraitePar().getId());
        row.setDernierTraitePar(latestTraitement == null ? null : fullName(latestTraitement.getTraitePar()));
        return row;
    }

    private boolean hasReinitialisationOnlyFilters(final BaPlateformeStatFilterDto filter) {
        return filter != null
                && (filter.getStatutReinitialisation() != null
                || filter.getStatutTraitement() != null
                || !BaUtils.isEmpty(filter.getIdTraitePar()));
    }

    private String fullName(final BaUser user) {
        if (user == null) {
            return null;
        }
        String nom = user.getNom() == null ? "" : user.getNom();
        String prenom = user.getPrenom() == null ? "" : user.getPrenom();
        String value = (nom + " " + prenom).trim();
        return value.isEmpty() ? user.getUsername() : value;
    }

    private boolean hasRole(final BaUser user, final String roleCode) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleCode.equals(role.getCode()));
    }

    private void initReinitialisationEtapes(final BaReinitialisationCompte demande) {
        List<BaCircuitEtape> configs = circuitEtapeRepository.findByCircuitIdOrderByOrdre(demande.getCircuit().getId());
        if (configs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune etape configuree pour ce circuit.");
        }
        List<BaReinitialisationCompteEtape> etapes = new ArrayList<>();
        for (BaCircuitEtape config : configs) {
            BaReinitialisationCompteEtape etape = new BaReinitialisationCompteEtape();
            etape.setId(BaUtils.randomUUID());
            etape.setDemande(demande);
            etape.setOrdre(config.getOrdre());
            etape.setStatutValidation(EHabilitationStatut.EN_ATTENTE);
            etape.setValidateur(resolveSignerForCircuitEtape(demande.getDemandeur(), config));
            etapes.add(etape);
        }
        reinitialisationCompteEtapeRepository.saveAll(etapes);
    }

    private BaReinitialisationCompteEtape currentReinitialisationEtape(final BaReinitialisationCompte demande) {
        return reinitialisationCompteEtapeRepository.findByDemandeIdOrderByOrdre(demande.getId()).stream()
                .filter(e -> e.getStatutValidation() == EHabilitationStatut.EN_ATTENTE)
                .findFirst()
                .orElse(null);
    }

    private boolean allReinitialisationEtapesValidated(final BaReinitialisationCompte demande) {
        return reinitialisationCompteEtapeRepository.findByDemandeIdOrderByOrdre(demande.getId()).stream()
                .allMatch(e -> e.getStatutValidation() == EHabilitationStatut.VALIDE);
    }

    private boolean isUserAllowedForReinitialisationValidation(final BaUserDto user,
                                                              final BaUser employe,
                                                              final BaReinitialisationCompteEtape etape) {
        BaCircuitEtape config = resolveCircuitEtapeForReinitialisationEtape(etape);
        BaUser demandeur = etape.getDemande() == null ? null : etape.getDemande().getDemandeur();
        return isUserAllowedByCircuitConfig(user, employe, demandeur, config);
    }

    private void ensureReinitialisationTraitement(final BaReinitialisationCompte demande) {
        reinitialisationCompteTraitementRepository.findByDemandeId(demande.getId()).orElseGet(() -> {
            BaReinitialisationCompteTraitement traitement = new BaReinitialisationCompteTraitement();
            traitement.setId(BaUtils.randomUUID());
            traitement.setDemande(demande);
            traitement.setStatutTraitement(EReinitialisationTraitementStatut.A_TRAITER);
            return reinitialisationCompteTraitementRepository.save(traitement);
        });
    }

    private BaReinitialisationCompteTraitement findReinitialisationTraitement(final String id) {
        return reinitialisationCompteTraitementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Traitement introuvable."));
    }

    private void assertCanResetCompteForPlateforme(final BaUser user, final BaPlateforme plateforme) {
        if (!canResetCompteForPlateforme(user, plateforme)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'etes pas autorise a reinitialiser un compte pour ce type de plateforme.");
        }
    }

    private boolean canResetCompteForPlateforme(final BaUser user, final BaPlateforme plateforme) {
        if (user == null || plateforme == null || plateforme.getTypePlateforme() == null) {
            return false;
        }
        if (hasRole(user, BaRolesConstants.BA_ADMIN)) {
            return true;
        }
        return switch (plateforme.getTypePlateforme()) {
            case RESEAUX -> hasRole(user, BaRolesConstants.HABILITATION_REINITIALISATION_COMPTE_RESEAUX);
            case CORE_BANKING -> hasRole(user, BaRolesConstants.HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING);
        };
    }

    private BaReinitialisationCompteDto toReinitialisationDto(final BaReinitialisationCompte demande) {
        BaReinitialisationCompteDto dto = new BaReinitialisationCompteDto();
        dto.setId(demande.getId());
        dto.setIdDemandeur(demande.getDemandeur() == null ? null : demande.getDemandeur().getId());
        dto.setNomCompletDemandeur(demande.getDemandeur() == null ? null : demande.getDemandeur().getNom() + " " + demande.getDemandeur().getPrenom());
        dto.setMatriculeDemandeur(demande.getDemandeur() == null ? null : demande.getDemandeur().getMatricule());
        dto.setIdPlateforme(demande.getPlateforme() == null ? null : demande.getPlateforme().getId());
        dto.setNomPlateforme(demande.getPlateforme() == null ? null : demande.getPlateforme().getNom());
        dto.setTypePlateforme(demande.getPlateforme() == null ? null : demande.getPlateforme().getTypePlateforme());
        dto.setIdCircuit(demande.getCircuit() == null ? null : demande.getCircuit().getId());
        dto.setNomCircuit(demande.getCircuit() == null ? null : demande.getCircuit().getLibelle());
        dto.setMotif(demande.getMotif());
        dto.setStatutDemande(demande.getStatutDemande());
        dto.setDateValidation(demande.getDateValidation());
        dto.setCreatedDate(demande.getCreatedDate());
        dto.setEtapes(reinitialisationCompteEtapeRepository.findByDemandeIdOrderByOrdre(demande.getId()).stream()
                .map(this::toReinitialisationEtapeDto)
                .collect(Collectors.toList()));
        reinitialisationCompteTraitementRepository.findByDemandeId(demande.getId()).ifPresent(t -> {
            dto.setIdTraitement(t.getId());
            dto.setStatutTraitement(t.getStatutTraitement());
            dto.setDateDebutTraitement(t.getDateDebutTraitement());
            dto.setDateTraitement(t.getDateTraitement());
            dto.setNomCompletTraitePar(t.getTraitePar() == null ? null : t.getTraitePar().getNom() + " " + t.getTraitePar().getPrenom());
            dto.setCommentaireTraitement(t.getCommentaireTraitement());
        });
        return dto;
    }

    private BaReinitialisationCompteEtapeDto toReinitialisationEtapeDto(final BaReinitialisationCompteEtape etape) {
        BaReinitialisationCompteEtapeDto dto = new BaReinitialisationCompteEtapeDto();
        dto.setId(etape.getId());
        dto.setIdDemande(etape.getDemande() == null ? null : etape.getDemande().getId());
        BaCircuitEtape config = resolveCircuitEtapeForReinitialisationEtape(etape);
        dto.setNomEtape(buildCircuitEtapeLabel(config));
        dto.setIdValidateur(etape.getValidateur() == null ? null : etape.getValidateur().getId());
        dto.setNomCompletValidateur(etape.getValidateur() == null ? null : etape.getValidateur().getNom() + " " + etape.getValidateur().getPrenom());
        dto.setOrdre(etape.getOrdre());
        dto.setStatutValidation(etape.getStatutValidation());
        dto.setCommentaire(etape.getCommentaire());
        dto.setDateValidation(etape.getDateValidation());
        return dto;
    }

    private List<BaFicheHabilitationDto> sortFiches(final List<BaFicheHabilitationDto> fiches,
                                                    final org.springframework.data.domain.Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return fiches;
        }
        java.util.Comparator<BaFicheHabilitationDto> comparator = null;
        for (org.springframework.data.domain.Sort.Order order : sort) {
            java.util.Comparator<BaFicheHabilitationDto> c = comparatorFor(order);
            if (c == null) {
                continue;
            }
            if (order.getDirection() == org.springframework.data.domain.Sort.Direction.DESC) {
                c = c.reversed();
            }
            comparator = comparator == null ? c : comparator.thenComparing(c);
        }
        if (comparator != null) {
            return fiches.stream().sorted(comparator).collect(Collectors.toList());
        }
        return fiches;
    }

    private java.util.Comparator<BaFicheHabilitationDto> comparatorFor(final org.springframework.data.domain.Sort.Order order) {
        String property = order.getProperty();
        if ("createdDate".equalsIgnoreCase(property)) {
            return java.util.Comparator.comparing(BaFicheHabilitationDto::getCreatedDate, java.util.Comparator.nullsLast(java.time.ZonedDateTime::compareTo));
        }
        if ("priorite".equalsIgnoreCase(property)) {
            return java.util.Comparator.comparing(BaFicheHabilitationDto::getPriorite, java.util.Comparator.nullsLast(Enum::compareTo));
        }
        return null;
    }

    private org.springframework.data.domain.Page<BaFicheHabilitationDto> toPage(
            final List<BaFicheHabilitationDto> fiches,
            final org.springframework.data.domain.Pageable pageable) {
        int total = fiches.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        if (start > end) {
            start = end;
        }
        List<BaFicheHabilitationDto> content = fiches.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    private BaUser resolveUserForCurrent(final BaUserDto user) {
        if (user == null || BaUtils.isEmpty(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable");
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable"));
    }

    private boolean isUserAllowedForValidation(final BaUserDto user,
                                               final BaUser employe,
                                               final BaFicheHabilitation fiche,
                                               final BaFicheHabilitationEtape ficheEtape) {
        if (employe == null || ficheEtape == null) {
            return false;
        }
        if (isUserAllowedByCircuitStep(user, employe, fiche, ficheEtape)) {
            return true;
        }
        if (!isEligibleSignerFunction(employe)) {
            return false;
        }
        return false;
    }

    private boolean isUserAllowedByCircuitStep(final BaUserDto user,
                                               final BaUser employe,
                                               final BaFicheHabilitation fiche,
                                               final BaFicheHabilitationEtape ficheEtape) {
        if (user == null || employe == null || fiche == null || fiche.getCircuit() == null || ficheEtape == null) {
            return false;
        }
        BaCircuitEtape config = resolveCircuitEtapeForFicheEtape(fiche, ficheEtape);
        return isUserAllowedByCircuitConfig(user, employe, fiche.getEmploye(), config);
    }

    private BaCircuitEtape resolveCircuitEtapeForFicheEtape(final BaFicheHabilitation fiche,
                                                            final BaFicheHabilitationEtape ficheEtape) {
        if (fiche == null || fiche.getCircuit() == null || ficheEtape == null || ficheEtape.getOrdre() == null) {
            return null;
        }
        return circuitEtapeRepository.findByCircuitIdOrderByOrdre(fiche.getCircuit().getId()).stream()
                .filter(config -> config.getOrdre() != null && config.getOrdre().equals(ficheEtape.getOrdre()))
                .findFirst()
                .orElse(null);
    }

    private BaCircuitEtape resolveCircuitEtapeForReinitialisationEtape(final BaReinitialisationCompteEtape etape) {
        if (etape == null || etape.getDemande() == null || etape.getDemande().getCircuit() == null || etape.getOrdre() == null) {
            return null;
        }
        return circuitEtapeRepository.findByCircuitIdOrderByOrdre(etape.getDemande().getCircuit().getId()).stream()
                .filter(config -> config.getOrdre() != null && config.getOrdre().equals(etape.getOrdre()))
                .findFirst()
                .orElse(null);
    }

    private String buildCircuitEtapeLabel(final BaCircuitEtape config) {
        if (config == null) {
            return null;
        }
        if (config.getRole() != null && !BaUtils.isEmpty(config.getRole().getLibelle())) {
            return config.getRole().getLibelle();
        }
        EFonctionEmploye fonction = getRequiredSignerFunction(config);
        if (fonction != null) {
            return fonction.name();
        }
        if (config.getService() != null) {
            return config.getService().getNom();
        }
        if (config.getDepartement() != null) {
            return config.getDepartement().getNom();
        }
        return "Etape " + config.getOrdre();
    }

    private boolean isUserAllowedByCircuitConfig(final BaUserDto user,
                                                final BaUser employe,
                                                final BaUser agentConcerne,
                                                final BaCircuitEtape config) {
        if (config == null || !isActiveValidator(employe)) {
            return false;
        }
        Set<String> allowedRoleCodes = resolveAllowedValidatorRoleCodes(config);
        return !allowedRoleCodes.isEmpty()
                && (hasAnyRole(user, allowedRoleCodes)
                || hasAnyRole(employe, allowedRoleCodes)
                || hasActiveValidationDelegation(employe, allowedRoleCodes, config))
                && (!requiresStructureScope(config) || isRoleValidatorInCircuitScope(employe, agentConcerne, config));
    }

    private EFonctionEmploye parseFonction(final String value) {
        if (BaUtils.isEmpty(value)) {
            return null;
        }
        try {
            return EFonctionEmploye.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Fonction requise inconnue dans le circuit: {}", value);
            return null;
        }
    }

    private boolean isEligibleSignerFunction(final BaUser user) {
        return user != null
                && (user.getFonction() == EFonctionEmploye.DIRECTEUR
                || user.getFonction() == EFonctionEmploye.CHEF_SERVICE
                || user.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                || user.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL);
    }

    private BaUser resolveSignerForCircuitEtape(final BaFicheHabilitation fiche, final BaCircuitEtape etape) {
        if (etape == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de determiner le signataire: etape de circuit incomplete.");
        }
        Set<String> allowedRoleCodes = resolveAllowedValidatorRoleCodes(etape);
        if (allowedRoleCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de determiner le signataire: role validateur manquant pour l'etape.");
        }
        BaUser agentConcerne = fiche == null ? null : fiche.getEmploye();
        return findRoleSigner(agentConcerne, etape, allowedRoleCodes);
    }

    private BaUser resolveSignerForCircuitEtape(final BaUser agentConcerne, final BaCircuitEtape etape) {
        if (etape == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de determiner le signataire: etape de circuit incomplete.");
        }
        Set<String> allowedRoleCodes = resolveAllowedValidatorRoleCodes(etape);
        if (allowedRoleCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de determiner le signataire: role validateur manquant pour l'etape.");
        }
        return findRoleSigner(agentConcerne, etape, allowedRoleCodes);
    }

    private BaUser resolveSignerForCircuitEtape(final BaCircuitEtape etape) {
        return resolveSignerForCircuitEtape((BaUser) null, etape);
    }

    private boolean isEligibleSignerFunction(final EFonctionEmploye fonction) {
        return fonction == EFonctionEmploye.DIRECTEUR
                || fonction == EFonctionEmploye.CHEF_SERVICE
                || fonction == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                || fonction == EFonctionEmploye.DIRECTEUR_GENERAL;
    }

    private boolean isGeneralManagementFunction(final EFonctionEmploye fonction) {
        return fonction == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                || fonction == EFonctionEmploye.DIRECTEUR_GENERAL;
    }

    private EFonctionEmploye getRequiredSignerFunction(final BaCircuitEtape etape) {
        if (etape == null) {
            return null;
        }
        EFonctionEmploye configured = parseFonction(etape.getFonctionRequise());
        if (configured != null) {
            return configured;
        }
        if (etape.getType() == EHabilitationEtapeType.SERVICE) {
            return EFonctionEmploye.CHEF_SERVICE;
        }
        if (etape.getType() == EHabilitationEtapeType.DEPARTEMENT) {
            return EFonctionEmploye.DIRECTEUR;
        }
        return null;
    }

    private boolean isUserMatchingCircuitEtape(final BaUser user,
                                               final BaUser agentConcerne,
                                               final BaCircuitEtape etape,
                                               final EFonctionEmploye requiredFunction) {
        if (user == null || etape == null || requiredFunction == null || user.getFonction() != requiredFunction) {
            return false;
        }
        if (isGeneralManagementFunction(requiredFunction)) {
            return isUserInValidationScope(user, agentConcerne, etape);
        }
        if (etape.getType() == EHabilitationEtapeType.SERVICE) {
            return user.getService() != null
                    && etape.getService() != null
                    && user.getService().getId().equals(etape.getService().getId());
        }
        return user.getDepartement() != null
                && etape.getDepartement() != null
                && user.getDepartement().getId().equals(etape.getDepartement().getId());
    }

    private boolean isActiveValidator(final BaUser user) {
        return user != null
                && user.getStatut() == EStatut.A
                && Boolean.TRUE.equals(user.getActivated())
                && !Boolean.TRUE.equals(user.getLocked());
    }

    private boolean isUserInValidationScope(final BaUser validateur,
                                            final BaUser agentConcerne,
                                            final BaCircuitEtape config) {
        if (validateur == null) {
            return false;
        }
        EFonctionEmploye requiredFunction = getRequiredSignerFunction(config);
        if (requiredFunction == EFonctionEmploye.DIRECTEUR_GENERAL) {
            if (validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL || hasRole(validateur, "BA_DG")) {
                return true;
            }
            return (validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT || hasRole(validateur, "BA_DGA"))
                    && !hasActiveDgValidator();
        }
        if (validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL) {
            return true;
        }
        if (agentConcerne != null) {
            BaUser dgaDepartement = resolveEffectiveDgaValidateur(agentConcerne.getDepartement());
            if (dgaDepartement != null) {
                return isActiveValidator(dgaDepartement) && dgaDepartement.getId().equals(validateur.getId());
            }
            if (sameDepartement(validateur, agentConcerne)
                    || sameService(validateur, agentConcerne)
                    || sameAgence(validateur, agentConcerne)) {
                return true;
            }
            if (hasAnyStructure(agentConcerne)) {
                return false;
            }
        }
        if (config != null) {
            if (config.getType() == EHabilitationEtapeType.SERVICE && config.getService() != null) {
                return validateur.getService() != null
                        && validateur.getService().getId().equals(config.getService().getId());
            }
            if (config.getType() == EHabilitationEtapeType.DEPARTEMENT && config.getDepartement() != null) {
                return validateur.getDepartement() != null
                        && validateur.getDepartement().getId().equals(config.getDepartement().getId());
            }
        }
        return !hasAnyStructure(validateur);
    }

    private boolean sameDepartement(final BaUser left, final BaUser right) {
        return left.getDepartement() != null
                && right.getDepartement() != null
                && left.getDepartement().getId().equals(right.getDepartement().getId());
    }

    private boolean sameService(final BaUser left, final BaUser right) {
        return left.getService() != null
                && right.getService() != null
                && left.getService().getId().equals(right.getService().getId());
    }

    private boolean sameAgence(final BaUser left, final BaUser right) {
        return left.getAgence() != null
                && right.getAgence() != null
                && left.getAgence().getId().equals(right.getAgence().getId());
    }

    private boolean hasAnyStructure(final BaUser user) {
        return user != null
                && (user.getDepartement() != null || user.getService() != null || user.getAgence() != null);
    }

    private BaUser resolveDgaValidateur(final String idDgaValidateur) {
        if (BaUtils.isEmpty(idDgaValidateur)) {
            return null;
        }
        BaUser dga = userRepository.findById(idDgaValidateur)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "DGA validateur introuvable"));
        if (dga.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le validateur du departement doit avoir la fonction DIRECTEUR_GENERAL_ADJOINT.");
        }
        if (!isActiveValidator(dga)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le DGA validateur selectionne doit etre actif, active et non verrouille.");
        }
        return dga;
    }

    private BaDepartement resolveParentDepartement(final String parentDepartementId,
                                                   final String currentDepartementId) {
        if (BaUtils.isEmpty(parentDepartementId)) {
            return null;
        }
        if (parentDepartementId.equals(currentDepartementId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un departement ne peut pas etre son propre parent.");
        }
        return departementRepository.findById(parentDepartementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Departement parent introuvable"));
    }

    private BaUser resolveEffectiveDgaValidateur(final BaDepartement departement) {
        if (departement == null) {
            return null;
        }
        BaDepartement cursor = departement;
        Set<String> visited = new HashSet<>();
        EDgaPole inheritedPole = null;
        while (cursor != null && cursor.getId() != null && visited.add(cursor.getId())) {
            if (inheritedPole == null && cursor.getDgaPole() != null) {
                inheritedPole = cursor.getDgaPole();
            }
            cursor = cursor.getParentDepartement();
        }
        if (inheritedPole == null) {
            inheritedPole = EDgaPole.DEVELOPPEMENT;
        }
        BaUser poleDga = resolveConfiguredDgaForPole(inheritedPole);
        if (poleDga != null) {
            return poleDga;
        }
        return dgaPoleValidateurRepository.findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut.A).stream()
                .filter(this::isCurrentDgaPoleValidateur)
                .map(BaDgaPoleValidateur::getDga)
                .filter(this::isActiveValidator)
                .findFirst()
                .orElse(null);
    }

    private BaUser resolveConfiguredDgaForPole(final EDgaPole pole) {
        return dgaPoleValidateurRepository.findByPoleAndStatutAndActifTrueOrderByCreatedDateDesc(pole, EStatut.A).stream()
                .filter(this::isCurrentDgaPoleValidateur)
                .map(BaDgaPoleValidateur::getDga)
                .filter(this::isActiveValidator)
                .findFirst()
                .orElse(null);
    }

    private boolean isCurrentDgaPoleValidateur(final BaDgaPoleValidateur config) {
        if (config == null || !Boolean.TRUE.equals(config.getActif())) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (config.getDateDebut() == null || !config.getDateDebut().isAfter(today))
                && (config.getDateFin() == null || !config.getDateFin().isBefore(today));
    }

    private BaDepartementDto toDepartementDto(final BaDepartement departement) {
        BaDepartementDto dto = mapper.maps(departement);
        BaUser dgaEffectif = resolveEffectiveDgaValidateur(departement);
        if (dgaEffectif != null) {
            dto.setIdDgaEffectif(dgaEffectif.getId());
            dto.setNomDgaEffectif(fullName(dgaEffectif));
        }
        return dto;
    }

    private List<BaUser> findSignerCandidates(final BaCircuitEtape etape,
                                              final EFonctionEmploye requiredFunction) {
        if (isGeneralManagementFunction(requiredFunction)) {
            return userRepository.findByFonctionAndStatut(requiredFunction, EStatut.A);
        }
        if (etape.getType() == EHabilitationEtapeType.SERVICE && etape.getService() != null) {
            return userRepository.findByFonctionAndServiceIdAndStatut(
                    requiredFunction, etape.getService().getId(), EStatut.A);
        }
        if (etape.getType() == EHabilitationEtapeType.DEPARTEMENT && etape.getDepartement() != null) {
            return userRepository.findByFonctionAndDepartementIdAndStatut(
                    requiredFunction, etape.getDepartement().getId(), EStatut.A);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Impossible de determiner le signataire: structure de l'etape incomplete.");
    }

    private BaUser findRoleSigner(final BaUser agentConcerne,
                                  final BaCircuitEtape etape,
                                  final Set<String> allowedRoleCodes) {
        return userRepository.findByStatut(EStatut.A).stream()
                .filter(this::isActiveValidator)
                .filter(user -> hasAnyRole(user, allowedRoleCodes)
                        || hasActiveValidationDelegation(user, allowedRoleCodes, etape))
                .filter(user -> !requiresStructureScope(etape) || isRoleValidatorInCircuitScope(user, agentConcerne, etape))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Aucun signataire trouve avec le role de l'etape " + etape.getOrdre()));
    }

    private boolean isRoleValidatorInCircuitScope(final BaUser validateur,
                                                  final BaUser agentConcerne,
                                                  final BaCircuitEtape etape) {
        if (validateur == null || etape == null) {
            return false;
        }
        EFonctionEmploye requiredFunction = getRequiredSignerFunction(etape);
        if (isGeneralManagementFunction(requiredFunction)) {
            return isUserInValidationScope(validateur, agentConcerne, etape);
        }
        if (etape.getType() == EHabilitationEtapeType.SERVICE) {
            return validateur.getService() != null
                    && etape.getService() != null
                    && validateur.getService().getId().equals(etape.getService().getId());
        }
        return validateur.getDepartement() != null
                && etape.getDepartement() != null
                && validateur.getDepartement().getId().equals(etape.getDepartement().getId());
    }

    private boolean hasActiveValidationDelegation(final BaUser user,
                                                  final Set<String> allowedRoleCodes,
                                                  final BaCircuitEtape etape) {
        if (user == null || allowedRoleCodes == null || allowedRoleCodes.isEmpty()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return validationDelegationRepository
                .findByDelegueIdAndStatutAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        user.getId(), EStatut.A, today, today)
                .stream()
                .filter(delegation -> delegationRoleMatches(delegation, allowedRoleCodes))
                .anyMatch(delegation -> delegationScopeMatches(delegation, user, etape));
    }

    private boolean delegationRoleMatches(final BaValidationDelegation delegation,
                                          final Set<String> allowedRoleCodes) {
        Set<String> delegationRoles = new HashSet<>();
        addRoleAliases(delegationRoles, delegation == null ? null : delegation.getRoleCode());
        return delegationRoles.stream().anyMatch(allowedRoleCodes::contains);
    }

    private boolean delegationScopeMatches(final BaValidationDelegation delegation,
                                           final BaUser user,
                                           final BaCircuitEtape etape) {
        if (delegation == null || etape == null) {
            return false;
        }
        if (delegation.getDepartement() != null) {
            return etape.getDepartement() != null
                    && delegation.getDepartement().getId().equals(etape.getDepartement().getId());
        }
        if (delegation.getService() != null) {
            return etape.getService() != null
                    && delegation.getService().getId().equals(etape.getService().getId());
        }
        if (delegation.getAgence() != null) {
            return user.getAgence() != null
                    && delegation.getAgence().getId().equals(user.getAgence().getId());
        }
        return isRoleValidatorInCircuitScope(user, null, etape);
    }

    private boolean requiresStructureScope(final BaCircuitEtape etape) {
        if (etape == null) {
            return false;
        }
        Set<String> configuredRoleCodes = new HashSet<>();
        addRoleAliases(configuredRoleCodes, etape.getRole() == null ? null : etape.getRole().getCode());
        addRoleAliases(configuredRoleCodes, etape.getRole() == null ? null : etape.getRole().getLibelle());
        if (!configuredRoleCodes.isEmpty()) {
            return configuredRoleCodes.stream().anyMatch(this::isBusinessValidatorRole);
        }
        String businessRole = roleCodeForFunction(getRequiredSignerFunction(etape));
        return isBusinessValidatorRole(normalizeRoleCode(businessRole));
    }

    private boolean isBusinessValidatorRole(final String roleCode) {
        String normalized = normalizeRoleCode(roleCode);
        return "BA_DIRECTEUR".equals(normalized)
                || "DIRECTEUR".equals(normalized)
                || "BA_CHEF_SERVICE".equals(normalized)
                || "CHEF_SERVICE".equals(normalized)
                || "BA_DGA".equals(normalized)
                || "DGA".equals(normalized)
                || "BA_DG".equals(normalized)
                || "DG".equals(normalized);
    }

    private Set<String> resolveAllowedValidatorRoleCodes(final BaCircuitEtape etape) {
        Set<String> roles = new HashSet<>();
        if (etape == null) {
            return roles;
        }
        addRoleAliases(roles, etape.getRole() == null ? null : etape.getRole().getCode());
        addRoleAliases(roles, etape.getRole() == null ? null : etape.getRole().getLibelle());
        EFonctionEmploye requiredFunction = getRequiredSignerFunction(etape);
        addRoleAliases(roles, roleCodeForFunction(requiredFunction));
        if (requiredFunction == EFonctionEmploye.DIRECTEUR_GENERAL) {
            addRoleAliases(roles, "BA_DGA");
        }
        return roles;
    }

    private boolean hasActiveDgValidator() {
        return userRepository.findByStatut(EStatut.A).stream()
                .filter(this::isActiveValidator)
                .anyMatch(user -> user.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL || hasRole(user, "BA_DG"));
    }

    private String roleCodeForFunction(final EFonctionEmploye fonction) {
        if (fonction == null) {
            return null;
        }
        return switch (fonction) {
            case DIRECTEUR -> "BA_DIRECTEUR";
            case CHEF_SERVICE -> "BA_CHEF_SERVICE";
            case DIRECTEUR_GENERAL_ADJOINT -> "BA_DGA";
            case DIRECTEUR_GENERAL -> "BA_DG";
            default -> null;
        };
    }

    private void addRoleAliases(final Set<String> roles, final String roleCode) {
        String normalized = normalizeRoleCode(roleCode);
        if (normalized == null) {
            return;
        }
        roles.add(normalized);
        if (normalized.startsWith(BaConstants.ROLE_PREFIX)) {
            roles.add(normalized.substring(BaConstants.ROLE_PREFIX.length()));
        } else {
            roles.add(BaConstants.ROLE_PREFIX + normalized);
        }
    }

    private boolean hasAnyRole(final BaUser user, final Set<String> allowedRoleCodes) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty() || allowedRoleCodes == null || allowedRoleCodes.isEmpty()) {
            return false;
        }
        return user.getRoles().stream().anyMatch(role ->
                allowedRoleCodes.contains(normalizeRoleCode(role.getCode()))
                        || allowedRoleCodes.contains(normalizeRoleCode(role.getLibelle())));
    }

    private boolean hasAnyRole(final BaUserDto user, final Set<String> allowedRoleCodes) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty() || allowedRoleCodes == null || allowedRoleCodes.isEmpty()) {
            return false;
        }
        return user.getRoles().stream().anyMatch(role ->
                allowedRoleCodes.contains(normalizeRoleCode(role.getCode()))
                        || allowedRoleCodes.contains(normalizeRoleCode(role.getLibelle())));
    }

    private String normalizeRoleCode(final String roleCode) {
        if (BaUtils.isEmpty(roleCode)) {
            return null;
        }
        return roleCode.trim().toUpperCase();
    }

    private boolean hasRole(final BaUser user, final BaRole requiredRole) {
        if (user == null || requiredRole == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        return user.getRoles().stream().anyMatch(r ->
                (r.getId() != null && r.getId().equals(requiredRole.getId()))
                        || (r.getCode() != null && requiredRole.getCode() != null
                        && r.getCode().equalsIgnoreCase(requiredRole.getCode())));
    }

    private boolean hasRole(final BaUserDto user, final BaRole requiredRole) {
        if (user == null || requiredRole == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        return user.getRoles().stream().anyMatch(r ->
                (r.getId() != null && r.getId().equals(requiredRole.getId()))
                        || (r.getCode() != null && requiredRole.getCode() != null
                        && r.getCode().equalsIgnoreCase(requiredRole.getCode())));
    }
}

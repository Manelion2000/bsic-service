package com.bakouan.app.service.impl;

import com.bakouan.app.dto.BaAutorisationSortieDto;
import com.bakouan.app.dto.BaAutorisationSortieRequestDto;
import com.bakouan.app.dto.BaAutorisationSortieSuiviDto;
import com.bakouan.app.dto.BaRhDemandesFilterDto;
import com.bakouan.app.dto.BaRhDemandesStatsDto;
import com.bakouan.app.dto.BaStatItemDto;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.EAutorisationSortieStatut;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaAgence;
import com.bakouan.app.model.BaAutorisationSortie;
import com.bakouan.app.model.BaDepartement;
import com.bakouan.app.model.BaService;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaAgenceRepository;
import com.bakouan.app.repositories.BaAutorisationSortieRepository;
import com.bakouan.app.repositories.BaDepartementRepository;
import com.bakouan.app.repositories.BaServiceRepository;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.service.BaAutorisationSortieService;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BaAutorisationSortieServiceImpl implements BaAutorisationSortieService {
    private static final DateTimeFormatter VALIDATION_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH'h'mm");
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);

    private final BaAutorisationSortieRepository autorisationSortieRepository;
    private final BaUserRepository userRepository;
    private final BaDepartementRepository departementRepository;
    private final BaServiceRepository serviceRepository;
    private final BaAgenceRepository agenceRepository;
    private final BaUserService userService;

    @Override
    public BaAutorisationSortieDto createDemande(final BaAutorisationSortieRequestDto request) {
        if (request == null || BaUtils.isEmpty(request.getMotif())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif est obligatoire.");
        }
        if (request.getDateSortie() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de sortie est obligatoire.");
        }

        String typeStructure = normalizeTypeStructure(request.getTypeStructure());
        BaUser demandeur = currentUserEntity();
        BaDepartement direction = resolveDirection(request.getIdDirection(), demandeur);
        BaService service = resolveService(request.getIdService(), demandeur);
        BaAgence agence = resolveAgence(request.getIdAgence(), demandeur);
        validateStructureChoice(typeStructure, direction, service, agence);
        BaUser superieur = resolveSuperieur(demandeur, typeStructure, direction, service, agence);

        BaAutorisationSortie entity = new BaAutorisationSortie();
        entity.setId(BaUtils.randomUUID());
        entity.setMotif(request.getMotif().trim());
        entity.setDateSortie(request.getDateSortie());
        entity.setTypeStructure(typeStructure);
        entity.setDirection(direction);
        entity.setService(service);
        entity.setAgence(agence);
        entity.setDemandeur(demandeur);
        entity.setValidateur(superieur);
        entity.setStatutValidation(EAutorisationSortieStatut.EN_ATTENTE);
        return mapper.maps(autorisationSortieRepository.save(entity));
    }

    @Override
    public List<BaAutorisationSortieDto> getMesDemandes() {
        BaUser current = currentUserEntity();
        return autorisationSortieRepository.findByDemandeurIdAndStatutOrderByCreatedDateDesc(current.getId(), EStatut.A)
                .stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public List<BaAutorisationSortieDto> getDemandesAValider() {
        BaUser current = currentUserEntity();
        return autorisationSortieRepository
                .findByStatutAndStatutValidationOrderByCreatedDateDesc(EStatut.A, EAutorisationSortieStatut.EN_ATTENTE)
                .stream()
                .filter(demande -> canValidateDemande(current, demande))
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public BaAutorisationSortieDto validerDemande(final String id, final String commentaire) {
        BaAutorisationSortie demande = getDemandeAValiderById(id);
        BaUser validateur = currentUserEntity();
        ZonedDateTime now = ZonedDateTime.now();
        String commentaireValidation = BaUtils.isEmpty(commentaire)
                ? "Valide par " + buildNomComplet(validateur) + " a la date " + now.format(VALIDATION_DTF)
                : commentaire.trim();
        demande.setStatutValidation(EAutorisationSortieStatut.VALIDEE);
        demande.setCommentaireValidation(commentaireValidation);
        demande.setDateValidation(now);
        demande.setValidateur(validateur);
        return mapper.maps(autorisationSortieRepository.save(demande));
    }

    @Override
    public BaAutorisationSortieDto rejeterDemande(final String id, final String commentaire) {
        if (BaUtils.isEmpty(commentaire)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le commentaire de rejet est obligatoire.");
        }
        BaAutorisationSortie demande = getDemandeAValiderById(id);
        demande.setStatutValidation(EAutorisationSortieStatut.REJETEE);
        demande.setCommentaireValidation(commentaire.trim());
        demande.setDateValidation(ZonedDateTime.now());
        demande.setValidateur(currentUserEntity());
        return mapper.maps(autorisationSortieRepository.save(demande));
    }

    @Override
    public BaRhDemandesStatsDto getStatistiques(final BaRhDemandesFilterDto filter) {
        List<BaAutorisationSortie> demandes = filteredDemandes(filter);
        BaRhDemandesStatsDto stats = new BaRhDemandesStatsDto();
        stats.setTotal(demandes.size());
        stats.setEnAttente(countByStatut(demandes, EAutorisationSortieStatut.EN_ATTENTE.name()));
        stats.setValidees(countByStatut(demandes, EAutorisationSortieStatut.VALIDEE.name()));
        stats.setRejetees(countByStatut(demandes, EAutorisationSortieStatut.REJETEE.name()));
        stats.setParDepartement(toStatItems(demandes, d -> d.getDemandeur() == null || d.getDemandeur().getDepartement() == null ? "NON_RENSEIGNE" : d.getDemandeur().getDepartement().getId(),
                d -> d.getDemandeur() == null || d.getDemandeur().getDepartement() == null ? "Non renseigne" : d.getDemandeur().getDepartement().getNom(), 10));
        stats.setParService(toStatItems(demandes, d -> d.getDemandeur() == null || d.getDemandeur().getService() == null ? "NON_RENSEIGNE" : d.getDemandeur().getService().getId(),
                d -> d.getDemandeur() == null || d.getDemandeur().getService() == null ? "Non renseigne" : d.getDemandeur().getService().getNom(), 10));
        stats.setParDemandeur(toStatItems(demandes, d -> d.getDemandeur() == null ? "NON_RENSEIGNE" : d.getDemandeur().getId(),
                d -> fullName(d.getDemandeur()), 10));
        stats.setParValidateur(toStatItems(demandes.stream().filter(d -> d.getValidateur() != null).collect(Collectors.toList()),
                d -> d.getValidateur().getId(), d -> fullName(d.getValidateur()), 10));
        stats.setParMois(toStatItems(demandes, d -> d.getDateSortie() == null ? "NON_RENSEIGNE" : d.getDateSortie().withDayOfMonth(1).toString(),
                d -> d.getDateSortie() == null ? "Non renseigne" : d.getDateSortie().getMonth() + " " + d.getDateSortie().getYear(), 12));
        stats.setParTypeStructure(toStatItems(demandes, d -> BaUtils.isEmpty(d.getTypeStructure()) ? "NON_RENSEIGNE" : d.getTypeStructure(),
                d -> BaUtils.isEmpty(d.getTypeStructure()) ? "Non renseigne" : d.getTypeStructure(), 10));
        return stats;
    }

    @Override
    public org.springframework.data.domain.Page<BaAutorisationSortieSuiviDto> getSuivi(
            final BaRhDemandesFilterDto filter,
            final org.springframework.data.domain.Pageable pageable) {
        List<BaAutorisationSortieSuiviDto> rows = filteredDemandes(filter).stream()
                .sorted(Comparator.comparing(BaAutorisationSortie::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSuiviDto)
                .collect(Collectors.toList());
        int total = rows.size();
        int start = Math.min((int) pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        return new org.springframework.data.domain.PageImpl<>(rows.subList(start, end), pageable, total);
    }

    private List<BaAutorisationSortie> filteredDemandes(final BaRhDemandesFilterDto filter) {
        BaUser current = currentUserEntity();
        return autorisationSortieRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .filter(demande -> canViewReporting(current, demande))
                .filter(demande -> matchesFilter(demande, filter))
                .collect(Collectors.toList());
    }

    private boolean canViewReporting(final BaUser current, final BaAutorisationSortie demande) {
        if (current == null || demande == null) {
            return false;
        }
        if (hasRole(current, "BA_ADMIN")) {
            return true;
        }
        return sameUser(current, demande.getDemandeur()) || sameUser(current, demande.getValidateur()) || canValidateDemande(current, demande);
    }

    private boolean matchesFilter(final BaAutorisationSortie demande, final BaRhDemandesFilterDto filter) {
        if (filter == null) {
            return true;
        }
        BaUser demandeur = demande.getDemandeur();
        if (!BaUtils.isEmpty(filter.getIdDemandeur()) && (demandeur == null || !filter.getIdDemandeur().equals(demandeur.getId()))) return false;
        if (!BaUtils.isEmpty(filter.getIdValidateur()) && (demande.getValidateur() == null || !filter.getIdValidateur().equals(demande.getValidateur().getId()))) return false;
        if (!BaUtils.isEmpty(filter.getIdDepartement()) && (demandeur == null || demandeur.getDepartement() == null || !filter.getIdDepartement().equals(demandeur.getDepartement().getId()))) return false;
        if (!BaUtils.isEmpty(filter.getIdService()) && (demandeur == null || demandeur.getService() == null || !filter.getIdService().equals(demandeur.getService().getId()))) return false;
        if (!BaUtils.isEmpty(filter.getStatutValidation()) && (demande.getStatutValidation() == null || !filter.getStatutValidation().equals(demande.getStatutValidation().name()))) return false;
        if (!BaUtils.isEmpty(filter.getTypeStructure()) && !filter.getTypeStructure().equalsIgnoreCase(demande.getTypeStructure())) return false;
        if (filter.getDateDebut() != null && (demande.getDateSortie() == null || demande.getDateSortie().isBefore(filter.getDateDebut()))) return false;
        return filter.getDateFin() == null || (demande.getDateSortie() != null && !demande.getDateSortie().isAfter(filter.getDateFin()));
    }

    private BaAutorisationSortieSuiviDto toSuiviDto(final BaAutorisationSortie demande) {
        BaUser demandeur = demande.getDemandeur();
        BaAutorisationSortieSuiviDto dto = new BaAutorisationSortieSuiviDto();
        dto.setId(demande.getId());
        dto.setMotif(demande.getMotif());
        dto.setDateSortie(demande.getDateSortie());
        dto.setCreatedDate(demande.getCreatedDate());
        dto.setStatutValidation(demande.getStatutValidation() == null ? null : demande.getStatutValidation().name());
        dto.setCommentaireValidation(demande.getCommentaireValidation());
        dto.setDateValidation(demande.getDateValidation());
        dto.setIdDemandeur(demandeur == null ? null : demandeur.getId());
        dto.setNomDemandeur(fullName(demandeur));
        dto.setMatriculeDemandeur(demandeur == null ? null : demandeur.getMatricule());
        dto.setIdDepartement(demandeur == null || demandeur.getDepartement() == null ? null : demandeur.getDepartement().getId());
        dto.setDepartement(demandeur == null || demandeur.getDepartement() == null ? null : demandeur.getDepartement().getNom());
        dto.setIdService(demandeur == null || demandeur.getService() == null ? null : demandeur.getService().getId());
        dto.setService(demandeur == null || demandeur.getService() == null ? null : demandeur.getService().getNom());
        dto.setTypeStructure(demande.getTypeStructure());
        dto.setStructure(resolveStructureName(demande));
        dto.setIdValidateur(demande.getValidateur() == null ? null : demande.getValidateur().getId());
        dto.setValidateur(fullName(demande.getValidateur()));
        return dto;
    }

    private String resolveStructureName(final BaAutorisationSortie demande) {
        if ("SERVICE".equalsIgnoreCase(demande.getTypeStructure()) && demande.getService() != null) return demande.getService().getNom();
        if ("AGENCE".equalsIgnoreCase(demande.getTypeStructure()) && demande.getAgence() != null) return demande.getAgence().getNom();
        return demande.getDirection() == null ? null : demande.getDirection().getNom();
    }

    private long countByStatut(final List<BaAutorisationSortie> demandes, final String statut) {
        return demandes.stream().filter(d -> d.getStatutValidation() != null && statut.equals(d.getStatutValidation().name())).count();
    }

    private <T> List<BaStatItemDto> toStatItems(final List<T> source,
                                                final java.util.function.Function<T, String> idResolver,
                                                final java.util.function.Function<T, String> labelResolver,
                                                final int limit) {
        Map<String, Long> counts = source.stream().collect(Collectors.groupingBy(idResolver, Collectors.counting()));
        Map<String, String> labels = source.stream().collect(Collectors.toMap(idResolver, labelResolver, (a, b) -> a));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new BaStatItemDto(e.getKey(), labels.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    private boolean hasRole(final BaUser user, final String roleCode) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> roleCode.equals(role.getCode()));
    }

    private String fullName(final BaUser user) {
        if (user == null) return null;
        String value = ((user.getNom() == null ? "" : user.getNom()) + " " + (user.getPrenom() == null ? "" : user.getPrenom())).trim();
        return value.isEmpty() ? user.getUsername() : value;
    }

    private BaAutorisationSortie getDemandeAValiderById(final String id) {
        BaUser current = currentUserEntity();
        BaAutorisationSortie demande = autorisationSortieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'autorisation introuvable."));

        if (demande.getStatut() != EStatut.A) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demande inactive.");
        }
        if (demande.getStatutValidation() != EAutorisationSortieStatut.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette demande a deja ete traitee.");
        }
        if (!canValidateDemande(current, demande)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'etes pas autorise a traiter cette demande.");
        }
        return demande;
    }

    private boolean canValidateDemande(final BaUser current, final BaAutorisationSortie demande) {
        if (current == null || demande == null || demande.getDemandeur() == null) {
            return false;
        }
        if (isDirecteurGeneral(current)) {
            return true;
        }

        BaUser demandeur = demande.getDemandeur();
        if (sameUser(current, demandeur)) {
            return false;
        }

        EFonctionEmploye fonctionDemandeur = demandeur.getFonction();
        if (fonctionDemandeur == null) {
            return demande.getValidateur() != null && sameUser(current, demande.getValidateur());
        }

        switch (fonctionDemandeur) {
            case DIRECTEUR:
                return current.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                        || current.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL;
            case CHEF_SERVICE:
                if (current.getFonction() != EFonctionEmploye.DIRECTEUR || !sameDepartement(current, demandeur)) {
                    return false;
                }
                return isDirectSuperior(current, demandeur) || demandeur.getSuperieur() == null;
            case AGENT:
                return isDirectSuperior(current, demandeur)
                        || isSecondarySuperior(current, demandeur);
            default:
                return demande.getValidateur() != null && sameUser(current, demande.getValidateur());
        }
    }

    private boolean sameUser(final BaUser left, final BaUser right) {
        return left != null
                && right != null
                && left.getId() != null
                && left.getId().equals(right.getId());
    }

    private boolean sameDepartement(final BaUser left, final BaUser right) {
        return left != null
                && right != null
                && left.getDepartement() != null
                && right.getDepartement() != null
                && left.getDepartement().getId() != null
                && left.getDepartement().getId().equals(right.getDepartement().getId());
    }

    private boolean isDirectSuperior(final BaUser candidate, final BaUser employe) {
        return employe.getSuperieur() != null && sameUser(candidate, employe.getSuperieur());
    }

    private boolean isSecondarySuperior(final BaUser candidate, final BaUser employe) {
        return employe.getSuperieurSecondaire() != null && sameUser(candidate, employe.getSuperieurSecondaire());
    }

    private String normalizeTypeStructure(final String typeStructure) {
        if (BaUtils.isEmpty(typeStructure)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de structure (DIRECTION, SERVICE, AGENCE) est obligatoire.");
        }
        String normalized = typeStructure.trim().toUpperCase();
        if (!"DIRECTION".equals(normalized) && !"SERVICE".equals(normalized) && !"AGENCE".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de structure invalide.");
        }
        return normalized;
    }

    private BaDepartement resolveDirection(final String idDirection, final BaUser demandeur) {
        if (!BaUtils.isEmpty(idDirection)) {
            return departementRepository.findById(idDirection)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction introuvable."));
        }
        return demandeur.getDepartement();
    }

    private BaService resolveService(final String idService, final BaUser demandeur) {
        if (!BaUtils.isEmpty(idService)) {
            return serviceRepository.findById(idService)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable."));
        }
        return demandeur.getService();
    }

    private BaAgence resolveAgence(final String idAgence, final BaUser demandeur) {
        if (!BaUtils.isEmpty(idAgence)) {
            return agenceRepository.findById(idAgence)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable."));
        }
        return demandeur.getAgence();
    }

    private void validateStructureChoice(final String typeStructure,
                                         final BaDepartement direction,
                                         final BaService service,
                                         final BaAgence agence) {
        if ("DIRECTION".equals(typeStructure) && direction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La direction est obligatoire.");
        }
        if ("SERVICE".equals(typeStructure) && service == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le service est obligatoire.");
        }
        if ("AGENCE".equals(typeStructure) && agence == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'agence est obligatoire.");
        }
    }

    private BaUser resolveSuperieur(final BaUser demandeur,
                                    final String typeStructure,
                                    final BaDepartement direction,
                                    final BaService service,
                                    final BaAgence agence) {
        EFonctionEmploye fonction = demandeur.getFonction();
        if (fonction == EFonctionEmploye.DIRECTEUR_GENERAL) {
            return null;
        }
        if (fonction == EFonctionEmploye.DIRECTEUR) {
            return findFirstByFonctions(EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT, EFonctionEmploye.DIRECTEUR_GENERAL);
        }
        if (fonction == EFonctionEmploye.CHEF_SERVICE) {
            if (direction == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La direction est obligatoire pour un chef de service.");
            }
            return findByFonctionAndDepartement(EFonctionEmploye.DIRECTEUR, direction);
        }
        if ("AGENCE".equals(typeStructure)) {
            if (agence == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'agence est obligatoire.");
            }
            return findByFonctionAndAgence(EFonctionEmploye.CHEF_SERVICE, agence);
        }
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le service est obligatoire pour determiner le superieur.");
        }
        return findByFonctionAndService(EFonctionEmploye.CHEF_SERVICE, service);
    }

    private BaUser findFirstByFonctions(final EFonctionEmploye... fonctions) {
        for (EFonctionEmploye fonction : fonctions) {
            List<BaUser> users = userRepository.findByFonctionAndStatut(fonction, EStatut.A);
            if (users != null && !users.isEmpty()) {
                return users.get(0);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun superieur hierarchique n'est defini.");
    }

    private BaUser findByFonctionAndDepartement(final EFonctionEmploye fonction, final BaDepartement departement) {
        List<BaUser> users = userRepository.findByFonctionAndDepartementIdAndStatut(fonction, departement.getId(), EStatut.A);
        if (users == null || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun directeur de departement trouve.");
        }
        return users.get(0);
    }

    private BaUser findByFonctionAndService(final EFonctionEmploye fonction, final BaService service) {
        List<BaUser> users = userRepository.findByFonctionAndServiceIdAndStatut(fonction, service.getId(), EStatut.A);
        if (users == null || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun chef de service trouve.");
        }
        return users.get(0);
    }

    private BaUser findByFonctionAndAgence(final EFonctionEmploye fonction, final BaAgence agence) {
        List<BaUser> users = userRepository.findByFonctionAndAgenceIdAndStatut(fonction, agence.getId(), EStatut.A);
        if (users == null || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun chef d'agence trouve.");
        }
        return users.get(0);
    }

    private BaUser currentUserEntity() {
        BaUserDto currentUser = userService.getUserInfoWithMoreDetails();
        if (currentUser == null || BaUtils.isEmpty(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur connecte introuvable.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur connecte introuvable."));
    }

    private boolean isDirecteurGeneral(final BaUser currentUserEntity) {
        if (currentUserEntity != null && currentUserEntity.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL) {
            return true;
        }
        BaUserDto current = userService.getUserInfoWithMoreDetails();
        return current != null && current.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL;
    }

    private String buildNomComplet(final BaUser user) {
        if (user == null) {
            return "Superieur hierarchique";
        }
        String nom = user.getNom() == null ? "" : user.getNom().trim();
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String full = (nom + " " + prenom).trim();
        return full.isEmpty() ? "Superieur hierarchique" : full;
    }

}

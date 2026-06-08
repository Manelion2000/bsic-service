package com.bakouan.app.service.impl;

import com.bakouan.app.dto.BaRepriseServiceDto;
import com.bakouan.app.dto.BaRepriseServiceRequestDto;
import com.bakouan.app.dto.BaRepriseServiceSuiviDto;
import com.bakouan.app.dto.BaRhDemandesFilterDto;
import com.bakouan.app.dto.BaRhDemandesStatsDto;
import com.bakouan.app.dto.BaStatItemDto;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.ERepriseServiceStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaRepriseService;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaRepriseServiceRepository;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.service.BaRepriseServiceManager;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BaRepriseServiceManagerImpl implements BaRepriseServiceManager {
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);
    private final BaRepriseServiceRepository repriseServiceRepository;
    private final BaUserRepository userRepository;
    private final BaUserService userService;

    @Override
    public BaRepriseServiceDto createDemande(final BaRepriseServiceRequestDto request) {
        validateRequest(request);
        BaUser demandeur = currentUserEntity();
        List<BaUser> validateurs = resolveValidateurs(demandeur);

        BaRepriseService entity = new BaRepriseService();
        entity.setId(BaUtils.randomUUID());
        entity.setDemandeur(demandeur);
        // Le signataire provisoire satisfait le schema existant. Il sera remplace
        // par le validateur effectif lors de la validation ou du rejet.
        entity.setSignataire(validateurs.get(0));
        entity.setMotifAbsence(request.getMotifAbsence().trim());
        entity.setDateDebutConge(request.getDateDebutConge());
        entity.setDateFinConge(request.getDateFinConge());
        entity.setDateReprise(request.getDateReprise());
        entity.setStatutValidation(ERepriseServiceStatut.EN_ATTENTE);
        return mapper.maps(repriseServiceRepository.save(entity));
    }

    @Override
    public List<BaRepriseServiceDto> getMesDemandes() {
        return repriseServiceRepository
                .findByDemandeurIdAndStatutOrderByCreatedDateDesc(currentUserEntity().getId(), EStatut.A)
                .stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public List<BaRepriseServiceDto> getDemandesAValider() {
        BaUser current = currentUserEntity();
        return repriseServiceRepository
                .findByStatutAndStatutValidationOrderByCreatedDateDesc(EStatut.A, ERepriseServiceStatut.EN_ATTENTE)
                .stream()
                .filter(demande -> canValidate(current, demande.getDemandeur()))
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public BaRepriseServiceDto getById(final String id) {
        BaRepriseService demande = findById(id);
        BaUser current = currentUserEntity();
        boolean peutConsulterCommeValidateur = demande.getStatutValidation() == ERepriseServiceStatut.EN_ATTENTE
                && canValidate(current, demande.getDemandeur());
        if (!sameUser(current, demande.getDemandeur())
                && !sameUser(current, demande.getSignataire())
                && !peutConsulterCommeValidateur) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'etes pas autorise a consulter cette demande.");
        }
        return mapper.maps(demande);
    }

    @Override
    public BaRepriseServiceDto valider(final String id, final String commentaire) {
        BaRepriseService demande = getPendingDemandeForCurrentSignataire(id);
        demande.setSignataire(currentUserEntity());
        demande.setStatutValidation(ERepriseServiceStatut.VALIDEE);
        demande.setCommentaireValidation(BaUtils.isEmpty(commentaire) ? null : commentaire.trim());
        demande.setDateValidation(ZonedDateTime.now());
        return mapper.maps(repriseServiceRepository.save(demande));
    }

    @Override
    public BaRepriseServiceDto rejeter(final String id, final String commentaire) {
        if (BaUtils.isEmpty(commentaire)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le commentaire de rejet est obligatoire.");
        }
        BaRepriseService demande = getPendingDemandeForCurrentSignataire(id);
        demande.setSignataire(currentUserEntity());
        demande.setStatutValidation(ERepriseServiceStatut.REJETEE);
        demande.setCommentaireValidation(commentaire.trim());
        demande.setDateValidation(ZonedDateTime.now());
        return mapper.maps(repriseServiceRepository.save(demande));
    }

    @Override
    public BaRhDemandesStatsDto getStatistiques(final BaRhDemandesFilterDto filter) {
        List<BaRepriseService> demandes = filteredDemandes(filter);
        BaRhDemandesStatsDto stats = new BaRhDemandesStatsDto();
        stats.setTotal(demandes.size());
        stats.setEnAttente(countByStatut(demandes, ERepriseServiceStatut.EN_ATTENTE.name()));
        stats.setValidees(countByStatut(demandes, ERepriseServiceStatut.VALIDEE.name()));
        stats.setRejetees(countByStatut(demandes, ERepriseServiceStatut.REJETEE.name()));
        stats.setParDepartement(toStatItems(demandes, d -> d.getDemandeur() == null || d.getDemandeur().getDepartement() == null ? "NON_RENSEIGNE" : d.getDemandeur().getDepartement().getId(),
                d -> d.getDemandeur() == null || d.getDemandeur().getDepartement() == null ? "Non renseigne" : d.getDemandeur().getDepartement().getNom(), 10));
        stats.setParService(toStatItems(demandes, d -> d.getDemandeur() == null || d.getDemandeur().getService() == null ? "NON_RENSEIGNE" : d.getDemandeur().getService().getId(),
                d -> d.getDemandeur() == null || d.getDemandeur().getService() == null ? "Non renseigne" : d.getDemandeur().getService().getNom(), 10));
        stats.setParDemandeur(toStatItems(demandes, d -> d.getDemandeur() == null ? "NON_RENSEIGNE" : d.getDemandeur().getId(),
                d -> fullName(d.getDemandeur()), 10));
        stats.setParValidateur(toStatItems(demandes.stream().filter(d -> d.getSignataire() != null).collect(Collectors.toList()),
                d -> d.getSignataire().getId(), d -> fullName(d.getSignataire()), 10));
        stats.setParMois(toStatItems(demandes, d -> d.getDateReprise() == null ? "NON_RENSEIGNE" : d.getDateReprise().withDayOfMonth(1).toString(),
                d -> d.getDateReprise() == null ? "Non renseigne" : d.getDateReprise().getMonth() + " " + d.getDateReprise().getYear(), 12));
        return stats;
    }

    @Override
    public org.springframework.data.domain.Page<BaRepriseServiceSuiviDto> getSuivi(
            final BaRhDemandesFilterDto filter,
            final org.springframework.data.domain.Pageable pageable) {
        List<BaRepriseServiceSuiviDto> rows = filteredDemandes(filter).stream()
                .sorted(Comparator.comparing(BaRepriseService::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSuiviDto)
                .collect(Collectors.toList());
        int total = rows.size();
        int start = Math.min((int) pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        return new org.springframework.data.domain.PageImpl<>(rows.subList(start, end), pageable, total);
    }

    private List<BaRepriseService> filteredDemandes(final BaRhDemandesFilterDto filter) {
        BaUser current = currentUserEntity();
        return repriseServiceRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .filter(demande -> canViewReporting(current, demande))
                .filter(demande -> matchesFilter(demande, filter))
                .collect(Collectors.toList());
    }

    private boolean canViewReporting(final BaUser current, final BaRepriseService demande) {
        if (current == null || demande == null) {
            return false;
        }
        if (hasRole(current, "BA_ADMIN")) {
            return true;
        }
        return sameUser(current, demande.getDemandeur())
                || sameUser(current, demande.getSignataire())
                || canValidate(current, demande.getDemandeur());
    }

    private boolean matchesFilter(final BaRepriseService demande, final BaRhDemandesFilterDto filter) {
        if (filter == null) {
            return true;
        }
        BaUser demandeur = demande.getDemandeur();
        if (!BaUtils.isEmpty(filter.getIdDemandeur()) && (demandeur == null || !filter.getIdDemandeur().equals(demandeur.getId()))) return false;
        if (!BaUtils.isEmpty(filter.getIdValidateur()) && (demande.getSignataire() == null || !filter.getIdValidateur().equals(demande.getSignataire().getId()))) return false;
        if (!BaUtils.isEmpty(filter.getIdDepartement()) && (demandeur == null || demandeur.getDepartement() == null || !filter.getIdDepartement().equals(demandeur.getDepartement().getId()))) return false;
        if (!BaUtils.isEmpty(filter.getIdService()) && (demandeur == null || demandeur.getService() == null || !filter.getIdService().equals(demandeur.getService().getId()))) return false;
        if (!BaUtils.isEmpty(filter.getStatutValidation()) && (demande.getStatutValidation() == null || !filter.getStatutValidation().equals(demande.getStatutValidation().name()))) return false;
        if (filter.getDateDebut() != null && (demande.getDateReprise() == null || demande.getDateReprise().isBefore(filter.getDateDebut()))) return false;
        return filter.getDateFin() == null || (demande.getDateReprise() != null && !demande.getDateReprise().isAfter(filter.getDateFin()));
    }

    private BaRepriseServiceSuiviDto toSuiviDto(final BaRepriseService demande) {
        BaUser demandeur = demande.getDemandeur();
        BaRepriseServiceSuiviDto dto = new BaRepriseServiceSuiviDto();
        dto.setId(demande.getId());
        dto.setMotifAbsence(demande.getMotifAbsence());
        dto.setDateDebutConge(demande.getDateDebutConge());
        dto.setDateFinConge(demande.getDateFinConge());
        dto.setDateReprise(demande.getDateReprise());
        dto.setNombreJoursAbsence(demande.getDateDebutConge() == null || demande.getDateFinConge() == null ? 0
                : ChronoUnit.DAYS.between(demande.getDateDebutConge(), demande.getDateFinConge()) + 1);
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
        dto.setIdSignataire(demande.getSignataire() == null ? null : demande.getSignataire().getId());
        dto.setSignataire(fullName(demande.getSignataire()));
        return dto;
    }

    private long countByStatut(final List<BaRepriseService> demandes, final String statut) {
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

    private void validateRequest(final BaRepriseServiceRequestDto request) {
        if (request == null || BaUtils.isEmpty(request.getMotifAbsence())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif d'absence est obligatoire.");
        }
        if (request.getDateDebutConge() == null || request.getDateFinConge() == null || request.getDateReprise() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les dates de conge/absence et de reprise sont obligatoires.");
        }
        if (request.getDateFinConge().isBefore(request.getDateDebutConge())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin de conge/absence doit suivre la date de debut.");
        }
        if (request.getDateReprise().isBefore(request.getDateFinConge())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de reprise ne peut pas preceder la fin du conge.");
        }
        if (request.getDateReprise().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reprise de service ne peut pas etre declaree dans le futur.");
        }
    }

    private List<BaUser> resolveValidateurs(final BaUser demandeur) {
        List<BaUser> validateurs = userRepository.findByStatut(EStatut.A).stream()
                .filter(validateur -> canValidate(validateur, demandeur))
                .collect(Collectors.toList());
        if (validateurs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucun validateur hierarchique n'est defini pour cet utilisateur.");
        }
        return validateurs;
    }

    private boolean canValidate(final BaUser validateur, final BaUser demandeur) {
        if (validateur == null || demandeur == null || sameUser(validateur, demandeur)
                || validateur.getFonction() == null || demandeur.getFonction() == null) {
            return false;
        }
        return switch (demandeur.getFonction()) {
            case AGENT -> (validateur.getFonction() == EFonctionEmploye.CHEF_SERVICE
                    && sameStructure(validateur.getService(), demandeur.getService()))
                    || (validateur.getFonction() == EFonctionEmploye.DIRECTEUR
                    && sameStructure(validateur.getDepartement(), demandeur.getDepartement()));
            case CHEF_SERVICE -> validateur.getFonction() == EFonctionEmploye.DIRECTEUR
                    && sameStructure(validateur.getDepartement(), demandeur.getDepartement());
            case DIRECTEUR -> validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL
                    || validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT;
            case DIRECTEUR_GENERAL_ADJOINT -> validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL
                    || validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT;
            case DIRECTEUR_GENERAL -> validateur.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT;
        };
    }

    private boolean sameStructure(final Object left, final Object right) {
        if (left instanceof com.bakouan.app.model.BaService leftService
                && right instanceof com.bakouan.app.model.BaService rightService) {
            return leftService.getId() != null && leftService.getId().equals(rightService.getId());
        }
        if (left instanceof com.bakouan.app.model.BaDepartement leftDepartement
                && right instanceof com.bakouan.app.model.BaDepartement rightDepartement) {
            return leftDepartement.getId() != null && leftDepartement.getId().equals(rightDepartement.getId());
        }
        return false;
    }

    private BaRepriseService getPendingDemandeForCurrentSignataire(final String id) {
        BaRepriseService demande = findById(id);
        if (demande.getStatut() != EStatut.A || demande.getStatutValidation() != ERepriseServiceStatut.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette demande a deja ete traitee ou est inactive.");
        }
        if (!canValidate(currentUserEntity(), demande.getDemandeur())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'etes pas autorise a traiter cette demande.");
        }
        return demande;
    }

    private BaRepriseService findById(final String id) {
        return repriseServiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de reprise introuvable."));
    }

    private BaUser currentUserEntity() {
        BaUserDto current = userService.getUserInfoWithMoreDetails();
        if (current == null || BaUtils.isEmpty(current.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur connecte introuvable.");
        }
        return userRepository.findById(current.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur connecte introuvable."));
    }

    private boolean sameUser(final BaUser left, final BaUser right) {
        return left != null && right != null && left.getId() != null && left.getId().equals(right.getId());
    }
}

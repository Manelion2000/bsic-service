package com.bakouan.app.service.impl;

import com.bakouan.app.dto.*;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.service.*;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BaAutorisationAbsenceServiceImpl implements BaAutorisationAbsenceService {
    private final BaAutorisationAbsenceRepository repository;
    private final BaUserRepository userRepository;
    private final BaUserService userService;
    private final RhCircuitService circuitService;
    private final CongePlanningService congePlanning;

    @Override
    public BaAutorisationAbsenceDto create(final BaAutorisationAbsenceRequestDto request) {
        validateDates(request.getDateDebut(), request.getDateFin());
        BaUser demandeur = currentUser();
        // Serialiser aussi les soumissions du meme agent avant le controle de chevauchement.
        demandeur = userRepository.findLockedById(demandeur.getId()).orElseThrow(() -> badRequest("Demandeur introuvable."));
        congePlanning.guardOtherSubmission(demandeur,request.getDateDebut(),request.getDateFin());
        boolean overlap = repository.existsByDemandeurIdAndStatutAndStatutValidationInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                demandeur.getId(), EStatut.A, List.of(EAbsenceStatut.EN_ATTENTE, EAbsenceStatut.VALIDEE),
                request.getDateFin(), request.getDateDebut());
        if (overlap) throw badRequest("Une demande en attente ou validee couvre deja tout ou partie de cette periode.");

        BaAutorisationAbsence entity = new BaAutorisationAbsence();
        entity.setMotif(request.getMotif().trim());
        entity.setDateDebut(request.getDateDebut());
        entity.setDateFin(request.getDateFin());
        entity.setNombreJours((int) ChronoUnit.DAYS.between(request.getDateDebut(), request.getDateFin()) + 1);
        entity.setDemandeur(demandeur);
        entity.setCircuit(circuitService.freezeFor("ABSENCE",demandeur));
        return toDto(repository.save(entity));
    }

    @Override public List<BaAutorisationAbsenceDto> mesDemandes() {
        return repository.findByDemandeurIdAndStatutOrderByCreatedDateDesc(currentUser().getId(), EStatut.A).stream().map(this::toDto).toList();
    }

    @Override public List<BaAutorisationAbsenceDto> aValider() {
        BaUser current = currentUser();
        return repository.findByStatutAndStatutValidationOrderByCreatedDateDesc(EStatut.A, EAbsenceStatut.EN_ATTENTE).stream()
                .filter(d -> circuitService.canDecide(d.getCircuit(), current)).map(this::toDto).toList();
    }

    @Override public List<BaAutorisationAbsenceDto> suivi(final LocalDate debut, final LocalDate fin) {
        BaUser current = currentUser();
        requireReportingAccess(current);
        return filtered(debut, fin).stream().filter(d -> canSee(current, d)).map(this::toDto).toList();
    }

    @Override public BaAbsenceStatsDto statistiques(final LocalDate debut, final LocalDate fin) {
        BaUser current = currentUser();
        requireReportingAccess(current);
        List<BaAutorisationAbsence> values = filtered(debut, fin).stream().filter(d -> canSee(current, d)).toList();
        BaAbsenceStatsDto dto = new BaAbsenceStatsDto();
        dto.setTotalDemandes(values.size());
        dto.setTotalJours(values.stream().filter(this::isValidated).mapToLong(BaAutorisationAbsence::getNombreJours).sum());
        dto.setEnAttente(count(values, EAbsenceStatut.EN_ATTENTE));
        dto.setValidees(count(values, EAbsenceStatut.VALIDEE));
        dto.setRejetees(count(values, EAbsenceStatut.REJETEE));
        dto.setParMois(group(values, d -> YearMonth.from(d.getDateDebut()).toString()));
        dto.setParAnnee(group(values, d -> Integer.toString(d.getDateDebut().getYear())));
        return dto;
    }

    @Override public BaAutorisationAbsenceDto valider(final String id, final int etape, final String commentaire) {
        return decide(id, etape, true, commentaire);
    }

    @Override public BaAutorisationAbsenceDto rejeter(final String id, final int etape, final String commentaire) {
        return decide(id, etape, false, commentaire);
    }

    @Override public BaAutorisationAbsenceDto annuler(final String id) {
        BaAutorisationAbsence entity = getLocked(id);
        if (!sameUser(currentUser(), entity.getDemandeur())) throw forbidden("Seul le demandeur peut annuler cette demande.");
        if (entity.getStatutValidation() != EAbsenceStatut.EN_ATTENTE) throw badRequest("Seule une demande en attente peut etre annulee.");
        entity.setStatutValidation(EAbsenceStatut.ANNULEE);
        entity.setDateValidation(ZonedDateTime.now());
        entity.setCommentaireValidation("Annulee par le demandeur");
        circuitService.cancel(entity.getCircuit(), currentUser());
        return toDto(repository.save(entity));
    }

    private BaAutorisationAbsenceDto decide(String id, int etape, boolean approve, String comment) {
        BaAutorisationAbsence entity = getLocked(id);
        BaUser current = currentUser();
        if (entity.getStatut() != EStatut.A || entity.getStatutValidation() != EAbsenceStatut.EN_ATTENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a deja ete traitee ou est inactive.");
        String status = circuitService.decide(entity.getCircuit(), current, etape, approve, comment == null ? null : comment.trim());
        if("VALIDEE".equals(status)){userRepository.findLockedById(entity.getDemandeur().getId()).orElseThrow();congePlanning.assertOtherFinal(entity.getDemandeur(),entity.getDateDebut(),entity.getDateFin(),entity.getId());}
        entity.setStatutValidation(EAbsenceStatut.valueOf(status));
        if (!"EN_ATTENTE".equals(status)) {
            entity.setValidateur(current);
            entity.setDateValidation(ZonedDateTime.now());
            entity.setCommentaireValidation(comment);
        }
        return toDto(repository.save(entity));
    }

    private List<BaAutorisationAbsence> filtered(LocalDate debut, LocalDate fin) {
        return repository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .filter(d -> debut == null || !d.getDateFin().isBefore(debut))
                .filter(d -> fin == null || !d.getDateDebut().isAfter(fin)).toList();
    }

    private boolean canSee(BaUser current, BaAutorisationAbsence absence) {
        return hasRole(current, "BA_ADMIN") || hasRole(current, "RH_CONSULTATION_ABSENCES") || isDga(current)
                || circuitService.participant(absence.getCircuit(), current) || sameUser(current, absence.getDemandeur());
    }

    private void requireReportingAccess(BaUser user) {
        if (!(hasRole(user, "BA_ADMIN") || hasRole(user, "RH_CONSULTATION_ABSENCES") || isDga(user)))
            throw forbidden("Le suivi consolide est reserve aux RH, DGA et administrateurs.");
    }

    private boolean isDga(BaUser user) { return user.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT || user.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL; }
    private boolean hasRole(BaUser user, String code) { return user.getRoles() != null && user.getRoles().stream().anyMatch(r -> code.equals(r.getCode())); }
    private boolean sameUser(BaUser a, BaUser b) { return a != null && b != null && Objects.equals(a.getId(), b.getId()); }
    private boolean isValidated(BaAutorisationAbsence d) { return d.getStatutValidation() == EAbsenceStatut.VALIDEE; }
    private long count(List<BaAutorisationAbsence> list, EAbsenceStatut s) { return list.stream().filter(d -> d.getStatutValidation() == s).count(); }

    private List<BaAbsenceStatsDto.Periode> group(List<BaAutorisationAbsence> values, Function<BaAutorisationAbsence, String> key) {
        return values.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList())).entrySet().stream()
                .map(e -> new BaAbsenceStatsDto.Periode(e.getKey(), e.getValue().size(), e.getValue().stream().filter(this::isValidated).mapToLong(BaAutorisationAbsence::getNombreJours).sum()))
                .sorted(Comparator.comparing(BaAbsenceStatsDto.Periode::getPeriode).reversed()).toList();
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) throw badRequest("Les dates de debut et de fin sont obligatoires.");
        if (end.isBefore(start)) throw badRequest("La date de fin doit etre posterieure ou egale a la date de debut.");
        if (start.isBefore(LocalDate.now())) throw badRequest("Une absence ne peut pas commencer dans le passe.");
        if (ChronoUnit.DAYS.between(start, end) > 365) throw badRequest("Une absence ne peut pas depasser 366 jours.");
    }

    private BaUser currentUser() {
        BaUserDto dto = userService.getUserInfoWithMoreDetails();
        if (dto == null || BaUtils.isEmpty(dto.getId())) throw badRequest("Utilisateur connecte introuvable.");
        return userRepository.findById(dto.getId()).orElseThrow(() -> badRequest("Utilisateur connecte introuvable."));
    }

    private BaAutorisationAbsence getLocked(String id) { return repository.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'absence introuvable.")); }
    private ResponseStatusException badRequest(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, text); }
    private ResponseStatusException forbidden(String text) { return new ResponseStatusException(HttpStatus.FORBIDDEN, text); }

    private BaAutorisationAbsenceDto toDto(BaAutorisationAbsence e) {
        BaAutorisationAbsenceDto d = new BaAutorisationAbsenceDto();
        d.setCircuit(circuitService.toDto(e.getCircuit(), currentUser()));
        d.setId(e.getId()); d.setMotif(e.getMotif()); d.setDateDebut(e.getDateDebut()); d.setDateFin(e.getDateFin());
        d.setNombreJours(e.getNombreJours()); d.setStatutValidation(e.getStatutValidation());
        d.setCommentaireValidation(e.getCommentaireValidation()); d.setDateValidation(e.getDateValidation());
        d.setIdDemandeur(e.getDemandeur().getId()); d.setMatriculeDemandeur(e.getDemandeur().getMatricule());
        d.setNomCompletDemandeur(fullName(e.getDemandeur()));
        d.setDepartement(e.getDemandeur().getDepartement() == null ? null : e.getDemandeur().getDepartement().getNom());
        d.setService(e.getDemandeur().getService() == null ? null : e.getDemandeur().getService().getNom());
        if (e.getValidateur() != null) { d.setIdValidateur(e.getValidateur().getId()); d.setNomCompletValidateur(fullName(e.getValidateur())); }
        return d;
    }
    private String fullName(BaUser u) { return ((u.getNom() == null ? "" : u.getNom()) + " " + (u.getPrenom() == null ? "" : u.getPrenom())).trim(); }
}

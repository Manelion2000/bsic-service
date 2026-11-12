package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import com.bakouan.app.enums.*;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.security.BaUserService;
import com.bakouan.app.utils.BaUtils;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class DirectionGeneraleService {
    private final BaUserService userService;
    private final BaUserRepository users;
    private final BaRoleRepository roles;
    private final BaDgaPoleValidateurRepository assignments;
    private final BaLogService logService;
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);

    public List<BaUserDto> getDg() { return getUsers(EFonctionEmploye.DIRECTEUR_GENERAL); }
    public List<BaUserDto> getDga() { return getUsers(EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT); }
    private List<BaUserDto> getUsers(EFonctionEmploye function) {
        userService.requireAdministrator();
        return users.findByStatut(EStatut.A).stream().filter(u -> u.getFonction() == function).map(mapper::maps).toList();
    }

    public List<BaDgaPoleValidateurDto> getAssignments() {
        userService.requireAdministrator();
        return assignments.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().map(this::toDto).toList();
    }

    public BaUserDto createDg(BaDirectionGeneraleRequestDto request) {
        userService.requireAdministrator();
        if (request.getPole() != null || request.getDateDebut() != null || request.getDateFin() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le DG n'est pas rattache a un pole DGA.");
        return userService.createUser(identity(request, EFonctionEmploye.DIRECTEUR_GENERAL, "BA_DG"));
    }

    /** Le compte et son affectation sont crees dans la meme transaction. */
    public BaUserDto createDga(BaDirectionGeneraleRequestDto request) {
        userService.requireAdministrator();
        userService.lockDirectionGenerale();
        LocalDate start = request.getDateDebut() == null ? LocalDate.now() : request.getDateDebut();
        validatePeriod(request.getPole(), start, request.getDateFin());
        validateNoOverlap(null, request.getPole(), null, start, request.getDateFin());
        BaUserDto created = userService.createUser(identity(request, EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT, "BA_DGA"));
        BaDgaPoleValidateurDto assignment = new BaDgaPoleValidateurDto();
        assignment.setPole(request.getPole()); assignment.setIdDga(created.getId());
        assignment.setDateDebut(start); assignment.setDateFin(request.getDateFin()); assignment.setActif(true);
        saveAssignment(null, assignment);
        return created;
    }

    private BaUserDto identity(BaDirectionGeneraleRequestDto request, EFonctionEmploye function, String roleCode) {
        BaUserDto dto = new BaUserDto();
        dto.setNom(request.getNom()); dto.setPrenom(request.getPrenom()); dto.setEmail(request.getEmail().trim());
        dto.setTelephone(request.getTelephone().trim()); dto.setSexe(request.getSexe());
        String matricule = request.getMatricule() == null ? null : request.getMatricule().trim();
        if (matricule != null && matricule.isEmpty()) matricule = null;
        if (matricule != null && users.existsByMatricule(matricule))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le matricule est deja utilise.");
        dto.setMatricule(matricule);
        dto.setFonction(function); dto.setLocked(false);
        Set<BaRoleDto> rights = new HashSet<>();
        for (String code : List.of("BA_CONNECT", roleCode)) {
            BaRole role = roles.findByCodeAndStatut(code, EStatut.A).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Role " + code + " absent ou inactif. Verifiez les migrations Flyway."));
            rights.add(mapper.maps(role));
        }
        dto.setRoles(rights);
        return dto;
    }

    public BaDgaPoleValidateurDto saveAssignment(String id, BaDgaPoleValidateurDto request) {
        userService.requireAdministrator();
        userService.lockDirectionGenerale();
        validatePeriod(request.getPole(), request.getDateDebut(), request.getDateFin());
        BaUser user = users.findById(Objects.toString(request.getIdDga(), "")).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "DGA introuvable."));
        if (user.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT || !available(user))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez un compte actif dont la fonction est Directeur general adjoint.");
        boolean active = !Boolean.FALSE.equals(request.getActif());
        if (active) validateNoOverlap(id, request.getPole(), user.getId(), request.getDateDebut(), request.getDateFin());
        BaDgaPoleValidateur entity = id == null ? new BaDgaPoleValidateur() : assignments.findById(id)
                .filter(a -> a.getStatut() == EStatut.A).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation introuvable."));
        entity.setDga(user); entity.setPole(request.getPole()); entity.setDateDebut(request.getDateDebut());
        entity.setDateFin(request.getDateFin()); entity.setActif(active);
        logService.log(new BaLogDto(id == null ? EAction.C : EAction.U, "Affectation DGA " + entity.getPole() + " : " + user.getId()));
        return toDto(assignments.save(entity));
    }

    public void deactivateAssignment(String id) {
        userService.requireAdministrator();
        userService.lockDirectionGenerale();
        BaDgaPoleValidateur entity = assignments.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation introuvable."));
        entity.setActif(false); assignments.save(entity);
        logService.log(new BaLogDto(EAction.D, "Desactivation affectation DGA " + id));
    }

    private void validatePeriod(EDgaPole pole, LocalDate start, LocalDate end) {
        if (pole == null || start == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pole et date de debut obligatoires.");
        if (end != null && end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit suivre la date de debut.");
    }

    private void validateNoOverlap(String id, EDgaPole pole, String userId, LocalDate start, LocalDate end) {
        for (BaDgaPoleValidateur other : assignments.findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut.A)) {
            if (Objects.equals(id, other.getId())) continue;
            boolean overlap = (end == null || !other.getDateDebut().isAfter(end))
                    && (other.getDateFin() == null || !start.isAfter(other.getDateFin()));
            if (overlap && other.getPole() == pole)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un DGA est deja affecte a ce pole sur cette periode. Modifiez ou desactivez cette affectation avant son remplacement.");
            if (overlap && userId != null && other.getDga() != null && userId.equals(other.getDga().getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un meme DGA ne peut pas occuper les deux poles sur la meme periode.");
        }
    }

    private boolean available(BaUser user) {
        return user.getStatut() == EStatut.A && Boolean.TRUE.equals(user.getActivated()) && !Boolean.TRUE.equals(user.getLocked());
    }

    private BaDgaPoleValidateurDto toDto(BaDgaPoleValidateur entity) {
        BaDgaPoleValidateurDto dto = new BaDgaPoleValidateurDto();
        dto.setId(entity.getId()); dto.setPole(entity.getPole()); dto.setIdDga(entity.getDga().getId());
        dto.setNomDga((Objects.toString(entity.getDga().getNom(), "") + " " + Objects.toString(entity.getDga().getPrenom(), "")).trim());
        dto.setDateDebut(entity.getDateDebut()); dto.setDateFin(entity.getDateFin()); dto.setActif(entity.getActif());
        return dto;
    }
}

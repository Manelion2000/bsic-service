package com.bakouan.app.service;

import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.security.BaUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor
public class CongeSecurity {
    private final BaUserService userService;
    private final BaUserRepository users;
    public BaUser current() {
        BaUser user = users.findById(userService.getUserInfoWithMoreDetails().getId()).orElseThrow(() -> forbidden("Compte introuvable."));
        if (!available(user)) throw forbidden("Compte indisponible.");
        return user;
    }
    public BaUser require(String permission) { BaUser actor = current(); require(actor, permission); return actor; }
    public void require(BaUser actor, String permission) {
        if (!has(actor, permission)) throw forbidden("Permission requise : " + permission + ".");
    }
    public boolean has(BaUser actor, String permission) {
        if (actor == null) return false;
        List<BaRole> roles = new ArrayList<>(actor.getRoles());
        if (actor.getProfil() != null && actor.getProfil().getRoles() != null) roles.addAll(actor.getProfil().getRoles());
        return roles.stream().anyMatch(r -> r.getStatut() == EStatut.A && permission.equals(r.getCode()));
    }
    public boolean global(BaUser actor) {
        return has(actor,"CONGES_DROITS") || has(actor,"CONGES_AUDITER");
    }
    public boolean sameDepartment(BaUser actor, BaUser target) {
        String a = departmentId(actor), b = departmentId(target);
        return a != null && a.equals(b);
    }
    public boolean planningScope(BaUser actor, BaUser target) {
        if (Objects.equals(actor.getId(),target.getId()) || global(actor)) return true;
        if (!has(actor,"CONGES_PLANNING")) return false;
        return actor.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL || actor.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                || ((actor.getFonction() == EFonctionEmploye.DIRECTEUR || actor.getFonction() == EFonctionEmploye.CHEF_SERVICE) && sameDepartment(actor,target));
    }
    public static String departmentId(BaUser user) {
        return user.getDepartement() != null ? user.getDepartement().getId()
                : user.getService() != null && user.getService().getDepartement() != null ? user.getService().getDepartement().getId() : null;
    }
    public static boolean available(BaUser user) {
        return user != null && user.getStatut() == EStatut.A && Boolean.TRUE.equals(user.getActivated()) && !Boolean.TRUE.equals(user.getLocked());
    }
    public static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    public static ResponseStatusException blocked(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    public static ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN,message); }
    public static ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND,message); }
}

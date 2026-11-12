package com.bakouan.app.service;

import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaDepartement;
import com.bakouan.app.model.BaUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Component
public class RhValidationPolicy {

    public boolean canValidate(final BaUser validator, final BaUser requester) {
        if (!isUsable(validator) || requester == null || sameUser(validator, requester)
                || validator.getFonction() == null || requester.getFonction() == null) {
            return false;
        }
        return switch (requester.getFonction()) {
            case AGENT, CHEF_SERVICE -> sameDepartment(validator, requester)
                    && (validator.getFonction() == EFonctionEmploye.CHEF_SERVICE
                    || validator.getFonction() == EFonctionEmploye.DIRECTEUR);
            case DIRECTEUR -> validator.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT
                    || validator.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL;
            case DIRECTEUR_GENERAL_ADJOINT -> validator.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL;
            case DIRECTEUR_GENERAL -> validator.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT;
        };
    }

    public List<BaUser> eligibleValidators(final BaUser requester, final List<BaUser> candidates) {
        return candidates.stream()
                .filter(candidate -> canValidate(candidate, requester))
                .sorted(Comparator.comparingInt(this::priority)
                        .thenComparing(user -> user.getNom() == null ? "" : user.getNom())
                        .thenComparing(BaUser::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public BaUser primaryValidator(final BaUser requester, final List<BaUser> candidates) {
        return eligibleValidators(requester, candidates).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Aucun validateur RH eligible n'est configure pour le departement du demandeur."));
    }

    private boolean isUsable(final BaUser user) {
        return user != null && user.getStatut() == EStatut.A && !Boolean.TRUE.equals(user.getLocked());
    }

    private boolean sameUser(final BaUser left, final BaUser right) {
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private boolean sameDepartment(final BaUser left, final BaUser right) {
        String leftId = departmentId(left);
        String rightId = departmentId(right);
        return leftId != null && leftId.equals(rightId);
    }

    private String departmentId(final BaUser user) {
        BaDepartement department = user.getDepartement();
        if (department == null && user.getService() != null) {
            department = user.getService().getDepartement();
        }
        return department == null ? null : department.getId();
    }

    private int priority(final BaUser user) {
        return switch (user.getFonction()) {
            case DIRECTEUR_GENERAL -> 0;
            case DIRECTEUR_GENERAL_ADJOINT -> 1;
            case DIRECTEUR -> 2;
            case CHEF_SERVICE -> 3;
            case AGENT -> 4;
        };
    }
}

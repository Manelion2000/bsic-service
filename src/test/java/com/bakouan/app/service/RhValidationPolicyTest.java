package com.bakouan.app.service;

import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaDepartement;
import com.bakouan.app.model.BaService;
import com.bakouan.app.model.BaUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RhValidationPolicyTest {

    private final RhValidationPolicy policy = new RhValidationPolicy();

    @Test
    void agentCanBeValidatedByAllManagersAndDirectorInDepartment() {
        BaDepartement department = department("DEP-IT");
        BaUser requester = user("agent", EFonctionEmploye.AGENT, department, service("S-OPS", department));
        BaUser otherManager = user("manager", EFonctionEmploye.CHEF_SERVICE, department, service("S-DEV", department));
        BaUser director = user("director", EFonctionEmploye.DIRECTEUR, department, null);

        assertThat(policy.canValidate(otherManager, requester)).isTrue();
        assertThat(policy.canValidate(director, requester)).isTrue();
    }

    @Test
    void managerCanBeValidatedByPeerOrDirectorButNeverSelf() {
        BaDepartement department = department("DEP-IT");
        BaUser requester = user("manager-1", EFonctionEmploye.CHEF_SERVICE, department, service("S-OPS", department));
        BaUser peer = user("manager-2", EFonctionEmploye.CHEF_SERVICE, department, service("S-DEV", department));
        BaUser director = user("director", EFonctionEmploye.DIRECTEUR, department, null);

        assertThat(policy.canValidate(peer, requester)).isTrue();
        assertThat(policy.canValidate(director, requester)).isTrue();
        assertThat(policy.canValidate(requester, requester)).isFalse();
    }

    @Test
    void rejectsOtherDepartmentAndUnavailableValidator() {
        BaDepartement requesterDepartment = department("DEP-IT");
        BaUser requester = user("agent", EFonctionEmploye.AGENT, requesterDepartment, null);
        BaUser foreignManager = user("foreign", EFonctionEmploye.CHEF_SERVICE, department("DEP-RH"), null);
        BaUser localManager = user("local", EFonctionEmploye.CHEF_SERVICE, requesterDepartment, null);

        assertThat(policy.canValidate(foreignManager, requester)).isFalse();
        localManager.setLocked(true);
        assertThat(policy.canValidate(localManager, requester)).isFalse();
        localManager.setLocked(false);
        localManager.setStatut(EStatut.D);
        assertThat(policy.canValidate(localManager, requester)).isFalse();
    }

    private BaUser user(final String id, final EFonctionEmploye function,
                        final BaDepartement department, final BaService service) {
        BaUser user = new BaUser(id);
        user.setFonction(function);
        user.setDepartement(department);
        user.setService(service);
        user.setStatut(EStatut.A);
        user.setLocked(false);
        return user;
    }

    private BaDepartement department(final String id) {
        BaDepartement department = new BaDepartement();
        department.setId(id);
        return department;
    }

    private BaService service(final String id, final BaDepartement department) {
        BaService service = new BaService();
        service.setId(id);
        service.setDepartement(department);
        return service;
    }
}

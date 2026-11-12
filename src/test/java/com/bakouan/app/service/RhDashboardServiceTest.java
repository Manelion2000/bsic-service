package com.bakouan.app.service;

import com.bakouan.app.dto.BaRhDashboardDto;
import com.bakouan.app.dto.BaRoleDto;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.security.BaUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RhDashboardServiceTest {
    @Mock BaAutorisationAbsenceRepository absences;
    @Mock BaAutorisationSortieRepository sorties;
    @Mock BaUserService users;
    @Mock BaDepartementRepository departments;
    @Mock BaServiceRepository services;
    @InjectMocks RhDashboardService service;
    private final LocalDate start = LocalDate.of(2026, 1, 1), end = LocalDate.of(2026, 2, 28);
    private BaUserDto rh;
    private BaUser agent, foreign;
    private BaDepartement department;
    private BaService unit;

    @BeforeEach void setup() {
        rh = new BaUserDto("rh"); rh.setPrenom("Awa"); rh.setNom("RH");
        BaRoleDto role = new BaRoleDto(); role.setCode("RH_CONSULTATION_ABSENCES"); rh.setRoles(Set.of(role));
        when(users.getUserInfoWithMoreDetails()).thenReturn(rh);
        department = new BaDepartement(); department.setId("D1"); department.setNom("Opérations");
        unit = new BaService(); unit.setId("S1"); unit.setNom("Agence centrale"); unit.setDepartement(department);
        agent = agent("A1", department, unit);
        BaDepartement other = new BaDepartement(); other.setId("D2"); other.setNom("Finance");
        foreign = agent("A2", other, null);
    }

    @Test void clipsDaysToPeriodAndDistributesThemBetweenMonthsAndAgents() {
        fixture();
        BaRhDashboardDto result = service.dashboard(start, end, null, null);
        assertThat(result.getNomUtilisateur()).isEqualTo("AWA RH");
        assertThat(result.getAbsencesTotal()).isEqualTo(6);
        assertThat(result.getAbsencesValidees()).isEqualTo(3);
        assertThat(result.getAbsencesEnAttente()).isEqualTo(1);
        assertThat(result.getAbsencesRejetees()).isEqualTo(1);
        assertThat(result.getAbsencesAnnulees()).isEqualTo(1);
        assertThat(result.getJoursAbsenceValides()).isEqualTo(8);
        assertThat(result.getJoursAbsenceEnAttente()).isEqualTo(3);
        assertThat(result.getAgentsConcernes()).isEqualTo(2);
        assertThat(result.getTauxValidation()).isEqualTo(75);
        assertThat(result.getTendances()).extracting(BaRhDashboardDto.Tendance::joursAbsence).containsExactly(5L, 3L);
        assertThat(result.getTendances().stream().mapToLong(BaRhDashboardDto.Tendance::joursAbsence).sum()).isEqualTo(result.getJoursAbsenceValides());
        assertThat(result.getParDepartement().stream().mapToLong(BaRhDashboardDto.Departement::joursAbsence).sum()).isEqualTo(8);
        assertThat(result.getParAgent().stream().mapToLong(BaRhDashboardDto.Agent::jours).sum()).isEqualTo(8);
        assertThat(result.getParAgent().get(0).jours()).isEqualTo(7);
        assertThat(result.getRegistre().get(0).nombreJours()).isEqualTo(5);
        assertThat(result.getRegistre().get(0).joursPeriode()).isEqualTo(3);
        verify(absences).findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(EStatut.A, end, start);
    }

    @Test void filtersDepartmentAndServiceButKeepsAllFilterOptions() {
        fixture();
        BaService otherService = new BaService(); otherService.setId("S2"); otherService.setNom("Autre service"); otherService.setDepartement(department);
        when(services.findByStatut(EStatut.A)).thenReturn(List.of(unit, otherService));
        BaRhDashboardDto result = service.dashboard(start, end, "D1", "S1");
        assertThat(result.getAbsencesTotal()).isEqualTo(5);
        assertThat(result.getJoursAbsenceValides()).isEqualTo(7);
        assertThat(result.getRegistre()).allMatch(a -> "D1".equals(a.idDepartement()) && "S1".equals(a.idService()));
        assertThat(result.getDepartements()).extracting(BaRhDashboardDto.Structure::id).containsExactlyInAnyOrder("D1", "D2");
        assertThat(result.getServices()).extracting(BaRhDashboardDto.Structure::id).containsExactlyInAnyOrder("S1", "S2");
    }

    @Test void getsDepartmentFromServiceWhenUserHasNoDirectDepartment() {
        fixture(); agent.setDepartement(null);
        assertThat(service.dashboard(start, end, "D1", null).getJoursAbsenceValides()).isEqualTo(7);
    }

    @Test void emptyPeriodHasZeroTotalsAndStillListsCreatedStructures() {
        when(departments.findByStatut(EStatut.A)).thenReturn(List.of(department));
        when(services.findByStatut(EStatut.A)).thenReturn(List.of(unit));
        BaRhDashboardDto result = service.dashboard(start, end, null, null);
        assertThat(result.getJoursAbsenceValides()).isZero();
        assertThat(result.getTauxValidation()).isZero();
        assertThat(result.getRegistre()).isEmpty();
        assertThat(result.getTendances()).hasSize(2);
        assertThat(result.getDepartements()).hasSize(1);
        assertThat(result.getServices()).hasSize(1);
    }

    @Test void countsOnlyValidatedAgentsAbsentTodayAndValidatedFutureRequests() {
        LocalDate today = LocalDate.now();
        when(absences.findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(any(), any(), any())).thenReturn(List.of(
                absence(agent, today.minusDays(1), today, EAbsenceStatut.VALIDEE),
                absence(agent, today.plusDays(1), today.plusDays(2), EAbsenceStatut.VALIDEE),
                absence(foreign, today, today, EAbsenceStatut.EN_ATTENTE)));
        BaRhDashboardDto result = service.dashboard(today.minusDays(2), today.plusDays(3), null, null);
        assertThat(result.getAbsentsAujourdHui()).isEqualTo(1);
        assertThat(result.getAbsencesAVenir()).isEqualTo(1);
        assertThat(service.dashboard(today.minusYears(1), today.minusDays(1), null, null).getAbsentsAujourdHui()).isZero();
    }

    @Test void forbiddenUsersCannotReadOrExportAnyRows() {
        rh.setRoles(Set.of()); rh.setFonction(EFonctionEmploye.CHEF_SERVICE);
        assertThatThrownBy(() -> service.dashboard(start, end, null, null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        assertThatThrownBy(() -> service.export("ABSENCES", start, end, null, null, null, null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(absences, sorties, departments, services);
    }

    @Test void grantsReportingToAdministratorsAndGeneralManagement() {
        BaRoleDto admin = new BaRoleDto(); admin.setCode("BA_ADMIN"); rh.setRoles(Set.of(admin));
        assertThatCode(() -> service.dashboard(start, end, null, null)).doesNotThrowAnyException();
        rh.setRoles(Set.of()); rh.setFonction(EFonctionEmploye.DIRECTEUR_GENERAL);
        assertThatCode(() -> service.dashboard(start, end, null, null)).doesNotThrowAnyException();
        rh.setFonction(EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT);
        assertThatCode(() -> service.dashboard(start, end, null, null)).doesNotThrowAnyException();
    }

    @Test void rejectsInvalidPeriodBeforeQueryingData() {
        assertThatThrownBy(() -> service.dashboard(end, start, null, null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(absences, sorties);
    }

    @Test void sameDayAndLeapDayAreIncluded() {
        service.dashboard(start, end, null, null);
        assertThat(RhDashboardService.daysInPeriod(start, start, start, end)).isEqualTo(1);
        assertThat(RhDashboardService.daysInPeriod(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29))).isEqualTo(2);
        assertThat(RhDashboardService.daysInPeriod(start, start, end, end)).isZero();
    }

    @Test void exportRespectsPeriodTeamStatusAndSearchAndPreservesFrenchEncoding() {
        fixture();
        String csv = new String(service.export("ABSENCES", start, end, "D1", "S1", "EN_ATTENTE", "A1"), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFType;Agent;Matricule;Département");
        assertThat(csv.lines()).hasSize(2);
        assertThat(csv).contains("Opérations", "EN_ATTENTE", "\"3\";\"3\"").doesNotContain("VALIDEE", "REJETEE", "A2");
    }

    @Test void neutralizesSpreadsheetFormulasAndQuotesSeparatorsInExport() {
        BaAutorisationAbsence value = absence(agent, start, start, EAbsenceStatut.VALIDEE);
        value.setMotif(" =HYPERLINK(\"x\");test\nligne");
        when(absences.findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(any(), any(), any())).thenReturn(List.of(
                value));
        String csv = new String(service.export("ABSENCES", start, end, null, null, null, null), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"' =HYPERLINK(\"\"x\"\");test ligne\"");
    }

    @Test void exportsAgentTotalsAndKeepsSortiesSeparateFromAbsenceDays() {
        fixture();
        BaAutorisationSortie exit = new BaAutorisationSortie(); exit.setDemandeur(agent); exit.setDateSortie(start); exit.setMotif("Rendez-vous");
        exit.setStatutValidation(EAutorisationSortieStatut.VALIDEE);
        when(sorties.findByStatutAndDateSortieBetweenOrderByDateSortieDesc(EStatut.A, start, end)).thenReturn(List.of(exit));
        BaRhDashboardDto result = service.dashboard(start, end, null, null);
        assertThat(result.getSortiesTotal()).isEqualTo(1); assertThat(result.getSortiesValidees()).isEqualTo(1);
        assertThat(result.getJoursAbsenceValides()).isEqualTo(8);
        String bilan = new String(service.export("AGENTS", start, end, null, null, null, null), StandardCharsets.UTF_8);
        assertThat(bilan.lines()).hasSize(3); assertThat(bilan).contains("Jours validés", "\"7\"");
        String csv = new String(service.export("SORTIES", start, end, null, null, null, null), StandardCharsets.UTF_8);
        assertThat(csv.lines()).hasSize(2); assertThat(csv).contains("Sortie", "Rendez-vous").doesNotContain("Absence");
    }

    @Test void rejectsUnknownExportTypesAndStatuses() {
        assertThatThrownBy(() -> service.export("UNKNOWN", start, end, null, null, null, null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> service.export("ABSENCES", start, end, null, null, "UNKNOWN", null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(absences, sorties);
    }

    private void fixture() {
        when(absences.findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(EStatut.A, end, start)).thenReturn(List.of(
                absence(agent, LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 3), EAbsenceStatut.VALIDEE),
                absence(agent, LocalDate.of(2026, 1, 30), LocalDate.of(2026, 2, 2), EAbsenceStatut.VALIDEE),
                absence(agent, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), EAbsenceStatut.EN_ATTENTE),
                absence(agent, start, start, EAbsenceStatut.REJETEE), absence(agent, start, start, EAbsenceStatut.ANNULEE),
                absence(foreign, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), EAbsenceStatut.VALIDEE)));
    }
    private BaUser agent(String id, BaDepartement department, BaService unit) {
        BaUser user = new BaUser(id); user.setPrenom("Agent"); user.setNom(id); user.setMatricule(id);
        user.setDepartement(department); user.setService(unit); return user;
    }
    private BaAutorisationAbsence absence(BaUser user, LocalDate start, LocalDate end, EAbsenceStatut status) {
        BaAutorisationAbsence a = new BaAutorisationAbsence(); a.setDemandeur(user); a.setDateDebut(start); a.setDateFin(end);
        a.setNombreJours((int) ChronoUnit.DAYS.between(start, end) + 1); a.setStatutValidation(status); a.setMotif("Motif"); return a;
    }
}

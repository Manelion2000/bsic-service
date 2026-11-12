package com.bakouan.app.service;

import com.bakouan.app.dto.BaRhDashboardDto;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.enums.EAbsenceStatut;
import com.bakouan.app.enums.EAutorisationSortieStatut;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.BaAutorisationAbsenceRepository;
import com.bakouan.app.repositories.BaAutorisationSortieRepository;
import com.bakouan.app.repositories.BaDepartementRepository;
import com.bakouan.app.repositories.BaServiceRepository;
import com.bakouan.app.security.BaUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RhDashboardService {
    private final BaAutorisationAbsenceRepository absences;
    private final BaAutorisationSortieRepository sorties;
    private final BaUserService users;
    private final BaDepartementRepository departmentRepository;
    private final BaServiceRepository serviceRepository;

    public BaRhDashboardDto dashboard(LocalDate debut, LocalDate fin, String idDepartement, String idService) {
        BaUserDto user = requireAccess();
        Period period = period(debut, fin);
        List<BaAutorisationAbsence> allAbsences = absences
                .findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(EStatut.A, period.fin, period.debut);
        List<BaAutorisationSortie> allSorties = sorties
                .findByStatutAndDateSortieBetweenOrderByDateSortieDesc(EStatut.A, period.debut, period.fin);
        List<BaAutorisationAbsence> values = allAbsences.stream()
                .filter(a -> matches(a.getDemandeur(), idDepartement, idService)).toList();
        List<BaAutorisationSortie> exits = allSorties.stream()
                .filter(a -> matches(a.getDemandeur(), idDepartement, idService)).toList();
        BaRhDashboardDto dto = new BaRhDashboardDto();
        dto.setNomUtilisateur(String.join(" ", safe(user.getPrenom()), safe(user.getNom())).trim());
        dto.setDebut(period.debut); dto.setFin(period.fin);
        dto.setSortiesTotal(exits.size());
        dto.setSortiesEnAttente(exits.stream().filter(a -> a.getStatutValidation() == EAutorisationSortieStatut.EN_ATTENTE).count());
        dto.setSortiesValidees(exits.stream().filter(a -> a.getStatutValidation() == EAutorisationSortieStatut.VALIDEE).count());
        dto.setAbsencesTotal(values.size());
        dto.setAbsencesEnAttente(count(values, EAbsenceStatut.EN_ATTENTE));
        dto.setAbsencesValidees(count(values, EAbsenceStatut.VALIDEE));
        dto.setAbsencesRejetees(count(values, EAbsenceStatut.REJETEE));
        dto.setAbsencesAnnulees(count(values, EAbsenceStatut.ANNULEE));
        dto.setJoursAbsenceValides(days(values, period));
        dto.setJoursAbsenceEnAttente(values.stream().filter(a -> a.getStatutValidation() == EAbsenceStatut.EN_ATTENTE)
                .mapToLong(a -> daysInPeriod(a.getDateDebut(), a.getDateFin(), period.debut, period.fin)).sum());
        dto.setAgentsConcernes(values.stream().filter(this::validated).map(a -> a.getDemandeur().getId()).distinct().count());
        LocalDate today = LocalDate.now();
        dto.setAbsentsAujourdHui(values.stream().filter(this::validated)
                .filter(a -> !today.isBefore(period.debut) && !today.isAfter(period.fin))
                .filter(a -> !a.getDateDebut().isAfter(today) && !a.getDateFin().isBefore(today))
                .map(a -> a.getDemandeur().getId()).distinct().count());
        dto.setAbsencesAVenir(values.stream().filter(this::validated).filter(a -> a.getDateDebut().isAfter(today)).count());
        long treated = dto.getAbsencesValidees() + dto.getAbsencesRejetees();
        dto.setTauxValidation(treated == 0 ? 0 : Math.round(dto.getAbsencesValidees() * 1000.0 / treated) / 10.0);
        List<BaRhDashboardDto.Tendance> trends = new ArrayList<>();
        for (YearMonth month = YearMonth.from(period.debut); !month.isAfter(YearMonth.from(period.fin)); month = month.plusMonths(1)) {
            LocalDate start = later(month.atDay(1), period.debut);
            LocalDate end = earlier(month.atEndOfMonth(), period.fin);
            trends.add(new BaRhDashboardDto.Tendance(month.toString(),
                    exits.stream().filter(a -> !a.getDateSortie().isBefore(start) && !a.getDateSortie().isAfter(end)).count(),
                    values.stream().filter(a -> daysInPeriod(a.getDateDebut(), a.getDateFin(), start, end) > 0).count(),
                    days(values, new Period(start, end))));
        }
        dto.setTendances(trends);
        List<BaUser> allUsers = new ArrayList<>();
        allAbsences.forEach(a -> allUsers.add(a.getDemandeur())); allSorties.forEach(a -> allUsers.add(a.getDemandeur()));
        Map<String, BaRhDashboardDto.Structure> departments = new TreeMap<>();
        Map<String, BaRhDashboardDto.Structure> services = new TreeMap<>();
        departmentRepository.findByStatut(EStatut.A).forEach(d -> departments.put(d.getId(), new BaRhDashboardDto.Structure(d.getId(), d.getNom(), null)));
        serviceRepository.findByStatut(EStatut.A).forEach(s -> services.put(s.getId(), new BaRhDashboardDto.Structure(
                s.getId(), s.getNom(), s.getDepartement() == null ? null : s.getDepartement().getId())));
        allUsers.stream().filter(Objects::nonNull).forEach(u -> {
            BaDepartement d = department(u);
            if (d != null) departments.put(d.getId(), new BaRhDashboardDto.Structure(d.getId(), d.getNom(), null));
            if (u.getService() != null) services.put(u.getService().getId(), new BaRhDashboardDto.Structure(
                    u.getService().getId(), u.getService().getNom(), d == null ? null : d.getId()));
        });
        dto.setDepartements(departments.values().stream().sorted(Comparator.comparing(BaRhDashboardDto.Structure::nom)).toList());
        dto.setServices(services.values().stream().sorted(Comparator.comparing(BaRhDashboardDto.Structure::nom)).toList());
        Map<String, List<BaAutorisationAbsence>> byDepartment = values.stream().collect(Collectors.groupingBy(a -> departmentKey(a.getDemandeur())));
        Map<String, List<BaAutorisationSortie>> exitsByDepartment = exits.stream().collect(Collectors.groupingBy(a -> departmentKey(a.getDemandeur())));
        Set<String> keys = new HashSet<>(byDepartment.keySet()); keys.addAll(exitsByDepartment.keySet());
        dto.setParDepartement(keys.stream().map(key -> {
            List<BaAutorisationAbsence> group = byDepartment.getOrDefault(key, List.of());
            return new BaRhDashboardDto.Departement(key, departments.containsKey(key) ? departments.get(key).nom() : "Sans département",
                    exitsByDepartment.getOrDefault(key, List.of()).size(), group.size(), days(group, period));
        }).sorted(Comparator.comparingLong(BaRhDashboardDto.Departement::joursAbsence).reversed()
                .thenComparing(BaRhDashboardDto.Departement::departement)).toList());
        dto.setParAgent(values.stream().collect(Collectors.groupingBy(a -> a.getDemandeur().getId())).values().stream().map(group -> {
            BaUser agent = group.get(0).getDemandeur();
            return new BaRhDashboardDto.Agent(agent.getId(), fullName(agent), agent.getMatricule(), departmentName(agent), serviceName(agent),
                    group.size(), count(group, EAbsenceStatut.VALIDEE), count(group, EAbsenceStatut.EN_ATTENTE), days(group, period));
        }).sorted(Comparator.comparingLong(BaRhDashboardDto.Agent::jours).reversed().thenComparing(BaRhDashboardDto.Agent::nom)).toList());
        dto.setRegistre(values.stream().map(a -> register(a, period)).toList());
        return dto;
    }

    public byte[] export(String type, LocalDate debut, LocalDate fin, String idDepartement, String idService, String statut, String recherche) {
        requireAccess();
        if (!List.of("TOUT", "ABSENCES", "SORTIES", "AGENTS").contains(type)) throw badRequest("Type d'export inconnu.");
        if (statut != null && !statut.isBlank() && Arrays.stream(EAbsenceStatut.values()).noneMatch(s -> s.name().equals(statut)))
            throw badRequest("Statut d'absence inconnu.");
        Period period = period(debut, fin);
        if (type.equals("AGENTS")) {
            StringBuilder bilan = new StringBuilder("\uFEFFAgent;Matricule;Département;Service;Demandes;Validées;En attente;Jours validés dans la période\r\n");
            dashboard(period.debut, period.fin, idDepartement, idService).getParAgent().forEach(a -> append(bilan,
                    a.nom(), a.matricule(), a.departement(), a.service(), a.demandes(), a.validees(), a.enAttente(), a.jours()));
            return bilan.toString().getBytes(StandardCharsets.UTF_8);
        }
        StringBuilder csv = new StringBuilder("\uFEFFType;Agent;Matricule;Département;Service;Début;Fin;Jours calendaires;Jours dans la période;Statut;Motif;Validateur\r\n");
        if (!type.equals("SORTIES")) {
            for (BaRhDashboardDto.Absence a : dashboard(period.debut, period.fin, idDepartement, idService).getRegistre()) {
                if (statut != null && !statut.isBlank() && !statut.equals(a.statutValidation())) continue;
                if (!searchMatches(recherche, a.nomCompletDemandeur(), a.matriculeDemandeur(), a.motif())) continue;
                append(csv, "Absence", a.nomCompletDemandeur(), a.matriculeDemandeur(), a.departement(), a.service(),
                        a.dateDebut(), a.dateFin(), a.nombreJours(), a.joursPeriode(), a.statutValidation(), a.motif(), a.validateur());
            }
        }
        if (!type.equals("ABSENCES")) {
            sorties.findByStatutAndDateSortieBetweenOrderByDateSortieDesc(EStatut.A, period.debut, period.fin).stream()
                    .filter(a -> matches(a.getDemandeur(), idDepartement, idService))
                    .filter(a -> statut == null || statut.isBlank() || a.getStatutValidation().name().equals(statut))
                    .filter(a -> searchMatches(recherche, fullName(a.getDemandeur()), a.getDemandeur().getMatricule(), a.getMotif()))
                    .forEach(a -> append(csv, "Sortie", fullName(a.getDemandeur()), a.getDemandeur().getMatricule(),
                            departmentName(a.getDemandeur()), serviceName(a.getDemandeur()), a.getDateSortie(), a.getDateSortie(),
                            "", "", a.getStatutValidation(), a.getMotif(), fullName(a.getValidateur())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BaUserDto requireAccess() {
        BaUserDto user = users.getUserInfoWithMoreDetails();
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        boolean permitted = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "BA_ADMIN".equals(r.getCode()) || "RH_CONSULTATION_ABSENCES".equals(r.getCode()));
        if (!permitted && user.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL && user.getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le tableau de bord est réservé aux RH et à la Direction générale.");
        return user;
    }

    private Period period(LocalDate debut, LocalDate fin) {
        LocalDate start = debut == null ? LocalDate.now().withDayOfYear(1) : debut;
        LocalDate end = fin == null ? LocalDate.of(start.getYear(), 12, 31) : fin;
        if (end.isBefore(start)) throw badRequest("La date de fin doit être postérieure ou égale à la date de début.");
        return new Period(start, end);
    }

    public static long daysInPeriod(LocalDate start, LocalDate end, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate from = later(start, periodStart), to = earlier(end, periodEnd);
        return to.isBefore(from) ? 0 : ChronoUnit.DAYS.between(from, to) + 1;
    }
    private long days(List<BaAutorisationAbsence> values, Period period) {
        return values.stream().filter(this::validated).mapToLong(a -> daysInPeriod(a.getDateDebut(), a.getDateFin(), period.debut, period.fin)).sum();
    }
    private long count(List<BaAutorisationAbsence> values, EAbsenceStatut status) { return values.stream().filter(a -> a.getStatutValidation() == status).count(); }
    private boolean validated(BaAutorisationAbsence a) { return a.getStatutValidation() == EAbsenceStatut.VALIDEE; }
    private boolean matches(BaUser u, String idDepartment, String idService) {
        return u != null && (idDepartment == null || idDepartment.isBlank() || idDepartment.equals(departmentKey(u)))
                && (idService == null || idService.isBlank() || u.getService() != null && idService.equals(u.getService().getId()));
    }
    private BaRhDashboardDto.Absence register(BaAutorisationAbsence a, Period p) {
        BaUser u = a.getDemandeur();
        return new BaRhDashboardDto.Absence(a.getId(), u.getId(), fullName(u), u.getMatricule(), departmentKey(u), departmentName(u),
                u.getService() == null ? null : u.getService().getId(), serviceName(u), a.getDateDebut(), a.getDateFin(),
                a.getNombreJours(), daysInPeriod(a.getDateDebut(), a.getDateFin(), p.debut, p.fin), a.getStatutValidation().name(),
                a.getMotif(), fullName(a.getValidateur()), a.getCommentaireValidation());
    }
    private static BaDepartement department(BaUser u) { return u == null ? null : u.getDepartement() != null ? u.getDepartement() : u.getService() == null ? null : u.getService().getDepartement(); }
    private static String departmentKey(BaUser u) { return department(u) == null ? "" : department(u).getId(); }
    private static String departmentName(BaUser u) { return department(u) == null ? "Sans département" : department(u).getNom(); }
    private static String serviceName(BaUser u) { return u == null || u.getService() == null ? "Sans service" : u.getService().getNom(); }
    private static String fullName(BaUser u) { return u == null ? "" : String.join(" ", safe(u.getPrenom()), safe(u.getNom())).trim(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static LocalDate later(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate earlier(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }
    private static boolean searchMatches(String search, String... values) {
        return search == null || search.isBlank() || Arrays.stream(values).anyMatch(s -> safe(s).toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT)));
    }
    private static void append(StringBuilder csv, Object... values) {
        csv.append(Arrays.stream(values).map(RhDashboardService::csvCell).collect(Collectors.joining(";"))).append("\r\n");
    }
    private static String csvCell(Object value) {
        String text = Objects.toString(value, "").replace('\r', ' ').replace('\n', ' ');
        String leading = text.stripLeading();
        if ((!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0) || text.startsWith("\t")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private record Period(LocalDate debut, LocalDate fin) {}
}

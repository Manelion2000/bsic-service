package com.bakouan.app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BaRhDashboardDto {
    private String nomUtilisateur;
    private LocalDate debut;
    private LocalDate fin;
    private long sortiesTotal;
    private long sortiesEnAttente;
    private long sortiesValidees;
    private long absencesTotal;
    private long absencesEnAttente;
    private long absencesValidees;
    private long absencesRejetees;
    private long absencesAnnulees;
    private long joursAbsenceValides;
    private long joursAbsenceEnAttente;
    private long agentsConcernes;
    private long absentsAujourdHui;
    private long absencesAVenir;
    private double tauxValidation;
    private List<Tendance> tendances;
    private List<Departement> parDepartement;
    private List<Agent> parAgent;
    private List<Absence> registre;
    private List<Structure> departements;
    private List<Structure> services;

    public record Tendance(String periode, long sorties, long absences, long joursAbsence) {}
    public record Departement(String id, String departement, long sorties, long absences, long joursAbsence) {}
    public record Agent(String id, String nom, String matricule, String departement, String service,
                        long demandes, long validees, long enAttente, long jours) {}
    public record Structure(String id, String nom, String idDepartement) {}
    public record Absence(String id, String idDemandeur, String nomCompletDemandeur, String matriculeDemandeur,
                          String idDepartement, String departement, String idService, String service,
                          LocalDate dateDebut, LocalDate dateFin, long nombreJours, long joursPeriode,
                          String statutValidation, String motif, String validateur, String commentaireValidation) {}
}

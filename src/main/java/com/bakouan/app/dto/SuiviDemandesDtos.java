package com.bakouan.app.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contrat volontairement limité à l'avancement, indépendant des DTO métier sensibles. */
public final class SuiviDemandesDtos {
    private SuiviDemandesDtos() {}
    public record Structure(String id, String nom) {}
    public record Configuration(boolean autorise, String perimetre, Structure departement,
                                List<Structure> departements, List<String> modules, String blocage) {}
    public record Demande(String module, String id, String agent, String departementId,
                          String departement, String service, String etat, Instant soumiseLe,
                          LocalDate debut, LocalDate fin, String etapeActive, int etapesValidees, int etapesTotal) {}
    public record Page(List<Demande> content, long totalElements, int number, int size, long totalPages) {}
    public record Detail(Demande demande, RhCircuitDto circuit) {}
}

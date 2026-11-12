package com.bakouan.app.dto;

import com.bakouan.app.enums.EFonctionEmploye;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/** Droits et calendriers explicites ; le DG est facultatif par défaut selon la politique confirmée. */
public final class CongeDefinitions {
    private CongeDefinitions() {}
    public enum Unite { JOUR, DEMI_JOUR, HEURE }
    public enum Attribution { DOTATION, ACQUISITION }
    public enum Frequence { MENSUELLE, TRIMESTRIELLE, ANNUELLE }
    public enum Prorata { AUCUN, JOURS_CALENDAIRES, JOURS_TRAVAILLES }
    public enum Portion { JOURNEE, MATIN, APRES_MIDI }
    public enum Priorite { EXPIRATION_PUIS_ORIGINE, ORIGINE_PUIS_EXPIRATION }
    public enum ExpirationReservation { PROTEGER_JUSQUA_DECISION, BLOQUER_ET_SIGNALER }
    public enum Annulation { INTERDITE, RH_AVANT_DEBUT, RH_PARTIE_NON_PRISE }

    @Data public static class Politique {
        @NotNull private Unite unite;
        @NotNull private Attribution attribution;
        @DecimalMin("0.0001") @Digits(integer=14,fraction=4) private BigDecimal dotation;
        @DecimalMin("0.0001") @Digits(integer=14,fraction=4) private BigDecimal acquisitionParEcheance;
        private Frequence frequence;
        @NotNull private Prorata prorata;
        @NotNull private RoundingMode arrondi;
        @NotNull @DecimalMin("0.0001") @Digits(integer=14,fraction=4) private BigDecimal pasDecompte;
        @NotNull private Priorite priorite;
        @NotNull private ExpirationReservation expirationReservation;
        @DecimalMin("0") @Digits(integer=14,fraction=4) private BigDecimal plafondAccumulation;
        @NotNull private Boolean reportAutorise;
        @DecimalMin("0") @Digits(integer=14,fraction=4) private BigDecimal plafondReport;
        @Min(1) private Integer expirationReportJours;
        @NotNull private Boolean anticipationAutorisee;
        @DecimalMin("0") @Digits(integer=14,fraction=4) private BigDecimal plafondAnticipation;
        @NotNull @Min(0) private Integer preavisJours;
        @NotNull @DecimalMin("0.0001") @Digits(integer=14,fraction=4) private BigDecimal dureeMin;
        @NotNull @DecimalMin("0.0001") @Digits(integer=14,fraction=4) private BigDecimal dureeMax;
        @NotNull private Boolean justificatifRequis;
        @NotNull private Boolean demiJourneesAutorisees;
        @NotNull private Boolean circuitAbsencesConfirme;
        @NotNull private Boolean dgObligatoire = false;
        @NotBlank private String politiqueRefus;
        @NotNull private Boolean modeleReservationDebitConfirme;
        @NotNull private Boolean rhGestionAdministrativeConfirmee;
        @NotNull private Boolean delegationsAutorisees;
        @NotNull private Annulation annulation;
        @NotNull private Boolean modificationAvantDebutAutorisee;
        @NotNull private Boolean retourAnticipeAutorise;
        @NotNull private Boolean confirmationRepriseRequise;
        @NotNull private Boolean remplacementRequis;
        @NotNull private Boolean passationRequise;
        @NotNull private Boolean chevauchementBloquant;
    }

    @Data public static class Plage {
        @NotNull private Portion portion;
        @NotNull private LocalTime debut;
        @NotNull private LocalTime fin;
        @NotNull @DecimalMin("0.0001") @Digits(integer=5,fraction=4) private BigDecimal poidsJour;
    }
    @Data public static class JourTravail {
        @NotNull private DayOfWeek jour;
        @Valid @NotNull private List<Plage> plages = new ArrayList<>();
    }
    @Data public static class ExceptionCalendrier {
        @NotNull private LocalDate date;
        @NotBlank @Size(max=150) private String libelle;
        @Valid @NotNull private List<Plage> plages = new ArrayList<>();
    }
    @Data public static class Calendrier {
        @NotBlank private String fuseau;
        @Valid @NotNull @Size(min=7,max=7) private List<JourTravail> semaine = new ArrayList<>();
        @Valid @NotNull private List<ExceptionCalendrier> exceptions = new ArrayList<>();
    }

    @Data public static class JourCalcule {
        private LocalDate date;
        private String calendrierId;
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private BigDecimal quantite = BigDecimal.ZERO;
        private String exclusion;
    }
    @Data public static class Calcul {
        private Unite unite;
        private String politiqueId;
        private String calendrierId;
        private String fuseau;
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private BigDecimal quantite = BigDecimal.ZERO;
        private List<JourCalcule> jours = new ArrayList<>();
    }
    @Data public static class Imputation {
        private String droitId;
        private LocalDate jour;
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private BigDecimal quantite;
        private LocalDate expiration;
        private LocalDate origine;
    }

    @Data public static class GroupeCouverture {
        @NotBlank private String libelle;
        @NotNull @Min(0) private Integer minimumPresent;
        @NotNull @Size(min=1) private List<String> agents = new ArrayList<>();
    }
    @Data public static class Couverture {
        @NotBlank @jakarta.validation.constraints.Pattern(regexp="JOURNEE") private String granularite;
        @NotBlank private String perimetre; // DEPARTEMENT, SERVICE ou AGENCE
        @NotBlank private String perimetreId;
        @Min(0) private Integer minimumPresent;
        @Min(0) private Integer maximumIndisponibles;
        @NotNull private Map<EFonctionEmploye,Integer> minimumParFonction = new LinkedHashMap<>();
        @Valid @NotNull private List<GroupeCouverture> groupes = new ArrayList<>();
        @NotNull private List<List<String>> incompatibilites = new ArrayList<>();
        @NotNull private Boolean bloquante;
    }
    public record Alerte(String regleId, LocalDate date, String message, boolean bloquante) {}
    public record ActionAudit(String action, String auteurId, String fonction, Instant date, String motif, String ancienneValeur, String nouvelleValeur) {}
}

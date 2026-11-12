package com.bakouan.app.dto;

import com.bakouan.app.dto.CongeDefinitions.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class CongeDtos {
    private CongeDtos() {}
    public record PolitiqueDto(String id,String typeCode,String libelle,String categorie,int version,String etat,LocalDate debutEffet,LocalDate finEffet,String sourceRh,Politique regles,String confirmePar,Instant confirmeLe) {}
    public record CalendrierDto(String id,String nom,LocalDate debutEffet,LocalDate finEffet,String sourceRh,Calendrier regles,boolean confirme) {}
    public record AffectationDto(String id,String agentId,String agent,String politiqueId,String typeCode,String calendrierId,LocalDate debut,LocalDate fin,LocalDate dateEntree,LocalDate dateSortie) {}
    public record SoldeDto(String droitId,String agentId,String agent,String typeCode,String libelle,Unite unite,LocalDate origineDebut,LocalDate origineFin,LocalDate expiration,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal acquis,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal reserves,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal debites,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal disponible,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal projetes,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal approuvesAVenir,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal pris,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal echusNonConfirmes,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal plafondAnticipation,boolean cloturee) {}
    public record MouvementDto(String id,String droitId,String demandeId,String nature,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantite,String source,String justification,String auteurId,Instant date) {}
    public record SimulationDto(Calcul calcul,List<Imputation> droits,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal disponibleAvant,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal disponibleApres,List<Alerte> alertes,RhCircuitDto circuit,List<String> blocages) {}
    public record PieceDto(String id,String nomFichier,String contentType,long taille,Instant creeLe) {}
    public record PassationDto(String id,String demandeId,String demandeur,LocalDate debut,LocalDate fin,String remplacantId,String remplacant,String tachesEtEcheances,String contactsUtiles,Instant priseEnChargeLe) {}
    public record DemandeDto(String id,long version,int revision,String agentId,String agent,String typeCode,String libelle,String etat,String situation,
            LocalDate debut,LocalDate fin,Portion portionDebut,Portion portionFin,LocalTime heureDebut,LocalTime heureFin,String motif,String remplaceDemandeId,
            LocalDate repriseConstatee,LocalDate repriseDemandee,Portion portionRepriseDemandee,LocalTime heureRepriseDemandee,String retourStatut,Calcul calcul,List<Imputation> droits,RhCircuitDto circuit,List<ActionAudit> historique,
            List<PieceDto> pieces,PassationDto passation,Set<String> actions,Instant soumiseLe,Instant approuveeLe) {}
    public record IndisponibiliteDto(String id,String agentId,String agent,String departementId,String serviceId,String agenceId,
            String categorie,String etat,LocalDate debut,LocalDate fin,boolean souhait) {}
    public record CouvertureDto(String id,String nom,LocalDate debut,LocalDate fin,String sourceRh,Couverture regles,boolean confirme) {}
    public record NotificationDto(String id,String demandeId,String evenement,Instant creeLe,Instant lueLe) {}
    public record AnomalieDto(String id,String droitId,String demandeId,String description,Instant creeLe) {}
    public record ImportLigneDto(int ligne,String cle,String affectationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal solde,List<String> anomalies) {}
    public record ImportSimulationDto(LocalDate arrete,String source,List<ImportLigneDto> lignes,boolean valide) {}
    public record SyntheseDto(LocalDate debut,LocalDate fin,String unite,Map<String,String> compteurs,Map<String,Long> demandesParEtape,long approuvesAVenir,long enCours,long passes,List<AnomalieDto> anomalies) {}
}

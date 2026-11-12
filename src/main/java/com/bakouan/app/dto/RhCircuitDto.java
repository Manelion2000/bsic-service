package com.bakouan.app.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RhCircuitDto {
    private String fonctionDemandeur;
    private String publicationId;
    private boolean dgObligatoire;
    private int etapeCourante;
    private String statut;
    private String blocage;
    private String etapeBloquee;
    private boolean peutDecider;
    private List<Etape> etapes = new ArrayList<>();
    private List<Decision> historique = new ArrayList<>();

    @Data
    public static class Etape {
        private String type;
        private String libelle;
        private String statut = "EN_ATTENTE";
        private List<Validateur> validateurs = new ArrayList<>();
        private String idSignataire;
        private String nomSignataire;
        private String dateDecision;
        private String commentaire;
        private String selection;
        private String fonctionRequise;
        private String departementId;
        private String serviceId;
        private String agenceId;
        private Integer delaiJours;
    }

    public record Validateur(String id, String nom) {}
    public record Decision(int etape, String type, String decision, String idSignataire,
                           String nomSignataire, String date, String commentaire, String fonctionSignataire,
                           String idDelegant, String idDelegation) {
        public Decision(int etape,String type,String decision,String idSignataire,String nomSignataire,String date,String commentaire) {
            this(etape,type,decision,idSignataire,nomSignataire,date,commentaire,null,null,null);
        }
    }
    public record Delegation(String id,String idDelegant,String idDelegue,String roleCode,java.time.LocalDate debut,java.time.LocalDate fin,String departementId,String serviceId,String agenceId) {}
}

package com.bakouan.app.dto;

import lombok.Data;

@Data
public class BaReunionParticipantDto {
    private String id;
    private String idReunion;
    private String idEmploye;
    private String matriculeEmploye;
    private String nomCompletEmploye;
    private String participantExterne;
}

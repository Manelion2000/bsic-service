package com.bakouan.app.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class BaReunionActionDto {
    private String id;
    private String idReunion;
    private String libelle;
    private String idResponsable;
    private String nomResponsable;
    private Instant dateLimite;
    private Boolean terminee;
}

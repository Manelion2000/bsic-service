package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationEtapeType;
import lombok.Data;

@Data
public class CircuitEtapeResponseDto {
    private String id;
    private EHabilitationEtapeType type;
    private Integer ordre;
    private Boolean obligatoire;
    private Boolean actif;
    private String departementId;
    private String departementLibelle;
    private String serviceId;
    private String serviceLibelle;
    private String roleId;
    private String roleLibelle;
    private String fonctionRequise;
    private Integer delaiValidationJours;
    private String conditions;
}

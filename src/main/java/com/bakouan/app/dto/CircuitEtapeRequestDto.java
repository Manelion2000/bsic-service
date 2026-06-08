package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationEtapeType;
import lombok.Data;

@Data
public class CircuitEtapeRequestDto {
    private EHabilitationEtapeType type;
    private String departementId;
    private String serviceId;
    private String roleId;
    private Integer ordre;
    private Boolean obligatoire;
    private Boolean actif;
    private String fonctionRequise;
    private Integer delaiValidationJours;
    private String conditions;
}

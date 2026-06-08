package com.bakouan.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class CircuitResponseDto {
    private String id;
    private String code;
    private String libelle;
    private String description;
    private Boolean actif;
    private com.bakouan.app.enums.ECircuitStatut statutCircuit;
    private List<CircuitEtapeResponseDto> etapes;
}

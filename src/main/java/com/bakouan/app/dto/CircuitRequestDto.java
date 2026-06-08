package com.bakouan.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class CircuitRequestDto {
    private String libelle;
    private String description;
    private Boolean actif;
    private com.bakouan.app.enums.ECircuitStatut statutCircuit;
    private List<CircuitEtapeRequestDto> etapes;
}

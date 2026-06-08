package com.bakouan.app.mapper;

import com.bakouan.app.dto.CircuitEtapeResponseDto;
import com.bakouan.app.dto.CircuitResponseDto;
import com.bakouan.app.model.BaCircuit;
import com.bakouan.app.model.BaCircuitEtape;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CircuitMapper {

    public CircuitResponseDto toResponse(final BaCircuit circuit,
                                         final List<BaCircuitEtape> etapes) {
        CircuitResponseDto dto = new CircuitResponseDto();
        dto.setId(circuit.getId());
        dto.setCode(circuit.getCode());
        dto.setLibelle(circuit.getLibelle());
        dto.setDescription(circuit.getDescription());
        dto.setActif(circuit.getActif());
        dto.setStatutCircuit(circuit.getStatu());
        dto.setEtapes(etapes.stream()
                .sorted(Comparator.comparingInt(BaCircuitEtape::getOrdre))
                .map(this::toEtapeResponse)
                .collect(Collectors.toList()));
        return dto;
    }

    public CircuitEtapeResponseDto toEtapeResponse(final BaCircuitEtape etape) {
        CircuitEtapeResponseDto dto = new CircuitEtapeResponseDto();
        dto.setId(etape.getId());
        dto.setType(etape.getType());
        dto.setOrdre(etape.getOrdre());
        dto.setObligatoire(etape.getObligatoire());
        dto.setActif(etape.getActif());
        dto.setFonctionRequise(etape.getFonctionRequise());
        dto.setDelaiValidationJours(etape.getDelaiValidationJours());
        dto.setConditions(etape.getConditions());
        if (etape.getDepartement() != null) {
            dto.setDepartementId(etape.getDepartement().getId());
            dto.setDepartementLibelle(etape.getDepartement().getNom());
        }
        if (etape.getRole() != null) {
            dto.setRoleId(etape.getRole().getId());
            dto.setRoleLibelle(etape.getRole().getLibelle());
        }
        if (etape.getService() != null) {
            dto.setServiceId(etape.getService().getId());
            dto.setServiceLibelle(etape.getService().getNom());
        }
        return dto;
    }
}

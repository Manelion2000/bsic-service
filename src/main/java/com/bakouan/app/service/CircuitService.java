package com.bakouan.app.service;

import com.bakouan.app.dto.CircuitEtapeRequestDto;
import com.bakouan.app.dto.CircuitEtapeResponseDto;
import com.bakouan.app.dto.CircuitRequestDto;
import com.bakouan.app.dto.CircuitResponseDto;

import java.util.List;

public interface CircuitService {
    CircuitResponseDto createCircuit(CircuitRequestDto request);

    CircuitResponseDto updateCircuit(String id, CircuitRequestDto request);

    CircuitResponseDto getCircuitById(String id);

    List<CircuitResponseDto> getCircuits();

    List<CircuitResponseDto> getCircuitsActifs();

    CircuitResponseDto activerCircuit(String id);

    CircuitResponseDto desactiverCircuit(String id);

    CircuitResponseDto dupliquerCircuit(String id, String nouveauNom);

    boolean testerCircuit(String id);

    void deleteCircuit(String id);

    List<CircuitEtapeResponseDto> getCircuitEtapes(String circuitId);

    List<CircuitEtapeResponseDto> replaceCircuitEtapes(String circuitId, List<CircuitEtapeRequestDto> etapes);

    CircuitEtapeResponseDto addCircuitEtape(String circuitId, CircuitEtapeRequestDto request);

    CircuitEtapeResponseDto updateCircuitEtape(String circuitId, String etapeId, CircuitEtapeRequestDto request);

    void deleteCircuitEtape(String circuitId, String etapeId);
}

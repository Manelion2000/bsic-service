package com.bakouan.app.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BaRhDemandesStatsDto {
    private long total;
    private long enAttente;
    private long validees;
    private long rejetees;
    private List<BaStatItemDto> parDepartement = new ArrayList<>();
    private List<BaStatItemDto> parService = new ArrayList<>();
    private List<BaStatItemDto> parDemandeur = new ArrayList<>();
    private List<BaStatItemDto> parValidateur = new ArrayList<>();
    private List<BaStatItemDto> parMois = new ArrayList<>();
    private List<BaStatItemDto> parTypeStructure = new ArrayList<>();
}

package com.bakouan.app.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BaPlateformeDashboardStatsDto {
    private long totalComptes;
    private long comptesACreer;
    private long comptesEnCoursCreation;
    private long comptesCrees;
    private long totalReinitialisations;
    private long reinitialisationsEnCours;
    private long reinitialisationsValidees;
    private long reinitialisationsRejetees;
    private long traitementsATraiter;
    private long traitementsEnCours;
    private long traitementsTraites;
    private List<BaStatItemDto> parTypePlateforme = new ArrayList<>();
    private List<BaStatItemDto> parDepartement = new ArrayList<>();
    private List<BaStatItemDto> topPlateformes = new ArrayList<>();
    private List<BaStatItemDto> topCreateurs = new ArrayList<>();
    private List<BaStatItemDto> topTraiteurs = new ArrayList<>();
}

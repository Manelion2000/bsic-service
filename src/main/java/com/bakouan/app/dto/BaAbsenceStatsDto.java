package com.bakouan.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class BaAbsenceStatsDto {
    private long totalDemandes;
    private long totalJours;
    private long enAttente;
    private long validees;
    private long rejetees;
    private List<Periode> parMois;
    private List<Periode> parAnnee;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Periode {
        private String periode;
        private long demandes;
        private long jours;
    }
}

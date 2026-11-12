package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface BaAutorisationAbsenceService {
    BaAutorisationAbsenceDto create(BaAutorisationAbsenceRequestDto request);
    List<BaAutorisationAbsenceDto> mesDemandes();
    List<BaAutorisationAbsenceDto> aValider();
    List<BaAutorisationAbsenceDto> suivi(LocalDate debut, LocalDate fin);
    BaAbsenceStatsDto statistiques(LocalDate debut, LocalDate fin);
    BaAutorisationAbsenceDto valider(String id, int etape, String commentaire);
    BaAutorisationAbsenceDto rejeter(String id, int etape, String commentaire);
    BaAutorisationAbsenceDto annuler(String id);
}

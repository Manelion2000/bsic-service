package com.bakouan.app.service;

import com.bakouan.app.dto.BaRepriseServiceDto;
import com.bakouan.app.dto.BaRepriseServiceRequestDto;
import com.bakouan.app.dto.BaRepriseServiceSuiviDto;
import com.bakouan.app.dto.BaRhDemandesFilterDto;
import com.bakouan.app.dto.BaRhDemandesStatsDto;

import java.util.List;

public interface BaRepriseServiceManager {
    BaRepriseServiceDto createDemande(BaRepriseServiceRequestDto request);

    List<BaRepriseServiceDto> getMesDemandes();

    List<BaRepriseServiceDto> getDemandesAValider();

    BaRepriseServiceDto getById(String id);

    BaRepriseServiceDto valider(String id, String commentaire);

    BaRepriseServiceDto rejeter(String id, String commentaire);

    BaRhDemandesStatsDto getStatistiques(BaRhDemandesFilterDto filter);

    org.springframework.data.domain.Page<BaRepriseServiceSuiviDto> getSuivi(
            BaRhDemandesFilterDto filter,
            org.springframework.data.domain.Pageable pageable);
}

package com.bakouan.app.service;

import com.bakouan.app.dto.BaAutorisationSortieDto;
import com.bakouan.app.dto.BaAutorisationSortieRequestDto;
import com.bakouan.app.dto.BaAutorisationSortieSuiviDto;
import com.bakouan.app.dto.BaRhDemandesFilterDto;
import com.bakouan.app.dto.BaRhDemandesStatsDto;

import java.util.List;

public interface BaAutorisationSortieService {
    BaAutorisationSortieDto createDemande(BaAutorisationSortieRequestDto request);

    List<BaAutorisationSortieDto> getMesDemandes();

    List<BaAutorisationSortieDto> getDemandesAValider();

    BaAutorisationSortieDto validerDemande(String id, String commentaire);

    BaAutorisationSortieDto rejeterDemande(String id, String commentaire);

    BaRhDemandesStatsDto getStatistiques(BaRhDemandesFilterDto filter);

    org.springframework.data.domain.Page<BaAutorisationSortieSuiviDto> getSuivi(
            BaRhDemandesFilterDto filter,
            org.springframework.data.domain.Pageable pageable);
}

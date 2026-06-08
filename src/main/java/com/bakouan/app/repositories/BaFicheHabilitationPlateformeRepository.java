package com.bakouan.app.repositories;

import com.bakouan.app.model.BaFicheHabilitationPlateforme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaFicheHabilitationPlateformeRepository extends JpaRepository<BaFicheHabilitationPlateforme, String> {
    List<BaFicheHabilitationPlateforme> findByFicheId(String ficheId);
}

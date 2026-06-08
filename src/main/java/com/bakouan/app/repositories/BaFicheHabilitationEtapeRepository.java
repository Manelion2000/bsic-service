package com.bakouan.app.repositories;

import com.bakouan.app.model.BaFicheHabilitationEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaFicheHabilitationEtapeRepository extends JpaRepository<BaFicheHabilitationEtape, String> {
    List<BaFicheHabilitationEtape> findByFicheId(String ficheId);

    List<BaFicheHabilitationEtape> findByFicheIdOrderByOrdre(String ficheId);
}

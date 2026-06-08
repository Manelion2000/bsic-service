package com.bakouan.app.repositories;

import com.bakouan.app.model.BaReinitialisationCompteEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaReinitialisationCompteEtapeRepository extends JpaRepository<BaReinitialisationCompteEtape, String> {
    List<BaReinitialisationCompteEtape> findByDemandeIdOrderByOrdre(String demandeId);
}

package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaPlateforme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaPlateformeRepository extends JpaRepository<BaPlateforme, String> {
    Boolean existsByCode(String code);
    List<BaPlateforme> findByStatut(EStatut statut);
    Optional<BaPlateforme> findFirstByCircuitIdAndStatut(String circuitId, EStatut statut);
    List<BaPlateforme> findByIdInAndStatut(List<String> ids, EStatut statut);
}

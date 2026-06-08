package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.enums.EHabilitationFicheStatut;
import com.bakouan.app.model.BaFicheHabilitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BaFicheHabilitationRepository extends JpaRepository<BaFicheHabilitation, String> {
    List<BaFicheHabilitation> findByStatut(EStatut statut);
    List<BaFicheHabilitation> findByEmployeId(String employeId);
    Page<BaFicheHabilitation> findByEmployeId(String employeId, Pageable pageable);
    Page<BaFicheHabilitation> findByEmployeIdAndStatutHabilitation(String employeId, EHabilitationFicheStatut statut, Pageable pageable);
}

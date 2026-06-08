package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaAgence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaAgenceRepository extends JpaRepository<BaAgence, String> {
    Boolean existsByCode(String code);
    List<BaAgence> findByStatut(EStatut statut);
}

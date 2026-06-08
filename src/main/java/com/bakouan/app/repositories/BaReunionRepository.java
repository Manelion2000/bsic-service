package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaReunion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaReunionRepository extends JpaRepository<BaReunion, String> {
    List<BaReunion> findByStatut(EStatut statut);
}

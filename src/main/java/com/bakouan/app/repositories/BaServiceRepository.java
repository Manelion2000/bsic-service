package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaServiceRepository extends JpaRepository<BaService, String> {
    Boolean existsByCode(String code);
    List<BaService> findByStatut(EStatut statut);
}

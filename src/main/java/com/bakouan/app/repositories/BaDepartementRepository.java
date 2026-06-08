package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaDepartement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaDepartementRepository extends JpaRepository<BaDepartement, String> {
    Boolean existsByCode(String code);
    List<BaDepartement> findByStatut(EStatut statut);
}

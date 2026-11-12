package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeMouvement;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeMouvementRepository extends JpaRepository<BaCongeMouvement,String> {
    List<BaCongeMouvement> findByDroitIdOrderByDateOperationAsc(String droitId);
    Optional<BaCongeMouvement> findByCle(String cle);
}


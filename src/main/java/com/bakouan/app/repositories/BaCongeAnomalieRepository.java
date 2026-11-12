package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeAnomalie;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeAnomalieRepository extends JpaRepository<BaCongeAnomalie,String> {
    List<BaCongeAnomalie> findByResolueFalseOrderByCreatedDateAsc();
    Optional<BaCongeAnomalie> findByCle(String cle);
}


package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeEcheance;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeEcheanceRepository extends JpaRepository<BaCongeEcheance,String> {
    List<BaCongeEcheance> findByDroitIdOrderByDateAcquisitionAsc(String droitId);
    List<BaCongeEcheance> findByExecuteeFalseAndDateAcquisitionLessThanEqualOrderByDateAcquisitionAsc(java.time.LocalDate date);
}


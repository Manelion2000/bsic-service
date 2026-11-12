package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeOperation;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeOperationRepository extends JpaRepository<BaCongeOperation,String> {
    Optional<BaCongeOperation> findByCle(String cle);
}


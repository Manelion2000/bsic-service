package com.bakouan.app.repositories;

import com.bakouan.app.model.BaRhCircuit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaRhCircuitRepository extends JpaRepository<BaRhCircuit, String> {
    boolean existsByPublicationId(String id);
}

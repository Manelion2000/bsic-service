package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCircuitEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaCircuitEtapeRepository extends JpaRepository<BaCircuitEtape, String> {
    List<BaCircuitEtape> findByCircuitIdOrderByOrdre(String circuitId);
    void deleteByCircuitId(String circuitId);
    boolean existsByCircuitIdAndOrdre(String circuitId, Integer ordre);
}

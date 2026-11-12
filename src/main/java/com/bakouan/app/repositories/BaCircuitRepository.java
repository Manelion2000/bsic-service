package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaCircuit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BaCircuitRepository extends JpaRepository<BaCircuit, String> {
    boolean existsByCode(String code);
    List<BaCircuit> findByPlateformeId(String plateformeId);
    List<BaCircuit> findByStatutOrderByCreatedDateDesc(EStatut statut);
    List<BaCircuit> findByStatutAndActifOrderByCreatedDateDesc(EStatut statut, Boolean actif);
    int countByCreatedDateBetween(Instant start, Instant end);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select c from BaCircuit c where c.id=:id") java.util.Optional<BaCircuit> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
}

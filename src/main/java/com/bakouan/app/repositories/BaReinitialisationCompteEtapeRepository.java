package com.bakouan.app.repositories;

import com.bakouan.app.model.BaReinitialisationCompteEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaReinitialisationCompteEtapeRepository extends JpaRepository<BaReinitialisationCompteEtape, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from BaReinitialisationCompteEtape e where e.id = :id")
    java.util.Optional<BaReinitialisationCompteEtape> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
    List<BaReinitialisationCompteEtape> findByDemandeIdOrderByOrdre(String demandeId);
}

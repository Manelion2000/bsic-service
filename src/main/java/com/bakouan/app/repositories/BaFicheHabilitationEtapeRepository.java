package com.bakouan.app.repositories;

import com.bakouan.app.model.BaFicheHabilitationEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaFicheHabilitationEtapeRepository extends JpaRepository<BaFicheHabilitationEtape, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from BaFicheHabilitationEtape e where e.id = :id")
    java.util.Optional<BaFicheHabilitationEtape> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
    List<BaFicheHabilitationEtape> findByFicheId(String ficheId);

    List<BaFicheHabilitationEtape> findByFicheIdOrderByOrdre(String ficheId);
}

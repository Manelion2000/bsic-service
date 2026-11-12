package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeDroit;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeDroitRepository extends JpaRepository<BaCongeDroit,String> {
    @org.springframework.data.jpa.repository.Query("select r.agent.id from BaCongeDroit r where r.id=:id and r.statut=com.bakouan.app.enums.EStatut.A")
    Optional<String> findAgentId(@org.springframework.data.repository.query.Param("id") String id);
    List<BaCongeDroit> findByAgentIdAndStatutOrderByOrigineDebutAsc(String agentId, EStatut statut);
    List<BaCongeDroit> findByStatutOrderByOrigineDebutAsc(EStatut statut);
    Optional<BaCongeDroit> findByCleOrigine(String cleOrigine);
}

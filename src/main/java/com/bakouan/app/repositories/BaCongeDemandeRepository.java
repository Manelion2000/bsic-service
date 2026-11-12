package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeDemande;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BaCongeDemandeRepository extends JpaRepository<BaCongeDemande,String> {
    @Query("select d.agent.id from BaCongeDemande d where d.id=:id and d.statut=com.bakouan.app.enums.EStatut.A")
    Optional<String> findAgentId(@Param("id") String id);
    List<BaCongeDemande> findByAgentIdAndStatutOrderByCreatedDateDesc(String agentId, EStatut statut);
    List<BaCongeDemande> findByStatutAndEtatOrderByCreatedDateDesc(EStatut statut, String etat);
    List<BaCongeDemande> findByStatutAndDebutLessThanEqualAndFinGreaterThanEqualOrderByDebutAsc(EStatut statut, java.time.LocalDate fin, java.time.LocalDate debut);
    List<BaCongeDemande> findByRemplaceDemandeIdAndStatut(String parentId, EStatut statut);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from BaCongeDemande d where d.id = :id")
Optional<BaCongeDemande> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
}

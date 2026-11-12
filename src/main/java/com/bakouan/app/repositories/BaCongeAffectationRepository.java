package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeAffectation;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeAffectationRepository extends JpaRepository<BaCongeAffectation,String> {
    List<BaCongeAffectation> findByAgentIdAndStatutOrderByDebutDesc(String agentId, EStatut statut);
    List<BaCongeAffectation> findByStatutOrderByCreatedDateDesc(EStatut statut);
}


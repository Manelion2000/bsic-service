package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongePassation;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongePassationRepository extends JpaRepository<BaCongePassation,String> {
    Optional<BaCongePassation> findByDemandeId(String demandeId);
    List<BaCongePassation> findByRemplacantId(String remplacantId);
}

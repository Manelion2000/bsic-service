package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeCalendrier;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeCalendrierRepository extends JpaRepository<BaCongeCalendrier,String> {
    List<BaCongeCalendrier> findByStatutOrderByCreatedDateDesc(EStatut statut);
}


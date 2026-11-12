package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeCouverture;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeCouvertureRepository extends JpaRepository<BaCongeCouverture,String> {
    List<BaCongeCouverture> findByStatutOrderByCreatedDateDesc(EStatut statut);
}


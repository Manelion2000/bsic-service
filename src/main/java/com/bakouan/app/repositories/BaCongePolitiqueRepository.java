package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongePolitique;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongePolitiqueRepository extends JpaRepository<BaCongePolitique,String> {
    List<BaCongePolitique> findByTypeCodeOrderByVersionPolitiqueDesc(String typeCode);
    List<BaCongePolitique> findByStatutOrderByTypeCodeAscVersionPolitiqueDesc(EStatut statut);
}


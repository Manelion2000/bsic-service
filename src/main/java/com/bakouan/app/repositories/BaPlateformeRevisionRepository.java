package com.bakouan.app.repositories;

import com.bakouan.app.model.BaPlateformeRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BaPlateformeRevisionRepository extends JpaRepository<BaPlateformeRevision,String> {
    Page<BaPlateformeRevision> findByPlateformeIdOrderByNumeroDesc(String id,Pageable page);
}

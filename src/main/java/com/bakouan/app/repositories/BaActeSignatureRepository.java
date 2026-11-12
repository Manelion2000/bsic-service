package com.bakouan.app.repositories;

import com.bakouan.app.model.BaActeSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface BaActeSignatureRepository extends JpaRepository<BaActeSignature, String> {
    List<BaActeSignature> findByDomaineAndReferenceIdOrderByEtapeAsc(String domaine, String referenceId);
}

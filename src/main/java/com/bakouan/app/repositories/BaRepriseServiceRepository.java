package com.bakouan.app.repositories;

import com.bakouan.app.enums.ERepriseServiceStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaRepriseService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaRepriseServiceRepository extends JpaRepository<BaRepriseService, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from BaRepriseService r where r.id = :id")
    java.util.Optional<BaRepriseService> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
    List<BaRepriseService> findByDemandeurIdAndStatutOrderByCreatedDateDesc(String demandeurId, EStatut statut);

    List<BaRepriseService> findBySignataireIdAndStatutAndStatutValidationOrderByCreatedDateDesc(
            String signataireId,
            EStatut statut,
            ERepriseServiceStatut statutValidation);

    List<BaRepriseService> findByStatutAndStatutValidationOrderByCreatedDateDesc(
            EStatut statut,
            ERepriseServiceStatut statutValidation);

    List<BaRepriseService> findByStatutOrderByCreatedDateDesc(EStatut statut);
}

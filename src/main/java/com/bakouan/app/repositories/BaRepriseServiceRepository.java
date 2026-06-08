package com.bakouan.app.repositories;

import com.bakouan.app.enums.ERepriseServiceStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaRepriseService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaRepriseServiceRepository extends JpaRepository<BaRepriseService, String> {
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

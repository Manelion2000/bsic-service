package com.bakouan.app.repositories;

import com.bakouan.app.enums.EAutorisationSortieStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaAutorisationSortie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaAutorisationSortieRepository extends JpaRepository<BaAutorisationSortie, String> {
    List<BaAutorisationSortie> findByDemandeurIdAndStatutOrderByCreatedDateDesc(String demandeurId, EStatut statut);
    List<BaAutorisationSortie> findByStatutAndStatutValidationOrderByCreatedDateDesc(
            EStatut statut,
            EAutorisationSortieStatut statutValidation);

    List<BaAutorisationSortie> findByValidateurIdAndStatutAndStatutValidationOrderByCreatedDateDesc(
            String validateurId,
            EStatut statut,
            EAutorisationSortieStatut statutValidation);

    List<BaAutorisationSortie> findByStatutOrderByCreatedDateDesc(EStatut statut);
}

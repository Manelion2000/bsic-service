package com.bakouan.app.repositories;

import com.bakouan.app.enums.EAutorisationSortieStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaAutorisationSortie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaAutorisationSortieRepository extends JpaRepository<BaAutorisationSortie, String> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"demandeur", "demandeur.departement", "demandeur.service", "demandeur.service.departement", "validateur"})
    List<BaAutorisationSortie> findByStatutAndDateSortieBetweenOrderByDateSortieDesc(
            EStatut statut, java.time.LocalDate debut, java.time.LocalDate fin);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from BaAutorisationSortie a where a.id = :id")
    java.util.Optional<BaAutorisationSortie> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
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

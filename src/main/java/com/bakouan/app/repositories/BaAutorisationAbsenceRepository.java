package com.bakouan.app.repositories;

import com.bakouan.app.enums.EAbsenceStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaAutorisationAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BaAutorisationAbsenceRepository extends JpaRepository<BaAutorisationAbsence, String> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"demandeur", "demandeur.departement", "demandeur.service", "demandeur.service.departement", "validateur"})
    List<BaAutorisationAbsence> findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(
            EStatut statut, LocalDate fin, LocalDate debut);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from BaAutorisationAbsence a where a.id = :id")
    java.util.Optional<BaAutorisationAbsence> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
    List<BaAutorisationAbsence> findByDemandeurIdAndStatutOrderByCreatedDateDesc(String id, EStatut statut);
    List<BaAutorisationAbsence> findByStatutAndStatutValidationOrderByCreatedDateDesc(EStatut statut, EAbsenceStatut validation);
    List<BaAutorisationAbsence> findByStatutOrderByCreatedDateDesc(EStatut statut);
    boolean existsByDemandeurIdAndStatutAndStatutValidationInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            String demandeurId, EStatut statut, List<EAbsenceStatut> validations, LocalDate dateFin, LocalDate dateDebut);
}

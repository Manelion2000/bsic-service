package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaValidationDelegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BaValidationDelegationRepository extends JpaRepository<BaValidationDelegation, String> {
    List<BaValidationDelegation> findByStatutOrderByCreatedDateDesc(EStatut statut);

    List<BaValidationDelegation> findByDelegueIdAndStatutAndActifTrueAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            String delegueId,
            EStatut statut,
            LocalDate dateDebut,
            LocalDate dateFin);
}

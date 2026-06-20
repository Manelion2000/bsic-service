package com.bakouan.app.repositories;

import com.bakouan.app.enums.EDgaPole;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaDgaPoleValidateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaDgaPoleValidateurRepository extends JpaRepository<BaDgaPoleValidateur, String> {
    List<BaDgaPoleValidateur> findByStatutOrderByCreatedDateDesc(EStatut statut);

    List<BaDgaPoleValidateur> findByPoleAndStatutAndActifTrueOrderByCreatedDateDesc(EDgaPole pole, EStatut statut);

    List<BaDgaPoleValidateur> findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut statut);
}

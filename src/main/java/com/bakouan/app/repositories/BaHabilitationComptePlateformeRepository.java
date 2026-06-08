package com.bakouan.app.repositories;

import com.bakouan.app.enums.EHabilitationCompteStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaHabilitationComptePlateforme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaHabilitationComptePlateformeRepository extends JpaRepository<BaHabilitationComptePlateforme, String> {
    Optional<BaHabilitationComptePlateforme> findByFicheIdAndPlateformeId(String ficheId, String plateformeId);

    List<BaHabilitationComptePlateforme> findByStatutAndStatutCreationOrderByCreatedDateDesc(
            EStatut statut,
            EHabilitationCompteStatut statutCreation);

    List<BaHabilitationComptePlateforme> findByStatutOrderByCreatedDateDesc(EStatut statut);
}

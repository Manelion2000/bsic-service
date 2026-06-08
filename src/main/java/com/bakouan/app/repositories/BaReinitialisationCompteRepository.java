package com.bakouan.app.repositories;

import com.bakouan.app.enums.EReinitialisationCompteStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaReinitialisationCompte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaReinitialisationCompteRepository extends JpaRepository<BaReinitialisationCompte, String> {
    List<BaReinitialisationCompte> findByDemandeurIdAndStatutOrderByCreatedDateDesc(String demandeurId, EStatut statut);
    List<BaReinitialisationCompte> findByStatutAndStatutDemandeOrderByCreatedDateDesc(EStatut statut, EReinitialisationCompteStatut statutDemande);
    List<BaReinitialisationCompte> findByStatutOrderByCreatedDateDesc(EStatut statut);
}

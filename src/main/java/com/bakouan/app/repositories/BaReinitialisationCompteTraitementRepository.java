package com.bakouan.app.repositories;

import com.bakouan.app.enums.EReinitialisationTraitementStatut;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaReinitialisationCompteTraitement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaReinitialisationCompteTraitementRepository extends JpaRepository<BaReinitialisationCompteTraitement, String> {
    Optional<BaReinitialisationCompteTraitement> findByDemandeId(String demandeId);
    List<BaReinitialisationCompteTraitement> findByStatutOrderByCreatedDateDesc(EStatut statut);
    List<BaReinitialisationCompteTraitement> findByStatutAndStatutTraitementOrderByCreatedDateDesc(EStatut statut, EReinitialisationTraitementStatut statutTraitement);
}

package com.bakouan.app.repositories;
import com.bakouan.app.model.BaExtractionDemande;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface BaExtractionDemandeRepository extends JpaRepository<BaExtractionDemande,String>,JpaSpecificationExecutor<BaExtractionDemande> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from BaExtractionDemande d where d.id=:id") Optional<BaExtractionDemande> locked(@Param("id") String id);
    @Query("select d.revision.requete.id from BaExtractionDemande d where d.id=:id") Optional<String> queryId(@Param("id") String id);
}

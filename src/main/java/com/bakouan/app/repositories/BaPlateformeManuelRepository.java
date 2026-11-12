package com.bakouan.app.repositories;

import com.bakouan.app.dto.PlateformeDtos.Manuel;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaPlateformeManuel;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BaPlateformeManuelRepository extends JpaRepository<BaPlateformeManuel,String> {
    // La liste et les copies historiques ne chargent pas les contenus binaires.
    @Query("select new com.bakouan.app.dto.PlateformeDtos$Manuel(m.id,m.titre,m.description,m.nomFichier,m.contentType,m.taille,m.sha256,m.createdDate,m.auteur) from BaPlateformeManuel m where m.plateforme.id=:id and m.statut=com.bakouan.app.enums.EStatut.A order by m.createdDate,m.id")
    List<Manuel> metadata(@Param("id") String id);
    Optional<BaPlateformeManuel> findByIdAndPlateformeId(String id,String plateformeId);
    @Modifying
    @Query("update BaPlateformeManuel m set m.statut=com.bakouan.app.enums.EStatut.D,m.lastModifiedBy=:actor,m.lastModifiedDate=:date,m.version=m.version+1 where m.plateforme.id=:plateforme and m.id in :ids and m.statut=com.bakouan.app.enums.EStatut.A")
    int retire(@Param("plateforme") String plateforme,@Param("ids") List<String> ids,@Param("actor") String actor,@Param("date") java.time.Instant date);
    boolean existsByPlateformeIdAndSha256AndStatut(String plateformeId,String sha256,EStatut statut);
}

package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaPlateforme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaPlateformeRepository extends JpaRepository<BaPlateforme, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<BaPlateforme> {
    boolean existsByCodeIgnoreCaseAndIdNot(String code,String id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from BaPlateforme p where p.id=:id")
    Optional<BaPlateforme> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
    @org.springframework.data.jpa.repository.Query("select distinct p from BaPlateforme p join p.dependances d where d.id=:id order by p.nom")
    List<BaPlateforme> findDependants(@org.springframework.data.repository.query.Param("id") String id);
    Boolean existsByCode(String code);
    List<BaPlateforme> findByStatut(EStatut statut);
    Optional<BaPlateforme> findFirstByCircuitIdAndStatut(String circuitId, EStatut statut);
    List<BaPlateforme> findByIdInAndStatut(List<String> ids, EStatut statut);
}

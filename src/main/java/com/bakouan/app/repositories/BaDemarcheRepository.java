package com.bakouan.app.repositories;import com.bakouan.app.model.BaDemarche; public interface BaDemarcheRepository extends org.springframework.data.jpa.repository.JpaRepository<BaDemarche,String>{java.util.Optional<BaDemarche> findByCode(String code); java.util.List<BaDemarche> findByStatutOrderByLibelleAsc(com.bakouan.app.enums.EStatut statut);
 java.util.List<BaDemarche> findByPublicationIdAndStatut(String id,com.bakouan.app.enums.EStatut statut);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select d from BaDemarche d where d.id=:id") java.util.Optional<BaDemarche> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
 }

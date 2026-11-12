package com.bakouan.app.repositories;

import com.bakouan.app.model.BaUserSignature;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface BaUserSignatureRepository extends JpaRepository<BaUserSignature, String> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from BaUserSignature s where s.userId = :id")
    Optional<BaUserSignature> findForSigning(@Param("id") String id);
}

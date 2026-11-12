package com.bakouan.app.repositories;

import com.bakouan.app.model.BaRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Repository
public interface BaRoleRepository extends JpaRepository<BaRole, String> {

    java.util.Optional<BaRole> findByCodeAndStatut(String code, com.bakouan.app.enums.EStatut statut);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from BaRole r where r.code = :code")
    java.util.Optional<BaRole> findLockedByCode(@org.springframework.data.repository.query.Param("code") String code);


}

package com.bakouan.app.repositories;
import com.bakouan.app.model.BaRequeteRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface BaRequeteRevisionRepository extends JpaRepository<BaRequeteRevision,String> {
    Optional<BaRequeteRevision> findByRequeteIdAndEdition(String id,int edition);
    List<BaRequeteRevision> findByRequeteIdOrderByEditionDesc(String id,Pageable page);
}

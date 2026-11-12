package com.bakouan.app.repositories;
import com.bakouan.app.model.BaExtractionExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface BaExtractionExecutionRepository extends JpaRepository<BaExtractionExecution,String> {
    List<BaExtractionExecution> findByRevisionRequeteIdOrderByEnregistreLeDescIdDesc(String id,Pageable page);
    List<BaExtractionExecution> findByDemandeIdOrderByEnregistreLeDescIdDesc(String id,Pageable page);
    Optional<BaExtractionExecution> findFirstByRevisionIdOrderByExecuteLeDescEnregistreLeDescIdDesc(String id);
}

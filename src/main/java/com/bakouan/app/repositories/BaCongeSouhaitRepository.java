package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongeSouhait;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongeSouhaitRepository extends JpaRepository<BaCongeSouhait,String> {
    List<BaCongeSouhait> findByStatutAndDebutLessThanEqualAndFinGreaterThanEqualOrderByDebutAsc(EStatut statut, java.time.LocalDate fin, java.time.LocalDate debut);
}


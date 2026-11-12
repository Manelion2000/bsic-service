package com.bakouan.app.repositories;

import com.bakouan.app.model.BaCongePiece;
import com.bakouan.app.enums.EStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BaCongePieceRepository extends JpaRepository<BaCongePiece,String> {
    List<BaCongePiece> findByDemandeIdAndStatut(String demandeId, EStatut statut);
    long countByDemandeIdAndStatut(String demandeId, EStatut statut);
}


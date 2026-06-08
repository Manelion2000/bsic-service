package com.bakouan.app.repositories;

import com.bakouan.app.model.BaReunionAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaReunionActionRepository extends JpaRepository<BaReunionAction, String> {
    List<BaReunionAction> findByReunionId(String reunionId);
}

package com.bakouan.app.repositories;

import com.bakouan.app.model.BaReunionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaReunionParticipantRepository extends JpaRepository<BaReunionParticipant, String> {
    List<BaReunionParticipant> findByReunionId(String reunionId);
}

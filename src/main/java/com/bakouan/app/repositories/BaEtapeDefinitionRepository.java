package com.bakouan.app.repositories;

import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.model.BaEtapeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaEtapeDefinitionRepository extends JpaRepository<BaEtapeDefinition, String> {
    List<BaEtapeDefinition> findByType(EHabilitationEtapeType type);

    List<BaEtapeDefinition> findByDepartementId(String departementId);

    List<BaEtapeDefinition> findByServiceId(String serviceId);

    Optional<BaEtapeDefinition> findFirstByLibelleIgnoreCase(String libelle);
}

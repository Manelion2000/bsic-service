package com.bakouan.app.model;

import com.bakouan.app.dto.RhCircuitDto;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Snapshot organisationnel : aucune modification par un DTO de demande. */
@Entity
@Getter
@Setter
@Table(name = "ba_rh_circuit")
public class BaRhCircuit {
    @Id private String id = BaUtils.randomUUID();
    @Column(nullable = false) private String demandeurId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private EFonctionEmploye fonctionDemandeur;
    private String departementId;
    private String publicationId;
    @Column(nullable = false) private boolean dgObligatoire;
    @Column(nullable = false, length = 30) private String politiqueRefus;
    @Column(nullable = false, length = 20) private String statut = "EN_ATTENTE";
    @Column(nullable = false) private int etapeCourante;
    @Column(nullable = false) private Instant createdDate = Instant.now();
    @Version private long lockVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private List<RhCircuitDto.Etape> etapes = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private List<RhCircuitDto.Decision> historique = new ArrayList<>();
    @Column(nullable = false) private boolean delegationsAutorisees;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private List<RhCircuitDto.Delegation> delegations = new ArrayList<>();
}

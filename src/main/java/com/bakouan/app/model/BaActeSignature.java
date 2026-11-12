package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

/** Copie immuable de l'image et de l'identité utilisées lors d'une approbation. */
@Entity @Immutable @Getter @Setter
@Table(name = "ba_acte_signature", uniqueConstraints = @UniqueConstraint(columnNames = {"domaine", "reference_id", "etape"}))
public class BaActeSignature {
    @Id private String id = BaUtils.randomUUID();
    @Column(nullable = false, length = 30) private String domaine;
    @Column(nullable = false) private String referenceId;
    @Column(nullable = false) private int etape;
    @Column(nullable = false) private String signataireId;
    @Column(nullable = false, length = 500) private String nomSignataire;
    @Column(length = 100) private String fonctionSignataire;
    @Column(nullable = false) private Instant dateSignature;
    @Column(nullable = false, length = 64) private String empreinte;
    @JsonIgnore @Column(nullable = false, columnDefinition = "bytea") private byte[] contenu;
}

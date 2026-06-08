package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaReunionAction extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne
    @JoinColumn(name = "reunion_id")
    private BaReunion reunion;

    @Column(name = "libelle", length = 1000)
    private String libelle;

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private BaUser responsable;

    @Column(name = "date_limite")
    private Instant dateLimite;

    @Column(name = "terminee")
    private Boolean terminee = Boolean.FALSE;
}

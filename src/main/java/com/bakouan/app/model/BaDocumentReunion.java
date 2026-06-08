package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor @NoArgsConstructor @Builder
@Table(name = "ba_document_reunion")
public class BaDocumentReunion extends BaAbstractAuditingEntity{
    @Id
    @Column(name="id")
    private String id= BaUtils.randomUUID();

    @Column(name = "libelle", unique = true,nullable = false)
    private String libelle;

    @Column(name = "url", unique = true,nullable = false)
    private String url;

    @OneToOne
    @JoinColumn(name = "reunion_id")
    private BaReunion reunion;

}

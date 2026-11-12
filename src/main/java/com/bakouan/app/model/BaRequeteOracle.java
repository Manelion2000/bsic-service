package com.bakouan.app.model;

import com.bakouan.app.dto.ExtractionDtos.Parametre;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;

@Entity @Getter @Setter
public class BaRequeteOracle extends BaAbstractAuditingEntity {
    @Id private String id=UUID.randomUUID().toString();
    @Column(nullable=false,length=200) private String titre;
    @Column(nullable=false,length=200) private String motCle="";
    @Column(nullable=false,length=4000) private String objectif;
    private java.time.LocalDate dateValidation;
    @Column(length=255) private String auteurId;
    @Column(length=255) private String auteur;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="departement_demandeur_id") private BaDepartement departementDemandeur;
    @Column(length=255) private String departementNom;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_demandeur_id") private BaService serviceDemandeur;
    @Column(length=255) private String serviceNom;
    @Column(length=128) private String schemaCible;
    @Column(nullable=false,columnDefinition="text") private String sqlTexte;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<Parametre> parametres=List.of();
    @Column(nullable=false) private int edition=1;
    @Column(nullable=false) private boolean verifiee;
}

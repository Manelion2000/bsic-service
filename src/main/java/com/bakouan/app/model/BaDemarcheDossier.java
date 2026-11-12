package com.bakouan.app.model;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;
@Entity @Getter @Setter public class BaDemarcheDossier extends BaAbstractAuditingEntity {
 @Id private String id=com.bakouan.app.utils.BaUtils.randomUUID(); @Version @Column(nullable=false) private long lockVersion;
 @ManyToOne(optional=false) private BaDemarche demarche;
 @ManyToOne(optional=false) private BaCircuitPublication publication;
 @ManyToOne(optional=false) private BaUser demandeur;
 @Column(nullable=false,length=200) private String titre;
 @Column(nullable=false,length=20) private String etat="BROUILLON";
 @Column(nullable=false,length=100) private String cleCreation;
 @Column(nullable=false,length=64) private String empreinteCreation;
 @ManyToOne private BaRhCircuit circuit;
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private java.util.Map<String,String> valeurs=new java.util.LinkedHashMap<>();
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private java.util.List<com.bakouan.app.dto.ProcessusDtos.Champ> champs=new java.util.ArrayList<>();
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private java.util.Map<String,String> operations=new java.util.LinkedHashMap<>();
}

package com.bakouan.app.model;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;
@Entity @Getter @Setter public class BaDemarche extends BaAbstractAuditingEntity {
 @Id private String id=com.bakouan.app.utils.BaUtils.randomUUID(); @Version @Column(nullable=false) private long lockVersion;
 @Column(nullable=false,unique=true,length=50) private String code;
 @Column(nullable=false,length=150) private String libelle;
 @Column(length=2000) private String description;
 @Column(nullable=false,length=30) private String domaine;
 @ManyToOne(optional=false) private BaCircuitPublication publication;
 @Column(nullable=false) private boolean actif;
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private java.util.List<com.bakouan.app.dto.ProcessusDtos.Champ> champs=new java.util.ArrayList<>();
}

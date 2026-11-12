package com.bakouan.app.model;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;
@Entity @Getter @Setter public class BaCircuitPublication extends BaAbstractAuditingEntity {
 @Id private String id=com.bakouan.app.utils.BaUtils.randomUUID(); @Version @Column(nullable=false) private long lockVersion;
 @ManyToOne(optional=false) private BaCircuit circuit;
 @Column(nullable=false) private int numero;
 @Column(nullable=false,length=150) private String libelle;
 @Column(length=2000) private String description;
 @Column(nullable=false,length=1500) private String justification;
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private com.bakouan.app.dto.ProcessusDtos.Definition definition;
}

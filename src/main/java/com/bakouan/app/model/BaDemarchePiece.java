package com.bakouan.app.model;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;
@Entity @Getter @Setter public class BaDemarchePiece extends BaAbstractAuditingEntity {
 @Id private String id=com.bakouan.app.utils.BaUtils.randomUUID(); @Version @Column(nullable=false) private long lockVersion;
 @ManyToOne(optional=false) private BaDemarcheDossier dossier;
 @Column(nullable=false,length=150) private String nom;
 @Column(nullable=false,length=100) private String mime;
 @Column(nullable=false,columnDefinition="bytea") private byte[] contenu;
}

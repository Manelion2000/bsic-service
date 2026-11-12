package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass @Getter @Setter
public abstract class BaCongeEntity extends BaAbstractAuditingEntity {
    @Id private String id = BaUtils.randomUUID();
    @Version @Column(nullable=false) private long lockVersion;
}

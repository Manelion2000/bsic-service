package com.bakouan.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Image personnelle privée, accessible uniquement par son propriétaire. */
@Entity @Getter @Setter @Table(name = "ba_user_signature")
public class BaUserSignature {
    @Id private String userId;
    @JsonIgnore @Column(nullable = false, columnDefinition = "bytea") private byte[] contenu;
    @Column(nullable = false, length = 64) private String empreinte;
    @Column(nullable = false) private Instant updatedDate;
    @Version private long lockVersion;
}

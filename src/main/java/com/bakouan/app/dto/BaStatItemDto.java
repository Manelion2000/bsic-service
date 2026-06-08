package com.bakouan.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaStatItemDto {
    private String id;
    private String libelle;
    private Long valeur;
}

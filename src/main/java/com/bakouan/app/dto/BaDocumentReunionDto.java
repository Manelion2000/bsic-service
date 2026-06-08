package com.bakouan.app.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class BaDocumentReunionDto {
    private String id;
    private String libelle;
    private String reunionId;
}

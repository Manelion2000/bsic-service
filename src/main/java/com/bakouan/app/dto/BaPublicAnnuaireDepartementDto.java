package com.bakouan.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class BaPublicAnnuaireDepartementDto {
    private String id;
    private String nom;
    private List<BaPublicAnnuaireAgentDto> agents;
}

package com.bakouan.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class BaAnnuaireDepartementDto {
    private String id;
    private String nom;
    private List<BaAnnuaireAgentDto> agents;
}

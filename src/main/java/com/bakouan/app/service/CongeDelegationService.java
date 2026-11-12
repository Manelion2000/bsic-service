package com.bakouan.app.service;

import com.bakouan.app.dto.BaValidationDelegationDto;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.security.BaUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

/** Reutilise le referentiel de delegations et son audit existants. */
@Service @Transactional @RequiredArgsConstructor
public class CongeDelegationService {
    private final BaUserService users;
    private final BaUserRepository repository;
    private final BaBusinessService business;
    public List<BaValidationDelegationDto> list(){users.requireAdministrator();return business.getValidationDelegations().stream().filter(d->Set.of("BA_CHEF_SERVICE","BA_DIRECTEUR").contains(d.getRoleCode())).toList();}
    public BaValidationDelegationDto save(BaValidationDelegationDto dto){users.requireAdministrator();if(dto.getIdDelegant()==null||dto.getIdDelegant().equals(dto.getIdDelegue())||!Set.of("BA_CHEF_SERVICE","BA_DIRECTEUR").contains(Objects.toString(dto.getRoleCode(),"")))throw bad("Un titulaire et un delegue distincts, chef ou directeur, sont requis. Les etapes DGA/DG ne peuvent pas etre deleguees.");var source=repository.findById(dto.getIdDelegant()).orElseThrow(()->missing("Titulaire introuvable."));if(source.getFonction()!=(dto.getRoleCode().equals("BA_CHEF_SERVICE")?EFonctionEmploye.CHEF_SERVICE:EFonctionEmploye.DIRECTEUR))throw bad("La fonction effective du titulaire ne correspond pas a l'etape deleguee.");if(dto.getMotif()==null||dto.getMotif().isBlank())throw bad("Justification de delegation obligatoire.");return dto.getId()==null?business.createValidationDelegation(dto):business.updateValidationDelegation(dto.getId(),dto);}
    public void revoke(String id){users.requireAdministrator();business.desactiverValidationDelegation(id);}
}

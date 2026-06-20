package com.bakouan.app.mapper;

import com.bakouan.app.dto.*;
import com.bakouan.app.model.*;
import com.bakouan.app.utils.BaUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Mapper(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
public interface YtMapper {
    @Mappings({})
    BaLogDto maps(BaLog entity);

    @InheritInverseConfiguration
    BaLog maps(BaLogDto dto);

    @Mappings({
            @Mapping(source = "profil.id", target = "idProfil"),
            @Mapping(source = "profil.libelle", target = "libelleProfil"),
            @Mapping(source = "service.id", target = "idService"),
            @Mapping(source = "service.nom", target = "nomService"),
            @Mapping(source = "departement.id", target = "idDepartement"),
            @Mapping(source = "departement.code", target = "codeDepartement"),
            @Mapping(source = "departement.nom", target = "nomDepartement"),
            @Mapping(source = "agence.id", target = "idAgence"),
            @Mapping(source = "agence.nom", target = "nomAgence"),
            @Mapping(source = "superieur.id", target = "idSuperieur"),
            @Mapping(target = "nomCompletSuperieur", expression = "java(entity.getSuperieur() == null ? null : entity.getSuperieur().getNom() + \" \" + entity.getSuperieur().getPrenom())"),
            @Mapping(source = "superieurSecondaire.id", target = "idSuperieurSecondaire"),
            @Mapping(target = "nomCompletSuperieurSecondaire", expression = "java(entity.getSuperieurSecondaire() == null ? null : entity.getSuperieurSecondaire().getNom() + \" \" + entity.getSuperieurSecondaire().getPrenom())")
    })
    BaUserDto maps(BaUser entity);

    @Mappings({})
    BaRoleDto maps(BaRole entity);

    @Mappings({})
    BaProfilDto maps(BaProfil entity);

    @Mappings({
            @Mapping(target = "service", ignore = true),
            @Mapping(target = "departement", ignore = true),
            @Mapping(target = "agence", ignore = true),
            @Mapping(target = "superieur", ignore = true),
            @Mapping(target = "superieurSecondaire", ignore = true),
            @Mapping(target = "profil", ignore = true)
    })
    BaUser maps(BaUserDto dto);

    @InheritInverseConfiguration
    BaRole maps(BaRoleDto dto);

    @InheritInverseConfiguration
    BaProfil maps(BaProfilDto dto);

    @Mappings({
            @Mapping(target = "idDepartement", source = "departement.id"),
            @Mapping(target = "nomDepartement", source = "departement.nom")
    })
    BaServiceDto maps(BaService entity);

    @InheritInverseConfiguration
    @Mapping(target = "departement", ignore = true)
    BaService maps(BaServiceDto dto);

    @Mappings({
            @Mapping(target = "idParentDepartement", source = "parentDepartement.id"),
            @Mapping(target = "nomParentDepartement", source = "parentDepartement.nom"),
            @Mapping(target = "idDgaValidateur", source = "dgaValidateur.id"),
            @Mapping(target = "nomDgaValidateur", expression = "java(entity.getDgaValidateur() == null ? null : entity.getDgaValidateur().getNom() + \" \" + entity.getDgaValidateur().getPrenom())")
    })
    BaDepartementDto maps(BaDepartement entity);

    @InheritInverseConfiguration
    @Mapping(target = "parentDepartement", ignore = true)
    @Mapping(target = "dgaValidateur", ignore = true)
    BaDepartement maps(BaDepartementDto dto);

    @Mappings({
            @Mapping(target = "idDepartement", source = "departement.id"),
            @Mapping(target = "nomDepartement", source = "departement.nom")
    })
    BaAgenceDto maps(BaAgence entity);

    @InheritInverseConfiguration
    @Mapping(target = "departement", ignore = true)
    BaAgence maps(BaAgenceDto dto);

    // BaEmployeDto mapping removed after fusion BaUser/BaEmploye

    @Mappings({
            @Mapping(target = "idCircuit", source = "circuit.id"),
            @Mapping(target = "libelleCircuit", source = "circuit.libelle")
    })
    BaPlateformeDto maps(BaPlateforme entity);

    @InheritInverseConfiguration
    @Mapping(target = "circuit", ignore = true)
    BaPlateforme maps(BaPlateformeDto dto);

    @Mappings({
            @Mapping(target = "idEmploye", source = "employe.id"),
            @Mapping(target = "matriculeEmploye", source = "employe.matricule"),
            @Mapping(target = "nomCompletEmploye", expression = "java(entity.getEmploye() == null ? null : entity.getEmploye().getNom() + \" \" + entity.getEmploye().getPrenom())"),
            @Mapping(target = "idServiceEmploye", source = "employe.service.id"),
            @Mapping(target = "nomServiceEmploye", source = "employe.service.nom"),
            @Mapping(target = "idDepartementEmploye", source = "employe.departement.id"),
            @Mapping(target = "nomDepartementEmploye", source = "employe.departement.nom"),
            @Mapping(target = "idDirectionEmploye", source = "employe.departement.id"),
            @Mapping(target = "nomDirectionEmploye", source = "employe.departement.nom"),
            @Mapping(target = "idAgenceEmploye", source = "employe.agence.id"),
            @Mapping(target = "nomAgenceEmploye", source = "employe.agence.nom"),
            @Mapping(target = "idCircuit", source = "circuit.id"),
            @Mapping(target = "nomCircuit", source = "circuit.libelle"),
            @Mapping(target = "nomPlateforme", source = "circuit.plateforme.nom"),
            @Mapping(target = "fonctionEmploye", expression = "java(entity.getFonctionEmploye() != null ? entity.getFonctionEmploye() : (entity.getEmploye() == null ? null : entity.getEmploye().getFonction()))"),
            @Mapping(target = "telephoneMobileEmploye", expression = "java(entity.getTelephoneMobileEmploye() != null ? entity.getTelephoneMobileEmploye() : (entity.getEmploye() == null ? null : entity.getEmploye().getTelephoneMobile()))")
    })
    BaFicheHabilitationDto maps(BaFicheHabilitation entity);

    @InheritInverseConfiguration
    BaFicheHabilitation maps(BaFicheHabilitationDto dto);

    @Mappings({
            @Mapping(target = "idFiche", source = "fiche.id"),
            @Mapping(target = "idValidateur", source = "validateur.id"),
            @Mapping(target = "nomCompletValidateur", expression = "java(entity.getValidateur() == null ? null : entity.getValidateur().getNom() + \" \" + entity.getValidateur().getPrenom())"),
            @Mapping(target = "idDepartementValidateur", source = "validateur.departement.id"),
            @Mapping(target = "nomDepartementValidateur", source = "validateur.departement.nom")
    })
    BaFicheHabilitationEtapeDto maps(BaFicheHabilitationEtape entity);

    @InheritInverseConfiguration
    BaFicheHabilitationEtape maps(BaFicheHabilitationEtapeDto dto);
    @Mappings({
            @Mapping(target = "idDepartement", source = "departement.id"),
            @Mapping(target = "nomDepartement", source = "departement.nom"),
            @Mapping(target = "idAgence", source = "agence.id"),
            @Mapping(target = "nomAgence", source = "agence.nom"),
            @Mapping(target = "idService", source = "service.id"),
            @Mapping(target = "nomService", source = "service.nom")
    })
    BaReunionDto maps(BaReunion entity);

    @InheritInverseConfiguration
    @Mapping(target = "departement", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "document", ignore = true)
    BaReunion maps(BaReunionDto dto);

    @Mappings({
            @Mapping(target = "idReunion", source = "reunion.id"),
            @Mapping(target = "idEmploye", source = "employe.id"),
            @Mapping(target = "matriculeEmploye", source = "employe.matricule"),
            @Mapping(target = "nomCompletEmploye", expression = "java(entity.getEmploye() == null ? null : entity.getEmploye().getNom() + \" \" + entity.getEmploye().getPrenom())")
    })
    BaReunionParticipantDto maps(BaReunionParticipant entity);

    @InheritInverseConfiguration
    BaReunionParticipant maps(BaReunionParticipantDto dto);

    @Mappings({
            @Mapping(target = "idReunion", source = "reunion.id"),
            @Mapping(target = "idResponsable", source = "responsable.id"),
            @Mapping(target = "nomResponsable", expression = "java(entity.getResponsable() == null ? null : entity.getResponsable().getNom() + \" \" + entity.getResponsable().getPrenom())")
    })
    BaReunionActionDto maps(BaReunionAction entity);

    @InheritInverseConfiguration
    BaReunionAction maps(BaReunionActionDto dto);

    @Mappings({
            @Mapping(target = "idDemandeur", source = "demandeur.id"),
            @Mapping(target = "nomCompletDemandeur", expression = "java(entity.getDemandeur() == null ? null : entity.getDemandeur().getNom() + \" \" + entity.getDemandeur().getPrenom())"),
            @Mapping(target = "idValidateur", source = "validateur.id"),
            @Mapping(target = "nomCompletValidateur", expression = "java(entity.getValidateur() == null ? null : entity.getValidateur().getNom() + \" \" + entity.getValidateur().getPrenom())"),
            @Mapping(target = "idDirection", source = "direction.id"),
            @Mapping(target = "nomDirection", source = "direction.nom"),
            @Mapping(target = "idService", source = "service.id"),
            @Mapping(target = "nomService", source = "service.nom"),
            @Mapping(target = "idAgence", source = "agence.id"),
            @Mapping(target = "nomAgence", source = "agence.nom")
    })
    BaAutorisationSortieDto maps(BaAutorisationSortie entity);

    @Mappings({
            @Mapping(target = "idDemandeur", source = "demandeur.id"),
            @Mapping(target = "matriculeDemandeur", source = "demandeur.matricule"),
            @Mapping(target = "nomCompletDemandeur", expression = "java(entity.getDemandeur() == null ? null : entity.getDemandeur().getNom() + \" \" + entity.getDemandeur().getPrenom())"),
            @Mapping(target = "nomServiceDemandeur", source = "demandeur.service.nom"),
            @Mapping(target = "nomDepartementDemandeur", source = "demandeur.departement.nom"),
            @Mapping(target = "idSignataire", source = "signataire.id"),
            @Mapping(target = "nomCompletSignataire", expression = "java(entity.getSignataire() == null ? null : entity.getSignataire().getNom() + \" \" + entity.getSignataire().getPrenom())"),
            @Mapping(target = "nomDepartementSignataire", source = "signataire.departement.nom"),
            @Mapping(target = "fonctionSignataire", source = "signataire.fonction")
    })
    BaRepriseServiceDto maps(BaRepriseService entity);

    @AfterMapping()
    default void afterMapping(final BaUserDto dto,
                              @MappingTarget BaUser entity) {
        if (dto == null) {
            return;
        }

        if (BaUtils.isEmpty(dto.getIdProfil())) {
            entity.setProfil(null);
        }
    }

    default ZonedDateTime map(final Instant value) {
        return value == null ? null : ZonedDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default Instant map(final ZonedDateTime value) {
        return value == null ? null : value.toInstant();
    }
}

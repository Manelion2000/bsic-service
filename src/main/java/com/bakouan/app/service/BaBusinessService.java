package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import com.bakouan.app.enums.EHabilitationStatut;

import java.util.List;

public interface BaBusinessService {

    /**
     * Lister les services actifs.
     *
     * @return la liste des services
     */
    List<BaServiceDto> getAllServices();

    /**
     * Creer un service.
     *
     * @param dto donnees du service
     * @return le service cree
     */
    BaServiceDto createService(BaServiceDto dto);
    /**
     * Mettre a jour un service.
     *
     * @param id identifiant du service
     * @param dto nouvelles donnees
     * @return le service mis a jour
     */
    BaServiceDto updateService(String id, BaServiceDto dto);

    /**
     * Lister les departements actifs.
     *
     * @return la liste des departements
     */
    List<BaDepartementDto> getAllDepartements();

    /**
     * Creer un departement.
     *
     * @param dto donnees du departement
     * @return le departement cree
     */
    BaDepartementDto createDepartement(BaDepartementDto dto);

    BaDepartementDto updateDepartement(String id, BaDepartementDto dto);

    /**
     * Lister les agences actives.
     *
     * @return la liste des agences
     */
    List<BaAgenceDto> getAllAgences();

    /**
     * Creer une agence.
     *
     * @param dto donnees de l'agence
     * @return l'agence creee
     */
    BaAgenceDto createAgence(BaAgenceDto dto);
    /**
     * Mettre a jour une agence.
     *
     * @param id identifiant de l'agence
     * @param dto nouvelles donnees
     * @return l'agence mise a jour
     */
    BaAgenceDto updateAgence(String id, BaAgenceDto dto);

    /**
     * Recuperer l'annuaire par departement avec filtres optionnels.
     *
     * @param departementId identifiant du departement (optionnel)
     * @param serviceId identifiant du service (optionnel)
     * @return l'annuaire organise par departement
     */
    List<BaAnnuaireDepartementDto> getAnnuaire(String departementId, String serviceId, String agenceId);

    /**
     * Lister les plateformes actives.
     *
     * @return la liste des plateformes
     */
    List<BaPlateformeDto> getAllPlateformes();

    /**
     * Creer une plateforme.
     *
     * @param dto donnees de la plateforme
     * @return la plateforme creee
     */
    BaPlateformeDto createPlateforme(BaPlateformeDto dto);
    /**
     * Mettre a jour une plateforme.
     *
     * @param id identifiant de la plateforme
     * @param dto nouvelles donnees
     * @return la plateforme mise a jour
     */
    BaPlateformeDto updatePlateforme(String id, BaPlateformeDto dto);

    /**
     * Creer une definition d'etape (departement/service).
     *
     * @param dto donnees de l'etape
     * @return l'etape creee
     */
    BaEtapeDefinitionDto createEtapeDefinition(BaEtapeDefinitionDto dto);
    /**
     * Mettre a jour une definition d'etape.
     *
     * @param id identifiant de l'etape
     * @param dto nouvelles donnees
     * @return l'etape mise a jour
     */
    BaEtapeDefinitionDto updateEtapeDefinition(String id, BaEtapeDefinitionDto dto);

    /**
     * Lister les definitions d'etape avec filtres optionnels.
     *
     * @param type type d'etape (optionnel)
     * @param departementId departement (optionnel)
     * @param serviceId service (optionnel)
     * @return la liste des etapes definies
     */
    List<BaEtapeDefinitionDto> getEtapeDefinitions(com.bakouan.app.enums.EHabilitationEtapeType type, String departementId, String serviceId);

    List<BaFicheHabilitationDto> getAllFichesHabilitation();

    /**
     * Lire une fiche d'habilitation avec ses signataires.
     *
     * @param id identifiant de la fiche
     * @return la fiche
     */
    BaFicheHabilitationDto getFicheHabilitationById(String id);

    /**
     * Creer une fiche d'habilitation.
     *
     * @param dto donnees de la fiche
     * @return la fiche creee
     */
    BaFicheHabilitationDto createFicheHabilitation(BaFicheHabilitationDto dto);

    /**
     * Supprimer logiquement une fiche d'habilitation.
     *
     * @param id identifiant de la fiche
     */
    void deleteFicheHabilitation(String id);

    /**
     * Recuperer les demandes de l'utilisateur connecte.
     *
     * @return la liste des fiches
     */
    org.springframework.data.domain.Page<BaFicheHabilitationDto> getMyFicheDemandes(
            com.bakouan.app.enums.EHabilitationFicheStatut statut,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Recuperer les validations en attente pour l'utilisateur connecte.
     *
     * @return la liste des fiches
     */
    org.springframework.data.domain.Page<BaFicheHabilitationDto> getMyFicheValidations(
            com.bakouan.app.enums.EHabilitationFicheStatut statut,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Generer les etapes d'une fiche depuis la configuration du circuit.
     *
     * @param ficheId identifiant de la fiche
     * @return la liste des etapes de la fiche
     */
    List<BaFicheHabilitationEtapeDto> initFicheEtapesFromCircuit(String ficheId);

    /**
     * Ajouter une etape de workflow a une fiche.
     *
     * @param dto donnees de l'etape
     * @return l'etape creee
     */
    BaFicheHabilitationEtapeDto addEtape(BaFicheHabilitationEtapeDto dto);

    /**
     * Mettre a jour le statut d'une etape.
     *
     * @param etapeId identifiant de l'etape
     * @param statut nouveau statut
     * @param commentaire commentaire de validation/rejet
     * @return l'etape mise a jour
     */
    BaFicheHabilitationEtapeDto updateEtapeStatus(String etapeId, EHabilitationStatut statut, String commentaire);

    List<BaHabilitationComptePlateformeDto> getComptesPlateforme(
            com.bakouan.app.enums.EHabilitationCompteStatut statutCreation);

    BaHabilitationComptePlateformeDto demarrerCreationCompte(String id, String commentaire);

    BaHabilitationComptePlateformeDto marquerCompteCree(String id, String commentaire);

    BaReinitialisationCompteDto createReinitialisationCompte(BaReinitialisationCompteRequestDto request);

    List<BaReinitialisationCompteDto> getMesReinitialisationsCompte();

    List<BaReinitialisationCompteDto> getReinitialisationsCompteAValider();

    BaReinitialisationCompteDto updateReinitialisationEtapeStatus(String etapeId, EHabilitationStatut statut, String commentaire);

    List<BaReinitialisationCompteDto> getReinitialisationsCompteATraiter(
            com.bakouan.app.enums.EReinitialisationTraitementStatut statutTraitement);

    BaReinitialisationCompteDto demarrerTraitementReinitialisation(String traitementId, String commentaire);

    BaReinitialisationCompteDto marquerReinitialisationTraitee(String traitementId, String commentaire);

    BaPlateformeDashboardStatsDto getPlateformeDashboardStats(BaPlateformeStatFilterDto filter);

    org.springframework.data.domain.Page<BaPlateformeUsageRowDto> getSuiviPlateformes(
            BaPlateformeStatFilterDto filter,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Lister les reunions actives.
     *
     * @return la liste des reunions
     */
    List<BaReunionDto> getAllReunions();

    /**
     * Creer une reunion.
     *
     * @param dto donnees de la reunion
     * @return la reunion creee
     */
    BaReunionDto createReunion(BaReunionDto dto);
    /**
     * Mettre a jour une reunion.
     *
     * @param id identifiant de la reunion
     * @param dto nouvelles donnees
     * @return la reunion mise a jour
     */
    BaReunionDto updateReunion(String id, BaReunionDto dto);

    /**
     * Ajouter un participant a une reunion.
     *
     * @param dto donnees du participant
     * @return le participant cree
     */
    BaReunionParticipantDto addReunionParticipant(BaReunionParticipantDto dto);
    /**
     * Mettre a jour un participant.
     *
     * @param id identifiant du participant
     * @param dto nouvelles donnees
     * @return le participant mis a jour
     */
    BaReunionParticipantDto updateReunionParticipant(String id, BaReunionParticipantDto dto);

    /**
     * Ajouter une action de suivi a une reunion.
     *
     * @param dto donnees de l'action
     * @return l'action creee
     */
    BaReunionActionDto addReunionAction(BaReunionActionDto dto);
    /**
     * Mettre a jour une action de reunion.
     *
     * @param id identifiant de l'action
     * @param dto nouvelles donnees
     * @return l'action mise a jour
     */
    BaReunionActionDto updateReunionAction(String id, BaReunionActionDto dto);
}

package com.bakouan.app.service;

import com.bakouan.app.dto.BaFicheHabilitationDto;
import com.bakouan.app.dto.BaFicheHabilitationEtapeDto;
import com.bakouan.app.dto.EReportFormat;
import com.bakouan.app.dto.EReportSource;
import com.bakouan.app.enums.EAutorisationSortieStatut;
import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.enums.ERepriseServiceStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaAutorisationSortie;
import com.bakouan.app.model.BaCircuitEtape;
import com.bakouan.app.model.BaRepriseService;
import com.bakouan.app.repositories.BaAutorisationSortieRepository;
import com.bakouan.app.repositories.BaCircuitEtapeRepository;
import com.bakouan.app.repositories.BaRepriseServiceRepository;
import com.bakouan.app.utils.BaConstants;
import com.bakouan.app.utils.BaReportUtilService;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.mapstruct.factory.Mappers;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BaReportService {
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);


    private final BaReportUtilService reportGeneratorService;
    private final BaBusinessService businessService;
    private final BaCircuitEtapeRepository circuitEtapeRepository;
    private final BaAutorisationSortieRepository autorisationSortieRepository;
    private final BaRepriseServiceRepository repriseServiceRepository;

    /**
     * Calls the utility to build the report and sends the report back.
     *
     * @param dto          Json dto containing the data
     * @param parameterMap if exists
     * @return ReportingResponseDto
     * @throws IOException
     * @throws JRException
     */
    private byte[] buildReport(
            final String reportTemplate,
            final EReportFormat format,
            final Object dto,
            final HashMap<String, ? super Object> parameterMap,
            final EReportSource source) throws IOException, JRException {

        InputStream fileInputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(reportTemplate);

        if (fileInputStream == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "jasper file : " + reportTemplate + " not found.");
        }
        // convert DTO into the JsonDatasource
        InputStream jsonFile = BaUtils.convertDtoToInputStream(dto);
        JRDataSource jsonDataSource = new JsonDataSource(jsonFile);

        return reportGeneratorService
                .genererRapport(fileInputStream, parameterMap, jsonDataSource, format, source);
    }

    /**
     * Imprime une fiche d'habilitation avec la liste des signataires.
     * Plusieurs signataires peuvent etre positionnes avant la Direction generale.
     *
     * @param idFiche identifiant de la fiche
     * @return le fichier pdf en binaire
     */
    public byte[] printFicheHabilitation(final String idFiche) {
        try {
            final BaFicheHabilitationDto fiche = businessService.getFicheHabilitationById(idFiche);
            if (fiche.getEtapes() != null) {
                fiche.getEtapes().sort(Comparator.comparingInt(etape -> etape.getOrdre() == null ? Integer.MAX_VALUE : etape.getOrdre()));
            }
            Map<String, Object> payload = buildFicheReportPayload(fiche);
            List<Map<String, ?>> signataires = buildSignatairesRows(fiche).stream()
                    .map(row -> (Map<String, ?>) row)
                    .collect(Collectors.toList());
            final HashMap<String, Object> parameters = new HashMap<>();
            parameters.put(BaConstants.REPORTS.PARAM_TITLE, "Fiche d'habilitation");
            parameters.put("SIGNATAIRES_DATA_SOURCE", new JRMapCollectionDataSource(signataires));
            final byte[] bytes = buildReport(
                    BaConstants.REPORTS.REPORT_FICHE_HABILITATION,
                    EReportFormat.PDF,
                    List.of(payload),
                    parameters,
                    EReportSource.SOURCE
            );
            if (bytes != null && bytes.length > 0) {
                BaUtils.createFileFromByteArray(bytes, "./target/fiche-habilitation-" + idFiche + ".pdf");
            }
            return bytes;
        } catch (Exception e) {
            log.error("Erreur de generation du report fiche habilitation", e);
            return new byte[]{};
        }
    }

    public byte[] printAutorisationSortie(final String idAutorisation) {
        try {
            BaAutorisationSortie entity = autorisationSortieRepository.findById(idAutorisation)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'autorisation introuvable"));

            final HashMap<String, Object> parameters = new HashMap<>();
            parameters.put(BaConstants.REPORTS.PARAM_TITLE, "Autorisation de sortie");
            parameters.put("LOGO_IMAGE", readLogoImage());
            parameters.put("QR_IMAGE", buildQrCodeImage(entity));

            final byte[] bytes = buildReport(
                    BaConstants.REPORTS.REPORT_AUTORISATION_SORTIE,
                    EReportFormat.PDF,
                    List.of(buildAutorisationPayload(entity)),
                    parameters,
                    EReportSource.SOURCE
            );
            if (bytes != null && bytes.length > 0) {
                BaUtils.createFileFromByteArray(bytes, "./target/autorisation-sortie-" + idAutorisation + ".pdf");
            }
            return bytes;
        } catch (Exception e) {
            log.error("Erreur de generation du report autorisation sortie", e);
            return new byte[]{};
        }
    }

    public byte[] printAttestationRepriseService(final String idReprise) {
        try {
            BaRepriseService entity = repriseServiceRepository.findById(idReprise)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de reprise introuvable"));
            if (entity.getStatutValidation() != ERepriseServiceStatut.VALIDEE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "L'attestation est disponible uniquement apres validation de la reprise.");
            }
            final HashMap<String, Object> parameters = new HashMap<>();
            parameters.put(BaConstants.REPORTS.PARAM_TITLE, "Attestation de reprise de service");
            parameters.put("LOGO_IMAGE", readLogoImage());
            parameters.put("QR_IMAGE", buildRepriseQrCodeImage(entity));
            return buildReport(BaConstants.REPORTS.REPORT_ATTESTATION_REPRISE_SERVICE, EReportFormat.PDF,
                    List.of(buildReprisePayload(entity)), parameters, EReportSource.SOURCE);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur de generation de l'attestation de reprise de service", e);
            return new byte[]{};
        }
    }

    private Map<String, Object> buildFicheReportPayload(final BaFicheHabilitationDto fiche) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", emptyIfNull(fiche.getId()));
        payload.put("matriculeEmploye", emptyIfNull(fiche.getMatriculeEmploye()));
        payload.put("nomCompletEmploye", emptyIfNull(fiche.getNomCompletEmploye()));
        payload.put("nomDirectionEmploye", emptyIfNull(fiche.getNomDirectionEmploye()));
        payload.put("nomServiceEmploye", emptyIfNull(fiche.getNomServiceEmploye()));
        payload.put("nomDepartementEmploye", emptyIfNull(fiche.getNomDepartementEmploye()));
        payload.put("nomAgenceEmploye", emptyIfNull(fiche.getNomAgenceEmploye()));
        payload.put("nomPlateforme", emptyIfNull(fiche.getNomPlateforme()));
        payload.put("nomCircuit", emptyIfNull(fiche.getNomCircuit()));
        payload.put("statutHabilitation", fiche.getStatutHabilitation() == null ? "" : fiche.getStatutHabilitation().name());
        payload.put("priorite", fiche.getPriorite() == null ? "" : fiche.getPriorite().name());
        payload.put("motif", emptyIfNull(fiche.getMotif()));
        payload.put("fonctionEmploye", fiche.getFonctionEmploye() == null ? "" : fiche.getFonctionEmploye().name());
        payload.put("telephoneMobileEmploye", emptyIfNull(fiche.getTelephoneMobileEmploye()));
        payload.put("etapes", "");
        return payload;
    }

    private String emptyIfNull(final String value) {
        return value == null ? "" : value;
    }

    private List<Map<String, Object>> buildSignatairesRows(final BaFicheHabilitationDto fiche) {
        if (fiche.getEtapes() != null && !fiche.getEtapes().isEmpty()) {
            return fiche.getEtapes().stream()
                    .sorted(Comparator.comparingInt(e -> e.getOrdre() == null ? Integer.MAX_VALUE : e.getOrdre()))
                    .map(this::toSignataireRow)
                    .collect(Collectors.toList());
        }
        if (BaUtils.isEmpty(fiche.getIdCircuit())) {
            return List.of();
        }
        return circuitEtapeRepository.findByCircuitIdOrderByOrdre(fiche.getIdCircuit()).stream()
                .map(this::toSignataireRowFromCircuit)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toSignataireRow(final BaFicheHabilitationEtapeDto etape) {
        Map<String, Object> row = new LinkedHashMap<>();
        String nomSignataire = firstNotEmpty(
                etape.getNomService(),
                etape.getNomDepartement(),
                etape.getNomDepartementValidateur(),
                "Non renseigne"
        );
        row.put("nomSignataire", nomSignataire);
        row.put("avis", buildAvis(etape.getStatutValidation(), etape.getDateValidation(), etape.getNomCompletValidateur(), etape.getCommentaire()));
        return row;
    }

    private String firstNotEmpty(final String... values) {
        for (String value : values) {
            if (!BaUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> toSignataireRowFromCircuit(final BaCircuitEtape etape) {
        Map<String, Object> row = new LinkedHashMap<>();
        String nomSignataire = etape.getService() != null ? etape.getService().getNom()
                : (etape.getDepartement() != null ? etape.getDepartement().getNom() : "Non renseigne");
        row.put("nomSignataire", nomSignataire);
        row.put("avis", "ENCOURS");
        return row;
    }

    private String buildAvis(final EHabilitationStatut statut,
                             final java.time.ZonedDateTime dateValidation,
                             final String nomValidateur,
                             final String commentaire) {
        if (statut == null || statut == EHabilitationStatut.EN_ATTENTE) {
            return "ENCOURS";
        }
        String base = statut == EHabilitationStatut.VALIDE ? "VALIDE" : "REJETTE";
        String detailDate = dateValidation == null ? ""
                : " le " + dateValidation.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy 'a' HH'h' mm", Locale.FRENCH));
        String detailValidateur = BaUtils.isEmpty(nomValidateur) ? "" : " par " + nomValidateur;
        String detailCommentaire = BaUtils.isEmpty(commentaire) ? "" : " (" + commentaire + ")";
        return base + detailDate + detailValidateur + detailCommentaire;
    }

    private Map<String, Object> buildAutorisationPayload(final BaAutorisationSortie entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", entity.getId());
        payload.put("nomCompletDemandeur", entity.getDemandeur() == null ? null
                : entity.getDemandeur().getNom() + " " + entity.getDemandeur().getPrenom());
        payload.put("matriculeDemandeur", entity.getDemandeur() == null ? null : entity.getDemandeur().getMatricule());
        payload.put("fonctionDemandeur", entity.getDemandeur() == null || entity.getDemandeur().getFonction() == null
                ? null : entity.getDemandeur().getFonction().name());
        payload.put("serviceDemandeur", entity.getDemandeur() == null || entity.getDemandeur().getService() == null
                ? null : entity.getDemandeur().getService().getNom());
        payload.put("directionDemandeur", entity.getDemandeur() == null || entity.getDemandeur().getDepartement() == null
                ? null : entity.getDemandeur().getDepartement().getNom());
        payload.put("agenceDemandeur", entity.getDemandeur() == null || entity.getDemandeur().getAgence() == null
                ? null : entity.getDemandeur().getAgence().getNom());
        payload.put("typeStructure", entity.getTypeStructure());
        payload.put("motif", entity.getMotif());
        payload.put("dateSortie", entity.getDateSortie() == null ? null : entity.getDateSortie().toString());
        payload.put("statutValidation", entity.getStatutValidation() == null ? null : entity.getStatutValidation().name());
        payload.put("commentaireValidation", entity.getCommentaireValidation());
        payload.put("nomCompletValidateur", entity.getValidateur() == null ? null
                : entity.getValidateur().getNom() + " " + entity.getValidateur().getPrenom());
        payload.put("dateValidation", entity.getDateValidation() == null
                ? null
                : entity.getDateValidation().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm", Locale.FRENCH)));
        payload.put("isValidee", entity.getStatutValidation() == EAutorisationSortieStatut.VALIDEE);
        return payload;
    }

    private BufferedImage readLogoImage() {
        try {
            ClassPathResource logo = new ClassPathResource("image/bsic.png");
            try (InputStream in = logo.getInputStream()) {
                return ImageIO.read(in);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage buildQrCodeImage(final BaAutorisationSortie entity) {
        try {
            String qrText = "AUTORISATION_SORTIE|ID=" + entity.getId()
                    + "|DEMANDEUR=" + (entity.getDemandeur() == null ? "" : entity.getDemandeur().getNom() + " " + entity.getDemandeur().getPrenom())
                    + "|DATE_SORTIE=" + (entity.getDateSortie() == null ? "" : entity.getDateSortie())
                    + "|STATUT=" + (entity.getStatutValidation() == null ? "" : entity.getStatutValidation().name())
                    + "|VALIDATEUR=" + (entity.getValidateur() == null ? "" : entity.getValidateur().getNom() + " " + entity.getValidateur().getPrenom())
                    + "|DATE_VALIDATION=" + (entity.getDateValidation() == null ? "" : entity.getDateValidation());
            var bitMatrix = new QRCodeWriter().encode(qrText, BarcodeFormat.QR_CODE, 180, 180);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildReprisePayload(final BaRepriseService entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", entity.getId());
        payload.put("nomDemandeur", entity.getDemandeur() == null ? null : entity.getDemandeur().getNom());
        payload.put("prenomDemandeur", entity.getDemandeur() == null ? null : entity.getDemandeur().getPrenom());
        payload.put("serviceDemandeur", entity.getDemandeur() == null || entity.getDemandeur().getService() == null ? null : entity.getDemandeur().getService().getNom());
        payload.put("departementDemandeur", entity.getDemandeur() == null || entity.getDemandeur().getDepartement() == null ? null : entity.getDemandeur().getDepartement().getNom());
        payload.put("motifAbsence", entity.getMotifAbsence());
        payload.put("dateDebutConge", formatDate(entity.getDateDebutConge()));
        payload.put("dateFinConge", formatDate(entity.getDateFinConge()));
        payload.put("dateReprise", formatDate(entity.getDateReprise()));
        payload.put("nomSignataire", entity.getSignataire() == null ? null : entity.getSignataire().getNom());
        payload.put("prenomSignataire", entity.getSignataire() == null ? null : entity.getSignataire().getPrenom());
        payload.put("departementSignataire", entity.getSignataire() == null || entity.getSignataire().getDepartement() == null ? null : entity.getSignataire().getDepartement().getNom());
        payload.put("fonctionSignataire", entity.getSignataire() == null || entity.getSignataire().getFonction() == null ? null : entity.getSignataire().getFonction().name());
        payload.put("dateValidation", entity.getDateValidation() == null ? null : entity.getDateValidation().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm")));
        payload.put("mentionValidation", "VALIDE ET AUTORISE A PRENDRE SERVICE");
        return payload;
    }

    private String formatDate(final java.time.LocalDate date) {
        return date == null ? null : date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private BufferedImage buildRepriseQrCodeImage(final BaRepriseService entity) {
        try {
            String qrText = "REPRISE_SERVICE|ID=" + entity.getId()
                    + "|DEMANDEUR=" + (entity.getDemandeur() == null ? "" : entity.getDemandeur().getNom() + " " + entity.getDemandeur().getPrenom())
                    + "|DATE_REPRISE=" + (entity.getDateReprise() == null ? "" : entity.getDateReprise())
                    + "|STATUT=" + entity.getStatutValidation().name()
                    + "|SIGNATAIRE=" + (entity.getSignataire() == null ? "" : entity.getSignataire().getNom() + " " + entity.getSignataire().getPrenom())
                    + "|DATE_VALIDATION=" + (entity.getDateValidation() == null ? "" : entity.getDateValidation());
            var bitMatrix = new QRCodeWriter().encode(qrText, BarcodeFormat.QR_CODE, 180, 180);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            log.warn("Impossible de generer le QR code de reprise de service", e);
            return null;
        }
    }

}

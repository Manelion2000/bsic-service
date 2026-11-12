package com.bakouan.app.service;

import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.bakouan.app.service.BaActePdfService.*;

@Service @Transactional(readOnly = true) @RequiredArgsConstructor
public class BaReportService {
    private final BaActePdfService pdf;
    private final SignatureService signatures;
    private final BaBusinessService business;
    private final BaAutorisationSortieRepository sorties;
    private final BaAutorisationAbsenceRepository absences;
    private final BaRepriseServiceRepository reprises;
    private final BaRepriseServiceManager repriseManager;
    private final CongeSecurity security;
    private final RhCircuitService circuits;

    public byte[] printAutorisationSortie(String id) {
        BaAutorisationSortie d = sorties.findById(id).orElseThrow(() -> missing("Autorisation de sortie introuvable."));
        requireRhRead(d.getDemandeur(), d.getValidateur(), d.getCircuit());
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Date de sortie", date(d.getDateSortie())); details.put("Structure", label(d.getTypeStructure()));
        if (d.getDateValidation() != null) details.put("Décision finale", formatInstant(d.getDateValidation().toInstant().toString()));
        return pdf.render(new Acte("Autorisation de sortie", d.getId(), d.getStatutValidation().name(), pdf.identity(d.getDemandeur()), details, d.getMotif(), pdf.circuitVisas(d.getCircuit())));
    }

    public byte[] printAutorisationAbsence(String id) {
        BaAutorisationAbsence d = absences.findById(id).orElseThrow(() -> missing("Autorisation d'absence introuvable."));
        requireRhRead(d.getDemandeur(), d.getValidateur(), d.getCircuit());
        if (d.getStatutValidation() != EAbsenceStatut.VALIDEE) throw blocked("L'acte est disponible après l'approbation finale de l'absence.");
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Période autorisée", "Du " + date(d.getDateDebut()) + " au " + date(d.getDateFin())); details.put("Durée", d.getNombreJours() + " jour(s) calendaire(s)");
        details.put("Décision finale", formatInstant(d.getDateValidation() == null ? null : d.getDateValidation().toInstant().toString()));
        return pdf.render(new Acte("Autorisation d'absence", d.getId(), d.getStatutValidation().name(), pdf.identity(d.getDemandeur()), details, d.getMotif(), pdf.circuitVisas(d.getCircuit())));
    }

    public byte[] printAttestationRepriseService(String id) {
        repriseManager.getById(id);
        BaRepriseService d = reprises.findById(id).orElseThrow(() -> missing("Reprise de service introuvable."));
        if (d.getStatutValidation() != ERepriseServiceStatut.VALIDEE) throw blocked("L'attestation est disponible après la validation de la reprise.");
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Période d'absence", "Du " + date(d.getDateDebutConge()) + " au " + date(d.getDateFinConge())); details.put("Reprise effective", date(d.getDateReprise())); details.put("Motif de l'absence", d.getMotifAbsence());
        BaActeSignature copy = signatures.forAct("REPRISE", id).stream().findFirst().orElse(null);
        String signedAt = copy != null ? copy.getDateSignature().toString() : d.getDateValidation() == null ? null : d.getDateValidation().toInstant().toString();
        Visa visa = new Visa("Validation de la reprise", copy == null ? CongeConfigurationService.name(d.getSignataire()) : copy.getNomSignataire(), copy == null ? Objects.toString(d.getSignataire().getFonction(), null) : copy.getFonctionSignataire(), "VALIDEE", signedAt, d.getCommentaireValidation(), null, copy);
        return pdf.render(new Acte("Attestation de reprise de service", id, "VALIDEE", pdf.identity(d.getDemandeur()), details, "Il est attesté que l'agent désigné ci-dessus a repris son service à la date indiquée, après validation par le signataire ci-dessous.", List.of(visa)));
    }

    public byte[] printFicheHabilitation(String id) {
        var d = business.getFicheHabilitationById(id);
        BaUser actor = security.current();
        boolean participant = d.getEtapes() != null && d.getEtapes().stream().anyMatch(e -> actor.getId().equals(e.getIdValidateur()));
        if (!security.has(actor, "BA_ADMIN") && !actor.getId().equals(d.getIdEmploye()) && !participant)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à consulter cette fiche.");
        Map<String, String> identity = new LinkedHashMap<>();
        identity.put("Agent", d.getNomCompletEmploye()); identity.put("Matricule", d.getMatriculeEmploye()); identity.put("Fonction", label(Objects.toString(d.getFonctionEmploye(), ""))); identity.put("Département", d.getNomDepartementEmploye()); identity.put("Service", d.getNomServiceEmploye());
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Plateformes", d.getNomsPlateformes() == null || d.getNomsPlateformes().isEmpty() ? d.getNomPlateforme() : String.join(", ", d.getNomsPlateformes())); details.put("Circuit", d.getNomCircuit()); details.put("Priorité", label(Objects.toString(d.getPriorite(), "")));
        var copies = signatures.forAct("FICHE", id);
        List<Visa> visas = d.getEtapes() == null ? List.of() : d.getEtapes().stream().sorted(Comparator.comparing(e -> e.getOrdre() == null ? Integer.MAX_VALUE : e.getOrdre())).map(e -> {
            BaActeSignature copy = copies.stream().filter(s -> Objects.equals(s.getEtape(), e.getOrdre()) && s.getSignataireId().equals(e.getIdValidateur())).findFirst().orElse(null);
            String title = e.getNomService() == null ? e.getNomDepartement() : e.getNomService();
            String signedAt = copy != null ? copy.getDateSignature().toString() : e.getDateValidation() == null ? null : e.getDateValidation().toInstant().toString();
            return new Visa(title, copy == null ? e.getNomCompletValidateur() : copy.getNomSignataire(), copy == null ? Objects.toString(e.getFonctionRequise(), null) : copy.getFonctionSignataire(), Objects.toString(e.getStatutValidation(), "EN_ATTENTE"), signedAt, e.getCommentaire(), null, copy);
        }).toList();
        return pdf.render(new Acte("Fiche d'habilitation", id, Objects.toString(d.getStatutHabilitation(), "EN_COURS"), identity, details, d.getMotif(), visas));
    }

    private void requireRhRead(BaUser requester, BaUser validator, BaRhCircuit circuit) {
        BaUser actor = security.current();
        if (security.has(actor, "BA_ADMIN") || actor.getId().equals(requester.getId()) || validator != null && actor.getId().equals(validator.getId()) || circuits.participant(circuit, actor)) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à consulter cette demande.");
    }
    private ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException blocked(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}

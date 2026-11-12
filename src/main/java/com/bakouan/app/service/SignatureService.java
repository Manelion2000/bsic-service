package com.bakouan.app.service;

import com.bakouan.app.dto.BaLogDto;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service @Transactional @RequiredArgsConstructor
public class SignatureService {
    private final CongeSecurity security;
    private final BaUserRepository users;
    private final BaUserSignatureRepository signatures;
    private final BaActeSignatureRepository visas;
    private final SignatureImageValidator images;
    private final BaLogService logs;

    public record Status(boolean presente, Instant miseAJour, String empreinte) {}

    @Transactional(readOnly = true)
    public Status status() {
        return signatures.findById(security.current().getId()).map(this::status).orElse(new Status(false, null, null));
    }
    private Status status(BaUserSignature s) { return new Status(true, s.getUpdatedDate(), s.getEmpreinte()); }

    /** Prévisualisation privée : même normalisation que l'enregistrement, sans modifier le compte. */
    @Transactional(readOnly = true)
    public byte[] preview(MultipartFile file) {
        security.current();
        return images.normalize(file);
    }

    public Status upload(MultipartFile file) {
        byte[] png = images.normalize(file);
        BaUser actor = security.current();
        users.findLockedById(actor.getId()).orElseThrow();
        BaUserSignature s = signatures.findById(actor.getId()).orElseGet(BaUserSignature::new);
        s.setUserId(actor.getId()); s.setContenu(png); s.setEmpreinte(hash(png)); s.setUpdatedDate(Instant.now());
        signatures.save(s);
        logs.log(new BaLogDto(EAction.U, "Signature personnelle enregistrée : " + actor.getId()));
        return status(s);
    }

    @Transactional(readOnly = true)
    public byte[] image() {
        return signatures.findById(security.current().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune signature enregistrée.")).getContenu().clone();
    }

    public void remove() {
        BaUser actor = security.current();
        users.findLockedById(actor.getId()).orElseThrow();
        signatures.deleteById(actor.getId());
        logs.log(new BaLogDto(EAction.D, "Signature personnelle retirée : " + actor.getId()));
    }

    /** Appelé dans la même transaction que la décision, après les contrôles d'éligibilité. */
    public void capture(String domaine, String reference, int etape, BaUser actor, Instant date) {
        BaUserSignature personal = signatures.findForSigning(actor.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Enregistrez votre signature dans « Ma signature » avant d'approuver cette demande."));
        BaActeSignature copy = new BaActeSignature();
        copy.setDomaine(domaine); copy.setReferenceId(reference); copy.setEtape(etape); copy.setSignataireId(actor.getId());
        copy.setNomSignataire((Objects.toString(actor.getNom(), "") + " " + Objects.toString(actor.getPrenom(), "")).trim());
        copy.setFonctionSignataire(Objects.toString(actor.getFonction(), null)); copy.setDateSignature(date);
        copy.setEmpreinte(personal.getEmpreinte()); copy.setContenu(personal.getContenu().clone());
        visas.save(copy);
    }

    @Transactional(readOnly = true)
    public List<BaActeSignature> forAct(String domaine, String reference) {
        return reference == null ? List.of() : visas.findByDomaineAndReferenceIdOrderByEtapeAsc(domaine, reference);
    }

    private String hash(byte[] png) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}

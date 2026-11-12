package com.bakouan.app.service;

import com.bakouan.app.utils.BaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Classe de gestion du stockage des fichiers dans le système de fichier.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class BaFileStorageService {

    @Value("${app.storage.path}")
    private String basePath;

    /**
     * Récupérer le contenu d'un fichier.
     *
     * @param idDoc identifiant du fichier.
     * @return un tableau de byte
     */
    private Path resolve(final String id) {
        if(id==null || !id.matches("[a-zA-Z0-9._-]{1,150}") || id.contains(".."))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Identifiant de document invalide.");
        Path root=Path.of(basePath).toAbsolutePath().normalize();Path target=root.resolve(id).normalize();
        if(!target.startsWith(root))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Identifiant de document invalide.");
        try{if(Files.exists(target)&&!target.toRealPath().startsWith(root.toRealPath()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Document hors du stockage autorisé.");}catch(IOException e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,"Stockage indisponible.");}
        return target;
    }

    public byte[] get(final String idDoc) {
        File file = resolve(idDoc).toFile();
        try {
            if (file.exists()) {
                return Files.readAllBytes(file.toPath());
            } else {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Document introuvable.");
            }
        } catch (IOException e) {
            log.error("Erreur de chargement du fichier : " + idDoc, e);
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,"Lecture du document impossible.");
        }
    }

    /**
     * Enregistrer un document.
     *
     * @param content le contenu du fichier avec les metadata
     * @return l'id généré après l'enregistrement
     */
    public String save(final byte[] content) {
        String id = BaUtils.randomUUID();
        if (content != null && content.length > 0) {
            File file = resolve(id).toFile();

            try {
                Files.createDirectories(file.toPath().getParent());
                log.debug("Saving File :  {} in : {}", id, file.getAbsolutePath());

Files.write(file.toPath(), content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                return id;
            } catch (IOException e) {
                log.error("Erreur d'enregistrement du fichier", e);
            }
        }
        return null;
    }

    /**
     * Mettre à jour un document.
     *
     * @param idDocument identifiant de l'imgae
     * @param content    le contenu du fichier avec les metadata
     * @return l'id généré après l'enregistrement
     */
    public boolean update(final String idDocument, final byte[] content) {

        if (content != null && content.length > 0) {
            File file = resolve(idDocument).toFile();
            try {
                log.debug("Saving File :  {} in : {}", idDocument, file.getAbsolutePath());
                Files.write(file.toPath(), content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            } catch (IOException e) {
                log.error("Erreur de modification du fichier", e);
            }
        }
        return false;
    }

    /**
     * Supprimer un fichier.
     *
     * @param idDocument identifiant du fichier
     * @return <code>true</code>, si la suppression réussi
     */
    public boolean remove(final String idDocument) {
        log.warn("Removing file {}", idDocument);
        File file = resolve(idDocument).toFile();
        try {
            if (file.exists()) {
                Files.delete(file.toPath());
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to remove file", e);
            return false;
        }
    }
}

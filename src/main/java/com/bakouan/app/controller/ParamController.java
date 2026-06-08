package com.bakouan.app.controller;

import com.bakouan.app.service.BaFileStorageService;
import com.bakouan.app.utils.BaConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RequiredArgsConstructor
@RestController
@RequestMapping(BaConstants.URL.BASE_URL)
public class ParamController {

    private final BaFileStorageService fileStorage;


    /**
     * Récupérer un fichier/image/document.
     *
     * @param id : l'id du fichier
     * @return {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.DOCUMENT + "/{id}")
    public ResponseEntity<byte[]> loadFile(@PathVariable final String id) {
        byte[] content = fileStorage.get(id);
        DocumentType documentType = detectDocumentType(content);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(documentType.mediaType);
        headers.setContentDisposition(ContentDisposition.inline().filename("pv-" + id + documentType.extension).build());
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    /**
     * Enregistrer un document.
     *
     * @param file fichier a stocker
     * @return identifiant du document stocke
     */
    @PostMapping(BaConstants.URL.DOCUMENT)
    public ResponseEntity<String> uploadFile(@RequestParam("file") final MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            byte[] bytes = file.getBytes();
            DocumentType documentType = detectDocumentType(bytes);
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            boolean extensionOk = filename.endsWith(".pdf") || filename.endsWith(".doc") || filename.endsWith(".docx");
            if (documentType == DocumentType.UNKNOWN || !extensionOk) {
                return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
            String id = fileStorage.save(bytes);
            if (id == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(id, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private DocumentType detectDocumentType(final byte[] bytes) {
        if (isPdf(bytes)) {
            return DocumentType.PDF;
        }
        if (isDoc(bytes)) {
            return DocumentType.DOC;
        }
        if (isZip(bytes)) {
            return DocumentType.DOCX;
        }
        return DocumentType.UNKNOWN;
    }

    private boolean isPdf(final byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46;
    }

    private boolean isDoc(final byte[] bytes) {
        return bytes != null && bytes.length >= 8
                && (bytes[0] & 0xFF) == 0xD0
                && (bytes[1] & 0xFF) == 0xCF
                && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0
                && (bytes[4] & 0xFF) == 0xA1
                && (bytes[5] & 0xFF) == 0xB1
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0xE1;
    }

    private boolean isZip(final byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x50
                && (bytes[1] & 0xFF) == 0x4B
                && ((bytes[2] & 0xFF) == 0x03 || (bytes[2] & 0xFF) == 0x05 || (bytes[2] & 0xFF) == 0x07)
                && ((bytes[3] & 0xFF) == 0x04 || (bytes[3] & 0xFF) == 0x06 || (bytes[3] & 0xFF) == 0x08);
    }

    private enum DocumentType {
        PDF(MediaType.APPLICATION_PDF, ".pdf"),
        DOC(MediaType.parseMediaType("application/msword"), ".doc"),
        DOCX(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"), ".docx"),
        UNKNOWN(MediaType.APPLICATION_OCTET_STREAM, ".bin");

        private final MediaType mediaType;
        private final String extension;

        DocumentType(final MediaType mediaType, final String extension) {
            this.mediaType = mediaType;
            this.extension = extension;
        }
    }
}

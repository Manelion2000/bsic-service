package com.bakouan.app.controller;

import com.bakouan.app.service.BaReportService;
import com.bakouan.app.utils.BaConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(BaConstants.URL.BASE_URL + "/reporting")
public class ReportController {

    private final BaReportService reportService;

    @GetMapping("/autorisations-absence/{id}")
    public ResponseEntity<byte[]> reportAbsence(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("autorisation-absence-" + id + ".pdf").build().toString())
                .body(reportService.printAutorisationAbsence(id));
    }

    @GetMapping(value = BaConstants.URL.HABILITATION + BaConstants.URL.FICHE_HABILITATION + "/{id}")
    public ResponseEntity<byte[]> reportFicheHabilitation(@PathVariable("id") String id) {
        byte[] data = reportService.printFicheHabilitation(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("fiche-habilitation-" + id + ".pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping(value = BaConstants.URL.AUTORISATION_SORTIE + "/{id}")
    public ResponseEntity<byte[]> reportAutorisationSortie(@PathVariable("id") String id) {
        byte[] data = reportService.printAutorisationSortie(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("autorisation-sortie-" + id + ".pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping(value = BaConstants.URL.REPRISE_SERVICE + "/{id}/attestation")
    public ResponseEntity<byte[]> reportAttestationRepriseService(@PathVariable("id") String id) {
        byte[] data = reportService.printAttestationRepriseService(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("attestation-reprise-service-" + id + ".pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

}

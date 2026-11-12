package com.bakouan.app.controller;

import com.bakouan.app.dto.BaRhDashboardDto;
import com.bakouan.app.service.RhDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rh-dashboard")
public class RhDashboardController {
    private final RhDashboardService service;

    @GetMapping
    public BaRhDashboardDto dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String idDepartement, @RequestParam(required = false) String idService) {
        return service.dashboard(debut, fin, idDepartement, idService);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "ABSENCES") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String idDepartement, @RequestParam(required = false) String idService,
            @RequestParam(required = false) String statut, @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registre-rh.csv")
                .body(service.export(type, debut, fin, idDepartement, idService, statut, recherche));
    }
}

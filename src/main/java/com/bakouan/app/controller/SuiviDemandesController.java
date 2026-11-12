package com.bakouan.app.controller;

import com.bakouan.app.dto.SuiviDemandesDtos.*;
import com.bakouan.app.service.SuiviDemandesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/suivi-demandes") @RequiredArgsConstructor
public class SuiviDemandesController {
    private final SuiviDemandesService service;
    @GetMapping("/configuration") public ResponseEntity<Configuration> configuration(){return response(service.configuration());}
    @GetMapping public ResponseEntity<Page> list(@RequestParam(defaultValue="") String module,@RequestParam(required=false) String etat,
            @RequestParam(defaultValue="") String recherche,@RequestParam(defaultValue="") String departementId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return response(service.list(module,etat==null?"EN_ATTENTE":etat,recherche,departementId,page,size));}
    @GetMapping("/{module}/{id}") public ResponseEntity<Detail> detail(@PathVariable String module,@PathVariable String id){return response(service.detail(module,id));}
    private <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}

package com.bakouan.app.controller;

import com.bakouan.app.dto.ExtractionDtos.*;
import com.bakouan.app.service.ExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/extractions")
public class ExtractionController {
    private final ExtractionService service;
    @GetMapping("/configuration") public ResponseEntity<Configuration> configuration(){return ok(service.configuration());}
    @GetMapping("/requetes") public ResponseEntity<Page<RequeteLigne>> queries(@RequestParam(defaultValue="") String recherche,@RequestParam(defaultValue="") String departementId,@RequestParam(defaultValue="false") boolean brouillons,@RequestParam(defaultValue="false") boolean archivees,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.library(recherche,departementId,brouillons,archivees,page,size));}
    @GetMapping("/requetes/{id}") public ResponseEntity<RequeteDetail> query(@PathVariable String id){return ok(service.query(id));}
    @PostMapping("/requetes") public ResponseEntity<RequeteDetail> createQuery(@RequestBody @Valid RequeteEcriture input){return created(service.saveQuery(null,input));}
    @PutMapping("/requetes/{id}") public ResponseEntity<RequeteDetail> updateQuery(@PathVariable String id,@RequestBody @Valid RequeteEcriture input){return ok(service.saveQuery(id,input));}
    @PostMapping("/requetes/{id}/essais") public ResponseEntity<RequeteDetail> test(@PathVariable String id,@RequestBody @Valid Essai input){return ok(service.testQuery(id,input));}
    @PostMapping("/requetes/{id}/verifier") public ResponseEntity<RequeteDetail> certify(@PathVariable String id,@RequestBody @Valid Operation input){return ok(service.certify(id,input));}
    @PostMapping("/requetes/{id}/archiver") public ResponseEntity<RequeteDetail> archive(@PathVariable String id,@RequestBody @Valid Operation input){return ok(service.archive(id,input,false));}
    @PostMapping("/requetes/{id}/restaurer") public ResponseEntity<RequeteDetail> restore(@PathVariable String id,@RequestBody @Valid Operation input){return ok(service.archive(id,input,true));}
    @GetMapping("/demandes") public ResponseEntity<Page<DemandeLigne>> requests(@RequestParam(defaultValue="") String recherche,@RequestParam(required=false) Etat etat,@RequestParam(defaultValue="") String departementId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.requests(recherche,etat,departementId,page,size));}
    @GetMapping("/demandes/{id}") public ResponseEntity<DemandeDetail> request(@PathVariable String id){return ok(service.request(id));}
    @PostMapping("/demandes") public ResponseEntity<DemandeDetail> create(@RequestBody @Valid DemandeEcriture input){return created(service.create(input));}
    @PostMapping("/demandes/{id}/prendre") public ResponseEntity<DemandeDetail> take(@PathVariable String id,@RequestBody @Valid PriseEnCharge input){return ok(service.take(id,input));}
    @PostMapping("/demandes/{id}/executions") public ResponseEntity<DemandeDetail> execute(@PathVariable String id,@RequestBody @Valid Essai input){return ok(service.execute(id,input));}
    @PostMapping("/demandes/{id}/annuler") public ResponseEntity<DemandeDetail> cancel(@PathVariable String id,@RequestBody @Valid Operation input){return ok(service.cancel(id,input));}
    private <T> ResponseEntity<T> ok(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    private <T> ResponseEntity<T> created(T body){return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(body);}
}

package com.bakouan.app.controller;

import com.bakouan.app.dto.PlateformeDtos.*;
import com.bakouan.app.model.PlateformeFiche;
import com.bakouan.app.service.PlateformeCatalogueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/plateformes")
public class PlateformeCatalogueController {
    private final PlateformeCatalogueService service;
    @GetMapping("/configuration") public ResponseEntity<Configuration> configuration(){return response(service.configuration());}
    @GetMapping public ResponseEntity<Page> list(@RequestParam(defaultValue="") String recherche,
        @RequestParam(required=false) PlateformeFiche.Categorie categorie,@RequestParam(required=false) PlateformeFiche.CycleVie cycleVie,
        @RequestParam(required=false) PlateformeFiche.Criticite criticite,@RequestParam(required=false) String departementId,
        @RequestParam(defaultValue="false") boolean archivees,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return response(service.list(recherche,categorie,cycleVie,criticite,departementId,archivees,page,size));
    }
    @GetMapping("/{id}") public ResponseEntity<Detail> detail(@PathVariable String id){return response(service.detail(id));}
    @GetMapping("/{id}/historique") public ResponseEntity<List<Revision>> history(@PathVariable String id,@RequestParam(defaultValue="0") int page){return response(service.history(id,page));}
    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<Detail> create(@RequestBody @Valid Ecriture input){return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.create(input));}
    @PutMapping(value="/{id}",consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<Detail> update(@PathVariable String id,@RequestBody @Valid Ecriture input){return response(service.update(id,input));}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<Detail> createWithManuels(
        @RequestPart("fiche") @Valid Ecriture input,@RequestPart("manuels") @Valid ModificationsManuels changes,
        @RequestPart(value="fichiers",required=false) List<org.springframework.web.multipart.MultipartFile> files){
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.create(input,changes,files));
    }
    @PutMapping(value="/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<Detail> updateWithManuels(@PathVariable String id,
        @RequestPart("fiche") @Valid Ecriture input,@RequestPart("manuels") @Valid ModificationsManuels changes,
        @RequestPart(value="fichiers",required=false) List<org.springframework.web.multipart.MultipartFile> files){return response(service.update(id,input,changes,files));}
    @GetMapping("/{id}/manuels/{manuelId}") public ResponseEntity<byte[]> download(@PathVariable String id,@PathVariable String manuelId){
        var manual=service.downloadManuel(id,manuelId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(manual.getNomFichier(),java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.parseMediaType(manual.getContentType())).contentLength(manual.getTaille()).body(manual.getContenu());
    }
    @PostMapping("/{id}/archiver") public ResponseEntity<Detail> archive(@PathVariable String id,@RequestBody @Valid Operation input){return response(service.archive(id,input,false));}
    @PostMapping("/{id}/restaurer") public ResponseEntity<Detail> restore(@PathVariable String id,@RequestBody @Valid Operation input){return response(service.archive(id,input,true));}
    private <T> ResponseEntity<T> response(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}

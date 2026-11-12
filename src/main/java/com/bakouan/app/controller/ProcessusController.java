
package com.bakouan.app.controller;
import com.bakouan.app.dto.ProcessusDtos.*;import com.bakouan.app.service.ProcessusService;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.multipart.MultipartFile;import java.util.*;
@RestController @RequestMapping("/api/processus") @RequiredArgsConstructor
public class ProcessusController {
 private final ProcessusService service;
 @GetMapping("/dossiers/{id}/document") public ResponseEntity<byte[]> document(@PathVariable String id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("acte-"+id+".pdf").build().toString()).body(service.document(id));}
 @GetMapping("/configuration") public Object configuration(){return service.configuration();}
 @GetMapping("/modeles") public Object models(){return service.modelList();}
 @PostMapping("/modeles") @ResponseStatus(HttpStatus.CREATED) public Object createModel(@Valid @RequestBody Modele input){return service.saveModel(null,input);}
 @PutMapping("/modeles/{id}") public Object saveModel(@PathVariable String id,@Valid @RequestBody Modele input){return service.saveModel(id,input);}
 @PostMapping("/modeles/{id}/publier") public Object publish(@PathVariable String id,@Valid @RequestBody Publication input){return service.publish(id,input);}
 @PostMapping("/modeles/suppression") public Object deleteModels(@Valid @RequestBody Suppression input){return service.deleteModels(input);}
 @GetMapping("/modeles/{id}/simulation") public Object simulate(@PathVariable String id,@RequestParam String agentId){return service.simulate(id,agentId);}
 @GetMapping("/demarches") public Object catalogue(){return service.catalogue();}
 @PostMapping("/demarches") @ResponseStatus(HttpStatus.CREATED) public Object create(@Valid @RequestBody Demarche input){return service.saveDemarche(null,input);}
 @PutMapping("/demarches/{id}") public Object edit(@PathVariable String id,@Valid @RequestBody Demarche input){return service.saveDemarche(id,input);}
 @GetMapping("/dossiers") public Object dossiers(@RequestParam(defaultValue="mes") String vue){return service.dossierList(vue);}
 @GetMapping("/dossiers/{id}") public Object detail(@PathVariable String id){return service.detail(id);}
 @PostMapping("/dossiers") @ResponseStatus(HttpStatus.CREATED) public Object draft(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Dossier input){return service.saveDossier(null,input,key);}
 @PutMapping("/dossiers/{id}") public Object editDraft(@PathVariable String id,@Valid @RequestBody Dossier input){return service.saveDossier(id,input,null);}
 @PostMapping("/dossiers/{id}/{action:soumettre|approuver|rejeter|retirer}") public Object action(@PathVariable String id,@PathVariable String action,@Valid @RequestBody Action input){return service.action(id,action,input);}
 @PostMapping("/dossiers/{id}/pieces") public Object upload(@PathVariable String id,@RequestParam long version,@RequestPart MultipartFile fichier)throws java.io.IOException{return service.addPiece(id,fichier,version);}
 @DeleteMapping("/dossiers/{id}/pieces/{pieceId}") public Object remove(@PathVariable String id,@PathVariable String pieceId,@RequestParam long version){return service.removePiece(id,pieceId,version);}
 @GetMapping("/pieces/{id}") public ResponseEntity<byte[]> piece(@PathVariable String id){var p=service.piece(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.getMime())).header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(p.getNom(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(p.getContenu());}
}

package com.bakouan.app.controller;

import com.bakouan.app.dto.CongeDtos.*;
import com.bakouan.app.dto.CongeRequests;
import com.bakouan.app.service.*;
import com.bakouan.app.model.BaCongePiece;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/conges")
public class CongeController {
    private final CongeConfigurationService configuration;
    private final CongeLedgerService ledger;
    private final CongeDemandeService requests;
    private final CongePlanningService planning;
    private final CongeNotificationService notifications;
    private final CongeExploitationService exploitation;
    private final CongeDelegationService delegations;

    @GetMapping("/configuration") public Map<String,Object> configuration(){return configuration.configuration();}
    @GetMapping("/delegations") public List<com.bakouan.app.dto.BaValidationDelegationDto> delegations(){return delegations.list();}
    @PostMapping("/delegations") public com.bakouan.app.dto.BaValidationDelegationDto delegation(@RequestBody com.bakouan.app.dto.BaValidationDelegationDto input){return delegations.save(input);}
    @DeleteMapping("/delegations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void revoke(@PathVariable String id){delegations.revoke(id);}
    @GetMapping("/politiques") public List<PolitiqueDto> policies(){return configuration.listPolicies();}
    @PostMapping("/politiques") @ResponseStatus(HttpStatus.CREATED) public PolitiqueDto policy(@RequestBody @Valid CongeRequests.PolitiqueRequest input){return configuration.createPolicy(input);}
    @PostMapping("/politiques/{id}/confirmer") public PolitiqueDto confirmPolicy(@PathVariable String id,@RequestBody @Valid CongeRequests.Confirmation input){return configuration.confirmPolicy(id,input);}
    @GetMapping("/calendriers") public List<CalendrierDto> calendars(){return configuration.listCalendars();}
    @PostMapping("/calendriers") @ResponseStatus(HttpStatus.CREATED) public CalendrierDto calendar(@RequestBody @Valid CongeRequests.CalendrierRequest input){return configuration.createCalendar(input);}
    @PostMapping("/calendriers/{id}/confirmer") public CalendrierDto confirmCalendar(@PathVariable String id,@RequestBody @Valid CongeRequests.Confirmation input){return configuration.confirmCalendar(id,input);}
    @GetMapping("/affectations") public List<AffectationDto> assignments(){return configuration.listAssignments();}
    @GetMapping("/referentiels-droits") public Map<String,Object> references(){return configuration.rightsReferences();}
    @PostMapping("/affectations") @ResponseStatus(HttpStatus.CREATED) public AffectationDto assign(@RequestBody @Valid CongeRequests.Affectation input){return configuration.assign(input);}
    @GetMapping("/couverture") public List<CouvertureDto> coverage(){return configuration.listCoverage();}
    @PostMapping("/couverture") @ResponseStatus(HttpStatus.CREATED) public CouvertureDto coverage(@RequestBody @Valid CongeRequests.CouvertureRequest input){return configuration.createCoverage(input);}
    @PostMapping("/couverture/{id}/confirmer") public CouvertureDto confirmCoverage(@PathVariable String id,@RequestBody @Valid CongeRequests.Confirmation input){return configuration.confirmCoverage(id,input);}
    @GetMapping("/mes-soldes") public List<SoldeDto> myBalances(){return ledger.mine();}
    @GetMapping("/soldes") public List<SoldeDto> balances(){return ledger.all();}
    @GetMapping("/droits/{id}/mouvements") public List<MouvementDto> movements(@PathVariable String id){return ledger.history(id);}
    @PostMapping("/droits/periodes") @ResponseStatus(HttpStatus.CREATED) public SoldeDto period(@RequestBody @Valid CongeRequests.PeriodeDroits input){return ledger.openPeriod(input);}
    @PostMapping("/droits/ajustements") public SoldeDto adjust(@RequestBody @Valid CongeRequests.Ajustement input){return ledger.adjust(input);}
    @PostMapping("/droits/reports") public SoldeDto report(@RequestBody @Valid CongeRequests.Report input){return ledger.report(input);}
    @PostMapping("/droits/traiter-echeances") public void process(){ledger.processDue();}
    @PostMapping("/droits/{id}/cloturer") public void close(@PathVariable String id,@RequestBody @Valid CongeRequests.Confirmation input){ledger.close(id,input.getJustification());}
    @PostMapping("/simulation") public SimulationDto simulation(@RequestBody @Valid CongeRequests.Demande input){return requests.simulate(input);}
    @PostMapping("/demandes") @ResponseStatus(HttpStatus.CREATED) public DemandeDto create(@RequestHeader("Idempotency-Key") String key,@RequestBody @Valid CongeRequests.Demande input){return requests.createDraft(input,key);}
    @PutMapping("/demandes/{id}") public DemandeDto update(@PathVariable String id,@RequestBody @Valid CongeRequests.ModificationBrouillon input){return requests.updateDraft(id,input.getDemande(),input.getOperation());}
    @GetMapping("/mes-demandes") public List<DemandeDto> mine(){return requests.mine();}
    @GetMapping("/a-valider") public List<DemandeDto> pending(){return requests.pending();}
    @GetMapping("/administratif") public List<DemandeDto> administrative(){return requests.administrative();}
    @GetMapping("/demandes/{id}") public DemandeDto detail(@PathVariable String id){return requests.get(id);}
    @PostMapping("/demandes/{id}/soumettre") public DemandeDto submit(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.submit(id,op);}
    @PostMapping("/demandes/{id}/approuver") public DemandeDto approve(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.decide(id,op,true);}
    @PostMapping("/demandes/{id}/rejeter") public DemandeDto reject(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.decide(id,op,false);}
    @PostMapping("/demandes/{id}/retirer") public DemandeDto withdraw(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.withdraw(id,op);}
    @PostMapping("/demandes/{id}/annuler") public DemandeDto cancel(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.cancel(id,op);}
    @PostMapping("/demandes/{id}/demander-retour") public DemandeDto askReturn(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.askReturn(id,op);}
    @PostMapping("/demandes/{id}/confirmer-retour") public DemandeDto confirmReturn(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.confirmReturn(id,op,true);}
    @PostMapping("/demandes/{id}/refuser-retour") public DemandeDto refuseReturn(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.confirmReturn(id,op,false);}
    @PostMapping("/demandes/{id}/confirmer-reprise") public DemandeDto resume(@PathVariable String id,@RequestBody @Valid CongeRequests.Operation op){return requests.confirmResumption(id,op);}
    @PutMapping("/demandes/{id}/passation") public PassationDto handover(@PathVariable String id,@RequestBody @Valid CongeRequests.Passation input){return requests.saveHandover(id,input);}
    @PostMapping("/demandes/{id}/passation/accepter") public PassationDto accept(@PathVariable String id){return requests.acceptHandover(id);}
    @GetMapping("/mes-passations") public List<PassationDto> handovers(){return requests.myHandovers();}
    @PostMapping("/demandes/{id}/pieces") public PieceDto upload(@PathVariable String id,@RequestParam("file") MultipartFile file){return requests.upload(id,file);}
    @GetMapping("/pieces/{id}") public ResponseEntity<byte[]> piece(@PathVariable String id){BaCongePiece p=requests.downloadPiece(id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(p.getNomFichier(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(p.getContentType())).body(p.getContenu());}
    @DeleteMapping("/pieces/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removePiece(@PathVariable String id){requests.removePiece(id);}
    @GetMapping("/planning") public List<IndisponibiliteDto> planning(@RequestParam LocalDate debut,@RequestParam LocalDate fin){return planning.planning(debut,fin);}
    @PostMapping("/souhaits") @ResponseStatus(HttpStatus.CREATED) public IndisponibiliteDto wish(@RequestBody @Valid CongeRequests.Souhait input){return planning.wish(input);}
    @DeleteMapping("/souhaits/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removeWish(@PathVariable String id){planning.removeWish(id);}
    @GetMapping("/notifications") public List<NotificationDto> notifications(){return notifications.mine();}
    @PostMapping("/notifications/{id}/lire") public void read(@PathVariable String id){notifications.markRead(id);}
    @PostMapping("/reprise/simuler") public Map<String,Object> simulateImport(@RequestBody @Valid CongeRequests.ImportSolde input){return exploitation.simulateImport(input);}
    @PostMapping("/reprise/confirmer") public List<SoldeDto> importBalances(@RequestBody @Valid CongeRequests.RepriseSoldes input){return exploitation.importBalances(input);}
    @GetMapping("/anomalies") public List<AnomalieDto> anomalies(){return exploitation.anomalies();}
    @PostMapping("/anomalies/{id}/resoudre") public void resolve(@PathVariable String id,@RequestBody @Valid CongeRequests.Confirmation input){exploitation.resolve(id,input);}
    @GetMapping("/synthese") public Map<String,Object> report(@RequestParam LocalDate debut,@RequestParam LocalDate fin){return exploitation.report(debut,fin);}
    @GetMapping("/export") public ResponseEntity<byte[]> export(@RequestParam LocalDate debut,@RequestParam LocalDate fin){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=compteurs-conges.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(exploitation.export(debut,fin));}
    @GetMapping("/demandes/{id}/document") public ResponseEntity<byte[]> document(@PathVariable String id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=autorisation-conge.pdf").contentType(MediaType.APPLICATION_PDF).body(exploitation.document(id));}
}

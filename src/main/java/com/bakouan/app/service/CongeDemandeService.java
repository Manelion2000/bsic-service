package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import com.bakouan.app.dto.CongeDefinitions.*;
import com.bakouan.app.dto.CongeDtos.*;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.BiConsumer;
import static com.bakouan.app.service.CongeSecurity.*;

@Service @Transactional @RequiredArgsConstructor
public class CongeDemandeService {
    private final CongeSecurity security;
    private final CongeConfigurationService configuration;
    private final CongeCalculService calculation;
    private final CongeLedgerService ledger;
    private final CongePlanningService planning;
    private final RhCircuitService circuits;
    private final CongeNotificationService notifications;
    private final BaCongeDemandeRepository requests;
    private final BaCongePieceRepository pieces;
    private final BaCongePassationRepository handovers;
    private final BaCongeOperationRepository operations;
    private final BaUserRepository users;
    private final ObjectMapper json;

    public SimulationDto simulate(CongeRequests.Demande input) {
        BaUser actor=security.require("CONGES_DEMANDER");ledger.lock(actor.getId());
        if(input.getIdBrouillon()!=null){BaCongeDemande d=entity(input.getIdBrouillon());owner(actor,d);state(d,"BROUILLON");}
        return simulate(input,actor,input.getIdBrouillon());
    }
    private SimulationDto simulate(CongeRequests.Demande input,BaUser agent,String ownId) {
        CongeCalculService.dates(input.getDebut(),input.getFin());BaCongeAffectation assignment=configuration.applicable(agent.getId(),input.getTypeCode(),input.getDebut(),input.getFin());
        BaCongePolitique policy=assignment.getPolitique();Politique rule=policy.getRegles();
        Calcul computed=calculation.calculate(input,policy.getId(),rule,assignment.getCalendrier().getId(),assignment.getCalendrier().getRegles());
        List<String> errors=new ArrayList<>();
        if(computed.getQuantite().compareTo(rule.getDureeMin())<0||computed.getQuantite().compareTo(rule.getDureeMax())>0)errors.add("La duree calculee est hors des limites confirmees de la politique.");
        if(input.getDebut().isBefore(LocalDate.now(ZoneId.of(assignment.getCalendrier().getRegles().getFuseau())).plusDays(rule.getPreavisJours())))errors.add("Le delai de prevenance n'est pas respecte.");
        BaCongeDemande parent=null;Map<String,BigDecimal> reusable=new HashMap<>();
        if(input.getRemplaceDemandeId()!=null) {
            parent=entity(input.getRemplaceDemandeId());owner(agent,parent);checkReplacement(parent,ownId);
            if(!Boolean.TRUE.equals(parent.getPolitique().getRegles().getModificationAvantDebutAutorisee())||!parent.getDebut().isAfter(today(parent)))throw blocked("Cette politique n'autorise pas la modification d'un conge deja commence.");
            reusable=ledger.grouped(parent.getImputations());
        }
        List<Alerte> alerts=new ArrayList<>(planning.overlaps(agent,input.getDebut(),input.getFin(),parent==null?ownId:parent.getId(),Boolean.TRUE.equals(rule.getChevauchementBloquant())));
        if(ownId!=null)alerts.removeIf(a->a.regleId().equals(ownId));
        alerts.addAll(planning.capacity(agent,input.getDebut(),input.getFin(),parent==null?ownId:parent.getId()));
        RhCircuitDto circuit=circuits.previewFor("CONGES",agent,Boolean.TRUE.equals(rule.getDgObligatoire()),rule.getPolitiqueRefus(),Boolean.TRUE.equals(rule.getDelegationsAutorisees()));
        if(circuit.getBlocage()!=null)errors.add(circuit.getBlocage());
        if(circuit.getBlocage()==null){BaRhCircuit proposal=circuits.proposalFor("CONGES",agent,Boolean.TRUE.equals(rule.getDgObligatoire()),rule.getPolitiqueRefus(),Boolean.TRUE.equals(rule.getDelegationsAutorisees()));for(RhCircuitDto.Etape step:proposal.getEtapes())if(circuits.candidates(proposal,step).stream().noneMatch(u->security.has(u,"CONGES_VALIDER")))errors.add("Aucun validateur ou delegue de l'etape "+step.getLibelle()+" ne possede la permission CONGES_VALIDER.");}
        List<Imputation> allocations=List.of();
        try{allocations=ledger.allocate(agent,input.getTypeCode(),computed,rule,reusable);}catch(org.springframework.web.server.ResponseStatusException e){errors.add(e.getReason());}
        BigDecimal available=ledger.availableFor(agent,input.getTypeCode(),computed,reusable);
        return new SimulationDto(computed,allocations,available,available.subtract(computed.getQuantite()),alerts,circuit,errors);
    }

    public DemandeDto createDraft(CongeRequests.Demande input,String key) {
        BaUser actor=security.require("CONGES_DEMANDER");ledger.lock(actor.getId());
        String operationKey=key(actor,"CREER",key),fingerprint=fingerprint(input);Optional<BaCongeOperation> old=operations.findByCle(operationKey);
        if(old.isPresent()){if(!old.get().getEmpreinte().equals(fingerprint))throw blocked("Cle de creation deja utilisee avec un autre contenu.");return dto(entity(old.get().getDemandeId()),actor);}
        CongeCalculService.dates(input.getDebut(),input.getFin());configuration.applicable(actor.getId(),input.getTypeCode(),input.getDebut(),input.getFin());
        BaCongeDemande d=new BaCongeDemande();d.setAgent(actor);hydrate(d,input);
        if(input.getRemplaceDemandeId()!=null){BaCongeDemande parent=entity(input.getRemplaceDemandeId());owner(actor,parent);checkReplacement(parent,null);if(!Boolean.TRUE.equals(parent.getPolitique().getRegles().getModificationAvantDebutAutorisee())||!parent.getDebut().isAfter(today(parent)))throw blocked("Modification non autorisee.");d.setRemplaceDemande(parent);d.setRevision(parent.getRevision()+1);}
        d=requests.save(d);audit(d,actor,"BROUILLON_CREE",null,"BROUILLON","");saveOperation(operationKey,actor,"CREER",d.getId(),fingerprint);requests.flush();return dto(d,actor);
    }
    public DemandeDto updateDraft(String id,CongeRequests.Demande input,CongeRequests.Operation operation) {
        return perform(id,operation,"MODIFIER_BROUILLON","CONGES_DEMANDER",(d,a)->{
            owner(a,d);state(d,"BROUILLON");
            if(!Objects.equals(d.getRemplaceDemande()==null?null:d.getRemplaceDemande().getId(),input.getRemplaceDemandeId()))throw bad("Le lien de revision ne peut pas etre modifie.");
            CongeCalculService.dates(input.getDebut(),input.getFin());configuration.applicable(a.getId(),input.getTypeCode(),input.getDebut(),input.getFin());hydrate(d,input);audit(d,a,"BROUILLON_MODIFIE",null,null,operation.getJustification());
        },input);
    }
    public DemandeDto submit(String id,CongeRequests.Operation operation) {
        return perform(id,operation,"SOUMETTRE","CONGES_DEMANDER",(d,a)->{
            owner(a,d);state(d,"BROUILLON");CongeRequests.Demande input=request(d);SimulationDto simulation=simulate(input,a,d.getId());
            if(!simulation.blocages().isEmpty())throw blocked(String.join(" ",simulation.blocages()));
            List<Alerte> blocking=simulation.alertes().stream().filter(Alerte::bloquante).toList();
            if(Boolean.TRUE.equals(configuration.applicable(a.getId(),d.getTypeCode(),d.getDebut(),d.getFin()).getPolitique().getRegles().getChevauchementBloquant())){
                planning.overlaps(a,d.getDebut(),d.getFin(),d.getRemplaceDemande()==null?d.getId():d.getRemplaceDemande().getId(),true).stream().filter(v->!v.regleId().equals(d.getId())).findFirst().ifPresent(v->{throw blocked(v.message());});
            }
            if(!blocking.isEmpty()){
                if(!Boolean.TRUE.equals(operation.getDerogationCouverture()))throw blocked(blocking.get(0).message());
                security.require(a,"CONGES_DEROGATION");justified(operation);d.setDerogationCouverture(operation.getJustification());d.setDerogationPar(a.getId());
            }
            BaCongeAffectation assignment=configuration.applicable(a.getId(),d.getTypeCode(),d.getDebut(),d.getFin());
            d.setPolitique(assignment.getPolitique());d.setCalendrier(assignment.getCalendrier());d.setCalcul(simulation.calcul());d.setImputations(new ArrayList<>(simulation.droits()));
            checkAdministrative(d);
            d.setReservations(deltaReservations(d));d.setCircuit(circuits.freezeFor("CONGES",a,Boolean.TRUE.equals(d.getPolitique().getRegles().getDgObligatoire()),d.getPolitique().getRegles().getPolitiqueRefus(),Boolean.TRUE.equals(d.getPolitique().getRegles().getDelegationsAutorisees())));
            d.setEtat("EN_ATTENTE");d.setSoumiseLe(Instant.now());requests.save(d);ledger.reserve(d,a);audit(d,a,"SOUMISSION","BROUILLON","EN_ATTENTE",operation.getJustification());notifications.event(d,"SOUMISE");
        },null);
    }
    public DemandeDto decide(String id,CongeRequests.Operation operation,boolean approve) {
        return perform(id,operation,approve?"APPROUVER":"REJETER","CONGES_VALIDER",(d,a)->{
            state(d,"EN_ATTENTE");if(operation.getEtape()==null)throw bad("Etape obligatoire.");
            if(d.getAgent().getId().equals(a.getId()))throw forbidden("Toute auto-validation est interdite.");
            String before=d.getEtat();String result=circuits.decide(d.getCircuit(),a,operation.getEtape(),approve,operation.getJustification());
            if("REJETEE".equals(result)){ledger.release(d,a,"Rejet");d.setEtat("REJETEE");expireReleased(d,a);}
            if("VALIDEE".equals(result)) {
                planning.lockCapacity();checkAdministrative(d);if(d.getRemplaceDemande()!=null)checkReplacement(d.getRemplaceDemande(),d.getId());
                if(Boolean.TRUE.equals(d.getPolitique().getRegles().getChevauchementBloquant()))planning.overlaps(d.getAgent(),d.getDebut(),d.getFin(),d.getRemplaceDemande()==null?d.getId():d.getRemplaceDemande().getId(),true).stream().filter(v->!v.regleId().equals(d.getId())).findFirst().ifPresent(v->{throw blocked(v.message());});
                List<Alerte> alerts=planning.capacity(d.getAgent(),d.getDebut(),d.getFin(),d.getRemplaceDemande()==null?d.getId():d.getRemplaceDemande().getId());
                if(alerts.stream().anyMatch(Alerte::bloquante)) {
                    if(!Boolean.TRUE.equals(operation.getDerogationCouverture()))throw blocked(alerts.stream().filter(Alerte::bloquante).findFirst().orElseThrow().message());
                    security.require(a,"CONGES_DEROGATION");justified(operation);d.setDerogationCouverture(operation.getJustification());d.setDerogationPar(a.getId());
                }
                if(d.getRemplaceDemande()!=null) {
                    BaCongeDemande parent=d.getRemplaceDemande();if(!parent.getDebut().isAfter(today(parent)))throw blocked("Le conge original a commence pendant la validation. Intervention RH requise.");
                    ledger.restore(parent,parent.getImputations(),a,"Remplacement de la revision approuvee",d.getId());parent.setEtat("REMPLACEE");audit(parent,a,"REVISION_REMPLACEE","APPROUVEE","REMPLACEE",d.getId());requests.save(parent);
                }
                ledger.debit(d,a);d.setEtat("APPROUVEE");d.setApprouveeLe(Instant.now());
            }
            audit(d,a,approve?"APPROBATION_ETAPE":"REFUS_ETAPE",before,d.getEtat(),operation.getJustification());notifications.event(d,"DECISION_"+operation.getEtape()+"_"+a.getId()+"_"+d.getEtat());
        },null);
    }
    public DemandeDto withdraw(String id,CongeRequests.Operation operation) {
        return perform(id,operation,"RETIRER","CONGES_DEMANDER",(d,a)->{owner(a,d);if(!Set.of("BROUILLON","EN_ATTENTE").contains(d.getEtat()))throw blocked("Seul un brouillon ou une demande en attente peut etre retire.");String old=d.getEtat();if("EN_ATTENTE".equals(old)){ledger.release(d,a,"Retrait");circuits.cancel(d.getCircuit(),a);expireReleased(d,a);}d.setEtat("RETIREE");audit(d,a,"RETRAIT",old,"RETIREE",operation.getJustification());notifications.event(d,"RETIREE");},null);
    }
    public DemandeDto cancel(String id,CongeRequests.Operation operation) {
        return perform(id,operation,"ANNULER","CONGES_ANNULER",(d,a)->{
            state(d,"APPROUVEE");justified(operation);ensureNoPendingReplacement(d);
            if(d.getPolitique().getRegles().getAnnulation()==Annulation.INTERDITE)throw blocked("L'annulation n'est pas autorisee par la politique.");
            if(!d.getDebut().isAfter(today(d)))throw blocked("Un conge commence doit etre traite par un retour anticipe controle ; aucune restitution retroactive automatique.");
            ledger.restore(d,d.getImputations(),a,operation.getJustification(),"annulation:"+d.getId());d.setRestitutions(new ArrayList<>(d.getImputations()));d.setEtat("ANNULEE");expireReleased(d,a);audit(d,a,"ANNULATION","APPROUVEE","ANNULEE",operation.getJustification());notifications.event(d,"ANNULEE");
        },null);
    }
    public DemandeDto askReturn(String id,CongeRequests.Operation operation) {
        return perform(id,operation,"DEMANDER_RETOUR","CONGES_DEMANDER",(d,a)->{
            owner(a,d);state(d,"APPROUVEE");if(!Boolean.TRUE.equals(d.getPolitique().getRegles().getRetourAnticipeAutorise()))throw blocked("Retour anticipe non autorise.");
            if(d.getRepriseConstatee()!=null||"EN_ATTENTE".equals(d.getRetourStatut()))throw blocked("Une reprise ou une demande de retour existe deja.");ensureNoPendingReplacement(d);
            returnDate(d,operation);d.setRepriseDemandee(operation.getDateReprise());d.setPortionRepriseDemandee(operation.getPortionReprise());d.setHeureRepriseDemandee(operation.getHeureReprise());d.setRetourStatut("EN_ATTENTE");audit(d,a,"RETOUR_DEMANDE",null,operation.getDateReprise().toString(),operation.getJustification());notifications.event(d,"RETOUR_DEMANDE");
        },null);
    }
    public DemandeDto confirmReturn(String id,CongeRequests.Operation operation,boolean approve) {
        return perform(id,operation,approve?"CONFIRMER_RETOUR":"REFUSER_RETOUR","CONGES_ANNULER",(d,a)->{
            state(d,"APPROUVEE");if(!"EN_ATTENTE".equals(d.getRetourStatut()))throw blocked("Aucun retour ne doit etre traite.");justified(operation);
            if(!approve){d.setRetourStatut("REJETEE");audit(d,a,"RETOUR_REJETE",null,null,operation.getJustification());notifications.event(d,"RETOUR_REJETE");return;}
            LocalDate actual=operation.getDateReprise()==null?d.getRepriseDemandee():operation.getDateReprise();
            if(actual.isAfter(today(d)))throw bad("Une reprise effective ne peut pas etre confirmee dans le futur.");
            CongeRequests.Operation selected=new CongeRequests.Operation();selected.setDateReprise(actual);selected.setPortionReprise(operation.getPortionReprise()==null?d.getPortionRepriseDemandee():operation.getPortionReprise());selected.setHeureReprise(operation.getHeureReprise()==null?d.getHeureRepriseDemandee():operation.getHeureReprise());returnDate(d,selected);
            CongeRequests.Demande remaining=request(d);remaining.setDebut(actual);remaining.setPortionDebut(selected.getPortionReprise());remaining.setHeureDebut(selected.getHeureReprise());
            Calcul rest=calculation.calculate(remaining,d.getPolitique().getId(),d.getPolitique().getRegles(),d.getCalendrier().getId(),d.getCalendrier().getRegles());
            List<Imputation> restored=restorationLines(d,rest);ledger.restore(d,restored,a,operation.getJustification(),"retour:"+d.getId());d.setRestitutions(restored);d.setRepriseConstatee(actual);d.setRetourStatut("VALIDEE");expireReleased(d,a);audit(d,a,"RETOUR_CONFIRME",null,actual.toString(),operation.getJustification());notifications.event(d,"RETOUR_CONFIRME");
        },null);
    }
    public DemandeDto confirmResumption(String id,CongeRequests.Operation operation) {
        return perform(id,operation,"CONFIRMER_REPRISE","CONGES_ANNULER",(d,a)->{
            state(d,"APPROUVEE");justified(operation);if(d.getRepriseConstatee()!=null)throw blocked("Reprise deja confirmee.");
            if(operation.getDateReprise()==null||!operation.getDateReprise().isAfter(d.getFin())||operation.getDateReprise().isAfter(today(d)))throw bad("Renseignez une date effective de reprise posterieure a la fin et non future.");
            d.setRepriseConstatee(operation.getDateReprise());audit(d,a,"REPRISE_CONFIRMEE",null,operation.getDateReprise().toString(),operation.getJustification());notifications.event(d,"REPRISE_CONFIRMEE");
        },null);
    }

    public List<DemandeDto> mine(){BaUser a=security.current();return requests.findByAgentIdAndStatutOrderByCreatedDateDesc(a.getId(),EStatut.A).stream().map(d->dto(d,a)).toList();}
    public List<DemandeDto> pending(){BaUser a=security.require("CONGES_VALIDER");return requests.findByStatutAndEtatOrderByCreatedDateDesc(EStatut.A,"EN_ATTENTE").stream().filter(d->circuits.canDecide(d.getCircuit(),a)).map(d->dto(d,a)).toList();}
    public List<DemandeDto> administrative(){BaUser a=security.current();if(!security.has(a,"CONGES_ANNULER")&&!security.has(a,"CONGES_AUDITER")&&!security.has(a,"CONGES_DROITS"))throw forbidden("Consultation administrative non autorisee.");return requests.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->dto(d,a)).toList();}
    public DemandeDto get(String id){BaUser actor=security.current();BaCongeDemande d=entity(id);read(actor,d);return dto(d,actor);}
    public BaCongeDemande readEntity(String id){BaUser actor=security.current();BaCongeDemande d=entity(id);read(actor,d);return d;}

    public PassationDto saveHandover(String id,CongeRequests.Passation input) {
        BaUser actor=security.require("CONGES_DEMANDER");BaCongeDemande d=locked(id);owner(actor,d);
        if(!Set.of("BROUILLON","EN_ATTENTE","APPROUVEE").contains(d.getEtat()))throw blocked("Passation non modifiable dans cet etat.");
        BaUser replacement=input.getRemplacantId()==null?null:users.findById(input.getRemplacantId()).orElseThrow(()->missing("Remplacant introuvable."));
        if(replacement!=null&&(!available(replacement)||replacement.getId().equals(actor.getId())))throw bad("Le remplacant doit etre un autre agent disponible.");
        planning.lockCapacity();
        if(replacement!=null&&!planning.overlaps(replacement,d.getDebut(),d.getFin(),d.getId(),true).isEmpty())throw blocked("Le remplacant possede une indisponibilite ou un remplacement obligatoire sur cette periode.");
        BaCongePassation h=handovers.findByDemandeId(id).orElseGet(BaCongePassation::new);h.setDemande(d);h.setRemplacant(replacement);h.setTachesEtEcheances(input.getTachesEtEcheances());h.setContactsUtiles(input.getContactsUtiles());h.setPriseEnChargeLe(null);handovers.save(h);audit(d,actor,"PASSATION_MODIFIEE",null,replacement==null?null:replacement.getId(),"");requests.saveAndFlush(d);return handoverDto(h);
    }
    public PassationDto acceptHandover(String id) {
        BaUser actor=security.current();BaCongeDemande d=locked(id);
        BaCongePassation h=handovers.findByDemandeId(id).orElseThrow(()->missing("Passation introuvable."));if(h.getRemplacant()==null||!h.getRemplacant().getId().equals(actor.getId()))throw forbidden("Seul le remplacant designe peut confirmer la prise en charge.");
        if(h.getPriseEnChargeLe()==null){h.setPriseEnChargeLe(Instant.now());handovers.save(h);audit(d,actor,"PASSATION_ACCEPTEE",null,actor.getId(),"");requests.saveAndFlush(d);}return handoverDto(h);
    }
    public List<PassationDto> myHandovers(){BaUser a=security.current();return handovers.findAll().stream().filter(h->h.getRemplacant()!=null&&a.getId().equals(h.getRemplacant().getId())&&Set.of("BROUILLON","EN_ATTENTE","APPROUVEE").contains(h.getDemande().getEtat())).map(this::handoverDto).toList();}
    public PieceDto upload(String id,MultipartFile file) {
        BaUser actor=security.require("CONGES_DEMANDER");BaCongeDemande d=locked(id);owner(actor,d);state(d,"BROUILLON");
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw bad("Le justificatif doit contenir entre 1 octet et 5 Mo.");
        try {
            byte[] bytes=file.getBytes();String type;
            if(bytes.length>=5&&new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))type="application/pdf";
            else if(bytes.length>=8&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71&&bytes[4]==13&&bytes[5]==10&&bytes[6]==26&&bytes[7]==10)type="image/png";
            else if(bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)type="image/jpeg";
            else throw bad("Formats de justificatif acceptes : PDF, PNG et JPEG.");
            String name=Objects.toString(file.getOriginalFilename(),"justificatif").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");if(name.length()>150)name=name.substring(name.length()-150);
            BaCongePiece p=new BaCongePiece();p.setDemande(d);p.setNomFichier(name);p.setContentType(type);p.setTaille(bytes.length);p.setContenu(bytes);p=pieces.save(p);audit(d,actor,"JUSTIFICATIF_AJOUTE",null,p.getId(),"");requests.saveAndFlush(d);return pieceDto(p);
        }catch(java.io.IOException e){throw bad("Justificatif illisible.");}
    }
    public BaCongePiece downloadPiece(String id) {
        BaUser actor=security.current();BaCongePiece p=pieces.findById(id).filter(v->v.getStatut()==EStatut.A).orElseThrow(()->missing("Justificatif introuvable."));
        if(!p.getDemande().getAgent().getId().equals(actor.getId())&&!security.has(actor,"CONGES_PIECES"))throw forbidden("Acces aux justificatifs sensibles interdit.");return p;
    }
    public void removePiece(String id){BaUser actor=security.require("CONGES_DEMANDER");BaCongePiece p=pieces.findById(id).filter(v->v.getStatut()==EStatut.A).orElseThrow(()->missing("Justificatif introuvable."));BaCongeDemande d=locked(p.getDemande().getId());owner(actor,d);state(d,"BROUILLON");p.setStatut(EStatut.D);pieces.save(p);audit(d,actor,"JUSTIFICATIF_RETIRE",id,null,"");requests.saveAndFlush(d);}

    private DemandeDto perform(String id,CongeRequests.Operation operation,String action,String permission,BiConsumer<BaCongeDemande,BaUser> body,Object extra) {
        BaUser actor=security.require(permission);BaCongeDemande d=locked(id);
        String key=key(actor,action,operation.getCle()),fingerprint=fingerprint(List.of(id,operation,extra==null?"":extra));
        Optional<BaCongeOperation> previous=operations.findByCle(key);
        if(previous.isPresent()){if(!previous.get().getEmpreinte().equals(fingerprint)||!id.equals(previous.get().getDemandeId()))throw blocked("Cle d'operation deja utilisee avec un autre contenu.");read(actor,d);return dto(d,actor);}
        if(operation.getVersion()==null||operation.getVersion()!=d.getLockVersion())throw blocked("La version de la demande a change. Actualisez avant de statuer.");
        body.accept(d,actor);saveOperation(key,actor,action,id,fingerprint);d=requests.saveAndFlush(d);return dto(d,actor);
    }
    private List<Imputation> deltaReservations(BaCongeDemande d) {
        if(d.getRemplaceDemande()==null)return new ArrayList<>(d.getImputations());
        Map<String,BigDecimal> reusable=ledger.grouped(d.getRemplaceDemande().getImputations());List<Imputation> result=new ArrayList<>();
        for(Imputation i:d.getImputations()){BigDecimal already=reusable.getOrDefault(i.getDroitId(),BigDecimal.ZERO).min(i.getQuantite());reusable.put(i.getDroitId(),reusable.getOrDefault(i.getDroitId(),BigDecimal.ZERO).subtract(already));BigDecimal extra=i.getQuantite().subtract(already);if(extra.signum()>0){Imputation line=copy(i);line.setQuantite(extra);result.add(line);}}
        return result;
    }
    private void checkAdministrative(BaCongeDemande d) {
        Politique p=d.getPolitique().getRegles();if(Boolean.TRUE.equals(p.getJustificatifRequis())&&pieces.countByDemandeIdAndStatut(d.getId(),EStatut.A)==0)throw blocked("Un justificatif est requis par la politique.");
        BaCongePassation h=handovers.findByDemandeId(d.getId()).orElse(null);
        if(Boolean.TRUE.equals(p.getPassationRequise())&&h==null)throw blocked("Une fiche de passation est requise.");
        if(Boolean.TRUE.equals(p.getRemplacementRequis())&&(h==null||h.getRemplacant()==null||h.getPriseEnChargeLe()==null))throw blocked("Le remplacant doit etre designe et confirmer sa prise en charge.");
        if(h!=null&&h.getRemplacant()!=null&&(!available(h.getRemplacant())||!planning.overlaps(h.getRemplacant(),d.getDebut(),d.getFin(),d.getId(),true).isEmpty()))throw blocked("Le remplacant n'est plus disponible sur cette periode.");
    }
    private void checkReplacement(BaCongeDemande parent,String childId){state(parent,"APPROUVEE");if(parent.getRepriseConstatee()!=null)throw blocked("Une demande avec reprise constatee ne peut pas etre remplacee.");if(requests.findByRemplaceDemandeIdAndStatut(parent.getId(),EStatut.A).stream().anyMatch(d->!Objects.equals(childId,d.getId())&&Set.of("BROUILLON","EN_ATTENTE").contains(d.getEtat())))throw blocked("Une modification de ce conge est deja en cours.");}
    private void ensureNoPendingReplacement(BaCongeDemande d){if(requests.findByRemplaceDemandeIdAndStatut(d.getId(),EStatut.A).stream().anyMatch(c->Set.of("BROUILLON","EN_ATTENTE").contains(c.getEtat())))throw blocked("Retirez d'abord la modification en attente.");}
    private void expireReleased(BaCongeDemande d,BaUser actor){for(String id:ledger.grouped(d.getImputations()).keySet()){BaCongeDroit r=ledger.right(id);if(r.getExpiration()!=null&&r.getExpiration().isBefore(today(d)))ledger.expire(r,actor.getId());}}
    private void returnDate(BaCongeDemande d,CongeRequests.Operation op){if(op.getDateReprise()==null||op.getPortionReprise()==null||op.getDateReprise().isBefore(d.getDebut())||op.getDateReprise().isAfter(d.getFin()))throw bad("Renseignez une date et une portion de reprise comprises dans le conge.");if(d.getPolitique().getRegles().getUnite()==Unite.HEURE&&op.getHeureReprise()==null)throw bad("L'heure de reprise est obligatoire pour un conge horaire.");}
    private List<Imputation> restorationLines(BaCongeDemande d,Calcul remaining){List<Imputation> result=new ArrayList<>();for(JourCalcule day:remaining.getJours()){BigDecimal quantity=day.getQuantite();List<Imputation> original=d.getImputations().stream().filter(i->i.getJour().equals(day.getDate())).toList();for(int n=original.size()-1;n>=0&&quantity.signum()>0;n--){Imputation i=original.get(n),r=copy(i);r.setQuantite(i.getQuantite().min(quantity));if(r.getQuantite().signum()>0)result.add(r);quantity=quantity.subtract(r.getQuantite());}if(quantity.signum()>0)throw blocked("Le retour propose excede les droits initialement debites.");}return result;}
    private Imputation copy(Imputation i){Imputation r=new Imputation();r.setDroitId(i.getDroitId());r.setJour(i.getJour());r.setQuantite(i.getQuantite());r.setExpiration(i.getExpiration());r.setOrigine(i.getOrigine());return r;}
    private String key(BaUser actor,String action,String client){if(client==null||client.isBlank()||client.length()>150)throw bad("Cle d'idempotence obligatoire (150 caracteres maximum).");return "operation:"+CongeLedgerService.hash(actor.getId()+"|"+action+"|"+client);}
    private String fingerprint(Object input){try{return CongeLedgerService.hash(json.writeValueAsString(input));}catch(Exception e){throw new IllegalStateException(e);}}
    private void saveOperation(String key,BaUser actor,String action,String id,String fingerprint){BaCongeOperation op=new BaCongeOperation();op.setCle(key);op.setAuteurId(actor.getId());op.setAction(action);op.setDemandeId(id);op.setEmpreinte(fingerprint);operations.save(op);}
    private void owner(BaUser actor,BaCongeDemande d){if(!actor.getId().equals(d.getAgent().getId()))throw forbidden("Seul le demandeur peut effectuer cette operation.");}
    private void read(BaUser actor,BaCongeDemande d){if(!actor.getId().equals(d.getAgent().getId())&&!security.global(actor)&&!security.has(actor,"CONGES_ANNULER")&&!circuits.participant(d.getCircuit(),actor))throw forbidden("Demande hors de votre perimetre.");}
    private void state(BaCongeDemande d,String state){if(!state.equals(d.getEtat()))throw blocked("Action impossible dans l'etat "+d.getEtat()+".");}
    private void justified(CongeRequests.Operation op){if(op.getJustification()==null||op.getJustification().isBlank())throw bad("Justification obligatoire.");}
    private BaCongeDemande locked(String id){String agentId=requests.findAgentId(id).orElseThrow(()->missing("Demande introuvable."));ledger.lock(agentId);return requests.findLockedById(id).orElseThrow(()->missing("Demande introuvable."));}
    private BaCongeDemande entity(String id){return requests.findById(id).filter(d->d.getStatut()==EStatut.A).orElseThrow(()->missing("Demande introuvable."));}
    private LocalDate today(BaCongeDemande d){return d.getCalendrier()==null?LocalDate.now():LocalDate.now(ZoneId.of(d.getCalendrier().getRegles().getFuseau()));}
    private void hydrate(BaCongeDemande d,CongeRequests.Demande i){d.setTypeCode(i.getTypeCode());d.setDebut(i.getDebut());d.setFin(i.getFin());d.setPortionDebut(i.getPortionDebut());d.setPortionFin(i.getPortionFin());d.setHeureDebut(i.getHeureDebut());d.setHeureFin(i.getHeureFin());d.setMotif(i.getMotif());}
    private CongeRequests.Demande request(BaCongeDemande d){CongeRequests.Demande i=new CongeRequests.Demande();i.setTypeCode(d.getTypeCode());i.setDebut(d.getDebut());i.setFin(d.getFin());i.setPortionDebut(d.getPortionDebut());i.setPortionFin(d.getPortionFin());i.setHeureDebut(d.getHeureDebut());i.setHeureFin(d.getHeureFin());i.setMotif(d.getMotif());i.setRemplaceDemandeId(d.getRemplaceDemande()==null?null:d.getRemplaceDemande().getId());return i;}
    private void audit(BaCongeDemande d,BaUser actor,String action,String before,String after,String reason){d.getHistorique().add(new ActionAudit(action,actor.getId(),Objects.toString(actor.getFonction(),null),Instant.now(),Objects.toString(reason,""),before,after));}
    public DemandeDto dto(BaCongeDemande d,BaUser actor) {
        boolean own=actor.getId().equals(d.getAgent().getId()),sensitive=own||security.has(actor,"CONGES_PIECES");Set<String> actions=new LinkedHashSet<>();
        if(own&&security.has(actor,"CONGES_DEMANDER")){
            if("BROUILLON".equals(d.getEtat()))actions.addAll(List.of("MODIFIER","SOUMETTRE","JUSTIFICATIF","PASSATION"));
            if(Set.of("BROUILLON","EN_ATTENTE").contains(d.getEtat()))actions.add("RETIRER");
            if("APPROUVEE".equals(d.getEtat())){actions.add("PASSATION");if(Boolean.TRUE.equals(d.getPolitique().getRegles().getModificationAvantDebutAutorisee())&&d.getDebut().isAfter(today(d)))actions.add("NOUVELLE_REVISION");if(Boolean.TRUE.equals(d.getPolitique().getRegles().getRetourAnticipeAutorise())&&d.getRepriseConstatee()==null&&!"EN_ATTENTE".equals(d.getRetourStatut()))actions.add("DEMANDER_RETOUR");}
        }
        RhCircuitDto circuit=circuits.toDto(d.getCircuit(),actor);circuit=json.convertValue(circuit,RhCircuitDto.class);
        if("EN_ATTENTE".equals(d.getEtat())&&security.has(actor,"CONGES_VALIDER")&&circuit.isPeutDecider())actions.addAll(List.of("APPROUVER","REJETER"));else circuit.setPeutDecider(false);
        if("APPROUVEE".equals(d.getEtat())&&security.has(actor,"CONGES_ANNULER")){
            if(d.getDebut().isAfter(today(d))&&d.getPolitique().getRegles().getAnnulation()!=Annulation.INTERDITE)actions.add("ANNULER");
            if("EN_ATTENTE".equals(d.getRetourStatut()))actions.addAll(List.of("CONFIRMER_RETOUR","REFUSER_RETOUR"));
            if(d.getRepriseConstatee()==null&&d.getFin().isBefore(today(d)))actions.add("CONFIRMER_REPRISE");
        }
        if(d.getApprouveeLe()!=null)actions.add("DOCUMENT");
        if(!sensitive){circuit.getEtapes().forEach(s->s.setCommentaire(null));circuit.setHistorique(circuit.getHistorique().stream().map(h->new RhCircuitDto.Decision(h.etape(),h.type(),h.decision(),h.idSignataire(),h.nomSignataire(),h.date(),null,h.fonctionSignataire(),h.idDelegant(),h.idDelegation())).toList());}
        List<ActionAudit> history=sensitive?d.getHistorique():d.getHistorique().stream().map(h->new ActionAudit(h.action(),h.auteurId(),h.fonction(),h.date(),null,h.ancienneValeur(),h.nouvelleValeur())).toList();
        BaCongePassation h=handovers.findByDemandeId(d.getId()).orElse(null);boolean handoverAllowed=own||security.global(actor)||h!=null&&h.getRemplacant()!=null&&h.getRemplacant().getId().equals(actor.getId());
        String situation=d.getDebut().isAfter(today(d))?"A_VENIR":d.getFin().isBefore(today(d))?"PERIODE_TERMINEE":"EN_COURS";
        if(d.getRepriseConstatee()!=null)situation="REPRISE_CONFIRMEE";else if(d.getPolitique()!=null&&Boolean.TRUE.equals(d.getPolitique().getRegles().getConfirmationRepriseRequise())&&d.getFin().isBefore(today(d)))situation="REPRISE_A_CONFIRMER";
        return new DemandeDto(d.getId(),d.getLockVersion(),d.getRevision(),d.getAgent().getId(),CongeConfigurationService.name(d.getAgent()),d.getTypeCode(),d.getPolitique()==null?d.getTypeCode():d.getPolitique().getLibelle(),d.getEtat(),situation,d.getDebut(),d.getFin(),d.getPortionDebut(),d.getPortionFin(),d.getHeureDebut(),d.getHeureFin(),sensitive?d.getMotif():null,d.getRemplaceDemande()==null?null:d.getRemplaceDemande().getId(),d.getRepriseConstatee(),d.getRepriseDemandee(),d.getPortionRepriseDemandee(),d.getHeureRepriseDemandee(),d.getRetourStatut(),d.getCalcul(),d.getImputations(),circuit,history,sensitive?pieces.findByDemandeIdAndStatut(d.getId(),EStatut.A).stream().map(this::pieceDto).toList():List.of(),handoverAllowed&&h!=null?handoverDto(h):null,actions,d.getSoumiseLe(),d.getApprouveeLe());
    }
    private PieceDto pieceDto(BaCongePiece p){return new PieceDto(p.getId(),p.getNomFichier(),p.getContentType(),p.getTaille(),p.getCreatedDate());}
    private PassationDto handoverDto(BaCongePassation h){return new PassationDto(h.getId(),h.getDemande().getId(),CongeConfigurationService.name(h.getDemande().getAgent()),h.getDemande().getDebut(),h.getDemande().getFin(),h.getRemplacant()==null?null:h.getRemplacant().getId(),h.getRemplacant()==null?null:CongeConfigurationService.name(h.getRemplacant()),h.getTachesEtEcheances(),h.getContactsUtiles(),h.getPriseEnChargeLe());}
}

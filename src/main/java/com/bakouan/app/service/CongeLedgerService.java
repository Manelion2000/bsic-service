package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import com.bakouan.app.dto.CongeDefinitions.*;
import com.bakouan.app.dto.CongeDtos.*;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

@Service @Transactional @RequiredArgsConstructor
public class CongeLedgerService {
    private final CongeSecurity security;
    private final CongeCalculService calculation;
    private final BaUserRepository users;
    private final BaCongeAffectationRepository assignments;
    private final BaCongeDroitRepository rights;
    private final BaCongeMouvementRepository movements;
    private final BaCongeEcheanceRepository accruals;
    private final BaCongeAnomalieRepository anomalies;
    private final BaCongeDemandeRepository requests;
    private final BaLogService logs;
    private final jakarta.persistence.EntityManager entityManager;

    public record Balance(BigDecimal acquis,BigDecimal reserves,BigDecimal debites) {
        public BigDecimal disponible(){return acquis.subtract(reserves).subtract(debites);}
    }
    public Balance balance(BaCongeDroit right) {
        BigDecimal credits=BigDecimal.ZERO,reserved=BigDecimal.ZERO,debited=BigDecimal.ZERO;
        for(BaCongeMouvement m:movements.findByDroitIdOrderByDateOperationAsc(right.getId())) {
            switch(m.getNature()) {
                case "ATTRIBUTION","ACQUISITION","REPORT_ENTRANT","AJUSTEMENT","REPRISE_SOLDE" -> credits=credits.add(m.getQuantite());
                case "EXPIRATION","REPORT_SORTANT" -> credits=credits.subtract(m.getQuantite());
                case "RESERVATION" -> reserved=reserved.add(m.getQuantite());
                case "LIBERATION" -> reserved=reserved.subtract(m.getQuantite());
                case "DEBIT" -> debited=debited.add(m.getQuantite());
                case "RESTITUTION" -> debited=debited.subtract(m.getQuantite());
                default -> throw new IllegalStateException("Mouvement de conges inconnu");
            }
        }
        if(reserved.signum()<0||debited.signum()<0)throw new IllegalStateException("Compteur de conges incoherent");
        return new Balance(credits,reserved,debited);
    }
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<SoldeDto> mine(){return balances(security.current());}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<SoldeDto> all(){security.require("CONGES_DROITS");return rights.findByStatutOrderByOrigineDebutAsc(EStatut.A).stream().map(this::dto).toList();}
    public List<SoldeDto> balances(BaUser agent){return rights.findByAgentIdAndStatutOrderByOrigineDebutAsc(agent.getId(),EStatut.A).stream().map(this::dto).toList();}
    public List<MouvementDto> history(String id) {
        BaUser actor=security.current();BaCongeDroit right=right(id);
        if(!actor.getId().equals(right.getAgent().getId())&&!security.has(actor,"CONGES_DROITS")&&!security.has(actor,"CONGES_AUDITER"))throw forbidden("Compteur hors de votre perimetre.");
        return movements.findByDroitIdOrderByDateOperationAsc(id).stream().map(m->new MouvementDto(m.getId(),id,m.getDemande()==null?null:m.getDemande().getId(),m.getNature(),m.getQuantite(),m.getSource(),m.getJustification(),m.getAuteurId(),m.getDateOperation())).toList();
    }

    public SoldeDto openPeriod(CongeRequests.PeriodeDroits request) {
        BaUser actor=security.require("CONGES_DROITS");BaCongeAffectation assignment=assignment(request.getAffectationId());lock(assignment.getAgent().getId());
        CongeCalculService.dates(request.getDebut(),request.getFin());checkPeriod(assignment,request.getDebut(),request.getFin(),request.getExpiration());
        String key="periode:"+hash(actor.getId()+"|"+request.getCle());
        Optional<BaCongeDroit> existing=rights.findByCleOrigine(key);
        if(existing.isPresent()) {
            BaCongeDroit r=existing.get();if(!r.getAffectation().getId().equals(assignment.getId())||!r.getOrigineDebut().equals(request.getDebut())||!r.getOrigineFin().equals(request.getFin())||!Objects.equals(r.getExpiration(),request.getExpiration()))throw blocked("La cle a deja ete utilisee pour une autre periode.");
            return dto(r);
        }
        if(rights.findByAgentIdAndStatutOrderByOrigineDebutAsc(assignment.getAgent().getId(),EStatut.A).stream().anyMatch(r->r.getOrigineDroitId()==null&&r.getTypeCode().equals(assignment.getPolitique().getTypeCode())&&CongeConfigurationService.overlap(r.getOrigineDebut(),r.getOrigineFin(),request.getDebut(),request.getFin())))throw blocked("Une periode de droits recouvre deja ces dates. Utilisez un ajustement trace.");
        BaCongeDroit right=newRight(assignment,request.getDebut(),request.getFin(),request.getExpiration(),key);
        right.setSource(request.getSource());right.setJustification(request.getJustification());rights.save(right);
        Politique policy=assignment.getPolitique().getRegles();
        if(policy.getAttribution()==Attribution.DOTATION) {
            BigDecimal quantity=prorated(policy.getDotation(),request.getDebut(),request.getFin(),assignment);
            schedule(right,request.getDebut(),quantity,"dotation:"+right.getId());
        } else {
            int months=switch(policy.getFrequence()){case MENSUELLE->1;case TRIMESTRIELLE->3;case ANNUELLE->12;};
            for(LocalDate start=request.getDebut();!start.isAfter(request.getFin());start=start.plusMonths(months)) {
                LocalDate fullEnd=start.plusMonths(months).minusDays(1),end=fullEnd.isAfter(request.getFin())?request.getFin():fullEnd;
                BigDecimal quantity=prorated(policy.getAcquisitionParEcheance(),start,fullEnd,assignment, end);
                schedule(right,end,quantity,"acquisition:"+right.getId()+":"+start);
            }
        }
        executeDue(right,actor.getId());return dto(right);
    }
    private BigDecimal prorated(BigDecimal quantity,LocalDate start,LocalDate end,BaCongeAffectation assignment){return prorated(quantity,start,end,assignment,end);}
    private BigDecimal prorated(BigDecimal quantity,LocalDate start,LocalDate end,BaCongeAffectation assignment,LocalDate coverageEnd) {
        LocalDate activeStart=assignment.getDateEntree()==null?assignment.getDebut():assignment.getDateEntree();
        if(assignment.getDebut().isAfter(activeStart))activeStart=assignment.getDebut();
        LocalDate activeEnd=assignment.getDateSortie()==null?coverageEnd:assignment.getDateSortie();
        if(assignment.getFin()!=null&&assignment.getFin().isBefore(activeEnd))activeEnd=assignment.getFin();
        if(coverageEnd.isBefore(activeEnd))activeEnd=coverageEnd;
        return calculation.prorata(quantity,start,end,activeStart,activeEnd,assignment.getPolitique().getRegles(),assignment.getCalendrier().getRegles());
    }
    private void schedule(BaCongeDroit right,LocalDate date,BigDecimal quantity,String key) {
        if(quantity.signum()<=0)return;BaCongeEcheance item=new BaCongeEcheance();item.setDroit(right);item.setDateAcquisition(date);item.setQuantite(quantity);item.setCle(key);accruals.save(item);
    }
    public SoldeDto adjust(CongeRequests.Ajustement request) {
        BaUser actor=security.require("CONGES_DROITS");BaCongeDroit right=lockedRight(request.getDroitId());
        if(request.getQuantite().signum()==0)throw bad("Un ajustement nul n'est pas un mouvement.");
        String key="ajustement:"+hash(actor.getId()+"|"+request.getCle());
        Optional<BaCongeMouvement> old=movements.findByCle("mouvement:"+hash(key));
        if(old.isPresent()){sameMovement(old.get(),right,"AJUSTEMENT",request.getQuantite(),request.getSource());return dto(right);}
        if(right.isCloturee())throw blocked("La periode est cloturee.");
        BigDecimal after=balance(right).disponible().add(request.getQuantite());
        if(after.compareTo(anticipation(right).negate())<0)throw blocked("Cet ajustement rendrait les droits reserves ou debites insuffisants.");
        if(request.getQuantite().signum()>0&&capped(right,request.getQuantite()).compareTo(request.getQuantite())<0)throw blocked("Le plafond d'accumulation serait depasse.");
        move(right,null,"AJUSTEMENT",request.getQuantite(),key,request.getSource(),request.getJustification(),actor.getId());return dto(right);
    }
    public SoldeDto report(CongeRequests.Report request) {
        BaUser actor=security.require("CONGES_DROITS");BaCongeDroit source=lockedRight(request.getSourceDroitId()),target=right(request.getCibleDroitId());
        String key="report:"+hash(actor.getId()+"|"+request.getCle());
        Optional<BaCongeDroit> previous=rights.findByCleOrigine(key);if(previous.isPresent()){
            BaCongeDroit r=previous.get();if(!Objects.equals(r.getOrigineDroitId(),source.getId())||!r.getAffectation().getId().equals(target.getAffectation().getId()))throw blocked("Cle de report deja utilisee.");return dto(r);
        }
        Politique policy=source.getPolitique().getRegles();
        if(source.isCloturee()||target.isCloturee())throw blocked("Une periode source ou cible est cloturee.");
        if(!Boolean.TRUE.equals(policy.getReportAutorise()))throw blocked("Report non autorise par la politique source.");
        if(!source.getAgent().getId().equals(target.getAgent().getId())||!source.getTypeCode().equals(target.getTypeCode())||source.getPolitique().getRegles().getUnite()!=target.getPolitique().getRegles().getUnite())throw bad("Le report doit conserver l'agent, le type et l'unite.");
        if(!target.getDebutDisponibilite().isAfter(source.getFinDisponibilite())||source.getFinDisponibilite().isAfter(today(source)))throw bad("Le report concerne une periode source terminee et une periode cible ulterieure.");
        Balance balance=balance(source);if(balance.reserves().signum()>0)throw blocked("Des droits de cette periode sont reserves. Traitez les demandes avant le report.");
        if(source.getExpiration()!=null&&source.getExpiration().isBefore(today(source)))throw blocked("Les droits source ont expire.");
        BigDecimal already=movements.findByDroitIdOrderByDateOperationAsc(source.getId()).stream().filter(m->"REPORT_SORTANT".equals(m.getNature())).map(BaCongeMouvement::getQuantite).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal quantity=balance.disponible().max(BigDecimal.ZERO).min(policy.getPlafondReport().subtract(already).max(BigDecimal.ZERO));
        // Le report est un transfert de credits existants ; il n'augmente pas l'accumulation.
        if(quantity.signum()==0)throw blocked("Aucun droit libre reportable dans les plafonds confirmes.");
        LocalDate expiration=target.getDebutDisponibilite().plusDays(policy.getExpirationReportJours()-1L);
        if(target.getExpiration()!=null&&target.getExpiration().isBefore(expiration))expiration=target.getExpiration();
        if(expiration.isBefore(today(target)))throw blocked("L'echeance confirmee du report est deja passee. Intervention RH requise.");
        BaCongeDroit carry=newRight(target.getAffectation(),source.getOrigineDebut(),source.getOrigineFin(),expiration,key);
        carry.setDebutDisponibilite(target.getDebutDisponibilite());carry.setFinDisponibilite(target.getFinDisponibilite());carry.setOrigineDroitId(source.getId());rights.save(carry);
        move(source,null,"REPORT_SORTANT",quantity,key+":sortie","Report vers "+carry.getId(),request.getJustification(),actor.getId());
        move(carry,null,"REPORT_ENTRANT",quantity,key+":entree","Report de "+source.getId(),request.getJustification(),actor.getId());return dto(carry);
    }

    /** L'agent est verrouille par le service appelant avant toute allocation ou ecriture. */
    public List<Imputation> allocate(BaUser agent,String type,Calcul calculation,Politique requestPolicy,Map<String,BigDecimal> replacementCredits) {
        List<BaCongeDroit> lots=rights.findByAgentIdAndStatutOrderByOrigineDebutAsc(agent.getId(),EStatut.A).stream().filter(r->r.getTypeCode().equals(type)&&!r.isCloturee()).toList();
        Comparator<BaCongeDroit> expiry=Comparator.comparing(BaCongeDroit::getExpiration,Comparator.nullsLast(Comparator.naturalOrder()));
        Priorite order=requestPolicy.getPriorite();
        lots=new ArrayList<>(lots);lots.sort(order==Priorite.ORIGINE_PUIS_EXPIRATION?Comparator.comparing(BaCongeDroit::getOrigineDebut).thenComparing(expiry):expiry.thenComparing(BaCongeDroit::getOrigineDebut));
        Map<String,BigDecimal> remaining=new HashMap<>();
        for(BaCongeDroit lot:lots)remaining.put(lot.getId(),balance(lot).disponible().add(replacementCredits.getOrDefault(lot.getId(),BigDecimal.ZERO)));
        List<Imputation> result=new ArrayList<>();
        for(JourCalcule day:calculation.getJours()) {
            BigDecimal missing=day.getQuantite();if(missing.signum()==0)continue;
            List<BaCongeDroit> eligible=lots.stream().filter(r->eligible(r,day.getDate())).toList();
            for(BaCongeDroit lot:eligible) {
                BigDecimal amount=remaining.get(lot.getId()).max(BigDecimal.ZERO).min(missing);
                if(amount.signum()>0){result.add(imputation(lot,day.getDate(),amount));remaining.put(lot.getId(),remaining.get(lot.getId()).subtract(amount));missing=missing.subtract(amount);}
                if(missing.signum()==0)break;
            }
            if(missing.signum()>0) {
                // Une avance est une dette plafonnee sur la periode d'origine, jamais un credit de projection.
                for(BaCongeDroit lot:eligible) {
                    BigDecimal amount=remaining.get(lot.getId()).add(anticipation(lot)).max(BigDecimal.ZERO).min(missing);
                    if(amount.signum()>0){result.add(imputation(lot,day.getDate(),amount));remaining.put(lot.getId(),remaining.get(lot.getId()).subtract(amount));missing=missing.subtract(amount);}
                    if(missing.signum()==0)break;
                }
            }
            if(missing.signum()>0)throw blocked("Solde acquis insuffisant pour le "+day.getDate()+". Les projections futures ne sont pas utilisables sans une prise anticipee explicitement autorisee.");
        }
        return result;
    }
    private boolean eligible(BaCongeDroit right,LocalDate day) {
        return !day.isBefore(right.getDebutDisponibilite())&&!day.isAfter(right.getFinDisponibilite())
                &&(right.getExpiration()==null||!day.isAfter(right.getExpiration())&&!today(right).isAfter(right.getExpiration()));
    }
    public BigDecimal availableFor(BaUser agent,String type,Calcul calculation,Map<String,BigDecimal> replacementCredits){
        return rights.findByAgentIdAndStatutOrderByOrigineDebutAsc(agent.getId(),EStatut.A).stream().filter(r->r.getTypeCode().equals(type)&&!r.isCloturee()&&calculation.getJours().stream().anyMatch(d->d.getQuantite().signum()>0&&eligible(r,d.getDate())))
                .map(r->balance(r).disponible().add(replacementCredits.getOrDefault(r.getId(),BigDecimal.ZERO))).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private Imputation imputation(BaCongeDroit right,LocalDate day,BigDecimal quantity){Imputation i=new Imputation();i.setDroitId(right.getId());i.setJour(day);i.setQuantite(quantity);i.setExpiration(right.getExpiration());i.setOrigine(right.getOrigineDebut());return i;}
    public Map<String,BigDecimal> grouped(List<Imputation> lines){Map<String,BigDecimal> result=new LinkedHashMap<>();lines.forEach(i->result.merge(i.getDroitId(),i.getQuantite(),BigDecimal::add));return result;}
    public void reserve(BaCongeDemande request,BaUser actor) {for(var e:grouped(request.getReservations()).entrySet())move(right(e.getKey()),request,"RESERVATION",e.getValue(),"reservation:"+request.getId()+":"+e.getKey(),"Soumission", "Reservation de la revision "+request.getRevision(),actor.getId());}
    public void release(BaCongeDemande request,BaUser actor,String reason) {
        for(var e:grouped(request.getReservations()).entrySet())move(right(e.getKey()),request,"LIBERATION",e.getValue(),"liberation:"+request.getId()+":"+e.getKey(),reason,reason,actor.getId());
    }
    public void debit(BaCongeDemande request,BaUser actor) {
        for(Imputation line:request.getImputations()) {
            BaCongeDroit lot=right(line.getDroitId());
            if(lot.getExpiration()!=null&&lot.getExpiration().isBefore(today(lot))&&lot.getPolitique().getRegles().getExpirationReservation()==ExpirationReservation.BLOQUER_ET_SIGNALER)
                throw blocked("Des droits reserves ont expire. Intervention RH requise ; aucun debit n'a ete effectue.");
        }
        release(request,actor,"Transformation en debit");
        for(var e:grouped(request.getImputations()).entrySet())move(right(e.getKey()),request,"DEBIT",e.getValue(),"debit:"+request.getId()+":"+e.getKey(),"Approbation finale","Debit unique de la revision "+request.getRevision(),actor.getId());
    }
    public void restore(BaCongeDemande request,List<Imputation> lines,BaUser actor,String source,String key) {
        for(var e:grouped(lines).entrySet())move(right(e.getKey()),request,"RESTITUTION",e.getValue(),"restitution:"+key+":"+e.getKey(),source,source,actor.getId());
    }
    public void processDue() {
        security.require("CONGES_DROITS");processDueInternal(security.current().getId());
    }
    public void processDueInternal(String actorId) {
        // Ordre stable des verrous entre deux rattrapages concurrents.
        java.util.Set<String> agents=new java.util.TreeSet<>();
        accruals.findByExecuteeFalseAndDateAcquisitionLessThanEqualOrderByDateAcquisitionAsc(LocalDate.now().plusDays(1)).forEach(e->agents.add(e.getDroit().getAgent().getId()));
        rights.findByStatutOrderByOrigineDebutAsc(EStatut.A).stream().filter(r->r.getExpiration()!=null&&r.getExpiration().isBefore(today(r))).forEach(r->agents.add(r.getAgent().getId()));
        agents.forEach(this::lock);
        // Les echeances lues avant l'attente du verrou peuvent avoir ete traitees par une autre transaction.
        entityManager.clear();
        for(BaCongeEcheance item:accruals.findByExecuteeFalseAndDateAcquisitionLessThanEqualOrderByDateAcquisitionAsc(LocalDate.now().plusDays(1))) {
            lock(item.getDroit().getAgent().getId());executeDue(item.getDroit(),actorId);
        }
        for(BaCongeDroit lot:rights.findByStatutOrderByOrigineDebutAsc(EStatut.A)) {
            if(lot.getExpiration()!=null&&lot.getExpiration().isBefore(today(lot))) {lock(lot.getAgent().getId());expire(lot,actorId);}
        }
    }
    private void executeDue(BaCongeDroit right,String actor) {
        for(BaCongeEcheance item:accruals.findByDroitIdOrderByDateAcquisitionAsc(right.getId())) {
            if(item.isExecutee()||item.getDateAcquisition().isAfter(today(right))||right.isCloturee())continue;
            BigDecimal quantity=capped(right,item.getQuantite());
            if(quantity.signum()>0)move(right,null,right.getPolitique().getRegles().getAttribution()==Attribution.DOTATION?"ATTRIBUTION":"ACQUISITION",quantity,item.getCle(),right.getSource(),right.getJustification()+" ; echeance "+item.getDateAcquisition(),actor);
            if(quantity.compareTo(item.getQuantite())<0)anomaly(right,null,"plafond:"+item.getId(),"Acquisition plafonnee : controle RH de l'echeance "+item.getDateAcquisition()+".");
            item.setExecutee(true);accruals.save(item);
        }
    }
    private BigDecimal capped(BaCongeDroit right,BigDecimal quantity) {
        BigDecimal cap=right.getPolitique().getRegles().getPlafondAccumulation();if(cap==null)return quantity;
        BigDecimal held=rights.findByAgentIdAndStatutOrderByOrigineDebutAsc(right.getAgent().getId(),EStatut.A).stream().filter(r->r.getTypeCode().equals(right.getTypeCode())&&(r.getExpiration()==null||!r.getExpiration().isBefore(today(r))))
                .map(this::balance).map(b->b.acquis().subtract(b.debites())).reduce(BigDecimal.ZERO,BigDecimal::add);
        return quantity.min(cap.subtract(held).max(BigDecimal.ZERO));
    }
    public void expire(BaCongeDroit lot,String actor) {
        Balance b=balance(lot);BigDecimal free=b.disponible().max(BigDecimal.ZERO);
        if(free.signum()>0)move(lot,null,"EXPIRATION",free,"expiration:"+lot.getId()+":"+hash(b.toString()),"Expiration "+lot.getExpiration(),"Expiration des droits libres, reservations preservees",actor);
        if(b.reserves().signum()>0)anomaly(lot,null,"expiration-reservee:"+lot.getId(),"Des droits reserves ont expire. Politique : "+lot.getPolitique().getRegles().getExpirationReservation()+". Aucune demande n'est annulee automatiquement.");
    }
    public void close(String id,String justification) {
        BaUser actor=security.require("CONGES_DROITS");BaCongeDroit lot=lockedRight(id);
        if(justification==null||justification.isBlank())throw bad("Justification de cloture obligatoire.");
        if(lot.getFinDisponibilite().isAfter(today(lot))||balance(lot).reserves().signum()>0)throw blocked("Periode non terminee ou droits encore reserves.");
        if(balance(lot).disponible().signum()>0)throw blocked("Des droits libres subsistent. Effectuez un report ou attendez leur expiration confirmee avant la cloture.");
        lot.setCloturee(true);rights.save(lot);BaLogDto audit=new BaLogDto(com.bakouan.app.enums.EAction.U,"Cloture compteur conges "+lot.getId());audit.setDetails("Auteur "+actor.getId()+" : "+justification);logs.log(audit);
    }
    public BaCongeMouvement move(BaCongeDroit right,BaCongeDemande request,String nature,BigDecimal quantity,String key,String source,String reason,String actor) {
        if(quantity.signum()==0)throw bad("Mouvement nul interdit.");
        String normalized="mouvement:"+hash(key);Optional<BaCongeMouvement> existing=movements.findByCle(normalized);
        if(existing.isPresent()){sameMovement(existing.get(),right,nature,quantity,source);return existing.get();}
        BaCongeMouvement m=new BaCongeMouvement();m.setDroit(right);m.setDemande(request);m.setNature(nature);m.setQuantite(quantity.setScale(4,RoundingMode.UNNECESSARY));m.setCle(normalized);m.setSource(source);m.setJustification(reason);m.setAuteurId(actor);
        m.setCreatedBy(actor.equals("systeme-conges")?actor:null);return movements.save(m);
    }
    private void sameMovement(BaCongeMouvement m,BaCongeDroit right,String nature,BigDecimal quantity,String source){if(!m.getDroit().getId().equals(right.getId())||!m.getNature().equals(nature)||m.getQuantite().compareTo(quantity)!=0||!m.getSource().equals(source))throw blocked("Cle de mouvement deja utilisee avec un contenu different.");}
    public BaCongeDroit right(String id){return rights.findById(id).filter(r->r.getStatut()==EStatut.A).orElseThrow(()->missing("Droits introuvables."));}
    private BaCongeDroit lockedRight(String id){String agentId=rights.findAgentId(id).orElseThrow(()->missing("Droits introuvables."));lock(agentId);return right(id);}
    public BaCongeAffectation assignment(String id){return assignments.findById(id).filter(a->a.getStatut()==EStatut.A).orElseThrow(()->missing("Affectation introuvable."));}
    public BaUser lock(String agentId){return users.findLockedById(agentId).orElseThrow(()->missing("Agent introuvable."));}
    private BigDecimal anticipation(BaCongeDroit right){Politique p=right.getPolitique().getRegles();return right.getOrigineDroitId()==null&&Boolean.TRUE.equals(p.getAnticipationAutorisee())?p.getPlafondAnticipation():BigDecimal.ZERO;}
    private void checkPeriod(BaCongeAffectation a,LocalDate from,LocalDate to,LocalDate expiry){if(!"ACTIVE".equals(a.getPolitique().getEtat())||!a.getCalendrier().isConfirme())throw blocked("Politique ou calendrier non confirme.");if(from.isBefore(a.getDebut())||a.getFin()!=null&&to.isAfter(a.getFin()))throw bad("La periode doit etre couverte par l'affectation.");if(expiry!=null&&expiry.isBefore(from))throw bad("Expiration anterieure a l'origine des droits.");}
    private BaCongeDroit newRight(BaCongeAffectation a,LocalDate from,LocalDate to,LocalDate expiry,String key){BaCongeDroit r=new BaCongeDroit();r.setAgent(a.getAgent());r.setAffectation(a);r.setPolitique(a.getPolitique());r.setTypeCode(a.getPolitique().getTypeCode());r.setOrigineDebut(from);r.setOrigineFin(to);r.setDebutDisponibilite(from);r.setFinDisponibilite(to);r.setExpiration(expiry);r.setCleOrigine(key);r.setSource("Politique "+a.getPolitique().getId());r.setJustification("Ouverture controlee des droits");return rights.save(r);}
    public void anomaly(BaCongeDroit right,BaCongeDemande request,String key,String description){if(anomalies.findByCle(key).isPresent())return;BaCongeAnomalie a=new BaCongeAnomalie();a.setDroit(right);a.setDemande(request);a.setCle(key);a.setDescription(description);a.setCreatedBy("systeme-conges");anomalies.save(a);}
    public SoldeDto dto(BaCongeDroit right) {
        Balance b=balance(right);BigDecimal projected=accruals.findByDroitIdOrderByDateAcquisitionAsc(right.getId()).stream().filter(e->!e.isExecutee()).map(BaCongeEcheance::getQuantite).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal future=BigDecimal.ZERO,taken=BigDecimal.ZERO,unconfirmed=BigDecimal.ZERO;LocalDate today=today(right);
        for(BaCongeDemande d:requests.findByAgentIdAndStatutOrderByCreatedDateDesc(right.getAgent().getId(),EStatut.A)) if("APPROUVEE".equals(d.getEtat())) {
            Map<String,BigDecimal> restored=new HashMap<>();
            d.getRestitutions().forEach(i->restored.merge(i.getDroitId()+"|"+i.getJour(),i.getQuantite(),BigDecimal::add));
            for(Imputation i:d.getImputations()) if(i.getDroitId().equals(right.getId())) {
                String key=i.getDroitId()+"|"+i.getJour();BigDecimal refund=restored.getOrDefault(key,BigDecimal.ZERO).min(i.getQuantite());restored.put(key,restored.getOrDefault(key,BigDecimal.ZERO).subtract(refund));BigDecimal consumed=i.getQuantite().subtract(refund);
                if(i.getJour().isAfter(today))future=future.add(consumed);else if(d.getRepriseConstatee()!=null)taken=taken.add(consumed);else unconfirmed=unconfirmed.add(consumed);
            }
        }
        boolean valid=!right.isCloturee()&&!today.isBefore(right.getDebutDisponibilite())&&!today.isAfter(right.getFinDisponibilite())&&(right.getExpiration()==null||!today.isAfter(right.getExpiration()));
        return new SoldeDto(right.getId(),right.getAgent().getId(),CongeConfigurationService.name(right.getAgent()),right.getTypeCode(),right.getPolitique().getLibelle(),right.getPolitique().getRegles().getUnite(),right.getOrigineDebut(),right.getOrigineFin(),right.getExpiration(),b.acquis(),b.reserves(),b.debites(),valid?b.disponible():BigDecimal.ZERO,projected,future,taken,unconfirmed,anticipation(right),right.isCloturee());
    }
    private LocalDate today(BaCongeDroit right){return LocalDate.now(ZoneId.of(right.getAffectation().getCalendrier().getRegles().getFuseau()));}
    public static String hash(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}

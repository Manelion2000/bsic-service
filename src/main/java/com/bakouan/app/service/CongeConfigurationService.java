package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import com.bakouan.app.dto.CongeDefinitions.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.dto.CongeDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import static com.bakouan.app.service.CongeSecurity.*;

@Service @Transactional @RequiredArgsConstructor
public class CongeConfigurationService {
    private final CongeSecurity security;
    private final CongeCalculService calculation;
    private final BaCongePolitiqueRepository policies;
    private final BaCongeCalendrierRepository calendars;
    private final BaCongeAffectationRepository assignments;
    private final BaCongeCouvertureRepository coverage;
    private final BaUserRepository users;
    private final BaRoleRepository roles;
    private final BaDepartementRepository departments;
    private final BaServiceRepository services;
    private final BaAgenceRepository agencies;
    private final BaLogService logs;

    public Map<String,Object> configuration() {
        BaUser actor=security.current(); Map<String,Object> result=new LinkedHashMap<>();
        Set<String> permissions=new LinkedHashSet<>();
        for(String p:List.of("CONGES_DEMANDER","CONGES_VALIDER","CONGES_POLITIQUES","CONGES_DROITS","CONGES_PIECES","CONGES_EXPORTER","CONGES_AUDITER","CONGES_PLANNING","CONGES_ANNULER","CONGES_DEROGATION")) if(security.has(actor,p)) permissions.add(p);
        result.put("permissions",permissions); result.put("agentId",actor.getId()); result.put("fonction",Objects.toString(actor.getFonction(),""));
        result.put("politiques",security.has(actor,"CONGES_POLITIQUES") ? listPolicies() : assignments.findByAgentIdAndStatutOrderByDebutDesc(actor.getId(),EStatut.A).stream().map(BaCongeAffectation::getPolitique).distinct().map(this::policyDto).toList());
        result.put("blocage",permissions.isEmpty()?"Aucune permission conges attribuee. Contactez la personne chargee des habilitations RH.":"");
        result.put("administrateurTechnique",security.has(actor,"BA_ADMIN"));
        if(!permissions.isEmpty()) result.put("agents",users.findByStatut(EStatut.A).stream().filter(CongeSecurity::available).map(u->Map.of("id",u.getId(),"nom",name(u))).toList());
        if(security.has(actor,"CONGES_POLITIQUES")||security.has(actor,"CONGES_PLANNING")||security.global(actor)){
            result.put("departements",departments.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->Map.of("id",d.getId(),"nom",Objects.toString(d.getNom(),d.getId()))).toList());
            result.put("services",services.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->Map.of("id",d.getId(),"nom",Objects.toString(d.getNom(),d.getId()))).toList());
            result.put("agences",agencies.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->Map.of("id",d.getId(),"nom",Objects.toString(d.getNom(),d.getId()))).toList());
        }
        result.put("quotaAbsence",Map.of("quantite",10,"independant",true,"configure",false,"message","Periode et decompte du quota d'absence encore a confirmer ; il ne constitue pas un droit annuel."));
        return result;
    }
    public List<PolitiqueDto> listPolicies() {security.require("CONGES_POLITIQUES");return policies.findByStatutOrderByTypeCodeAscVersionPolitiqueDesc(EStatut.A).stream().map(this::policyDto).toList();}
    public Map<String,Object> rightsReferences(){security.require("CONGES_DROITS");return Map.of("politiques",policies.findByStatutOrderByTypeCodeAscVersionPolitiqueDesc(EStatut.A).stream().filter(p->"ACTIVE".equals(p.getEtat())).map(this::policyDto).toList(),"calendriers",calendars.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().filter(BaCongeCalendrier::isConfirme).map(this::calendarDto).toList());}
    public PolitiqueDto createPolicy(CongeRequests.PolitiqueRequest request) {
        security.require("CONGES_POLITIQUES"); lockConfiguration(); dates(request.getDebutEffet(),request.getFinEffet());
        validatePolicy(request.getRegles(),false);
        if(policies.findByTypeCodeOrderByVersionPolitiqueDesc(request.getTypeCode()).stream().anyMatch(p->p.getRegles().getUnite()!=request.getRegles().getUnite()))
            throw blocked("L'unite d'un type existant ne peut pas changer sans une reprise explicite des compteurs. Utilisez un type distinct.");
        BaCongePolitique entity=new BaCongePolitique(); entity.setTypeCode(request.getTypeCode());entity.setLibelle(request.getLibelle());entity.setCategorie(request.getCategorie());
        entity.setVersionPolitique(policies.findByTypeCodeOrderByVersionPolitiqueDesc(request.getTypeCode()).stream().mapToInt(BaCongePolitique::getVersionPolitique).max().orElse(0)+1);
        entity.setDebutEffet(request.getDebutEffet());entity.setFinEffet(request.getFinEffet());entity.setSourceRh(request.getSourceRh());entity.setRegles(request.getRegles());
        audit("Creation politique conges "+entity.getTypeCode()+" version "+entity.getVersionPolitique());
        return policyDto(policies.save(entity));
    }
    public PolitiqueDto confirmPolicy(String id,CongeRequests.Confirmation request) {
        BaUser actor=security.require("CONGES_POLITIQUES");lockConfiguration();confirmed(request);
        BaCongePolitique entity=policy(id);validatePolicy(entity.getRegles(),true);
        if("ACTIVE".equals(entity.getEtat())) return policyDto(entity);
        boolean overlap=policies.findByTypeCodeOrderByVersionPolitiqueDesc(entity.getTypeCode()).stream().filter(p->"ACTIVE".equals(p.getEtat()))
                .anyMatch(p->overlap(p.getDebutEffet(),p.getFinEffet(),entity.getDebutEffet(),entity.getFinEffet()));
        if(overlap)throw blocked("Une politique de ce type est deja active sur cette periode. Creez des versions avec des dates d'effet sans chevauchement.");
        entity.setEtat("ACTIVE");entity.setConfirmePar(actor.getId());entity.setConfirmeLe(Instant.now());audit("Confirmation politique "+id+" : "+request.getJustification());
        return policyDto(policies.save(entity));
    }
    public List<CalendrierDto> listCalendars() {security.require("CONGES_POLITIQUES");return calendars.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().map(this::calendarDto).toList();}
    public CalendrierDto createCalendar(CongeRequests.CalendrierRequest request) {
        security.require("CONGES_POLITIQUES");dates(request.getDebutEffet(),request.getFinEffet());calculation.validateCalendar(request.getRegles());
        BaCongeCalendrier entity=new BaCongeCalendrier();entity.setNom(request.getNom());entity.setDebutEffet(request.getDebutEffet());entity.setFinEffet(request.getFinEffet());entity.setSourceRh(request.getSourceRh());entity.setRegles(request.getRegles());
        audit("Creation version calendrier conges "+entity.getNom());return calendarDto(calendars.save(entity));
    }
    public CalendrierDto confirmCalendar(String id,CongeRequests.Confirmation request) {
        BaUser actor=security.require("CONGES_POLITIQUES");confirmed(request);BaCongeCalendrier entity=calendar(id);calculation.validateCalendar(entity.getRegles());
        entity.setConfirme(true);entity.setConfirmePar(actor.getId());entity.setConfirmeLe(Instant.now());audit("Confirmation calendrier "+id+" : "+request.getJustification());return calendarDto(calendars.save(entity));
    }
    public List<AffectationDto> listAssignments() {security.require("CONGES_DROITS");return assignments.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().map(this::assignmentDto).toList();}
    public AffectationDto assign(CongeRequests.Affectation request) {
        security.require("CONGES_DROITS");BaUser agent=users.findLockedById(request.getAgentId()).orElseThrow(()->missing("Agent introuvable."));
        if(!available(agent))throw bad("Agent indisponible.");dates(request.getDebut(),request.getFin());
        BaCongePolitique policy=policy(request.getPolitiqueId());BaCongeCalendrier calendar=calendar(request.getCalendrierId());
        if(!"ACTIVE".equals(policy.getEtat())||!calendar.isConfirme())throw blocked("Confirmez la politique et le calendrier avant leur affectation.");
        LocalDate end=request.getFin();
        if(request.getDebut().isBefore(policy.getDebutEffet())||policy.getFinEffet()!=null&&(end==null||end.isAfter(policy.getFinEffet()))
                ||request.getDebut().isBefore(calendar.getDebutEffet())||calendar.getFinEffet()!=null&&(end==null||end.isAfter(calendar.getFinEffet())))throw bad("L'affectation doit etre couverte par les dates d'effet de la politique et du calendrier.");
        if(assignments.findByAgentIdAndStatutOrderByDebutDesc(agent.getId(),EStatut.A).stream().anyMatch(a->a.getPolitique().getTypeCode().equals(policy.getTypeCode())&&overlap(a.getDebut(),a.getFin(),request.getDebut(),end)))throw blocked("Une affectation de ce type couvre deja cette periode.");
        if(policy.getRegles().getProrata()!=Prorata.AUCUN&&request.getDateEntree()==null)throw bad("La date d'entree est obligatoire pour appliquer la proratisation choisie.");
        if(request.getDateEntree()!=null&&request.getDateSortie()!=null&&request.getDateSortie().isBefore(request.getDateEntree()))throw bad("Les dates d'emploi sont inversees.");
        BaCongeAffectation entity=new BaCongeAffectation();entity.setAgent(agent);entity.setPolitique(policy);entity.setCalendrier(calendar);entity.setDebut(request.getDebut());entity.setFin(end);entity.setDateEntree(request.getDateEntree());entity.setDateSortie(request.getDateSortie());entity.setJustification(request.getJustification());
        audit("Affectation conges agent "+agent.getId()+" politique "+policy.getId()+" : "+request.getJustification());return assignmentDto(assignments.save(entity));
    }
    public BaCongeAffectation applicable(String agentId,String type,LocalDate start,LocalDate end) {
        List<BaCongeAffectation> found=assignments.findByAgentIdAndStatutOrderByDebutDesc(agentId,EStatut.A).stream().filter(a->a.getPolitique().getTypeCode().equals(type)&&"ACTIVE".equals(a.getPolitique().getEtat())&&a.getCalendrier().isConfirme()&&!start.isBefore(a.getDebut())&&(a.getFin()==null||!end.isAfter(a.getFin()))).toList();
        if(found.size()!=1)throw blocked("Aucune affectation unique et confirmee ne couvre toute la demande. Verifiez la politique et le calendrier ; scindez la demande si leur version change pendant cette periode.");
        return found.get(0);
    }
    public List<CouvertureDto> listCoverage() {security.require("CONGES_POLITIQUES");return coverage.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().map(this::coverageDto).toList();}
    public CouvertureDto createCoverage(CongeRequests.CouvertureRequest request) {
        security.require("CONGES_POLITIQUES");dates(request.getDebut(),request.getFin());validateCoverage(request.getRegles());
        BaCongeCouverture entity=new BaCongeCouverture();entity.setNom(request.getNom());entity.setDebut(request.getDebut());entity.setFin(request.getFin());entity.setSourceRh(request.getSourceRh());entity.setRegles(request.getRegles());audit("Creation regle couverture conges "+entity.getNom());return coverageDto(coverage.save(entity));
    }
    public CouvertureDto confirmCoverage(String id,CongeRequests.Confirmation request) {
        BaUser actor=security.require("CONGES_POLITIQUES");confirmed(request);BaCongeCouverture entity=coverage.findById(id).orElseThrow(()->missing("Regle introuvable."));validateCoverage(entity.getRegles());
        entity.setConfirme(true);entity.setConfirmePar(actor.getId());entity.setConfirmeLe(Instant.now());audit("Confirmation couverture "+id+" : "+request.getJustification());return coverageDto(coverage.save(entity));
    }
    private void validateCoverage(Couverture rule) {
        if(!"JOURNEE".equals(rule.getGranularite()))throw bad("Confirmez la couverture a la journee : toute indisponibilite partielle retire l'agent de l'effectif de la journee.");
        boolean existing=switch(rule.getPerimetre()) {case "DEPARTEMENT"->departments.existsById(rule.getPerimetreId());case "SERVICE"->services.existsById(rule.getPerimetreId());case "AGENCE"->agencies.existsById(rule.getPerimetreId());default->false;};
        if(!existing)throw bad("Perimetre de couverture introuvable.");
        if(rule.getMinimumParFonction().values().stream().anyMatch(v->v==null||v<0))throw bad("Les minima par fonction doivent etre positifs ou nuls.");
        List<String> ids=new ArrayList<>();rule.getGroupes().forEach(g->ids.addAll(g.getAgents()));rule.getIncompatibilites().forEach(ids::addAll);
        for(String id:ids)if(!users.existsById(id))throw bad("Agent de couverture introuvable : "+id);
        if(rule.getIncompatibilites().stream().anyMatch(g->new HashSet<>(g).size()<2))throw bad("Une incompatibilite doit contenir au moins deux agents distincts.");
    }
    public void validatePolicy(Politique rule,boolean activation) {
        if(rule.getAttribution()==Attribution.DOTATION&&(rule.getDotation()==null||rule.getDotation().signum()<=0))throw bad("Volume de dotation obligatoire.");
        if(rule.getAttribution()==Attribution.ACQUISITION&&(rule.getFrequence()==null||rule.getAcquisitionParEcheance()==null||rule.getAcquisitionParEcheance().signum()<=0))throw bad("Frequence et volume par acquisition obligatoires.");
        if(rule.getDureeMin().compareTo(rule.getDureeMax())>0)throw bad("Les durees minimale et maximale sont inversees.");
        if(Boolean.TRUE.equals(rule.getReportAutorise())&&(rule.getPlafondReport()==null||rule.getExpirationReportJours()==null))throw bad("Plafond et expiration du report obligatoires.");
        if(Boolean.TRUE.equals(rule.getAnticipationAutorisee())&&(rule.getPlafondAnticipation()==null||rule.getPlafondAnticipation().signum()<=0))throw bad("Plafond de prise anticipee obligatoire.");
        if(!Set.of("NON_CONFIGURE","DEFINITIF","AUTRES_VALIDATEURS").contains(rule.getPolitiqueRefus()))throw bad("Politique de refus inconnue.");
        if(activation&&(!Boolean.TRUE.equals(rule.getCircuitAbsencesConfirme())||!Boolean.TRUE.equals(rule.getModeleReservationDebitConfirme())||!Boolean.TRUE.equals(rule.getRhGestionAdministrativeConfirmee())||"NON_CONFIGURE".equals(rule.getPolitiqueRefus())))
            throw blocked("Confirmez le circuit des absences, le role administratif RH, le modele reservation/debit et le traitement d'un refus avant d'activer cette politique.");
    }
    public BaCongePolitique policy(String id){return policies.findById(id).filter(p->p.getStatut()==EStatut.A).orElseThrow(()->missing("Politique introuvable."));}
    public BaCongeCalendrier calendar(String id){return calendars.findById(id).filter(c->c.getStatut()==EStatut.A).orElseThrow(()->missing("Calendrier introuvable."));}
    public PolitiqueDto policyDto(BaCongePolitique p){return new PolitiqueDto(p.getId(),p.getTypeCode(),p.getLibelle(),p.getCategorie(),p.getVersionPolitique(),p.getEtat(),p.getDebutEffet(),p.getFinEffet(),p.getSourceRh(),p.getRegles(),p.getConfirmePar(),p.getConfirmeLe());}
    public CalendrierDto calendarDto(BaCongeCalendrier c){return new CalendrierDto(c.getId(),c.getNom(),c.getDebutEffet(),c.getFinEffet(),c.getSourceRh(),c.getRegles(),c.isConfirme());}
    public AffectationDto assignmentDto(BaCongeAffectation a){return new AffectationDto(a.getId(),a.getAgent().getId(),name(a.getAgent()),a.getPolitique().getId(),a.getPolitique().getTypeCode(),a.getCalendrier().getId(),a.getDebut(),a.getFin(),a.getDateEntree(),a.getDateSortie());}
    public CouvertureDto coverageDto(BaCongeCouverture c){return new CouvertureDto(c.getId(),c.getNom(),c.getDebut(),c.getFin(),c.getSourceRh(),c.getRegles(),c.isConfirme());}
    private void lockConfiguration(){roles.findLockedByCode("BA_CONNECT").orElseThrow(()->blocked("Role BA_CONNECT absent."));}
    private void confirmed(CongeRequests.Confirmation request){if(!request.isConfirme()||request.getJustification()==null||request.getJustification().isBlank())throw bad("La confirmation RH et sa justification sont obligatoires.");}
    private void dates(LocalDate start,LocalDate end){if(start==null||end!=null&&end.isBefore(start))throw bad("Dates d'effet invalides.");}
    public static boolean overlap(LocalDate a,LocalDate b,LocalDate c,LocalDate d){return(b==null||!c.isAfter(b))&&(d==null||!a.isAfter(d));}
    public static String name(BaUser user){return(Objects.toString(user.getPrenom(),"")+" "+Objects.toString(user.getNom(),"")).trim();}
    private void audit(String text){BaLogDto entry=new BaLogDto(com.bakouan.app.enums.EAction.U,"Configuration des conges");entry.setDetails(text);logs.log(entry);}
}

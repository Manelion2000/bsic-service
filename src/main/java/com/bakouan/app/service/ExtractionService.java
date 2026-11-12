package com.bakouan.app.service;

import com.bakouan.app.dto.*;
import com.bakouan.app.dto.ExtractionDtos.*;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

@Service @RequiredArgsConstructor @Transactional
public class ExtractionService {
    public static final String DEMANDER="EXTRACTIONS_DEMANDER", LIRE="REQUETES_CONSULTER", GERER="EXTRACTIONS_GERER";
    private final CongeSecurity security;
    private final BaUserRepository users;
    private final BaDepartementRepository departements;
    private final BaServiceRepository services;
    private final BaRequeteOracleRepository requetes;
    private final BaRequeteRevisionRepository revisions;
    private final BaExtractionDemandeRepository demandes;
    private final BaExtractionExecutionRepository executions;
    private final BaLogService audit;
    private final Validator validator;
    private final OracleSqlCatalogueValidator sqlValidator;

    @Transactional(readOnly=true)
    public Configuration configuration() {
        var a=security.current();boolean manage=manage(a),read=manage||has(a,LIRE),request=manage||has(a,DEMANDER);
        var dep=department(a);
        return new Configuration(request,read,manage,ref(a),dep==null?null:new Reference(dep.getId(),dep.getNom()),currentService(a),
            manage?users.findAll().stream().filter(u->available(u)&&department(u)!=null&&department(u).getStatut()==EStatut.A).map(u->new Demandeur(u.getId(),name(u),new Reference(department(u).getId(),department(u).getNom()),currentService(u))).sorted(Comparator.comparing(Demandeur::nom)).toList():List.of(),
            read?departements.findByStatut(EStatut.A).stream().map(d->new Reference(d.getId(),d.getNom())).sorted(Comparator.comparing(Reference::nom,String.CASE_INSENSITIVE_ORDER)).toList():List.of(),
            read||request?services.findByStatut(EStatut.A).stream().filter(s->s.getDepartement()!=null&&s.getDepartement().getStatut()==EStatut.A)
                .filter(s->read||(dep!=null&&s.getDepartement().getId().equals(dep.getId())))
                .map(s->new ServiceReference(s.getId(),s.getNom(),s.getDepartement().getId())).sorted(Comparator.comparing(ServiceReference::nom,String.CASE_INSENSITIVE_ORDER)).toList():List.of());
    }
    @Transactional(readOnly=true)
    public ExtractionDtos.Page<RequeteLigne> library(String search,String departmentId,boolean drafts,boolean archived,int page,int size) {
        var a=reader();bounds(search,page,size);if((drafts||archived)&&!manage(a))throw forbidden("Les brouillons et archives sont réservés aux gestionnaires.");
        Specification<BaRequeteOracle> spec=(root,q,cb)->{
            var ps=new ArrayList<jakarta.persistence.criteria.Predicate>();ps.add(cb.equal(root.get("statut"),archived?EStatut.D:EStatut.A));
            if(!drafts&&!archived)ps.add(cb.isTrue(root.get("verifiee")));
            if(!clean(departmentId).isEmpty())ps.add(cb.equal(root.get("departementDemandeur").get("id"),clean(departmentId)));
            if(!clean(search).isEmpty()){String term=term(search);ps.add(cb.or(cb.like(cb.lower(root.get("titre")),term,'\\'),cb.like(cb.lower(root.get("objectif")),term,'\\'),cb.like(cb.lower(root.get("motCle")),term,'\\')));}
            return cb.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var rows=requetes.findAll(spec,PageRequest.of(page,size,Sort.by("titre","id")));
        return new ExtractionDtos.Page<>(rows.getContent().stream().map(this::line).toList(),rows.getTotalElements(),rows.getTotalPages(),page,size);
    }
    public RequeteDetail query(String id) {var a=reader();var r=loadQuery(id);if(!manage(a)&&(!r.isVerifiee()||r.getStatut()!=EStatut.A))throw missing("Requête vérifiée introuvable.");audit(a,"CONSULTATION_SQL",id);var result=detail(r);return manage(a)?result:new RequeteDetail(result.fiche(),result.schemaCible(),result.sqlTexte(),result.parametres(),result.revisions().stream().filter(v->v.edition()==r.getEdition()).toList(),result.executions().stream().filter(e->e.edition()==r.getEdition()).toList());}
    public RequeteDetail saveQuery(String id,RequeteEcriture input) {
        var a=manager();validate(input);sqlValidator.validateSyntax(input.sqlTexte());
        if(!input.parametres().isEmpty())sqlValidator.validate(input.sqlTexte(),input.parametres());
        var dep=departements.findById(clean(input.departementId())).filter(d->d.getStatut()==EStatut.A).orElseThrow(()->bad("Choisissez un département demandeur actif de l'annuaire."));
        var service=selectedService(input.serviceId(),dep);
        var r=id==null?new BaRequeteOracle():lockQuery(id);if(id!=null){check(r,input.version());if(r.getStatut()!=EStatut.A)throw blocked("Restaurez la requête avant de la modifier.");}
        if(id!=null)r.setEdition(r.getEdition()+1);
        r.setTitre(clean(input.titre()));r.setMotCle(input.motCle());r.setObjectif(clean(input.objectif()));r.setDateValidation(input.dateValidation());r.setAuteurId(a.getId());r.setAuteur(name(a));
        r.setDepartementDemandeur(dep);r.setDepartementNom(dep.getNom());
        r.setServiceDemandeur(service);r.setServiceNom(service==null?null:service.getNom());
        r.setSchemaCible(clean(input.schemaCible()));r.setSqlTexte(input.sqlTexte().trim());r.setParametres(List.copyOf(input.parametres()));r.setVerifiee(true);
        r.setLastModifiedDate(Instant.now());r=requetes.saveAndFlush(r);
        String motif=clean(input.motif()).isEmpty()?(id==null?"Enregistrement d'une requête déjà validée":"Nouvelle édition d'une requête déjà validée"):clean(input.motif());
        var rev=new BaRequeteRevision();rev.setRequete(r);rev.setEdition(r.getEdition());rev.setDate(Instant.now());rev.setAuteurId(a.getId());rev.setAuteur(name(a));rev.setMotif(motif);
        rev.setContenu(new RequeteEcriture(r.getTitre(),r.getObjectif(),r.getDateValidation(),dep.getId(),dep.getNom(),service==null?null:service.getId(),r.getServiceNom(),r.getSchemaCible(),r.getSqlTexte(),r.getParametres(),r.getVersion(),motif,r.getMotCle()));revisions.saveAndFlush(rev);
        audit(a,id==null?"ENREGISTREMENT_REQUETE_VALIDEE":"NOUVELLE_EDITION",r.getId()+";edition="+r.getEdition()+";dateValidation="+r.getDateValidation());return detail(r);
    }
    public RequeteDetail testQuery(String id,Essai input) {
        var a=manager();validate(input);var r=lockQuery(id);check(r,input.version());active(r);
        var rev=currentRevision(r);saveExecution(rev,null,input,a);
        // Un nouvel essai change la version technique, jamais le SQL ou son édition.
        if(lastSuccess(rev)==false)r.setVerifiee(false);r.setLastModifiedDate(Instant.now());requetes.saveAndFlush(r);
        audit(a,"ESSAI_"+input.resultat(),id+";edition="+rev.getEdition());return detail(r);
    }
    public RequeteDetail certify(String id,Operation input) {
        var a=manager();validate(input);var r=lockQuery(id);check(r,input.version());active(r);
        if(r.isVerifiee())throw blocked("Cette édition est déjà proposée à la réutilisation.");
        if(!lastSuccess(currentRevision(r)))throw blocked("Enregistrez un essai réussi de cette édition ; le dernier essai daté doit être réussi.");
        r.setVerifiee(true);r.setLastModifiedDate(Instant.now());requetes.saveAndFlush(r);audit(a,"VERIFICATION_REUTILISATION",id+";edition="+r.getEdition()+";motif="+clean(input.motif()));return detail(r);
    }
    public RequeteDetail archive(String id,Operation input,boolean restore) {
        var a=manager();validate(input);var r=lockQuery(id);check(r,input.version());
        if(r.getStatut()==(restore?EStatut.A:EStatut.D))throw blocked("Cette opération est déjà effectuée.");
        r.setStatut(restore?EStatut.A:EStatut.D);r.setVerifiee(false);requetes.saveAndFlush(r);
        audit(a,restore?"RESTAURATION_REQUETE":"ARCHIVAGE_REQUETE",id+";motif="+clean(input.motif()));return detail(r);
    }
    @Transactional(readOnly=true)
    public ExtractionDtos.Page<DemandeLigne> requests(String search,Etat state,String dept,int page,int size) {
        var a=requester();bounds(search,page,size);boolean all=manage(a);
        if(!all&&!clean(dept).isEmpty())throw forbidden("Le filtre département est réservé aux gestionnaires.");
        Specification<BaExtractionDemande> spec=(root,q,cb)->{
            var ps=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(!all)ps.add(cb.equal(root.get("demandeur").get("id"),a.getId()));
            if(state!=null)ps.add(cb.equal(root.get("etat"),state));
            if(!clean(dept).isEmpty())ps.add(cb.equal(root.get("departement").get("id"),dept));
            if(!clean(search).isEmpty()){String term=term(search);ps.add(cb.or(cb.like(cb.lower(root.get("reference")),term,'\\'),cb.like(cb.lower(root.get("titre")),term,'\\'),cb.like(cb.lower(root.get("demandeurNom")),term,'\\')));}
            return cb.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var rows=demandes.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdDate","id")));
        return new ExtractionDtos.Page<>(rows.getContent().stream().map(this::line).toList(),rows.getTotalElements(),rows.getTotalPages(),page,size);
    }
    @Transactional(readOnly=true)
    public DemandeDetail request(String id) {var a=requester();var d=loadRequest(id);scope(a,d);return detail(d,a);}
    public DemandeDetail create(DemandeEcriture input) {
        var a=requester();validate(input);
        String userId=clean(input.demandeurId()).isEmpty()?a.getId():input.demandeurId();
        if(!a.getId().equals(userId)&&!manage(a))throw forbidden("Vous pouvez demander uniquement pour votre compte.");
        var target=users.findById(userId).filter(CongeSecurity::available).orElseThrow(()->bad("Demandeur indisponible."));
        var dep=department(target);if(dep==null||dep.getStatut()!=EStatut.A)throw bad("Le demandeur doit être rattaché à un département actif.");
        var service=selectedService(input.serviceId(),dep);
        if(input.echeance()!=null&&input.echeance().isBefore(input.dateDemande()))throw bad("L'échéance ne peut pas précéder la demande.");
        if((input.periodeDebut()==null)!=(input.periodeFin()==null)||(input.periodeDebut()!=null&&input.periodeFin().isBefore(input.periodeDebut())))throw bad("Renseignez une période complète et ordonnée.");
        var d=new BaExtractionDemande();d.setReference("EXT-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase(Locale.ROOT));
        d.setTitre(clean(input.titre()));d.setObjectif(clean(input.objectif()));d.setDateDemande(input.dateDemande());d.setEcheance(input.echeance());d.setPeriodeDebut(input.periodeDebut());d.setPeriodeFin(input.periodeFin());d.setCriteres(clean(input.criteres()));
        d.setDemandeur(target);d.setDemandeurNom(name(target));d.setDepartement(dep);d.setDepartementNom(dep.getNom());d.setAuteurId(a.getId());d.setAuteur(name(a));event(d,a,"CREATION","Demande enregistrée");
        d.setService(service);d.setServiceNom(service==null?null:service.getNom());
        d=demandes.saveAndFlush(d);audit(a,"CREATION_DEMANDE",d.getId());return detail(d,a);
    }
    public DemandeDetail take(String id,PriseEnCharge input) {
        var a=manager();validate(input);
        // Même ordre de verrous que l'enregistrement d'une exécution : requête puis demande.
        var r=lockQuery(input.requeteId());var d=lockRequest(id);check(d,input.version());
        if(d.getEtat()!=Etat.NOUVELLE)throw blocked("Seule une nouvelle demande peut être prise en charge.");
        if(r.getStatut()!=EStatut.A||!r.isVerifiee())throw blocked("Choisissez une édition active validée de la bibliothèque.");
        d.setRevision(currentRevision(r));d.setChargeDe(a);d.setEtat(Etat.EN_COURS);event(d,a,"PRISE_EN_CHARGE",clean(input.motif()));demandes.saveAndFlush(d);audit(a,"PRISE_EN_CHARGE",id);return detail(d,a);
    }
    public DemandeDetail execute(String id,Essai input) {
        var a=manager();validate(input);String queryId=demandes.queryId(id).orElseThrow(()->blocked("Prenez en charge la demande avec une requête vérifiée."));
        var r=lockQuery(queryId);var d=lockRequest(id);check(d,input.version());
        if(d.getEtat()!=Etat.EN_COURS)throw blocked("Cette demande n'est plus en traitement.");
        if(!d.getChargeDe().getId().equals(a.getId()))throw forbidden("L'exécution doit être déclarée par la personne chargée du traitement.");
        if(input.executeLe().atZone(ZoneId.of("Africa/Ouagadougou")).toLocalDate().isBefore(d.getDateDemande()))throw bad("La date d'exécution ne peut pas précéder la date de demande.");
        saveExecution(d.getRevision(),d,input,a);
        if(d.getRevision().getEdition()==r.getEdition()&&!lastSuccess(d.getRevision())){r.setVerifiee(false);r.setLastModifiedDate(Instant.now());requetes.saveAndFlush(r);}
        if(input.resultat()==Resultat.SUCCES)d.setEtat(Etat.TERMINEE);
        event(d,a,"EXECUTION_"+input.resultat(),clean(input.referenceVerification()));d.setLastModifiedDate(Instant.now());demandes.saveAndFlush(d);
        audit(a,"EXECUTION_DEMANDE_"+input.resultat(),id+";edition="+d.getRevision().getEdition());return detail(d,a);
    }
    public DemandeDetail cancel(String id,Operation input) {
        var a=requester();validate(input);var d=lockRequest(id);scope(a,d);check(d,input.version());
        if(d.getEtat()!=Etat.NOUVELLE&&!(manage(a)&&d.getEtat()==Etat.EN_COURS))throw blocked("L'annulation est réservée aux demandes nouvelles ou au gestionnaire pendant le traitement.");
        d.setEtat(Etat.ANNULEE);event(d,a,"ANNULATION",clean(input.motif()));demandes.saveAndFlush(d);audit(a,"ANNULATION_DEMANDE",id);return detail(d,a);
    }
    private void saveExecution(BaRequeteRevision revision,BaExtractionDemande demande,Essai input,BaUser a) {
        if(input.resultat()==Resultat.ECHEC&&clean(input.commentaire()).isEmpty())throw bad("Expliquez l'échec sans saisir de données extraites ou de secrets.");
        var e=new BaExtractionExecution();e.setRevision(revision);e.setDemande(demande);e.setExecuteLe(input.executeLe());e.setEnregistreLe(Instant.now());e.setAuteurId(a.getId());e.setAuteur(name(a));
        e.setEnvironnement(input.environnement());e.setResultat(input.resultat());e.setReferenceVerification(clean(input.referenceVerification()));e.setLignes(input.lignes());e.setCommentaire(clean(input.commentaire()));executions.saveAndFlush(e);
    }
    private boolean lastSuccess(BaRequeteRevision rev){return executions.findFirstByRevisionIdOrderByExecuteLeDescEnregistreLeDescIdDesc(rev.getId()).map(e->e.getResultat()==Resultat.SUCCES).orElse(false);}
    private RequeteDetail detail(BaRequeteOracle r){return new RequeteDetail(line(r),r.getSchemaCible(),r.getSqlTexte(),r.getParametres(),revisions.findByRequeteIdOrderByEditionDesc(r.getId(),PageRequest.of(0,50)).stream().map(this::revision).toList(),executions.findByRevisionRequeteIdOrderByEnregistreLeDescIdDesc(r.getId(),PageRequest.of(0,50)).stream().map(this::execution).toList());}
    private RequeteLigne line(BaRequeteOracle r){return new RequeteLigne(r.getId(),r.getTitre(),r.getObjectif(),r.getDateValidation(),r.getAuteur(),r.getDepartementDemandeur()==null?null:new Reference(r.getDepartementDemandeur().getId(),r.getDepartementNom()),r.getServiceDemandeur()==null?null:new Reference(r.getServiceDemandeur().getId(),r.getServiceNom()),r.getEdition(),r.isVerifiee(),r.getStatut()!=EStatut.A,r.getVersion(),r.getLastModifiedDate(),r.getMotCle());}
    private Revision revision(BaRequeteRevision r){return new Revision(r.getId(),r.getEdition(),r.getDate(),r.getAuteur(),r.getMotif(),r.getContenu());}
    private DemandeLigne line(BaExtractionDemande d){var rev=d.getRevision();return new DemandeLigne(d.getId(),d.getReference(),d.getTitre(),d.getEtat(),d.getDateDemande(),d.getEcheance(),new Reference(d.getDemandeur().getId(),d.getDemandeurNom()),new Reference(d.getDepartement().getId(),d.getDepartementNom()),d.getService()==null?null:new Reference(d.getService().getId(),d.getServiceNom()),d.getAuteur(),ref(d.getChargeDe()),rev==null?null:rev.getContenu().titre(),rev==null?null:rev.getEdition(),d.getVersion(),d.getCreatedDate());}
    private DemandeDetail detail(BaExtractionDemande d,BaUser a){boolean m=manage(a);return new DemandeDetail(line(d),d.getObjectif(),d.getPeriodeDebut(),d.getPeriodeFin(),d.getCriteres(),m&&d.getRevision()!=null?revision(d.getRevision()):null,m?executions.findByDemandeIdOrderByEnregistreLeDescIdDesc(d.getId(),PageRequest.of(0,50)).stream().map(this::execution).toList():List.of(),d.getHistorique(),m&&(d.getEtat()==Etat.NOUVELLE||(d.getEtat()==Etat.EN_COURS&&d.getChargeDe().getId().equals(a.getId()))),d.getEtat()==Etat.NOUVELLE||(m&&d.getEtat()==Etat.EN_COURS));}
    private Execution execution(BaExtractionExecution e){return new Execution(e.getId(),e.getRevision().getEdition(),e.getExecuteLe(),e.getEnregistreLe(),e.getAuteur(),e.getEnvironnement(),e.getResultat(),e.getReferenceVerification(),e.getLignes(),e.getCommentaire(),e.getDemande()==null?null:e.getDemande().getReference());}
    private void event(BaExtractionDemande d,BaUser a,String action,String motif){var history=new ArrayList<>(d.getHistorique());history.add(new Evenement(Instant.now(),name(a),action,motif));d.setHistorique(history);}
    private BaRequeteRevision currentRevision(BaRequeteOracle r){return revisions.findByRequeteIdAndEdition(r.getId(),r.getEdition()).orElseThrow(()->blocked("Édition de requête manquante."));}
    private BaRequeteOracle loadQuery(String id){return requetes.findById(id).orElseThrow(()->missing("Requête introuvable."));}
    private BaRequeteOracle lockQuery(String id){return requetes.locked(id).orElseThrow(()->missing("Requête introuvable."));}
    private BaExtractionDemande loadRequest(String id){return demandes.findById(id).orElseThrow(()->missing("Demande introuvable."));}
    private BaExtractionDemande lockRequest(String id){return demandes.locked(id).orElseThrow(()->missing("Demande introuvable."));}
    private void scope(BaUser a,BaExtractionDemande d){if(!manage(a)&&!a.getId().equals(d.getDemandeur().getId()))throw missing("Demande introuvable dans votre périmètre.");}
    private void active(BaRequeteOracle r){if(r.getStatut()!=EStatut.A)throw blocked("La requête est archivée.");}
    private void check(BaAbstractAuditingEntity e,Long version){if(version==null)throw bad("La version lue est obligatoire.");if(!e.getVersion().equals(version))throw blocked("La fiche a changé. Actualisez-la avant de recommencer.");}
    private boolean has(BaUser a,String code){var roles=new ArrayList<>(a.getRoles());if(a.getProfil()!=null&&a.getProfil().getStatut()==EStatut.A)roles.addAll(a.getProfil().getRoles());return roles.stream().anyMatch(r->r.getStatut()==EStatut.A&&r.getCode().equals(code));}
    private boolean manage(BaUser a){return has(a,GERER)||has(a,"BA_ADMIN");}
    private BaUser manager(){var a=security.current();if(!manage(a))throw forbidden("Permission EXTRACTIONS_GERER requise.");return a;}
    private BaUser reader(){var a=security.current();if(!manage(a)&&!has(a,LIRE))throw forbidden("Permission REQUETES_CONSULTER requise.");return a;}
    private BaUser requester(){var a=security.current();if(!manage(a)&&!has(a,DEMANDER))throw forbidden("Permission EXTRACTIONS_DEMANDER requise.");return a;}
    private BaDepartement department(BaUser u){return u.getDepartement()!=null?u.getDepartement():u.getService()==null?null:u.getService().getDepartement();}
    private BaService selectedService(String id,BaDepartement dep){
        if(clean(id).isEmpty())return null;
        return services.findById(clean(id)).filter(s->s.getStatut()==EStatut.A&&s.getDepartement()!=null&&s.getDepartement().getId().equals(dep.getId()))
            .orElseThrow(()->bad("Choisissez un service actif rattaché au département demandeur."));
    }
    private Reference currentService(BaUser u){
        var s=u.getService();var dep=department(u);
        return s!=null&&s.getStatut()==EStatut.A&&dep!=null&&dep.getStatut()==EStatut.A&&s.getDepartement()!=null&&s.getDepartement().getId().equals(dep.getId())?new Reference(s.getId(),s.getNom()):null;
    }
    private Reference ref(BaUser u){return u==null?null:new Reference(u.getId(),name(u));}
    private static String name(BaUser u){String name=clean(u.getPrenom())+" "+clean(u.getNom());return name.isBlank()?u.getUsername():name.trim();}
    private void audit(BaUser a,String action,String target){var log=new BaLogDto(EAction.V,"Extractions Oracle");log.setCreatedBy(a.getUsername());log.setDetails("action="+action+";acteur="+a.getId()+";cible="+target);audit.log(log);}
    private void validate(Object o){var errors=validator.validate(o);if(!errors.isEmpty())throw bad(errors.iterator().next().getMessage());}
    private static String clean(String v){return v==null?"":v.trim();}
    private static String term(String v){return "%"+clean(v).toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";}
    private static void bounds(String search,int page,int size){if(clean(search).length()>200||page<0||size<1||size>100)throw bad("Filtres ou pagination invalides.");}
}

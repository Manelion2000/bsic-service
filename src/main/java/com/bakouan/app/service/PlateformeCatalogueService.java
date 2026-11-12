package com.bakouan.app.service;

import com.bakouan.app.config.FeatureConfig;
import com.bakouan.app.dto.*;
import com.bakouan.app.dto.PlateformeDtos.*;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import com.bakouan.app.utils.BaUtils;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

@Service @RequiredArgsConstructor @Transactional
public class PlateformeCatalogueService {
    public static final String CONSULTER="PLATEFORMES_CONSULTER", GERER="PLATEFORMES_GERER";
    private final CongeSecurity security;
    private final BaPlateformeRepository plateformes;
    private final BaPlateformeRevisionRepository revisions;
    private final BaDepartementRepository departements;
    private final BaUserRepository users;
    private final BaCircuitRepository circuits;
    private final FeatureConfig features;
    private final BaLogService audit;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final BaPlateformeManuelRepository manuels;
    private final PlateformeManuelValidator manuelValidator;

    @Transactional(readOnly=true)
    public Configuration configuration() {
        var actor=security.current();boolean manage=has(actor,GERER)||has(actor,"BA_ADMIN");
        boolean read=manage||has(actor,CONSULTER);
        return new Configuration(read,manage,features.isHabilitationsEnabled(),
            read?departements.findByStatut(EStatut.A).stream().map(this::ref).sorted(byName()).toList():List.of(),
            manage?users.findAll().stream().filter(CongeSecurity::available).map(this::ref).sorted(byName()).toList():List.of(),
            manage&&features.isHabilitationsEnabled()?circuits.findByStatutAndActifOrderByCreatedDateDesc(EStatut.A,true).stream()
                .map(c->new Reference(c.getId(),c.getCode()+" — "+c.getLibelle(),true)).toList():List.of());
    }
    @Transactional(readOnly=true)
    public Page list(String search,PlateformeFiche.Categorie category,PlateformeFiche.CycleVie lifecycle,
                     PlateformeFiche.Criticite criticality,String department,boolean archived,int page,int size) {
        actor(false);if(page<0||size<1||size>100||clean(search).length()>200)throw bad("Filtres ou pagination invalides.");
        Specification<BaPlateforme> spec=(root,q,cb)-> {
            var clauses=new ArrayList<jakarta.persistence.criteria.Predicate>();
            clauses.add(cb.equal(root.get("statut"),archived?EStatut.D:EStatut.A));
            if(!clean(department).isEmpty())clauses.add(cb.equal(root.get("departementProprietaire").get("id"),department));
            // jsonb_extract_path_text renvoie NULL pour les anciennes fiches non qualifiées.
            if(category!=null)clauses.add(cb.equal(cb.coalesce(cb.function("jsonb_extract_path_text",String.class,root.get("fiche"),cb.literal("categorie")),"AUTRE"),category.name()));
            if(lifecycle!=null)clauses.add(cb.equal(cb.coalesce(cb.function("jsonb_extract_path_text",String.class,root.get("fiche"),cb.literal("cycleVie")),"A_QUALIFIER"),lifecycle.name()));
            if(criticality!=null)clauses.add(cb.equal(cb.coalesce(cb.function("jsonb_extract_path_text",String.class,root.get("fiche"),cb.literal("criticite")),"A_QUALIFIER"),criticality.name()));
            if(!clean(search).isEmpty()) {
                String term="%"+clean(search).toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                clauses.add(cb.or(cb.like(cb.lower(root.get("code")),term,'\\'),cb.like(cb.lower(root.get("nom")),term,'\\'),cb.like(cb.lower(root.get("description")),term,'\\')));
            }
            return cb.and(clauses.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var rows=plateformes.findAll(spec,PageRequest.of(page,size,Sort.by("nom","id")));
        return new Page(rows.getContent().stream().map(this::line).toList(),rows.getTotalElements(),rows.getTotalPages(),page,size);
    }
    @Transactional(readOnly=true)
    public Detail detail(String id) { actor(false);return detail(loaded(id)); }
    @Transactional(readOnly=true)
    public List<Revision> history(String id,int page) {
        actor(false);loaded(id);if(page<0)throw bad("Page invalide.");
        return revisions.findByPlateformeIdOrderByNumeroDesc(id,PageRequest.of(page,20)).stream()
            .map(r->new Revision(r.getNumero(),r.getDate(),r.getAuteur(),r.getAction(),r.getMotif(),r.getSnapshot())).toList();
    }
    public Detail create(Ecriture input) {
        return create(input,ModificationsManuels.vides(),List.of());
    }
    public Detail create(Ecriture input,ModificationsManuels changes,List<org.springframework.web.multipart.MultipartFile> files) {
        var actor=actor(true);validate(input);var validated=validateManuels(changes,files);lockGraph();
        var entity=new BaPlateforme();apply(entity,input);entity=plateformes.saveAndFlush(entity);
        applyManuels(entity,actor,changes,validated);
        saveRevision(entity,actor,"CREATION",input.motif());return detail(entity);
    }
    public Detail update(String id,Ecriture input) {
        return update(id,input,ModificationsManuels.vides(),List.of());
    }
    public Detail update(String id,Ecriture input,ModificationsManuels changes,List<org.springframework.web.multipart.MultipartFile> files) {
        var actor=actor(true);validate(input);var validated=validateManuels(changes,files);lockGraph();var entity=locked(id);checkVersion(entity,input.version());
        if(entity.getStatut()!=EStatut.A)throw blocked("Restaurez la plateforme avant de modifier sa fiche.");
        baseline(entity,actor);apply(entity,input);entity.setLastModifiedDate(Instant.now());plateformes.saveAndFlush(entity);
        applyManuels(entity,actor,changes,validated);
        saveRevision(entity,actor,"MODIFICATION",input.motif());return detail(entity);
    }
    public BaPlateformeManuel downloadManuel(String plateformeId,String manuelId) {
        var actor=actor(false);loaded(plateformeId);
        var file=manuels.findByIdAndPlateformeId(manuelId,plateformeId)
            .filter(m->m.getStatut()==EStatut.A||m.getStatut()==EStatut.D).orElseThrow(()->missing("Manuel introuvable pour cette plateforme."));
        var log=new BaLogDto(EAction.V,"Manuel plateforme "+plateformeId);log.setCreatedBy(actor.getUsername());log.setDetails("TÉLÉCHARGEMENT ; manuel "+manuelId+" ; SHA-256 "+file.getSha256());audit.log(log);
        return file;
    }
    private List<PlateformeManuelValidator.Fichier> validateManuels(ModificationsManuels changes,List<org.springframework.web.multipart.MultipartFile> files) {
        validate(changes);files=files==null?List.of():files;
        if(changes.ajouts().size()!=files.size())throw bad("Chaque fichier doit avoir un titre dans la sélection des manuels.");
        if(files.stream().mapToLong(org.springframework.web.multipart.MultipartFile::getSize).sum()>PlateformeManuelValidator.MAX_BATCH)
            throw bad("La sélection de manuels ne doit pas dépasser 20 Mo par enregistrement.");
        if(new HashSet<>(changes.retraits()).size()!=changes.retraits().size())throw bad("Les manuels à retirer doivent être distincts.");
        var result=files.stream().map(manuelValidator::validate).toList();
        if(result.stream().map(PlateformeManuelValidator.Fichier::sha256).distinct().count()!=result.size())throw bad("Le même manuel a été sélectionné plusieurs fois.");
        return result;
    }
    private void applyManuels(BaPlateforme p,BaUser actor,ModificationsManuels changes,List<PlateformeManuelValidator.Fichier> files) {
        if(changes.ajouts().isEmpty()&&changes.retraits().isEmpty())return;
        var existing=manuels.metadata(p.getId());var ids=existing.stream().map(Manuel::id).collect(java.util.stream.Collectors.toSet());
        if(!ids.containsAll(changes.retraits()))throw bad("Un manuel à retirer est introuvable sur cette plateforme.");
        if(existing.size()-changes.retraits().size()+files.size()>20)throw bad("Une plateforme peut référencer au maximum 20 manuels actifs.");
        if(!changes.retraits().isEmpty())manuels.retire(p.getId(),changes.retraits(),actor.getUsername(),Instant.now());
        for(int i=0;i<files.size();i++) {
            var file=files.get(i);var metadata=changes.ajouts().get(i);
            if(manuels.existsByPlateformeIdAndSha256AndStatut(p.getId(),file.sha256(),EStatut.A))throw blocked("Ce manuel est déjà présent sur cette plateforme : "+file.nom()+".");
            var manual=new BaPlateformeManuel();manual.setPlateforme(p);manual.setTitre(clean(metadata.titre()));manual.setDescription(clean(metadata.description()));
            manual.setNomFichier(file.nom());manual.setContentType(file.mime());manual.setTaille(file.contenu().length);manual.setContenu(file.contenu());
            manual.setSha256(file.sha256());manual.setAuteur(name(actor));manuels.save(manual);
        }
    }
    public Detail archive(String id,Operation op,boolean restore) {
        var actor=actor(true);validate(op);lockGraph();var entity=locked(id);checkVersion(entity,op.version());
        if(entity.getStatut()==(restore?EStatut.A:EStatut.D))throw blocked(restore?"La plateforme est déjà active.":"La plateforme est déjà archivée.");
        if(!restore&&plateformes.findDependants(id).stream().anyMatch(p->p.getStatut()==EStatut.A))
            throw blocked("Une plateforme active dépend de cette plateforme. Modifiez ses dépendances avant l’archivage.");
        if(restore&&entity.getDependances().stream().anyMatch(p->p.getStatut()!=EStatut.A))
            throw blocked("Restaurez d’abord les plateformes dont cette fiche dépend.");
        baseline(entity,actor);entity.setStatut(restore?EStatut.A:EStatut.D);plateformes.saveAndFlush(entity);
        saveRevision(entity,actor,restore?"RESTAURATION":"ARCHIVAGE",op.motif());return detail(entity);
    }
    // Compatibilité : les anciennes écritures passent par les mêmes contrôles, sans effacer la fiche détaillée.
    public BaPlateformeDto legacyCreate(BaPlateformeDto dto) {
        var result=create(legacy(dto,PlateformeFiche.vide(),null,null,null,List.of()));return legacyDto(result);
    }
    public BaPlateformeDto legacyUpdate(String id,BaPlateformeDto dto) {
        actor(true);lockGraph();var current=locked(id);
        var result=update(id,legacy(dto,current.getFiche(),id(current.getDepartementProprietaire()),id(current.getResponsableMetier()),
            id(current.getResponsableTechnique()),current.getDependances().stream().map(BaPlateforme::getId).toList()));
        return legacyDto(result);
    }
    private Ecriture legacy(BaPlateformeDto d,PlateformeFiche fiche,String dept,String business,String technical,List<String> dependencies) {
        return new Ecriture(d.getCode(),d.getNom(),d.getDescription(),d.getTypePlateforme(),d.getIdCircuit(),dept,business,technical,fiche,dependencies,d.getVersion(),"Modification depuis le référentiel des habilitations");
    }
    private BaPlateformeDto legacyDto(Detail d) {
        var dto=new BaPlateformeDto();dto.setId(d.id());dto.setCode(d.code());dto.setNom(d.nom());dto.setDescription(d.description());
        dto.setTypePlateforme(d.typePlateforme());dto.setVersion(d.version());
        if(d.circuit()!=null){dto.setIdCircuit(d.circuit().id());dto.setLibelleCircuit(d.circuit().nom());}return dto;
    }
    private void apply(BaPlateforme p,Ecriture in) {
        if(plateformes.existsByCodeIgnoreCaseAndIdNot(clean(in.code()),p.getId()))throw blocked("Ce code de plateforme est déjà utilisé, y compris dans les archives.");
        // Résoudre et vérifier toutes les références avant de modifier l'entité gérée.
        // Sinon une requête de résolution peut provoquer un flush JPA intermédiaire.
        BaCircuit circuit=p.getCircuit();
        String circuitId=clean(in.idCircuit());
        if(!Objects.equals(circuitId,id(p.getCircuit()))) {
            if(!features.isHabilitationsEnabled())throw bad("Les habilitations sont désactivées : le circuit existant doit être conservé.");
            circuit=circuitId.isEmpty()?null:circuits.findLockedById(circuitId)
                .filter(c->c.getStatut()==EStatut.A&&Boolean.TRUE.equals(c.getActif())).orElseThrow(()->bad("Circuit introuvable ou inactif."));
        }
        var department=clean(in.departementId()).isEmpty()?null:departements.findById(in.departementId())
            .filter(d->d.getStatut()==EStatut.A||Objects.equals(id(p.getDepartementProprietaire()),d.getId())).orElseThrow(()->bad("Département introuvable ou inactif."));
        var business=owner(in.responsableMetierId(),p.getResponsableMetier());
        var technical=owner(in.responsableTechniqueId(),p.getResponsableTechnique());
        var ids=in.dependanceIds()==null?List.<String>of():in.dependanceIds();
        if(new HashSet<>(ids).size()!=ids.size()||ids.contains(p.getId()))throw bad("Les dépendances doivent être distinctes et différentes de la plateforme elle-même.");
        var dependencies=plateformes.findByIdInAndStatut(ids,EStatut.A);
        if(dependencies.size()!=ids.size())throw bad("Une dépendance est introuvable ou archivée.");
        p.getDependances().clear();p.getDependances().addAll(dependencies);p.setFiche(in.fiche());
        p.setCode(clean(in.code()));p.setNom(clean(in.nom()));p.setDescription(clean(in.description()));
        p.setTypePlateforme(in.typePlateforme()==null?ETypePlateforme.RESEAUX:in.typePlateforme());
        p.setCircuit(circuit);p.setDepartementProprietaire(department);p.setResponsableMetier(business);p.setResponsableTechnique(technical);
    }
    private BaUser owner(String selected,BaUser previous) {
        if(clean(selected).isEmpty())return null;
        // Conserver un ancien responsable indisponible permet de corriger d'autres rubriques.
        return users.findById(selected).filter(u->available(u)||Objects.equals(id(previous),selected)).orElseThrow(()->bad("Responsable introuvable ou indisponible."));
    }
    private void validate(Object input) {
        var errors=validator.validate(input);if(!errors.isEmpty())throw bad(errors.stream().map(e->e.getPropertyPath()+" : "+e.getMessage()).sorted().reduce((a,b)->a+" ; "+b).orElse("Fiche invalide."));
        if(input instanceof Ecriture in) {
            var fiche=in.fiche();
            fiche.liens().forEach(l->url(l.url(),true));fiche.environnements().forEach(e->url(e.url(),false));
            if(fiche.dateMiseService()!=null&&fiche.dateFinSupport()!=null&&fiche.dateFinSupport().isBefore(fiche.dateMiseService()))throw bad("La fin de support précède la mise en service.");
        }
    }
    private void url(String value,boolean required) {
        if(clean(value).isEmpty()){if(required)throw bad("Adresse de documentation obligatoire.");return;}
        try {
            URI uri=URI.create(value);
            if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)
                throw new IllegalArgumentException();
        } catch(IllegalArgumentException e){throw bad("Utilisez une adresse HTTP(S) sans identifiant, mot de passe, paramètre ni fragment.");}
    }
    private BaUser actor(boolean manage) {
        var actor=security.current();if(!has(actor,"BA_ADMIN")&&!has(actor,GERER)&&(manage||!has(actor,CONSULTER)))
            throw forbidden("Permission requise : "+(manage?GERER:CONSULTER)+".");return actor;
    }
    private boolean has(BaUser actor,String code) {
        var roles=new ArrayList<>(actor.getRoles());if(actor.getProfil()!=null&&actor.getProfil().getStatut()==EStatut.A)roles.addAll(actor.getProfil().getRoles());
        return roles.stream().anyMatch(r->r.getStatut()==EStatut.A&&code.equals(r.getCode()));
    }
    private BaPlateforme loaded(String id){return plateformes.findById(id).orElseThrow(()->missing("Plateforme introuvable."));}
    private BaPlateforme locked(String id){return plateformes.findLockedById(id).orElseThrow(()->missing("Plateforme introuvable."));}
    private void checkVersion(BaPlateforme p,Long version){if(version==null)throw bad("La version de la fiche est obligatoire.");if(!version.equals(p.getVersion()))throw blocked("La fiche a été modifiée. Rechargez-la avant de recommencer.");}
    // Sérialise le graphe, y compris les archivages : aucune dépendance active vers une fiche archivée.
    private void lockGraph(){jdbc.query("SELECT pg_advisory_xact_lock(281028)",(org.springframework.jdbc.core.ResultSetExtractor<Void>)rs->null);}
    private void baseline(BaPlateforme p,BaUser actor) {
        if(revisions.findByPlateformeIdOrderByNumeroDesc(p.getId(),PageRequest.of(0,1)).isEmpty())
            saveRevision(p,actor,"REPRISE","État existant avant la première modification dans le catalogue ; approbation non supposée.");
    }
    private void saveRevision(BaPlateforme p,BaUser actor,String action,String motif) {
        var r=new BaPlateformeRevision();r.setId(BaUtils.randomUUID());r.setPlateforme(p);r.setNumero(p.getVersion());r.setDate(Instant.now());
        r.setAuteurId(actor.getId());r.setAuteur(name(actor));r.setAction(action);r.setMotif(clean(motif));r.setSnapshot(detail(p));revisions.save(r);
        var log=new BaLogDto(action.equals("CREATION")?EAction.C:EAction.U,"Catalogue plateforme "+p.getId());
        log.setCreatedBy(actor.getUsername());log.setDetails(action+" ; version "+p.getVersion()+" ; "+clean(motif));audit.log(log);
    }
    private Ligne line(BaPlateforme p){var f=p.getFiche();return new Ligne(p.getId(),p.getCode(),p.getNom(),p.getDescription(),p.getTypePlateforme(),f.categorie(),f.cycleVie(),f.criticite(),ref(p.getDepartementProprietaire()),ref(p.getResponsableTechnique()),p.getStatut()==EStatut.D,p.getVersion(),f.dateProchaineRevue(),f.dateFinSupport(),modified(p));}
    private Detail detail(BaPlateforme p){return new Detail(p.getId(),p.getCode(),p.getNom(),p.getDescription(),p.getTypePlateforme(),p.getCircuit()==null?null:new Reference(p.getCircuit().getId(),p.getCircuit().getLibelle(),p.getCircuit().getStatut()==EStatut.A),ref(p.getDepartementProprietaire()),ref(p.getResponsableMetier()),ref(p.getResponsableTechnique()),p.getFiche(),p.getDependances().stream().map(this::ref).sorted(byName()).toList(),plateformes.findDependants(p.getId()).stream().map(this::ref).sorted(byName()).toList(),p.getStatut()==EStatut.D,p.getVersion(),modified(p),manuels.metadata(p.getId()));}
    private Reference ref(BaPlateforme p){return new Reference(p.getId(),p.getNom(),p.getStatut()==EStatut.A);}
    private Reference ref(BaDepartement d){return d==null?null:new Reference(d.getId(),d.getNom(),d.getStatut()==EStatut.A);}
    private Reference ref(BaUser u){return u==null?null:new Reference(u.getId(),name(u),available(u));}
    private static String name(BaUser u){String result=clean(u.getPrenom())+" "+clean(u.getNom());return result.isBlank()?u.getUsername():result.trim();}
    private static String id(BaAbstractAuditingEntity value){return value==null?"":value instanceof BaUser u?u.getId():value instanceof BaDepartement d?d.getId():value instanceof BaCircuit c?c.getId():"";}
    private static String clean(String s){return s==null?"":s.trim();}
    private static Instant modified(BaPlateforme p){return p.getLastModifiedDate()==null?p.getCreatedDate():p.getLastModifiedDate();}
    private static Comparator<Reference> byName(){return Comparator.comparing(r->Objects.toString(r.nom(),""),String.CASE_INSENSITIVE_ORDER);}
}

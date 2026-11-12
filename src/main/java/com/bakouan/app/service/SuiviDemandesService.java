package com.bakouan.app.service;

import com.bakouan.app.config.FeatureConfig;
import com.bakouan.app.dto.*;
import com.bakouan.app.dto.SuiviDemandesDtos.*;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class SuiviDemandesService {
    public static final String DEPARTMENT="SUIVI_DEMANDES_DEPARTEMENT", GLOBAL="SUIVI_DEMANDES_GLOBAL";
    private static final List<String> MODULES=List.of("DEMARCHE","ABSENCE","SORTIE","CONGE","REPRISE","HABILITATION","REINITIALISATION","EXTRACTION");
    private static final Set<String> STATES=Set.of("EN_ATTENTE","APPROUVEE","REJETEE","ANNULEE","RETIREE","REMPLACEE","TERMINEE");
    private final CongeSecurity security;
    private final SuiviDemandesRepository repository;
    private final BaRhCircuitRepository circuits;
    private final FeatureConfig features;
    private final BaLogService audit;

    public Configuration configuration() {
        var actor=security.current();var scope=scope(actor,false);
        if(scope==null)return new Configuration(false,null,null,List.of(),List.of(),null);
        var department=department(actor);
        String blockage=!scope.global && scope.department==null?"Rattachez votre compte à un département pour utiliser le suivi départemental.":null;
        return new Configuration(true,scope.global?"GLOBAL":"DEPARTEMENT",department==null?null:new Structure(department.getId(),department.getNom()),
                blockage==null?repository.departments(scope.global?null:scope.department):List.of(),availableModules(),blockage);
    }
    public Page list(String module,String state,String search,String department,int page,int size) {
        var actor=security.current();var scope=scope(actor,true);String perimeter=perimeter(scope,department);
        module=clean(module);state=clean(state);search=clean(search);
        if(!module.isEmpty()&&!availableModules().contains(module))throw bad("Module de suivi inconnu ou désactivé.");
        if(!state.isEmpty()&&!STATES.contains(state))throw bad("Statut de suivi inconnu.");
        if(page<0||size<1||size>100||search.length()>200)throw bad("Filtres ou pagination invalides.");
        var result=repository.list(perimeter,features.isHabilitationsEnabled(),module,state,search,page,size);
        audit(actor,"LISTE",scope,module);
        return result;
    }
    public Detail detail(String module,String id) {
        var actor=security.current();var scope=scope(actor,true);
        if(!availableModules().contains(module))throw missing();
        // Le périmètre est appliqué dans la requête : un identifiant externe ou un brouillon donne 404.
        var row=repository.detail(scope.global?null:scope.department,features.isHabilitationsEnabled(),module,id).orElseThrow(SuiviDemandesService::missing);
        var source=repository.circuitId(module,id).flatMap(circuits::findById);
        RhCircuitDto circuit=source.map(this::safeCircuit).orElseGet(()->legacyCircuit(row));
        audit(actor,"DETAIL",scope,module+":"+id);
        return new Detail(row,circuit);
    }
    private Scope scope(BaUser actor,boolean required) {
        boolean global=has(actor,GLOBAL), department=has(actor,DEPARTMENT);
        if(!global&&!department){if(required)throw forbidden("Permission de suivi requise.");return null;}
        String id=CongeSecurity.departmentId(actor);
        if(!global&&id==null&&required)throw forbidden("Votre compte doit être rattaché à un département.");
        return new Scope(global,id);
    }
    private boolean has(BaUser actor,String code) {
        var roles=new ArrayList<>(actor.getRoles());
        if(actor.getProfil()!=null&&actor.getProfil().getStatut()==EStatut.A)roles.addAll(actor.getProfil().getRoles());
        return roles.stream().anyMatch(r->r.getStatut()==EStatut.A&&code.equals(r.getCode()));
    }
    private String perimeter(Scope scope,String requested) {
        String id=clean(requested);
        if(!scope.global&&!id.isEmpty()&&!id.equals(scope.department))throw forbidden("Le suivi est limité à votre département.");
        return scope.global?(id.isEmpty()?null:id):scope.department;
    }
    private List<String> availableModules(){return MODULES.stream().filter(m->features.isHabilitationsEnabled()||!Set.of("HABILITATION","REINITIALISATION").contains(m)).toList();}
    private BaDepartement department(BaUser a){return a.getDepartement()!=null?a.getDepartement():a.getService()==null?null:a.getService().getDepartement();}
    private RhCircuitDto safeCircuit(BaRhCircuit source) {
        var result=new RhCircuitDto();result.setStatut(source.getStatut());result.setDgObligatoire(source.isDgObligatoire());result.setEtapeCourante(source.getEtapeCourante());
        result.setEtapes(source.getEtapes().stream().map(e->{var s=new RhCircuitDto.Etape();s.setType(e.getType());s.setLibelle(e.getLibelle());s.setStatut(e.getStatut());
            s.setValidateurs(e.getValidateurs());s.setNomSignataire(e.getNomSignataire());s.setDateDecision(e.getDateDecision());return s;}).toList());
        // Ne jamais appeler RhCircuitService.view ici : il calcule les actions du signataire et expose les commentaires.
        result.setHistorique(source.getHistorique().stream().map(d->new RhCircuitDto.Decision(d.etape(),d.type(),d.decision(),null,d.nomSignataire(),d.date(),null)).toList());
        return result;
    }
    private RhCircuitDto legacyCircuit(Demande row) {
        var result=new RhCircuitDto();result.setStatut(row.etat());
        if("EXTRACTION".equals(row.module())){result.setBlocage("Suivi de traitement informatique sans circuit de signature : "+Objects.toString(row.etapeActive(),row.etat())+". Le contenu est réservé à l'espace des extractions.");return result;}
        var steps=repository.legacySteps(row.module(),row.id());var values=new ArrayList<RhCircuitDto.Etape>();
        for(var e:steps){var s=new RhCircuitDto.Etape();s.setType("HISTORIQUE");s.setLibelle("REPRISE".equals(row.module())?"Validation de la reprise":"Validation "+e.ordre());
            s.setStatut(switch(e.etat()){case "VALIDE","VALIDEE"->"VALIDEE";case "REJETE","REJETEE"->"REJETEE";default->"EN_ATTENTE";});
            if(e.titulaire()!=null)s.setValidateurs(List.of(new RhCircuitDto.Validateur(e.titulaire(),e.nom())));
            if(e.date()!=null){s.setNomSignataire(e.nom());s.setDateDecision(e.date().toInstant().toString());}
            values.add(s);
        }
        result.setEtapes(values);result.setEtapeCourante(values.size());
        for(int i=0;i<values.size();i++)if("EN_ATTENTE".equals(values.get(i).getStatut())){result.setEtapeCourante(i);break;}
        if(values.isEmpty())result.setBlocage("Circuit détaillé indisponible pour cette demande historique.");
        return result;
    }
    private void audit(BaUser actor,String action,Scope scope,String target){var log=new BaLogDto(EAction.V,"Suivi des demandes");log.setDetails("acteur="+actor.getId()+";action="+action+";perimetre="+(scope.global?"GLOBAL":scope.department)+";cible="+target);audit.log(log);}
    private record Scope(boolean global,String department) {}
    private static String clean(String s){return s==null?"":s.trim();}
    private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private static ResponseStatusException forbidden(String m){return new ResponseStatusException(HttpStatus.FORBIDDEN,m);}
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Demande introuvable dans votre périmètre de suivi.");}
}

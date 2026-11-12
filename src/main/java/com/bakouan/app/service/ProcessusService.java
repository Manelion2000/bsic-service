
package com.bakouan.app.service;
import com.bakouan.app.dto.*;import com.bakouan.app.dto.ProcessusDtos.*;import com.bakouan.app.enums.*;import com.bakouan.app.model.*;import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;import java.math.BigDecimal;import java.time.LocalDate;
import static com.bakouan.app.service.CongeSecurity.*;
/** Publications et dossiers utilisent le moteur de decision partage. Aucune execution de scripts fournis par l'utilisateur. */
@Service @Transactional @RequiredArgsConstructor
public class ProcessusService {
 private final BaActePdfService actes;
 public byte[] document(String id) {
  var dossier = dossiers.findById(id).filter(d -> d.getStatut() == EStatut.A)
          .orElseThrow(() -> missing("Dossier introuvable."));
  read(dossier, security.current());
  if (!"APPROUVEE".equals(dossier.getEtat())) throw blocked("L'acte est disponible après l'approbation finale.");
  Map<String, String> fields = new LinkedHashMap<>();
  fields.put("Démarche", dossier.getDemarche().getLibelle());
  fields.put("Version du circuit", Integer.toString(dossier.getPublication().getNumero()));
  for (var field : dossier.getChamps()) fields.put(field.libelle(), dossier.getValeurs().get(field.cle()));
  return actes.render(new BaActePdfService.Acte("Acte de validation", dossier.getId(), dossier.getEtat(),
          actes.identity(dossier.getDemandeur()), fields, dossier.getTitre(), actes.circuitVisas(dossier.getCircuit())));
 }
 private final CongeSecurity security; private final RhCircuitService engine;private final BaCircuitRepository models;private final BaCircuitPublicationRepository publications;private final BaDemarcheRepository catalogue;private final BaDemarcheDossierRepository dossiers;private final BaDemarchePieceRepository pieces;private final BaUserRepository users;private final BaDepartementRepository departments;private final BaServiceRepository services;private final BaAgenceRepository agencies;private final BaLogService logs;private final BaRhCircuitRepository frozenCircuits;
 public boolean manages(BaUser u){return security.has(u,"BA_ADMIN")||security.has(u,"PROCESSUS_CONFIGURER");}
 private BaUser manager(){BaUser u=security.current();if(!manages(u))throw forbidden("La configuration des circuits exige une permission dédiée.");return u;}
 public Map<String,Object> configuration(){var u=security.current();Map<String,Object> result=new LinkedHashMap<>();result.put("gerer",manages(u));result.put("demander",security.has(u,"PROCESSUS_DEMANDER"));result.put("valider",security.has(u,"PROCESSUS_VALIDER"));result.put("auditer",security.has(u,"PROCESSUS_AUDITER"));result.put("agentId",u.getId());result.put("fonctions",EFonctionEmploye.values());
  if(manages(u)){result.put("agents",users.findByStatut(EStatut.A).stream().filter(CongeSecurity::available).map(a->Map.of("id",a.getId(),"nom",CongeConfigurationService.name(a))).toList());result.put("departements",departments.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->Map.of("id",d.getId(),"nom",Objects.toString(d.getNom(),d.getId()))).toList());result.put("services",services.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->Map.of("id",d.getId(),"nom",Objects.toString(d.getNom(),d.getId()))).toList());result.put("agences",agencies.findAll().stream().filter(d->d.getStatut()==EStatut.A).map(d->Map.of("id",d.getId(),"nom",Objects.toString(d.getNom(),d.getId()))).toList());}return result;
 }
 public List<Map<String,Object>> modelList(){manager();return models.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().filter(BaCircuit::isAutonome).map(this::modelDto).toList();}
 public Map<String,Object> saveModel(String id,Modele input){manager();validateDefinition(input.definition(),false);BaCircuit c=id==null?new BaCircuit():lockedModel(id);if(id!=null)version(c.getLockVersion(),input.version());c.setAutonome(true);if(id==null)c.setCode("CIR-"+UUID.randomUUID().toString().substring(0,16).toUpperCase(Locale.ROOT));c.setLibelle(input.libelle().trim());c.setDescription(input.description());c.setActif(false);c.setStatu(ECircuitStatut.EN_CONFIGURATION);c.setDefinition(input.definition());models.saveAndFlush(c);audit("Configuration du circuit "+c.getId(),"Version "+c.getLockVersion());return modelDto(c);}
 public Map<String,Object> publish(String id,Publication input){manager();var c=lockedModel(id);version(c.getLockVersion(),input.version());validateDefinition(c.getDefinition(),true);var existing=publications.findByCircuitIdOrderByNumeroDesc(id);
  var latest=existing.stream().filter(p->p.getStatut()==EStatut.A).findFirst();
  if(latest.isPresent()&&sameContent(c,latest.get()))throw blocked("Ce contenu est déjà publié. Modifiez le brouillon avant de publier une nouvelle version.");
  var p=new BaCircuitPublication();p.setCircuit(c);p.setNumero(existing.isEmpty()?1:existing.get(0).getNumero()+1);p.setLibelle(c.getLibelle());p.setDescription(c.getDescription());p.setDefinition(c.getDefinition());p.setJustification(input.justification().trim());publications.saveAndFlush(p);c.setActif(true);c.setStatu(ECircuitStatut.ACTIF);models.saveAndFlush(c);audit("Publication circuit "+id,"Publication "+p.getId()+" n° "+p.getNumero()+" ; "+input.justification());return modelDto(c);}

 public Map<String,Object> deleteModels(Suppression input) {
  manager();
  Map<String,CibleSuppression> selectedModels=new TreeMap<>(),selectedPublications=new TreeMap<>();
  Map<String,String> publicationModels=new HashMap<>();
  Set<String> modelIds=new TreeSet<>();
  for(var target:input.cibles()) {
   if("CIRCUIT".equals(target.type())) {
    if(selectedModels.putIfAbsent(target.id(),target)!=null)throw bad("Circuit sélectionné plusieurs fois.");
    modelIds.add(target.id());
   } else if("PUBLICATION".equals(target.type())) {
    if(selectedPublications.putIfAbsent(target.id(),target)!=null)throw bad("Publication sélectionnée plusieurs fois.");
    String modelId=publications.findCircuitId(target.id()).orElseThrow(()->missing("Publication introuvable."));
    publicationModels.put(target.id(),modelId);modelIds.add(modelId);
   } else throw bad("Type de suppression inconnu.");
  }
  // Le même verrou protège aussi l'affectation et la publication. Ordre stable pour les lots.
  Map<String,BaCircuit> locked=new LinkedHashMap<>();
  for(String id:modelIds)locked.put(id,lockedModel(id));
  Map<String,BaCircuitPublication> removed=new LinkedHashMap<>();
  for(var target:selectedModels.values()) {
   var c=locked.get(target.id());version(c.getLockVersion(),target.version());
   for(var p:publications.findByCircuitIdOrderByNumeroDesc(c.getId())) {
    String reason=deletionBlockage(p);
    if(reason!=null)throw blocked(c.getLibelle()+" — version "+p.getNumero()+" : "+reason);
    if(p.getStatut()==EStatut.A)removed.put(p.getId(),p);
   }
  }
  for(var target:selectedPublications.values()) {
   if(selectedModels.containsKey(publicationModels.get(target.id())))throw bad("Sélectionnez le circuit complet ou ses versions, sans les cumuler.");
   var p=publications.findLockedById(target.id()).filter(v->v.getStatut()==EStatut.A).orElseThrow(()->missing("Publication introuvable."));
   version(p.getLockVersion(),target.version());
   String reason=deletionBlockage(p);
   if(reason!=null)throw blocked(p.getLibelle()+" — version "+p.getNumero()+" : "+reason);
   removed.put(p.getId(),p);
  }
  // Suppression logique : le contenu et l'auteur restent disponibles pour l'audit.
  for(var p:removed.values()) {
   p.setStatut(EStatut.D);
   auditDeletion("Suppression publication "+p.getId(),p.getLibelle()+" ; circuit "+p.getCircuit().getId()+" ; version "+p.getNumero());
  }
  publications.saveAllAndFlush(removed.values());
  for(var c:locked.values()) {
   if(selectedModels.containsKey(c.getId())) {
    c.setStatut(EStatut.D);c.setActif(false);c.setStatu(ECircuitStatut.INACTIF);
    auditDeletion("Suppression circuit "+c.getId(),c.getLibelle()+" ; révision "+c.getLockVersion());
   } else {
    boolean published=publications.findByCircuitIdOrderByNumeroDesc(c.getId()).stream().anyMatch(p->p.getStatut()==EStatut.A);
    c.setActif(published);c.setStatu(published?ECircuitStatut.ACTIF:ECircuitStatut.EN_CONFIGURATION);
   }
  }
  models.saveAllAndFlush(locked.values());
  return Map.of("circuitsSupprimes",selectedModels.size(),"publicationsSupprimees",removed.size());
 }
 private boolean sameContent(BaCircuit c,BaCircuitPublication p) {
  return Objects.equals(c.getLibelle(),p.getLibelle())&&Objects.equals(c.getDescription(),p.getDescription())&&Objects.equals(c.getDefinition(),p.getDefinition());
 }
 private String deletionBlockage(BaCircuitPublication p) {
  var assigned=catalogue.findByPublicationIdAndStatut(p.getId(),EStatut.A);
  if(!assigned.isEmpty())return "Affectée à : "+String.join(", ",assigned.stream().map(BaDemarche::getLibelle).toList())+". Réaffectez ces démarches à une autre version avant la suppression.";
  if(dossiers.existsByPublicationId(p.getId())||frozenCircuits.existsByPublicationId(p.getId()))return "Utilisée par une demande. Cette version et son historique doivent être conservés.";
  return null;
 }
 private BaCircuitPublication publicationForAssignment(String id) {
  String modelId=publications.findCircuitId(id).orElseThrow(()->bad("Publication introuvable."));
  lockedModel(modelId);
  return publications.findById(id).filter(p->p.getStatut()==EStatut.A).orElseThrow(()->bad("Publication supprimée ou introuvable."));
 }
 @Transactional(readOnly=true)
 public Map<String,Object> simulate(String id,String requesterId){
  manager();var c=model(id);var requester=users.findById(requesterId).filter(CongeSecurity::available).orElseThrow(()->bad("Agent indisponible."));validateDefinition(c.getDefinition(),true);
  try{
   var circuit=engine.proposal(requester,c.getDefinition());
   var dto=engine.toDto(circuit,requester);
   // Le studio ne connaît pas encore l'affectation du modèle. Le parcours RH
   // vérifie la fonction et le périmètre ; la permission des dossiers génériques
   // reste obligatoire à leur soumission, même pour un modèle hiérarchique.
   if("LIBRE".equals(c.getDefinition().mode())){
    var missing=missingGenericValidationPermission(circuit);
    if(missing.isPresent())return Map.of("circuit",dto,"blocage",missing.get());
   }
   return Map.of("circuit",dto);
  }catch(org.springframework.web.server.ResponseStatusException e){return Map.of("blocage",Objects.toString(e.getReason(),"Circuit bloqué"));}
 }
 public List<Map<String,Object>> catalogue(){var u=security.current();return catalogue.findByStatutOrderByLibelleAsc(EStatut.A).stream().filter(d->manages(u)||d.isActif()).map(this::demarcheDto).toList();}
 public Map<String,Object> saveDemarche(String id,Demarche input){manager();if(!Set.of("GENERIQUE","ABSENCE","SORTIE","CONGES").contains(input.domaine()))throw bad("Domaine non pris en charge.");if(!"GENERIQUE".equals(input.domaine())&&!input.domaine().equals(input.code()))throw bad("Une démarche RH utilise son code réservé.");if("GENERIQUE".equals(input.domaine())&&Set.of("ABSENCE","SORTIE","CONGES").contains(input.code()))throw bad("Ce code est réservé au module RH.");validateFields(input.champs());var p=publicationForAssignment(input.publicationId());if(!"GENERIQUE".equals(input.domaine())&&!"HIERARCHIQUE".equals(p.getDefinition().mode()))throw bad("Les démarches RH conservent leur circuit organisationnel obligatoire.");
  var d=id==null?new BaDemarche():catalogue.findLockedById(id).orElseThrow(()->missing("Démarche introuvable."));if(id!=null){version(d.getLockVersion(),input.version());if(!d.getCode().equals(input.code())||!d.getDomaine().equals(input.domaine()))throw bad("Le code et le domaine d'une démarche sont immuables.");}if(id==null&&catalogue.findByCode(input.code()).isPresent())throw blocked("Ce code de démarche existe déjà.");d.setCode(input.code());d.setLibelle(input.libelle().trim());d.setDescription(input.description());d.setDomaine(input.domaine());d.setPublication(p);d.setActif(input.actif());d.setChamps(input.champs()==null?List.of():input.champs());catalogue.saveAndFlush(d);audit("Affectation démarche "+d.getCode(),"Publication "+p.getId()+" ; active "+d.isActif());return demarcheDto(d);
 }
 public List<Map<String,Object>> dossierList(String view){BaUser u=security.current();if(!Set.of("mes","a-traiter","audit").contains(view))throw bad("Vue inconnue.");if("audit".equals(view))security.require(u,"PROCESSUS_AUDITER");return dossiers.findByStatutOrderByCreatedDateDesc(EStatut.A).stream().filter(d->switch(view){case "mes"->d.getDemandeur().getId().equals(u.getId());case "a-traiter"->security.has(u,"PROCESSUS_VALIDER")&&engine.canDecide(d.getCircuit(),u);default->true;}).map(d->dossierDto(d,u)).toList();}
 public Map<String,Object> detail(String id){var u=security.current();var d=dossier(id);read(d,u);return dossierDto(d,u);}
 public Map<String,Object> saveDossier(String id,Dossier input,String key){var u=security.require("PROCESSUS_DEMANDER");users.findLockedById(u.getId()).orElseThrow();BaDemarcheDossier d;
  if(id==null){if(key==null||key.isBlank()||key.length()>100)throw bad("Une clé de création est requise.");String hash=hash(input);var replay=dossiers.findByDemandeurIdAndCleCreation(u.getId(),key);if(replay.isPresent()){if(!hash.equals(replay.get().getEmpreinteCreation()))throw blocked("La clé a déjà été utilisée pour un autre contenu.");return dossierDto(replay.get(),u);}var dem=catalogue.findById(input.demarcheId()).filter(v->v.getStatut()==EStatut.A&&v.isActif()).orElseThrow(()->bad("Démarche inactive."));if(!"GENERIQUE".equals(dem.getDomaine()))throw bad("Cette démarche se soumet depuis son écran métier.");d=new BaDemarcheDossier();d.setDemarche(dem);d.setPublication(dem.getPublication());d.setDemandeur(u);d.setChamps(dem.getChamps());d.setCleCreation(key);d.setEmpreinteCreation(hash);
  }else{d=lockedDossier(id);owner(d,u);version(d.getLockVersion(),input.version());if(!"BROUILLON".equals(d.getEtat())||!input.demarcheId().equals(d.getDemarche().getId()))throw blocked("Seul un brouillon de la même démarche peut être modifié.");}
  values(d.getChamps(),input.valeurs(),false);d.setTitre(input.titre().trim());d.setValeurs(input.valeurs()==null?new LinkedHashMap<>():new LinkedHashMap<>(input.valeurs()));dossiers.saveAndFlush(d);audit("Brouillon démarche "+d.getId(),"Révision "+d.getLockVersion());return dossierDto(d,u);
 }
 public Map<String,Object> action(String id,String action,Action input){BaUser u=security.current();if(!Set.of("soumettre","approuver","rejeter","retirer").contains(action))throw bad("Action inconnue.");var d=lockedDossier(id);read(d,u);String fingerprint=hash(List.of(action,input));String prior=d.getOperations().get(input.cle());if(prior!=null){if(!prior.equals(fingerprint))throw blocked("Cette clé d'opération porte un autre contenu.");return dossierDto(d,u);}version(d.getLockVersion(),input.version());
  switch(action){case "soumettre"->{owner(d,u);security.require(u,"PROCESSUS_DEMANDER");if(!"BROUILLON".equals(d.getEtat()))throw blocked("La demande est déjà soumise.");if(!d.getDemarche().isActif())throw blocked("La démarche est désactivée.");values(d.getChamps(),d.getValeurs(),true);proposal(d.getDemandeur(),d.getPublication().getDefinition());d.setCircuit(engine.freeze(d.getDemandeur(),d.getPublication().getDefinition()));d.getCircuit().setPublicationId(d.getPublication().getId());d.setEtat("EN_ATTENTE");}
   case "retirer"->{owner(d,u);if(!"EN_ATTENTE".equals(d.getEtat()))throw blocked("Seule une demande en attente peut être retirée.");engine.cancel(d.getCircuit(),u);d.setEtat("RETIREE");}
   default->{security.require(u,"PROCESSUS_VALIDER");if(!"EN_ATTENTE".equals(d.getEtat())||input.etape()==null)throw blocked("La demande ou son étape n'est plus en attente.");String outcome=engine.decide(d.getCircuit(),u,input.etape(),"approuver".equals(action),input.commentaire());if("VALIDEE".equals(outcome))d.setEtat("APPROUVEE");else if("REJETEE".equals(outcome))d.setEtat("REJETEE");}}
  d.getOperations().put(input.cle(),fingerprint);dossiers.saveAndFlush(d);audit("Démarche "+d.getId()+" : "+action,"Version "+input.version()+" ; étape "+input.etape()+" ; "+Objects.toString(input.commentaire(),""));return dossierDto(d,u);
 }
 public void validateDefinition(Definition d,boolean publication){if(d==null||!Set.of("HIERARCHIQUE","LIBRE").contains(d.mode()))throw bad("Choisissez le mode de circuit.");if(!Set.of("DEFINITIF","AUTRES_VALIDATEURS").contains(d.politiqueRefus()))throw bad("Choisissez explicitement le traitement du refus.");if("HIERARCHIQUE".equals(d.mode())){if(d.dgObligatoire()==null)throw bad("Précisez l'étape DG.");if(d.etapes()!=null&&!d.etapes().isEmpty())throw bad("Le mode hiérarchique construit ses étapes depuis la fonction du demandeur.");return;}if(d.dgObligatoire()!=null)throw bad("Le mode libre définit explicitement toutes ses étapes.");if(d.etapes()==null||publication&&d.etapes().isEmpty())throw bad("Ajoutez au moins une étape.");
  for(var s:d.etapes()){if(!Set.of("FONCTION","UTILISATEURS").contains(s.selection()))throw bad("Sélection de validateurs inconnue.");if("FONCTION".equals(s.selection())&&s.fonction()==null)throw bad("La fonction organisationnelle est obligatoire.");if("UTILISATEURS".equals(s.selection())&&(s.utilisateurs()==null||s.utilisateurs().isEmpty()))throw bad("Sélectionnez au moins un titulaire.");if(s.departementDemandeur()&&(s.departementId()!=null||s.serviceId()!=null))throw bad("Le périmètre dynamique ne doit pas être associé à un département ou service fixe.");if(s.departementId()!=null&&departments.findById(s.departementId()).filter(v->v.getStatut()==EStatut.A).isEmpty())throw bad("Département indisponible.");if(s.serviceId()!=null){var svc=services.findById(s.serviceId()).filter(v->v.getStatut()==EStatut.A).orElseThrow(()->bad("Service indisponible."));if(s.departementId()!=null&&(svc.getDepartement()==null||!s.departementId().equals(svc.getDepartement().getId())))throw bad("Service et département incompatibles.");}if(s.agenceId()!=null&&agencies.findById(s.agenceId()).filter(v->v.getStatut()==EStatut.A).isEmpty())throw bad("Agence indisponible.");if(s.utilisateurs()!=null)for(String uid:s.utilisateurs())if(users.findById(uid).filter(CongeSecurity::available).isEmpty())throw bad("Titulaire indisponible.");}
 }
 private void validateFields(List<Champ> fields){if(fields==null)return;Set<String> keys=new HashSet<>();for(var f:fields){if(!keys.add(f.cle())||!Set.of("TEXTE","DATE","NOMBRE","CHOIX").contains(f.type()))throw bad("Champ dupliqué ou type inconnu.");if("CHOIX".equals(f.type())&&(f.choix()==null||f.choix().isEmpty()||new HashSet<>(f.choix()).size()!=f.choix().size()))throw bad("Définissez des choix distincts.");}}
 private void values(List<Champ> fields,Map<String,String> values,boolean submitting){Map<String,String> data=values==null?Map.of():values;if(data.keySet().stream().anyMatch(k->fields.stream().noneMatch(f->f.cle().equals(k))))throw bad("Champ hors du formulaire publié.");for(var f:fields){String value=data.get(f.cle());if(value==null||value.isBlank()){if(submitting&&f.obligatoire())throw bad("Champ obligatoire : "+f.libelle());continue;}if(value.length()>4000)throw bad("Champ trop long : "+f.libelle());try{switch(f.type()){case "DATE"->LocalDate.parse(value);case "NOMBRE"->{BigDecimal n=new BigDecimal(value);if(n.precision()>18||n.scale()>4)throw new IllegalArgumentException();}case "CHOIX"->{if(!f.choix().contains(value))throw new IllegalArgumentException();}default->{}}}catch(RuntimeException e){throw bad("Valeur invalide : "+f.libelle());}}}
 private BaRhCircuit proposal(BaUser requester,Definition definition){var c=engine.proposal(requester,definition);missingGenericValidationPermission(c).ifPresent(message->{throw blocked(message);});return c;}
 private Optional<String> missingGenericValidationPermission(BaRhCircuit circuit){
  for(var step:circuit.getEtapes())if(engine.candidates(circuit,step).stream().noneMatch(u->security.has(u,"PROCESSUS_VALIDER")))
   return Optional.of("Aucun titulaire de l'étape "+step.getLibelle()+" ne possède la permission PROCESSUS_VALIDER. Pour une démarche générique, un administrateur doit attribuer ce rôle à au moins un validateur éligible dans Utilisateurs → Gestion des rôles.");
  return Optional.empty();
 }
 private BaCircuit model(String id){return models.findById(id).filter(c->c.isAutonome()&&c.getStatut()==EStatut.A).orElseThrow(()->missing("Modèle introuvable."));}
 private BaCircuit lockedModel(String id){var c=models.findLockedById(id).orElseThrow(()->missing("Modèle introuvable."));if(!c.isAutonome()||c.getStatut()!=EStatut.A)throw missing("Modèle introuvable.");return c;}
 private BaDemarcheDossier dossier(String id){return dossiers.findById(id).filter(d->d.getStatut()==EStatut.A).orElseThrow(()->missing("Dossier introuvable."));}
 private BaDemarcheDossier lockedDossier(String id){String uid=dossiers.findDemandeurId(id).orElseThrow(()->missing("Dossier introuvable."));users.findLockedById(uid).orElseThrow();return dossiers.findLockedById(id).filter(d->d.getStatut()==EStatut.A).orElseThrow(()->missing("Dossier introuvable."));}
 private void owner(BaDemarcheDossier d,BaUser u){if(!d.getDemandeur().getId().equals(u.getId()))throw forbidden("Ce dossier appartient à un autre agent.");}
 private void read(BaDemarcheDossier d,BaUser u){if(!d.getDemandeur().getId().equals(u.getId())&&!engine.participant(d.getCircuit(),u)&&!security.has(u,"PROCESSUS_AUDITER"))throw forbidden("Dossier hors de votre périmètre.");}
 private void version(long current,Long expected){if(expected==null||current!=expected)throw blocked("La version a changé. Actualisez avant de recommencer.");}
 private Map<String,Object> modelDto(BaCircuit c){
  var r=new LinkedHashMap<String,Object>();r.put("id",c.getId());r.put("code",c.getCode());r.put("libelle",c.getLibelle());r.put("description",c.getDescription());r.put("version",c.getLockVersion());r.put("definition",c.getDefinition());
  var versions=publications.findByCircuitIdOrderByNumeroDesc(c.getId());
  List<Map<String,Object>> visible=new ArrayList<>();String modelBlockage=null;
  for(var p:versions) {
   String reason=deletionBlockage(p);
   if(modelBlockage==null&&reason!=null)modelBlockage="Version "+p.getNumero()+" : "+reason;
   if(p.getStatut()!=EStatut.A)continue;
   var dto=new LinkedHashMap<String,Object>();dto.put("id",p.getId());dto.put("version",p.getLockVersion());dto.put("numero",p.getNumero());dto.put("libelle",p.getLibelle());dto.put("description",p.getDescription());dto.put("date",p.getCreatedDate());dto.put("definition",p.getDefinition());dto.put("supprimable",reason==null);dto.put("blocageSuppression",reason);visible.add(dto);
  }
  r.put("publications",visible);r.put("supprimable",modelBlockage==null);r.put("blocageSuppression",modelBlockage);
  r.put("dejaPublie",versions.stream().filter(p->p.getStatut()==EStatut.A).findFirst().map(p->sameContent(c,p)).orElse(false));return r;
 }
 private Map<String,Object> demarcheDto(BaDemarche d){var r=new LinkedHashMap<String,Object>();r.put("id",d.getId());r.put("version",d.getLockVersion());r.put("code",d.getCode());r.put("libelle",d.getLibelle());r.put("description",d.getDescription());r.put("domaine",d.getDomaine());r.put("actif",d.isActif());r.put("publicationId",d.getPublication().getId());r.put("circuit",d.getPublication().getLibelle());r.put("numero",d.getPublication().getNumero());r.put("champs",d.getChamps());r.put("route",switch(d.getDomaine()){case "ABSENCE"->"/autorisations-absence";case "SORTIE"->"/autorisation-sorties";case "CONGES"->"/conges";default->"";});return r;}
 private Map<String,Object> dossierDto(BaDemarcheDossier d,BaUser u){var r=new LinkedHashMap<String,Object>();r.put("id",d.getId());r.put("version",d.getLockVersion());r.put("titre",d.getTitre());r.put("etat",d.getEtat());r.put("demarcheId",d.getDemarche().getId());r.put("demarche",d.getDemarche().getLibelle());r.put("publicationId",d.getPublication().getId());r.put("numero",d.getPublication().getNumero());r.put("demandeur",CongeConfigurationService.name(d.getDemandeur()));r.put("createdDate",d.getCreatedDate());r.put("valeurs",d.getValeurs());r.put("champs",d.getChamps());if(d.getCircuit()!=null){var dto=engine.toDto(d.getCircuit(),u);dto.setPeutDecider(dto.isPeutDecider()&&security.has(u,"PROCESSUS_VALIDER"));if("EN_ATTENTE".equals(d.getCircuit().getStatut())&&engine.candidates(d.getCircuit(),d.getCircuit().getEtapes().get(d.getCircuit().getEtapeCourante())).stream().noneMatch(v->engine.canDecide(d.getCircuit(),v)&&security.has(v,"PROCESSUS_VALIDER")))dto.setBlocage("Aucun titulaire éligible avec la permission de validation. La demande reste bloquée.");r.put("circuit",dto);}List<String> actions=new ArrayList<>();boolean own=d.getDemandeur().getId().equals(u.getId());if(own&&"BROUILLON".equals(d.getEtat())&&security.has(u,"PROCESSUS_DEMANDER"))actions.addAll(List.of("MODIFIER","SOUMETTRE","PIECE"));if(own&&"EN_ATTENTE".equals(d.getEtat()))actions.add("RETIRER");if(security.has(u,"PROCESSUS_VALIDER")&&engine.canDecide(d.getCircuit(),u))actions.addAll(List.of("APPROUVER","REJETER"));r.put("actions",actions);r.put("pieces",pieces.findByDossierIdAndStatut(d.getId(),EStatut.A).stream().map(p->Map.of("id",p.getId(),"nom",p.getNom(),"mime",p.getMime())).toList());return r;}
 public Map<String,Object> addPiece(String id,org.springframework.web.multipart.MultipartFile file,long version)throws java.io.IOException{var u=security.require("PROCESSUS_DEMANDER");var d=lockedDossier(id);owner(d,u);version(d.getLockVersion(),version);if(!"BROUILLON".equals(d.getEtat()))throw blocked("Les pièces sont figées à la soumission.");if(file.isEmpty()||file.getSize()>5*1024*1024)throw bad("La pièce doit peser au plus 5 Mo.");byte[] b=file.getBytes();String mime;if(b.length>=5&&new String(b,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))mime="application/pdf";else if(b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))mime="image/png";else if(b.length>=3&&b[0]==(byte)255&&b[1]==(byte)216&&b[2]==(byte)255)mime="image/jpeg";else throw bad("Format PDF, PNG ou JPEG requis.");var p=new BaDemarchePiece();p.setDossier(d);p.setMime(mime);String name=Objects.toString(file.getOriginalFilename(),"document").replaceAll("[^a-zA-Z0-9._ -]","_");p.setNom(name.substring(0,Math.min(150,name.length())));p.setContenu(b);pieces.save(p);d.getOperations().put("pièce:"+p.getId(),mime);dossiers.saveAndFlush(d);audit("Pièce ajoutée au dossier "+id,p.getId());return dossierDto(d,u);}
 public BaDemarchePiece piece(String id){var p=pieces.findById(id).filter(a->a.getStatut()==EStatut.A).orElseThrow(()->missing("Pièce introuvable."));read(p.getDossier(),security.current());return p;}
 public Map<String,Object> removePiece(String id,String pid,long expected){var u=security.require("PROCESSUS_DEMANDER");var d=lockedDossier(id);owner(d,u);version(d.getLockVersion(),expected);if(!"BROUILLON".equals(d.getEtat()))throw blocked("Les pièces sont figées.");var p=pieces.findById(pid).filter(a->a.getDossier().getId().equals(id)&&a.getStatut()==EStatut.A).orElseThrow(()->missing("Pièce introuvable."));p.setStatut(EStatut.D);d.getOperations().put("retrait-pièce:"+pid,"retirée");dossiers.saveAndFlush(d);audit("Pièce retirée du dossier "+id,pid);return dossierDto(d,u);}
 private String hash(Object value){try{return CongeLedgerService.hash(new com.fasterxml.jackson.databind.ObjectMapper().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true).writeValueAsString(value));}catch(Exception e){throw bad("Contenu invalide.");}}
 private void audit(String title,String detail){var log=new BaLogDto(EAction.U,title);log.setDetails(detail);logs.log(log);}
 private void auditDeletion(String title,String detail){var log=new BaLogDto(EAction.D,title);log.setDetails(detail);logs.log(log);}
}

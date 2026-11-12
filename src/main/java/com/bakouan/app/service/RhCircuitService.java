package com.bakouan.app.service;

import com.bakouan.app.dto.RhCircuitDto;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class RhCircuitService {
    private final BaUserRepository users;
    private final BaDgaPoleValidateurRepository assignments;
    private final BaRhCircuitRepository circuits;
    private final boolean dgEnabled;
    private final String refusalPolicy;
    private final SignatureService signatures;
    private BaValidationDelegationRepository delegationRepository;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private BaDemarcheRepository demarches;

    @org.springframework.beans.factory.annotation.Autowired
    public RhCircuitService(BaUserRepository users, BaDgaPoleValidateurRepository assignments,
                            BaRhCircuitRepository circuits,
                            @Value("${app.rh.validation.dg-enabled:false}") boolean dgEnabled,
                            @Value("${app.rh.validation.refusal-policy:NON_CONFIGURE}") String refusalPolicy,
                            BaValidationDelegationRepository delegationRepository, SignatureService signatures) {
        this(users,assignments,circuits,dgEnabled,refusalPolicy,signatures);
        this.delegationRepository=delegationRepository;
    }
    public RhCircuitService(BaUserRepository users,BaDgaPoleValidateurRepository assignments,BaRhCircuitRepository circuits,boolean dgEnabled,String refusalPolicy,SignatureService signatures) {
        this.users = users;
        this.assignments = assignments;
        this.circuits = circuits;
        this.dgEnabled = dgEnabled;
        if (!Set.of("NON_CONFIGURE", "DEFINITIF", "AUTRES_VALIDATEURS").contains(refusalPolicy))
            throw new IllegalArgumentException("Politique de refus RH invalide");
        this.refusalPolicy = refusalPolicy;
        this.signatures = signatures;
    }


    private BaRhCircuit proposalFor(String code,BaUser requester) {
        var published=demarches==null?null:demarches.findByCode(code).orElse(null);
        return proposalFor(published,requester);
    }
    private BaRhCircuit proposalFor(BaDemarche published,BaUser requester) {
        if(published==null)return build(requester);
        if(!published.isActif())throw blocked("Cette démarche est désactivée dans le catalogue.");
        var p=published.getPublication();if(!"HIERARCHIQUE".equals(p.getDefinition().mode()))throw blocked("Une démarche RH exige le circuit organisationnel.");
        var c=proposal(requester,Boolean.TRUE.equals(p.getDefinition().dgObligatoire()),p.getDefinition().politiqueRefus(),false);c.setPublicationId(p.getId());return c;
    }
    public BaRhCircuit freezeFor(String code,BaUser requester){return circuits.save(proposalFor(code,requester));}
    public RhCircuitDto previewFor(String code,BaUser requester){
        var published=demarches==null?null:demarches.findByCode(code).orElse(null);
        try{return toDto(proposalFor(published,requester),requester);}
        catch(ResponseStatusException e){
            boolean dgRequired=published==null?dgEnabled:Boolean.TRUE.equals(published.getPublication().getDefinition().dgObligatoire());
            var dto=blockedPreview(requester,dgRequired,e);
            if(published!=null){
                dto.setPublicationId(published.getPublication().getId());
                if(!"HIERARCHIQUE".equals(published.getPublication().getDefinition().mode()))dto.getEtapes().clear();
            }
            return dto;
        }
    }
    public BaRhCircuit proposalFor(String code,BaUser requester,boolean dg,String refusal,boolean delegation) {
        var published=demarches==null?null:demarches.findByCode(code).orElse(null);
        if(published==null)return proposal(requester,dg,refusal,delegation);
        if(!published.isActif())throw blocked("Cette démarche est désactivée dans le catalogue.");
        var p=published.getPublication();var def=p.getDefinition();
        if(!"HIERARCHIQUE".equals(def.mode())||Boolean.TRUE.equals(def.dgObligatoire())!=dg||!def.politiqueRefus().equals(refusal))throw blocked("L'affectation du circuit doit correspondre à la politique de congés confirmée.");
        var c=proposal(requester,dg,refusal,delegation);c.setPublicationId(p.getId());return c;
    }
    public BaRhCircuit freezeFor(String code,BaUser requester,boolean dg,String refusal,boolean delegation){return circuits.save(proposalFor(code,requester,dg,refusal,delegation));}
    public RhCircuitDto previewFor(String code,BaUser requester,boolean dg,String refusal,boolean delegation){try{return toDto(proposalFor(code,requester,dg,refusal,delegation),requester);}catch(ResponseStatusException e){var dto=new RhCircuitDto();dto.setBlocage(e.getReason());return dto;}}
    public BaRhCircuit freeze(BaUser requester) { return circuits.save(build(requester)); }
    public BaRhCircuit freeze(BaUser requester,boolean dgRequired,String refusal,boolean delegationsAllowed) {
        return circuits.save(proposal(requester,dgRequired,refusal,delegationsAllowed));
    }
    public BaRhCircuit proposal(BaUser requester,boolean dgRequired,String refusal,boolean delegationsAllowed) {
        BaRhCircuit circuit=build(requester,dgRequired,refusal);
        circuit.setDelegationsAutorisees(delegationsAllowed);
        if(delegationsAllowed&&delegationRepository!=null) {
            for(BaValidationDelegation d:delegationRepository.findByStatutOrderByCreatedDateDesc(EStatut.A)) {
                if(!Boolean.TRUE.equals(d.getActif())||d.getDelegant()==null||!usable(d.getDelegue())||same(d.getDelegue(),requester))continue;
                // Les etapes DGA et DG restent strictement liees a leurs titulaires de reference.
                boolean matches=circuit.getEtapes().stream().filter(s->Set.of("CHEF_SERVICE","DIRECTEUR").contains(s.getType()))
                        .anyMatch(s->s.getValidateurs().stream().anyMatch(v->v.id().equals(d.getDelegant().getId()))&&delegationRole(s.getType()).equals(d.getRoleCode()));
                if(!matches||d.getDepartement()!=null&&!Objects.equals(circuit.getDepartementId(),d.getDepartement().getId()))continue;
                if(d.getService()!=null&&(requester.getService()==null||!d.getService().getId().equals(requester.getService().getId())))continue;
                if(d.getAgence()!=null&&(requester.getAgence()==null||!d.getAgence().getId().equals(requester.getAgence().getId())))continue;
                circuit.getDelegations().add(new RhCircuitDto.Delegation(d.getId(),d.getDelegant().getId(),d.getDelegue().getId(),d.getRoleCode(),d.getDateDebut(),d.getDateFin(),d.getDepartement()==null?null:d.getDepartement().getId(),d.getService()==null?null:d.getService().getId(),d.getAgence()==null?null:d.getAgence().getId()));
            }
        }
        return circuit;
    }
    public List<BaUser> candidates(BaRhCircuit circuit,RhCircuitDto.Etape step){
        java.util.stream.Stream<BaUser> direct=step.getValidateurs().stream().map(v->users.findById(v.id()).orElse(null)).filter(RhCircuitService::usable).filter(u->directEligible(circuit,u,step));
        java.util.stream.Stream<BaUser> delegated=circuit.getDelegations().stream().map(d->users.findById(d.idDelegue()).orElse(null)).filter(RhCircuitService::usable).filter(u->delegation(circuit,u,step)!=null);
        return java.util.stream.Stream.concat(direct,delegated).distinct().toList();
    }
    public RhCircuitDto preview(BaUser requester,boolean dgRequired,String refusal) {
        try{return toDto(build(requester,dgRequired,refusal),requester);}catch(ResponseStatusException e){return blockedPreview(requester,dgRequired,e);}
    }

    public RhCircuitDto preview(BaUser requester) {
        try { return toDto(build(requester), requester); }
        catch (ResponseStatusException e) {
            return blockedPreview(requester,dgEnabled,e);
        }
    }

    private RhCircuitDto blockedPreview(BaUser requester,boolean dgRequired,ResponseStatusException e) {
            RhCircuitDto result = new RhCircuitDto();
            result.setFonctionDemandeur(requester.getFonction() == null ? null : requester.getFonction().name());
            result.setDgObligatoire(dgRequired);
            result.setBlocage(e.getReason());
            if(e instanceof ConfigurationBlocked blocked)result.setEtapeBloquee(blocked.etape);
            if (requester.getFonction() != null) {
                List<String> types = switch (requester.getFonction()) {
                    case AGENT -> List.of("CHEF_SERVICE", "DIRECTEUR", "DGA");
                    case CHEF_SERVICE -> List.of("DIRECTEUR", "DGA");
                    case DIRECTEUR -> List.of("DGA");
                    case DIRECTEUR_GENERAL_ADJOINT -> List.of("AUTRE_DGA");
                    case DIRECTEUR_GENERAL -> List.of();
                };
                for (String type : types) result.getEtapes().add(displayStep(type));
                if (dgRequired && !types.isEmpty()) result.getEtapes().add(displayStep("DG"));
            }
            return result;
    }

    private BaRhCircuit build(BaUser requester) {
        return build(requester,dgEnabled,refusalPolicy);
    }
    private BaRhCircuit build(BaUser requester,boolean dgRequired,String refusal) {
        if(!Set.of("NON_CONFIGURE","DEFINITIF","AUTRES_VALIDATEURS").contains(refusal))throw blocked("Politique de refus RH invalide.");
        if (!usable(requester) || requester.getFonction() == null)
            throw blocked("Une fonction organisationnelle de reference active doit etre renseignee.");
        if (requester.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL)
            throw blocked("Le circuit des demandes du DG n'est pas defini. La soumission est bloquee.");
        BaRhCircuit circuit = new BaRhCircuit();
        circuit.setDemandeurId(requester.getId());
        circuit.setFonctionDemandeur(requester.getFonction());
        circuit.setDepartementId(departmentId(requester));
        circuit.setDgObligatoire(dgRequired);
        circuit.setPolitiqueRefus(refusal);
        List<BaUser> candidates = users.findByStatut(EStatut.A).stream()
                .filter(RhCircuitService::usable).filter(u -> !same(u, requester)).toList();
        if (requester.getFonction() == EFonctionEmploye.AGENT) {
            add(circuit, "CHEF_SERVICE", "Chef de service", candidates.stream()
                    .filter(u -> u.getFonction() == EFonctionEmploye.CHEF_SERVICE && sameDepartment(u, requester)).toList());
        }
        if (Set.of(EFonctionEmploye.AGENT, EFonctionEmploye.CHEF_SERVICE).contains(requester.getFonction())) {
            List<BaUser> directors = candidates.stream()
                    .filter(u -> u.getFonction() == EFonctionEmploye.DIRECTEUR && sameDepartment(u, requester)).toList();
            if (directors.size() > 1) throw configurationBlocked("DIRECTEUR","Plusieurs directeurs de reference sont configures pour ce departement.");
            add(circuit, "DIRECTEUR", "Directeur", directors);
        }
        Map<EDgaPole, BaUser> dgas = configuredDgas();
        if (requester.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT) {
            List<EDgaPole> poles = dgas.entrySet().stream().filter(e -> same(e.getValue(), requester)).map(Map.Entry::getKey).toList();
            if (poles.size() != 1) throw blocked("Le pole de reference du DGA demandeur est absent ou ambigu.");
            EDgaPole other = poles.get(0) == EDgaPole.OPERATIONS ? EDgaPole.DEVELOPPEMENT : EDgaPole.OPERATIONS;
            BaUser validator = dgas.get(other);
            if (!usable(validator) || same(validator, requester))
                throw configurationBlocked("AUTRE_DGA","L'autre DGA est indisponible ou non configure. Cette etape reste obligatoire.");
            add(circuit, "AUTRE_DGA", "Autre DGA", List.of(validator));
        } else {
            add(circuit, "DGA", "Un DGA", dgas.values().stream().filter(RhCircuitService::usable).filter(u -> !same(u, requester)).distinct().toList());
        }
        if (dgRequired) {
            List<BaUser> dg = candidates.stream().filter(u -> u.getFonction() == EFonctionEmploye.DIRECTEUR_GENERAL).toList();
            if (dg.size() > 1) throw configurationBlocked("DG","Plusieurs comptes actifs exercent la fonction de DG. Corrigez le titulaire dans Utilisateurs → Gestion du DG.");
            add(circuit, "DG", "DG", dg);
        }
        return circuit;
    }

    private Map<EDgaPole, BaUser> configuredDgas() {
        Map<EDgaPole, BaUser> result = new EnumMap<>(EDgaPole.class);
        LocalDate today = LocalDate.now();
        for (BaDgaPoleValidateur assignment : assignments.findByStatutAndActifTrueOrderByCreatedDateDesc(EStatut.A)) {
            if (assignment.getDateDebut().isAfter(today) || assignment.getDateFin() != null && assignment.getDateFin().isBefore(today)) continue;
            if (result.putIfAbsent(assignment.getPole(), assignment.getDga()) != null)
                throw blocked("Plusieurs DGA sont configures pour le pole " + assignment.getPole() + ".");
        }
        result.entrySet().removeIf(e -> e.getValue().getFonction() != EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT);
        return result;
    }

    private void add(BaRhCircuit circuit, String type, String label, List<BaUser> candidates) {
        if (candidates.isEmpty()) throw configurationBlocked(type,"DG".equals(type)
                ? "Le circuit exige l'accord du DG, mais aucun DG actif et déverrouillé n'est configuré. Ouvrez Utilisateurs → Gestion du DG."
                : "Aucun validateur disponible pour l'etape " + label + ". Configurez l'organigramme.");
        RhCircuitDto.Etape step = new RhCircuitDto.Etape();
        step.setType(type); step.setLibelle(label);
        step.setValidateurs(candidates.stream().sorted(Comparator.comparing(BaUser::getId))
                .map(u -> new RhCircuitDto.Validateur(u.getId(), fullName(u))).toList());
        circuit.getEtapes().add(step);
    }

    public boolean canDecide(BaRhCircuit circuit, BaUser actor) {
        if (circuit == null || !"EN_ATTENTE".equals(circuit.getStatut()) || !usable(actor)
                || actor.getId().equals(circuit.getDemandeurId())) return false;
        RhCircuitDto.Etape step = circuit.getEtapes().get(circuit.getEtapeCourante());
        RhCircuitDto.Delegation delegated=delegation(circuit,actor,step);String source=delegated==null?actor.getId():delegated.idDelegant();
        return (step.getValidateurs().stream().anyMatch(v -> v.id().equals(actor.getId()))&&directEligible(circuit,actor,step)||delegated!=null)
                && circuit.getHistorique().stream().noneMatch(d -> d.etape() == circuit.getEtapeCourante() && (d.idSignataire().equals(actor.getId())||source.equals(d.idDelegant()==null?d.idSignataire():d.idDelegant())));
    }

    /** Le service appelant doit verrouiller la demande AVANT de charger son circuit. */
    public String decide(BaRhCircuit circuit, BaUser actor, int expectedStep, boolean approve, String comment) {
        if (circuit == null) throw blocked("Ancienne demande sans circuit fige : annulez-la puis soumettez une nouvelle demande.");
        if (!"EN_ATTENTE".equals(circuit.getStatut()) || circuit.getEtapeCourante() != expectedStep)
            throw blocked("Cette etape a deja ete traitee. Actualisez la demande.");
        if (!canDecide(circuit, actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'etes pas eligible a l'etape courante. Toute auto-validation est interdite.");
        if (comment != null && comment.length() > 1500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le commentaire ne doit pas depasser 1500 caracteres.");
        if (!approve && (comment == null || comment.isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif du refus est obligatoire.");
        RhCircuitDto.Etape step = circuit.getEtapes().get(expectedStep);
        if (step.getValidateurs().size() > 1 && "NON_CONFIGURE".equals(circuit.getPolitiqueRefus()))
            throw blocked("Le traitement d'un refus dans une etape a plusieurs validateurs doit etre precise avant de statuer.");
        String date = Instant.now().toString();
        if (approve) signatures.capture("CIRCUIT", circuit.getId(), expectedStep, actor, Instant.parse(date));
        String decision = approve ? "VALIDEE" : "REJETEE";
        RhCircuitDto.Delegation delegation=delegation(circuit,actor,step);
        circuit.getHistorique().add(new RhCircuitDto.Decision(expectedStep, step.getType(), decision, actor.getId(), fullName(actor), date, comment,
                Objects.toString(actor.getFonction(),null),delegation==null?null:delegation.idDelegant(),delegation==null?null:delegation.id()));
        if (!approve && step.getValidateurs().size() > 1 && "AUTRES_VALIDATEURS".equals(circuit.getPolitiqueRefus())) {
            // Le refus est conserve dans l'audit, sans remplacer la decision d'un autre signataire.
            circuits.save(circuit);
            return circuit.getStatut();
        }
        step.setStatut(decision); step.setIdSignataire(actor.getId()); step.setNomSignataire(fullName(actor));
        step.setDateDecision(date); step.setCommentaire(comment);
        if (!approve) circuit.setStatut("REJETEE");
        else if (expectedStep == circuit.getEtapes().size() - 1) circuit.setStatut("VALIDEE");
        else circuit.setEtapeCourante(expectedStep + 1);
        circuits.save(circuit);
        return circuit.getStatut();
    }

    public void cancel(BaRhCircuit circuit, BaUser actor) {
        if (circuit == null) return; // Annulation des demandes historiques autorisee.
        if (!actor.getId().equals(circuit.getDemandeurId()) || !"EN_ATTENTE".equals(circuit.getStatut())) throw blocked("Annulation impossible.");
        circuit.setStatut("ANNULEE");
        circuit.getHistorique().add(new RhCircuitDto.Decision(circuit.getEtapeCourante(), "ANNULATION", "ANNULEE", actor.getId(), fullName(actor), Instant.now().toString(), "Annulee par le demandeur"));
    }

    public RhCircuitDto toDto(BaRhCircuit circuit, BaUser actor) {
        RhCircuitDto dto = new RhCircuitDto();
        if (circuit == null) { dto.setBlocage("Le circuit de cette demande historique n'a pas ete enregistre. Une demande en attente doit etre soumise a nouveau."); return dto; }
        dto.setPublicationId(circuit.getPublicationId()); dto.setFonctionDemandeur(circuit.getFonctionDemandeur().name()); dto.setDgObligatoire(circuit.isDgObligatoire());
        dto.setStatut(circuit.getStatut()); dto.setEtapeCourante(circuit.getEtapeCourante()); dto.setEtapes(circuit.getEtapes());
        dto.setHistorique(circuit.getHistorique()); dto.setPeutDecider(canDecide(circuit, actor));
        if ("EN_ATTENTE".equals(circuit.getStatut())) {
            if (circuit.getEtapes().get(circuit.getEtapeCourante()).getValidateurs().size() > 1 && "NON_CONFIGURE".equals(circuit.getPolitiqueRefus())) {
                dto.setPeutDecider(false);
                dto.setBlocage("Le traitement d'un refus dans une etape a plusieurs validateurs doit etre precise avant toute decision.");
                return dto;
            }
            boolean available = circuit.getEtapes().get(circuit.getEtapeCourante()).getValidateurs().stream()
                    .map(v -> users.findById(v.id()).orElse(null)).anyMatch(u -> canDecide(circuit, u));
            available=available||circuit.getDelegations().stream().map(d->users.findById(d.idDelegue()).orElse(null)).anyMatch(u->canDecide(circuit,u));
            if (!available) dto.setBlocage("Aucun validateur fige disponible pour l'etape courante. La demande reste bloquee.");
        }
        return dto;
    }

    public boolean participant(BaRhCircuit circuit, BaUser actor) {
        return circuit != null && usable(actor) && (circuit.getEtapes().stream().anyMatch(s -> s.getValidateurs().stream().anyMatch(v -> v.id().equals(actor.getId()))||delegation(circuit,actor,s)!=null)
                ||circuit.getHistorique().stream().anyMatch(d->d.idSignataire().equals(actor.getId())));
    }
    private String delegationRole(String type){return "CHEF_SERVICE".equals(type)?"BA_CHEF_SERVICE":"BA_DIRECTEUR";}
    private RhCircuitDto.Delegation delegation(BaRhCircuit circuit,BaUser actor,RhCircuitDto.Etape step) {
        if(actor==null||!circuit.isDelegationsAutorisees()||delegationRepository==null||!Set.of("CHEF_SERVICE","DIRECTEUR").contains(step.getType()))return null;
        LocalDate today=LocalDate.now();
        return circuit.getDelegations().stream().filter(d->d.idDelegue().equals(actor.getId())&&d.roleCode().equals(delegationRole(step.getType()))
                &&!today.isBefore(d.debut())&&!today.isAfter(d.fin())&&step.getValidateurs().stream().anyMatch(v->v.id().equals(d.idDelegant())))
                .filter(d->delegationRepository.findById(d.id()).filter(live->live.getStatut()==EStatut.A&&Boolean.TRUE.equals(live.getActif())
                        &&live.getDelegant()!=null&&live.getDelegant().getId().equals(d.idDelegant())&&live.getDelegue().getId().equals(d.idDelegue())
                        &&live.getRoleCode().equals(d.roleCode())&&directEligible(circuit,live.getDelegant(),step)
                        &&Objects.equals(live.getDepartement()==null?null:live.getDepartement().getId(),d.departementId())
                        &&Objects.equals(live.getService()==null?null:live.getService().getId(),d.serviceId())
                        &&Objects.equals(live.getAgence()==null?null:live.getAgence().getId(),d.agenceId())
                        &&!today.isBefore(live.getDateDebut())&&!today.isAfter(live.getDateFin())).isPresent()).findFirst().orElse(null);
    }

    private boolean functionMatches(BaUser user,String step){return user!=null&&user.getFonction()==switch(step){case "CHEF_SERVICE"->EFonctionEmploye.CHEF_SERVICE;case "DIRECTEUR"->EFonctionEmploye.DIRECTEUR;case "DGA","AUTRE_DGA"->EFonctionEmploye.DIRECTEUR_GENERAL_ADJOINT;case "DG"->EFonctionEmploye.DIRECTEUR_GENERAL;default->null;};}
    private boolean directEligible(BaRhCircuit circuit,BaUser user,String step){return functionMatches(user,step)&&(!Set.of("CHEF_SERVICE","DIRECTEUR").contains(step)||Objects.equals(circuit.getDepartementId(),departmentId(user)));}

    private boolean directEligible(BaRhCircuit circuit,BaUser user,RhCircuitDto.Etape step) {
        if (!"LIBRE".equals(step.getType())) return directEligible(circuit,user,step.getType());
        return usable(user) && (step.getFonctionRequise()==null || step.getFonctionRequise().equals(Objects.toString(user.getFonction(),null)))
                && (step.getDepartementId()==null || step.getDepartementId().equals(departmentId(user)))
                && (step.getServiceId()==null || user.getService()!=null && step.getServiceId().equals(user.getService().getId()))
                && (step.getAgenceId()==null || user.getAgence()!=null && step.getAgenceId().equals(user.getAgence().getId()));
    }
    /** Les definitions sont publiees par une personne habilitee, jamais envoyees par le demandeur. */
    // Construction sans écriture : un blocage métier doit pouvoir être affiché
    // par la simulation sans rendre sa transaction inutilisable.
    @Transactional(readOnly=true, noRollbackFor=ResponseStatusException.class)
    public BaRhCircuit proposal(BaUser requester,com.bakouan.app.dto.ProcessusDtos.Definition definition) {
        if ("HIERARCHIQUE".equals(definition.mode())) return proposal(requester,Boolean.TRUE.equals(definition.dgObligatoire()),definition.politiqueRefus(),false);
        if (!usable(requester)||requester.getFonction()==null) throw blocked("La fonction de reference du demandeur est requise.");
        BaRhCircuit c=new BaRhCircuit();c.setDemandeurId(requester.getId());c.setFonctionDemandeur(requester.getFonction());c.setDepartementId(departmentId(requester));c.setPolitiqueRefus(definition.politiqueRefus());
        List<BaUser> available=users.findByStatut(EStatut.A).stream().filter(RhCircuitService::usable).filter(u->!same(u,requester)).toList();
        for(var rule:definition.etapes()) {
            if(rule.fonctionsDemandeur()!=null&&!rule.fonctionsDemandeur().isEmpty()&&!rule.fonctionsDemandeur().contains(requester.getFonction()))continue;
            RhCircuitDto.Etape step=new RhCircuitDto.Etape();step.setType("LIBRE");step.setLibelle(rule.libelle());step.setSelection(rule.selection());step.setFonctionRequise(rule.fonction()==null?null:rule.fonction().name());step.setDepartementId(rule.departementDemandeur()?departmentId(requester):rule.departementId());step.setServiceId(rule.serviceId());step.setAgenceId(rule.agenceId());step.setDelaiJours(rule.delaiJours());
            if(rule.departementDemandeur()&&step.getDepartementId()==null)throw blocked("Le departement de reference est requis pour "+rule.libelle());
            var validators=available.stream().filter(u->directEligible(c,u,step)).filter(u->!"UTILISATEURS".equals(rule.selection())||rule.utilisateurs().contains(u.getId())).toList();
            if(validators.isEmpty())throw blocked("Aucun validateur eligible pour "+rule.libelle()+". La demande ne peut pas sauter cette etape.");
            step.setValidateurs(validators.stream().sorted(Comparator.comparing(BaUser::getId)).map(u->new RhCircuitDto.Validateur(u.getId(),fullName(u))).toList());c.getEtapes().add(step);
        }
        if(c.getEtapes().isEmpty())throw blocked("Aucune etape applicable a la fonction du demandeur.");
        return c;
    }
    public BaRhCircuit freeze(BaUser requester,com.bakouan.app.dto.ProcessusDtos.Definition definition){return circuits.save(proposal(requester,definition));}
    public static boolean usable(BaUser user) { return user != null && user.getStatut() == EStatut.A && Boolean.TRUE.equals(user.getActivated()) && !Boolean.TRUE.equals(user.getLocked()); }
    private RhCircuitDto.Etape displayStep(String type) {
        RhCircuitDto.Etape step = new RhCircuitDto.Etape(); step.setType(type);
        step.setLibelle(switch (type) {case "CHEF_SERVICE" -> "Chef de service"; case "DIRECTEUR" -> "Directeur"; case "DGA" -> "Un DGA"; case "AUTRE_DGA" -> "Autre DGA"; default -> "DG";});
        return step;
    }
    private boolean same(BaUser a, BaUser b) { return a != null && b != null && Objects.equals(a.getId(), b.getId()); }
    public String departmentId(BaUser user) {
        String direct = user.getDepartement() == null ? null : user.getDepartement().getId();
        String service = user.getService() == null || user.getService().getDepartement() == null ? null : user.getService().getDepartement().getId();
        if (direct != null && service != null && !direct.equals(service)) throw blocked("Le departement de reference et celui du service sont incoherents. Clarifiez l'organigramme.");
        return direct == null ? service : direct;
    }
    private boolean sameDepartment(BaUser a, BaUser b) { String id = departmentId(a); return id != null && id.equals(departmentId(b)); }
    private String fullName(BaUser user) { return ((user.getNom() == null ? "" : user.getNom()) + " " + (user.getPrenom() == null ? "" : user.getPrenom())).trim(); }
    private ResponseStatusException blocked(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ConfigurationBlocked configurationBlocked(String etape,String message){return new ConfigurationBlocked(etape,message);}
    private static final class ConfigurationBlocked extends ResponseStatusException {
        private final String etape;
        private ConfigurationBlocked(String etape,String message){super(HttpStatus.CONFLICT,message);this.etape=etape;}
    }
}

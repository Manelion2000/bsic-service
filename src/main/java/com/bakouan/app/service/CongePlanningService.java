package com.bakouan.app.service;

import com.bakouan.app.dto.CongeDefinitions.*;
import com.bakouan.app.dto.CongeDtos.*;
import com.bakouan.app.dto.CongeRequests;
import com.bakouan.app.enums.*;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

@Service @Transactional @RequiredArgsConstructor
public class CongePlanningService {
    private final CongeSecurity security;
    private final BaUserRepository users;
    private final BaCongeDemandeRepository leaves;
    private final BaAutorisationAbsenceRepository absences;
    private final BaAutorisationSortieRepository outings;
    private final BaCongeSouhaitRepository wishes;
    private final BaCongeCouvertureRepository coverage;
    private final BaRoleRepository roles;
    private final BaCongeAffectationRepository assignments;
    private final CongeCalculService calculation;
    private final BaCongePassationRepository handovers;

    public List<IndisponibiliteDto> planning(LocalDate from,LocalDate to) {
        BaUser actor=security.current();CongeCalculService.dates(from,to);
        return indisponibilites(from,to,actor.getId(),true).stream().filter(i->security.planningScope(actor,users.findById(i.agentId()).orElseThrow())).toList();
    }
    public List<IndisponibiliteDto> indisponibilites(LocalDate from,LocalDate to,String owner,boolean withWishes) {
        List<IndisponibiliteDto> result=new ArrayList<>();
        for(BaCongeDemande d:leaves.findByStatutAndDebutLessThanEqualAndFinGreaterThanEqualOrderByDebutAsc(EStatut.A,to,from)) {
            if(!"APPROUVEE".equals(d.getEtat())&&!(Objects.equals(owner,d.getAgent().getId())&&"EN_ATTENTE".equals(d.getEtat())))continue;
            LocalDate end=effectiveEnd(d);
            if(!end.isBefore(d.getDebut())&&!end.isBefore(from))result.add(row(d.getId(),d.getAgent(),"CONGE",d.getEtat(),d.getDebut(),end,false));
        }
        for(BaAutorisationAbsence a:absences.findByStatutAndDateDebutLessThanEqualAndDateFinGreaterThanEqualOrderByDateDebutDesc(EStatut.A,to,from))
            if(a.getStatutValidation()==EAbsenceStatut.VALIDEE||Objects.equals(owner,a.getDemandeur().getId())&&a.getStatutValidation()==EAbsenceStatut.EN_ATTENTE)
                result.add(row(a.getId(),a.getDemandeur(),"ABSENCE_NON_IMPUTABLE",a.getStatutValidation().name(),a.getDateDebut(),a.getDateFin(),false));
        for(BaAutorisationSortie o:outings.findByStatutAndDateSortieBetweenOrderByDateSortieDesc(EStatut.A,from,to))
            if(o.getStatutValidation()==EAutorisationSortieStatut.VALIDEE||Objects.equals(owner,o.getDemandeur().getId())&&o.getStatutValidation()==EAutorisationSortieStatut.EN_ATTENTE)
                result.add(row(o.getId(),o.getDemandeur(),"SORTIE",o.getStatutValidation().name(),o.getDateSortie(),o.getDateSortie(),false));
        if(withWishes)for(BaCongeSouhait w:wishes.findByStatutAndDebutLessThanEqualAndFinGreaterThanEqualOrderByDebutAsc(EStatut.A,to,from))result.add(row(w.getId(),w.getAgent(),"SOUHAIT", "INDICATIF",w.getDebut(),w.getFin(),true));
        return result;
    }
    public List<Alerte> overlaps(BaUser agent,LocalDate from,LocalDate to,String excluded,boolean blocking) {
        List<Alerte> result=new ArrayList<>(indisponibilites(from,to,agent.getId(),false).stream().filter(i->i.agentId().equals(agent.getId())&&!Objects.equals(i.id(),excluded))
                .map(i->new Alerte(i.id(),i.debut(),"Chevauchement avec une indisponibilite "+i.categorie()+" du "+i.debut()+" au "+i.fin()+".",blocking)).toList());
        for(BaCongePassation h:handovers.findByRemplacantId(agent.getId())){BaCongeDemande d=h.getDemande();if(!Objects.equals(excluded,d.getId())&&h.getPriseEnChargeLe()!=null&&Set.of("EN_ATTENTE","APPROUVEE").contains(d.getEtat())&&d.getPolitique()!=null&&Boolean.TRUE.equals(d.getPolitique().getRegles().getRemplacementRequis())&&CongeConfigurationService.overlap(from,to,d.getDebut(),effectiveEnd(d)))result.add(new Alerte(d.getId(),d.getDebut(),"Un remplacement obligatoire a ete accepte sur cette periode. Faites controler une autre passation avant de demander une indisponibilite.",true));}
        return result;
    }
    public void guardOtherSubmission(BaUser agent,LocalDate from,LocalDate to) {
        overlaps(agent,from,to,null,false).stream().filter(Alerte::bloquante).findFirst().ifPresent(a->{throw blocked(a.message());});
        for(BaCongeDemande d:leaves.findByAgentIdAndStatutOrderByCreatedDateDesc(agent.getId(),EStatut.A))
            if(Set.of("EN_ATTENTE","APPROUVEE").contains(d.getEtat())&&d.getPolitique()!=null&&Boolean.TRUE.equals(d.getPolitique().getRegles().getChevauchementBloquant())
                    &&CongeConfigurationService.overlap(from,to,d.getDebut(),effectiveEnd(d)))
                throw blocked("Cette periode chevauche un conge dont la politique interdit les chevauchements.");
    }
    /** Un verrou commun couvre tous les perimetres, dont les competences transversales. */
    public void lockCapacity(){roles.findLockedByCode("BA_CONNECT").orElseThrow(()->blocked("Configuration des roles incomplete."));}
    public void assertOtherFinal(BaUser agent,LocalDate from,LocalDate to,String id){lockCapacity();guardOtherSubmission(agent,from,to);List<Alerte> alerts=capacity(agent,from,to,id);alerts.stream().filter(Alerte::bloquante).findFirst().ifPresent(a->{throw blocked(a.message());});}
    public List<Alerte> capacity(BaUser agent,LocalDate from,LocalDate to,String excluded) {
        List<Alerte> result=new ArrayList<>();List<BaUser> all=users.findByStatut(EStatut.A).stream().filter(CongeSecurity::available).toList();
        List<IndisponibiliteDto> occupied=indisponibilites(from,to,null,false).stream().filter(i->!Objects.equals(excluded,i.id())).toList();
        for(BaCongeCouverture version:coverage.findByStatutOrderByCreatedDateDesc(EStatut.A)) {
            if(!version.isConfirme()||!inScope(agent,version.getRegles()))continue;
            Couverture rule=version.getRegles();List<BaUser> staff=all.stream().filter(u->inScope(u,rule)).toList();
            for(LocalDate day=from;!day.isAfter(to);day=day.plusDays(1)) {
                if(day.isBefore(version.getDebut())||version.getFin()!=null&&day.isAfter(version.getFin()))continue;
                LocalDate date=day;
                if(!working(agent,date,result,version))continue;
                List<BaUser> scheduled=staff.stream().filter(u->working(u,date,result,version)).toList();
                Set<String> unavailable=new HashSet<>();unavailable.add(agent.getId());
                occupied.stream().filter(i->!date.isBefore(i.debut())&&!date.isAfter(i.fin())).map(IndisponibiliteDto::agentId).forEach(unavailable::add);
                long present=scheduled.stream().filter(u->!unavailable.contains(u.getId())).count();
                if(rule.getMinimumPresent()!=null&&present<rule.getMinimumPresent())result.add(alert(version,date,"Effectif present "+present+", minimum "+rule.getMinimumPresent()+"."));
                long absent=scheduled.size()-present;if(rule.getMaximumIndisponibles()!=null&&absent>rule.getMaximumIndisponibles())result.add(alert(version,date,"Indisponibilites simultanees "+absent+", maximum "+rule.getMaximumIndisponibles()+"."));
                for(var criterion:rule.getMinimumParFonction().entrySet())if(scheduled.stream().filter(u->u.getFonction()==criterion.getKey()&&!unavailable.contains(u.getId())).count()<criterion.getValue())result.add(alert(version,date,"Couverture insuffisante de la fonction "+criterion.getKey()+"."));
                for(GroupeCouverture group:rule.getGroupes())if(group.getAgents().stream().filter(id->!unavailable.contains(id)&&all.stream().filter(u->u.getId().equals(id)).anyMatch(u->working(u,date,result,version))).count()<group.getMinimumPresent())result.add(alert(version,date,"Couverture insuffisante du groupe "+group.getLibelle()+"."));
                for(List<String> incompatible:rule.getIncompatibilites())if(incompatible.stream().filter(unavailable::contains).count()>1)result.add(alert(version,date,"Des agents declares incompatibles sont simultanement indisponibles."));
            }
        }
        return result;
    }
    private boolean working(BaUser agent,LocalDate day,List<Alerte> alerts,BaCongeCouverture rule){
        List<BaCongeCalendrier> calendars=assignments.findByAgentIdAndStatutOrderByDebutDesc(agent.getId(),EStatut.A).stream()
                .filter(a->!day.isBefore(a.getDebut())&&(a.getFin()==null||!day.isAfter(a.getFin())))
                .map(BaCongeAffectation::getCalendrier).distinct().toList();
        if(calendars.size()!=1){alerts.add(alert(rule,day,"Calendrier absent ou ambigu pour "+CongeConfigurationService.name(agent)+" ; couverture non calculable."));return false;}
        return !calculation.plages(calendars.get(0).getRegles(),day).isEmpty();
    }
    private LocalDate effectiveEnd(BaCongeDemande d){
        if(d.getRepriseConstatee()==null||d.getRepriseConstatee().isAfter(d.getFin()))return d.getFin();
        boolean retained=d.getImputations().stream().filter(i->i.getJour().equals(d.getRepriseConstatee())).map(Imputation::getQuantite).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add)
                .compareTo(d.getRestitutions().stream().filter(i->i.getJour().equals(d.getRepriseConstatee())).map(Imputation::getQuantite).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add))>0;
        return retained?d.getRepriseConstatee():d.getRepriseConstatee().minusDays(1);
    }
    private Alerte alert(BaCongeCouverture v,LocalDate date,String text){return new Alerte(v.getId(),date,v.getNom()+" : "+text,Boolean.TRUE.equals(v.getRegles().getBloquante()));}
    private boolean inScope(BaUser user,Couverture rule){return switch(rule.getPerimetre()){case "DEPARTEMENT"->rule.getPerimetreId().equals(departmentId(user));case "SERVICE"->user.getService()!=null&&rule.getPerimetreId().equals(user.getService().getId());case "AGENCE"->user.getAgence()!=null&&rule.getPerimetreId().equals(user.getAgence().getId());default->false;};}
    public IndisponibiliteDto wish(CongeRequests.Souhait request) {
        BaUser actor=security.require("CONGES_DEMANDER");CongeCalculService.dates(request.getDebut(),request.getFin());BaCongeSouhait w=new BaCongeSouhait();w.setAgent(actor);w.setDebut(request.getDebut());w.setFin(request.getFin());wishes.save(w);return row(w.getId(),actor,"SOUHAIT","INDICATIF",w.getDebut(),w.getFin(),true);
    }
    public void removeWish(String id){BaUser actor=security.current();BaCongeSouhait w=wishes.findById(id).orElseThrow(()->missing("Souhait introuvable."));if(!w.getAgent().getId().equals(actor.getId()))throw forbidden("Seul l'auteur peut retirer ce souhait.");w.setStatut(EStatut.D);wishes.save(w);}
    private IndisponibiliteDto row(String id,BaUser user,String category,String state,LocalDate from,LocalDate to,boolean wish){return new IndisponibiliteDto(id,user.getId(),CongeConfigurationService.name(user),departmentId(user),user.getService()==null?null:user.getService().getId(),user.getAgence()==null?null:user.getAgence().getId(),category,state,from,to,wish);}
}

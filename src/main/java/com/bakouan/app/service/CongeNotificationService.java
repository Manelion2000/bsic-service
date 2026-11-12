package com.bakouan.app.service;

import com.bakouan.app.dto.CongeDtos.NotificationDto;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.*;
import com.bakouan.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

/** Outbox transactionnelle : aucun email n'est envoye dans une transaction de decision. */
@Service @Transactional @RequiredArgsConstructor
public class CongeNotificationService {
    private final BaCongeNotificationRepository notifications;
    private final BaUserRepository users;
    private final CongeSecurity security;
    private final JavaMailSender sender;
    private final RhCircuitService circuits;
    @Value("${app.conges.notifications-mail-enabled:false}") private boolean mailEnabled;
    @Value("${spring.mail.username:}") private String from;

    public void event(BaCongeDemande request,String event) {
        Set<String> ids=new LinkedHashSet<>();ids.add(request.getAgent().getId());
        if(request.getCircuit()!=null&&"EN_ATTENTE".equals(request.getEtat())) {
            circuits.candidates(request.getCircuit(),request.getCircuit().getEtapes().get(request.getCircuit().getEtapeCourante())).stream().filter(u->security.has(u,"CONGES_VALIDER")&&circuits.canDecide(request.getCircuit(),u)).forEach(u->ids.add(u.getId()));
        }
        if(event.startsWith("RETOUR_"))users.findByStatut(EStatut.A).stream().filter(CongeSecurity::available).filter(u->security.has(u,"CONGES_ANNULER")).forEach(u->ids.add(u.getId()));
        for(String id:ids) {
            String key="notification:"+CongeLedgerService.hash(request.getId()+"|"+event+"|"+id);
            if(notifications.existsByCle(key))continue;
            BaCongeNotification n=new BaCongeNotification();n.setDemande(request);n.setDestinataire(users.findById(id).orElseThrow());n.setEvenement(event);n.setCle(key);notifications.save(n);
        }
    }
    public List<NotificationDto> mine(){return notifications.findByDestinataireIdAndStatutOrderByCreatedDateDesc(security.current().getId(),EStatut.A).stream().map(n->new NotificationDto(n.getId(),n.getDemande().getId(),n.getEvenement(),n.getCreatedDate(),n.getLueLe())).toList();}
    public void markRead(String id){BaUser actor=security.current();BaCongeNotification n=notifications.findById(id).orElseThrow(()->missing("Notification introuvable."));if(!n.getDestinataire().getId().equals(actor.getId()))throw forbidden("Notification hors de votre perimetre.");if(n.getLueLe()==null){n.setLueLe(Instant.now());notifications.save(n);}}
    public void dispatch() {
        if(!mailEnabled)return;
        for(BaCongeNotification n:notifications.findMailBatch()) {
            n.setEssais(n.getEssais()+1);
            try {
                if(n.getDestinataire().getEmail()==null||from.isBlank())throw new IllegalStateException("Email non configure");
                SimpleMailMessage mail=new SimpleMailMessage();mail.setFrom(from);mail.setTo(n.getDestinataire().getEmail());mail.setSubject("BSIC - Gestion des conges");
                mail.setText("Un evenement vous concerne : "+n.getEvenement()+". Consultez le module Conges dans le portail BSIC. Reference "+n.getDemande().getId()+". Revision "+n.getDemande().getRevision()+".");
                sender.send(mail);n.setEnvoyee(true);
            } catch(Exception e) {n.setProchainEssai(Instant.now().plusSeconds(Math.min(3600L,60L*(1L<<Math.min(n.getEssais(),6)))));}
            n.setLastModifiedBy("systeme-conges");notifications.save(n);
        }
    }
}

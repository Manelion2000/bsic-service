package com.bakouan.app.service;

import com.bakouan.app.dto.CongeDefinitions.*;
import com.bakouan.app.dto.CongeRequests;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.*;

/** Calcul pur, utilise aussi pour simuler, proratiser et restituer les jours non pris. */
@Service
public class CongeCalculService {
    public Calcul calculate(CongeRequests.Demande request, String policyId, Politique policy, String calendarId, Calendrier calendar) {
        dates(request.getDebut(), request.getFin()); validateCalendar(calendar);
        if (request.getPortionDebut() == null || request.getPortionFin() == null) throw bad("Les portions de journee doivent etre renseignees.");
        if (policy.getUnite() != Unite.HEURE && (request.getHeureDebut() != null || request.getHeureFin() != null)) throw bad("Les heures ne sont utilisables qu'avec un decompte horaire.");
        boolean partial = request.getPortionDebut() != Portion.JOURNEE || request.getPortionFin() != Portion.JOURNEE;
        if (partial && !Boolean.TRUE.equals(policy.getDemiJourneesAutorisees())) throw bad("Les demi-journees ne sont pas autorisees par cette politique.");
        if (request.getDebut().equals(request.getFin()) && request.getPortionDebut() == Portion.APRES_MIDI && request.getPortionFin() == Portion.MATIN)
            throw bad("Les portions de debut et de fin sont inversees.");
        if (policy.getUnite() == Unite.HEURE && (request.getHeureDebut() == null || request.getHeureFin() == null)) throw bad("Les heures de debut et de fin sont obligatoires.");
        if (policy.getUnite() == Unite.HEURE && (request.getHeureDebut().getSecond()!=0 || request.getHeureDebut().getNano()!=0 || request.getHeureFin().getSecond()!=0 || request.getHeureFin().getNano()!=0)) throw bad("Renseignez des heures a la minute.");
        if (request.getDebut().equals(request.getFin()) && request.getHeureDebut() != null && !request.getHeureFin().isAfter(request.getHeureDebut())) throw bad("La plage horaire est invalide.");
        Calcul result = new Calcul(); result.setUnite(policy.getUnite()); result.setPolitiqueId(policyId); result.setCalendrierId(calendarId); result.setFuseau(calendar.getFuseau());
        for (LocalDate day = request.getDebut(); !day.isAfter(request.getFin()); day = day.plusDays(1)) {
            JourCalcule row = new JourCalcule(); row.setDate(day); row.setCalendrierId(calendarId);
            List<Plage> ranges = plages(calendar,day);
            BigDecimal quantity = BigDecimal.ZERO;
            for (Plage range : ranges) {
                if (!selected(request,day,range.getPortion())) continue;
                if (policy.getUnite() == Unite.HEURE) {
                    LocalTime from = range.getDebut(), to = range.getFin();
                    if (day.equals(request.getDebut()) && request.getHeureDebut().isAfter(from)) from = request.getHeureDebut();
                    if (day.equals(request.getFin()) && request.getHeureFin().isBefore(to)) to = request.getHeureFin();
                    if (to.isAfter(from)) quantity = quantity.add(BigDecimal.valueOf(ChronoUnit.MINUTES.between(from,to)).divide(BigDecimal.valueOf(60),12,RoundingMode.HALF_UP));
                } else quantity = quantity.add(range.getPoidsJour().multiply(policy.getUnite() == Unite.DEMI_JOUR ? BigDecimal.valueOf(2) : BigDecimal.ONE));
            }
            row.setQuantite(round(quantity,policy));
            if (row.getQuantite().signum() == 0) row.setExclusion(exceptionLabel(calendar,day,ranges.isEmpty() ? "Jour non travaille" : "Plage demandee hors horaire de travail"));
            result.getJours().add(row); result.setQuantite(result.getQuantite().add(row.getQuantite()));
        }
        return result;
    }

    private boolean selected(CongeRequests.Demande request, LocalDate day, Portion portion) {
        if (portion == Portion.JOURNEE) throw bad("Chaque plage du calendrier doit identifier MATIN ou APRES_MIDI.");
        if (day.equals(request.getDebut()) && request.getPortionDebut() == Portion.APRES_MIDI && portion == Portion.MATIN) return false;
        if (day.equals(request.getFin()) && request.getPortionFin() == Portion.MATIN && portion == Portion.APRES_MIDI) return false;
        return true;
    }
    public List<Plage> plages(Calendrier calendar, LocalDate date) {
        return calendar.getExceptions().stream().filter(e -> date.equals(e.getDate())).findFirst().map(ExceptionCalendrier::getPlages)
                .orElseGet(() -> calendar.getSemaine().stream().filter(d -> d.getJour() == date.getDayOfWeek()).findFirst().orElseThrow().getPlages());
    }
    private String exceptionLabel(Calendrier calendar, LocalDate date, String fallback) {
        return calendar.getExceptions().stream().filter(e -> date.equals(e.getDate())).findFirst().map(ExceptionCalendrier::getLibelle).orElse(fallback);
    }
    public BigDecimal round(BigDecimal quantity, Politique policy) {
        if (quantity.signum() == 0) return BigDecimal.ZERO.setScale(4);
        try { return quantity.divide(policy.getPasDecompte(),0,policy.getArrondi()).multiply(policy.getPasDecompte()).setScale(4,RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw bad("La quantite ne respecte pas le pas et la regle d'arrondi de la politique."); }
    }
    public BigDecimal prorata(BigDecimal quantity, LocalDate periodStart, LocalDate periodEnd, LocalDate activeStart, LocalDate activeEnd, Politique policy, Calendrier calendar) {
        if (policy.getProrata() == Prorata.AUCUN) return round(quantity,policy);
        LocalDate from = activeStart.isAfter(periodStart) ? activeStart : periodStart;
        LocalDate to = activeEnd.isBefore(periodEnd) ? activeEnd : periodEnd;
        if (to.isBefore(from)) return BigDecimal.ZERO;
        long denominator, numerator;
        if (policy.getProrata() == Prorata.JOURS_CALENDAIRES) {
            denominator = ChronoUnit.DAYS.between(periodStart,periodEnd)+1; numerator = ChronoUnit.DAYS.between(from,to)+1;
        } else {
            denominator = workedDays(periodStart,periodEnd,calendar); numerator = workedDays(from,to,calendar);
        }
        if (denominator == 0) throw bad("La periode de reference ne contient aucun jour travaille.");
        return round(quantity.multiply(BigDecimal.valueOf(numerator)).divide(BigDecimal.valueOf(denominator),12,RoundingMode.HALF_UP),policy);
    }
    private long workedDays(LocalDate start, LocalDate end, Calendrier calendar) {
        long count=0; for(LocalDate day=start;!day.isAfter(end);day=day.plusDays(1)) if(!plages(calendar,day).isEmpty()) count++; return count;
    }
    public void validateCalendar(Calendrier calendar) {
        if (calendar == null || calendar.getFuseau() == null) throw bad("Calendrier incomplet.");
        try { ZoneId.of(calendar.getFuseau()); } catch (DateTimeException e) { throw bad("Fuseau du calendrier invalide."); }
        if (calendar.getSemaine() == null || calendar.getSemaine().size() != 7 || calendar.getSemaine().stream().map(JourTravail::getJour).distinct().count() != 7)
            throw bad("Renseignez explicitement les sept jours de la semaine, y compris les jours non travailles.");
        calendar.getSemaine().forEach(d -> validateRanges(d.getPlages()));
        if (calendar.getExceptions().stream().map(ExceptionCalendrier::getDate).distinct().count() != calendar.getExceptions().size()) throw bad("Une seule exception par date est permise.");
        calendar.getExceptions().forEach(e -> validateRanges(e.getPlages()));
    }
    private void validateRanges(List<Plage> ranges) {
        if (ranges == null) throw bad("Les plages du calendrier doivent etre renseignees.");
        List<Plage> sorted = ranges.stream().sorted(Comparator.comparing(Plage::getDebut)).toList();
        LocalTime previous = null;
        for (Plage range : sorted) {
            if (range.getDebut() == null || range.getFin() == null || !range.getFin().isAfter(range.getDebut())
                    || range.getDebut().getSecond() != 0 || range.getFin().getSecond() != 0
                    || range.getPortion() == null || range.getPortion() == Portion.JOURNEE || range.getPoidsJour() == null || range.getPoidsJour().signum() <= 0)
                throw bad("Plage de travail invalide : heures a la minute et portion MATIN/APRES_MIDI requises.");
            if (previous != null && range.getDebut().isBefore(previous)) throw bad("Les plages de travail se chevauchent.");
            previous = range.getFin();
        }
    }
    public static void dates(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) throw bad("Les dates de debut et de fin sont invalides.");
        if (ChronoUnit.DAYS.between(start,end) > 3660) throw bad("La periode est trop longue pour une seule operation.");
    }
}

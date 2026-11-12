package com.bakouan.app.config;

import com.bakouan.app.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

/** Activation volontaire. Les services assurent l'idempotence et l'outbox apres commit. */
@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name="app.conges.traitements-automatiques-enabled",havingValue="true")
public class CongeJobs {
    private final CongeLedgerService ledger;
    private final CongeNotificationService notifications;
    @Scheduled(cron="${app.conges.acquisition-cron:0 5 0 * * *}",zone="${app.conges.fuseau-traitements:Africa/Ouagadougou}")
    public void rights(){ledger.processDueInternal("systeme-conges");}
    @Scheduled(fixedDelayString="${app.conges.notifications-delai-ms:60000}")
    public void mail(){notifications.dispatch();}
}

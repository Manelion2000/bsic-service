package com.bakouan.app.config;

import com.bakouan.app.security.InitialAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Component
@DependsOnDatabaseInitialization
@RequiredArgsConstructor
@Slf4j
public class InitialAdminRunner implements ApplicationRunner {
    private final InitialAdminService service;

    @Override
    public void run(ApplicationArguments args) {
        // Auteur technique explicite de l'audit, sans rôle, identifiant de connexion ou secret partagé.
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("bootstrap-admin", null, List.of()));
        InitialAdminService.Result result;
        try {
            SecurityContextHolder.setContext(context);
            result = service.initialize();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
        if (result == InitialAdminService.Result.CREATED) {
            log.info("Administrateur initial créé. Utiliser les identifiants définis au déploiement.");
        } else if (result == InitialAdminService.Result.ALREADY_PRESENT) {
            log.info("Un administrateur existe déjà ; initialisation ignorée et comptes conservés.");
        }
    }
}

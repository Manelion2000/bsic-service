package com.bakouan.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Paramètres de déploiement ; aucun mot de passe par défaut dans le code. */
@Configuration
@ConfigurationProperties(prefix = "app.bootstrap.admin")
@Getter @Setter
public class InitialAdminProperties {
    private boolean enabled = false;
    private String email = "admin@bsic.bf";
    private String password;
    private String telephone;
    private String nom = "Portail";
    private String prenom = "Administrateur";
}

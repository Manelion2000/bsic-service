package com.bakouan.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration @ConfigurationProperties(prefix="app.security.password-access") @Getter @Setter
public class PasswordAccessProperties {
    private int temporaryValidityHours=24;
    private int setupSessionMinutes=15;
}

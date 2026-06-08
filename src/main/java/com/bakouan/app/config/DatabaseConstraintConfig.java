package com.bakouan.app.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConstraintConfig {

    @Bean
    public ApplicationRunner fixEtapeDefinitionTypeConstraint(final JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                    ALTER TABLE ba_etape_definition
                    DROP CONSTRAINT IF EXISTS ba_etape_definition_type_check
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ba_etape_definition
                    ADD CONSTRAINT ba_etape_definition_type_check
                    CHECK (type IN ('DEPARTEMENT', 'SERVICE'))
                    """);
        };
    }
}

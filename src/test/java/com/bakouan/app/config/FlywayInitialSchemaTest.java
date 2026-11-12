package com.bakouan.app.config;

import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration PostgreSQL opt-in : chaque test utilise son propre schema temporaire. */
@EnabledIfEnvironmentVariable(named = "BSIC_MIGRATION_TEST_URL", matches = ".+")
class FlywayInitialSchemaTest {
    private String schema;
    private Connection connection;

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        connection = DriverManager.getConnection(databaseUrl(), databaseUser(), databasePassword());
        schema = "bsic_bootstrap_test_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        execute("SET search_path TO " + schema);
    }

    @AfterEach
    void dropIsolatedSchema() throws Exception {
        if (connection != null) {
            try {
                if (schema != null && schema.matches("bsic_bootstrap_test_[0-9a-f]{32}")) {
                    execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
                }
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void emptySchemaMigratesAndMatchesAllJpaEntities() throws Exception {
        Flyway flyway = flyway();

        assertEquals(16, flyway.migrate().migrationsExecuted);
        assertEquals("1.35", flyway.info().current().getVersion().getVersion());
        assertEquals(16, count("SELECT COUNT(*) FROM flyway_schema_history WHERE success"));
        assertEquals(30, count("SELECT COUNT(*) FROM ba_role"));
        assertEquals(0, count("SELECT COUNT(*) FROM ba_utilisateur"));
        validateHibernateMappings();

        // Un second demarrage ne recree pas les tables et ne duplique pas les roles.
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
        assertEquals(30, count("SELECT COUNT(*) FROM ba_role"));
    }

    @Test
    void existingSchemaIsBaselinedWithoutReplayingInitialScript() throws Exception {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/B1.20__initial_schema.sql"));
        execute("INSERT INTO ba_profil (id, libelle, created_by, created_date) "
                + "VALUES ('existing-profile', 'Profil existant', 'test', CURRENT_TIMESTAMP)");

        Flyway flyway = flyway();
        assertEquals(15, flyway.migrate().migrationsExecuted);
        assertEquals("1.35", flyway.info().current().getVersion().getVersion());
        assertEquals(1, count("SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'BASELINE'"));
        assertEquals(0, count("SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE script = 'B1.20__initial_schema.sql'"));
        assertEquals(1, count("SELECT COUNT(*) FROM ba_profil WHERE id = 'existing-profile'"));
        validateHibernateMappings();
        flyway.validate();
    }

    @Test
    void simplifiedQueriesPreserveExistingRecordsAndAllowRegistrationWithoutCodeOrPlatform() throws Exception {
        assertEquals(11, flyway("1.30").migrate().migrationsExecuted);
        execute("INSERT INTO ba_plateforme(id,code,nom,created_by,created_date) VALUES ('old-platform','ORACLE','Oracle','test',now())");
        execute("INSERT INTO ba_requete_oracle(id,code,titre,objectif,plateforme_id,sql_texte,edition,verifiee,created_by,created_date) "
                + "VALUES ('old-query','OLD-CODE','Requête existante','Objectif','old-platform','SELECT 1 FROM dual',1,true,'test',now())");
        execute("INSERT INTO ba_requete_revision(id,requete_id,edition,date,auteur_id,auteur,motif,contenu) VALUES "
                + "('old-revision','old-query',1,now(),'old-author','Auteur connu','Reprise',"
                + "'{\"code\":\"OLD-CODE\",\"plateformeId\":\"old-platform\",\"titre\":\"Requête existante\",\"sqlTexte\":\"SELECT 1 FROM dual\"}')");

        Flyway flyway = flyway();
        assertEquals(5, flyway.migrate().migrationsExecuted);
        assertEquals(1, count("SELECT COUNT(*) FROM ba_requete_oracle WHERE id='old-query' AND code='OLD-CODE' "
                + "AND plateforme_id='old-platform' AND departement_demandeur_id IS NULL AND departement_nom IS NULL AND verifiee AND auteur_id='old-author' AND auteur='Auteur connu' AND date_validation IS NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM ba_requete_revision WHERE id='old-revision' "
                + "AND contenu->>'code'='OLD-CODE' AND contenu->>'plateformeId'='old-platform' AND NOT contenu ? 'dateValidation'"));
        execute("INSERT INTO ba_requete_oracle(id,titre,objectif,sql_texte,edition,verifiee,date_validation,auteur_id,auteur,created_by,created_date) "
                + "VALUES ('new-query','Requête existante','Objectif','SELECT 2 FROM dual',1,true,CURRENT_DATE,'new-author','Nouvel auteur','test',now())");
        assertEquals(1, count("SELECT COUNT(*) FROM ba_requete_oracle WHERE id='new-query' AND code IS NULL AND plateforme_id IS NULL"));
        assertEquals(0, count("SELECT COUNT(*) FROM ba_extraction_execution"));
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
        validateHibernateMappings();
    }

    @Test
    void accessMigrationPreservesPasswordsAndDisablesOnlyLegacyEmailKeys() throws Exception {
        assertEquals(13,flyway("1.32").migrate().migrationsExecuted);
        execute("INSERT INTO ba_utilisateur(id,username,password_hash,telephone,activated,account_locked,reset_key,reset_date,created_by,created_date) VALUES ('existing','existing@example.test','existing-password-hash','existing-phone',true,false,'legacy',now(),'test',now())");
        var current=flyway();assertEquals(3,current.migrate().migrationsExecuted);
        assertEquals(1,count("SELECT count(*) FROM ba_utilisateur WHERE id='existing' AND password_hash='existing-password-hash' AND activated AND NOT account_locked AND credential_version=0 AND NOT password_change_required AND NOT temporary_password_used AND temporary_password_expires_at IS NULL AND reset_key IS NULL AND reset_date IS NULL"));
        assertEquals(0,current.migrate().migrationsExecuted);current.validate();validateHibernateMappings();
    }

    @Test
    void serviceMigrationPreservesHistoricalRequestsAndSqlWithoutInferringCurrentAssignment() throws Exception {
        assertEquals(14,flyway("1.33").migrate().migrationsExecuted);
        execute("INSERT INTO ba_departement(id,nom,created_by,created_date) VALUES ('dept','Département actuel','test',now())");
        execute("INSERT INTO ba_service(id,nom,departement_id,created_by,created_date) VALUES ('service','Service actuel','dept','test',now())");
        execute("INSERT INTO ba_utilisateur(id,username,password_hash,telephone,activated,account_locked,departement_id,service_id,created_by,created_date) VALUES ('agent','agent@example.test','hash','phone',true,false,'dept','service','test',now())");
        execute("INSERT INTO ba_requete_oracle(id,titre,objectif,sql_texte,edition,verifiee,departement_demandeur_id,departement_nom,created_by,created_date) VALUES ('query','Requête','Objectif','SELECT 1 FROM dual',1,true,'dept','Département historique','test',now())");
        execute("INSERT INTO ba_requete_revision(id,requete_id,edition,date,auteur_id,auteur,motif,contenu) VALUES ('revision','query',1,now(),'agent','Agent historique','Reprise','{\"titre\":\"Requête\",\"sqlTexte\":\"SELECT 1 FROM dual\",\"departementId\":\"dept\",\"departementNom\":\"Département historique\"}')");
        execute("INSERT INTO ba_extraction_demande(id,reference,titre,objectif,date_demande,demandeur_id,demandeur_nom,departement_id,departement_nom,auteur_id,auteur,etat,charge_de_id,revision_id,created_by,created_date) VALUES ('request','EXT-HISTORIQUE','Demande','Objectif',CURRENT_DATE,'agent','Agent historique','dept','Département historique','agent','Agent historique','EN_COURS','agent','revision','test',now())");
        var current=flyway();assertEquals(2,current.migrate().migrationsExecuted);
        assertEquals(1,count("SELECT count(*) FROM ba_extraction_demande WHERE id='request' AND service_id IS NULL AND service_nom IS NULL AND demandeur_id='agent' AND departement_nom='Département historique' AND etat='EN_COURS' AND revision_id='revision' AND version=1"));
        assertEquals(1,count("SELECT count(*) FROM ba_requete_oracle WHERE id='query' AND service_demandeur_id IS NULL AND service_nom IS NULL AND sql_texte='SELECT 1 FROM dual' AND departement_nom='Département historique' AND edition=1 AND verifiee"));
        assertEquals(1,count("SELECT count(*) FROM ba_requete_revision WHERE id='revision' AND contenu->>'departementNom'='Département historique' AND NOT contenu ? 'serviceId' AND NOT contenu ? 'serviceNom'"));
        assertEquals(0,current.migrate().migrationsExecuted);current.validate();validateHibernateMappings();
    }

    private Flyway flyway() {return flyway(null);}
    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(databaseUrl(), databaseUser(), databasePassword())
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(false)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.20")
                .validateOnMigrate(true)
                .outOfOrder(false);
        if(target!=null)configuration.target(target);
        return configuration.load();
    }

    private void validateHibernateMappings() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", databaseUrl());
        settings.put("hibernate.connection.username", databaseUser());
        settings.put("hibernate.connection.password", databasePassword());
        settings.put("hibernate.default_schema", schema);
        settings.put("hibernate.hbm2ddl.auto", "validate");
        settings.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        settings.put("hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName());

        try (var registry = new StandardServiceRegistryBuilder().applySettings(settings).build()) {
            MetadataSources sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            var entities = scanner.findCandidateComponents("com.bakouan.app");
            assertTrue(!entities.isEmpty(), "Les entites JPA doivent etre detectees");
            for (var definition : entities) {
                sources.addAnnotatedClass(Class.forName(definition.getBeanClassName()));
            }
            SchemaManagementToolCoordinator.process(sources.buildMetadata(), registry, settings, action -> {});
        }
    }

    private void execute(String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String sql) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private String databaseUrl() {
        return System.getenv("BSIC_MIGRATION_TEST_URL");
    }

    private String databaseUser() {
        return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_USERNAME", "postgres");
    }

    private String databasePassword() {
        return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_PASSWORD", "");
    }
}

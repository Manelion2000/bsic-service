package com.bakouan.app.service;

import com.bakouan.app.BsicServiceApplication;
import com.bakouan.app.config.InitialAdminProperties;
import com.bakouan.app.security.InitialAdminService;
import com.bakouan.app.repositories.BaUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named="BSIC_MIGRATION_TEST_URL",matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitialAdminPostgresApiTest {
    private static final String EMAIL="bootstrap@example.test", PASSWORD="Startup-test-Password", PHONE="+22670001122";
    private String schema,base;
    private Connection sql;
    private ConfigurableApplicationContext app;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    private final HttpClient http=HttpClient.newHttpClient();

    @BeforeAll void start() throws Exception {
        schema="bsic_initial_admin_test_"+UUID.randomUUID().toString().replace("-","");
        sql=DriverManager.getConnection(url(),user(),password());
        execute("CREATE SCHEMA "+schema);execute("SET search_path TO "+schema);
        app=startApplication(Map.of());
        base="http://127.0.0.1:"+((ServletWebServerApplicationContext)app).getWebServer().getPort();
    }
    @BeforeEach void restoreInitialAccount() throws Exception {
        // Remise à l'état initial dans le schéma de recette uniquement, jamais dans la base applicative.
        try(var query=sql.prepareStatement("UPDATE ba_utilisateur SET password_hash=?,activated=true,account_locked=false,statut='A',password_change_required=false,temporary_password_used=false,temporary_password_expires_at=NULL WHERE username=?")) {
            query.setString(1,app.getBean(PasswordEncoder.class).encode(PASSWORD));query.setString(2,EMAIL);query.executeUpdate();
        }
        var config=app.getBean(InitialAdminProperties.class);
        config.setEnabled(true);config.setEmail(EMAIL);config.setPassword(PASSWORD);config.setTelephone(PHONE);
    }
    @AfterAll void close() throws Exception {
        if(app!=null)app.close();
        if(sql!=null)try{
            if(schema!=null&&schema.matches("bsic_initial_admin_test_[0-9a-f]{32}"))execute("DROP SCHEMA "+schema+" CASCADE");
        }finally{sql.close();}
    }

    @Test void startupAdminAuthenticatesAndCreatesStructuresAndOtherAccounts() throws Exception {
        var token=login(PASSWORD,200);
        var details=call("GET","users/details",null,token,200);
        assertThat(details.path("roles").findValuesAsText("code")).contains("BA_ADMIN");
        assertThat(details.path("activated").asBoolean()).isTrue();assertThat(details.path("locked").asBoolean()).isFalse();
        assertThat(details.path("fonction").isNull()).isTrue();assertThat(details.has("password")).isFalse();
        var department=call("POST","annuaires/departements",Map.of("code","BOOT-DEP","nom","Département de recette"),token,201);
        var team=call("POST","annuaires/services",Map.of("code","BOOT-SRV","nom","Service de recette","idDepartement",department.path("id").asText()),token,201);
        call("POST","users",Map.of("email","new-agent@example.test","nom","Agent","prenom","Test","telephone","+22670003344",
                "sexe","MASCULIN","fonction","AGENT","idDepartement",department.path("id").asText(),"idService",team.path("id").asText()),token,201);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE username='new-agent@example.test' AND activated AND NOT account_locked")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_utilisateur u JOIN ba_user_roles ur ON ur.user_id=u.id JOIN ba_role r ON r.id=ur.role_id WHERE r.code='BA_ADMIN'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_log WHERE sujet='Création de l''administrateur initial' AND details LIKE '%origine=DEPLOIEMENT_INITIAL%'")).isGreaterThanOrEqualTo(1);
    }
    @Test void secondApplicationDoesNotCreateAnotherAdminOrChangeExistingPassword() throws Exception {
        String hash=hash();long before=count("SELECT count(*) FROM ba_utilisateur");
        try(var second=startApplication(Map.of("app.bootstrap.admin.email","different@example.test","app.bootstrap.admin.password",""))) {
            assertThat(second.isActive()).isTrue();assertThat(hash()).isEqualTo(hash);
            assertThat(count("SELECT count(*) FROM ba_utilisateur")).isEqualTo(before);
        }
        login(PASSWORD,200);
    }
    @Test void passwordChangedThroughTheApiIsPreservedByInitialization() throws Exception {
        String token=login(PASSWORD,200), changed="Changed-test-Password";
        call("PUT","users/change-password",Map.of("ancien",PASSWORD,"nouveau",changed,"confirmer",changed),token,200);
        String hash=hash();assertThat(app.getBean(InitialAdminService.class).initialize()).isEqualTo(InitialAdminService.Result.ALREADY_PRESENT);
        assertThat(hash()).isEqualTo(hash);login(changed,200);login(PASSWORD,401);
    }
    @Test void disabledAdministratorIsNotReactivatedOrReplacedByBootstrap() throws Exception {
        long before=count("SELECT count(*) FROM ba_utilisateur");String hash=hash();
        execute("UPDATE ba_utilisateur SET activated=false,account_locked=true,statut='D' WHERE username='"+EMAIL+"'");
        assertThat(app.getBean(InitialAdminService.class).initialize()).isEqualTo(InitialAdminService.Result.ALREADY_PRESENT);
        assertThat(count("SELECT count(*) FROM ba_utilisateur")).isEqualTo(before);assertThat(hash()).isEqualTo(hash);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE username='"+EMAIL+"' AND statut='D' AND NOT activated AND account_locked")).isEqualTo(1);
        login(PASSWORD,401);
    }
    @Test void simultaneousInitializationsCreateExactlyOneAdminAndAuditEntry() throws Exception {
        execute("DELETE FROM ba_user_roles WHERE user_id IN (SELECT id FROM ba_utilisateur WHERE username='"+EMAIL+"')");
        execute("DELETE FROM ba_utilisateur WHERE username='"+EMAIL+"'");
        long before=count("SELECT count(*) FROM ba_log WHERE sujet='Création de l''administrateur initial'");
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try{
            Callable<InitialAdminService.Result> operation=()->{ready.countDown();go.await();return app.getBean(InitialAdminService.class).initialize();};
            var first=pool.submit(operation);var second=pool.submit(operation);assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
            assertThat(List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(InitialAdminService.Result.CREATED,InitialAdminService.Result.ALREADY_PRESENT);
        }finally{go.countDown();pool.shutdownNow();}
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE username='"+EMAIL+"'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_log WHERE sujet='Création de l''administrateur initial'")).isEqualTo(before+1);
        var initial=call("POST","authenticate",Map.of("username",EMAIL,"password",PASSWORD),null,200);
        assertThat(initial.path("passwordChangeRequired").asBoolean()).isTrue();
        String ticket=initial.path("access_token").asText();
        call("GET","users/details",null,ticket,403);login(PASSWORD,401);
        call("PUT","users/complete-initial-password",Map.of("nouveau","Personal-admin-Password","confirmer","Personal-admin-Password"),ticket,204);
        login("Personal-admin-Password",200);
    }
    @Test void administratorInheritedFromAProfilePreventsAnotherBootstrap() throws Exception {
        execute("INSERT INTO ba_profil(id,libelle,created_by,created_date) VALUES ('bootstrap-profile','Administration','test',now()) ON CONFLICT DO NOTHING");
        execute("INSERT INTO ba_profils_roles(profil_id,role_id) SELECT 'bootstrap-profile',id FROM ba_role WHERE code='BA_ADMIN' ON CONFLICT DO NOTHING");
        execute("UPDATE ba_utilisateur SET profil_uuid='bootstrap-profile' WHERE username='"+EMAIL+"'");
        execute("DELETE FROM ba_user_roles WHERE user_id IN (SELECT id FROM ba_utilisateur WHERE username='"+EMAIL+"')");
        try {
            assertThat(app.getBean(BaUserRepository.class).existsWithRoleCode("BA_ADMIN")).isTrue();
            assertThat(app.getBean(InitialAdminService.class).initialize()).isEqualTo(InitialAdminService.Result.ALREADY_PRESENT);
        } finally {
            execute("UPDATE ba_utilisateur SET profil_uuid=NULL WHERE username='"+EMAIL+"'");
            execute("INSERT INTO ba_user_roles(user_id,role_id) SELECT u.id,r.id FROM ba_utilisateur u CROSS JOIN ba_role r WHERE u.username='"+EMAIL+"' AND r.code='BA_ADMIN' ON CONFLICT DO NOTHING");
        }
    }

    private ConfigurableApplicationContext startApplication(Map<String,Object> overrides) {
        var p=new HashMap<String,Object>();
        p.put("server.port",0);p.put("server.address","127.0.0.1");
        p.put("spring.datasource.url",url()+(url().contains("?")?"&":"?")+"currentSchema="+schema);
        p.put("spring.datasource.username",user());p.put("spring.datasource.password",password());
        p.put("spring.flyway.schemas",schema);p.put("spring.flyway.default-schema",schema);
        p.put("spring.jpa.properties.hibernate.default_schema",schema);p.put("spring.jpa.hibernate.ddl-auto","validate");
        p.put("spring.jpa.show-sql",false);p.put("spring.jpa.properties.hibernate.show_sql",false);
        p.put("app.features.habilitations-enabled",false);p.put("app.conges.traitements-automatiques-enabled",false);p.put("app.conges.notifications-mail-enabled",false);
        p.put("app.security.jwt.base64-secret",Base64.getEncoder().encodeToString(new byte[64]));p.put("spring.mail.username","noreply@example.test");
        p.put("app.bootstrap.admin.enabled",true);p.put("app.bootstrap.admin.email",EMAIL);p.put("app.bootstrap.admin.password",PASSWORD);p.put("app.bootstrap.admin.telephone",PHONE);
        p.put("logging.level.root","WARN");p.put("logging.level.com.bakouan.app.config.BaRestExceptionHandler","OFF");p.putAll(overrides);
        return new SpringApplicationBuilder(BsicServiceApplication.class,RhCircuitPostgresApiTest.IsolatedMailConfiguration.class,CongePostgresApiTest.TestMail.class)
                .profiles("prod").initializers(c->{c.getEnvironment().setActiveProfiles("prod");c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("initial-admin-test",p));})
                .run("--spring.profiles.active=prod","--logging.level.root=WARN");
    }
    private String login(String password,int status) throws Exception {
        var response=call("POST","authenticate",Map.of("username",EMAIL,"password",password),null,status);
        return response.path("access_token").asText();
    }
    private JsonNode call(String method,String path,Object body,String token,int expected) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+"/api/"+path)).header("Content-Type","application/json").header("Accept","application/json");
        if(token!=null)b.header("Authorization","Bearer "+token);
        var response=http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("Réponse HTTP de %s %s",method,path).isEqualTo(expected);
        if(expected>=400)return json.nullNode();
        return response.body().isBlank()?json.nullNode():json.readTree(response.body());
    }
    private String hash() throws Exception {try(var p=sql.prepareStatement("SELECT password_hash FROM ba_utilisateur WHERE username=?")){p.setString(1,EMAIL);try(var rows=p.executeQuery()){assertThat(rows.next()).isTrue();return rows.getString(1);}}}
    private long count(String query) throws Exception {try(var s=sql.createStatement();var rows=s.executeQuery(query)){rows.next();return rows.getLong(1);}}
    private void execute(String query) throws Exception {try(var s=sql.createStatement()){s.execute(query);}}
    private String url(){return System.getenv("BSIC_MIGRATION_TEST_URL");}
    private String user(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_USERNAME","postgres");}
    private String password(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_PASSWORD","");}
}

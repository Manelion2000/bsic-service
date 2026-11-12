package com.bakouan.app.service;

import com.bakouan.app.BsicServiceApplication;
import com.bakouan.app.security.LoginAttemptService;
import com.bakouan.app.security.jwt.TokenProvider;
import com.fasterxml.jackson.databind.*;
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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** API réelle, connexions concurrentes et transactions PostgreSQL dans un schéma jetable. */
@EnabledIfEnvironmentVariable(named="BSIC_MIGRATION_TEST_URL",matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordAccessPostgresApiTest {
    private static final String ORIGINAL="Original-test-Password",PERSONAL="Personal-test-Password";
    private String schema,base,admin;
    private Connection sql;
    private ConfigurableApplicationContext app;
    private BaMailService mail;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    private final HttpClient http=HttpClient.newHttpClient();
    @BeforeAll void start() throws Exception {
        schema="bsic_password_test_"+UUID.randomUUID().toString().replace("-","");
        sql=DriverManager.getConnection(url(),user(),password());
        execute("CREATE SCHEMA "+schema);execute("SET search_path TO "+schema);
        var p=new HashMap<String,Object>();
        p.put("server.port",0);p.put("server.address","127.0.0.1");
        p.put("spring.datasource.url",url()+(url().contains("?")?"&":"?")+"currentSchema="+schema);
        p.put("spring.datasource.username",user());p.put("spring.datasource.password",password());
        p.put("spring.flyway.schemas",schema);p.put("spring.flyway.default-schema",schema);
        p.put("spring.jpa.properties.hibernate.default_schema",schema);p.put("spring.jpa.hibernate.ddl-auto","validate");
        p.put("spring.jpa.show-sql",false);p.put("spring.jpa.properties.hibernate.show_sql",false);
        p.put("app.bootstrap.admin.enabled",false);p.put("app.features.habilitations-enabled",false);
        p.put("app.conges.traitements-automatiques-enabled",false);p.put("app.conges.notifications-mail-enabled",false);
        p.put("app.security.jwt.base64-secret",Base64.getEncoder().encodeToString(new byte[64]));
        p.put("logging.level.root","WARN");p.put("logging.level.com.bakouan.app.config.BaRestExceptionHandler","OFF");
        app=new SpringApplicationBuilder(BsicServiceApplication.class,RhCircuitPostgresApiTest.IsolatedMailConfiguration.class,CongePostgresApiTest.TestMail.class)
            .profiles("prod").initializers(c->{c.getEnvironment().setActiveProfiles("prod");c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("password-test",p));})
            .run("--spring.profiles.active=prod","--logging.level.root=WARN");
        base="http://127.0.0.1:"+((ServletWebServerApplicationContext)app).getWebServer().getPort();mail=app.getBean(BaMailService.class);
    }
    @BeforeEach void fixture() throws Exception {
        execute("TRUNCATE ba_utilisateur CASCADE");execute("DELETE FROM ba_log");
        app.getBean(LoginAttemptService.class).loginSucceeded("127.0.0.1");app.getBean(LoginAttemptService.class).loginSucceeded("password-reset:127.0.0.1");
        String hash=app.getBean(PasswordEncoder.class).encode(ORIGINAL);
        for(String id:List.of("admin","admin2","agent","chief")){
            try(var q=sql.prepareStatement("INSERT INTO ba_utilisateur(id,username,email,password_hash,nom,prenom,telephone,sexe,fonction,activated,account_locked,created_by,created_date,version) VALUES (?,?,?,?,?,?,?,'MASCULIN',?,true,false,'test',now(),1)")){
                q.setString(1,id);q.setString(2,id+"@example.test");q.setString(3,id+"@example.test");q.setString(4,hash);q.setString(5,"Test");q.setString(6,id);q.setString(7,"phone-"+id);q.setString(8,id.equals("chief")?"CHEF_SERVICE":"AGENT");q.executeUpdate();
            }
            execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('"+id+"','role-ba-connect')");
        }
        execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('admin','role-ba-admin'),('admin2','role-ba-admin')");
        admin=login("admin",ORIGINAL,200).path("access_token").asText();reset(mail);
    }
    @AfterEach void noMail(){verifyNoInteractions(mail);}
    @AfterAll void cleanup() throws Exception {
        if(app!=null)app.close();if(sql!=null)try{if(schema!=null&&schema.matches("bsic_password_test_[0-9a-f]{32}"))execute("DROP SCHEMA "+schema+" CASCADE");}finally{sql.close();}
    }
    @Test void resetIsOneTimeHashedAuditedAndInvalidatesEveryOldSession() throws Exception {
        var old=login("agent",ORIGINAL,200).path("access_token").asText();
        var receipt=resetAgent();String code=receipt.path("temporaryPassword").asText();
        assertThat(code.length()).isEqualTo(24);assertThat(receipt.path("username").asText()).isEqualTo("agent@example.test");
        assertThat(Instant.parse(receipt.path("expiresAt").asText())).isBetween(Instant.now().plusSeconds(86300),Instant.now().plusSeconds(86500));
        assertThat(app.getBean(PasswordEncoder.class).matches(code,hash("agent"))).isTrue();
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND password_change_required AND NOT temporary_password_used AND credential_version=1 AND activated AND NOT account_locked")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_log WHERE sujet LIKE 'Réinitialisation accès utilisateur : agent%' AND created_by='admin@example.test'")).isEqualTo(1);
        status(401,call("GET","users/details",null,old));login("agent",ORIGINAL,401);
        var list=status(200,call("GET","users",null,admin));assertThat(list.toString().contains(code)).isFalse();
        assertThat(list.findValues("initialCredential")).isEmpty();assertThat(list.findValues("password")).isEmpty();
        var initial=login("agent",code,200);assertThat(initial.path("passwordChangeRequired").asBoolean()).isTrue();
        String ticket=initial.path("access_token").asText();
        assertThat(app.getBean(TokenProvider.class).getAuthentication(ticket).getAuthorities()).isEmpty();
        assertThat(app.getBean(TokenProvider.class).extractExpiration(ticket).toInstant()).isBetween(Instant.now().plusSeconds(890),Instant.now().plusSeconds(910));
        login("agent",code,401);status(403,call("GET","users/details",null,ticket));status(403,call("POST","users/agent/initialiser-acces",resetBody(),ticket));
        status(204,call("PUT","users/complete-initial-password",Map.of("nouveau",PERSONAL,"confirmer",PERSONAL),ticket));
        status(401,call("PUT","users/complete-initial-password",Map.of("nouveau",PERSONAL,"confirmer",PERSONAL),ticket));
        login("agent",code,401);var normal=login("agent",PERSONAL,200);
        assertThat(normal.path("passwordChangeRequired").asBoolean()).isFalse();status(200,call("GET","users/details",null,normal.path("access_token").asText()));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND NOT password_change_required AND NOT temporary_password_used AND temporary_password_expires_at IS NULL AND credential_version=2")).isEqualTo(1);
    }
    @Test void directApiProtectsPermissionSelfResetLockedTargetsAndStaleForms() throws Exception {
        String chief=login("chief",ORIGINAL,200).path("access_token").asText();
        status(403,call("POST","users/agent/initialiser-acces",resetBody(),chief));
        status(401,call("POST","users/agent/initialiser-acces",resetBody(),null));
        status(400,call("POST","users/admin/initialiser-acces",Map.of("version",version("admin"),"motif","Test"),admin));
        status(400,call("POST","users/agent/initialiser-acces",Map.of("version",version("agent"),"motif"," "),admin));
        status(400,call("POST","users/agent/initialiser-acces",Map.of("motif","Test"),admin));
        status(409,call("POST","users/agent/initialiser-acces",Map.of("version",0,"motif","Test"),admin));
        execute("UPDATE ba_utilisateur SET account_locked=true WHERE id='agent'");
        status(409,call("POST","users/agent/initialiser-acces",resetBody(),admin));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND account_locked AND credential_version=0")).isEqualTo(1);
        execute("DELETE FROM ba_user_roles WHERE user_id='admin' AND role_id='role-ba-admin'");
        status(403,call("POST","users/chief/initialiser-acces",Map.of("version",version("chief"),"motif","Test"),admin));
    }
    @Test void activeProfileAdminCanResetAndInactiveProfileCannot() throws Exception {
        execute("INSERT INTO ba_profil(id,libelle,statut,created_by,created_date) VALUES ('profile','Admin profil','A','test',now())");
        execute("INSERT INTO ba_profils_roles(profil_id,role_id) VALUES ('profile','role-ba-admin')");
        execute("UPDATE ba_utilisateur SET profil_uuid='profile' WHERE id='chief'");
        String chief=login("chief",ORIGINAL,200).path("access_token").asText();
        assertThat(status(200,call("GET","users/details",null,chief)).path("canResetUserAccess").asBoolean()).isTrue();
        status(200,call("POST","users/agent/initialiser-acces",resetBody(),chief));
        execute("UPDATE ba_profil SET statut='D' WHERE id='profile'");
        assertThat(status(200,call("GET","users/details",null,chief)).path("canResetUserAccess").asBoolean()).isFalse();
        status(403,call("POST","users/agent/initialiser-acces",resetBody(),chief));
    }
    @Test void passwordValidationKeepsSetupSessionUsableUntilValidCompletion() throws Exception {
        String code=resetAgent().path("temporaryPassword").asText();String setup=login("agent",code,200).path("access_token").asText();
        for(var body:List.of(Map.of("nouveau","1234","confirmer","1234"),Map.of("nouveau",PERSONAL,"confirmer","different"),Map.of("nouveau",code,"confirmer",code),Map.of("nouveau","é".repeat(37),"confirmer","é".repeat(37))))status(400,call("PUT","users/complete-initial-password",body,setup));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND password_change_required AND temporary_password_used AND credential_version=1")).isEqualTo(1);
        status(403,call("PUT","users/complete-initial-password",Map.of("nouveau",PERSONAL,"confirmer",PERSONAL),admin));
        status(204,call("PUT","users/complete-initial-password",Map.of("nouveau",PERSONAL,"confirmer",PERSONAL),setup));
    }
    @Test void expiredOrReplacedTemporaryPasswordCannotBeReused() throws Exception {
        String first=resetAgent().path("temporaryPassword").asText();
        execute("UPDATE ba_utilisateur SET temporary_password_expires_at=now()-interval '1 minute' WHERE id='agent'");login("agent",first,401);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND NOT temporary_password_used")).isEqualTo(1);
        String second=resetAgent().path("temporaryPassword").asText();login("agent",first,401);
        String stale=login("agent",second,200).path("access_token").asText();
        String third=resetAgent().path("temporaryPassword").asText();
        status(401,call("PUT","users/complete-initial-password",Map.of("nouveau",PERSONAL,"confirmer",PERSONAL),stale));
        login("agent",third,200);
    }
    @Test void concurrentResetsAcceptOneVersionAndProduceOneAuditEvent() throws Exception {
        var body=resetBody();String other=login("admin2",ORIGINAL,200).path("access_token").asText();
        assertThat(race(()->call("POST","users/agent/initialiser-acces",body,admin).statusCode(),()->call("POST","users/agent/initialiser-acces",body,other).statusCode())).containsExactlyInAnyOrder(200,409);
        assertThat(count("SELECT count(*) FROM ba_log WHERE sujet LIKE 'Réinitialisation accès utilisateur : agent%'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND credential_version=1")).isEqualTo(1);
    }
    @Test void concurrentTemporaryLoginsConsumeTheCodeOnce() throws Exception {
        String code=resetAgent().path("temporaryPassword").asText();var body=Map.of("username","agent@example.test","password",code);
        assertThat(race(()->call("POST","authenticate",body,null).statusCode(),()->call("POST","authenticate",body,null).statusCode())).containsExactlyInAnyOrder(200,401);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND credential_version=1 AND temporary_password_used")).isEqualTo(1);
    }
    @Test void concurrentCompletionsChangeCredentialsOnlyOnce() throws Exception {
        String setup=login("agent",resetAgent().path("temporaryPassword").asText(),200).path("access_token").asText();var body=Map.of("nouveau",PERSONAL,"confirmer",PERSONAL);
        assertThat(race(()->call("PUT","users/complete-initial-password",body,setup).statusCode(),()->call("PUT","users/complete-initial-password",body,setup).statusCode())).containsExactlyInAnyOrder(204,401);
        assertThat(count("SELECT count(*) FROM ba_log WHERE sujet='Définition du mot de passe personnel : agent'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND credential_version=2")).isEqualTo(1);
    }
    @Test void personalChangeInvalidatesOtherSessionsAndRequiresCurrentPassword() throws Exception {
        String token=login("agent",ORIGINAL,200).path("access_token").asText();String other=login("agent",ORIGINAL,200).path("access_token").asText();
        status(400,call("PUT","users/change-password",Map.of("ancien","wrong","nouveau",PERSONAL,"confirmer",PERSONAL),token));
        status(200,call("PUT","users/change-password",Map.of("ancien",ORIGINAL,"nouveau",PERSONAL,"confirmer",PERSONAL),token));
        status(401,call("GET","users/details",null,token));status(401,call("GET","users/details",null,other));login("agent",PERSONAL,200);
    }
    @Test void newAccountsReturnAccessOnceAndProfileEditsCannotAlterCredentialState() throws Exception {
        var createdResponse=call("POST","users",Map.of("nom","Compte","prenom","Nouveau","email","new@example.test","telephone","phone-new","sexe","MASCULIN","fonction","DIRECTEUR_GENERAL_ADJOINT","passwordChangeRequired",false),admin);
        assertThat(createdResponse.headers().firstValue("Cache-Control").orElse("")).contains("no-store");var created=status(201,createdResponse);
        assertThat(created.path("passwordChangeRequired").asBoolean()).isTrue();
        String code=created.at("/initialCredential/temporaryPassword").asText();assertThat(code.length()).isEqualTo(24);
        String id=created.path("id").asText();String setup=login("new",code,200).path("access_token").asText();
        var edit=json.convertValue(created,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
        edit.put("nom","Modifié");edit.put("passwordChangeRequired",false);edit.put("credentialVersion",0);edit.put("password",PERSONAL);
        status(200,call("PUT","users/"+id,edit,admin));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='"+id+"' AND password_change_required AND temporary_password_used AND credential_version=1")).isEqualTo(1);
        status(204,call("PUT","users/complete-initial-password",Map.of("nouveau",PERSONAL,"confirmer",PERSONAL),setup));
    }
    @Test void legacyEmailRecoveryNeverSendsCredentialsOrBypassesForcedSetup() throws Exception {
        String code=resetAgent().path("temporaryPassword").asText();
        execute("UPDATE ba_utilisateur SET reset_key='legacy-code',reset_date=now() WHERE id='agent'");
        for(String email:List.of("agent@example.test","unknown@example.test"))status(200,call("PUT","users/request-reset-password",Map.of("email",email),null));
        status(400,call("PUT","users/complete-reset-password",Map.of("email","agent@example.test","resetKey","legacy-code","nouveau",PERSONAL,"confirmer",PERSONAL),null));
        login("agent",code,200);
    }
    @Test void auditFailureRollsBackTheResetAndKeepsOldAccessUsable() throws Exception {
        String originalHash=hash("agent");String token=login("agent",ORIGINAL,200).path("access_token").asText();
        execute("CREATE FUNCTION reject_password_audit() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''test audit failure''; END'");
        execute("CREATE TRIGGER reject_password_audit BEFORE INSERT ON ba_log FOR EACH ROW EXECUTE FUNCTION reject_password_audit()");
        try {
            status(500,call("POST","users/agent/initialiser-acces",resetBody(),admin));
            assertThat(hash("agent").equals(originalHash)).isTrue();
            assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE id='agent' AND credential_version=0 AND NOT password_change_required")).isEqualTo(1);
        } finally {
            execute("DROP TRIGGER reject_password_audit ON ba_log");execute("DROP FUNCTION reject_password_audit()");
        }
        status(200,call("GET","users/details",null,token));
    }
    private JsonNode resetAgent() throws Exception {
        var response=call("POST","users/agent/initialiser-acces",resetBody(),admin);
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");return status(200,response);
    }
    private Map<String,Object> resetBody() throws Exception{return Map.of("version",version("agent"),"motif","Identité vérifiée en personne");}
    private JsonNode login(String id,String value,int expected) throws Exception{return status(expected,call("POST","authenticate",Map.of("username",id+"@example.test","password",value),null));}
    private HttpResponse<String> call(String method,String path,Object body,String token) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+"/api/"+path)).header("Content-Type","application/json").header("Accept","application/json");if(token!=null)b.header("Authorization","Bearer "+token);
        return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode status(int expected,HttpResponse<String> response) throws Exception {
        // Ne jamais mettre un ticket ou mot de passe dans les rapports de tests.
        assertThat(response.statusCode()).as("Statut HTTP").isEqualTo(expected);return response.body().isBlank()?json.nullNode():json.readTree(response.body());
    }
    private List<Integer> race(Callable<Integer> first,Callable<Integer> second) throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->{ready.countDown();go.await();return first.call();});var b=pool.submit(()->{ready.countDown();go.await();return second.call();});assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();return List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    private long version(String id) throws Exception{return count("SELECT version FROM ba_utilisateur WHERE id='"+id+"'");}
    private String hash(String id) throws Exception{try(var q=sql.prepareStatement("SELECT password_hash FROM ba_utilisateur WHERE id=?")){q.setString(1,id);try(var r=q.executeQuery()){assertThat(r.next()).isTrue();return r.getString(1);}}}
    private long count(String query) throws Exception{try(var s=sql.createStatement();var r=s.executeQuery(query)){r.next();return r.getLong(1);}}
    private void execute(String query) throws Exception{try(var s=sql.createStatement()){s.execute(query);}}
    private String url(){return System.getenv("BSIC_MIGRATION_TEST_URL");}
    private String user(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_USERNAME","postgres");}
    private String password(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_PASSWORD","");}
}

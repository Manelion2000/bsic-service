package com.bakouan.app.service;

import com.bakouan.app.BsicServiceApplication;
import com.bakouan.app.security.jwt.TokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Vraies transactions et API, base isolee, aucun envoi d'email. */
@EnabledIfEnvironmentVariable(named = "BSIC_MIGRATION_TEST_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectionGeneralePostgresApiTest {
    private String schema, base;
    private Connection sql;
    private ConfigurableApplicationContext app;
    private BaMailService mail;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String, String> tokens = new HashMap<>();

    @BeforeAll void start() throws Exception {
        schema = "bsic_direction_test_" + UUID.randomUUID().toString().replace("-", "");
        sql = DriverManager.getConnection(url(), username(), password());
        execute("CREATE SCHEMA " + schema); execute("SET search_path TO " + schema);
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", 0); properties.put("server.address", "127.0.0.1");
        properties.put("spring.datasource.url", url() + (url().contains("?") ? "&" : "?") + "currentSchema=" + schema);
        properties.put("spring.datasource.username", username()); properties.put("spring.datasource.password", password());
        properties.put("spring.flyway.enabled", true); properties.put("spring.flyway.schemas", schema);
        properties.put("spring.flyway.default-schema", schema); properties.put("spring.jpa.properties.hibernate.default_schema", schema);
        properties.put("spring.jpa.hibernate.ddl-auto", "validate"); properties.put("spring.jpa.show-sql", false);
        properties.put("spring.jpa.properties.hibernate.show_sql", false); properties.put("app.features.habilitations-enabled", false);
        properties.put("app.rh.validation.dg-enabled", true); properties.put("app.rh.validation.refusal-policy", "DEFINITIF");
        properties.put("app.security.jwt.base64-secret", Base64.getEncoder().encodeToString(new byte[64]));
        properties.put("logging.level.root", "WARN"); properties.put("logging.level.com.bakouan.app.config.BaRestExceptionHandler", "OFF");
        properties.put("logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper", "OFF");
        properties.put("logging.level.org.apache.catalina.core", "OFF");
        app = new SpringApplicationBuilder(BsicServiceApplication.class, RhCircuitPostgresApiTest.IsolatedMailConfiguration.class)
                .profiles("prod").initializers(c -> {
                    c.getEnvironment().setActiveProfiles("prod");
                    c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("direction-test", properties));
                }).run("--spring.profiles.active=prod", "--logging.level.root=WARN");
        base = "http://127.0.0.1:" + ((ServletWebServerApplicationContext) app).getWebServer().getPort();
        mail = app.getBean(BaMailService.class);
    }

    @BeforeEach void resetDatabase() throws Exception {
        execute("TRUNCATE ba_utilisateur CASCADE");
        for (String id : List.of("admin", "agent", "chief")) {
            execute("INSERT INTO ba_utilisateur(id,username,password_hash,nom,telephone,fonction,activated,account_locked,created_by,created_date) "
                    + "VALUES ('" + id + "','" + id + "','unused','Test','" + id + "',"
                    + (id.equals("chief") ? "'CHEF_SERVICE'" : "'AGENT'") + ",true,false,'test',now())");
            execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('" + id + "','role-ba-connect')");
            tokens.put(id, token(id));
        }
        execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('admin','role-ba-admin')");
        reset(mail);
    }

    @AfterAll void cleanup() throws Exception {
        if (app != null) app.close();
        if (sql != null) try {
            if (schema != null && schema.matches("bsic_direction_test_[0-9a-f]{32}")) execute("DROP SCHEMA " + schema + " CASCADE");
        } finally { sql.close(); }
    }

    @Test void createsDgAndBothDgaWithoutDepartmentsAndTheirCircuitsUseTheNewAssignments() throws Exception {
        JsonNode ops = create("dga", "ops", "OPERATIONS");
        JsonNode dev = create("dga", "dev", "DEVELOPPEMENT");
        JsonNode dg = create("dg", "dg", null);
        assertThat(ops.path("fonction").asText()).isEqualTo("DIRECTEUR_GENERAL_ADJOINT");
        assertThat(dg.path("fonction").asText()).isEqualTo("DIRECTEUR_GENERAL");
        assertThat(ops.path("idDepartement").isNull()).isTrue();
        assertThat(ops.path("idService").isNull()).isTrue();
        assertThat(ops.path("idSuperieur").isNull()).isTrue();
        assertThat(ops.path("activated").asBoolean()).isTrue();
        assertThat(roleCodes(ops)).containsExactlyInAnyOrder("BA_CONNECT", "BA_DGA");
        assertThat(roleCodes(dg)).containsExactlyInAnyOrder("BA_CONNECT", "BA_DG");
        assertThat(ops.has("password")).isFalse();
        assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur WHERE actif")).isEqualTo(2);
        verifyNoInteractions(mail);
        JsonNode assignments = status(200, call("admin", "GET", "/api/direction-generale/affectations-dga", null));
        assertThat(assignments.size()).isEqualTo(2);
        String email = ops.path("username").asText(); tokens.put(email, personalToken(ops));
        JsonNode circuit = status(200, call(email, "GET", "/api/rh/circuit", null));
        assertThat(circuit.path("blocage").isNull()).isTrue();
        assertThat(circuit.at("/etapes/0/type").asText()).isEqualTo("AUTRE_DGA");
        assertThat(circuit.at("/etapes/0/validateurs/0/id").asText()).isEqualTo(dev.path("id").asText());
        assertThat(circuit.at("/etapes/1/validateurs/0/id").asText()).isEqualTo(dg.path("id").asText());
    }

    @Test void directApiRejectsAnonymousAgentsChiefsAndTechnicalDgaRoleWithoutAdmin() throws Exception {
        execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('agent','role-ba-dga')");
        for (String path : List.of("/dg", "/dga", "/affectations-dga")) {
            for (String actor : List.of("agent", "chief")) {
                status(403, call(actor, "GET", "/api/direction-generale" + path, null));
                status(403, call(actor, "POST", "/api/direction-generale" + path, body("forbidden", path.equals("/dga") ? "OPERATIONS" : null)));
            }
        }
        assertThat(call(null, "GET", "/api/direction-generale/dg", null).statusCode()).isEqualTo(401);
        assertThat(count("SELECT count(*) FROM ba_utilisateur")).isEqualTo(3);
        verifyNoInteractions(mail);
    }

    @Test void duplicateDgIsBlockedThroughDedicatedAndGenericApiIncludingReactivation() throws Exception {
        JsonNode old = create("dg", "old-dg", null);
        status(409, call("admin", "POST", "/api/direction-generale/dg", body("second-dg", null)));
        Map<String, Object> generic = identity("generic-dg"); generic.put("fonction", "DIRECTEUR_GENERAL");
        status(409, call("admin", "POST", "/api/users", json.writeValueAsString(generic)));
        status(200, call("admin", "PUT", "/api/users/" + old.path("id").asText() + "/desactiver", "{}"));
        create("dg", "replacement", null);
        assertThat(call("admin", "PUT", "/api/users/" + old.path("id").asText() + "/activer", "{}").statusCode()).isEqualTo(409);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL' AND activated AND NOT account_locked")).isEqualTo(1);
    }

    @Test void duplicatePoleNeverCreatesAnOrphanAndRoleOnlyAccountsCannotBeAssigned() throws Exception {
        create("dga", "ops", "OPERATIONS");
        status(409, call("admin", "POST", "/api/direction-generale/dga", body("duplicate", "OPERATIONS")));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL_ADJOINT'")).isEqualTo(1);
        execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('agent','role-ba-dga')");
        Map<String, Object> assignment = assignment("agent", "DEVELOPPEMENT");
        status(400, call("admin", "POST", "/api/direction-generale/affectations-dga", json.writeValueAsString(assignment)));
        verifyNoInteractions(mail);
    }

    @Test void existingDgaCanBeAssignedButCannotHoldBothPolesAndReplacementPreservesHistory() throws Exception {
        JsonNode ops = create("dga", "ops", "OPERATIONS");
        Map<String, Object> assignment = assignment(ops.path("id").asText(), "DEVELOPPEMENT");
        status(409, call("admin", "POST", "/api/direction-generale/affectations-dga", json.writeValueAsString(assignment)));
        JsonNode previous = status(200, call("admin", "GET", "/api/direction-generale/affectations-dga", null)).get(0);
        assertThat(call("agent", "DELETE", "/api/direction-generale/affectations-dga/" + previous.path("id").asText(), null).statusCode()).isEqualTo(403);
        assertThat(call("admin", "DELETE", "/api/direction-generale/affectations-dga/" + previous.path("id").asText(), null).statusCode()).isEqualTo(204);
        status(201, call("admin", "POST", "/api/direction-generale/affectations-dga", json.writeValueAsString(assignment)));
        assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur WHERE actif")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL_ADJOINT'")).isEqualTo(1);
    }

    @Test void malformedIdentityAndDatesAreRejectedAndClientCannotGrantAdminOrChangeFunction() throws Exception {
        Map<String, Object> invalid = identity("invalid"); invalid.put("email", "invalid");
        status(400, call("admin", "POST", "/api/direction-generale/dg", json.writeValueAsString(invalid)));
        status(400, call("admin", "POST", "/api/direction-generale/dga", body("no-pole", null)));
        Map<String, Object> dates = identity("bad-dates"); dates.put("pole", "OPERATIONS");
        dates.put("dateDebut", LocalDate.now().toString()); dates.put("dateFin", LocalDate.now().minusDays(1).toString());
        status(400, call("admin", "POST", "/api/direction-generale/dga", json.writeValueAsString(dates)));
        Map<String, Object> forged = identity("forged"); forged.put("fonction", "AGENT");
        forged.put("roles", List.of(Map.of("id", "role-ba-admin"))); forged.put("idDepartement", "missing");
        JsonNode result = status(201, call("admin", "POST", "/api/direction-generale/dg", json.writeValueAsString(forged)));
        assertThat(result.path("fonction").asText()).isEqualTo("DIRECTEUR_GENERAL");
        assertThat(roleCodes(result)).containsExactlyInAnyOrder("BA_CONNECT", "BA_DG");
    }

    @Test void concurrentCreationsProduceExactlyOneDgaAndOneAssignment() throws Exception {
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> { ready.countDown(); go.await(); return call("admin", "POST", "/api/direction-generale/dga", body("race-a", "OPERATIONS")).statusCode(); });
            var b = executor.submit(() -> { ready.countDown(); go.await(); return call("admin", "POST", "/api/direction-generale/dga", body("race-b", "OPERATIONS")).statusCode(); });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        } finally { executor.shutdownNow(); }
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL_ADJOINT'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur")).isEqualTo(1);
        verifyNoInteractions(mail);
    }

    @Test void databaseFailureRollsBackAccountAssignmentAndCredentialEmail() throws Exception {
        execute("CREATE FUNCTION reject_test_assignment() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''test assignment failure''; END'");
        execute("CREATE TRIGGER reject_test_assignment BEFORE INSERT ON ba_dga_pole_validateur FOR EACH ROW EXECUTE FUNCTION reject_test_assignment()");
        try {
            assertThat(call("admin", "POST", "/api/direction-generale/dga", body("rollback", "OPERATIONS")).statusCode()).isGreaterThanOrEqualTo(400);
            assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL_ADJOINT'")).isZero();
            assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur")).isZero();
            verifyNoInteractions(mail);
        } finally { execute("DROP TRIGGER reject_test_assignment ON ba_dga_pole_validateur"); execute("DROP FUNCTION reject_test_assignment()"); }
    }

    @Test void concurrentDedicatedAndGenericDgCreationStillCreatesOnlyOneActiveDg() throws Exception {
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> { ready.countDown(); go.await(); return call("admin", "POST", "/api/direction-generale/dg", body("dedicated", null)).statusCode(); });
            var b = executor.submit(() -> {
                Map<String, Object> generic = identity("generic"); generic.put("fonction", "DIRECTEUR_GENERAL");
                ready.countDown(); go.await(); return call("admin", "POST", "/api/users", json.writeValueAsString(generic)).statusCode();
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        } finally { executor.shutdownNow(); }
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL'")).isEqualTo(1);
        verifyNoInteractions(mail);
    }

    @Test void periodsIncludeTheirEndDateAndAllowAReplacementStartingTheNextDay() throws Exception {
        Map<String, Object> first = identity("first"); first.put("pole", "OPERATIONS");
        first.put("dateDebut", LocalDate.now().toString()); first.put("dateFin", LocalDate.now().toString());
        status(201, call("admin", "POST", "/api/direction-generale/dga", json.writeValueAsString(first)));
        status(409, call("admin", "POST", "/api/direction-generale/dga", body("same-day", "OPERATIONS")));
        Map<String, Object> next = identity("next"); next.put("pole", "OPERATIONS"); next.put("dateDebut", LocalDate.now().plusDays(1).toString());
        status(201, call("admin", "POST", "/api/direction-generale/dga", json.writeValueAsString(next)));
        assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur WHERE actif")).isEqualTo(2);
    }

    @Test void legacyAssignmentEndpointUsesTheSameSecurityAndOverlapChecks() throws Exception {
        JsonNode ops = create("dga", "ops", "OPERATIONS");
        String body = json.writeValueAsString(assignment(ops.path("id").asText(), "OPERATIONS"));
        var feature = app.getBean(com.bakouan.app.config.FeatureConfig.class);
        assertThat(call("admin", "POST", "/api/dga-pole-validateurs", body).statusCode()).isEqualTo(404);
        feature.setHabilitationsEnabled(true);
        try {
            status(403, call("agent", "POST", "/api/dga-pole-validateurs", body));
            status(409, call("admin", "POST", "/api/dga-pole-validateurs", body));
            assertThat(count("SELECT count(*) FROM ba_dga_pole_validateur")).isEqualTo(1);
        } finally { feature.setHabilitationsEnabled(false); }
    }

    @Test void duplicateMatriculeIsReportedWithoutCreatingAnAccount() throws Exception {
        execute("UPDATE ba_utilisateur SET matricule='EXISTING' WHERE id='agent'");
        Map<String, Object> duplicate = identity("duplicate-matricule"); duplicate.put("matricule", " EXISTING ");
        status(400, call("admin", "POST", "/api/direction-generale/dg", json.writeValueAsString(duplicate)));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE fonction='DIRECTEUR_GENERAL'")).isZero();
        verifyNoInteractions(mail);
    }

    private String personalToken(JsonNode created) throws Exception {
        var login=Map.of("username",created.path("username").asText(),"password",created.at("/initialCredential/temporaryPassword").asText());
        var temporary=status(200,call(null,"POST","/api/authenticate",json.writeValueAsString(login)));
        tokens.put("setup",temporary.path("access_token").asText());
        assertThat(call("setup","PUT","/api/users/complete-initial-password",json.writeValueAsString(Map.of("nouveau","Personal-test-Password","confirmer","Personal-test-Password"))).statusCode()).isEqualTo(204);
        return status(200,call(null,"POST","/api/authenticate",json.writeValueAsString(Map.of("username",created.path("username").asText(),"password","Personal-test-Password")))).path("access_token").asText();
    }
    private JsonNode create(String kind, String name, String pole) throws Exception {
        return status(201, call("admin", "POST", "/api/direction-generale/" + kind, body(name, pole)));
    }
    private Map<String, Object> identity(String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("nom", name); result.put("prenom", "Test"); result.put("email", name + "@example.test");
        result.put("telephone", "phone-" + name); result.put("sexe", "MASCULIN"); return result;
    }
    private String body(String name, String pole) throws Exception {
        Map<String, Object> result = identity(name); if (pole != null) result.put("pole", pole); return json.writeValueAsString(result);
    }
    private Map<String, Object> assignment(String user, String pole) {
        return new HashMap<>(Map.of("idDga", user, "pole", pole, "dateDebut", LocalDate.now().toString(), "actif", true));
    }
    private List<String> roleCodes(JsonNode user) {
        List<String> result = new ArrayList<>(); user.path("roles").forEach(role -> result.add(role.path("code").asText())); return result;
    }
    private String token(String user) {
        return app.getBean(TokenProvider.class).createToken(new UsernamePasswordAuthenticationToken(user, "",
                List.of(new SimpleGrantedAuthority("BA_CONNECT"))), false).getToken();
    }
    private HttpResponse<String> call(String actor, String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json").header("Accept", "application/json");
        if (actor != null) request.header("Authorization", "Bearer " + tokens.get(actor));
        return http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode status(int expected, HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).withFailMessage("HTTP %s attendu, obtenu %s : %s", expected, response.statusCode(), response.body()).isEqualTo(expected);
        return json.readTree(response.body());
    }
    private void execute(String command) throws Exception { try (Statement statement = sql.createStatement()) { statement.execute(command); } }
    private long count(String command) throws Exception { try (Statement statement = sql.createStatement(); ResultSet rows = statement.executeQuery(command)) { rows.next(); return rows.getLong(1); } }
    private String url() { return System.getenv("BSIC_MIGRATION_TEST_URL"); }
    private String username() { return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_USERNAME", "postgres"); }
    private String password() { return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_PASSWORD", ""); }
}

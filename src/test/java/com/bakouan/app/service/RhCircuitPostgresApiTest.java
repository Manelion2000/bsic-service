package com.bakouan.app.service;

import com.bakouan.app.BsicServiceApplication;
import com.bakouan.app.security.jwt.TokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

/** Vraie API securisee et vrais verrous PostgreSQL, dans un schema jetable. */
@EnabledIfEnvironmentVariable(named="BSIC_MIGRATION_TEST_URL",matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RhCircuitPostgresApiTest {
    @TestConfiguration(proxyBeanMethods=false)
    public static class IsolatedMailConfiguration {
        @Bean @Primary BaMailService isolatedMailService() {return org.mockito.Mockito.mock(BaMailService.class);}
    }
    private String schema, base;
    private Connection sql;
    private ConfigurableApplicationContext app;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String,String> tokens = new HashMap<>();

    @BeforeAll void start() throws Exception {
        schema="bsic_rh_test_"+UUID.randomUUID().toString().replace("-","");
        sql=DriverManager.getConnection(url(),username(),password());
        execute("CREATE SCHEMA "+schema); execute("SET search_path TO "+schema);
        app=startApplication(true,"DEFINITIF");
        base=address(app);
        execute("INSERT INTO ba_departement (id,nom,created_by,created_date) VALUES ('dept','Departement','test',now()),('foreign','Autre departement','test',now())");
        execute("INSERT INTO ba_service (id,nom,departement_id,created_by,created_date) VALUES ('service-1','Service 1','dept','test',now()),('service-2','Service 2','dept','test',now())");
        fixture("agent","AGENT","dept","service-1"); fixture("chief","CHEF_SERVICE","dept","service-1");
        fixture("peer","CHEF_SERVICE","dept","service-2"); fixture("foreign-chief","CHEF_SERVICE","foreign",null);
        fixture("director","DIRECTEUR","dept",null); fixture("ops","DIRECTEUR_GENERAL_ADJOINT",null,null);
        fixture("dev","DIRECTEUR_GENERAL_ADJOINT",null,null); fixture("dg","DIRECTEUR_GENERAL",null,null);
        fixture("admin",null,null,null);
        fixture("rh",null,null,null);
        execute("INSERT INTO ba_user_roles(user_id,role_id) SELECT 'rh',id FROM ba_role WHERE code='RH_CONSULTATION_ABSENCES'");
        execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('admin','role-ba-admin')");
        execute("INSERT INTO ba_dga_pole_validateur(id,pole,dga_id,date_debut,actif,created_by,created_date) VALUES ('ops-assignment','OPERATIONS','ops',CURRENT_DATE-1,true,'test',now()),('dev-assignment','DEVELOPPEMENT','dev',CURRENT_DATE-1,true,'test',now())");
        for(String id:List.of("agent","chief","peer","foreign-chief","director","ops","dev","dg","admin","rh"))
            tokens.put(id,token(app,id));
    }

    @BeforeEach void reset() throws Exception {
        execute("TRUNCATE ba_acte_signature,ba_reprise_service,ba_autorisation_absence,ba_autorisation_sortie,ba_rh_circuit CASCADE");
        for(String id:tokens.keySet()) SignatureTestImages.seed(sql,id);
        execute("UPDATE ba_utilisateur SET account_locked=false,activated=true");
        execute("UPDATE ba_dga_pole_validateur SET actif=true");
    }

    @AfterAll void cleanup() throws Exception {
        if(app!=null) app.close();
        if(sql!=null) try {if(schema!=null&&schema.matches("bsic_rh_test_[0-9a-f]{32}")) execute("DROP SCHEMA "+schema+" CASCADE");} finally {sql.close();}
    }

    @Test void privateUploadRequiredBeforeApprovalAndArchivedSignatureSurvivesReplacementAndRemoval() throws Exception {
        String ownerDigest = assertStatus(200, call(base, "agent", "GET", "/api/me/signature", null)).path("empreinte").asText();
        execute("DELETE FROM ba_user_signature WHERE user_id='chief'");
        JsonNode request = create("agent", false);
        String id = request.path("id").asText();
        assertStatus(409, decide("chief", id, 0, true, false));
        assertThat(count("SELECT count(*) FROM ba_acte_signature")).isZero();
        assertThat(assertStatus(200, call(base, "agent", "GET", "/api/autorisations-absence/mes-demandes", null)).get(0).at("/circuit/etapeCourante").asInt()).isZero();
        assertStatus(404, call(base, "chief", "GET", "/api/me/signature/image", null));
        var preview=http.send(signatureRequest("chief",0,true),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(preview.statusCode()).isEqualTo(200);assertThat(preview.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(preview.body()).startsWith(new byte[]{(byte)137,80,78,71,13,10,26,10});
        assertThat(assertStatus(200,call(base,"chief","GET","/api/me/signature",null)).path("presente").asBoolean()).isFalse();
        assertStatus(200, uploadSignature("chief", 0));
        assertThat(assertStatus(200, call(base, "agent", "GET", "/api/me/signature", null)).path("empreinte").asText()).isEqualTo(ownerDigest);
        assertStatus(200, decide("chief", id, 0, true, false));
        byte[] archived;
        try (var p = sql.prepareStatement("SELECT contenu FROM ba_acte_signature WHERE signataire_id='chief'"); var rows = p.executeQuery()) { assertThat(rows.next()).isTrue(); archived = rows.getBytes(1); }
        assertStatus(200, uploadSignature("chief", 18));
        assertStatus(204, call(base, "chief", "DELETE", "/api/me/signature", null));
        assertThat(assertStatus(200, call(base, "chief", "GET", "/api/me/signature", null)).path("presente").asBoolean()).isFalse();
        try (var p = sql.prepareStatement("SELECT contenu FROM ba_acte_signature WHERE signataire_id='chief'"); var rows = p.executeQuery()) { assertThat(rows.next()).isTrue(); assertThat(rows.getBytes(1)).isEqualTo(archived); }
        assertStatus(200, decide("director", id, 1, true, false)); assertStatus(200, decide("ops", id, 2, true, false)); assertStatus(200, decide("dg", id, 3, true, false));
        var pdf = http.send(HttpRequest.newBuilder(URI.create(base + "/api/reporting/autorisations-absence/" + id)).header("Authorization", "Bearer " + tokens.get("agent")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(pdf.statusCode()).isEqualTo(200); assertThat(pdf.headers().firstValue("Cache-Control").orElse("")).contains("no-store"); assertThat(pdf.body()).startsWith("%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf.body())) { assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc)).contains("chief", "Signature du validateur"); }
        assertStatus(403, call(base, "foreign-chief", "GET", "/api/reporting/autorisations-absence/" + id, null));
        var anonymous = http.send(HttpRequest.newBuilder(URI.create(base + "/api/me/signature/image")).GET().build(), HttpResponse.BodyHandlers.ofString()); assertThat(anonymous.statusCode()).isEqualTo(401);
        for (String path : List.of("/baUserSignatures", "/baActeSignatures")) {
            var direct = http.send(HttpRequest.newBuilder(URI.create(base + path)).header("Authorization", "Bearer " + tokens.get("admin")).GET().build(), HttpResponse.BodyHandlers.ofString()); assertThat(direct.statusCode()).isEqualTo(404);
        }
    }

    @Test void repriseApprovalIsAtomicAndConcurrentValidatorsProduceOneArchivedVisa() throws Exception {
        String body = json.writeValueAsString(Map.of("motifAbsence", "Congé", "dateDebutConge", LocalDate.now().minusDays(4).toString(), "dateFinConge", LocalDate.now().minusDays(1).toString(), "dateReprise", LocalDate.now().toString()));
        String id = assertStatus(201, call(base, "agent", "POST", "/api/reprises-service", body)).path("id").asText();
        execute("DELETE FROM ba_user_signature WHERE user_id='chief'");
        assertStatus(409, call(base, "chief", "PUT", "/api/reprises-service/" + id + "/valider", null));
        assertThat(assertStatus(200, call(base, "agent", "GET", "/api/reprises-service/" + id, null)).path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
        assertThat(count("SELECT count(*) FROM ba_acte_signature")).isZero();
        SignatureTestImages.seed(sql, "chief");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> call(base, "chief", "PUT", "/api/reprises-service/" + id + "/valider", null));
            var second = executor.submit(() -> call(base, "director", "PUT", "/api/reprises-service/" + id + "/valider", null));
            assertThat(List.of(first.get(15, TimeUnit.SECONDS).statusCode(), second.get(15, TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200, 400);
        } finally { executor.shutdownNow(); }
        assertThat(count("SELECT count(*) FROM ba_acte_signature WHERE domaine='REPRISE' AND reference_id='" + id + "'")).isEqualTo(1);
        var pdf = http.send(HttpRequest.newBuilder(URI.create(base + "/api/reporting/reprises-service/" + id + "/attestation")).header("Authorization", "Bearer " + tokens.get("agent")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(pdf.statusCode()).isEqualTo(200);
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf.body())) { assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc)).contains("ATTESTATION DE REPRISE DE SERVICE", "Signature du validateur"); }
        assertStatus(403, call(base, "foreign-chief", "GET", "/api/reporting/reprises-service/" + id + "/attestation", null));
    }

    private HttpResponse<String> uploadSignature(String actor, int variant) throws Exception {
        return http.send(signatureRequest(actor,variant,false),HttpResponse.BodyHandlers.ofString());
    }
    private HttpRequest signatureRequest(String actor, int variant, boolean preview) throws Exception {
        String boundary = "bsic-signature-test-boundary";
        var bytes = new java.io.ByteArrayOutputStream();
        bytes.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"userId\"\r\n\r\ndirector\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"signature.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        bytes.write(SignatureTestImages.png(variant)); bytes.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(URI.create(base + "/api/me/signature" + (preview ? "/preview" : ""))).header("Authorization", "Bearer " + tokens.get(actor)).header("Content-Type", "multipart/form-data; boundary=" + boundary).method(preview ? "POST" : "PUT", HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
    }

    @Test void missingDgBlocksBothApisUntilAnAdministratorConfiguresAnAvailableDg() throws Exception {
        execute("UPDATE ba_utilisateur SET activated=false WHERE id='dg'");
        for(String code:List.of("ABSENCE","SORTIE")){
            var preview=assertStatus(200,call(base,"agent","GET","/api/rh/circuit?demarche="+code,null));
            assertThat(preview.path("dgObligatoire").asBoolean()).isTrue();assertThat(preview.path("etapeBloquee").asText()).isEqualTo("DG");assertThat(preview.path("etapes").size()).isEqualTo(4);
            assertThat(preview.path("blocage").asText()).contains("Gestion du DG");assertStatus(409,post("agent",code.equals("SORTIE")));
        }
        assertThat(count("SELECT count(*) FROM ba_rh_circuit")).isZero();
        assertThat(count("SELECT count(*) FROM ba_autorisation_absence")).isZero();assertThat(count("SELECT count(*) FROM ba_autorisation_sortie")).isZero();
        assertStatus(200,call(base,"admin","PUT","/api/users/dg/activer",null));
        for(String code:List.of("ABSENCE","SORTIE")){
            var preview=assertStatus(200,call(base,"agent","GET","/api/rh/circuit?demarche="+code,null));assertThat(preview.path("blocage").isNull()).isTrue();assertThat(preview.path("dgObligatoire").asBoolean()).isTrue();
        }
        var absence=create("agent",false);assertThat(absence.at("/circuit/dgObligatoire").asBoolean()).isTrue();
        var sortie=assertStatus(201,call(base,"agent","POST","/api/autorisation-sorties",json.writeValueAsString(Map.of("motif","Sortie","dateSortie",LocalDate.now().plusDays(3).toString(),"typeStructure","DIRECTION","idDirection","dept"))));
        assertThat(sortie.at("/circuit/dgObligatoire").asBoolean()).isTrue();
    }

    @Test void defaultCircuitDoesNotNeedDgAndFinalizesOnlyAfterOneAvailableDga() throws Exception {
        execute("UPDATE ba_utilisateur SET activated=false WHERE id IN ('dg','dev')");
        try(var defaults=startApplication(null,"DEFINITIF")) {
            String address=address(defaults);
            for(boolean sortie:List.of(false,true)) {
                String prefix=sortie?"/api/autorisation-sorties":"/api/autorisations-absence";
                var preview=assertStatus(200,call(address,"agent","GET","/api/rh/circuit?demarche="+(sortie?"SORTIE":"ABSENCE"),null));
                assertThat(preview.path("dgObligatoire").asBoolean()).isFalse();assertThat(preview.path("blocage").isNull()).isTrue();
                assertThat(preview.path("etapes").findValuesAsText("type")).containsExactly("CHEF_SERVICE","DIRECTEUR","DGA");
                String payload=sortie?json.writeValueAsString(Map.of("motif","Sortie","dateSortie",LocalDate.now().plusDays(3).toString(),"typeStructure","DIRECTION","idDirection","dept","dgObligatoire",true)):absenceBody();
                var request=assertStatus(201,call(address,"agent","POST",prefix,payload));String id=request.path("id").asText();
                assertThat(request.at("/circuit/dgObligatoire").asBoolean()).isFalse();
                assertStatus(403,call(address,"ops","PUT",prefix+"/"+id+"/valider?etape=0",null));
                assertStatus(403,call(address,"agent","PUT",prefix+"/"+id+"/valider?etape=0",null));
                var after=assertStatus(200,call(address,"peer","PUT",prefix+"/"+id+"/valider?etape=0",null));assertThat(after.path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
                after=assertStatus(200,call(address,"director","PUT",prefix+"/"+id+"/valider?etape=1",null));assertThat(after.path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
                assertThat(after.at("/circuit/etapes/2/validateurs").size()).isEqualTo(1);
                after=assertStatus(200,call(address,"ops","PUT",prefix+"/"+id+"/valider?etape=2",null));assertThat(after.path("statutValidation").asText()).isEqualTo("VALIDEE");
                assertThat(after.at("/circuit/historique").size()).isEqualTo(3);assertStatus(409,call(address,"ops","PUT",prefix+"/"+id+"/valider?etape=2",null));
            }
            execute("UPDATE ba_utilisateur SET activated=false WHERE id='ops'");
            for(boolean sortie:List.of(false,true)){
                String prefix=sortie?"/api/autorisation-sorties":"/api/autorisations-absence";
                String payload=sortie?json.writeValueAsString(Map.of("motif","Sans DGA","dateSortie",LocalDate.now().plusDays(8).toString(),"typeStructure","DIRECTION","idDirection","dept")):json.writeValueAsString(Map.of("motif","Sans DGA","dateDebut",LocalDate.now().plusDays(8).toString(),"dateFin",LocalDate.now().plusDays(8).toString()));
                assertStatus(409,call(address,"agent","POST",prefix,payload));
            }
        }
    }

    @Test void otherServiceChiefCanApproveAbsenceButForeignChiefAndDirectorCannotSkip() throws Exception {
        JsonNode request=create("agent",false);
        String id=request.path("id").asText();
        assertStatus(403,decide("agent",id,0,true,false));
        assertStatus(403,decide("foreign-chief",id,0,true,false));
        assertStatus(403,decide("director",id,0,true,false));
        assertStatus(403,decide("admin",id,0,true,false));
        JsonNode after=assertStatus(200,decide("peer",id,0,true,false));
        assertThat(after.path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
        assertThat(after.at("/circuit/etapeCourante").asInt()).isEqualTo(1);
        assertThat(after.at("/circuit/etapes/0/idSignataire").asText()).isEqualTo("peer");
        assertStatus(409,decide("chief",id,0,true,false));
        assertThat(count("SELECT jsonb_array_length(historique) FROM ba_rh_circuit")).isEqualTo(1);
    }

    @Test void twoChiefsSigningSimultaneouslyCauseExactlyOneTransition() throws Exception {
        String id=create("agent",false).path("id").asText();
        var ready=new CountDownLatch(2); var go=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(() -> {ready.countDown();go.await();return decide("chief",id,0,true,false).statusCode();});
            var b=executor.submit(() -> {ready.countDown();go.await();return decide("peer",id,0,true,false).statusCode();});
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        } finally {executor.shutdownNow();}
        assertThat(count("SELECT etape_courante FROM ba_rh_circuit")).isEqualTo(1);
        assertThat(count("SELECT jsonb_array_length(historique) FROM ba_rh_circuit")).isEqualTo(1);
    }

    @Test void simultaneousOverlappingSubmissionsCannotCreateTwoRequests() throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> submit=()->{ready.countDown();go.await();return post("agent",false).statusCode();};
            var a=executor.submit(submit);var b=executor.submit(submit);
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,400);
        } finally {executor.shutdownNow();}
        assertThat(count("SELECT count(*) FROM ba_autorisation_absence")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM ba_rh_circuit")).isEqualTo(1);
    }

    @Test void chiefsAndDirectorsStartAtTheirActualFirstStage() throws Exception {
        assertThat(create("chief",false).at("/circuit/etapes/0/type").asText()).isEqualTo("DIRECTEUR");
        assertThat(create("director",false).at("/circuit/etapes/0/type").asText()).isEqualTo("DGA");
    }

    @Test void oneDgaSufficesForAgentChiefAndDirectorAndDgIsMandatory() throws Exception {
        for(String requester:List.of("agent","chief","director")) {
            String id=create(requester,false).path("id").asText(); int step=0;
            if(requester.equals("agent")) assertStatus(200,decide("peer",id,step++,true,false));
            if(!requester.equals("director")) assertStatus(200,decide("director",id,step++,true,false));
            JsonNode pending=assertStatus(200,decide("ops",id,step,true,false));
            assertThat(pending.path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
            assertStatus(409,decide("dev",id,step,true,false));
            JsonNode done=assertStatus(200,decide("dg",id,step+1,true,false));
            assertThat(done.path("statutValidation").asText()).isEqualTo("VALIDEE");
        }
    }

    @Test void eachDgaRequiresOnlyTheOtherDgaThenDgAndNeverSelfApproval() throws Exception {
        for(String requester:List.of("ops","dev")) {
            String id=create(requester,false).path("id").asText();
            assertStatus(403,decide(requester,id,0,true,false));
            assertStatus(403,decide("dg",id,0,true,false));
            assertStatus(403,decide("director",id,0,true,false));
            assertStatus(200,decide(requester.equals("ops")?"dev":"ops",id,0,true,false));
            assertThat(assertStatus(200,decide("dg",id,1,true,false)).path("statutValidation").asText()).isEqualTo("VALIDEE");
        }
    }

    @Test void unavailableOtherDgaAndDgRequestsAreBlocked() throws Exception {
        execute("UPDATE ba_utilisateur SET account_locked=true WHERE id='dev'");
        assertStatus(409,post("ops",false));
        execute("UPDATE ba_utilisateur SET account_locked=false WHERE id='dev'");
        execute("UPDATE ba_dga_pole_validateur SET actif=false WHERE id='dev-assignment'");
        assertStatus(409,post("ops",false)); assertStatus(409,post("dg",false));
        assertStatus(409,post("admin",false));
    }

    @Test void sortieUsesTheSameCircuitAndClientCannotChooseValidatorsOrDisableDg() throws Exception {
        String id=create("agent",true).path("id").asText();
        assertStatus(403,decide("foreign-chief",id,0,true,true));
        assertStatus(200,decide("peer",id,0,true,true)); assertStatus(200,decide("director",id,1,true,true));
        assertThat(assertStatus(200,decide("dev",id,2,true,true)).path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
        assertThat(assertStatus(200,decide("dg",id,3,true,true)).path("statutValidation").asText()).isEqualTo("VALIDEE");
    }

    @Test void inactiveFrozenValidatorCannotSignAndUnavailableStageRemainsBlocked() throws Exception {
        String id=create("ops",false).path("id").asText();
        execute("UPDATE ba_utilisateur SET activated=false WHERE id='dev'");
        assertThat(decide("dev",id,0,true,false).statusCode()).isIn(401,403);
        JsonNode mine=assertStatus(200,call(base,"ops","GET","/api/autorisations-absence/mes-demandes",null)).get(0);
        assertThat(mine.at("/circuit/blocage").asText()).contains("bloquee");
        assertThat(count("SELECT etape_courante FROM ba_rh_circuit")).isZero();
    }

    @Test void staleAndMissingStageParametersCannotAdvanceAndAnonymousApiIsDenied() throws Exception {
        String id=create("agent",false).path("id").asText();
        assertStatus(409,decide("peer",id,2,true,false));
        assertStatus(400,call(base,"peer","PUT","/api/autorisations-absence/"+id+"/valider",null));
        assertThat(http.send(HttpRequest.newBuilder(URI.create(base+"/api/autorisations-absence/"+id+"/valider?etape=0")).PUT(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test void disabledDgFinalizesLastMandatoryStageButDoesNotAlterFrozenOlderRequests() throws Exception {
        String old=create("director",false).path("id").asText();
        try(ConfigurableApplicationContext other=startApplication(false,"DEFINITIF")) {
            String otherBase=address(other);
            JsonNode created=assertStatus(201,call(otherBase,"ops","POST","/api/autorisations-absence",absenceBody()));
            assertThat(created.at("/circuit/etapes").size()).isEqualTo(1);
            String id=created.path("id").asText();
            assertThat(assertStatus(200,call(otherBase,"dev","PUT","/api/autorisations-absence/"+id+"/valider?etape=0",null)).path("statutValidation").asText()).isEqualTo("VALIDEE");
            assertThat(assertStatus(200,call(otherBase,"dev","PUT","/api/autorisations-absence/"+old+"/valider?etape=0",null)).path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
        }
    }

    @Test void twoDgasAtFinalStageCannotFinalizeTwice() throws Exception {
        try(ConfigurableApplicationContext other=startApplication(false,"DEFINITIF")) {
            String address=address(other);
            String id=assertStatus(201,call(address,"director","POST","/api/autorisations-absence",absenceBody())).path("id").asText();
            var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
            try {
                var a=executor.submit(()->{ready.countDown();go.await();return call(address,"ops","PUT","/api/autorisations-absence/"+id+"/valider?etape=0",null).statusCode();});
                var b=executor.submit(()->{ready.countDown();go.await();return call(address,"dev","PUT","/api/autorisations-absence/"+id+"/valider?etape=0",null).statusCode();});
                assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
                assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
            } finally {executor.shutdownNow();}
            assertThat(count("SELECT jsonb_array_length(historique) FROM ba_rh_circuit")).isEqualTo(1);
            assertThat(count("SELECT count(*) FROM ba_autorisation_absence WHERE statut_validation='VALIDEE'")).isEqualTo(1);
        }
    }

    @Test void alternativeRefusalIsPersistedAndAnotherChiefCanCloseTheStage() throws Exception {
        try(ConfigurableApplicationContext other=startApplication(true,"AUTRES_VALIDATEURS")) {
            String address=address(other);
            String id=assertStatus(201,call(address,"agent","POST","/api/autorisations-absence",absenceBody())).path("id").asText();
            assertThat(assertStatus(200,call(address,"chief","PUT","/api/autorisations-absence/"+id+"/rejeter?etape=0&commentaire=Motif",null)).path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
            assertThat(count("SELECT jsonb_array_length(historique) FROM ba_rh_circuit")).isEqualTo(1);
            assertStatus(403,call(address,"chief","PUT","/api/autorisations-absence/"+id+"/valider?etape=0",null));
            JsonNode after=assertStatus(200,call(address,"peer","PUT","/api/autorisations-absence/"+id+"/valider?etape=0",null));
            assertThat(after.at("/circuit/etapeCourante").asInt()).isEqualTo(1);
            assertThat(after.at("/circuit/etapes/0/idSignataire").asText()).isEqualTo("peer");
            assertThat(count("SELECT jsonb_array_length(historique) FROM ba_rh_circuit")).isEqualTo(2);
        }
    }

    @Test void unconfiguredRefusalPolicyBlocksApprovalAsWellAsRefusal() throws Exception {
        try(ConfigurableApplicationContext other=startApplication(true,"NON_CONFIGURE")) {
            String address=address(other);
            String id=assertStatus(201,call(address,"agent","POST","/api/autorisations-absence",absenceBody())).path("id").asText();
            assertStatus(409,call(address,"chief","PUT","/api/autorisations-absence/"+id+"/valider?etape=0",null));
            assertStatus(409,call(address,"peer","PUT","/api/autorisations-absence/"+id+"/rejeter?etape=0&commentaire=Motif",null));
            assertThat(count("SELECT jsonb_array_length(historique) FROM ba_rh_circuit")).isZero();
        }
    }

    @Test void adminCanCreateDepartmentServiceThenAgentWithoutManagersAndUpdateTheAccount() throws Exception {
        JsonNode department=assertStatus(201,call(base,"admin","POST","/api/annuaires/departements",json.writeValueAsString(Map.of("code","ACCOUNT-INIT","nom","Nouveau departement"))));
        JsonNode service=assertStatus(201,call(base,"admin","POST","/api/annuaires/services",json.writeValueAsString(Map.of("code","ACCOUNT-INIT-S","nom","Nouveau service","idDepartement",department.path("id").asText()))));
        String body=json.writeValueAsString(Map.of("nom","Creation","prenom","Agent","email","account-init@example.test","telephone","account-init-phone",
                "sexe","MASCULIN","fonction","AGENT","idDepartement",department.path("id").asText(),"idService",service.path("id").asText()));
        assertStatus(201,call(base,"admin","POST","/api/users",body));
        JsonNode created=null;
        for(JsonNode user:assertStatus(200,call(base,"admin","GET","/api/users",null)))
            if("account-init@example.test".equals(user.path("email").asText())) created=user;
        assertThat(created).isNotNull();
        assertThat(created.path("id").asText()).isNotBlank();
        assertThat(created.path("idSuperieur").isNull()||created.path("idSuperieur").isMissingNode()).isTrue();
        assertThat(created.path("idSuperieurSecondaire").isNull()||created.path("idSuperieurSecondaire").isMissingNode()).isTrue();
        assertStatus(200,call(base,"admin","PUT","/api/users/"+created.path("id").asText(),body));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE email='account-init@example.test' AND superieur_id IS NULL AND superieur_secondaire_id IS NULL AND activated=true")).isEqualTo(1);
    }

    @Test void directUserCreationApiStillEnforcesManagerPermissionsAndServiceDepartmentConsistency() throws Exception {
        String valid=json.writeValueAsString(Map.of("nom","Creation","prenom","Agent","email","account-api@example.test","telephone","account-api-phone",
                "sexe","MASCULIN","fonction","AGENT","idDepartement","dept","idService","service-1"));
        assertStatus(403,call(base,"agent","POST","/api/users",valid));
        String invalid=json.writeValueAsString(Map.of("nom","Creation","prenom","Agent","email","account-api@example.test","telephone","account-api-phone",
                "sexe","MASCULIN","fonction","AGENT","idDepartement","foreign","idService","service-1"));
        assertStatus(400,call(base,"admin","POST","/api/users",invalid));
        assertThat(count("SELECT count(*) FROM ba_utilisateur WHERE email='account-api@example.test'")).isZero();
    }

    @Test void rhDashboardCountsClippedDaysAndFiltersRealPostgresRows() throws Exception {
        dashboardFixture();
        JsonNode dashboard=assertStatus(200,call(base,"rh","GET","/api/rh-dashboard?debut=2026-01-01&fin=2026-02-28",null));
        assertThat(dashboard.path("absencesTotal").asInt()).isEqualTo(5);
        assertThat(dashboard.path("joursAbsenceValides").asInt()).isEqualTo(8);
        assertThat(dashboard.path("joursAbsenceEnAttente").asInt()).isEqualTo(2);
        assertThat(dashboard.at("/tendances/0/joursAbsence").asInt()).isEqualTo(5);
        assertThat(dashboard.at("/tendances/1/joursAbsence").asInt()).isEqualTo(3);
        assertThat(dashboard.path("parAgent").findValues("jours").stream().mapToInt(JsonNode::asInt).sum()).isEqualTo(8);
        assertThat(dashboard.path("registre").size()).isEqualTo(5);
        JsonNode team=assertStatus(200,call(base,"rh","GET","/api/rh-dashboard?debut=2026-01-01&fin=2026-02-28&idDepartement=dept&idService=service-1",null));
        assertThat(team.path("joursAbsenceValides").asInt()).isEqualTo(7);
        assertThat(team.path("absencesTotal").asInt()).isEqualTo(3);
        assertThat(team.path("services").size()).isEqualTo(2);
    }

    @Test void rhDashboardExportMatchesTheRegisterAndRejectsUnauthorizedReaders() throws Exception {
        dashboardFixture();
        assertStatus(403,call(base,"agent","GET","/api/rh-dashboard",null));
        assertStatus(403,call(base,"chief","GET","/api/rh-dashboard/export?type=ABSENCES",null));
        assertThat(call(base,"agent","GET","/api/rh-dashboard/export?type=AGENTS",null).statusCode()).isEqualTo(403);
        HttpResponse<String> csv=call(base,"rh","GET","/api/rh-dashboard/export?type=ABSENCES&debut=2026-01-01&fin=2026-02-28&idDepartement=dept&statut=VALIDEE&recherche=agent",null);
        assertThat(csv.statusCode()).isEqualTo(200);
        assertThat(csv.headers().firstValue("Content-Type").orElse("")).contains("text/csv");
        assertThat(csv.body().lines()).hasSize(3);
        assertThat(csv.body()).startsWith("\uFEFFType;").contains("VALIDEE").doesNotContain("EN_ATTENTE","foreign-chief");
        assertStatus(400,call(base,"rh","GET","/api/rh-dashboard?debut=2026-02-28&fin=2026-01-01",null));
        assertStatus(400,call(base,"rh","GET","/api/rh-dashboard/export?type=UNKNOWN",null));
    }

    private void dashboardFixture() throws Exception {
        execute("INSERT INTO ba_autorisation_absence(id,motif,date_debut,date_fin,nombre_jours,demandeur_id,statut_validation,created_by,created_date,statut) VALUES "
                +"('report-1','Test','2025-12-30','2026-01-03',5,'agent','VALIDEE','test',now(),'A'),"
                +"('report-2','Test','2026-01-30','2026-02-02',4,'agent','VALIDEE','test',now(),'A'),"
                +"('report-3','Test','2026-02-10','2026-02-11',2,'peer','EN_ATTENTE','test',now(),'A'),"
                +"('report-4','Test','2026-01-20','2026-01-20',1,'agent','REJETEE','test',now(),'A'),"
                +"('report-5','Test','2026-02-01','2026-02-01',1,'foreign-chief','VALIDEE','test',now(),'A')");
    }

    private ConfigurableApplicationContext startApplication(Boolean dg,String refusal) {
        Map<String,Object> p=new HashMap<>();
        p.put("server.port",0); p.put("server.address","127.0.0.1");
        p.put("spring.datasource.url",url()+(url().contains("?")?"&":"?")+"currentSchema="+schema);
        p.put("spring.datasource.username",username()); p.put("spring.datasource.password",password());
        p.put("spring.flyway.enabled",true); p.put("spring.flyway.locations","classpath:db/migration");
        p.put("spring.flyway.schemas",schema); p.put("spring.flyway.default-schema",schema);
        p.put("spring.jpa.properties.hibernate.default_schema",schema); p.put("spring.jpa.hibernate.ddl-auto","validate");
        p.put("spring.jpa.show-sql",false); p.put("spring.jpa.properties.hibernate.show_sql",false);
        if(dg!=null)p.put("app.rh.validation.dg-enabled",dg); p.put("app.rh.validation.refusal-policy",refusal);
        p.put("app.security.jwt.base64-secret",Base64.getEncoder().encodeToString(new byte[64]));
        p.put("logging.level.root","WARN"); p.put("logging.level.com.bakouan.app.config.BaRestExceptionHandler","OFF");
        return new SpringApplicationBuilder(BsicServiceApplication.class,IsolatedMailConfiguration.class).profiles("prod")
                .initializers(c -> {c.getEnvironment().setActiveProfiles("prod"); c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("rh-test",p));}).run("--spring.profiles.active=prod", "--logging.level.root=WARN", "--logging.level.com.bakouan.app.config.BaRestExceptionHandler=OFF");
    }
    private String address(ConfigurableApplicationContext c) {return "http://127.0.0.1:"+((ServletWebServerApplicationContext)c).getWebServer().getPort();}
    private String token(ConfigurableApplicationContext c,String user) {
        return c.getBean(TokenProvider.class).createToken(new UsernamePasswordAuthenticationToken(user,"",List.of(new SimpleGrantedAuthority("BA_CONNECT"))),false).getToken();
    }
    private void fixture(String id,String function,String department,String service) throws Exception {
        try(PreparedStatement p=sql.prepareStatement("INSERT INTO ba_utilisateur(id,username,password_hash,nom,telephone,fonction,departement_id,service_id,activated,account_locked,created_by,created_date) VALUES (?,?,? ,?,?,?,?,?,true,false,'test',now())")) {
            p.setString(1,id);p.setString(2,id);p.setString(3,"test-password-unused");p.setString(4,id);p.setString(5,id);p.setString(6,function);p.setString(7,department);p.setString(8,service);p.executeUpdate();
        }
        SignatureTestImages.seed(sql,id);
        execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('"+id+"','role-ba-connect')");
    }
    private JsonNode create(String actor,boolean sortie) throws Exception {return assertStatus(201,post(actor,sortie));}
    private HttpResponse<String> post(String actor,boolean sortie) throws Exception {
        String body=sortie?json.writeValueAsString(Map.of("motif","Test","dateSortie",LocalDate.now().plusDays(2).toString(),"typeStructure","DIRECTION","idDirection","dept","dgObligatoire",false,"idValidateur","foreign-chief")):absenceBody();
        return call(base,actor,"POST",sortie?"/api/autorisation-sorties":"/api/autorisations-absence",body);
    }
    private String absenceBody() throws Exception {String date=LocalDate.now().plusDays(2).toString();return json.writeValueAsString(Map.of("motif","Test","dateDebut",date,"dateFin",date,"dgObligatoire",false,"idValidateur","foreign-chief"));}
    private HttpResponse<String> decide(String actor,String id,int step,boolean approve,boolean sortie) throws Exception {
        return call(base,actor,"PUT",(sortie?"/api/autorisation-sorties/":"/api/autorisations-absence/")+id+(approve?"/valider":"/rejeter")+"?etape="+step+"&commentaire=Test",null);
    }
    private HttpResponse<String> call(String address,String actor,String method,String path,String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(address+path)).header("Authorization","Bearer "+tokens.get(actor)).header("Content-Type","application/json").header("Accept","application/json")
                .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode assertStatus(int status,HttpResponse<String> response) throws Exception {assertThat(response.statusCode()).withFailMessage("HTTP %s attendu, obtenu %s : %s",status,response.statusCode(),response.body()).isEqualTo(status);return json.readTree(response.body());}
    private void execute(String command) throws Exception {try(Statement statement=sql.createStatement()){statement.execute(command);}}
    private long count(String command) throws Exception {try(Statement statement=sql.createStatement();ResultSet rows=statement.executeQuery(command)){rows.next();return rows.getLong(1);}}
    private String url(){return System.getenv("BSIC_MIGRATION_TEST_URL");}
    private String username(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_USERNAME","postgres");}
    private String password(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_PASSWORD","");}
}


package com.bakouan.app.service;
import com.bakouan.app.BsicServiceApplication;import com.bakouan.app.security.jwt.TokenProvider;import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;import org.springframework.boot.builder.SpringApplicationBuilder;import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;import org.springframework.context.ConfigurableApplicationContext;import org.springframework.core.env.MapPropertySource;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;import java.net.*;import java.net.http.*;import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="BSIC_MIGRATION_TEST_URL",matches=".+") @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessusPostgresApiTest {
 private String schema,base;private Connection sql;private ConfigurableApplicationContext app;private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();private final HttpClient http=HttpClient.newHttpClient();private final Map<String,String> tokens=new HashMap<>();
     @BeforeAll void start()throws Exception{
        schema="bsic_processus_test_"+UUID.randomUUID().toString().replace("-","");sql=DriverManager.getConnection(url(),user(),password());execute("CREATE SCHEMA "+schema);execute("SET search_path TO "+schema);
        Map<String,Object> props=new HashMap<>();props.put("server.port",0);props.put("server.address","127.0.0.1");props.put("spring.datasource.url",url()+(url().contains("?")?"&":"?")+"currentSchema="+schema);props.put("spring.datasource.username",user());props.put("spring.datasource.password",password());props.put("spring.flyway.schemas",schema);props.put("spring.flyway.default-schema",schema);props.put("spring.jpa.properties.hibernate.default_schema",schema);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.jpa.show-sql",false);props.put("spring.jpa.properties.hibernate.show_sql",false);props.put("app.features.habilitations-enabled",false);props.put("app.conges.traitements-automatiques-enabled",false);props.put("app.conges.notifications-mail-enabled",false);props.put("app.security.jwt.base64-secret",Base64.getEncoder().encodeToString(new byte[64]));props.put("logging.level.root","WARN");props.put("logging.level.com.bakouan.app.config.BaRestExceptionHandler","OFF");
        props.put("spring.mail.username","noreply@example.test");
        app=new SpringApplicationBuilder(BsicServiceApplication.class,RhCircuitPostgresApiTest.IsolatedMailConfiguration.class,CongePostgresApiTest.TestMail.class).profiles("prod").initializers(c->{c.getEnvironment().setActiveProfiles("prod");c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("conges-test",props));}).run("--spring.profiles.active=prod","--logging.level.root=WARN");base="http://127.0.0.1:"+((ServletWebServerApplicationContext)app).getWebServer().getPort();
    }

 @BeforeEach void reset()throws Exception{execute("TRUNCATE ba_utilisateur,ba_departement,ba_service,ba_circuit,ba_demarche,ba_rh_circuit CASCADE");execute("INSERT INTO ba_departement(id,nom,created_by,created_date) VALUES ('dept','Département','test',now()),('foreign','Autre','test',now())");execute("INSERT INTO ba_service(id,nom,departement_id,created_by,created_date) VALUES ('s1','Service 1','dept','test',now()),('s2','Service 2','dept','test',now())");fixture("agent","AGENT","dept","s1");fixture("chief","CHEF_SERVICE","dept","s1");fixture("peer","CHEF_SERVICE","dept","s2");fixture("director","DIRECTEUR","dept",null);fixture("foreign","CHEF_SERVICE","foreign",null);fixture("ops","DIRECTEUR_GENERAL_ADJOINT",null,null);fixture("dev","DIRECTEUR_GENERAL_ADJOINT",null,null);fixture("dg","DIRECTEUR_GENERAL",null,null);fixture("admin","AGENT",null,null);execute("DELETE FROM ba_user_roles WHERE user_id='admin'");execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('admin','role-ba-admin')");execute("INSERT INTO ba_dga_pole_validateur(id,pole,dga_id,date_debut,actif,created_by,created_date) VALUES ('ops-pole','OPERATIONS','ops',CURRENT_DATE-1,true,'test',now()),('dev-pole','DEVELOPPEMENT','dev',CURRENT_DATE-1,true,'test',now())");}
 @AfterAll void close()throws Exception{if(app!=null)app.close();if(sql!=null)try{if(schema!=null&&schema.matches("bsic_processus_test_[0-9a-f]{32}"))execute("DROP SCHEMA "+schema+" CASCADE");}finally{sql.close();}}
 private Map<String,Object> step(String label,List<String> ids){return Map.of("libelle",label,"selection","UTILISATEURS","utilisateurs",ids,"departementDemandeur",false,"fonctionsDemandeur",List.of());}
 private Map<String,Object> definition(String refusal){return Map.of("mode","LIBRE","politiqueRefus",refusal,"etapes",List.of(step("Chef",List.of("chief","peer")),step("Direction",List.of("director"))));}
 private JsonNode model(Map<String,Object> def)throws Exception{return post("admin","modeles",Map.of("libelle","Circuit de recette","description","Données fictives","definition",def),201);}
 private JsonNode publish(JsonNode model)throws Exception{return post("admin","modeles/"+model.path("id").asText()+"/publier",Map.of("version",model.path("version").asLong(),"justification","Vérification explicite de recette"),200);}
 private JsonNode procedure(JsonNode model,String code,String domain,List<Object> fields)throws Exception{return post("admin","demarches",Map.of("code",code,"libelle",code,"description","Recette","domaine",domain,"publicationId",model.at("/publications/0/id").asText(),"actif",true,"champs",fields),201);}
 private JsonNode draft(JsonNode p,Map<String,String> values)throws Exception{return post("agent","dossiers",Map.of("demarcheId",p.path("id").asText(),"titre","Dossier de test","valeurs",values),201);}
 private JsonNode operation(String who,JsonNode d,String action,int expected)throws Exception{return post(who,"dossiers/"+d.path("id").asText()+"/"+action,op(d),expected);}
 private Map<String,Object> op(JsonNode d){return Map.of("version",d.path("version").asLong(),"cle",UUID.randomUUID().toString(),"etape",d.at("/circuit/etapeCourante").asInt(),"commentaire","Recette");}
 @Test void catalogueIndependentOfDisabledHabilitationsAndFrozenPublication()throws Exception{var m=publish(model(definition("DEFINITIF")));var p=procedure(m,"COURRIER","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);String captured=d.path("publicationId").asText();var changed=new LinkedHashMap<>(definition("DEFINITIF"));changed.put("etapes",List.of(step("Autre direction",List.of("dg"))));m=status(call("admin","PUT","modeles/"+m.path("id").asText(),Map.of("libelle","Nouvelle version","description","Suite","version",m.path("version").asLong(),"definition",changed)),200);m=publish(m);var updated=new LinkedHashMap<String,Object>();updated.put("code","COURRIER");updated.put("libelle","Courrier");updated.put("domaine","GENERIQUE");updated.put("publicationId",m.at("/publications/0/id").asText());updated.put("actif",true);updated.put("champs",List.of());updated.put("version",p.path("version").asLong());status(call("admin","PUT","demarches/"+p.path("id").asText(),updated),200);d=get("agent","dossiers/"+d.path("id").asText(),200);assertThat(d.path("publicationId").asText()).isEqualTo(captured);assertThat(d.at("/circuit/etapes/0/libelle").asText()).isEqualTo("Chef");assertThat(operation("peer",d,"approuver",200).at("/circuit/etapeCourante").asInt()).isEqualTo(1);}
 @Test void strictOrderSelfScopeTechnicalAdminAndConcurrency()throws Exception{var p=procedure(publish(model(definition("DEFINITIF"))),"DOCUMENT","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);operation("agent",d,"approuver",403);operation("admin",d,"approuver",403);operation("director",d,"approuver",403);get("foreign","dossiers/"+d.path("id").asText(),403);var body=op(d);String path="dossiers/"+d.path("id").asText()+"/approuver";assertThat(race(()->call("chief","POST",path,body).statusCode(),()->call("peer","POST",path,op(d)).statusCode())).containsExactlyInAnyOrder(200,409);JsonNode next=get("agent","dossiers/"+d.path("id").asText(),200);assertThat(next.at("/circuit/historique").size()).isEqualTo(1);assertThat(operation("director",next,"approuver",200).path("etat").asText()).isEqualTo("APPROUVEE");}
 @Test void actionReplayHasOneEffectAndDifferentPayloadConflict()throws Exception{var p=procedure(publish(model(definition("DEFINITIF"))),"DOSSIER","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);String path="dossiers/"+d.path("id").asText()+"/approuver";var body=new LinkedHashMap<>(op(d));var next=post("peer",path,body,200);assertThat(post("peer",path,body,200).path("version").asLong()).isEqualTo(next.path("version").asLong());body.put("commentaire","Autre contenu");post("peer",path,body,409);assertThat(get("agent","dossiers/"+d.path("id").asText(),200).at("/circuit/historique").size()).isEqualTo(1);}
 @Test void alternativeRefusalAndWithdrawalUseSharedEngine()throws Exception{var p=procedure(publish(model(definition("AUTRES_VALIDATEURS"))),"FORMULAIRE","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);d=operation("chief",d,"rejeter",200);assertThat(d.path("etat").asText()).isEqualTo("EN_ATTENTE");operation("chief",d,"approuver",403);d=operation("peer",d,"approuver",200);assertThat(d.at("/circuit/etapeCourante").asInt()).isEqualTo(1);d=operation("agent",d,"retirer",200);assertThat(d.path("etat").asText()).isEqualTo("RETIREE");operation("director",d,"approuver",409);}
 @Test void backendValidatesPublishedFormAndDraftsArePrivate()throws Exception{var field=Map.of("cle","reference","libelle","Référence","type","DATE","obligatoire",true);var p=procedure(publish(model(definition("DEFINITIF"))),"DATE_TEST","GENERIQUE",List.of(field));var d=draft(p,Map.of());operation("agent",d,"soumettre",400);get("admin","dossiers/"+d.path("id").asText(),403);status(call("agent","PUT","dossiers/"+d.path("id").asText(),Map.of("version",d.path("version").asLong(),"demarcheId",p.path("id").asText(),"titre","Test","valeurs",Map.of("reference","invalide"))),400);status(call("agent","PUT","dossiers/"+d.path("id").asText(),Map.of("version",d.path("version").asLong(),"demarcheId",p.path("id").asText(),"titre","Test","valeurs",Map.of("injection","autre"))),400);d=status(call("agent","PUT","dossiers/"+d.path("id").asText(),Map.of("version",d.path("version").asLong(),"demarcheId",p.path("id").asText(),"titre","Test","valeurs",Map.of("reference","2026-10-08"))),200);assertThat(operation("agent",d,"soumettre",200).path("etat").asText()).isEqualTo("EN_ATTENTE");}
 @Test void noAutoApprovalWithMissingOrRemovedReferenceFunction()throws Exception{var def=Map.of("mode","LIBRE","politiqueRefus","DEFINITIF","etapes",List.of(Map.of("libelle","Chefs du département","selection","FONCTION","fonction","CHEF_SERVICE","departementDemandeur",true)));var p=procedure(publish(model(def)),"CHEFS","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);execute("UPDATE ba_utilisateur SET departement_id='foreign',service_id=NULL WHERE id='chief'");operation("chief",d,"approuver",403);assertThat(operation("peer",d,"approuver",200).path("etat").asText()).isEqualTo("APPROUVEE");execute("UPDATE ba_utilisateur SET activated=false WHERE id='peer'");d=draft(p,Map.of());operation("agent",d,"soumettre",409);}
 @Test void rhAssignmentKeepsFunctionRulesAndCannotUseFreeCircuit()throws Exception{var free=publish(model(definition("DEFINITIF")));post("admin","demarches",Map.of("code","ABSENCE","libelle","Absences","domaine","ABSENCE","publicationId",free.at("/publications/0/id").asText(),"actif",true,"champs",List.of()),400);var hierarchical=publish(model(Map.of("mode","HIERARCHIQUE","politiqueRefus","DEFINITIF","dgObligatoire",false,"etapes",List.of())));procedure(hierarchical,"ABSENCE","ABSENCE",List.of());var body=Map.of("motif","Recette","dateDebut",LocalDate.now().plusDays(2),"dateFin",LocalDate.now().plusDays(2));var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/autorisations-absence")).header("Authorization","Bearer "+tokens.get("agent")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());var d=status(response,201);assertThat(d.at("/circuit/dgObligatoire").asBoolean()).isFalse();assertThat(d.at("/circuit/etapes").size()).isEqualTo(3);assertThat(d.at("/circuit/publicationId").asText()).isEqualTo(hierarchical.at("/publications/0/id").asText());assertThat(get("agent","configuration",200).path("gerer").asBoolean()).isFalse();post("agent","modeles",Map.of("libelle","Interdit","definition",definition("DEFINITIF")),403);}
 @Test void rolesAndProfilesRequireExactAdministrator()throws Exception{String roleId="role-processus-configurer";var request=HttpRequest.newBuilder(URI.create(base+"/api/users/agent/roles/"+roleId)).header("Authorization","Bearer "+tokens.get("chief")).POST(HttpRequest.BodyPublishers.noBody()).build();assertThat(http.send(request,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);execute("INSERT INTO ba_role(id,code,libelle,created_by,created_date) VALUES ('fake-admin','NOT_AN_ADMIN','Administrateur de présentation','test',now())");execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('foreign','fake-admin')");assertThat(get("foreign","configuration",200).path("gerer").asBoolean()).isFalse();}

 @Test void piecesStayPrivateAndAreFrozenWithTheRequest()throws Exception{
  var p=procedure(publish(model(definition("DEFINITIF"))),"PIECES","GENERIQUE",List.of());var d=draft(p,Map.of());String boundary="testBoundary";String data="--"+boundary+"\r\nContent-Disposition: form-data; name=\"fichier\"; filename=\"document.pdf\"\r\nContent-Type: application/pdf\r\n\r\n%PDF-1.4\nTest\r\n--"+boundary+"--\r\n";
  var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/processus/dossiers/"+d.path("id").asText()+"/pieces?version="+d.path("version").asLong())).header("Authorization","Bearer "+tokens.get("agent")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofString(data)).build(),HttpResponse.BodyHandlers.ofString());d=status(response,200);String fileId=d.at("/pieces/0/id").asText();assertThat(fileId).isNotBlank();get("foreign","pieces/"+fileId,403);get("admin","pieces/"+fileId,403);get(null,"pieces/"+fileId,401);d=operation("agent",d,"soumettre",200);get("chief","pieces/"+fileId,200);status(call("agent","DELETE","dossiers/"+d.path("id").asText()+"/pieces/"+fileId+"?version="+d.path("version").asLong(),null),409);
 }
 @Test void aMissingValidationPermissionBlocksSubmissionInsteadOfSkippingTheStage()throws Exception{var p=procedure(publish(model(definition("DEFINITIF"))),"PERMISSIONS","GENERIQUE",List.of());execute("DELETE FROM ba_user_roles WHERE user_id IN ('chief','peer') AND role_id='role-processus-valider'");var d=draft(p,Map.of());operation("agent",d,"soumettre",409);assertThat(get("agent","dossiers/"+d.path("id").asText(),200).path("etat").asText()).isEqualTo("BROUILLON");}
 @Test void hierarchicalSimulationAndAbsenceValidationDoNotRequireGenericPermissions()throws Exception{
  execute("DELETE FROM ba_user_roles WHERE role_id='role-processus-valider'");
  var m=model(Map.of("mode","HIERARCHIQUE","politiqueRefus","DEFINITIF","dgObligatoire",false,"etapes",List.of()));
  String simulation="modeles/"+m.path("id").asText()+"/simulation?agentId=agent";
  var preview=get("admin",simulation,200);
  assertThat(preview.has("blocage")).isFalse();
  assertThat(preview.at("/circuit/etapes").size()).isEqualTo(3);
  get("agent",simulation,403);
  procedure(publish(m),"ABSENCE","ABSENCE",List.of());
  var d=business("agent","POST","autorisations-absence",Map.of("motif","Recette sans permission générique","dateDebut",LocalDate.now().plusDays(10),"dateFin",LocalDate.now().plusDays(11)),201);
  String decision="autorisations-absence/"+d.path("id").asText()+"/valider?etape=";
  business("ops","PUT",decision+"0",null,403);
  d=business("peer","PUT",decision+"0",null,200);
  assertThat(d.path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
  d=business("director","PUT",decision+"1",null,200);
  assertThat(d.path("statutValidation").asText()).isEqualTo("EN_ATTENTE");
  d=business("ops","PUT",decision+"2",null,200);
  assertThat(d.path("statutValidation").asText()).isEqualTo("VALIDEE");
  execute("UPDATE ba_utilisateur SET activated=false WHERE id IN ('chief','peer')");
  assertThat(get("admin",simulation,200).has("blocage")).isTrue();
 }
 @Test void configurableSimulationShowsEligibleUsersAndExplainsMissingPermission()throws Exception{
  var m=model(definition("DEFINITIF"));
  execute("DELETE FROM ba_user_roles WHERE user_id IN ('chief','peer') AND role_id='role-processus-valider'");
  String path="modeles/"+m.path("id").asText()+"/simulation?agentId=agent";
  var result=get("admin",path,200);
  assertThat(result.path("blocage").asText()).contains("PROCESSUS_VALIDER","administrateur");
  assertThat(result.at("/circuit/etapes/0/validateurs").size()).isEqualTo(2);
  execute("INSERT INTO ba_user_roles(user_id,role_id) VALUES ('peer','role-processus-valider')");
  assertThat(get("admin",path,200).has("blocage")).isFalse();
 }
 @Test void hierarchicalGenericSubmissionStillRequiresGenericValidationPermission()throws Exception{
  var m=model(Map.of("mode","HIERARCHIQUE","politiqueRefus","DEFINITIF","dgObligatoire",false,"etapes",List.of()));
  execute("DELETE FROM ba_user_roles WHERE role_id='role-processus-valider'");
  assertThat(get("admin","modeles/"+m.path("id").asText()+"/simulation?agentId=agent",200).has("blocage")).isFalse();
  var p=procedure(publish(m),"GENERIC_HIERARCHICAL","GENERIQUE",List.of());
  var d=draft(p,Map.of());
  operation("agent",d,"soumettre",409);
  assertThat(get("agent","dossiers/"+d.path("id").asText(),200).path("etat").asText()).isEqualTo("BROUILLON");
 }
 private JsonNode business(String actor,String method,String path,Object body,int expected)throws Exception{
  var request=HttpRequest.newBuilder(URI.create(base+"/api/"+path)).header("Authorization","Bearer "+tokens.get(actor)).header("Content-Type","application/json")
    .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
  return status(http.send(request,HttpResponse.BodyHandlers.ofString()),expected);
 }
 @Test void administratorCanAssignAndRemoveRolesIdempotentlyThroughTheUserApi()throws Exception{
  String path="users/chief/roles/role-conges-valider";
  var result=business("admin","POST",path,null,200);
  assertThat(result.path("roles").findValuesAsText("code")).contains("CONGES_VALIDER");
  business("admin","POST",path,null,200);
  result=business("admin","GET","users/chief/roles",null,200);
  assertThat(result.findValuesAsText("code")).contains("CONGES_VALIDER");
  result=business("admin","DELETE",path,null,200);
  assertThat(result.path("roles").findValuesAsText("code")).doesNotContain("CONGES_VALIDER");
  business("admin","DELETE",path,null,200);
  business("peer","POST",path,null,403);
  business("peer","DELETE",path,null,403);
 }
 @Test void simultaneousRoleAssignmentsHaveOnePersistentEffect()throws Exception{
  String path="users/chief/roles/role-conges-valider";
  assertThat(race(()->{business("admin","POST",path,null,200);return 200;},()->{business("admin","POST",path,null,200);return 200;})).containsOnly(200);
  var roles=business("admin","GET","users/chief/roles",null,200).findValuesAsText("code");
  assertThat(roles.stream().filter("CONGES_VALIDER"::equals).count()).isEqualTo(1);
 }
 @Test void deletingUnusedCircuitsRemovesThemFromLibraryAndAssignmentChoices()throws Exception {
  var draft=model(definition("DEFINITIF"));var published=publish(model(definition("DEFINITIF")));
  String publicationId=published.at("/publications/0/id").asText();
  var result=post("admin","modeles/suppression",deletion(target("CIRCUIT",draft),target("CIRCUIT",published)),200);
  assertThat(result.path("circuitsSupprimes").asInt()).isEqualTo(2);
  assertThat(result.path("publicationsSupprimees").asInt()).isEqualTo(1);
  assertThat(get("admin","modeles",200).size()).isZero();
  get("admin","modeles/"+published.path("id").asText()+"/simulation?agentId=agent",404);
  post("admin","demarches",Map.of("code","DELETED","libelle","Interdite","domaine","GENERIQUE","publicationId",publicationId,"actif",true,"champs",List.of()),404);
  try(var statement=sql.prepareStatement("SELECT statut FROM ba_circuit_publication WHERE id=?")) {
   statement.setString(1,publicationId);try(var rows=statement.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("D");}
  }
  try(var statement=sql.prepareStatement("SELECT created_by,action,details FROM ba_log WHERE sujet=?")) {
   statement.setString(1,"Suppression publication "+publicationId);
   try(var rows=statement.executeQuery()) {
    assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("admin");
    assertThat(rows.getInt(2)).isEqualTo(com.bakouan.app.enums.EAction.D.ordinal());
    assertThat(rows.getString(3)).contains(published.path("id").asText(),"version 1");assertThat(rows.next()).isFalse();
   }
  }
 }
 @Test void anAccidentalDuplicatePublicationCanBeDeletedWithoutDeletingTheCircuit()throws Exception {
  var m=publish(model(definition("DEFINITIF")));String original=m.at("/publications/0/id").asText();
  // Reprise d'une double publication créée avant la protection contre les doublons.
  execute("INSERT INTO ba_circuit_publication(id,circuit_id,numero,libelle,description,justification,definition,created_by,created_date) SELECT 'accidental-copy',circuit_id,2,libelle,description,'Double clic historique',definition,'test',now() FROM ba_circuit_publication WHERE id='"+original+"'");
  m=loadedModel(m.path("id").asText());assertThat(m.path("publications").size()).isEqualTo(2);
  var result=post("admin","modeles/suppression",deletion(target("PUBLICATION",m.at("/publications/0"))),200);
  assertThat(result.path("circuitsSupprimes").asInt()).isZero();assertThat(result.path("publicationsSupprimees").asInt()).isEqualTo(1);
  m=loadedModel(m.path("id").asText());assertThat(m.path("publications").size()).isEqualTo(1);assertThat(m.at("/publications/0/id").asText()).isEqualTo(original);
  post("admin","modeles/suppression",deletion(target("PUBLICATION",m.at("/publications/0"))),200);
  m=publish(loadedModel(m.path("id").asText()));assertThat(m.at("/publications/0/numero").asInt()).isEqualTo(3);
 }
 @Test void deletionOfAnAssignedVersionBlocksTheWholeBatchIncludingInactiveProcedures()throws Exception {
  var unused=model(definition("DEFINITIF"));var used=publish(model(definition("DEFINITIF")));var procedure=procedure(used,"USED","GENERIQUE",List.of());
  execute("UPDATE ba_demarche SET actif=false WHERE id='"+procedure.path("id").asText()+"'");
  used=loadedModel(used.path("id").asText());assertThat(used.path("supprimable").asBoolean()).isFalse();assertThat(used.at("/publications/0/blocageSuppression").asText()).contains("USED","Réaffectez");
  post("admin","modeles/suppression",deletion(target("CIRCUIT",unused),target("PUBLICATION",used.at("/publications/0"))),409);
  assertThat(loadedModel(unused.path("id").asText()).path("supprimable").asBoolean()).isTrue();
  assertThat(loadedModel(used.path("id").asText()).path("publications").size()).isEqualTo(1);
 }
 @Test void aHistoricVersionRemainsProtectedAfterTheProcedureIsReassigned()throws Exception {
  var m=publish(model(definition("DEFINITIF")));var first=m.at("/publications/0");
  var p=procedure(m,"HISTORY","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);
  m=status(call("admin","PUT","modeles/"+m.path("id").asText(),Map.of("libelle","Circuit révisé","description","Données fictives","version",m.path("version").asLong(),"definition",definition("DEFINITIF"))),200);m=publish(m);
  status(call("admin","PUT","demarches/"+p.path("id").asText(),Map.of("code","HISTORY","libelle","HISTORY","domaine","GENERIQUE","publicationId",m.at("/publications/0/id").asText(),"actif",true,"champs",List.of(),"version",p.path("version").asLong())),200);
  post("admin","modeles/suppression",deletion(target("PUBLICATION",first)),409);
  var current=loadedModel(m.path("id").asText());assertThat(current.at("/publications/1/blocageSuppression").asText()).contains("Utilisée par une demande");
  d=operation("peer",d,"approuver",200);d=operation("director",d,"approuver",200);
  assertThat(d.path("etat").asText()).isEqualTo("APPROUVEE");assertThat(d.path("publicationId").asText()).isEqualTo(first.path("id").asText());
 }
 @Test void deletionRequiresConfigurationPermissionAndCurrentVersions()throws Exception {
  var m=model(definition("DEFINITIF"));var body=deletion(target("CIRCUIT",m));
  post("agent","modeles/suppression",body,403);post(null,"modeles/suppression",body,401);
  post("admin","modeles/suppression",Map.of("cibles",List.of()),400);
  post("admin","modeles/suppression",deletion(Map.of("type","CIRCUIT","id",m.path("id").asText(),"version",-1)),409);
  post("admin","modeles/suppression",deletion(target("CIRCUIT",m),target("CIRCUIT",m)),400);
  assertThat(loadedModel(m.path("id").asText()).isMissingNode()).isFalse();
  business("admin","POST","users/chief/roles/role-processus-configurer",null,200);
  post("chief","modeles/suppression",body,200);
 }
 @Test void aFrozenAbsenceCircuitProtectsItsPublicationAfterReassignment()throws Exception {
  var definition=Map.<String,Object>of("mode","HIERARCHIQUE","politiqueRefus","DEFINITIF","dgObligatoire",false,"etapes",List.of());
  var m=publish(model(definition));var first=m.at("/publications/0");var p=procedure(m,"ABSENCE","ABSENCE",List.of());
  var absence=business("agent","POST","autorisations-absence",Map.of("motif","Historique RH","dateDebut",LocalDate.now().plusDays(20),"dateFin",LocalDate.now().plusDays(20)),201);
  m=status(call("admin","PUT","modeles/"+m.path("id").asText(),Map.of("libelle","Circuit RH révisé","description","Données fictives","version",m.path("version").asLong(),"definition",definition)),200);m=publish(m);
  status(call("admin","PUT","demarches/"+p.path("id").asText(),Map.of("code","ABSENCE","libelle","ABSENCE","domaine","ABSENCE","publicationId",m.at("/publications/0/id").asText(),"actif",true,"champs",List.of(),"version",p.path("version").asLong())),200);
  var current=loadedModel(m.path("id").asText());assertThat(current.at("/publications/1/supprimable").asBoolean()).isFalse();
  assertThat(current.at("/publications/1/blocageSuppression").asText()).contains("Utilisée par une demande");
  post("admin","modeles/suppression",deletion(target("PUBLICATION",first)),409);
  String decision="autorisations-absence/"+absence.path("id").asText()+"/valider?etape=";
  business("peer","PUT",decision+"0",null,200);business("director","PUT",decision+"1",null,200);
  var approved=business("ops","PUT",decision+"2",null,200);
  assertThat(approved.path("statutValidation").asText()).isEqualTo("VALIDEE");assertThat(approved.at("/circuit/publicationId").asText()).isEqualTo(first.path("id").asText());
 }
 @Test void repeatedAndConcurrentPublicationCannotCreateAnIdenticalVersion()throws Exception {
  var m=model(definition("DEFINITIF"));String path="modeles/"+m.path("id").asText()+"/publier";
  var body=Map.of("version",m.path("version").asLong(),"justification","Recette concurrente");
  assertThat(race(()->call("admin","POST",path,body).statusCode(),()->call("admin","POST",path,body).statusCode())).containsExactlyInAnyOrder(200,409);
  m=loadedModel(m.path("id").asText());assertThat(m.path("publications").size()).isEqualTo(1);assertThat(m.path("dejaPublie").asBoolean()).isTrue();
  post("admin",path,Map.of("version",m.path("version").asLong(),"justification","Deuxième publication par erreur"),409);
  assertThat(loadedModel(m.path("id").asText()).path("publications").size()).isEqualTo(1);
 }
 @Test void aVisualReorderAffectsOnlyTheNewPublicationAndItsFutureRequests()throws Exception {
  var m=publish(model(definition("DEFINITIF")));var original=m.at("/publications/0");
  var p=procedure(m,"VISUAL_WORKFLOW","GENERIQUE",List.of());var oldDraft=draft(p,Map.of());
  var reordered=new LinkedHashMap<>(definition("DEFINITIF"));reordered.put("etapes",List.of(step("Direction",List.of("director")),step("Chef",List.of("chief","peer"))));
  m=status(call("admin","PUT","modeles/"+m.path("id").asText(),Map.of("libelle",m.path("libelle").asText(),"description",m.path("description").asText(),"definition",reordered,"version",m.path("version").asLong())),200);
  assertThat(m.at("/definition/etapes/0/libelle").asText()).isEqualTo("Direction");
  assertThat(m.at("/publications/0/definition/etapes/0/libelle").asText()).isEqualTo("Chef");
  assertThat(m.at("/publications/0/id").asText()).isEqualTo(original.path("id").asText());
  m=publish(m);p=status(call("admin","PUT","demarches/"+p.path("id").asText(),Map.of("code","VISUAL_WORKFLOW","libelle","VISUAL_WORKFLOW","domaine","GENERIQUE","publicationId",m.at("/publications/0/id").asText(),"actif",true,"champs",List.of(),"version",p.path("version").asLong())),200);
  var fresh=operation("agent",draft(p,Map.of()),"soumettre",200);
  assertThat(fresh.at("/circuit/etapes/0/libelle").asText()).isEqualTo("Direction");
  operation("chief",fresh,"approuver",403);fresh=operation("director",fresh,"approuver",200);fresh=operation("peer",fresh,"approuver",200);
  assertThat(fresh.path("etat").asText()).isEqualTo("APPROUVEE");
  oldDraft=operation("agent",oldDraft,"soumettre",200);assertThat(oldDraft.at("/circuit/etapes/0/libelle").asText()).isEqualTo("Chef");
  assertThat(oldDraft.path("publicationId").asText()).isEqualTo(original.path("id").asText());
 }
 @BeforeEach void resetSuiviFeature(){app.getBean(com.bakouan.app.config.FeatureConfig.class).setHabilitationsEnabled(false);}
 @Test void monitoringRequiresItsOwnActivePermissionEvenForAdminAndDg()throws Exception {
  for(String actor:List.of("agent","admin","dg")){
   assertThat(business(actor,"GET","suivi-demandes/configuration",null,200).path("autorise").asBoolean()).isFalse();
   business(actor,"GET","suivi-demandes",null,403);
  }
  monitoringRole("ops","SUIVI_DEMANDES_DEPARTEMENT");
  assertThat(business("ops","GET","suivi-demandes/configuration",null,200).path("blocage").asText()).contains("département");
  business("ops","GET","suivi-demandes",null,403);
 }
 @Test void departmentMonitoringIncludesOtherServicesButRejectsExternalIdsAndFilters()throws Exception {
  var p=procedure(publish(model(definition("DEFINITIF"))),"SUIVI_DEPT","GENERIQUE",List.of());
  var a=operation("agent",draft(p,Map.of()),"soumettre",200);
  var peerDraft=post("peer","dossiers",Map.of("demarcheId",p.path("id").asText(),"titre","Contenu confidentiel","valeurs",Map.of()),201);
  operation("peer",peerDraft,"soumettre",200);
  var foreignDraft=post("foreign","dossiers",Map.of("demarcheId",p.path("id").asText(),"titre","Autre département","valeurs",Map.of()),201);
  var outside=operation("foreign",foreignDraft,"soumettre",200);
  fixture("observer","AGENT","dept","s2");monitoringRole("observer","SUIVI_DEMANDES_DEPARTEMENT");
  var list=business("observer","GET","suivi-demandes",null,200);
  assertThat(list.path("totalElements").asInt()).isEqualTo(2);assertThat(list.path("content").toString()).doesNotContain(outside.path("id").asText(),"Contenu confidentiel");
  for(var row:list.path("content"))assertThat(row.path("departementId").asText()).isEqualTo("dept");
  business("observer","GET","suivi-demandes?departementId=foreign",null,403);
  business("observer","GET","suivi-demandes/DEMARCHE/"+outside.path("id").asText(),null,404);
  var detail=business("observer","GET","suivi-demandes/DEMARCHE/"+a.path("id").asText(),null,200);
  assertThat(detail.at("/circuit/peutDecider").asBoolean()).isFalse();assertThat(detail.at("/circuit/etapes").size()).isEqualTo(2);
  get("observer","dossiers/"+a.path("id").asText(),403);operation("observer",a,"approuver",403);
  business("observer","POST","suivi-demandes/DEMARCHE/"+a.path("id").asText(),Map.of(),405);
 }
 @Test void globalMonitoringIsPaginatedDefaultsToPendingAndRecordsTheEffectiveSigner()throws Exception {
  var p=procedure(publish(model(definition("DEFINITIF"))),"SUIVI_GLOBAL","GENERIQUE",List.of());
  var pending=operation("agent",draft(p,Map.of()),"soumettre",200);
  var approved=operation("agent",draft(p,Map.of()),"soumettre",200);approved=operation("chief",approved,"approuver",200);approved=operation("director",approved,"approuver",200);
  var external=post("foreign","dossiers",Map.of("demarcheId",p.path("id").asText(),"titre","Autre","valeurs",Map.of()),201);operation("foreign",external,"soumettre",200);
  fixture("observer","AGENT",null,null);monitoringRole("observer","SUIVI_DEMANDES_GLOBAL");
  assertThat(business("observer","GET","suivi-demandes/configuration",null,200).path("perimetre").asText()).isEqualTo("GLOBAL");
  var list=business("observer","GET","suivi-demandes?size=1",null,200);assertThat(list.path("totalElements").asInt()).isEqualTo(2);assertThat(list.path("content").size()).isEqualTo(1);assertThat(list.path("totalPages").asInt()).isEqualTo(2);
  assertThat(business("observer","GET","suivi-demandes?etat=",null,200).path("totalElements").asInt()).isEqualTo(3);
  var detail=business("observer","GET","suivi-demandes/DEMARCHE/"+approved.path("id").asText(),null,200);
  assertThat(detail.at("/demande/etapesValidees").asInt()).isEqualTo(2);assertThat(detail.at("/circuit/etapes/1/nomSignataire").asText()).contains("director");
  assertThat(detail.at("/circuit/historique").size()).isEqualTo(2);assertThat(detail.at("/circuit/peutDecider").asBoolean()).isFalse();
  try(var s=sql.createStatement();var r=s.executeQuery("SELECT count(*) FROM ba_log WHERE sujet='Suivi des demandes' AND details LIKE '%acteur=observer%action=DETAIL%'")){r.next();assertThat(r.getInt(1)).isGreaterThan(0);}
  assertThat(get("agent","dossiers/"+pending.path("id").asText(),200).path("version").asLong()).isEqualTo(pending.path("version").asLong());
 }
 @Test void monitoringRechecksRolesCurrentDepartmentAndServiceFallbackWithoutANewToken()throws Exception {
  var p=procedure(publish(model(definition("DEFINITIF"))),"SUIVI_MUTATION","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);
  fixture("observer","AGENT",null,"s2");monitoringRole("observer","SUIVI_DEMANDES_DEPARTEMENT");
  assertThat(business("observer","GET","suivi-demandes",null,200).path("totalElements").asInt()).isEqualTo(1);
  execute("UPDATE ba_utilisateur SET departement_id='foreign',service_id=NULL WHERE id='agent'");
  assertThat(business("observer","GET","suivi-demandes",null,200).path("totalElements").asInt()).isZero();business("observer","GET","suivi-demandes/DEMARCHE/"+d.path("id").asText(),null,404);
  execute("UPDATE ba_utilisateur SET departement_id='foreign',service_id=NULL WHERE id='observer'");
  assertThat(business("observer","GET","suivi-demandes",null,200).path("totalElements").asInt()).isEqualTo(1);
  execute("DELETE FROM ba_user_roles WHERE user_id='observer' AND role_id='role-suivi-departement'");
  business("observer","GET","suivi-demandes",null,403);assertThat(business("observer","GET","suivi-demandes/configuration",null,200).path("autorise").asBoolean()).isFalse();
 }
 @Test void monitoringAcceptsActiveProfileRightsAndGlobalWinsOverDepartment()throws Exception {
  fixture("observer","AGENT",null,null);monitoringRole("observer","SUIVI_DEMANDES_DEPARTEMENT");
  execute("INSERT INTO ba_profil(id,libelle,created_by,created_date) VALUES ('suivi-profile','Profil supervision','test',now())");
  execute("INSERT INTO ba_profils_roles(profil_id,role_id) VALUES ('suivi-profile','role-suivi-global')");execute("UPDATE ba_utilisateur SET profil_uuid='suivi-profile' WHERE id='observer'");
  assertThat(business("observer","GET","suivi-demandes/configuration",null,200).path("perimetre").asText()).isEqualTo("GLOBAL");
  execute("UPDATE ba_profil SET statut='D' WHERE id='suivi-profile'");business("observer","GET","suivi-demandes",null,403);
  execute("UPDATE ba_role SET statut='D' WHERE id='role-suivi-departement'");assertThat(business("observer","GET","suivi-demandes/configuration",null,200).path("autorise").asBoolean()).isFalse();
  execute("UPDATE ba_role SET statut='A' WHERE id='role-suivi-departement'");
 }
 @Test void monitoringExcludesPrivateDraftsAndValidatesFiltersAndLiteralSearch()throws Exception {
  var p=procedure(publish(model(definition("DEFINITIF"))),"SUIVI_FILTRES","GENERIQUE",List.of());var privateDraft=draft(p,Map.of());var withdrawn=draft(p,Map.of());
  execute("UPDATE ba_demarche_dossier SET etat='RETIREE' WHERE id='"+withdrawn.path("id").asText()+"'");
  operation("agent",draft(p,Map.of()),"soumettre",200);fixture("observer","AGENT","dept","s1");monitoringRole("observer","SUIVI_DEMANDES_GLOBAL");
  assertThat(business("observer","GET","suivi-demandes?etat=",null,200).path("totalElements").asInt()).isEqualTo(1);
  for(var d:List.of(privateDraft,withdrawn))business("observer","GET","suivi-demandes/DEMARCHE/"+d.path("id").asText(),null,404);
  for(String filter:List.of("size=101","page=-1","page=texte","etat=BROUILLON","module=INCONNU"))business("observer","GET","suivi-demandes?"+filter,null,400);
  assertThat(business("observer","GET","suivi-demandes?recherche=%25",null,200).path("totalElements").asInt()).isZero();
  assertThat(business("observer","GET","suivi-demandes?recherche=agent",null,200).path("totalElements").asInt()).isEqualTo(1);
 }
 @Test void monitoringCombinesAllModulesWithoutSensitiveContentAndHonorsFeatureFlag()throws Exception {
  var m=publish(model(definition("DEFINITIF")));var p=procedure(m,"SUIVI_MODULES","GENERIQUE",List.of());var d=operation("agent",draft(p,Map.of()),"soumettre",200);
  String circuit;try(var s=sql.createStatement();var r=s.executeQuery("SELECT circuit_id FROM ba_demarche_dossier WHERE id='"+d.path("id").asText()+"'")){r.next();circuit=r.getString(1);}
  execute("INSERT INTO ba_autorisation_absence(id,motif,date_debut,date_fin,nombre_jours,demandeur_id,statut_validation,circuit_id,statut,created_by,created_date) VALUES ('suivi-absence','SECRET_MEDICAL',CURRENT_DATE,CURRENT_DATE,1,'agent','EN_ATTENTE','"+circuit+"','A','test',now())");
  execute("INSERT INTO ba_autorisation_sortie(id,motif,date_sortie,demandeur_id,statut_validation,circuit_id,created_by,created_date) VALUES ('suivi-sortie','SECRET_SORTIE',CURRENT_DATE,'agent','EN_ATTENTE','"+circuit+"','test',now())");
  execute("INSERT INTO ba_conge_demande(id,agent_id,type_code,etat,debut,fin,portion_debut,portion_fin,motif,circuit_id,created_by,created_date) VALUES ('suivi-conge','agent','TEST','EN_ATTENTE',CURRENT_DATE,CURRENT_DATE,'JOURNEE','JOURNEE','SECRET_CONGE','"+circuit+"','test',now())");
  execute("INSERT INTO ba_reprise_service(id,demandeur_id,signataire_id,motif_absence,date_debut_conge,date_fin_conge,date_reprise,statut_validation,created_by,created_date) VALUES ('suivi-reprise','agent','director','SECRET_REPRISE',CURRENT_DATE,CURRENT_DATE,CURRENT_DATE,'EN_ATTENTE','test',now())");
  execute("INSERT INTO ba_fiche_habilitation(id,employe_id,circuit_id,motif,statut_habilitation,created_by,created_date) VALUES ('suivi-fiche','agent','"+m.path("id").asText()+"','SECRET_HABILITATION','EN_COURS','test',now())");
  execute("INSERT INTO ba_fiche_habilitation_etape(id,fiche_id,ordre,validateur_id,statut_validation,commentaire,created_by,created_date) VALUES ('suivi-etape','suivi-fiche',1,'director','EN_ATTENTE','SECRET_COMMENTAIRE','test',now())");
  execute("INSERT INTO ba_plateforme(id,nom,circuit_id,created_by,created_date) VALUES ('suivi-platform','Plateforme','"+m.path("id").asText()+"','test',now())");
  execute("INSERT INTO ba_reinitialisation_compte(id,demandeur_id,plateforme_id,circuit_id,motif,statut_demande,created_by,created_date) VALUES ('suivi-reset','agent','suivi-platform','"+m.path("id").asText()+"','SECRET_COMPTE','EN_COURS','test',now())");
  execute("INSERT INTO ba_reinitialisation_compte_etape(id,demande_id,ordre,validateur_id,statut_validation,created_by,created_date) VALUES ('suivi-reset-etape','suivi-reset',1,'director','EN_ATTENTE','test',now())");
  fixture("observer","AGENT","dept","s2");monitoringRole("observer","SUIVI_DEMANDES_DEPARTEMENT");
  var rhList=business("observer","GET","suivi-demandes",null,200);assertThat(rhList.path("totalElements").asInt()).as(rhList.toString()).isEqualTo(5);business("observer","GET","suivi-demandes/HABILITATION/suivi-fiche",null,404);
  app.getBean(com.bakouan.app.config.FeatureConfig.class).setHabilitationsEnabled(true);
  var list=business("observer","GET","suivi-demandes",null,200);assertThat(list.path("totalElements").asInt()).isEqualTo(7);assertThat(list.toString()).doesNotContain("SECRET_");
  for(var row:list.path("content")){var detail=business("observer","GET","suivi-demandes/"+row.path("module").asText()+"/"+row.path("id").asText(),null,200);
   assertThat(detail.toString()).doesNotContain("SECRET_");assertThat(detail.at("/circuit/peutDecider").asBoolean()).isFalse();assertThat(detail.at("/circuit/etapes").size()).isPositive();assertThat(row.path("etapesTotal").asInt()).isPositive();}
 }
 private void monitoringRole(String actor,String code)throws Exception{execute("DELETE FROM ba_user_roles WHERE user_id='"+actor+"'");execute("INSERT INTO ba_user_roles(user_id,role_id) SELECT '"+actor+"',id FROM ba_role WHERE code IN ('BA_CONNECT','"+code+"')");}
 @Test void assigningAndDeletingTheSamePublicationCannotBothSucceed()throws Exception {
  var m=publish(model(definition("DEFINITIF")));String id=m.at("/publications/0/id").asText();
  var assignment=Map.of("code","CONCURRENT","libelle","Concurrente","domaine","GENERIQUE","publicationId",id,"actif",true,"champs",List.of());
  var deletion=deletion(target("PUBLICATION",m.at("/publications/0")));
  var outcomes=race(()->call("admin","POST","demarches",assignment).statusCode(),()->call("admin","POST","modeles/suppression",deletion).statusCode());
  assertThat(outcomes.equals(List.of(201,409))||outcomes.equals(List.of(400,200))).as("Une seule opération peut réussir : %s",outcomes).isTrue();
 }
 private Map<String,Object> target(String type,JsonNode entity){return Map.of("type",type,"id",entity.path("id").asText(),"version",entity.path("version").asLong());}
 @SafeVarargs private final Map<String,Object> deletion(Map<String,Object>... targets){return Map.of("cibles",List.of(targets));}
 private JsonNode loadedModel(String id)throws Exception{for(var m:get("admin","modeles",200))if(id.equals(m.path("id").asText()))return m;return json.missingNode();}
 @Test void platformCatalogueIsIndependentFromHabilitationsAndStoresDetailedMetadata()throws Exception {
  var cfg=platform("admin","GET","configuration",null,200);assertThat(cfg.path("consulter").asBoolean()).isTrue();assertThat(cfg.path("gerer").asBoolean()).isTrue();assertThat(cfg.path("habilitations").asBoolean()).isFalse();
  assertThat(cfg.path("responsables").findValuesAsText("id")).contains("agent","chief");assertThat(cfg.toString()).doesNotContain("password_hash","telephone","email");
  var form=platformForm("CORE");var fiche=new LinkedHashMap<String,Object>();fiche.put("categorie","APPLICATION");fiche.put("cycleVie","EN_SERVICE");fiche.put("criticite","CRITIQUE");fiche.put("hebergement","INTERNE");fiche.put("fonctionnalites","Tenue des comptes et reporting");fiche.put("versionLogicielle","2026.1");fiche.put("rtoMinutes",120);fiche.put("rpoMinutes",15);
  fiche.put("environnements",List.of(Map.of("type","PRODUCTION","nom","Production","url","https://core.example.test","description","Usage courant")));fiche.put("liens",List.of(Map.of("titre","Guide d’exploitation","url","https://docs.example.test/core")));form.put("fiche",fiche);form.put("departementId","dept");form.put("responsableMetierId","director");form.put("responsableTechniqueId","chief");
  var created=platform("admin","POST","",form,201);assertThat(created.at("/fiche/rtoMinutes").asInt()).isEqualTo(120);assertThat(created.at("/responsableTechnique/id").asText()).isEqualTo("chief");assertThat(created.at("/fiche/liens/0/titre").asText()).contains("exploitation");
  var page=platform("admin","GET","?categorie=APPLICATION&cycleVie=EN_SERVICE&criticite=CRITIQUE&departementId=dept",null,200);assertThat(page.path("totalElements").asInt()).isEqualTo(1);
  assertThat(platform("admin","GET","?criticite=FAIBLE",null,200).path("content").size()).isZero();
  var history=platform("admin","GET",created.path("id").asText()+"/historique",null,200);assertThat(history.size()).isEqualTo(1);assertThat(history.at("/0/snapshot/fiche/versionLogicielle").asText()).isEqualTo("2026.1");assertThat(history.at("/0/auteur").asText()).isEqualTo("admin");
  business("admin","GET","habilitations/plateformes",null,404);platform(null,"GET","",null,401);
 }
 @Test void platformPermissionsAreCheckedFromDatabaseForEveryApiCall()throws Exception {
  var created=platform("admin","POST","",platformForm("DOC"),201);String id=created.path("id").asText();
  assertThat(platform("agent","GET","configuration",null,200).path("consulter").asBoolean()).isFalse();platform("agent","GET","",null,403);platform("agent","GET",id,null,403);platform("agent","GET",id+"/historique",null,403);
  platformRole("agent","PLATEFORMES_CONSULTER");platform("agent","GET",id,null,200);platform("agent","POST","",platformForm("FORBIDDEN"),403);platform("agent","PUT",id,platformEdit(created),403);platform("agent","POST",id+"/archiver",Map.of("version",created.path("version").asLong(),"motif","Test"),403);
  platformRole("chief","PLATEFORMES_GERER");platform("chief","POST","",platformForm("OWNED"),201);
  execute("DELETE FROM ba_user_roles WHERE user_id='chief' AND role_id='role-plateformes-gerer'");platform("chief","POST","",platformForm("REVOKED"),403);
  execute("INSERT INTO ba_profil(id,libelle,created_by,created_date) VALUES ('platform-profile','Catalogue','test',now())");execute("INSERT INTO ba_profils_roles(profil_id,role_id) VALUES ('platform-profile','role-plateformes-consulter')");execute("UPDATE ba_utilisateur SET profil_uuid='platform-profile' WHERE id='peer'");
  platform("peer","GET",id,null,200);execute("UPDATE ba_profil SET statut='D' WHERE id='platform-profile'");platform("peer","GET",id,null,403);
  execute("UPDATE ba_role SET statut='D' WHERE code='PLATEFORMES_CONSULTER'");try{platform("agent","GET",id,null,403);}finally{execute("UPDATE ba_role SET statut='A' WHERE code='PLATEFORMES_CONSULTER'");}
 }
 @Test void platformInvalidInputsAndSecretBearingUrlsCannotBePersisted()throws Exception {
  var form=platformForm("BAD");form.put("code","<script>");platform("admin","POST","",form,400);
  form=platformForm("BAD");form.put("departementId","missing");platform("admin","POST","",form,400);
  form=platformForm("BAD");form.put("dependanceIds",List.of("missing"));platform("admin","POST","",form,400);
  for(String address:List.of("javascript:alert(1)","https://user:password@example.test","https://example.test?token=secret","https://example.test#secret","file:///C:/secret")){
   form=platformForm("BAD");form.put("fiche",Map.of("liens",List.of(Map.of("titre","Interdit","url",address))));platform("admin","POST","",form,400);
  }
  form=platformForm("BAD");form.put("fiche",Map.of("rtoMinutes",-1));platform("admin","POST","",form,400);
  form=platformForm("BAD");form.put("fiche",Map.of("rtoMinutes",1.5));platform("admin","POST","",form,400);
  form=platformForm("BAD");form.put("fiche",Map.of("dateMiseService","2026-10-01","dateFinSupport","2026-09-01"));platform("admin","POST","",form,400);
  form=platformForm("BAD");form.put("fiche",Map.of("environnements",List.of(Map.of("type","PRODUCTION","nom",""))));platform("admin","POST","",form,400);
  form=platformForm("BAD");form.put("motif"," ");platform("admin","POST","",form,400);
  assertThat(platform("admin","GET","",null,200).path("totalElements").asInt()).isZero();platform("admin","GET","?size=101",null,400);platform("admin","GET","?cycleVie=INCONNU",null,400);
 }
 @Test void platformDependenciesArchivingAndRestorationKeepTheExistingReferential()throws Exception {
  var dependency=platform("admin","POST","",platformForm("INFRA"),201);String dep=dependency.path("id").asText();
  var form=platformForm("APP");form.put("dependanceIds",List.of(dep));var appPlatform=platform("admin","POST","",form,201);String id=appPlatform.path("id").asText();
  assertThat(platform("admin","GET",dep,null,200).path("utiliseePar").findValuesAsText("id")).containsExactly(id);
  platform("admin","POST",dep+"/archiver",Map.of("version",dependency.path("version").asLong(),"motif","Retrait"),409);
  form=platformEdit(appPlatform);form.put("dependanceIds",List.of(id));platform("admin","PUT",id,form,400);form.put("dependanceIds",List.of(dep,dep));platform("admin","PUT",id,form,400);
  execute("INSERT INTO ba_circuit(id,code,libelle,plateforme_id,created_by,created_date) VALUES ('platform-historical','HIST','Historique','"+id+"','test',now())");
  appPlatform=platform("admin","POST",id+"/archiver",Map.of("version",appPlatform.path("version").asLong(),"motif","Fin d’usage"),200);assertThat(appPlatform.path("archivee").asBoolean()).isTrue();
  assertThat(platform("admin","GET","?archivees=true",null,200).path("totalElements").asInt()).isEqualTo(1);
  dependency=platform("admin","POST",dep+"/archiver",Map.of("version",dependency.path("version").asLong(),"motif","Fin d’usage"),200);
  platform("admin","PUT",id,platformEdit(appPlatform),409);platform("admin","POST",id+"/restaurer",Map.of("version",appPlatform.path("version").asLong(),"motif","Reprise"),409);
  platform("admin","POST",dep+"/restaurer",Map.of("version",dependency.path("version").asLong(),"motif","Reprise"),200);
  appPlatform=platform("admin","POST",id+"/restaurer",Map.of("version",appPlatform.path("version").asLong(),"motif","Reprise"),200);assertThat(appPlatform.path("archivee").asBoolean()).isFalse();
  try(var q=sql.createStatement();var r=q.executeQuery("SELECT plateforme_id FROM ba_circuit WHERE id='platform-historical'")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo(id);}
 }
 @Test void platformConcurrentEditsAndDuplicateCreationHaveOnlyOneEffect()throws Exception {
  var d=platform("admin","POST","",platformForm("LOCK"),201);String id=d.path("id").asText();var first=platformEdit(d);first.put("nom","Première modification");var second=platformEdit(d);second.put("nom","Autre modification");
  assertThat(race(()->platformRaw("admin","PUT",id,first).statusCode(),()->platformRaw("admin","PUT",id,second).statusCode())).containsExactlyInAnyOrder(200,409);
  assertThat(platform("admin","GET",id+"/historique",null,200).size()).isEqualTo(2);assertThat(platform("admin","GET",id,null,200).path("version").asLong()).isEqualTo(d.path("version").asLong()+1);
  assertThat(race(()->platformRaw("admin","POST","",platformForm("SAME")).statusCode(),()->platformRaw("admin","POST","",platformForm("same")).statusCode())).containsExactlyInAnyOrder(201,409);
  var stale=platformEdit(d);stale.remove("version");platform("admin","PUT",id,stale,400);
 }
 @Test void platformLegacyRowsRemainReadableAndFirstModificationCapturesTheirBaseline()throws Exception {
  execute("INSERT INTO ba_plateforme(id,code,nom,description,created_by,created_date,version) VALUES ('old-platform','OLD','Ancienne plateforme','Usage existant','test',now(),7)");
  var old=platform("admin","GET","old-platform",null,200);assertThat(old.at("/fiche/cycleVie").asText()).isEqualTo("A_QUALIFIER");assertThat(old.at("/fiche/criticite").asText()).isEqualTo("A_QUALIFIER");
  assertThat(platform("admin","GET","?cycleVie=A_QUALIFIER",null,200).path("totalElements").asInt()).isEqualTo(1);
  var form=platformEdit(old);form.put("fiche",Map.of("cycleVie","EN_SERVICE","contactSupport","Support informatique"));platform("admin","PUT","old-platform",form,200);
  var h=platform("admin","GET","old-platform/historique",null,200);assertThat(h.size()).isEqualTo(2);assertThat(h.at("/1/action").asText()).isEqualTo("REPRISE");assertThat(h.at("/1/numero").asLong()).isEqualTo(7);assertThat(h.at("/1/snapshot/description").asText()).isEqualTo("Usage existant");
 }
 @Test void platformLegacyApiCannotBypassCataloguePermissionsOrEraseDetailedMetadata()throws Exception {
  var d=platform("admin","POST","",platformForm("LEGACY"),201);String id=d.path("id").asText();var form=platformEdit(d);form.put("fiche",Map.of("fonctionnalites","Fonctions détaillées"));d=platform("admin","PUT",id,form,200);
  var features=app.getBean(com.bakouan.app.config.FeatureConfig.class);features.setHabilitationsEnabled(true);
  try {
   var oldForm=Map.of("code","LEGACY","nom","Nouveau nom","description","Description","typePlateforme","CORE_BANKING","version",d.path("version").asLong());
   business("agent","PUT","habilitations/plateformes/"+id,oldForm,403);business("agent","POST","habilitations/plateformes",oldForm,403);
   business("admin","PUT","habilitations/plateformes/"+id,oldForm,200);
   var current=platform("admin","GET",id,null,200);assertThat(current.path("nom").asText()).isEqualTo("Nouveau nom");assertThat(current.at("/fiche/fonctionnalites").asText()).isEqualTo("Fonctions détaillées");
   business("admin","PUT","habilitations/plateformes/"+id,oldForm,409);
   assertThat(business("agent","GET","habilitations/plateformes",null,200).findValuesAsText("id")).contains(id);
  } finally {features.setHabilitationsEnabled(false);}
 }
 @Test void platformConcurrentArchivingAndNewDependencyCannotLeaveAnActiveDanglingDependency()throws Exception {
  var dep=platform("admin","POST","",platformForm("DEPENDENCY"),201);String depId=dep.path("id").asText();var consumer=platform("admin","POST","",platformForm("CONSUMER"),201);var form=platformEdit(consumer);form.put("dependanceIds",List.of(depId));
  var results=race(()->platformRaw("admin","POST",depId+"/archiver",Map.of("version",dep.path("version").asLong(),"motif","Archivage concurrent")).statusCode(),()->platformRaw("admin","PUT",consumer.path("id").asText(),form).statusCode());
  assertThat(results).contains(200);assertThat(results.stream().filter(code->code==200).count()).isEqualTo(1);assertThat(results).allMatch(code->Set.of(200,400,409).contains(code));
  try(var q=sql.createStatement();var r=q.executeQuery("SELECT count(*) FROM ba_plateforme_dependance d JOIN ba_plateforme p ON p.id=d.plateforme_id JOIN ba_plateforme target ON target.id=d.dependance_id WHERE p.statut='A' AND target.statut='D'")){assertThat(r.next()).isTrue();assertThat(r.getInt(1)).isZero();}
  assertThat(platform("admin","GET","?recherche=%25",null,200).path("totalElements").asInt()).isZero();
 }
 @Test void platformManuelsCanBeUploadedWithCreationAndDownloadedPrivately()throws Exception {
  byte[] pdf=PlateformeManuelValidatorTest.pdf("Guide utilisateur");var d=platformMultipart("admin","POST","",platformForm("MANUAL"),List.of(Map.of("titre","Guide agent","description","Version française")),List.of(),List.of(pdf),List.of("manuel.pdf"),201);String id=d.path("id").asText(),manualId=d.at("/manuels/0/id").asText();
  assertThat(d.path("manuels").size()).isEqualTo(1);assertThat(d.at("/manuels/0/titre").asText()).isEqualTo("Guide agent");assertThat(d.at("/manuels/0/sha256").asText()).hasSize(64);assertThat(d.at("/manuels/0").has("contenu")).isFalse();
  var file=manualDownload("admin",id,manualId);assertThat(file.statusCode()).isEqualTo(200);assertThat(file.body()).isEqualTo(pdf);assertThat(file.headers().firstValue("Content-Type")).contains("application/pdf");assertThat(file.headers().firstValue("Content-Disposition").orElse("")).startsWith("attachment;");assertThat(file.headers().firstValue("Cache-Control")).contains("no-store");assertThat(file.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
  assertThat(manualDownload(null,id,manualId).statusCode()).isEqualTo(401);assertThat(manualDownload("agent",id,manualId).statusCode()).isEqualTo(403);
  platformRole("agent","PLATEFORMES_CONSULTER");assertThat(manualDownload("agent",id,manualId).statusCode()).isEqualTo(200);
  var second=platform("admin","POST","",platformForm("SECOND"),201);assertThat(manualDownload("agent",second.path("id").asText(),manualId).statusCode()).isEqualTo(404);
  var history=platform("admin","GET",id+"/historique",null,200);assertThat(history.at("/0/snapshot/manuels/0/id").asText()).isEqualTo(manualId);assertThat(history.toString()).doesNotContain("JVBERi0","contenu");
  execute("DELETE FROM ba_user_roles WHERE user_id='agent' AND role_id='role-plateformes-consulter'");assertThat(manualDownload("agent",id,manualId).statusCode()).isEqualTo(403);
 }
 @Test void platformManuelsSupportPdfAndDocxAndPreserveDownloadsInHistoryAfterRemoval()throws Exception {
  var d=platformMultipart("admin","POST","",platformForm("TWO"),List.of(Map.of("titre","Guide PDF"),Map.of("titre","Guide Word")),List.of(),List.of(PlateformeManuelValidatorTest.pdf("PDF"),PlateformeManuelValidatorTest.docx(false,false)),List.of("guide.pdf","guide.docx"),201);String id=d.path("id").asText();
  assertThat(d.path("manuels").size()).isEqualTo(2);String removed=d.at("/manuels/0/id").asText();
  var input=platformEdit(d);var updated=platformMultipart("admin","PUT",id,input,List.of(),List.of(removed),List.of(),List.of(),200);assertThat(updated.path("manuels").size()).isEqualTo(1);assertThat(updated.path("version").asLong()).isEqualTo(d.path("version").asLong()+1);
  var history=platform("admin","GET",id+"/historique",null,200);assertThat(history.at("/1/snapshot/manuels").size()).isEqualTo(2);assertThat(manualDownload("admin",id,removed).statusCode()).isEqualTo(200);
  platform("admin","POST",id+"/archiver",Map.of("version",updated.path("version").asLong(),"motif","Archivage"),200);assertThat(manualDownload("admin",id,removed).statusCode()).isEqualTo(200);
  // Compatibilité des copies historiques créées avant V1.29.
  execute("UPDATE ba_plateforme_revision SET snapshot=snapshot-'manuels' WHERE plateforme_id='"+id+"'");assertThat(platform("admin","GET",id+"/historique",null,200).at("/0/snapshot/manuels").size()).isZero();
 }
 @Test void platformManuelsInvalidUploadsRollbackTheWholeCreationOrEdit()throws Exception {
  platformMultipart("admin","POST","",platformForm("INVALID"),List.of(Map.of("titre","Faux PDF")),List.of(),List.of("<html>fake</html>".getBytes()),List.of("fake.pdf"),400);
  assertThat(platform("admin","GET","",null,200).path("totalElements").asInt()).isZero();
  var pdf=PlateformeManuelValidatorTest.pdf("Same manual");var d=platformMultipart("admin","POST","",platformForm("DUP"),List.of(Map.of("titre","Guide")),List.of(),List.of(pdf),List.of("guide.pdf"),201);String id=d.path("id").asText();var input=platformEdit(d);input.put("nom","Ne doit pas être enregistré");
  platformMultipart("admin","PUT",id,input,List.of(Map.of("titre","Copie")),List.of(),List.of(pdf),List.of("copy.pdf"),409);
  var current=platform("admin","GET",id,null,200);assertThat(current.path("nom").asText()).isEqualTo(d.path("nom").asText());assertThat(current.path("version").asLong()).isEqualTo(d.path("version").asLong());assertThat(current.path("manuels").size()).isEqualTo(1);assertThat(platform("admin","GET",id+"/historique",null,200).size()).isEqualTo(1);
  platformMultipart("admin","PUT",id,input,List.of(),List.of("foreign-manual"),List.of(),List.of(),400);assertThat(platform("admin","GET",id,null,200).path("version").asLong()).isEqualTo(d.path("version").asLong());
 }
 @Test void platformManuelsRequireManagementPermissionAndRejectStaleConcurrentEdits()throws Exception {
  var d=platform("admin","POST","",platformForm("RACE-MANUAL"),201);String id=d.path("id").asText();var input=platformEdit(d);
  platformRole("agent","PLATEFORMES_CONSULTER");platformMultipart("agent","PUT",id,input,List.of(Map.of("titre","Guide")),List.of(),List.of(PlateformeManuelValidatorTest.pdf("A")),List.of("guide.pdf"),403);
  var results=race(()->platformMultipartRaw("admin","PUT",id,input,List.of(Map.of("titre","Guide A")),List.of(),List.of(PlateformeManuelValidatorTest.pdf("A")),List.of("guide-a.pdf")).statusCode(),()->platformMultipartRaw("admin","PUT",id,input,List.of(Map.of("titre","Guide B")),List.of(),List.of(PlateformeManuelValidatorTest.pdf("B")),List.of("guide-b.pdf")).statusCode());
  assertThat(results).containsExactlyInAnyOrder(200,409);var current=platform("admin","GET",id,null,200);assertThat(current.path("manuels").size()).isEqualTo(1);assertThat(current.path("version").asLong()).isEqualTo(d.path("version").asLong()+1);assertThat(platform("admin","GET",id+"/historique",null,200).size()).isEqualTo(2);
 }
 @Test void platformManuelsEnforceMultipartLimitsAndMetadataConsistency()throws Exception {
  var form=platformForm("LIMIT");platformMultipart("admin","POST","",form,List.of(),List.of(),List.of(PlateformeManuelValidatorTest.pdf("A")),List.of("a.pdf"),400);
  platformMultipart("admin","POST","",form,List.of(Map.of("titre","")),List.of(),List.of(PlateformeManuelValidatorTest.pdf("A")),List.of("a.pdf"),400);
  platformMultipart("admin","POST","",form,List.of(Map.of("titre","A"),Map.of("titre","B")),List.of(),List.of(PlateformeManuelValidatorTest.pdf("Same"),PlateformeManuelValidatorTest.pdf("Same")),List.of("a.pdf","b.pdf"),400);
  platformMultipart("admin","POST","",form,List.of(Map.of("titre","Trop volumineux")),List.of(),List.of(new byte[11*1024*1024]),List.of("large.pdf"),413);
  assertThat(platform("admin","GET","",null,200).path("totalElements").asInt()).isZero();
 }
 private HttpResponse<byte[]> manualDownload(String actor,String platformId,String manualId)throws Exception {
  var request=HttpRequest.newBuilder(URI.create(base+"/api/plateformes/"+platformId+"/manuels/"+manualId));if(actor!=null)request.header("Authorization","Bearer "+tokens.get(actor));return http.send(request.GET().build(),HttpResponse.BodyHandlers.ofByteArray());
 }
 private JsonNode platformMultipart(String actor,String method,String path,Object form,List<?> metadata,List<String> removals,List<byte[]> files,List<String> names,int expected)throws Exception{return status(platformMultipartRaw(actor,method,path,form,metadata,removals,files,names),expected);}
 private HttpResponse<String> platformMultipartRaw(String actor,String method,String path,Object form,List<?> metadata,List<String> removals,List<byte[]> files,List<String> names)throws Exception {
  String boundary="bsic-manual-"+UUID.randomUUID();var out=new java.io.ByteArrayOutputStream();
  multipartPart(out,boundary,"fiche",null,"application/json",json.writeValueAsBytes(form));multipartPart(out,boundary,"manuels",null,"application/json",json.writeValueAsBytes(Map.of("ajouts",metadata,"retraits",removals)));
  for(int i=0;i<files.size();i++)multipartPart(out,boundary,"fichiers",names.get(i),"application/octet-stream",files.get(i));out.write(("--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var request=HttpRequest.newBuilder(URI.create(base+"/api/plateformes"+(path.isEmpty()?"":"/")+path)).header("Content-Type","multipart/form-data; boundary="+boundary).header("Accept","application/json");if(actor!=null)request.header("Authorization","Bearer "+tokens.get(actor));return http.send(request.method(method,HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
 }
 private void multipartPart(java.io.ByteArrayOutputStream out,String boundary,String name,String filename,String mime,byte[] bytes)throws Exception {
  out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+name+"\""+(filename==null?"":"; filename=\""+filename+"\"")+"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(bytes);out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }

 private LinkedHashMap<String,Object> platformForm(String code){var data=new LinkedHashMap<String,Object>();data.put("code",code);data.put("nom","Plateforme "+code);data.put("description","Description de recette");data.put("typePlateforme","RESEAUX");data.put("fiche",Map.of());data.put("dependanceIds",List.of());data.put("motif","Recette du catalogue");return data;}
 private LinkedHashMap<String,Object> platformEdit(JsonNode d){var data=platformForm(d.path("code").asText());data.put("nom",d.path("nom").asText());data.put("version",d.path("version").asLong());data.put("fiche",json.convertValue(d.path("fiche"),Map.class));data.put("dependanceIds",d.path("dependances").findValuesAsText("id"));data.put("idCircuit",d.at("/circuit/id").asText(""));data.put("departementId",d.at("/departement/id").asText(""));data.put("responsableMetierId",d.at("/responsableMetier/id").asText(""));data.put("responsableTechniqueId",d.at("/responsableTechnique/id").asText(""));return data;}
 private void platformRole(String actor,String role)throws Exception{execute("INSERT INTO ba_user_roles(user_id,role_id) SELECT '"+actor+"',id FROM ba_role WHERE code='"+role+"' ON CONFLICT DO NOTHING");}
 @Test void agentRegistrationAutomaticallyPersistsChiefAsN1AndDepartmentDirectorAsN2()throws Exception{
  var d=agentAccount("admin","POST","",agentAccountForm(),201);
  assertThat(d.path("idSuperieur").asText()).isEqualTo("chief");assertThat(d.path("idSuperieurSecondaire").asText()).isEqualTo("director");
  assertThat(d.path("nomCompletSuperieur").asText()).contains("chief");assertThat(d.path("nomCompletSuperieurSecondaire").asText()).contains("director");
  assertThat(d.path("passwordChangeRequired").asBoolean()).isTrue();assertAgentHierarchy(d.path("id").asText(),"chief","director");
  agentAccount("agent","POST","",agentAccountForm(),403);var allowed=agentAccount("chief","POST","",agentAccountForm(),201);assertAgentHierarchy(allowed.path("id").asText(),"chief","director");
 }
 @Test void agentRegistrationAllowsVacanciesAndRequiresExplicitChoiceToResolveAmbiguity()throws Exception{
  execute("UPDATE ba_utilisateur SET account_locked=true WHERE id='chief'");execute("UPDATE ba_utilisateur SET activated=false WHERE id='director'");
  var d=agentAccount("admin","POST","",agentAccountForm(),201);assertAgentHierarchy(d.path("id").asText(),null,null);
  execute("UPDATE ba_utilisateur SET account_locked=false WHERE id='chief'");execute("UPDATE ba_utilisateur SET activated=true WHERE id='director'");fixture("other-chief","CHEF_SERVICE","dept","s1");fixture("other-director","DIRECTEUR","dept",null);
  d=agentAccount("admin","POST","",agentAccountForm(),201);assertAgentHierarchy(d.path("id").asText(),null,null);
  var input=agentAccountForm();input.put("idSuperieur","other-chief");input.put("idSuperieurSecondaire","other-director");d=agentAccount("admin","POST","",input,201);assertAgentHierarchy(d.path("id").asText(),"other-chief","other-director");
 }
 @Test void agentRegistrationRejectsForeignInactiveLockedOrTechnicalSuperiorsViaDirectApi()throws Exception{
  for(String id:List.of("peer","foreign","admin","agent")){var input=agentAccountForm();input.put("idSuperieur",id);agentAccount("admin","POST","",input,400);}
  execute("UPDATE ba_utilisateur SET account_locked=true WHERE id='chief'");var input=agentAccountForm();input.put("idSuperieur","chief");agentAccount("admin","POST","",input,400);
  execute("UPDATE ba_utilisateur SET activated=false WHERE id='director'");input=agentAccountForm();input.put("idSuperieurSecondaire","director");agentAccount("admin","POST","",input,400);
  input=agentAccountForm();input.put("idSuperieur","director");input.put("idSuperieurSecondaire","director");agentAccount("admin","POST","",input,400);
 }
 @Test void agentUpdatesCanonicalizeOldOrderAndRecomputeHierarchyAfterReassignmentWithoutResettingAccess()throws Exception{
  var input=agentAccountForm();input.put("idSuperieur","director");input.put("idSuperieurSecondaire","chief");var d=agentAccount("admin","POST","",input,201);String id=d.path("id").asText();assertAgentHierarchy(id,"chief","director");input.put("roles",json.convertValue(d.path("roles"),List.class));
  // Simuler une fiche historique inversée : elle n'est corrigée que lors de sa modification.
  execute("UPDATE ba_utilisateur SET superieur_id='director',superieur_secondaire_id='chief' WHERE id='"+id+"'");agentAccount("admin","PUT",id,input,200);assertAgentHierarchy(id,"chief","director");
  input.put("idSuperieur",id);input.remove("idSuperieurSecondaire");agentAccount("admin","PUT",id,input,400);input.remove("idSuperieur");input.put("idSuperieurSecondaire",id);agentAccount("admin","PUT",id,input,400);assertAgentHierarchy(id,"chief","director");
  execute("INSERT INTO ba_service(id,nom,departement_id,created_by,created_date) VALUES ('foreign-service','Service externe','foreign','test',now())");execute("UPDATE ba_utilisateur SET service_id='foreign-service' WHERE id='foreign'");fixture("foreign-director","DIRECTEUR","foreign",null);
  input.remove("idSuperieurSecondaire");input.put("idDepartement","foreign");input.put("idService","foreign-service");agentAccount("admin","PUT",id,input,200);assertAgentHierarchy(id,"foreign","foreign-director");
  try(var p=sql.prepareStatement("SELECT credential_version,password_change_required,activated FROM ba_utilisateur WHERE id=?")){p.setString(1,id);try(var rows=p.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getLong(1)).isEqualTo(1);assertThat(rows.getBoolean(2)).isTrue();assertThat(rows.getBoolean(3)).isTrue();}}
 }
 private LinkedHashMap<String,Object> agentAccountForm(){var b=new LinkedHashMap<String,Object>();String key=UUID.randomUUID().toString().substring(0,20);b.put("nom","Agent");b.put("prenom","Recette");b.put("email",key+"@example.test");b.put("telephone",key);b.put("sexe","MASCULIN");b.put("fonction","AGENT");b.put("idDepartement","dept");b.put("idService","s1");return b;}
 private JsonNode agentAccount(String actor,String method,String id,Object body,int expected)throws Exception{
  var request=HttpRequest.newBuilder(URI.create(base+"/api/users"+(id.isEmpty()?"":"/"+id))).header("Content-Type","application/json").header("Accept","application/json").header("Authorization","Bearer "+tokens.get(actor)).method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
  var response=http.send(request,HttpResponse.BodyHandlers.ofString());assertThat(response.statusCode()).as("HTTP status de la gestion d'agent").isEqualTo(expected);
  return response.body().isBlank()||!response.headers().firstValue("content-type").orElse("").contains("json")?json.createObjectNode():json.readTree(response.body());
 }
 private void assertAgentHierarchy(String id,String chief,String director)throws Exception{try(var p=sql.prepareStatement("SELECT superieur_id,superieur_secondaire_id FROM ba_utilisateur WHERE id=?")){p.setString(1,id);try(var rows=p.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo(chief);assertThat(rows.getString(2)).isEqualTo(director);}}}
 @Test void extractionLibraryRegistersValidatedQueriesDirectlyAndKeepsEditionHistory()throws Exception{
  var q=oracleQuery();String path="requetes/"+q.at("/fiche/id").asText();
  assertThat(q.at("/fiche/verifiee").asBoolean()).isTrue();assertThat(q.at("/fiche/dateValidation").asText()).isEqualTo(LocalDate.now().toString());
  assertThat(q.at("/fiche/auteur").asText()).isEqualTo("admin");assertThat(q.path("executions").size()).isZero();
  assertThat(q.path("fiche").has("code")).isFalse();assertThat(q.path("fiche").has("plateforme")).isFalse();
  assertThat(q.at("/revisions/0/contenu").has("plateformeId")).isFalse();assertThat(extraction("admin","GET","configuration",null,200).has("plateformes")).isFalse();
  platformRole("chief","REQUETES_CONSULTER");assertThat(extraction("chief","GET","requetes",null,200).path("totalElements").asInt()).isEqualTo(1);
  var published=extraction("chief","GET",path,null,200);assertThat(published.path("sqlTexte").asText()).isEqualTo("SELECT 1 FROM dual");extraction("chief","GET","requetes?brouillons=true",null,403);
  var edit=oracleEdit(q);edit.put("sqlTexte","SELECT 2 FROM dual");edit.remove("motif");q=extraction("admin","PUT",path,edit,200);
  assertThat(q.at("/fiche/edition").asInt()).isEqualTo(2);assertThat(q.at("/fiche/verifiee").asBoolean()).isTrue();assertThat(q.at("/revisions/1/contenu/sqlTexte").asText()).isEqualTo("SELECT 1 FROM dual");
  assertThat(q.path("executions").size()).isZero();assertThat(extraction("chief","GET",path,null,200).path("revisions").size()).isEqualTo(1);
  var duplicate=oracleQuery();assertThat(duplicate.at("/fiche/id").asText()).isNotEqualTo(q.at("/fiche/id").asText());assertThat(duplicate.at("/fiche/titre").asText()).isEqualTo(q.at("/fiche/titre").asText());
  assertThat(extraction("chief","GET","requetes?recherche=fictive",null,200).path("totalElements").asInt()).isEqualTo(2);
 }
 @Test void extractionKeywordIsSavedSearchedAndFrozenInEachEdition()throws Exception{
  var q=oracleQuery();String path="requetes/"+q.at("/fiche/id").asText();assertThat(q.at("/fiche/motCle").asText()).isEmpty();
  var edit=oracleEdit(q);edit.put("motCle","  Rapprochement trésorerie  ");q=extraction("admin","PUT",path,edit,200);
  assertThat(q.at("/fiche/motCle").asText()).isEqualTo("Rapprochement trésorerie");assertThat(q.at("/revisions/0/contenu/motCle").asText()).isEqualTo("Rapprochement trésorerie");
  platformRole("chief","REQUETES_CONSULTER");
  var rows=extraction("chief","GET","requetes?recherche=RAPPROCHEMENT&departementId=dept&page=0&size=1",null,200);
  assertThat(rows.path("totalElements").asInt()).isEqualTo(1);assertThat(rows.at("/content/0/id").asText()).isEqualTo(q.at("/fiche/id").asText());assertThat(rows.at("/content/0/motCle").asText()).isEqualTo("Rapprochement trésorerie");
  assertThat(extraction("chief","GET","requetes?recherche=rapprochement&departementId=foreign",null,200).path("totalElements").asInt()).isZero();
  assertThat(extraction("chief","GET","requetes?recherche=inexistant",null,200).path("totalElements").asInt()).isZero();
  assertThat(extraction("chief","GET","requetes?recherche=fictive",null,200).path("totalElements").asInt()).isEqualTo(1);
  assertThat(extraction("chief","GET","requetes?recherche=fonctionnel",null,200).path("totalElements").asInt()).isEqualTo(1);
  edit=oracleEdit(q);edit.put("motCle","Liquidité");q=extraction("admin","PUT",path,edit,200);
  assertThat(q.at("/revisions/1/contenu/motCle").asText()).isEqualTo("Rapprochement trésorerie");assertThat(q.at("/revisions/0/contenu/motCle").asText()).isEqualTo("Liquidité");
  assertThat(extraction("chief","GET","requetes?recherche=rapprochement",null,200).path("totalElements").asInt()).isZero();
  edit=oracleEdit(q);edit.put("motCle","x".repeat(201));extraction("admin","PUT",path,edit,400);
  assertThat(extraction("chief","GET",path,null,200).at("/fiche/motCle").asText()).isEqualTo("Liquidité");
  execute("UPDATE ba_requete_revision SET contenu=contenu-'motCle' WHERE requete_id='"+q.at("/fiche/id").asText()+"'");
  assertThat(extraction("admin","GET",path,null,200).at("/revisions/0/contenu/motCle").asText()).isEmpty();
 }
 @Test void extractionKeywordSearchEscapesWildcardsAndPreservesVisibility()throws Exception{
  var q=oracleQuery();String path="requetes/"+q.at("/fiche/id").asText();var edit=oracleEdit(q);edit.put("motCle","Contrôle_100%\\lot");q=extraction("admin","PUT",path,edit,200);oracleQuery();
  platformRole("chief","REQUETES_CONSULTER");
  for(String search:List.of("%","_","\\"))assertThat(extraction("chief","GET","requetes?recherche="+URLEncoder.encode(search,java.nio.charset.StandardCharsets.UTF_8),null,200).path("totalElements").asInt()).isEqualTo(1);
  var page=extraction("chief","GET","requetes?size=1&page=1",null,200);assertThat(page.path("totalElements").asInt()).isEqualTo(2);assertThat(page.path("totalPages").asInt()).isEqualTo(2);assertThat(page.path("content").size()).isEqualTo(1);
  extraction("agent","GET","requetes?recherche=100",null,403);
  execute("UPDATE ba_requete_oracle SET verifiee=false WHERE id='"+q.at("/fiche/id").asText()+"'");
  assertThat(extraction("chief","GET","requetes?recherche=100",null,200).path("totalElements").asInt()).isZero();
  extraction("chief","GET","requetes?recherche=100&brouillons=true",null,403);
  assertThat(extraction("admin","GET","requetes?recherche=100&brouillons=true",null,200).path("totalElements").asInt()).isEqualTo(1);
  q=extraction("admin","POST",path+"/archiver",extractOp(q),200);
  assertThat(extraction("chief","GET","requetes?recherche=100",null,200).path("totalElements").asInt()).isZero();
  extraction("chief","GET","requetes?recherche=100&archivees=true",null,403);
  assertThat(extraction("admin","GET","requetes?recherche=100&archivees=true",null,200).path("totalElements").asInt()).isEqualTo(1);
 }
 @Test void extractionRegistrationRequiresValidationDateAndKeepsOptionalMetadataOptional()throws Exception{
  var q=oracleQuery();var body=oracleEdit(q);body.remove("version");body.remove("dateValidation");extraction("admin","POST","requetes",body,400);
  body.put("dateValidation",LocalDate.now().plusDays(1));extraction("admin","POST","requetes",body,400);
  body.put("dateValidation",LocalDate.now().minusDays(7));body.remove("schemaCible");body.remove("parametres");body.remove("motif");body.put("sqlTexte","SELECT :date_debut FROM dual");body.put("auteur","Imposteur");
  var registered=extraction("admin","POST","requetes",body,201);assertThat(registered.at("/fiche/verifiee").asBoolean()).isTrue();assertThat(registered.at("/fiche/auteur").asText()).isEqualTo("admin");
  assertThat(registered.path("parametres").size()).isZero();assertThat(registered.path("executions").size()).isZero();assertThat(registered.at("/fiche/dateValidation").asText()).isEqualTo(LocalDate.now().minusDays(7).toString());
  platformRole("chief","REQUETES_CONSULTER");extraction("chief","POST","requetes",body,403);
 }
 @Test void extractionDepartmentIsRequiredActiveAndFrozenInEachEdition()throws Exception{
  var q=oracleQuery();var body=oracleEdit(q);body.remove("version");body.remove("departementId");extraction("admin","POST","requetes",body,400);
  body.put("departementId","missing");extraction("admin","POST","requetes",body,400);execute("UPDATE ba_departement SET statut='D' WHERE id='foreign'");body.put("departementId","foreign");extraction("admin","POST","requetes",body,400);
  execute("INSERT INTO ba_departement(id,nom,created_by,created_date) VALUES ('empty-dep','Sans agents','test',now())");assertThat(extraction("admin","GET","configuration",null,200).path("departements").findValuesAsText("id")).contains("empty-dep").doesNotContain("foreign");
  body.put("departementId","empty-dep");body.put("departementNom","Imposteur");var d=extraction("admin","POST","requetes",body,201);String path="requetes/"+d.at("/fiche/id").asText();assertThat(d.at("/fiche/departementDemandeur/nom").asText()).isEqualTo("Sans agents");
  assertThat(d.at("/revisions/0/contenu/departementId").asText()).isEqualTo("empty-dep");execute("UPDATE ba_departement SET nom='Renommé' WHERE id='empty-dep'");d=extraction("admin","GET",path,null,200);assertThat(d.at("/fiche/departementDemandeur/nom").asText()).isEqualTo("Sans agents");
  var change=oracleEdit(d);change.put("departementId","dept");d=extraction("admin","PUT",path,change,200);assertThat(d.at("/fiche/departementDemandeur/id").asText()).isEqualTo("dept");assertThat(d.at("/revisions/1/contenu/departementNom").asText()).isEqualTo("Sans agents");
  platformRole("chief","REQUETES_CONSULTER");assertThat(extraction("chief","GET","requetes?departementId=dept",null,200).path("totalElements").asInt()).isEqualTo(2);assertThat(extraction("chief","GET","requetes?departementId=empty-dep",null,200).path("totalElements").asInt()).isZero();
 }
 @Test void extractionConfigurationProvidesOnlyEligibleServicesAndConsistentRequesterDefaults()throws Exception{
  platformRole("agent","EXTRACTIONS_DEMANDER");
  execute("INSERT INTO ba_service(id,nom,departement_id,statut,created_by,created_date) VALUES ('foreign-service','Service externe','foreign','A','test',now()),('inactive-service','Service inactif','dept','D','test',now())");
  var c=extraction("agent","GET","configuration",null,200);assertThat(c.path("services").findValuesAsText("id")).containsExactly("s1","s2");assertThat(c.at("/service/id").asText()).isEqualTo("s1");assertThat(c.path("demandeurs").size()).isZero();
  c=extraction("admin","GET","configuration",null,200);assertThat(c.path("services").findValuesAsText("id")).contains("s1","s2","foreign-service").doesNotContain("inactive-service");
  assertThat(c.path("demandeurs").findValuesAsText("id")).contains("agent");
  var agent=java.util.stream.StreamSupport.stream(c.path("demandeurs").spliterator(),false).filter(u->u.path("id").asText().equals("agent")).findFirst().orElseThrow();assertThat(agent.at("/service/id").asText()).isEqualTo("s1");
  execute("UPDATE ba_utilisateur SET service_id='foreign-service' WHERE id='agent'");assertThat(extraction("agent","GET","configuration",null,200).path("service").isNull()).isTrue();
  execute("UPDATE ba_departement SET statut='D' WHERE id='foreign'");assertThat(extraction("admin","GET","configuration",null,200).path("services").findValuesAsText("id")).doesNotContain("foreign-service");
 }
 @Test void extractionQueryServiceMustBeActiveInChosenDepartmentAndIsCanonical()throws Exception{
  var q=oracleQuery();String path="requetes/"+q.at("/fiche/id").asText();var body=oracleEdit(q);
  execute("INSERT INTO ba_service(id,nom,departement_id,statut,created_by,created_date) VALUES ('foreign-service','Service externe','foreign','A','test',now()),('inactive-service','Service inactif','dept','D','test',now())");
  for(String id:List.of("missing","foreign-service","inactive-service")){body.put("serviceId",id);assertThat(extraction("admin","PUT",path,body,400).path("message").asText()).contains("service");}
  q=extraction("admin","GET",path,null,200);assertThat(q.path("revisions").size()).isEqualTo(1);assertThat(q.at("/fiche/serviceDemandeur").isNull()).isTrue();
  body.put("serviceId","s2");body.put("serviceNom","Nom usurpé");q=extraction("admin","PUT",path,body,200);assertThat(q.at("/fiche/serviceDemandeur/id").asText()).isEqualTo("s2");assertThat(q.at("/fiche/serviceDemandeur/nom").asText()).isEqualTo("Service 2");assertThat(q.at("/revisions/0/contenu/serviceNom").asText()).isEqualTo("Service 2");
  assertThat(q.at("/revisions/1/contenu/serviceId").isNull()).isTrue();
  body=oracleEdit(q);body.put("departementId","foreign");extraction("admin","PUT",path,body,400);body.put("serviceId","foreign-service");q=extraction("admin","PUT",path,body,200);assertThat(q.at("/fiche/serviceDemandeur/id").asText()).isEqualTo("foreign-service");assertThat(q.at("/revisions/1/contenu/serviceId").asText()).isEqualTo("s2");
 }
 @Test void extractionRequestServiceUsesDerivedDepartmentAndAllowsWholeDepartment()throws Exception{
  platformRole("agent","EXTRACTIONS_DEMANDER");execute("INSERT INTO ba_service(id,nom,departement_id,statut,created_by,created_date) VALUES ('foreign-service','Externe','foreign','A','test',now()),('inactive-service','Inactif','dept','D','test',now())");
  var body=extractionForm("agent");body.put("departementId","foreign");body.put("serviceNom","Nom usurpé");
  for(String id:List.of("missing","foreign-service","inactive-service")){body.put("serviceId",id);extraction("agent","POST","demandes",body,400);}
  assertThat(extraction("admin","GET","demandes",null,200).path("totalElements").asInt()).isZero();body.put("serviceId","s2");var d=extraction("agent","POST","demandes",body,201);String path="demandes/"+d.at("/fiche/id").asText();assertThat(d.at("/fiche/departement/id").asText()).isEqualTo("dept");assertThat(d.at("/fiche/service/nom").asText()).isEqualTo("Service 2");
  body.put("demandeurId","foreign");extraction("agent","POST","demandes",body,403);extraction("admin","POST","demandes",body,400);body.put("serviceId","foreign-service");assertThat(extraction("admin","POST","demandes",body,201).at("/fiche/service/id").asText()).isEqualTo("foreign-service");
  var without=extraction("agent","POST","demandes",extractionForm("agent"),201);assertThat(without.at("/fiche/service").isNull()).isTrue();
  execute("UPDATE ba_service SET nom='Renommé',departement_id='foreign',statut='D' WHERE id='s2'");execute("UPDATE ba_utilisateur SET departement_id='foreign',service_id=NULL WHERE id='agent'");
  d=extraction("agent","GET",path,null,200);assertThat(d.at("/fiche/service/id").asText()).isEqualTo("s2");assertThat(d.at("/fiche/service/nom").asText()).isEqualTo("Service 2");assertThat(d.at("/fiche/departement/id").asText()).isEqualTo("dept");
  assertThat(extraction("agent","GET","demandes/"+without.at("/fiche/id").asText(),null,200).at("/fiche/service").isNull()).isTrue();
 }
 @Test void extractionServiceSnapshotsSurviveOrganigramChangesAndQueryEditions()throws Exception{
  var q=oracleQuery();var body=oracleEdit(q);body.put("serviceId","s1");String qp="requetes/"+q.at("/fiche/id").asText();q=extraction("admin","PUT",qp,body,200);
  body=extractionForm("agent");body.put("serviceId","s2");var d=extraction("admin","POST","demandes",body,201);String dp="demandes/"+d.at("/fiche/id").asText();d=extraction("admin","POST",dp+"/prendre",Map.of("version",d.at("/fiche/version").asLong(),"requeteId",q.at("/fiche/id").asText(),"motif","Traitement"),200);
  execute("UPDATE ba_service SET nom='Service déplacé',departement_id='foreign',statut='D' WHERE id='s1'");
  q=extraction("admin","GET",qp,null,200);assertThat(q.at("/fiche/serviceDemandeur/nom").asText()).isEqualTo("Service 1");assertThat(q.at("/revisions/0/contenu/serviceNom").asText()).isEqualTo("Service 1");
  body=oracleEdit(q);body.put("serviceId","s2");q=extraction("admin","PUT",qp,body,200);assertThat(q.at("/fiche/serviceDemandeur/nom").asText()).isEqualTo("Service 2");
  d=extraction("admin","GET",dp,null,200);assertThat(d.at("/requete/contenu/serviceId").asText()).isEqualTo("s1");assertThat(d.at("/requete/contenu/serviceNom").asText()).isEqualTo("Service 1");assertThat(d.at("/fiche/service/id").asText()).isEqualTo("s2");assertThat(d.at("/fiche/edition").asInt()).isEqualTo(2);
  assertThat(extraction("admin","GET","requetes",null,200).at("/content/0/serviceDemandeur/nom").asText()).isEqualTo("Service 2");
 }
 @Test void extractionHistoricalSnapshotsRemainReadableWithoutInventedValidationDates()throws Exception{
  var q=oracleQuery();String id=q.at("/fiche/id").asText();
  execute("UPDATE ba_requete_revision SET contenu=(contenu-'dateValidation') || '{\"code\":\"OLD-QUERY\",\"plateformeId\":\"old-platform\"}'::jsonb WHERE requete_id='"+id+"'");
  execute("UPDATE ba_requete_oracle SET date_validation=NULL WHERE id='"+id+"'");
  q=extraction("admin","GET","requetes/"+id,null,200);assertThat(q.at("/fiche/dateValidation").isNull()).isTrue();assertThat(q.at("/revisions/0/contenu/sqlTexte").asText()).isEqualTo("SELECT 1 FROM dual");
  platformRole("chief","REQUETES_CONSULTER");extraction("chief","GET","requetes/"+id,null,200);
  var d=extraction("admin","POST","demandes",extractionForm("agent"),201);d=extraction("admin","POST","demandes/"+d.at("/fiche/id").asText()+"/prendre",Map.of("version",d.at("/fiche/version").asLong(),"requeteId",id,"motif","Recette historique"),200);
  assertThat(d.at("/fiche/requeteTitre").asText()).isEqualTo("Extraction fictive");assertThat(d.at("/requete/contenu/sqlTexte").asText()).isEqualTo("SELECT 1 FROM dual");
 }
 @Test void extractionRequestFreezesIdentityDepartmentAndSuccessfulSqlEdition()throws Exception{
  platformRole("agent","EXTRACTIONS_DEMANDER");var q=oracleReady();String qp="requetes/"+q.at("/fiche/id").asText();
  var input=extractionForm("agent");input.put("departementId","foreign");input.put("auteur","Imposteur");var d=extraction("agent","POST","demandes",input,201);String dp="demandes/"+d.at("/fiche/id").asText();
  assertThat(d.at("/fiche/departement/id").asText()).isEqualTo("dept");assertThat(d.at("/fiche/auteur").asText()).isEqualTo("agent");assertThat(d.at("/fiche/demandeur/id").asText()).isEqualTo("agent");
  d=extraction("admin","POST",dp+"/prendre",Map.of("version",d.at("/fiche/version").asLong(),"requeteId",q.at("/fiche/id").asText(),"motif","Traitement autorisé"),200);
  assertThat(d.at("/requete/contenu/sqlTexte").asText()).isEqualTo("SELECT 1 FROM dual");
  var edit=oracleEdit(q);edit.put("sqlTexte","SELECT 2 FROM dual");q=extraction("admin","PUT",qp,edit,200);
  var observed=extraction("agent","GET",dp,null,200);assertThat(observed.path("requete").isNull()).isTrue();assertThat(observed.path("executions").size()).isZero();assertThat(observed.at("/fiche/edition").asInt()).isEqualTo(1);
  d=extraction("admin","POST",dp+"/executions",oracleRun(d,"SUCCES"),200);assertThat(d.at("/fiche/etat").asText()).isEqualTo("TERMINEE");assertThat(d.at("/executions/0/edition").asInt()).isEqualTo(1);
  assertThat(extraction("admin","GET",qp,null,200).at("/fiche/verifiee").asBoolean()).isTrue();
  assertThat(extraction("agent","GET",dp,null,200).at("/fiche/etat").asText()).isEqualTo("TERMINEE");extraction("admin","POST",dp+"/annuler",extractOp(d),409);
 }
 @Test void extractionApiProtectsOwnScopeSqlAndLivePermissions()throws Exception{
  extraction(null,"GET","configuration",null,401);assertThat(extraction("agent","GET","configuration",null,200).path("demander").asBoolean()).isFalse();extraction("agent","POST","demandes",extractionForm("agent"),403);
  platformRole("agent","EXTRACTIONS_DEMANDER");platformRole("foreign","EXTRACTIONS_DEMANDER");platformRole("chief","REQUETES_CONSULTER");var q=oracleReady();String qp="requetes/"+q.at("/fiche/id").asText();
  var d=extraction("agent","POST","demandes",extractionForm("agent"),201);String dp="demandes/"+d.at("/fiche/id").asText();
  extraction("foreign","GET",dp,null,404);extraction("agent","GET",qp,null,403);extraction("chief","GET",dp,null,403);
  assertThat(extraction("foreign","GET","demandes",null,200).path("totalElements").asInt()).isZero();extraction("agent","GET","demandes?departementId=foreign",null,403);extraction("agent","POST","demandes",extractionForm("foreign"),403);
  extraction("agent","POST",dp+"/prendre",Map.of("version",d.at("/fiche/version").asLong(),"requeteId",q.at("/fiche/id").asText(),"motif","Tentative"),403);
  extraction("chief","POST",qp+"/essais",oracleRun(q,"SUCCES"),403);execute("DELETE FROM ba_user_roles WHERE user_id='chief' AND role_id='role-requetes-consulter'");extraction("chief","GET",qp,null,403);
  execute("DELETE FROM ba_user_roles WHERE user_id='agent' AND role_id='role-extractions-demander'");extraction("agent","GET",dp,null,403);
 }
 @Test void extractionCannotTakeDraftAndFailureKeepsRequestOpen()throws Exception{
  var q=oracleQuery();var d=extraction("admin","POST","demandes",extractionForm("agent"),201);String dp="demandes/"+d.at("/fiche/id").asText(),qp="requetes/"+q.at("/fiche/id").asText();
  execute("UPDATE ba_requete_oracle SET verifiee=false WHERE id='"+q.at("/fiche/id").asText()+"'");q=extraction("admin","GET",qp,null,200);
  var take=Map.of("version",d.at("/fiche/version").asLong(),"requeteId",q.at("/fiche/id").asText(),"motif","Affectation");extraction("admin","POST",dp+"/prendre",take,409);
  q=extraction("admin","POST",qp+"/essais",oracleRun(q,"SUCCES"),200);q=extraction("admin","POST",qp+"/verifier",extractOp(q),200);d=extraction("admin","POST",dp+"/prendre",take,200);
  platformRole("chief","EXTRACTIONS_GERER");extraction("chief","POST",dp+"/executions",oracleRun(d,"SUCCES"),403);
  d=extraction("admin","POST",dp+"/executions",oracleRun(d,"ECHEC"),200);assertThat(d.at("/fiche/etat").asText()).isEqualTo("EN_COURS");assertThat(extraction("admin","GET",qp,null,200).at("/fiche/verifiee").asBoolean()).isFalse();
  d=extraction("admin","POST",dp+"/executions",oracleRun(d,"SUCCES"),200);assertThat(d.at("/fiche/etat").asText()).isEqualTo("TERMINEE");assertThat(d.path("executions").size()).isEqualTo(2);
 }
 @Test void extractionConcurrentExecutionsHaveExactlyOneEffect()throws Exception{
  var q=oracleReady();var d=extraction("admin","POST","demandes",extractionForm("agent"),201);String dp="demandes/"+d.at("/fiche/id").asText();d=extraction("admin","POST",dp+"/prendre",Map.of("version",d.at("/fiche/version").asLong(),"requeteId",q.at("/fiche/id").asText(),"motif","Traitement"),200);
  var body=oracleRun(d,"SUCCES");assertThat(race(()->extractionRaw("admin","POST",dp+"/executions",body).statusCode(),()->extractionRaw("admin","POST",dp+"/executions",body).statusCode())).containsExactlyInAnyOrder(200,409);
  d=extraction("admin","GET",dp,null,200);assertThat(d.path("executions").size()).isEqualTo(1);assertThat(d.path("historique").size()).isEqualTo(3);assertThat(d.at("/fiche/etat").asText()).isEqualTo("TERMINEE");
 }
 @Test void extractionConcurrentEditResetAndArchivePreserveEvidence()throws Exception{
  var q=oracleReady();String path="requetes/"+q.at("/fiche/id").asText();var body=oracleEdit(q);body.put("titre","Titre modifié");
  assertThat(race(()->extractionRaw("admin","PUT",path,body).statusCode(),()->extractionRaw("admin","PUT",path,body).statusCode())).containsExactlyInAnyOrder(200,409);
  q=extraction("admin","GET",path,null,200);assertThat(q.at("/fiche/edition").asInt()).isEqualTo(2);assertThat(q.path("revisions").size()).isEqualTo(2);assertThat(q.at("/fiche/verifiee").asBoolean()).isTrue();
  q=extraction("admin","POST",path+"/archiver",extractOp(q),200);assertThat(extraction("admin","GET","requetes?archivees=true",null,200).path("content").size()).isEqualTo(1);
  extraction("admin","POST",path+"/essais",oracleRun(q,"SUCCES"),409);q=extraction("admin","POST",path+"/restaurer",extractOp(q),200);assertThat(q.path("executions").size()).isEqualTo(1);assertThat(q.at("/fiche/verifiee").asBoolean()).isFalse();
 }
 @Test void extractionValidationAndCancellationAreEnforcedByServer()throws Exception{
  platformRole("agent","EXTRACTIONS_DEMANDER");var input=extractionForm("agent");input.put("dateDemande",LocalDate.now().plusDays(1));extraction("agent","POST","demandes",input,400);input=extractionForm("agent");input.put("periodeDebut",LocalDate.now());extraction("agent","POST","demandes",input,400);
  extraction("admin","POST","demandes",extractionForm("admin"),400);var q=oracleQuery();String path="requetes/"+q.at("/fiche/id").asText();var edit=oracleEdit(q);edit.put("sqlTexte","DELETE FROM comptes");extraction("admin","PUT",path,edit,400);assertThat(extraction("admin","GET",path,null,200).path("revisions").size()).isEqualTo(1);
  edit=oracleEdit(q);edit.put("sqlTexte","SELECT :date_debut FROM dual");edit.put("parametres",List.of(Map.of("nom","autre","type","DATE","description","Date du contrôle")));extraction("admin","PUT",path,edit,400);var run=oracleRun(q,"ECHEC");run.put("commentaire","");extraction("admin","POST",path+"/essais",run,400);run=oracleRun(q,"SUCCES");run.put("executeLe",Instant.now().plusSeconds(300));extraction("admin","POST",path+"/essais",run,400);
  var d=extraction("agent","POST","demandes",extractionForm("agent"),201);String dp="demandes/"+d.at("/fiche/id").asText();d=extraction("agent","POST",dp+"/annuler",extractOp(d),200);assertThat(d.at("/fiche/etat").asText()).isEqualTo("ANNULEE");extraction("agent","POST",dp+"/annuler",extractOp(d),409);
 }
 @Test void extractionMonitoringReusesDepartmentScopeWithoutSqlOrSensitiveContent()throws Exception{
  platformRole("agent","EXTRACTIONS_DEMANDER");var d=extraction("agent","POST","demandes",extractionForm("agent"),201);
  fixture("observer","AGENT","dept","s1");monitoringRole("observer","SUIVI_DEMANDES_DEPARTEMENT");fixture("outsider","AGENT","foreign",null);monitoringRole("outsider","SUIVI_DEMANDES_DEPARTEMENT");
  var rows=monitoring("observer","?module=EXTRACTION&etat=EN_ATTENTE",200);assertThat(rows.path("totalElements").asInt()).isEqualTo(1);assertThat(rows.at("/content/0/etapeActive").asText()).isEqualTo("Nouvelle demande");assertThat(rows.at("/content/0/etapesTotal").asInt()).isZero();assertThat(rows.toString()).doesNotContain("objectif","sqlTexte","criteres","Données fictives");
  assertThat(monitoring("outsider","?module=EXTRACTION",200).path("totalElements").asInt()).isZero();monitoring("outsider","/EXTRACTION/"+d.at("/fiche/id").asText(),404);extraction("observer","GET","demandes/"+d.at("/fiche/id").asText(),null,403);
  var q=oracleReady();String path="demandes/"+d.at("/fiche/id").asText();d=extraction("admin","POST",path+"/prendre",Map.of("version",d.at("/fiche/version").asLong(),"requeteId",q.at("/fiche/id").asText(),"motif","Recette"),200);assertThat(monitoring("observer","?module=EXTRACTION&etat=EN_ATTENTE",200).at("/content/0/etapeActive").asText()).isEqualTo("Traitement informatique");
  d=extraction("admin","POST",path+"/executions",oracleRun(d,"SUCCES"),200);assertThat(monitoring("observer","?module=EXTRACTION&etat=TERMINEE",200).path("totalElements").asInt()).isEqualTo(1);assertThat(monitoring("observer","?module=EXTRACTION&etat=EN_ATTENTE",200).path("totalElements").asInt()).isZero();
 }
 private LinkedHashMap<String,Object> extractionForm(String requester){var b=new LinkedHashMap<String,Object>();b.put("titre","Extraction de recette");b.put("objectif","Vérifier le suivi sans données de clientèle");b.put("dateDemande",LocalDate.now());b.put("demandeurId",requester);b.put("criteres","Données fictives");return b;}
 private JsonNode oracleQuery()throws Exception{var b=new LinkedHashMap<String,Object>();b.put("titre","Extraction fictive");b.put("objectif","Contrôle fonctionnel");b.put("dateValidation",LocalDate.now());b.put("departementId","dept");b.put("sqlTexte","SELECT 1 FROM dual");return extraction("admin","POST","requetes",b,201);}
 private JsonNode oracleReady()throws Exception{var q=oracleQuery();return extraction("admin","POST","requetes/"+q.at("/fiche/id").asText()+"/essais",oracleRun(q,"SUCCES"),200);}
 private LinkedHashMap<String,Object> oracleEdit(JsonNode q){var b=new LinkedHashMap<String,Object>();b.put("titre",q.at("/fiche/titre").asText());b.put("objectif",q.at("/fiche/objectif").asText());b.put("dateValidation",q.at("/fiche/dateValidation").asText());b.put("departementId",q.at("/fiche/departementDemandeur/id").asText());b.put("serviceId",q.at("/fiche/serviceDemandeur/id").asText(""));b.put("schemaCible",q.path("schemaCible").asText());b.put("sqlTexte",q.path("sqlTexte").asText());b.put("parametres",json.convertValue(q.path("parametres"),List.class));b.put("version",q.at("/fiche/version").asLong());b.put("motif","Nouvelle édition de recette");return b;}
 private JsonNode monitoring(String actor,String path,int expected)throws Exception{return business(actor,"GET","suivi-demandes"+path,null,expected);}
 private Map<String,Object> extractOp(JsonNode d){return Map.of("version",d.at("/fiche/version").asLong(),"motif","Opération de recette");}
 private LinkedHashMap<String,Object> oracleRun(JsonNode d,String result){var b=new LinkedHashMap<String,Object>();b.put("version",d.at("/fiche/version").asLong());b.put("executeLe",Instant.now().minusMillis(1));b.put("environnement","RECETTE");b.put("resultat",result);b.put("referenceVerification","TICKET-RECETTE");b.put("lignes",result.equals("SUCCES")?0:null);b.put("commentaire",result.equals("ECHEC")?"Erreur fictive":"Contrôle fictif réussi");return b;}
 private JsonNode extraction(String actor,String method,String path,Object body,int expected)throws Exception{var r=extractionRaw(actor,method,path,body);if(expected<300)assertThat(r.headers().firstValue("cache-control").orElse("")).contains("no-store");var d=status(r,expected);if(expected<300&&d.has("fiche")){String table=path.startsWith("requetes")?"ba_requete_oracle":"ba_extraction_demande";try(var p=sql.prepareStatement("SELECT version FROM "+table+" WHERE id=?")){p.setString(1,d.at("/fiche/id").asText());try(var rows=p.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getLong(1)).isEqualTo(d.at("/fiche/version").asLong());}}}return d;}
 private HttpResponse<String> extractionRaw(String actor,String method,String path,Object body)throws Exception{var b=HttpRequest.newBuilder(URI.create(base+"/api/extractions/"+path)).header("Content-Type","application/json").header("Accept","application/json");if(actor!=null)b.header("Authorization","Bearer "+tokens.get(actor));return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 private JsonNode platform(String actor,String method,String path,Object data,int expected)throws Exception{
  var result=status(platformRaw(actor,method,path,data),expected);
  if(expected<300&&result.has("id")&&result.has("version")){
   try(var q=sql.prepareStatement("SELECT version FROM ba_plateforme WHERE id=?")){q.setString(1,result.path("id").asText());try(var rows=q.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getLong(1)).as("La réponse HTTP doit exposer la version effectivement enregistrée").isEqualTo(result.path("version").asLong());}}
  }
  return result;
 }
 private HttpResponse<String> platformRaw(String actor,String method,String path,Object body)throws Exception {
  var request=HttpRequest.newBuilder(URI.create(base+"/api/plateformes"+(path.isEmpty()||path.startsWith("?")?"":"/")+path)).header("Content-Type","application/json");if(actor!=null)request.header("Authorization","Bearer "+tokens.get(actor));
  return http.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
 }

 private void fixture(String id,String fn,String dept,String service)throws Exception{try(var p=sql.prepareStatement("INSERT INTO ba_utilisateur(id,username,password_hash,nom,telephone,fonction,departement_id,service_id,activated,account_locked,created_by,created_date) VALUES (?,?, 'unused',?,?,?,?,?,true,false,'test',now())")){p.setString(1,id);p.setString(2,id);p.setString(3,id);p.setString(4,id);p.setString(5,fn);p.setString(6,dept);p.setString(7,service);p.executeUpdate();}SignatureTestImages.seed(sql,id);execute("INSERT INTO ba_user_roles(user_id,role_id) SELECT '"+id+"',id FROM ba_role WHERE code IN ('BA_CONNECT','PROCESSUS_DEMANDER','PROCESSUS_VALIDER')");tokens.put(id,app.getBean(TokenProvider.class).createToken(new UsernamePasswordAuthenticationToken(id,"",List.of(new SimpleGrantedAuthority("BA_CONNECT"))),false).getToken());}
 private JsonNode post(String actor,String path,Object body,int expected)throws Exception{return status(call(actor,"POST",path,body),expected);}private JsonNode get(String actor,String path,int expected)throws Exception{return status(call(actor,"GET",path,null),expected);}
 private HttpResponse<String> call(String actor,String method,String path,Object body)throws Exception{var b=HttpRequest.newBuilder(URI.create(base+"/api/processus/"+path)).header("Content-Type","application/json").header("Accept","application/json").header("Idempotency-Key",UUID.randomUUID().toString());if(actor!=null)b.header("Authorization","Bearer "+tokens.get(actor));return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 private JsonNode status(HttpResponse<String> response,int expected)throws Exception{assertThat(response.statusCode()).as(response.body().substring(0,Math.min(response.body().length(),1500))).isEqualTo(expected);if(!response.headers().firstValue("content-type").orElse("").contains("json"))return json.createObjectNode();return json.readTree(response.body());}
 private List<Integer> race(Callable<Integer> a,Callable<Integer> b)throws Exception{var pool=Executors.newFixedThreadPool(2);var go=new CountDownLatch(1);try{var aa=pool.submit(()->{go.await();return a.call();});var bb=pool.submit(()->{go.await();return b.call();});go.countDown();return List.of(aa.get(30,TimeUnit.SECONDS),bb.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
 private void execute(String query)throws Exception{try(var s=sql.createStatement()){s.execute(query);}}private String url(){return System.getenv("BSIC_MIGRATION_TEST_URL");}private String user(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_USERNAME","postgres");}private String password(){return System.getenv().getOrDefault("BSIC_MIGRATION_TEST_PASSWORD","");}
}

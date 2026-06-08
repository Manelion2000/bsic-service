# Documentation ? bsic-service

## Vue d'ensemble
Service Spring Boot pour la gestion de l'annuaire (directions, services, departements, agences, employes), de l'habilitation (plateformes, circuits, fiches, etapes), des reunions, et la generation de rapports PDF.

## Stack technique
- Java 17
- Spring Boot 3.5.3 (Web, Data JPA, Validation, Security, Actuator)
- PostgreSQL
- MapStruct
- JWT (jjwt)
- Spring Mail + Thymeleaf
- JasperReports
- Springdoc OpenAPI (UI)

## Lancement
Port par defaut: `8083`

### Profil dev (securite ouverte)
```powershell
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### Profil prod (JWT + securite)
```powershell
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

### Build
```powershell
mvnw.cmd clean package
```

## Configuration
Fichiers:
- `src/main/resources/application.yml` (commun)
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

### Base
- `server.port`: `8083`
- `spring.profiles.active`: `prod`
- `app.storage.path`: `./datas` (stockage fichiers)

### DB
Profil dev:
- `jdbc:postgresql://localhost:5432/categoriedb`

Profil prod:
- `jdbc:postgresql://localhost:5432/categorie_db`

### Mail
Configurer via `spring.mail.*` dans `application.yml`.

## Securite
- Profil `dev`: toutes les requetes sont permises, CORS/CSRF desactives.
- Profil `prod`: JWT + stateless, certaines routes publiques (auth, activation, CSRF, etc.), le reste authentifie.

## Stockage fichiers
Service: `BaFileStorageService`
- Lecture par id dans `app.storage.path`.
- Sauvegarde genere un id (UUID) et ecrit le fichier.
- Suppression logique par suppression du fichier.

## Rapports
Service: `BaReportService`
- Fiche habilitation: `reports/fiche-habilitation.jrxml`.
- Fichiers generes dans `./target`.

## APIs
Base URL: `/api`

### Auth
- `POST /api/authenticate` ? connexion, retourne JWT
- `GET /api/csrf` ? recupere CSRF token

### Utilisateurs / roles / profils
- `GET /api/users` ? liste utilisateurs
- `POST /api/users` ? creation utilisateur
- `PUT /api/users/{code}` ? maj utilisateur
- `DELETE /api/users/{code}` ? suppression logique
- `GET /api/users/details` ? infos utilisateur courant
- `PUT /api/users/change-password`
- `PUT /api/users/request-reset-password`
- `PUT /api/users/complete-reset-password`
- `GET /api/users/{id}/activate`

- `GET /api/roles`
- `POST /api/roles`
- `PUT /api/roles`
- `DELETE /api/roles/{id}`

- `GET /api/profils`
- `POST /api/profils`
- `PUT /api/profils`
- `DELETE /api/profils/{id}`

### Documents
- `GET /api/documents/{id}` ? recuperer un document

### Annuaire
- `GET /api/annuaires/directions`
- `POST /api/annuaires/directions`
- `GET /api/annuaires/services`
- `POST /api/annuaires/services`
- `GET /api/annuaires/departements`
- `POST /api/annuaires/departements`
- `GET /api/annuaires/agences`
- `POST /api/annuaires/agences`
- `GET /api/annuaires/employes?q=...`
- `POST /api/annuaires/employes`
- `PUT /api/annuaires/employes/{id}`
- `DELETE /api/annuaires/employes/{id}`
- `GET /api/annuaires/annuaire?directionId=&departementId=&serviceId=`

### Habilitation
- `GET /api/habilitations/plateformes`
- `POST /api/habilitations/plateformes`
- `POST /api/habilitations/etapes`
- `GET /api/habilitations/etapes?type=&directionId=&serviceId=`
- `GET /api/habilitations/plateformes/{id}/etapes`
- `PUT /api/habilitations/plateformes/{id}/etapes`
- `GET /api/habilitations/circuits`
  - filtre optionnel: `?plateformeId=...`
- `POST /api/habilitations/circuits`
- `PUT /api/habilitations/circuits/{id}`
- `GET /api/habilitations/fiches`
- `GET /api/habilitations/fiches/{id}`
- `POST /api/habilitations/fiches`
- `GET /api/habilitations/mes-demandes?statut=`
- `GET /api/habilitations/mes-validations?statut=`
- `POST /api/habilitations/fiches/{id}/etapes-from-plateforme`
- `POST /api/habilitations/fiches-etapes`
- `PUT /api/habilitations/fiches-etapes/{id}/status?statut=&commentaire=`

### Reunions
- `GET /api/reunions`
- `POST /api/reunions`
- `POST /api/reunions-participants`
- `POST /api/reunions-actions`

### Reporting
Base: `/api/reporting`
- `GET /api/reporting/habilitations/fiches/{id}` ? PDF fiche habilitation

## Domaines principaux
- Annuaire: `BaDirection`, `BaService`, `BaDepartement`, `BaAgence`, `BaUser`
- Habilitation: `BaPlateforme`, `BaCircuit`, `BaEtapeDefinition`, `BaFicheHabilitation`, `BaFicheHabilitationEtape`
- Reunions: `BaReunion`, `BaReunionParticipant`, `BaReunionAction`

## Divers
- Swagger UI (springdoc) disponible si non desactive: `/swagger-ui/index.html`.
- Logs metiers via `BaLogService`.

---

Sources principales: controllers et services du projet.

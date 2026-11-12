# Création et évolution de la base avec Flyway

Flyway exécute les fichiers SQL qui créent ou modifient les tables PostgreSQL.
Hibernate, avec `ddl-auto: validate`, vérifie ensuite que ces tables correspondent
aux entités Java. Hibernate ne crée pas les tables manquantes.

## Premier démarrage sur une base vide

Le script `src/main/resources/db/migration/B1.20__initial_schema.sql` crée les
tables du schéma initial, leurs clés étrangères, les contraintes et les index.
Il ajoute les rôles techniques de base, sans créer d'utilisateur ni de mot de passe.

Au démarrage sur un schéma vide, Flyway exécute dans cet ordre :

1. `B1.20__initial_schema.sql` : schéma consolidé jusqu'à la version `1.20`.
2. `V1.21__create_autorisation_absence.sql` : table des absences et rôle RH associé.
3. `V1.22__frozen_rh_validation_circuits.sql` : circuits RH figés et historique.
4. `V1.23__direction_generale_roles.sql` : rôles de connexion des comptes DG et DGA.
5. `V1.24__gestion_conges.sql` : politiques, calendriers, droits, demandes, planning,
   audit et permissions des congés. Aucun droit ni calendrier bancaire n'est activé.
6. `V1.25__audit_conges_details.sql` : détails d'audit et références RH conservés en entier.
7. `V1.26__circuits_autonomes_demarches.sql` : publications, catalogue et dossiers des démarches autonomes.
8. `V1.27__suivi_demandes_par_perimetre.sql` : permissions de suivi départemental et global.
9. `V1.28__catalogue_informatique_plateformes.sql` : fiches, dépendances et historiques des plateformes.
10. `V1.29__manuels_utilisateur_plateformes.sql` : fichiers privés des manuels utilisateur, liés aux plateformes.
11. `V1.30__demandes_extraction_requetes_oracle.sql` : demandes d'extraction, bibliothèque SQL, éditions et essais ; intégration au suivi commun.
12. `V1.31__simplifier_enregistrement_requetes_validees.sql` : saisie directe des requêtes validées, sans code ni plateforme obligatoires ; date de validation et auteur.
13. `V1.32__departement_demandeur_requetes.sql` : département demandeur des requêtes et conservation du nom par édition.
14. `V1.33__acces_temporaires_utilisateurs.sql` : codes temporaires à usage unique, changement obligatoire et invalidation des anciennes sessions.
15. `V1.34__service_demandeur_extractions_requetes.sql` : service lié au département demandeur pour les extractions et les requêtes, avec conservation du nom à la saisie.
16. Les futures migrations `V1.35`, etc., lorsqu'elles seront ajoutées.

Les anciens scripts `V1.3` à `V1.20` sont ignorés sur cette installation : leur
résultat utile est déjà intégré au script initial. Hibernate valide ensuite
l'ensemble des tables. Un nouveau démarrage ne rejoue pas les scripts exécutés.

### Exemple local sous PowerShell

La base PostgreSQL doit déjà exister : Flyway crée les tables, pas la base elle-même.
Si `bsic_db` n'existe pas, exécuter depuis la base administrative `postgres` :

```sql
CREATE DATABASE bsic_db;
```

Si `bsic_db` existe déjà et est vide, cette commande n'est pas nécessaire.
Depuis le dossier du projet, configurer la connexion et démarrer :

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/bsic_db'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = '<votre_mot_de_passe_postgresql>'

# Clé JWT temporaire pour ce démarrage local.
$jwtBytes = New-Object byte[] 64
$jwtGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtGenerator.GetBytes($jwtBytes)
$env:BSIC_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtGenerator.Dispose()

mvn.cmd spring-boot:run
```

En production, conserver une clé JWT stable dans le gestionnaire de secrets.
Voir aussi [AUTHENTIFICATION-SECURITE.md](AUTHENTIFICATION-SECURITE.md).

Dans IntelliJ, vérifier aussi les paramètres de la configuration de lancement :
une option `spring.datasource.url` passée au lancement prend le dessus sur les
fichiers YAML et les variables d'environnement. Elle doit désigner la bonne base.

Laisser Flyway exécuter le script initial au démarrage, afin qu'il enregistre
son exécution dans `flyway_schema_history`.

### Erreur de connexion avec l'utilisateur `training` dans IntelliJ

Une erreur `SQL State: 28P01` avec l'utilisateur `training` signifie que le
lancement utilise encore ces anciens identifiants PostgreSQL.

Dans `Run > Edit Configurations`, ouvrir la configuration Spring Boot
`BsicServiceApplication`, puis retirer les trois paramètres supplémentaires :

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Avec le profil `prod`, l'application utilise alors la connexion configurée dans
`application-prod.yml` : base `bsic_db`, utilisateur `postgres` et mot de passe
PostgreSQL configuré. Relancer ensuite l'application. Les paramètres de connexion
du panneau Database d'IntelliJ sont indépendants de cette configuration de lancement.

## Base existante avec des tables et des données

Cette procédure suppose que la base contient déjà le schéma BSIC complet jusqu'à
`V1.20`. Faire une sauvegarde avant le premier déploiement.

Avec `baseline-on-migrate: true` et `baseline-version: "1.20"`, Flyway inscrit
ce point de départ dans `flyway_schema_history` si la base n'est pas encore suivie.
Il ne recrée pas les tables et applique ensuite les migrations après `1.20`.
Le script `B1.20` est également ignoré sur les bases déjà suivies par Flyway.

Une base partiellement initialisée doit être réparée ou restaurée avant cette
adoption : déclarer une baseline ne crée pas ses tables manquantes.

| Élément | Rôle |
| --- | --- |
| `B1.20__initial_schema.sql` | Crée les tables sur un schéma vide. |
| `baseline-version: "1.20"` | Déclare le point de départ d'un schéma déjà existant, sans créer de tables. |
| `V1.21__create_autorisation_absence.sql` | Ajoute les absences après le schéma initial. |

Le fonctionnement des scripts `B` est décrit dans la
[documentation Flyway](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/baseline-migrations).

## Nouvelles migrations

`V1.34` existe déjà. Toute prochaine évolution commence à `V1.35` :

```text
src/main/resources/db/migration/V1.35__description_suivante.sql
```

Règles :

- Ne jamais modifier une migration déjà exécutée, y compris le script initial `B1.20`.
- Ne jamais réutiliser un numéro de version pour une nouvelle migration `V`.
- Rendre les identifiants des données initiales déterministes.
- Tester les migrations avant leur livraison et sauvegarder la base de production.

## Vérifier l'état de la base

```sql
SELECT installed_rank, version, description, type, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Sur une installation vide, cet historique contient l'exécution de `B1.20`, puis
de `V1.21`, de `V1.22`, de `V1.23`, de `V1.24`, de `V1.25`, de `V1.26`, de `V1.27`
de `V1.28`, de `V1.29`, de `V1.30`, de `V1.31`, de `V1.32`, de `V1.33` et de `V1.34`. Toutes les migrations exécutées doivent avoir `success = true`.

`V1.27` ajoute les permissions de suivi des demandes par périmètre ; `V1.28`
enrichit le référentiel des plateformes avec le catalogue informatique, ses
dépendances et ses historiques. `V1.29` ajoute le stockage privé des manuels
PDF/DOCX, avec empreinte de contenu et retrait logique conservant l'historique.
Les sauvegardes PostgreSQL doivent inclure cette table et ses données binaires.
Les permissions ajoutées ne sont attribuées automatiquement à aucun utilisateur.
Voir [PLATEFORMES-INFORMATIQUES.md](PLATEFORMES-INFORMATIQUES.md).

`V1.30` ajoute les demandes d'extraction et les requêtes Oracle versionnées, avec
leurs essais déclarés. Elle élargit le suivi départemental/global et crée trois
permissions sans attribution automatique. Elle n'exécute aucun SQL sur Oracle.
`V1.31` rend le code et la plateforme facultatifs en base pour préserver leurs
anciennes valeurs, retire la contrainte de code unique et ajoute la date de
validation ainsi que l'auteur de saisie. Elle reprend uniquement l'auteur connu
de l'édition courante, sans inventer de date ou d'exécution. Les nouvelles fiches
ne contiennent plus de code ni de référence de plateforme.
`V1.32` ajoute le rattachement au département demandeur et son nom au moment
de la saisie. Les nouvelles éditions exigent un département actif de l’annuaire ;
les anciennes fiches restent sans département connu jusqu’à une correction
autorisée, sans affectation automatique.
`V1.34` ajoute le service demandeur facultatif aux requêtes et aux extractions.
Les anciens enregistrements et copies d'éditions restent sans service connu,
sans utiliser l'affectation actuelle des agents pour reconstituer leur passé.
Les nouvelles saisies vérifient le rattachement du service au département.
Voir [EXTRACTIONS-ORACLE.md](EXTRACTIONS-ORACLE.md).

La migration `V1.22` conserve les anciennes demandes. Les demandes historiques
en attente sans circuit figé nécessitent une nouvelle soumission. Le détail du
paramétrage et des contrôles figure dans [CIRCUIT-VALIDATION-RH.md](CIRCUIT-VALIDATION-RH.md).

## Tester le script sur PostgreSQL

`FlywayInitialSchemaTest` vérifie l'installation sur un schéma vide, la validation
de toutes les entités par Hibernate, un second démarrage sans réexécution et
l'adoption d'un schéma existant avec conservation de ses données.

Le test crée puis supprime uniquement ses propres schémas temporaires, nommés
`bsic_bootstrap_test_*`. Le compte PostgreSQL doit pouvoir créer des schémas dans
la base de test choisie. Sans `BSIC_MIGRATION_TEST_URL`, ces tests sont ignorés.

```powershell
$env:BSIC_MIGRATION_TEST_URL = 'jdbc:postgresql://localhost:5432/postgres'
$env:BSIC_MIGRATION_TEST_USERNAME = 'postgres'
$env:BSIC_MIGRATION_TEST_PASSWORD = '<votre_mot_de_passe_postgresql>'
mvn.cmd '-Dtest=FlywayInitialSchemaTest' test
```

## Accès temporaires des utilisateurs

`V1.33` conserve les mots de passe existants, initialise leur version d’accès à zéro
et laisse le changement obligatoire désactivé pour ces comptes. Les anciens codes
de récupération par e-mail sont effacés. Les comptes créés ou réinitialisés avec le
nouveau backend reçoivent un accès temporaire à usage unique. Voir
[ACCES-TEMPORAIRES.md](ACCES-TEMPORAIRES.md). Ne pas modifier une migration appliquée.

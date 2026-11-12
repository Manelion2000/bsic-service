# Catalogue des plateformes informatiques

## Existant et choix d'intégration

Le backend Spring Boot 3.5.3 / Java 17 possède `BaPlateforme` et la table
`ba_plateforme`, liés aux circuits, fiches d'habilitation, comptes et
réinitialisations. L'écran Angular 19 initial propose essentiellement code,
nom, famille de compte et circuit. Le catalogue réutilise ces identifiants et
ces associations ; il ne crée pas un second inventaire ou moteur de validation.

Les choix de présentation s'inspirent des responsables, métadonnées, filtres et
relations du [catalogue Backstage](https://backstage.io/docs/features/software-catalog/)
et de son [format de description](https://backstage.io/docs/features/software-catalog/descriptor-format/),
ainsi que du suivi des logiciels et versions dans
[GLPI](https://help.glpi-project.org/documentation/modules/assets/softwares).
Sources consultées le 11 octobre 2026. Il s'agit d'une adaptation au portail,
sans installation ni synchronisation avec ces produits.

## Accès et utilisation

Menu **Informatique → Catalogue des plateformes**, routes
`/informatique/plateformes` et `/informatique/plateformes/:id` (lien partageable
entre personnes autorisées). L'ancienne route des plateformes redirige vers ce
module. La consultation fonctionne lorsque `app.features.habilitations-enabled`
est désactivé ; les autres API d'habilitation restent désactivées.

- `PLATEFORMES_CONSULTER` : catalogue, fiches détaillées, historique et téléchargement des manuels.
- `PLATEFORMES_GERER` : consultation, création, modification, archivage, restauration et gestion des manuels.
- `BA_ADMIN` : gestion du catalogue et attribution des permissions.

Le serveur relit les rôles actifs du compte et de son profil actif à chaque
appel. Aucune permission n'est attribuée automatiquement à un agent ou au
département informatique. Nommer un référent ne lui donne aucun accès bancaire.
Le catalogue ne retourne pas les comptes d'habilitation ou les dossiers RH.
Les fiches sont destinées aux personnes habilitées à consulter l'ensemble de ce
catalogue ; il ne s'agit pas d'un annuaire public.

Les rubriques couvrent usage, catégorie, cycle de vie, criticité, département,
référents métier/technique, fournisseur, version, hébergement, technologies,
support, exploitation, sauvegarde, continuité, RTO/RPO facultatifs, dates de
service/support/revue, environnements, dépendances, liens documentaires et manuels téléversés.
La famille `RESEAUX` / `CORE_BANKING` conserve son sens dans les habilitations ;
la catégorie du catalogue est un champ distinct.

Les valeurs métier restent à qualifier par la banque. Aucun RTO, RPO,
responsable ou niveau de criticité n'est supposé. Le module ne teste pas les
serveurs, ne découvre pas automatiquement le réseau et ne gère pas les licences.

## Manuels utilisateur

Dans le formulaire de création ou de modification, la rubrique **Documentation**
permet de sélectionner plusieurs PDF/DOCX ou de les glisser-déposer. Chaque fichier
possède un titre obligatoire et une description facultative (langue, version,
public concerné). Les fichiers sont envoyés au clic sur **Enregistrer la fiche** ;
annuler l'édition abandonne la sélection et les retraits préparés.

Limites vérifiées au serveur : 10 Mo par fichier, 20 Mo de nouveaux fichiers par
enregistrement et 20 manuels actifs par plateforme. Les contenus identiques à un
manuel actif sont refusés par leur empreinte SHA-256. Le serveur vérifie la
signature du PDF et la structure DOCX, limite la décompression et refuse les
macros, objets incorporés et entités XML externes dans les DOCX. Ces contrôles de
format ne constituent pas une analyse antivirus.

Les fichiers sont privés, stockés en PostgreSQL (`bytea`) et transmis en pièces
jointes par une API authentifiée avec `no-store` et `nosniff`. Ils n'utilisent pas
le répertoire des documents publics. Les droits actifs du catalogue sont relus
à chaque téléchargement ; connaître l'identifiant d'un fichier ne donne aucun
accès supplémentaire. Le téléchargement est tracé avec l'auteur réel.

Ajouter ou retirer des manuels, modifier la fiche et enregistrer sa nouvelle
version forment une transaction unique. Un fichier invalide, un conflit de
version ou un retrait visant une autre plateforme annule l'ensemble. Le retrait
est logique : la copie de la fiche dans **Historique** permet encore aux lecteurs
autorisés de télécharger l'ancienne édition. Pour remplacer un manuel, ajouter
le nouveau fichier et retirer l'ancien dans le même enregistrement.

## Données, concurrence et audit

Migration **V1.28__catalogue_informatique_plateformes.sql** :

- Métadonnées `fiche` JSONB sur `ba_plateforme`, typées et validées au backend.
- Références avec clés étrangères vers départements et utilisateurs existants.
- Table `ba_plateforme_dependance`, paire unique, clé étrangère et auto-dépendance interdite.
- Table `ba_plateforme_revision`, copie de chaque version avec auteur réel,
  horodatage serveur, action et motif ; unicité plateforme/numéro.
- Deux permissions nouvelles, sans attribution automatique.

Migration **V1.29__manuels_utilisateur_plateformes.sql** : table
`ba_plateforme_manuel`, clé étrangère plateforme, métadonnées et contenu privé,
auditeur, taille bornée et unicité du contenu actif par plateforme. Les copies
historiques contiennent uniquement les métadonnées et identifiants, sans dupliquer
les fichiers binaires. Les anciennes copies sans manuels restent lisibles.
Les sauvegardes/restaurations doivent inclure cette table et son contenu binaire.

Les anciennes fiches restent lisibles, avec cycle et criticité **À qualifier**.
Leur première modification capture une version `REPRISE` de l'état antérieur.
L'auteur de cette capture est l'opérateur de reprise, sans attribution inventée
de l'auteur d'origine ou d'une approbation historique. Les versions ultérieures
sont ajoutées ; aucune API ne permet de modifier/supprimer ces copies.

Les écritures sont transactionnelles. Un verrou PostgreSQL de transaction
sérialise les changements du graphe et les archivages ; un verrou de ligne
protège la version de la fiche. L'écriture exige la version lue et renvoie 409
si elle est périmée. Les références sont résolues avant mutation de l'entité
pour éviter un flush JPA intermédiaire. Un double enregistrement ne produit pas
deux modifications acceptées sur la même version. Les codes sont comparés sans tenir compte
de la casse, archives comprises, dans la transaction verrouillée.

Une auto-dépendance ou une référence inexistante/archivée est refusée. Des
dépendances réciproques peuvent être décrites : le catalogue représente les
relations réelles et n'impose pas un graphe sans cycles. L'archivage est logique
(`statut=D`), réversible, bloque les nouvelles sélections et préserve les liens
avec les données existantes. Il est refusé lorsqu'une plateforme active dépend
encore de la fiche. Une restauration exige des dépendances actives.

Les écritures des anciennes API `/api/habilitations/plateformes` utilisent les
mêmes contrôles et exigent désormais une version lors des mises à jour ; elles
préservent les métadonnées détaillées. Leur liste reste compatible avec les
formulaires d'habilitation. Le choix du circuit existant est conservé lorsque
les habilitations sont désactivées ; son changement nécessite leur activation.

Chaque écriture produit aussi une entrée dans `ba_log`. Les réponses du
catalogue portent `Cache-Control: no-store`. Les URL sont limitées à HTTP(S),
sans userinfo, query ou fragment ; aucune URL n'est téléchargée par le serveur.
Les textes sont rendus comme du texte par Angular. Les liens externes utilisent
`noopener noreferrer`. Le contrôle d'accès au document reste celui de sa source.
Les champs libres ne sont pas un coffre-fort : ne jamais y saisir de secrets.

## API

Base `/api/plateformes`, authentification requise :

| Méthode / chemin | Utilisation |
|---|---|
| GET `/configuration` | Capacités et références légères ; liste des responsables/circuits réservée aux gestionnaires |
| GET `/` | Filtres `recherche`, `categorie`, `cycleVie`, `criticite`, `departementId`, `archivees`, `page`, `size` |
| GET `/{id}` | Fiche et dépendances dans les deux sens |
| POST `/` | Création, motif obligatoire |
| PUT `/{id}` | Mise à jour complète, version et motif obligatoires |
| POST `/{id}/archiver` | Archivage, version et motif obligatoires |
| POST `/{id}/restaurer` | Restauration, version et motif obligatoires |
| GET `/{id}/historique?page=0` | Copies historiques, 20 par page |
| GET `/{id}/manuels/{manuelId}` | Téléchargement authentifié d'un manuel actif ou historique de cette plateforme |

POST et PUT acceptent toujours `application/json` pour les fiches sans changement
de manuels. Pour ajouter ou retirer des fichiers, utiliser `multipart/form-data` :

| Partie | Contenu |
|---|---|
| `fiche` | JSON du DTO d'écriture existant, avec motif et version pour PUT |
| `manuels` | JSON `{ "ajouts": [{ "titre": "Guide agent", "description": "Version 2026" }], "retraits": ["id-ancien-manuel"] }` |
| `fichiers` | Une partie fichier par ajout, dans le même ordre que `ajouts` ; absente pour un retrait seul |

Les deux parties JSON portent `Content-Type: application/json`. Le navigateur
construit la frontière multipart. La réponse détaillée contient `manuels` avec
identifiant, titre, description, nom, MIME, taille, SHA-256, date et auteur ; aucun
contenu binaire. Un accès refusé renvoie 401/403, un fichier d'une autre plateforme
404, un conflit de version/contenu actif 409, une erreur métier 400 et un dépassement
de limite multipart 413.

Pagination des fiches : 20 par défaut, maximum 100. Les caractères `%` et `_`
de la recherche sont traités littéralement. RTO/RPO sont des minutes entières
non négatives, avec une borne technique de 525600 minutes ; cette borne n'est
pas une exigence de continuité de la banque.

## Déploiement et reprise

1. Sauvegarder la base selon la procédure habituelle, arrêter les anciens
   serveurs d'application et conserver leurs artefacts de déploiement.
2. Déployer le backend contenant V1.28 et V1.29 et vérifier la réussite de Flyway.
   `ddl-auto` doit rester `validate` : ne pas créer ces tables avec Hibernate.
3. Déployer le frontend compilé dans `bsic-frontend/dist/bsic-frontend/browser`.
4. Attribuer les permissions aux gestionnaires et lecteurs autorisés, puis
   actualiser/reconnecter leur session pour rafraîchir la navigation.
5. Inventorier les fiches existantes et compléter progressivement leurs
   finalités, référents, cycles de vie et procédures. Vérifier les dépendances
   avec les équipes avant d'archiver une plateforme.

Les profils `dev` et `prod` autorisent 10 Mo par fichier et 25 Mo par requête
multipart ; la limite métier des nouveaux manuels reste 20 Mo par enregistrement.
Si un reverse proxy est utilisé, vérifier qu'il accepte ces requêtes. Les autres
modules conservent leurs limites métier propres. Aucun fichier existant n'est
importé automatiquement ; les gestionnaires peuvent ajouter les manuels après
la migration. Aucun nouveau rôle n'est nécessaire au-delà des permissions V1.28.

Aucune donnée de démonstration n'est injectée. La migration est additive et
conserve les identifiants. Flyway ne fournit pas ici de migration inverse :
ne pas supprimer les nouvelles colonnes/tables pour revenir à une ancienne
version, car elles contiennent les fiches et historiques. Un ancien serveur
ne connaît pas les nouveaux contrôles ; arrêter les anciennes instances avant
la mise en service et privilégier une correction vers l'avant.

## Vérifications

Les tests PostgreSQL utilisent exclusivement des schémas temporaires distincts
dans la base de test, supprimés à la fin. Ils couvrent migration vide/reprise,
validation JPA, métadonnées, rôles/profils actifs, révocation avec le même JWT,
accès API direct, URL interdites, minutes fractionnaires, dépendances, archivage,
restauration, versions HTTP réellement enregistrées, concurrence des écritures,
unicité des codes, reprise d'une ancienne fiche et compatibilité des habilitations.
Les tests de manuels couvrent PDF/DOCX, refus des faux formats et archives
malformées, taille, données XML externes, création multipart, téléchargement
protégé, droits retirés, refus hors plateforme, retrait, conservation historique,
doublons, retour arrière transactionnel et téléversements concurrents.

Les tests Angular couvrent filtres, droits du serveur, garde de route, lecture
seule, dépendances paginées, copie éditable, annulation, double clic/conflit,
environnements/documents, circuit désactivé conservé, historique, archivage,
révocation et fiche inaccessible.
La sélection par glisser-déposer, les limites, titres, annulations, retraits,
conflits conservant la sélection et téléchargements authentifiés sont aussi testés.

Vérification finale exécutée le 11 octobre 2026 :

- `mvn.cmd -q verify` : **213 tests**, 23 suites, aucun échec, erreur ou test ignoré.
  Le JAR `target/param-service-1.0.0.jar` a été généré.
- `ng test --watch=false --browsers=ChromeHeadless` : **143 tests réussis**.
- `ng build` : compilation de production réussie, sans dépassement de budget.
  Sortie `bsic-frontend/dist/bsic-frontend/browser`.

Journaux locaux : `target/manuels-verify.log` pour le backend,
`bsic-frontend/tmp/manuels-tests.log` et `manuels-build.log` pour le
frontend. La migration n'a pas été appliquée au schéma applicatif de travail.
La recette visuelle manuelle dans l'application reste à faire ; la compilation
et les tests DOM ne la remplacent pas.

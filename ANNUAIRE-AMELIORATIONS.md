# Annuaire et département demandeur des requêtes

## Analyse de l’existant

Le portail utilise Spring Boot 3.5.3, PostgreSQL/Flyway et Angular 19. Les
référentiels agents, départements, services et agences sont réutilisés.
L’annuaire possède déjà des endpoints publics et internes distincts. Le public
reçoit les coordonnées prévues par son DTO, sans rôles ni téléphone secondaire.

Les écarts corrigés concernaient les tableaux peu lisibles sur petit écran,
les filtres de services non limités au département, les appels réseau à chaque
frappe, l’absence de fiche contact, la pagination après recherche et l’ancien
formulaire agent imposant encore la présence d’un chef et d’un directeur.

## Sources consultées

Consultation le 11 octobre 2026 :

- [Microsoft Org Explorer](https://support.microsoft.com/en-us/viva/view-and-explore-your-organization)
  présente les personnes par fiches et permet de consulter leur contexte
  organisationnel et leurs coordonnées. Le portail reprend le principe d’une
  fiche lisible centrée sur les informations déjà disponibles.
- [Atlassian Teams](https://www.atlassian.com/platform/platform-apps/teams)
  propose un annuaire partagé d’équipes. Le portail privilégie une navigation
  commune entre contacts et structures, avec accès direct aux contacts d’un service,
  département ou agence.

Ces références orientent la présentation. Aucune intégration Microsoft/Atlassian,
présence en ligne, photographie ou relation hiérarchique supplémentaire n’est créée.

## Utilisation

### Contacts

`/annuaire/annuaire` : annuaire interne authentifié. `/` et `/annuaire-public` :
contacts autorisés par les endpoints publics, affichés après une recherche.

- Recherche de plusieurs mots, indépendante des accents et de la casse ; recherche
  de numéros de téléphone sans leurs espaces de présentation.
- Filtres département, service, agence et fonction ; choix incompatibles de service
  ou d’agence supprimés lors du changement de département.
- Présentation **Cartes** ou **Liste**, tri par nom ou département et pagination.
- **Voir la fiche contact** : identité, fonction réelle, organisation et coordonnées.
  Les liens e-mail et téléphone utilisent les outils configurés sur le poste.
- Copie d’une fiche individuelle ; le téléphone secondaire reste réservé à la vue interne.
- États de chargement, erreur avec relance et absence de résultat distincts.

La recherche publique initiale est temporisée de 250 ms ; la recherche textuelle
suivante utilise les contacts déjà reçus. Les anciennes requêtes HTTP sont annulées
au changement de filtre, à la réinitialisation et à la destruction du composant.
Effacer tous les critères sur l’accueil public masque les contacts et ferme leur fiche.

### Structures et collaborateurs

Départements, services et agences utilisent une navigation, des tableaux et des
fenêtres cohérents. **Contacts** ouvre directement le filtre correspondant dans
l’annuaire global. La pagination revient à la première page après recherche.
Les actions de gestion restent réservées à l’administrateur et les enregistrements
en cours bloquent les doubles clics ainsi que la fermeture de leur formulaire.

`/annuaire/employes` est protégé par la garde administrateur existante. Un agent
peut être créé avec un département et un service sans responsable préexistant.
Pour les agents, le chef du service est le supérieur **N+1**, et le directeur du
département le supérieur **N+2**. Le titulaire disponible unique est sélectionné
automatiquement. Plusieurs titulaires nécessitent un choix explicite pour
renseigner le poste ; aucun n'est choisi selon l'ordre de la liste. Les postes
vacants restent facultatifs. Les responsables sont actualisés après changement
de rattachement et contrôlés au serveur. Voir [HIERARCHIE-AGENTS.md](HIERARCHIE-AGENTS.md).
La suppression conserve l’API existante, précédée d’une confirmation dans l’interface.
Les habilitations et signatures restent gérées dans leurs modules existants.

Pour les fiches historiques sans département explicite, les endpoints d’annuaire
utilisent le département du service lorsqu’il est connu. Un département explicite
reste prioritaire. Les contacts sans nom de département ne font plus échouer le tri.

### Requêtes validées

Le formulaire demande **titre, département demandeur, objectif, SQL et date de
validation**. Le département provient des départements actifs, même sans agents.
Le serveur vérifie son existence et son état ; son nom n’est pas accepté depuis le client.
Il conserve identité et nom dans la fiche et dans chaque édition immuable.

Le département apparaît dans les cartes et le détail. La bibliothèque peut être
filtrée avec `departementId`. Il s’agit du département à l’origine du besoin,
distinct du compte informatique qui enregistre le SQL. L’auteur de saisie reste
calculé au serveur. Les codes uniques, plateformes sources et essais préalables
ne sont pas réintroduits.

Pour une **demande d’extraction**, le département continue de provenir du rattachement
du demandeur choisi ; ce contrôle de l’API reste inchangé.

## Migration et exploitation

`V1.32__departement_demandeur_requetes.sql` ajoute la référence de département,
son nom enregistré et un index au catalogue SQL. Les anciennes requêtes et
révisions restent sans département connu : aucune valeur n’est inventée.
Une modification d’une ancienne requête exige de choisir le département réel.
Les demandes déjà prises en charge conservent leur ancienne édition SQL.

Redémarrer le backend mis à jour pour appliquer Flyway jusqu’à V1.32, puis utiliser
le frontend compilé. Aucun rôle nouveau, référentiel en doublon ou jeu de données
de démonstration n’est ajouté. La migration n’est pas appliquée au schéma applicatif
pendant les tests : ceux-ci créent des schémas PostgreSQL temporaires.

## Vérifications

Les tests backend vérifient le département obligatoire, actif et contrôlé, la
disponibilité d’un département sans agents, les filtres et les noms figés par édition,
les migrations et la reprise sans données inventées. Les tests d’annuaire couvrent
le département hérité du service, la priorité du département explicite et les
champs autorisés dans la vue publique.

Les tests Angular vérifient la saisie du département, les cartes et listes, les
fiches et liens de contact, les accents et téléphones, la temporisation publique,
les réponses obsolètes, la pagination, la confidentialité du téléphone secondaire,
les droits de gestion, les doubles clics et la création d’agent sans responsables.

Journaux : `target/annuaire-verify.log`, `bsic-frontend/tmp/annuaire-tests.log` et
`bsic-frontend/tmp/annuaire-build.log`. La recette visuelle manuelle dans l’application
reste à effectuer ; les tests d’écran exécutés utilisent Chrome Headless.

Recette du 11 octobre 2026 : **232 tests backend dans 24 suites**, aucun échec,
erreur ou test ignoré (`mvn.cmd -q verify`) ; **180 tests Angular réussis**
(`ng test --watch=false --browsers=ChromeHeadless`) ; build de production réussi.
Le JAR et le frontend compilés ont été générés. Aucun déploiement en production
ni changement de rôle n’a été effectué pendant ce travail.


# Circuits autonomes et démarches de la banque

Pour apprendre les parcours écran par écran, commencer par le
[guide d'utilisation du portail](GUIDE-UTILISATION.md).

## Diagnostic et solution intégrée

Le backend utilise Spring Boot 3.5.3, Java 17, PostgreSQL et Flyway ; le frontend
utilise Angular 19. Le projet possède déjà des référentiels d'agents, directions,
services et agences, des habilitations, des réunions, des absences, sorties,
reprises et congés. L'authentification et l'audit sont réutilisés.

Deux usages existaient pour les circuits : la bibliothèque BaCircuit des
habilitations et les instances figées BaRhCircuit des demandes RH. L'interface
des anciens circuits dépendait du drapeau Habilitations. Leur configuration
pouvait être modifiée après utilisation ; les comptes étaient détectés comme
administrateurs par une recherche partielle sur le nom du rôle. Les écrans
étaient majoritairement chargés dès l'ouverture, et certains composants
réimportaient les styles globaux dans leur feuille locale.

La bibliothèque autonome étend **BaCircuit**. Ses publications sont immuables,
les démarches choisissent une publication précise, et les dossiers utilisent
**RhCircuitService** pour l'éligibilité, l'ordre et les décisions. Il n'y a pas
un second moteur de décisions concurrent. Les anciens identifiants et
historiques sont conservés.

La conception s'inspire des principes documentés par l'éditeur d'ELISE :
modélisation visuelle, paramétrage des processus, suivi et traçabilité.
Sources publiques consultées le 8 octobre 2026 :
- [Collaboration, workflows et suivi ELISE](https://www.neoledge.com/en/ecm-elise/collaboration-workflow-and-rpa/).
- [Studio de conception et supervision Elise.Automate](https://www.neoledge.com/ecm-elise/automatisation-processus-metier/).

Aucune connexion à un compte ELISE, récupération de données ou intégration
commerciale n'a été effectuée. Le module ne prétend pas fournir l'OCR,
l'archivage probant, la signature certifiée ou les connecteurs de cette solution.

## Parcours disponibles

La fenêtre **Vue workflow** présente un circuit vertical avec blocs compacts,
connexions et détails au clic. Cette disposition suit la séquence des validations
et garde une lecture simple sur mobile. Les brouillons configurables autorisent
le glisser-déposer à la souris et au toucher, avec boutons Monter/Descendre au
clavier. Le panneau permet aussi de modifier les paramètres d'une étape et
d'en ajouter une. La sauvegarde réutilise l'API des modèles et sa version attendue ;
elle ne publie pas automatiquement et ne modifie aucun dossier existant.

Le composant Angular partagé `CircuitWorkflowComponent` manipule une copie locale
du brouillon. Annuler abandonne cette copie. La sélection et les titulaires restent
associés à l'étape après déplacement. Les erreurs de sauvegarde conservent les
modifications visibles ; un conflit ne réécrit pas silencieusement le serveur.
Les publications configurables et les circuits des dossiers sont consultables
dans la même fenêtre en lecture seule, avec leurs décisions et signataires réels.
Les circuits RH sont affichés à partir de la simulation serveur pour l'agent
choisi ; leur ordre hiérarchique reste obligatoire. Aucun moteur ni migration
supplémentaire n'est nécessaire pour cette vue.

La fenêtre **Supprimer des circuits** permet une sélection de modèles complets
ou de versions publiées. **Gérer les versions publiées**, dans le studio,
limite la liste au modèle ouvert. Le serveur exige `BA_ADMIN` ou
`PROCESSUS_CONFIGURER`, vérifie les versions attendues et protège les publications
affectées à des démarches (y compris inactives) ou utilisées par des dossiers
génériques et des circuits RH figés. La suppression logique et l'audit utilisent
les colonnes existantes ; aucune nouvelle migration n'est nécessaire.

L'API `POST /api/processus/modeles/suppression` reçoit
`{"cibles":[{"type":"CIRCUIT","id":"…","version":0}]}` (100 éléments
maximum). Pour une seule version publiée, utiliser le type `PUBLICATION`.
Chaque cible utilise sa révision `version` renvoyée dans la bibliothèque.
Le lot est transactionnel. Les suppressions et affectations prennent le verrou
du modèle ; les numéros de publication sont conservés et ne sont jamais réutilisés.
Une republication du contenu déjà publié retourne un conflit, sans créer de doublon.

| Parcours | Écran et comportement |
|---|---|
| Configurer un circuit | /circuits : brouillon, étapes, réorganisation, duplication, simulation |
| Publier | Publication justifiée de la version enregistrée, avec contrôle de concurrence |
| Affecter | /demarches : catalogue, choix de publication et formulaire configurable |
| Demander | Catalogue → Commencer → brouillon → pièces → soumission |
| Valider | À traiter : actions calculées au serveur, refus motivé, étape active |
| Suivre | Mes dossiers, circuit et historique des signataires réels |
| Auditer | Vue réservée à PROCESSUS_AUDITER ; le configurateur n'accède pas automatiquement aux dossiers |
| Utiliser les modules RH | Catalogue → écran Absences, Sorties ou Congés, avec leurs règles et compteurs |

L'éditeur propose le glisser-déposer des étapes et des champs. Les boutons
Monter/Descendre fournissent une alternative au clavier et sur mobile.
Les pièces peuvent être déposées dans la zone prévue ou sélectionnées avec
le bouton de fichier ; les contrôles de format et de taille restent au backend.
Le portail dispose d'un menu mobile repliable, d'un lien Aller au contenu,
de focus visibles et de formulaires/tableaux mieux adaptés aux petits écrans.
Les routes chargent les écrans à la demande ; les styles globaux dupliqués ont
été retirés des deux anciens composants de circuits.

## Mise en service

La migration **V1.26__circuits_autonomes_demarches.sql** crée les publications,
le catalogue, les dossiers et leurs pièces privées, et ajoute les métadonnées
à BaCircuit et BaRhCircuit. Aucun modèle, démarche active ni rôle attribué à un
compte n'est créé automatiquement. La compilation ne migre pas la base réelle.
Valider les migrations en recette et sauvegarder la base avant déploiement.

1. Attribuer les permissions nécessaires dans la gestion existante des rôles.
2. Ouvrir le studio et créer un circuit : nom, description et mode.
3. Choisir explicitement le traitement du refus : rejet définitif ou possibilité
   pour un autre titulaire de décider. Aucun choix n'est implicite.
4. Ajouter les étapes et les périmètres ; un seul accord suffit dans un groupe.
   Un périmètre peut être le département du demandeur ou une structure fixe.
   Une condition peut limiter l'étape à certaines fonctions du demandeur.
5. Enregistrer, simuler sur un agent et corriger les éventuels blocages.
   En mode configurable, chaque étape doit avoir au moins un titulaire éligible
   avec PROCESSUS_VALIDER. Le mode RH simule le circuit organisationnel sans
   exiger cette permission propre aux dossiers génériques.
6. Justifier la publication. Les modifications non enregistrées ne sont ni
   simulées ni publiées par l'interface.
7. Créer une démarche, choisir une publication et les champs requis, puis
   activer explicitement cette démarche après vérification.

| Permission | Portée |
|---|---|
| BA_ADMIN ou PROCESSUS_CONFIGURER | Bibliothèque, publications, affectations et formulaires |
| PROCESSUS_DEMANDER | Brouillons et soumissions personnelles |
| PROCESSUS_VALIDER | Décision dans une étape où le compte est également éligible |
| PROCESSUS_AUDITER | Consultation des dossiers, valeurs, pièces et décisions pour audit |

BA_ADMIN permet de configurer ; il ne donne pas automatiquement le droit de
signer ou de consulter les dossiers. Un titulaire désigné doit aussi posséder
la permission de validation. L'ADMIN technique n'est jamais assimilé à une
fonction de chef, directeur, DGA ou DG.

## Modes et rattachement aux modules existants

**Étapes configurables** : séquence de validations par fonction organisationnelle
ou titulaires désignés. Les identités éligibles sont figées à la soumission.
Le serveur recontrôle compte actif, fonction et périmètre avant chaque décision.
Une auto-validation est toujours refusée. Une étape vide ou privée de permission
ne disparaît pas et n'entraîne aucune approbation automatique.

**Circuit organisationnel RH** : le moteur existant construit le parcours selon
la fonction effective. Agent → un chef du département → directeur → un DGA ;
chef → directeur → un DGA ; directeur → un DGA ; DGA → uniquement l'autre DGA.
Le DG est inclus seulement si le configurateur rend son étape obligatoire.
Une demande du DG reste bloquée tant que son circuit métier n'est pas défini.

Les codes réservés ABSENCE, SORTIE et CONGES raccordent effectivement leurs
modules à une publication hiérarchique. Leur simulation et leur soumission
utilisent l'affectation du catalogue. Sans affectation, la configuration RH
existante continue de s'appliquer. Une affectation désactivée bloque les
nouvelles soumissions du module correspondant. Les demandes déjà soumises
conservent leur circuit.

Pour CONGES, les choix DG et refus du circuit publié doivent correspondre à
la politique RH confirmée. Les calendriers, droits, réservations, restitutions,
permissions et délégations de congés restent gérés par le module métier.
Le quota indépendant de 10 jours d'absence ne devient jamais un droit annuel.
Les circuits RH publiés ne configurent pas de délégations supplémentaires ;
les congés conservent celles explicitement autorisées par leur politique.

Les habilitations techniques et les réunions conservent leurs écrans et contrats
existants. Leurs nouveaux dossiers administratifs peuvent être configurés comme
démarches génériques. L'automatisation de création de comptes, la validation de
PV ou un branchement aux systèmes bancaires nécessitent un adaptateur métier
et des règles confirmées ; ils ne sont pas exécutés par un circuit générique.
Un ancien circuit d'habilitation déjà utilisé ne peut plus réécrire ses étapes :
il faut le dupliquer et réaffecter les plateformes pour les futures demandes.
Un modèle autonome ne peut pas servir directement d'ancien circuit technique.

## Formulaires, dossiers et confidentialité

Chaque démarche générique propose un titre, des pièces et des champs de type
texte, date, nombre décimal ou choix. Les clés, types, listes et obligations
sont validés au backend. Les quantités décimales restent des chaînes et utilisent
BigDecimal, avec au plus 18 chiffres et 4 décimales pour ce formulaire générique.
Aucun script utilisateur ni expression arbitraire n'est exécuté.

Le brouillon capture la publication et le formulaire choisis à sa création.
La soumission calcule et fige les titulaires depuis l'organigramme courant.
Une nouvelle affectation ne réécrit ni un brouillon existant ni un dossier soumis.
Les pièces PDF/PNG/JPEG sont limitées à 5 Mo et stockées dans PostgreSQL ;
leur téléchargement exige une session et l'accès au dossier. Les pièces ne
peuvent être ajoutées ou retirées qu'au brouillon et ne sont jamais publiées
par une URL permanente. La suppression d'une pièce est logique et auditée.

Les statuts sont BROUILLON, EN_ATTENTE, APPROUVEE, REJETEE et RETIREE.
Le retrait est réservé au demandeur en attente. Une approbation finale ferme
le circuit ; aucune autre signature ne remplace la décision. Le retrait,
le refus et les approbations utilisent le même historique de décisions que
les instances RH. Les mutations sensibles sont également tracées dans BaLog.

## Transactions et API

Base API : /api/processus. Configuration, modèles, publication, simulation,
démarches, dossiers, décisions et pièces sont exposés par ProcessusController.
Le module est indépendant du drapeau Habilitations.

Les décisions requièrent version, cle et etape ; la création d'un brouillon
requiert Idempotency-Key. Les dossiers sont chargés sous verrou après le verrou
commun de leur demandeur. Une répétition identique d'une décision retourne son
résultat courant ; une clé réutilisée avec un contenu différent ou une version
périmée produit un conflit 409. Deux titulaires simultanés ne déclenchent qu'une
transition. Les publications et affectations portent aussi une version attendue.

Les lectures sont limitées au demandeur, aux participants du circuit ou à un
auditeur habilité. Les contrôles s'appliquent également par appel direct à l'API.
Les nouvelles permissions ne sont pas accordées par la migration.

## Corrections transversales

- Opérations sur rôles/profils et attributions/retraits de rôles réservés aux
  administrateurs ; le backend conserve les statuts 403/409 métier.
- Identification de l'administrateur par le code exact BA_ADMIN, y compris
  dans les gardes et l'écran utilisateurs.
- Un chef autorisé à gérer ses agents ne peut plus élever leurs droits
  techniques via les rôles ou profils d'un formulaire utilisateur.
- Création/modification des structures réservées à l'administrateur,
  avec les actions correspondantes masquées pour les autres comptes.
- Documents du stockage existant : identifiants contrôlés, chemins confinés,
  absence de fichier renvoyée comme 404, répertoire créé au premier dépôt,
  téléchargements marqués no-store et nosniff.
- Les nouveaux codes de circuits ne reposent plus sur un comptage concurrent.
- Chargement des écrans à la demande et suppression des réimports CSS globaux.

## Limites et recette

Cette version traite les circuits séquentiels avec groupes alternatifs et
conditions sur la fonction du demandeur. Les branches parallèles nécessitant
plusieurs accords, les retours vers une étape antérieure, l'escalade automatique,
les délais calendaires métier et les conditions sur montant ne sont pas activés.
Les délais des étapes sont indicatifs. Il n'y a pas d'approbation financière,
de paiement ou de modification d'habilitation bancaire automatique.

Les tests PostgreSQL migrent exclusivement des schémas temporaires isolés.
Ils couvrent publication figée, affectation, formulaires, permissions, accès,
pièces privées, refus alternatif, retrait, idempotence, deux signataires
simultanés, rattachement RH et régressions des modules existants.
Les tests Angular couvrent les déplacements, la publication, les changements
non enregistrés, les actions serveur, le refus motivé et les conflits.
La vérification visuelle manuelle des écrans reste à effectuer ; aucun navigateur
pilotable n'était disponible dans cette session.

Les politiques RH manquantes demeurent celles décrites dans CONGES.md.
Pour le courrier, confirmer les classifications/confidentialités, les acteurs,
les délais, les règles d'archivage et les éventuels adaptateurs métier avant
activation d'un processus de production.

## Vérifications réalisées le 8 octobre 2026

- Backend : 174 tests vérifiés au total, sans échec, erreur ni test ignoré
  (suite complète puis contrôle ciblé du stockage et des droits utilisateurs).
- Frontend : 77 tests réussis avec ChromeHeadless, dont le dépôt de fichier,
  les déplacements des étapes/champs et les protections de publication.
- `mvn.cmd -q -DskipTests package` : compilation et JAR produits.
- `npm.cmd run build` : compilation de production réussie ; bundle initial
  de 473,23 kB, sans avertissement de budget Angular.
- Migrations : création d'une base logique vide et adoption du schéma existant
  vérifiées dans des schémas PostgreSQL temporaires, jusqu'à la version 1.26.

Les commandes de test et compilation n'ont pas appliqué les migrations à la
base réelle ni déployé l'application. La recette visuelle manuelle et la
configuration des permissions/processus restent nécessaires pour la mise
en service.

Vérification du 9 octobre 2026 : correction de l'URL du bouton **Simuler le circuit**
et ajout d'un test vérifiant la transmission du paramètre `agentId` encodé.
Les 78 tests frontend et la compilation Angular de production réussissent.
Le parcours complet des absences est détaillé dans le guide d'utilisation.

Vérification du 10 octobre 2026 : la simulation organisationnelle RH n'exige
plus PROCESSUS_VALIDER, propre aux dossiers génériques. La simulation configurable
affiche le circuit et ses titulaires lorsqu'une permission manque, avec une
indication d'attribution par l'administrateur. La simulation ne modifie aucun droit.
Un responsable organisationnel absent reste un blocage explicite ; ce diagnostic
n'est plus transformé en erreur interne par une transaction marquée en échec.
Les soumissions génériques conservent la vérification des permissions, y compris
lorsqu'elles utilisent un modèle hiérarchique. La simulation ne contrôle pas
les politiques, droits ou permissions spécifiques des congés ; leurs écrans
métier restent nécessaires pour ces vérifications.

Les 45 tests backend ciblés (processus et régressions du circuit RH) et les
79 tests frontend réussissent. Le package Maven et la compilation Angular de
production ont réussi. Les essais PostgreSQL utilisent les schémas temporaires
isolés ; aucune permission n'a été attribuée aux comptes de la base réelle.

Vérification du 10 octobre 2026 — suppression des circuits : les 23 tests API
PostgreSQL des processus et les 14 tests unitaires du circuit RH réussissent.
La recette couvre la suppression d'un doublon historique, les lots atomiques,
les versions utilisées par une démarche inactive ou une demande générique/RH,
les permissions, l'audit, les révisions périmées et une affectation concurrente
avec une suppression. Les 91 tests frontend réussissent, dont la fenêtre native,
la sélection, la confirmation, l'annulation, la limite de 100 éléments et les
refus du serveur. Le package Maven et le build Angular de production réussissent.
Les essais utilisent uniquement des schémas PostgreSQL temporaires ; les circuits
de la base réelle n'ont pas été supprimés.

Vérification du 11 octobre 2026 — fenêtre workflow : les 104 tests frontend
réussissent avec ChromeHeadless. Les contrôles couvrent les blocs et détails,
le glisser-déposer réel, le déplacement tactile, la sélection et le focus,
l'annulation, l'enregistrement versionné, les conflits et les vues en lecture
seule. Les 24 tests API PostgreSQL des processus réussissent, sans test ignoré.
Le nouveau test vérifie le changement d'ordre, sa publication et son affectation :
les nouveaux dossiers suivent le nouvel ordre, tandis que les publications et
brouillons de dossiers antérieurs restent associés à leur circuit initial.
Le build Angular de production réussit. Aucun navigateur pilotable n'était
disponible pour une recette visuelle manuelle ; les interactions sont testées
dans le navigateur ChromeHeadless. Aucune nouvelle migration ni modification
des circuits de la base réelle n'a été effectuée pour cette fenêtre.

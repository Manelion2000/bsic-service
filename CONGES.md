# Congés des agents : configuration et exploitation

Pour le parcours agent, validateur et RH, consulter le
[guide d'utilisation du portail](GUIDE-UTILISATION.md#6-congés--configurer-les-droits-puis-demander).

## Diagnostic et intégration

Le dépôt utilise Spring Boot 3.5.3, Java 17, PostgreSQL et Flyway ; le frontend
voisin `bsic-frontend` utilise Angular 19. Les agents, fonctions, services,
départements, agences, rôles, délégations et circuits RH existent déjà.
Les absences non imputables, sorties et reprises de service étaient disponibles.
Les dates de congé présentes dans une reprise n'étaient pas un module de droits.
Aucune politique RH, calendrier de travail ou acquisition annuelle exploitable
n'a été trouvé dans ces composants. Aucune intégration paie n'a été ajoutée.

Le module réutilise ces référentiels, l'authentification, l'audit et
`RhCircuitService`. Il ajoute un registre de droits, des versions de politiques
et de calendriers, des demandes avec révisions, le planning, la passation,
les justificatifs privés, l'outbox de notifications et les exports.
Les frameworks n'ont pas été changés pour cette intégration.

## Mise en service

La compilation ne modifie pas la base utilisée par l'application.
Au prochain démarrage, Flyway appliquera `V1.24__gestion_conges.sql` et
`V1.25__audit_conges_details.sql` après les
migrations existantes ; Hibernate reste en mode `validate`. Sauvegarder la base
et vérifier la migration en environnement de recette avant un déploiement.
Cette livraison n'effectue aucun déploiement de production.

1. Dans la gestion existante des utilisateurs/profils, attribuer les permissions
   choisies aux agents, responsables et personnes RH habilitées.
2. Ouvrir **Congés → Gestion RH → Politiques**. Créer une version issue d'un
   document RH identifié. Aucun volume de jours, taux d'acquisition ou choix de
   refus n'est prérempli. Vérifier les paramètres affichés puis confirmer.
3. Dans **Calendriers**, définir explicitement les sept jours, les plages matin
   et après-midi, leur poids en jours, les jours fériés/exceptions et le fuseau.
   Déclarer les jours non travaillés ; ne pas supposer une semaine lundi-vendredi.
   Confirmer le calendrier.
4. Dans **Droits et affectations**, affecter une politique active et un calendrier
   confirmé aux agents. Leurs intervalles d'effet doivent couvrir l'affectation.
   Renseigner les dates d'emploi si une proratisation est prévue.
5. Ouvrir les périodes de référence, ou reprendre des soldes arrêtés après
   simulation. Les dates de référence et d'expiration sont des décisions RH.
6. Vérifier le circuit et les permissions `CONGES_VALIDER` des titulaires/délégués.
   Configurer les deux pôles DGA dans le module existant de direction générale.
7. Ajouter et confirmer des règles de couverture seulement si la banque les
   exige. Affecter les calendriers de tous les membres du périmètre concerné.

Une politique est activable uniquement après confirmation du circuit des
absences, du modèle réservation/débit, du rôle administratif RH et du traitement
d'un refus. La personne demandant un congé ne choisit pas ses validateurs et ne
désactive pas le DG. Le paramètre DG est figé depuis la politique à la soumission.

| Permission | Usage |
|---|---|
| `CONGES_DEMANDER` | Brouillons, simulation, soumission et opérations personnelles |
| `CONGES_VALIDER` | Décision, avec fonction/périmètre/étape/délégation contrôlés |
| `CONGES_POLITIQUES` | Versions, calendriers et règles de couverture |
| `CONGES_DROITS` | Affectations, périodes, ajustements, reprises, reports et contrôles |
| `CONGES_PIECES` | Motifs et justificatifs sensibles |
| `CONGES_PLANNING` | Planning et synthèse dans le périmètre organisationnel |
| `CONGES_ANNULER` | Contrôle administratif d'annulation et de reprise |
| `CONGES_DEROGATION` | Dérogation justifiée à une règle de couverture |
| `CONGES_EXPORTER` | Export des compteurs autorisés, sans pièces ni motifs |
| `CONGES_AUDITER` | Consultation des opérations et anomalies |

Aucune permission n'est attribuée automatiquement, y compris à `ADMIN`.
Un administrateur technique ne devient pas chef/directeur/DGA/DG.
Une permission de validation ne permet pas de signer hors du circuit.
Les délégués doivent eux-mêmes posséder `CONGES_VALIDER`.
La gestion des délégations réutilise le référentiel existant et requiert
l'administrateur technique ; ce pouvoir ne lui permet pas de signer.
Les délégations des étapes DGA/DG sont exclues pour conserver la règle de l'autre DGA.

## Comptabilité et calcul

Chaque lot conserve l'agent, le type, l'origine, la politique, la disponibilité
et l'expiration. Les quantités utilisent `BigDecimal` et `numeric(18,4)`.
Les quantités de compteurs, calculs et imputations sont échangées comme chaînes
décimales ; le frontend n'effectue aucun calcul de solde.

`Disponible = crédits valides − débits nets − réservations actives`.

- Une dotation est créditée au début de référence ; une acquisition à la fin de
  son intervalle configuré. Une échéance future reste une projection.
- La soumission réserve. L'approbation finale libère la réservation et débite.
- Le rejet définitif ou le retrait libère. Un refus laissant d'autres validateurs
  décider conserve la réservation et l'historique du refus.
- Une annulation RH autorisée avant le départ restitue le débit.
- Un retour anticipé n'ajuste rien avant le contrôle RH. La partie non prise est
  calculée au serveur, avec fractions/heures et calendrier de la version figée.
- Une modification avant le départ conserve le congé approuvé original. Seul le
  supplément de droits est réservé ; l'accord final remplace la révision et ses
  imputations atomiquement. Son rejet/retrait laisse l'original valable.

Les congés approuvés à venir sont déjà inclus dans les débits : ils ne sont pas
soustraits deux fois. Le champ `pris` correspond aux imputations sur dates échues,
après restitution, dont la reprise a été explicitement constatée par les RH.
Le champ `echusNonConfirmes` conserve séparément les imputations échues sans cette
confirmation. La reprise effective n'est jamais déduite de la seule date de fin.
Dans la synthèse, `joursEchusImputes` est la somme de ces deux quantités ;
`prisConfirmes` et `echusSansRepriseConfirmee` restent affichés séparément.

Une avance autorisée constitue une dette plafonnée du lot d'origine ; elle ne
transforme pas les projections en crédits. Les lots de report n'ajoutent pas un
nouveau plafond d'avance. Les reports débitent les crédits libres de la source
et créent un lot conservant sa provenance, avec échéance propre.

Les dates de début/fin sont incluses. Chaque date utilise les plages configurées
et les exceptions du calendrier ; le calcul arrondit chaque date au pas confirmé.
La proratisation utilise soit un ratio de dates calendaires, soit un ratio de
dates travaillées ; un jour travaillé est une date comportant au moins une plage.
Le poids des plages détermine la durée, y compris le temps partiel.
Une politique d'acquisition mensuelle/trimestrielle/annuelle découpe la référence
en intervalles à partir de son début ; la proratisation de l'intervalle final
incomplet suit le mode explicitement choisi.

Une demande peut utiliser plusieurs périodes de droits. Si la politique ou la
version de calendrier change à l'intérieur de sa plage, la demande doit être
scindée : aucune interprétation entre versions n'est inventée.

Les dates d'expiration sont incluses. À l'expiration, seuls les crédits libres
sont expirés. Les réservations restent tracées ; une anomalie est créée. Le choix
confirmé protège jusqu'à la décision ou bloque le débit avec intervention RH.
Après libération/restitution, les droits libres déjà expirés sont expirés à leur tour.
Une clôture conserve tous les mouvements et requiert une justification.

## Planning, décisions et confidentialité

**Congés** présente les droits, les simulations, demandes et décisions.
Le planning filtre par intervalle, affiche les catégories d'indisponibilité et
les souhaits indicatifs sans motif médical. Les souhaits ne réservent rien.
Les chefs/directeurs avec permission de planning consultent leur département ;
DG/DGA disposent du périmètre de direction, les RH de droits/audit du périmètre RH.
Un agent conserve sa consultation personnelle.

Les règles de couverture sont des versions confirmées, avec effet avertissant ou
bloquant : minima, fonctions, groupes de compétences et incompatibilités.
La granularité disponible doit être explicitement confirmée : **journée entière**,
avec toute absence partielle comptée comme indisponibilité sur cette journée.
Une couverture à l'heure nécessite une évolution après confirmation métier.
Un calendrier absent/ambigu est signalé ; un résultat complet n'est pas inventé.
La capacité est contrôlée de nouveau à l'accord final, sous un verrou commun
couvrant aussi les groupes transversaux. Les absences et sorties finales utilisent
ce même contrôle lorsqu'une règle de congés a été explicitement confirmée.

Le premier accord valable clôt une étape. Les fonctions, étapes et signataires
proviennent du moteur RH existant. Une nouvelle décision ne remplace pas la
précédente. La fonction du signataire doit encore correspondre à l'étape.
La délégation doit être figée, active, non expirée et de même portée ; une
révocation s'applique aux demandes en cours. L'auteur réel et le délégant restent
dans l'historique. Aucune auto-validation ni circuit de DG n'est inventé.

Une passation distingue les tâches/remplacement de la délégation de signature.
Elle ne copie aucun rôle ni accès. Une passation obligatoire doit être acceptée
par le remplaçant et sa disponibilité est contrôlée au départ et à l'accord final.
Un remplacement obligatoire accepté est signalé lorsqu'il entre en conflit
avec une nouvelle indisponibilité du remplaçant.

Les justificatifs restent en stockage PostgreSQL privé et sont servis par une
API authentifiée. Limite actuelle : 5 Mo, PDF/PNG/JPEG, sans URL publique durable.
L'accès aux motifs/pièces exige la permission distincte, même pour un validateur
ou un auditeur. Le PDF d'autorisation n'existe qu'après accord final, affiche la
révision, les décisions réelles et l'état actuel, sans motifs confidentiels.
Ce document n'est pas une signature électronique certifiée.

## Concurrence et API

Les écritures de droits sont sérialisées par agent. La demande est chargée sous
verrou après ce verrou d'agent, pour éviter une version chargée avant une attente.
Chaque action exige `version` et `cle`, et la décision l'étape attendue. La création
de brouillon utilise l'en-tête `Idempotency-Key`. Une répétition identique retourne
le résultat courant ; une réutilisation avec contenu différent ou version périmée
renvoie un conflit 409. Ces contrôles sont au backend, indépendants des boutons.

Les traitements périodiques conservent échéances et clés uniques. Les rattrapages
verrouillent les agents dans un ordre stable et relisent après attente.
Le planning, droits, demande, décision et outbox appartiennent à la même transaction.

## Reprise des données

Les données d'absences/sorties et leurs historiques ne sont pas convertis en
congés. Aucun solde annuel ne leur est attribué automatiquement.

Dans **Reprise des soldes**, saisir l'arrêté, sa source et les lignes : affectation,
origine, expiration, solde disponible arrêté, justification. La simulation repère
clés dupliquées, origines recouvrantes, affectations invalides, droits existants et
demandes risquant un double comptage. Elle n'écrit aucun crédit.
La confirmation porte sur le contenu affiché et son empreinte, sous permission RH,
avec justification. La validation est atomique et idempotente ; chaque lot garde
la source et un mouvement `REPRISE_SOLDE`. Une quantité nulle ne crée pas de mouvement.

Une reprise ne génère pas de nouvelles acquisitions sur la période importée.
Les acquisitions futures de cette période doivent être rapprochées et configurées
explicitement par les RH, ou la période suivante ouverte séparément. Ne pas ouvrir
deux origines recouvrantes et ne pas importer un solde net puis débiter une seconde
fois les anciennes demandes qu'il intègre. Les anomalies exigent un rapprochement
manuel documenté ; aucune approbation historique manquante n'est fabriquée.

Après migration, le retour arrière n'est pas une suppression des nouvelles tables :
il ferait perdre leurs mouvements et décisions. Conserver une sauvegarde avant
mise en service et utiliser une migration corrective. Un retour à une sauvegarde
nécessite une fenêtre d'arrêt et un rapprochement des opérations intervenues.

## Notifications et exploitation quotidienne

Les notifications internes sont créées dans une outbox transactionnelle unique
par événement/destinataire. Un email ne peut être expédié qu'après commit.
Les essais sont repris avec délai croissant, jusqu'à dix essais ; les clés empêchent
de recréer l'événement métier. Un crash après l'envoi SMTP et avant son commit peut
encore produire une répétition de transport : le SMTP ne garantit pas l'exactement-une-fois.

Par défaut, traitements automatiques et emails sont désactivés. L'action RH
**Traiter les échéances / expirations** permet un rattrapage contrôlé sans email.
Après validation de la configuration et du SMTP, les paramètres sont :

```yaml
app:
  conges:
    traitements-automatiques-enabled: true
    notifications-mail-enabled: true
    fuseau-traitements: Africa/Ouagadougou
    acquisition-cron: "0 5 0 * * *"
    notifications-delai-ms: 60000
```

Les variables `BSIC_CONGES_TRAITEMENTS_ENABLED`, `BSIC_CONGES_MAIL_ENABLED` et
`BSIC_CONGES_FUSEAU` remplacent ces valeurs. Les messages n'incluent pas de motif,
pièce médicale ou mot de passe. Aucun rappel automatique de départ/expiration
n'est activé sans une fréquence et des destinataires RH confirmés.
Surveiller les anomalies ouvertes, échéances, reports et réservations expirées.
Pour les erreurs SMTP définitives, corriger l'adresse/configuration avant une
reprogrammation contrôlée de l'entrée d'outbox ; ne pas rejouer la décision RH.

Les synthèses indiquent intervalle, périmètre, unité par type, compteurs actuels
des origines recouvrantes, demandes par état/étape et délai moyen des approbations
finales. Les quantités de jours et d'heures ne sont jamais additionnées entre unités.
Les exports ne classent pas les agents à partir de causes médicales.

## Décisions métier restant à confirmer

Les questions ont été posées ; aucune réponse n'a encore été intégrée comme
politique de la banque. Le formulaire exige les choix avant activation : droits,
mode/fréquence d'acquisition, calendrier, unités, proratisation, arrondis, reports,
expiration, avance, circuit, refus, contrôle RH, remplacement et couverture.
Préciser aussi les modifications/prolongations après le début, les rappels
exceptionnels et les éventuels rappels de notifications avant de les autoriser.

Le quota de **10 jours d'absence non imputable** demeure entièrement séparé.
Sa période de référence et sa règle de décompte n'étant pas définies dans les
éléments accessibles, son enforcement annuel n'a pas été inventé. Le module le
signale explicitement comme non configuré. Il ne vaut jamais droit annuel à congé
et un dépassement n'est jamais converti automatiquement en congé annuel.

## Vérification

Les tests PostgreSQL utilisent uniquement des schémas temporaires aléatoires
`bsic_conges_test_*`, `bsic_bootstrap_test_*`, etc. Ils n'écrivent pas dans le schéma
de l'application. Le courrier est neutralisé par des mocks et les calendriers/
volumes utilisés sont des fixtures de recette, sans valeur de politique bancaire.
Voir `FLYWAY.md` pour les variables de connexion de test.

```powershell
mvn.cmd test
mvn.cmd -DskipTests package
cd C:\workspace\bsic-frontend
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build
```

Les tests couvrent calcul exact, fractions/fériés/temps partiel, proratisation,
deux périodes, avances/projections, acquisitions idempotentes, reports/expiration,
réservation/débit/libération/restitution, révisions, retour partiel, fonctions du
circuit, décisions concurrentes, capacité concurrente, API hors périmètre,
confidentialité, passation sans transfert d'accès, délégations, outbox/reprise,
migrations et régressions absences/sorties. Les règles RH non confirmées ne sont
pas présentées comme ayant passé une recette bancaire.

Vérification du 8 octobre 2026 : **160 tests backend réussis**, sans échec,
erreur ni test ignoré, et **65 tests frontend réussis**. Le package Maven et
la compilation Angular de production ont réussi. Angular signale les dépassements
de budgets déjà présents pour le bundle initial et le composant de circuit existant.
Les migrations de congés ont été exécutées dans les schémas isolés des tests ;
elles n'ont pas été appliquées au schéma réel de l'application lors de cette livraison.
La recette visuelle manuelle reste à effectuer : aucun navigateur pilotable n'était
disponible dans la session. Les tests Angular vérifient les formulaires, les actions
serveur, les conflits de version et les confirmations RH.

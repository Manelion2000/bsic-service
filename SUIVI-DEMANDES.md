# Suivi des demandes par département et pour la banque

## Solution

Les consultations existantes donnent des droits propres à chaque module et
peuvent inclure des motifs, formulaires et pièces. Le nouvel espace Angular
`/suivi-demandes` fournit une projection commune de l'avancement. Il réutilise
les comptes, rôles/profils, circuits enregistrés et la fenêtre workflow. Il ne
crée pas de moteur de validation ni de copie de demandes.

La migration `V1.27__suivi_demandes_par_perimetre.sql` crée deux rôles actifs,
sans les attribuer, et la vue PostgreSQL `ba_suivi_demande`. Les modules inclus
sont les démarches génériques, absences, sorties, congés, reprises, habilitations
et réinitialisations de comptes. V1.30 ajoute les demandes d'extraction Oracle,
avec leur état de traitement et sans exposer le SQL ou les critères.
Le drapeau Habilitations reste effectif sur
les listes, filtres et détails du suivi.

## Autorisation et confidentialité

- `SUIVI_DEMANDES_DEPARTEMENT` : département actuel de l'observateur, déduit de
  son affectation directe ou, à défaut, de son service. Rattachement manquant :
  blocage explicite. Aucun accès aux personnes sans département.
- `SUIVI_DEMANDES_GLOBAL` : tous les départements et personnes sans département.
  Ce rôle prévaut si les deux permissions sont attribuées.
- Les rôles actifs du compte et de son profil actif sont relus en base à chaque
  requête ; les anciennes autorités d'un JWT ne suffisent pas après révocation.
- Aucun accès implicite pour `BA_ADMIN`, une fonction ou un rôle d'audit.
- Le périmètre organisationnel du demandeur est son rattachement actuel. Le
  circuit consulté est le circuit enregistré de sa demande et reste figé.
- Les filtres de périmètre sont appliqués en SQL avant comptage et pagination.
  Un filtre étranger donne 403 pour un observateur départemental ; un identifiant
  externe, désactivé ou de brouillon donne 404 après contrôle de permission.
- Les DTO ne reprennent pas les contenus métier : pas de titre personnel,
  valeurs de formulaire, motif, commentaire, justificatif, passation ou document.
  Les décisions montrent auteur, résultat et date, sans commentaire.
- Le circuit renvoyé reste `peutDecider=false`. Les routes existantes de contenu,
  pièces et décisions gardent leurs contrôles ; aucune permission n'est ajoutée.
- Les consultations réussies des listes et détails sont journalisées avec
  auteur, action, périmètre et cible. Les réponses utilisent `Cache-Control: no-store`.

## API

| Requête GET | Résultat |
|---|---|
| `/api/suivi-demandes/configuration` | Accès effectif, périmètre, département, filtres autorisés et blocage éventuel |
| `/api/suivi-demandes` | Page des demandes soumises du périmètre autorisé |
| `/api/suivi-demandes/{module}/{id}` | Métadonnées et circuit en lecture seule, expurgé des contenus sensibles |

Les filtres sont `module`, `etat`, `recherche`, `departementId`, `page` et `size`.
Par défaut : `etat=EN_ATTENTE`, page 0, 20 résultats. Taille maximale : 100.
`etat=` affiche tous les dossiers soumis. Les états approuvés des anciens
modules sont normalisés en `APPROUVEE`. Le filtre **En validation ou en traitement**
regroupe les signatures attendues et les extractions nouvelles/en cours.
Une extraction terminée conserve l'état `TERMINEE`, distinct d'une approbation.
La colonne d'évolution indique **Nouvelle demande** ou **Traitement informatique**.
Elle ne présente ni étapes approuvées ni bouton de circuit de signature pour
les extractions. Les contrôles de détail restent effectifs par appel API direct.

L'ordre des résultats est stable : date de soumission décroissante, module,
identifiant. Les recherches portent sur nom, matricule et identifiant ; `%`
et `_` sont recherchés littéralement. Les étapes des anciens modules sont lues
dans leurs tables de décisions. Un circuit non enregistré est signalé sans
reconstituer d'approbation. Les congés gardent leurs droits et le quota d'absence
de 10 jours reste indépendant.

Le détail métier des extractions reste dans son module protégé par ses propres
permissions. Le suivi conserve le rattachement actuel du demandeur ; la demande
d'extraction conserve séparément son département d'origine. Voir
[EXTRACTIONS-ORACLE.md](EXTRACTIONS-ORACLE.md).

## Mise en service

Appliquer les migrations en recette, puis attribuer la permission choisie dans
la gestion des rôles existante. Une compilation ne migre pas la base réelle.
Ne pas renommer les codes des permissions. Les autres droits du compte restent
cumulatifs : utiliser un compte `BA_CONNECT` + suivi pour un observateur dédié.

Le bouton Actualiser et la garde de route relisent les permissions au serveur.
Une réponse de refus efface les anciens résultats. Aucun export de suivi ni
action métier n'est proposé dans cet espace.

Pour un retour arrière applicatif, retirer les permissions et désactiver la
route. La vue ne contient pas de données propres ; ne pas supprimer une entrée
de l'historique Flyway pour rejouer une migration.

## Vérifications du 11 octobre 2026

- `mvn.cmd -q verify` : 195 tests dans 22 suites, sans échec, erreur ni test ignoré ;
  JAR généré. Les tests PostgreSQL utilisent exclusivement des schémas temporaires.
- 117 tests Angular réussis avec ChromeHeadless et build de production réussi.
- La recette couvre le suivi des sept modules, les comptes sans département,
  les profils/rôles inactifs, les demandes de services différents d'un même
  département, les identifiants et filtres étrangers, les brouillons privés,
  les mutations, révocations, décisions finales, pagination et absence d'accès
  au contenu ou à la validation par le seul rôle de suivi.
- Les migrations d'une base logique vide et l'adoption d'un schéma existant sont
  vérifiées jusqu'à V1.27. Aucun rôle n'a été attribué et aucune migration n'a
  été appliquée au schéma réel par ces commandes.
- Le rendu interactif est couvert par les tests du navigateur sans interface.
  La recette visuelle manuelle reste à effectuer.

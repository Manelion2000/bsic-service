# Rattachement hiérarchique des agents

## Choix retenu

Un agent a un supérieur direct **N+1 : chef de son service**, et un supérieur
hiérarchique **N+2 : directeur de son département**. Le directeur constitue le
deuxième niveau de la hiérarchie ; l'intitulé « deuxième supérieur direct » est
remplacé par « supérieur N+2 » dans les formulaires.

Les fonctions proviennent de l'organigramme. Un rôle `BA_ADMIN` ne suffit pas pour
être proposé comme responsable métier.

## Enregistrement

1. Créer les départements et leurs services.
2. Créer le chef avec la fonction `CHEF_SERVICE` et le service concerné, puis le
   directeur avec la fonction `DIRECTEUR` et le département concerné.
3. Ouvrir **Annuaire → Collaborateurs** ou **Utilisateurs → Utilisateurs**.
4. Choisir la fonction **Agent**, son département et son service.
5. Les deux responsables disponibles uniques apparaissent automatiquement.
   Changer de département ou de service recalcule les propositions.
6. Enregistrer l'agent : le serveur recherche à nouveau les responsables et
   contrôle tout identifiant transmis, même par appel direct à l'API.

Un responsable doit avoir un compte actif, non supprimé et non verrouillé.
Le chef doit appartenir au service exact de l'agent et au même département ; le
directeur doit appartenir à ce département. Le chef d'un autre service n'est pas
proposé comme supérieur direct.

Un poste vacant reste non renseigné sans empêcher la création. Si plusieurs
chefs ou directeurs éligibles existent, le formulaire permet de choisir le
titulaire de référence. Aucun choix arbitraire n'est effectué. Sans choix, le
poste reste non renseigné ; il peut être précisé ultérieurement.

Ces rattachements ne dispensent pas des permissions de validation. Le circuit RH
reste calculé selon la fonction et figé à la soumission : tous les chefs éligibles
du département continuent de pouvoir traiter la première étape d'une demande
d'agent, conformément aux règles existantes. N+1/N+2 ne remplacent pas ce circuit.

## Compatibilité et API

Les colonnes et routes existantes sont réutilisées. Aucune migration n'est
nécessaire et aucun compte existant n'est réécrit automatiquement au démarrage.

Pour les écritures d'agents via `POST /api/users` ou `PUT /api/users/{id}` :

- `idSuperieur` enregistre le chef en N+1.
- `idSuperieurSecondaire` enregistre le directeur en N+2.
- Omettre ces champs permet la récupération des titulaires uniques disponibles.
- Des références hors périmètre, indisponibles, doublonnées ou vers l'agent lui-même
  sont refusées. Un choix explicite invalide n'est pas remplacé silencieusement.

Les anciennes fiches qui stockaient directeur puis chef sont présentées dans
l'ordre N+1/N+2 à l'ouverture du formulaire et normalisées lors de leur
enregistrement. Le serveur accepte également cet ancien ordre lorsqu'il s'agit
de responsables éligibles. Les demandes déjà soumises et leurs décisions ne sont
pas modifiées. Les fonctions chef, directeur, DGA et DG conservent leurs règles
existantes ; aucune nouvelle hiérarchie de direction n'est inventée.

L'accès temporaire à usage unique, les habilitations et les restrictions de
gestion des comptes restent applicables. Les tests PostgreSQL utilisent des
schémas temporaires ; le schéma applicatif n'est pas modifié pendant la recette.

## Vérification

Les tests couvrent N+1/N+2 automatiques, postes vacants, titulaires multiples,
choix explicites, changements de rattachement, anciennes fiches inversées,
comptes indisponibles, responsables hors périmètre, auto-rattachement interdit et
contrôles par appel direct à l'API. Les parcours Angular couvrent les deux écrans,
la modification par le formulaire et l'arrivée différée du référentiel.

Journaux : `target/agent-hierarchy-verify.log`,
`bsic-frontend/tmp/agent-hierarchy-tests.log` et `agent-hierarchy-build.log`.

Vérification du 13 octobre 2026 : **285 tests backend dans 29 suites**, aucun
échec, erreur ou test ignoré (`mvn.cmd -q verify`) ; **217 tests Angular réussis**
(`ng test --watch=false --browsers=ChromeHeadless`) ; build de production réussi.
Le JAR et le frontend compilés ont été générés. Installer ces versions et
redémarrer le backend pour utiliser le nouveau comportement ; cette évolution
n'ajoute aucune migration de base de données.

# Création du DG et des DGA

Deux entrées du menu **Utilisateurs** sont réservées aux administrateurs :
**Gestion du DG** (`/administration/dg`) et **Gestion des DGA** (`/administration/dga`).
Elles fonctionnent également lorsque les habilitations sont désactivées.

Renseigner nom, prénom, email de connexion, téléphone et sexe. Le matricule est
facultatif. Aucun département, service ou supérieur n'est obligatoire.
La fonction organisationnelle et les rôles `BA_CONNECT` + `BA_DG` ou `BA_DGA`
sont attribués par le serveur. Aucun rôle d'administration n'est attribué.
La création affiche une fenêtre de remise du mot de passe temporaire, valable
24 heures par défaut et utilisable une seule fois. Aucun e-mail n’est envoyé.
Le responsable doit choisir son mot de passe personnel à sa première connexion
avant de valider les demandes. Voir [ACCES-TEMPORAIRES.md](ACCES-TEMPORAIRES.md).

Pour un DGA, choisir **Opérations** ou **Développement**, une date de début et,
facultativement, une date de fin. Le compte et son affectation sont enregistrés
dans la même transaction. Un compte DGA déjà créé peut aussi être rattaché
depuis **Rattacher un DGA existant**. Le rôle technique seul ne suffit pas :
la fonction doit être `DIRECTEUR_GENERAL_ADJOINT` et le compte disponible.

Une période inclut ses deux dates. Les affectations actives ne peuvent pas se
chevaucher pour un même pôle, ni placer la même personne sur les deux pôles.
Pour remplacer un DGA, modifier la date de fin ou désactiver l'affectation
précédente. La désactivation conserve le compte et l'affectation dans l'historique.
Les circuits déjà soumis restent figés.

Un seul DG actif et non verrouillé est permis. Avant de créer son remplaçant,
désactiver le compte précédent dans **Gérer les comptes**. Cette règle s'applique
aussi aux créations, modifications et réactivations par l'API utilisateurs.
Les changements sont audités et sérialisés par un verrou transactionnel commun.

## DGA obligatoire, DG facultatif

Pour les absences, sorties et congés, le circuit nécessite au moins un DGA
éligible. Le DG est désactivé par défaut et n’est ajouté que si un configurateur
l’active explicitement. Les étapes chef/directeur applicables sont conservées.
Sans DG, l’accord d’un DGA finalise la demande ; pour un demandeur DGA, seul
l’autre DGA peut signer. Un DGA indisponible ne conduit pas à une approbation
automatique.

Créer les DGA dans **Utilisateurs → Gestion des DGA**, puis vérifier leurs pôles,
les dates des affectations et l’activation des comptes. La règle « un DGA suffit »
n’exige pas l’accord des deux titulaires. Les demandes de DGA nécessitent toutefois
le titulaire du pôle opposé.

## Si le DG a été ajouté explicitement au circuit

Si le message indique **« Aucun DG disponible »** ou **« Le circuit exige l’accord
du DG »**, le circuit impose cette dernière validation et aucun compte actif et
déverrouillé n’exerce la fonction organisationnelle **DIRECTEUR_GENERAL**.

1. Un administrateur ouvre **Utilisateurs → Gestion du DG** et crée le compte du
   véritable titulaire. Si le compte existe déjà, vérifier sa fonction et son
   activation dans **Utilisateurs → Utilisateurs**. Attribuer seulement BA_ADMIN
   ou BA_DG ne remplace pas la fonction DG.
2. Remettre l’accès temporaire au titulaire afin qu’il puisse définir son mot de
   passe personnel et utiliser son compte.
3. Sur le formulaire d’absence ou de sortie, cliquer sur **Actualiser le circuit**.
   Les dates et le motif restent saisis ; le bouton de soumission devient
   disponible si aucun autre blocage ne subsiste.

L’aperçu conserve l’étape DG lorsqu’elle est obligatoire et indique **À configurer**.
Les administrateurs disposent d’un lien **Ouvrir Gestion du DG** dans un autre
onglet pour garder le formulaire ouvert. Les autres utilisateurs sont invités à
contacter leur administrateur.

L’envoi reste contrôlé au backend : modifier le bouton dans le navigateur ou
appeler directement l’API ne permet pas de sauter le DG. Créer ce titulaire est
une opération d’administration de l’organigramme ; il n’est pas inventé ou créé
automatiquement à partir du compte administrateur initial.

Si la banque confirme une finalisation par le DGA, le configurateur peut publier
un circuit organisationnel sans DG obligatoire et l’affecter aux démarches
**ABSENCE** et **SORTIE**. Sans affectation publiée, le paramètre de déploiement
**BSIC_RH_DG_ENABLED** s’applique (false par défaut ; true uniquement pour
ajouter explicitement une validation DG). Un changement du paramétrage serveur
nécessite un redémarrage du backend. Le demandeur ne peut pas modifier ce choix.
Les circuits des demandes déjà soumises restent figés.

## API

Toutes les opérations ci-dessous exigent un administrateur connecté et actif.

- `GET/POST /api/direction-generale/dg` : comptes DG.
- `GET/POST /api/direction-generale/dga` : comptes DGA et création avec pôle.
- `GET/POST /api/direction-generale/affectations-dga` : affectations.
- `PUT/DELETE /api/direction-generale/affectations-dga/{id}` : modification ou désactivation.

Les anciens endpoints d'affectation DGA utilisent les mêmes contrôles.
La migration `V1.23` ajoute les deux rôles techniques sans modifier les migrations
déjà appliquées, ni créer automatiquement de responsables. Redémarrer le backend
pour appliquer Flyway puis actualiser le frontend.

## Vérification des circuits DG facultatif

Le 12 octobre 2026 : **271 tests backend dans 29 suites**, sans échec, erreur
ou test ignoré (mvn.cmd -q verify avec PostgreSQL dans des schémas temporaires),
et **205 tests Angular réussis** dans ChromeHeadless. Le JAR et le frontend de
production ont été reconstruits. Les tests vérifient la soumission des absences, sorties et congés sans DG
par défaut, le maintien d’au moins un DGA obligatoire, la finalisation au premier
accord DGA et le débit unique des congés. Ils vérifient aussi le blocage lorsque
un configurateur ajoute explicitement un DG indisponible, la conservation des
choix des publications et des circuits déjà figés. Ils contrôlent aussi l’actualisation sans perte de saisie et les
erreurs réseau. Aucun compte réel n’a été créé ou réactivé et aucun paramétrage
DG de la base applicative n’a été modifié.

Journaux : target/dg-optionnel-verify.log, tmp/dg-optionnel-tests.log et tmp/dg-optionnel-build.log
dans le dépôt correspondant. Ces contrôles automatisés ne constituent pas une
recette visuelle manuelle du navigateur.

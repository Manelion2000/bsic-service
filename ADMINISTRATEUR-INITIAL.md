# Administrateur initial au déploiement

Le premier administrateur peut être créé automatiquement au démarrage du backend,
après les migrations Flyway. Il utilise la connexion normale du portail et le rôle
existant `BA_ADMIN` pour créer les structures, comptes, profils et permissions.
Aucune fonction de chef, directeur, DGA ou DG ne lui est attribuée automatiquement.

## Paramètres

Les variables d’environnement suivantes sont reconnues directement par Spring Boot :

| Variable | Valeur par défaut | Utilisation |
|---|---|---|
| `APP_BOOTSTRAP_ADMIN_ENABLED` | `false` | Mettre `true` pour le premier déploiement |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | `admin@bsic.bf` | E-mail et identifiant de connexion ; 50 caractères maximum |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Aucune | Mot de passe temporaire choisi au déploiement ; au moins 12 caractères et au plus 72 octets UTF-8 |
| `APP_BOOTSTRAP_ADMIN_TELEPHONE` | Aucune | Téléphone obligatoire et distinct des comptes existants |
| `APP_BOOTSTRAP_ADMIN_NOM` | `Portail` | Nom affiché, modifiable, 50 caractères maximum |
| `APP_BOOTSTRAP_ADMIN_PRENOM` | `Administrateur` | Prénom affiché, modifiable, 50 caractères maximum |

Le mot de passe est fourni par l’opérateur ou par le gestionnaire de secrets du
déploiement. Aucun mot de passe universel n’est inscrit dans le dépôt, les migrations,
les journaux ou les réponses API. Sa casse et ses espaces sont conservés.
Seule son empreinte BCrypt est enregistrée en base.

Le téléphone est requis par le modèle de compte existant. Renseigner un contact
valide et unique ; ne pas reprendre le numéro d’un autre utilisateur.

## Premier démarrage sous Windows

Depuis le dossier `C:\workspace\bsic-service`, avec les paramètres PostgreSQL,
JWT et de déploiement habituels déjà configurés :

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:APP_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'admin@bsic.bf'
$env:APP_BOOTSTRAP_ADMIN_TELEPHONE = Read-Host 'Téléphone du compte administrateur initial'
$adminInitialSecret = Read-Host 'Mot de passe initial (12 caractères minimum)' -AsSecureString
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminInitialSecret).Password

try {
    java -jar target/param-service-1.0.0.jar
} finally {
    Remove-Item Env:APP_BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    $adminInitialSecret.Dispose()
}
```

Pour un service Windows ou un conteneur, définir les mêmes variables dans son
environnement de déploiement. Le secret ne doit pas être passé comme argument
visible de la commande Java ni commité dans un fichier de configuration.
La base PostgreSQL doit exister ; Flyway crée ou fait évoluer les tables.

## Connexion et création des autres comptes

1. Ouvrir la page **Connexion** du frontend.
2. Saisir l’e-mail configuré, `admin@bsic.bf` si le paramètre n’a pas été changé,
   et le mot de passe choisi au déploiement (valable 24 heures après création).
3. Choisir et confirmer le mot de passe personnel demandé, puis se reconnecter.
4. Créer les départements et services dans **Annuaire**.
5. Créer les agents dans **Annuaire → Collaborateurs** ou **Utilisateurs → Utilisateurs**.
6. Attribuer les permissions nécessaires dans **Gestion des rôles** et configurer
   les responsables, circuits et démarches.

Pour récupérer automatiquement la hiérarchie d'un agent, créer d'abord les
responsables avec leurs fonctions organisationnelles : **Chef de service** affecté
au service, et **Directeur** affecté au département. Le formulaire agent les place
respectivement en **N+1** et **N+2** lorsqu'un seul titulaire est disponible. Un rôle
technique administrateur ne fait pas d'un compte un chef ou un directeur.
Les postes vacants n'empêchent pas la création. Détails :
[Hiérarchie des agents](HIERARCHIE-AGENTS.md).

La création de l’administrateur initial ne dépend pas de l’envoi de mails.
Les autres comptes reçoivent un code temporaire affiché une seule fois dans une
fenêtre de remise directe. Aucun e-mail n’est envoyé. La première connexion impose
également un changement personnel avant tout accès aux modules.

Après cette première installation, mettre `APP_BOOTSTRAP_ADMIN_ENABLED=false`
et retirer le secret initial de l’environnement des prochains démarrages.
Le compte reste enregistré et utilisable avec son mot de passe courant.
Le menu **Mon mot de passe** permet un changement personnel. Toutes les anciennes
sessions sont invalidées après ce changement ; une nouvelle connexion est requise.
L’API authentifiée existante `PUT /api/users/change-password` permet de le changer
avec les champs `ancien`, `nouveau` et `confirmer`.

## Validation des absences, sorties et congés

La validation d’au moins **un DGA** est obligatoire. Le **DG est facultatif** et
désactivé par défaut pour les nouvelles demandes. Les étapes chef/directeur
continuent de dépendre de la fonction du demandeur. Un DGA demandeur doit obtenir
l’accord de l’autre DGA ; aucune auto-validation n’est permise.

Le circuit RH de secours utilise **BSIC_RH_DG_ENABLED=false** par défaut. Les
nouvelles politiques de congés et les nouveaux circuits organisationnels du
studio proposent également le DG désactivé. Un configurateur peut ajouter son
accord explicitement ; le demandeur ne peut pas modifier ce choix.

Après la mise à jour, redémarrer le backend et cliquer sur **Actualiser le circuit**
dans le formulaire d’absence ou de sortie. Les dates et le motif sont conservés.
Aucun compte DG n’est requis pour un circuit dont cette étape est désactivée.
La disponibilité des DGA et les autres responsables requis restent contrôlées.

Une variable de déploiement BSIC_RH_DG_ENABLED=true, une publication affectée ou
une politique existante imposant le DG conserve ce choix explicite. Pour une
publication, créer et affecter une nouvelle version sans DG ; pour les congés,
utiliser une politique confirmée sans DG. Les demandes déjà soumises gardent
leur circuit figé ; une annulation puis une nouvelle soumission peut être
nécessaire pour appliquer le nouveau circuit, sans fabriquer d’approbations.

L’administrateur initial reste un compte technique ; il n’est ni DG ni DGA
automatiquement. Voir [DIRECTION-GENERALE.md](DIRECTION-GENERALE.md) et
[CIRCUIT-VALIDATION-RH.md](CIRCUIT-VALIDATION-RH.md).

## Redémarrages, concurrence et reprise

- Si un compte possède déjà `BA_ADMIN`, directement ou via son profil,
  l’initialisation est ignorée. Son identité, mot de passe et permissions restent
  inchangés, même si les paramètres d’installation ont changé.
- Les comptes administrateurs désactivés, archivés ou verrouillés sont également
  pris en compte : ce mécanisme ne les réactive pas et ne sert pas de récupération.
- Si l’e-mail ou le téléphone appartient à un compte sans rôle administrateur,
  l’initialisation échoue explicitement. Aucun rôle n’est ajouté à ce compte.
- Les démarrages simultanés sont sérialisés par un verrou transactionnel sur le
  rôle `BA_ADMIN`. Une seule création et une seule trace d’audit sont produites.
- Le compte, son rôle et l’audit sont enregistrés dans la même transaction.
  L’audit porte l’origine `DEPLOIEMENT_INITIAL`, sans mot de passe ni empreinte.
- Si l’installation est activée mais ses paramètres nécessaires sont absents ou
  invalides et qu’aucun administrateur n’existe, le démarrage échoue avec un message
  de configuration. Il n’y a pas de création partielle ni de mot de passe de secours.

Le rôle `BA_ADMIN` est déjà créé par les migrations existantes. La migration
**V1.33** ajoute l’accès temporaire et le changement obligatoire, sans modifier
les mots de passe des comptes existants ni les anciennes migrations.
Une base contenant déjà un administrateur ne reçoit pas de compte supplémentaire.

## Vérification

Les tests vérifient les variables d’environnement, la désactivation du mécanisme,
les paramètres manquants, le hachage, les conflits d’identité, la connexion HTTP,
la création d’un agent, les administrateurs hérités d’un profil, la conservation
d’un mot de passe modifié, les redémarrages et les créations concurrentes.
Les tests PostgreSQL utilisent des schémas temporaires ; ils ne créent pas
d’administrateur dans le schéma applicatif.

Le premier accès du compte créé a également été vérifié : session limitée,
code consommé une fois, définition personnelle puis connexion normale.
Voir [ACCES-TEMPORAIRES.md](ACCES-TEMPORAIRES.md) pour réinitialiser les autres
utilisateurs et consulter les résultats de vérification du parcours complet.

Journaux : target/initial-admin-verify.log pour le mécanisme initial,
target/credentials-verify.log pour le parcours temporaire. Aucun déploiement,
compte ou changement de mot de passe n’a été effectué dans le schéma applicatif.

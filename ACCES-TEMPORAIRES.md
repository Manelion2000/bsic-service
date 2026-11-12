# Initialiser et réinitialiser un accès sans e-mail

## Administrateur

1. Ouvrir **Utilisateurs → Utilisateurs** avec un compte actif ayant **BA_ADMIN**, directement ou via un profil actif.
2. Dans la ligne de la personne, cliquer sur l’**icône de clé « Réinitialiser l’accès »**. L’action existe aussi dans **Voir détails → Sécurité du compte → Initialiser / Réinitialiser l’accès**.
3. Vérifier l’identité de la personne et saisir un **motif**. Cette action remplace son mot de passe et invalide ses anciennes sessions.
4. Cliquer sur **Générer le code temporaire**. La fenêtre affiche l’identifiant, le code masqué et sa date d’expiration ; les boutons **Afficher** et **Copier les identifiants** permettent la remise directe.
5. Remettre les informations à la personne en main propre ou par un canal privé autorisé. **Aucun e-mail n’est envoyé.** Fermer ensuite la fenêtre.

Le code est affiché une seule fois et n’est pas récupérable après fermeture. S’il est perdu, expiré, ou si la personne a quitté la connexion initiale sans terminer, générer un nouvel accès. Un nouvel accès invalide le précédent et son éventuelle session de changement.

La réinitialisation ne réactive pas un compte désactivé ou verrouillé. Utiliser d’abord l’action d’activation prévue. Pour son propre compte connecté, l’administrateur utilise **Mon mot de passe** ; la réinitialisation par administrateur vise un autre utilisateur.

Si le serveur signale « Le compte a changé », fermer la fenêtre et rouvrir l’action depuis la liste actualisée. Une ancienne fiche ne peut pas écraser une nouvelle réinitialisation. Le motif est conservé dans l’audit avec l’auteur réel et la date serveur.

## Utilisateur

1. Ouvrir **Connexion**, saisir l’identifiant et le mot de passe temporaire.
2. Le code est consommé à cette première connexion. L’application affiche obligatoirement **Choisissez votre mot de passe personnel**.
3. Saisir et confirmer un nouveau mot de passe : **12 caractères minimum**, maximum **72 octets UTF-8**, différent du code temporaire. Les caractères accentués peuvent occuper plusieurs octets.
4. Après enregistrement, se reconnecter normalement avec le mot de passe personnel.

Pendant le changement obligatoire, la session permet uniquement d’enregistrer le mot de passe personnel. Les modules restent inaccessibles, y compris par URL ou appel direct à l’API. Une erreur de confirmation laisse la session utilisable jusqu’à son expiration ; quitter ou laisser expirer cette session nécessite une nouvelle remise par l’administrateur.

**Mot de passe oublié ?** sur la page de connexion indique de contacter l’administrateur. Aucun code n’est envoyé par courrier électronique.

## Création d’un compte et premier administrateur

Les écrans **Utilisateurs**, **Annuaire → Collaborateurs**, **Gestion du DG** et **Gestion des DGA** affichent la même fenêtre de remise après une création réussie. Le nouveau compte exige le changement personnel à sa première connexion. Son secret ne réapparaît pas dans les listes ou les fiches.

Le secret de déploiement du premier administrateur est également temporaire et valable 24 heures après création. Sa première connexion impose le choix d’un mot de passe personnel. Voir [ADMINISTRATEUR-INITIAL.md](ADMINISTRATEUR-INITIAL.md). Les administrateurs déjà présents ne sont pas modifiés par un redémarrage.

## Configuration et mise à jour

| Propriété Spring Boot | Valeur par défaut | Bornes appliquées |
|---|---|---|
| app.security.password-access.temporary-validity-hours | 24 heures | 1 à 168 heures |
| app.security.password-access.setup-session-minutes | 15 minutes | 1 à 60 minutes |

Ces propriétés peuvent être fournies dans la configuration externe du backend. La durée du code s’applique aux nouvelles créations et réinitialisations ; les accès déjà délivrés conservent leur date d’expiration. La durée initiale du secret de déploiement est de 24 heures.

Le code aléatoire contient 18 octets générés par SecureRandom, encodés en Base64URL (24 caractères). Seule son empreinte BCrypt est persistée. Les réponses de création, réinitialisation et authentification portent **Cache-Control: no-store**. Le frontend garde le code uniquement dans la fenêtre en mémoire ; la copie dans le presse-papiers est une action explicite. La session de changement reste dans sessionStorage, même avec « Se souvenir de moi ».

Le modèle reprend les principes de [l’OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html) : secret aléatoire, durée limitée, usage unique, session limitée au changement et connexion normale après sa réalisation. La remise humaine remplace l’envoi par e-mail demandé habituellement.

La migration **V1.33__acces_temporaires_utilisateurs.sql** ajoute les états d’accès et la version des identifiants. Les comptes existants gardent leur mot de passe et n’ont pas de changement obligatoire ajouté automatiquement. Les anciens codes de récupération par e-mail sont invalidés ; leurs endpoints ne permettent plus de modifier un mot de passe. Les compteurs RH, rôles, fonctions et circuits ne sont pas modifiés par une réinitialisation.

Les transactions verrouillent et relisent le compte avant de consommer un code ou de modifier son accès. La version des identifiants est contrôlée pour chaque JWT à chaque requête : elle invalide les anciennes sessions sur toutes les instances, sans liste de révocation partagée. Deux changements simultanés ne peuvent consommer le même accès.

Pour activer le parcours, installer ensemble le backend reconstruit, le frontend reconstruit et laisser Flyway appliquer V1.33 au démarrage habituel. Aucun reset de compte réel ni déploiement n’est exécuté par les tests. Une sauvegarde préalable relève de la procédure de mise à jour existante. Ne pas supprimer les colonnes d’accès pour revenir à l’ancien mécanisme : une telle rétrogradation nécessite une reprise contrôlée des comptes temporaires et des sessions.

## Vérification

Les tests API PostgreSQL utilisent des schémas temporaires et une messagerie simulée : permissions directes et héritées, absence d’e-mail, usage unique, expiration, remplacement, anciennes sessions, changement obligatoire, hachage, validation, création, édition protégée, concurrence et audit. Les tests Angular vérifient la fenêtre, le masquage, la suppression du code à la fermeture, le routage forcé, les doubles soumissions et les erreurs du serveur.

Vérification du 12 octobre 2026 : **262 tests backend dans 29 suites**, sans échec, erreur ou test ignoré (mvn.cmd -q verify avec PostgreSQL dans des schémas temporaires), et **197 tests Angular réussis** dans ChromeHeadless (ng test --watch=false --browsers=ChromeHeadless). Le JAR et le frontend de production ont été reconstruits. Aucun compte du schéma applicatif n’a été réinitialisé.

Les commandes et résultats sont consignés dans les journaux target/credentials-verify.log (backend), tmp/credentials-tests.log et tmp/credentials-build.log (frontend). Ces contrôles incluent les composants réels dans ChromeHeadless ; aucune recette visuelle manuelle n’est revendiquée.

# Authentification et sessions — analyse et corrections

Périmètre : `bsic-service` et le frontend Angular trouvé dans `C:\workspace\bsic-frontend`.
Les corrections portent sur la connexion, les sessions JWT, la création des identifiants et la réinitialisation des mots de passe. Elles ne constituent pas un audit exhaustif de toutes les autorisations métier.

## Faiblesses identifiées et corrections

| Faiblesse observée | Conséquence | Correction |
| --- | --- | --- |
| Le compteur était réinitialisé pendant le chargement du compte, avant la validation du mot de passe ; les échecs de connexion ne l'incrémentaient pas. | Le blocage ne protégeait pas la connexion. | `BaAuthenticationService` compte les échecs et réinitialise le compteur après validation. |
| La clé de blocage incluait le port TCP distant. | Un changement de connexion produisait une nouvelle clé. | La clé utilise une adresse IP canonique, sans port. |
| `X-Forwarded-For` était accepté quel que soit l'émetteur. | Un client pouvait choisir la clé du compteur. | Seuls les proxies explicitement configurés sont acceptés ; la chaîne est parcourue depuis le proxy le plus proche. |
| Le compteur utilisait une initialisation différée et un incrément non atomique. | Des requêtes concurrentes pouvaient perdre des échecs. | Cache initialisé au constructeur, incréments atomiques et taille limitée à 10 000 entrées. |
| La date de connexion était modifiée avant la vérification du mot de passe. | Le chargement avait des effets de bord, même si une transaction pouvait ensuite annuler l'écriture. | Chargement en lecture seule ; écriture de la date uniquement après validation des identifiants. |
| Le contrôleur vérifiait d'abord l'existence et l'état du compte et masquait ensuite toutes les exceptions. | Réponses permettant de distinguer les comptes ; erreurs techniques présentées comme de mauvais identifiants. | Validation par `DaoAuthenticationProvider`, réponse générique 401 pour les identifiants refusés ; les erreurs techniques restent des erreurs serveur. |
| La clé de signature JWT était présente dans le code. | La même clé était partagée par les déploiements et accessible aux lecteurs du dépôt. | Secret externe obligatoire, Base64 et au moins 64 octets pour HS512. |
| Le filtre JWT ne vérifiait pas l'activation et utilisait les rôles du token. | Un token pouvait conserver des droits retirés ou rester utilisable sur un compte désactivé sans verrouillage. | Rechargement de l'état du compte et de ses droits actuels à chaque requête authentifiée. |
| Le filtre acceptait un token dans l'URL. | Le token pouvait apparaître dans l'historique et les journaux HTTP. | Seul l'en-tête `Authorization: Bearer ...` est utilisé. Le frontend télécharge déjà les documents avec HttpClient. |
| `/api/users` était déclaré public pour toutes les méthodes. | La liste était accessible sans connexion ; la protection de la création reposait uniquement sur le service. | Liste, création et activation nécessitent une session. Les exceptions publiques sont limitées à leurs méthodes HTTP. |
| Spring Data REST pouvait exposer directement les interfaces publiques des dépôts. | Ces routes pouvaient contourner les règles des services métier. | `spring.data.rest.detection-strategy: annotated` ; aucun dépôt actuel ne porte une annotation d'export REST. |
| Les comptes créés recevaient le même mot de passe initial. | Un identifiant connu suffisait pour tenter ce mot de passe. | Mot de passe initial aléatoire de 24 caractères, généré avec SecureRandom. |
| Une demande de réinitialisation désactivait immédiatement le compte ; le code n'était pas envoyé par cette méthode. | Une demande empêchait la connexion sans preuve de possession de l'adresse email. | La demande conserve l'état du compte et envoie un code aléatoire de 20 caractères hexadécimaux, valable deux heures. |
| La finalisation pouvait réactiver un compte et supposait que la date et le code étaient non nuls. | Réactivation involontaire et erreurs NullPointerException. | Les comptes inactifs ou verrouillés restent refusés ; vérifications des valeurs absentes. |
| Le mot de passe et le code pouvaient figurer dans certaines représentations et journaux. | Exposition de données sensibles dans les logs. | Mot de passe de l'entité exclu du JSON ; secrets supprimés de son toString et du journal du contenu des emails. |
| « Se souvenir de moi » ne changeait pas le stockage du frontend. | Toutes les sessions persistaient dans localStorage. | sessionStorage par défaut ; localStorage uniquement si l'option est cochée. |
| Le frontend envoyait le token à toutes les URLs et acceptait les tokens sans expiration. | Envoi possible à un autre service ; sessions locales invalides. | Contrôle de l'origine et du préfixe API ; décodage Base64URL et expiration obligatoire. |
| Un rejet de session ne supprimait pas le token local. | L'utilisateur restait dans une session refusée par le serveur. | Réponse 401 sur une requête authentifiée : nettoyage et redirection vers la connexion. Une réponse 403 conserve la session. |
| Le formulaire autorisait des soumissions simultanées et ignorait returnUrl. | Connexions répétées et perte de la destination demandée. | Boutons désactivés pendant l'appel, soumission par Entrée, reprise d'une destination interne uniquement. |
| Le bundle initial dépassait le budget de production après les corrections. | Échec du build Angular. | Chargement différé de la configuration des circuits, en conservant ses gardes et chemins. |

La connexion utilise maintenant le parcours :
`BaUserController → BaAuthenticationService → AuthenticationManager → DaoAuthenticationProvider → BaUserDetailsService`.
Le fournisseur vérifie le mot de passe BCrypt et les indicateurs du compte, puis efface les credentials du principal.

## Configuration avant le prochain démarrage

### Clé JWT

Définir `BSIC_JWT_SECRET` dans l'environnement du processus Java. La valeur doit être une clé aléatoire de **64 octets minimum**, encodée en Base64.

Exemple PowerShell pour un environnement local :

```powershell
$jwtBytes = New-Object byte[] 64
$jwtGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtGenerator.GetBytes($jwtBytes)
$env:BSIC_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtGenerator.Dispose()
mvn.cmd spring-boot:run
```

Cette variable ne persiste que dans ce terminal. En production, conserver une clé stable dans le gestionnaire de secrets du déploiement et l'injecter dans le service. Ne pas la committer ni la journaliser.

**Le backend refuse de démarrer si la clé manque ou est invalide.** Remplacer l'ancienne clé invalide les sessions émises avec elle : les utilisateurs devront se reconnecter. Les tests créent leurs propres clés temporaires et n'utilisent aucune clé de production.

### Proxies de confiance

Sans configuration, les en-têtes transmis sont ignorés et l'adresse du pair réseau est utilisée.

Si un reverse proxy transmet les adresses clients, définir `BSIC_TRUSTED_PROXIES` avec ses adresses IP exactes séparées par des virgules, par exemple :

```powershell
$env:BSIC_TRUSTED_PROXIES = '127.0.0.1,::1'
```

Ce réglage convient uniquement si ces adresses sont celles des proxies de votre déploiement. Le proxy en bordure doit supprimer ou normaliser les en-têtes fournis par les clients. À défaut de configuration derrière un proxy, ses utilisateurs partagent la même clé de limitation.

### Réponses et limites

- Connexion refusée : HTTP 401 et message générique.
- Seuil atteint : HTTP 429. Par défaut : cinq échecs, expiration du compteur cinq minutes après la dernière écriture.
- Demande et finalisation de réinitialisation : compteur séparé de celui de connexion ; cinq appels combinés par adresse sur la fenêtre configurée.
- Demande de réinitialisation pour un compte inconnu, inactif ou verrouillé : réponse normale sans révéler cet état.
- Session invalide, expirée, révoquée ou compte refusé : HTTP 401 au format JSON.
- Les durées JWT restent de 24 heures, ou 72 heures avec « Se souvenir de moi ».

## Vérifications réalisées

- **56 tests Java passants**, dont 29 tests ajoutés sur l'authentification, la concurrence, l'expiration, les proxies, les tokens, la réinitialisation et les routes de production.
- **12 tests Angular d'authentification passants** avec ChromeHeadless.
- **Build Angular de production réussi** : bundle initial de 939,76 Ko, sous le budget bloquant de 1 Mo.
- Des avertissements de taille subsistent pour le seuil indicatif de 500 Ko et les styles des pages de circuits.
- Les tests de la chaîne de production utilisent un contexte Spring MVC et les vrais filtres, avec des dépendances de données simulées. Aucune connexion à la base de production ni aucun envoi réel d'email n'a été effectué.

Commandes utilisées :

```powershell
# Depuis bsic-service
mvn.cmd '-Dmaven.repo.local=C:\Users\BSIC\.m2\repository' -q test

# Depuis bsic-frontend
$env:CHROME_BIN = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
npm.cmd test -- --watch=false --browsers=ChromeHeadless '--include=src/app/services/auth*.spec.ts' '--include=src/app/pages/login/login.component.spec.ts'
npm.cmd run build -- --configuration production
```

## Limites restant à traiter

- Les compteurs et la révocation des JWT restent en mémoire dans chaque instance. Leur état n'est ni partagé entre serveurs ni conservé après un redémarrage. Un stockage partagé est nécessaire pour un déploiement avec plusieurs instances.
- Le changement de mot de passe ne révoque pas globalement les JWT déjà émis. Une version persistante des credentials permettrait cette invalidation.
- Les comptes créés avant cette correction conservent leur ancien mot de passe. Leur renouvellement doit être organisé.
- La finalisation de réinitialisation est disponible via l'API existante ; le frontend ne dispose toujours que du bouton de demande, sans écran de saisie du code et du nouveau mot de passe.
- L'envoi effectif des codes dépend de la configuration SMTP existante ; sa livraison n'a pas été vérifiée.
- Les tokens stockés dans le navigateur restent accessibles à JavaScript. Un passage aux cookies HttpOnly nécessite aussi d'adapter la protection CSRF.
- Le profil `dev` conserve sa configuration permissive existante et doit rester limité à un environnement de développement.

## Références

- [Spring Security : DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html)
- [Spring Framework : en-têtes transmis et frontière de confiance](https://docs.spring.io/spring/reference/web/webmvc/filters.html)
- [Spring Data REST : stratégies d'export des dépôts](https://docs.spring.io/spring-data/rest/docs/current/api/org/springframework/data/rest/core/mapping/RepositoryDetectionStrategy.RepositoryDetectionStrategies.html)
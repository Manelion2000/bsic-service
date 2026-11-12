# Circuit des absences et autorisations de sortie

Le champ `fonction` de l'utilisateur est la fonction organisationnelle de
reference. Les roles techniques, notamment `BA_ADMIN`, ne donnent aucun droit
de signature dans ces circuits. Le departement est celui du demandeur, ou celui
de son service si aucun departement direct n'est renseigne. Une incoherence
entre ces deux rattachements bloque la soumission.

| Fonction du demandeur | Etapes obligatoires |
| --- | --- |
| Agent | Un chef du departement → Directeur du departement → Un DGA |
| Chef de service | Directeur du departement → Un DGA |
| Directeur | Un DGA |
| DGA Operations | DGA Developpement |
| DGA Developpement | DGA Operations |
| DG | Circuit non defini : soumission bloquee |

Le DG est facultatif et désactivé par défaut. S’il est explicitement activé
par le configurateur, son accord constitue la dernière étape obligatoire.
Un DGA éligible reste toujours requis, même sans étape DG. Les affectations des DGA proviennent de
`ba_dga_pole_validateur` : elles doivent etre actives, couvrir la date de
soumission et identifier sans ambiguite le titulaire de chaque pole.
Le compte d'un validateur doit etre actif, active et non verrouille.

La creation des comptes ne necessite pas que les responsables soient deja
configures. Pour un agent, un departement actif et un service actif appartenant
a ce departement suffisent pour son rattachement organisationnel. Les superieurs
N+1/N+2 restent facultatifs pour les comptes : le chef du service est récupéré en
N+1, puis le directeur du département en N+2 lorsqu'un seul titulaire disponible
existe. Les titulaires multiples demandent un choix explicite, sans choix arbitraire.
Leurs fonctions et périmètres restent contrôlés, et tout rattachement à soi-même
est refusé. Ce rattachement ne restreint pas l'étape RH aux seuls supérieurs
enregistrés : tous les chefs éligibles du département restent disponibles pour
une demande d'agent. Voir [HIERARCHIE-AGENTS.md](HIERARCHIE-AGENTS.md).
Les responsables manquants bloquent
la soumission d'une demande RH, sans bloquer la creation du compte utilisateur.

## Parametrage serveur

```powershell
$env:BSIC_RH_DG_ENABLED = 'false' # valeur par défaut ; true pour ajouter le DG explicitement
```

Le traitement d'un refus dans un groupe doit etre choisi explicitement avant
toute decision :

- `BSIC_RH_REFUSAL_POLICY=DEFINITIF` : le premier refus rejette la demande.
- `BSIC_RH_REFUSAL_POLICY=AUTRES_VALIDATEURS` : le refus est audite, le signataire
  ne peut plus statuer a cette etape et les autres validateurs restent eligibles.
  Si aucun validateur disponible ne reste, la demande demeure bloquee.

En l'absence de choix, la valeur `NON_CONFIGURE` bloque les decisions dans une
etape a plusieurs validateurs. Elle ne choisit pas implicitement une politique.
Ce choix et l'option DG sont figes pour chaque nouvelle demande.
Le demandeur ne peut les modifier par le formulaire ni par l'API.

Les démarches **ABSENCE** et **SORTIE** utilisent en priorité la publication
organisationnelle qui leur est affectée. Sans publication, le paramétrage serveur
s’applique. Les **CONGES** utilisent le choix de la politique RH confirmée,
désactivé par défaut pour une nouvelle politique. Une publication CONGES, si
elle est affectée, doit correspondre aux choix DG et refus de cette politique.

Le studio propose le DG désactivé à l’ouverture du mode organisationnel. Les
versions et politiques déjà enregistrées gardent leurs choix explicites : une
nouvelle version doit être publiée/configurée pour les futures demandes. Aucun
choix DG n’est ajouté au formulaire du demandeur. Le premier accord valable d’un
DGA clôt sa seule étape ; le second ne peut pas modifier la décision ou débiter
une seconde fois les droits.

## Snapshot, ordre et audit

La migration `V1.22` ajoute `ba_rh_circuit` et les references depuis les demandes.
Le circuit conserve la fonction, le departement, les etapes applicables, les
identifiants et noms des validateurs eligibles et chaque decision signee.
Les changements ulterieurs de parametrage ou d'organigramme ne recalculent pas
ce circuit. Un compte devenu indisponible ne peut cependant pas signer.

Chaque signature verrouille la demande avec un verrou PostgreSQL dans la
transaction qui modifie le circuit et le statut de la demande. Le premier accord
clot l'etape. Les autres signatures et les appels perimes obtiennent une erreur
`409`. Un acteur non eligible ou le demandeur obtient une erreur `403`.
La demande reste `EN_ATTENTE` jusqu'a l'accord de la derniere etape obligatoire.
Le circuit dispose aussi d'un compteur de version JPA pour prevenir une ecriture
concurrente qui contournerait le verrou de la demande.

Les demandes historiques gardent leurs donnees et leurs decisions. Une demande
historique en attente sans snapshot est bloquee a la validation : une nouvelle
soumission est necessaire. Le circuit historique n'est pas reconstruit a partir
de l'organigramme actuel. L'annulation d'une ancienne absence reste possible.

## API et interface

- `GET /api/rh/circuit?demarche=ABSENCE` ou `SORTIE` affiche le circuit applicable et les blocages de configuration. Un aperçu bloqué conserve la fonction du demandeur, les étapes, le choix DG de la publication affectée (ou du paramétrage de secours), sa référence et `etapeBloquee` quand un titulaire manque.
- Les formulaires proposent **Actualiser le circuit** sans perdre les dates ni le motif. Un DG manquant est signalé explicitement, avec un accès à **Gestion du DG** pour les administrateurs. Aucun responsable ni aucune approbation n’est créé automatiquement.
- Les DTO de demandes exposent `circuit`, ses etapes et son historique.
- Les endpoints `/{id}/valider` et `/{id}/rejeter` exigent `etape`, index de
  l'etape courante renvoye dans le DTO. Le commentaire est obligatoire au refus.
- Les listes `a-valider` ne contiennent que les demandes accessibles a l'etape
  courante. Les controles de signature sont egalement appliques aux appels API directs.
- Le frontend distingue l'approbation d'une etape de la validation definitive.
  Les etapes non applicables ne sont jamais ajoutees comme deja approuvees.

Les reprises de service conservent leur traitement existant ; le nouveau circuit
concerne les absences et les autorisations de sortie.

## Quota d'absence : point restant a definir

Le quota separe de 10 jours n'est pas encore implemente dans cette evolution.
Sa periode de reference et son mode de decompte doivent etre precises pour
ajouter les reservations, le debit unique a la validation definitive et leurs
tests de concurrence. La duree des demandes existantes est calculee en jours
calendaires ; elle ne constitue pas un controle de quota.

## Verification

`RhCircuitServiceTest` teste les fonctions, les groupes, le DG optionnel,
l'auto-validation, les blocages et les deux politiques de refus explicites.
`RhCircuitPostgresApiTest` teste la vraie API authentifiee et les signatures
simultanees sur PostgreSQL. Il cree et supprime uniquement son schema temporaire
`bsic_rh_test_*`, avec les memes variables de connexion de test que
`FlywayInitialSchemaTest` dans [FLYWAY.md](FLYWAY.md).

```powershell
mvn.cmd '-Dtest=RhCircuitServiceTest,RhCircuitPostgresApiTest,FlywayInitialSchemaTest' test
```

Les tests PostgreSQL necessitent `BSIC_MIGRATION_TEST_URL`,
`BSIC_MIGRATION_TEST_USERNAME` et `BSIC_MIGRATION_TEST_PASSWORD`.
Les tests Angular du composant commun verifient les etapes affichees, les
signataires et les blocages ; les tests HTTP verifient l'index transmis a l'API.

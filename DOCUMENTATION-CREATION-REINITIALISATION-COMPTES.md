# Documentation - Creation et reinitialisation des comptes plateforme

Ce document decrit le fonctionnement des modules de creation de comptes plateforme et de reinitialisation de comptes.

## 1. Concepts

Une plateforme represente une application ou un systeme pour lequel un utilisateur peut demander un acces.

Chaque plateforme appartient a un type:

- `RESEAUX`
- `CORE_BANKING`

Chaque plateforme doit etre liee a un circuit. Le circuit contient les etapes d'avis a valider avant que la demande puisse passer au traitement final.

## 2. Roles

Les roles ne doivent pas etre confondus avec les types de plateforme.

Types de plateforme:

- `RESEAUX`
- `CORE_BANKING`

Roles pour la creation de comptes:

- `HABILITATION_CREATION_COMPTE_RESEAUX`
- `HABILITATION_CREATION_COMPTE_CORE_BANKING`

Roles pour la reinitialisation de comptes:

- `HABILITATION_REINITIALISATION_COMPTE_RESEAUX`
- `HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING`

Regle d'autorisation:

- Une personne ayant `HABILITATION_CREATION_COMPTE_RESEAUX` peut creer les comptes des plateformes de type `RESEAUX`.
- Une personne ayant `HABILITATION_CREATION_COMPTE_CORE_BANKING` peut creer les comptes des plateformes de type `CORE_BANKING`.
- Une personne ayant `HABILITATION_REINITIALISATION_COMPTE_RESEAUX` peut traiter les reinitialisations des plateformes de type `RESEAUX`.
- Une personne ayant `HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING` peut traiter les reinitialisations des plateformes de type `CORE_BANKING`.
- `BA_ADMIN` peut tout voir et tout traiter.

## 3. Creation de comptes plateforme

### 3.1 Parcours fonctionnel

1. Le demandeur cree une fiche d'habilitation.
2. Il selectionne une ou plusieurs plateformes.
3. Toutes les plateformes selectionnees doivent utiliser le meme circuit.
4. Les validateurs donnent leurs avis selon les etapes du circuit.
5. Quand toutes les etapes sont validees, la fiche passe a `VALIDEE`.
6. Le systeme cree automatiquement une ligne de compte a creer pour chaque plateforme de la fiche.
7. Une personne habilitee traite la creation du compte.

### 3.2 Statuts de creation

Chaque compte plateforme a un statut de creation:

- `A_CREER`: le compte est en attente de creation.
- `EN_COURS_CREATION`: un agent habilite a commence le traitement.
- `CREE`: le compte a ete cree.

Le systeme conserve l'audit:

- personne qui a cree le compte;
- date de debut de creation;
- date de creation;
- commentaire de creation.

### 3.3 Utilisation dans le frontend

Menu:

- `Habilitations > Comptes a creer`

Actions disponibles:

- filtrer les comptes par statut;
- demarrer la creation;
- marquer le compte comme cree;
- renseigner un commentaire.

La liste affiche uniquement les comptes que l'utilisateur connecte a le droit de traiter selon ses roles et le type de plateforme.

### 3.4 Endpoints backend

Lister les comptes plateforme:

```http
GET /api/habilitations/comptes-plateforme
GET /api/habilitations/comptes-plateforme?statutCreation=A_CREER
```

Demarrer la creation:

```http
PUT /api/habilitations/comptes-plateforme/{id}/demarrer?commentaire=...
```

Marquer le compte comme cree:

```http
PUT /api/habilitations/comptes-plateforme/{id}/marquer-cree?commentaire=...
```

## 4. Reinitialisation de comptes

### 4.1 Prerequis

Une demande de reinitialisation ne peut etre creee que si le compte existe deja pour le demandeur et la plateforme.

Concretement, il doit exister un compte plateforme avec:

- le meme demandeur;
- la meme plateforme;
- `statutCreation = CREE`.

Si ce prerequis n'est pas respecte, le backend retourne:

```text
Le compte doit deja etre cree avant une reinitialisation.
```

### 4.2 Parcours fonctionnel

1. Le demandeur cree une demande de reinitialisation pour une plateforme.
2. Le systeme recupere le circuit lie a cette plateforme.
3. Le systeme initialise les etapes d'avis du circuit.
4. Les validateurs donnent leurs avis dans l'ordre du circuit.
5. Si une etape est rejetee, la demande passe a `REJETEE`.
6. Si toutes les etapes sont validees, la demande passe a `VALIDEE`.
7. Le systeme cree automatiquement un traitement de reinitialisation.
8. Une personne habilitee demarre le traitement.
9. La personne habilitee marque la reinitialisation comme traitee.

### 4.3 Statuts de la demande

La demande de reinitialisation peut avoir les statuts suivants:

- `EN_COURS`: les avis sont en cours.
- `VALIDEE`: tous les avis ont ete favorables.
- `REJETEE`: au moins une etape a ete rejetee.

### 4.4 Statuts du traitement

Apres validation de tous les avis, le traitement de reinitialisation est cree avec un statut:

- `A_TRAITER`: la reinitialisation attend un agent habilite.
- `EN_COURS_TRAITEMENT`: un agent habilite a commence le traitement.
- `TRAITEE`: la reinitialisation est terminee.

Le systeme conserve l'audit:

- personne qui a traite la reinitialisation;
- date de debut du traitement;
- date de traitement;
- commentaire de traitement.

### 4.5 Utilisation dans le frontend

Menu:

- `Habilitations > Reinitialisation comptes`

La page contient:

- un formulaire de nouvelle demande;
- la liste des demandes de l'utilisateur connecte;
- la liste des avis a donner;
- la liste des reinitialisations a traiter.

Actions disponibles:

- creer une demande de reinitialisation;
- valider une etape;
- rejeter une etape avec motif;
- demarrer le traitement;
- marquer la reinitialisation comme traitee.

La liste des traitements affiche uniquement les plateformes que l'utilisateur connecte peut traiter selon ses roles.

### 4.6 Endpoints backend

Creer une demande:

```http
POST /api/habilitations/reinitialisations-comptes
Content-Type: application/json

{
  "idPlateforme": "id-plateforme",
  "motif": "Motif de la reinitialisation"
}
```

Lister mes demandes:

```http
GET /api/habilitations/reinitialisations-comptes/mes-demandes
```

Lister les demandes a valider:

```http
GET /api/habilitations/reinitialisations-comptes/a-valider
```

Valider ou rejeter une etape:

```http
PUT /api/habilitations/reinitialisations-comptes/etapes/{id}/status?statut=VALIDE
PUT /api/habilitations/reinitialisations-comptes/etapes/{id}/status?statut=REJETE&commentaire=...
```

Lister les traitements:

```http
GET /api/habilitations/reinitialisations-comptes/a-traiter
GET /api/habilitations/reinitialisations-comptes/a-traiter?statutTraitement=A_TRAITER
GET /api/habilitations/reinitialisations-comptes/a-traiter?statutTraitement=EN_COURS_TRAITEMENT
GET /api/habilitations/reinitialisations-comptes/a-traiter?statutTraitement=TRAITEE
```

Demarrer un traitement:

```http
PUT /api/habilitations/reinitialisations-comptes/traitements/{id}/demarrer?commentaire=...
```

Marquer comme traitee:

```http
PUT /api/habilitations/reinitialisations-comptes/traitements/{id}/marquer-traitee?commentaire=...
```

## 5. Parametrage obligatoire

Pour que les deux modules fonctionnent correctement:

1. La plateforme doit avoir un `typePlateforme`.
2. La plateforme doit etre liee a un circuit.
3. Le circuit doit contenir au moins une etape.
4. Chaque etape doit permettre de trouver un validateur.
5. Les agents de creation ou de reinitialisation doivent avoir les roles correspondants.

## 6. Points de controle en base

Verifier les roles:

```sql
select id, code, libelle
from ba_role
where code in (
  'HABILITATION_CREATION_COMPTE_RESEAUX',
  'HABILITATION_CREATION_COMPTE_CORE_BANKING',
  'HABILITATION_REINITIALISATION_COMPTE_RESEAUX',
  'HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING'
)
order by code;
```

Verifier les plateformes:

```sql
select id, code, nom, type_plateforme, circuit_id
from ba_plateforme
order by code;
```

Verifier les comptes crees:

```sql
select h.id,
       u.username as demandeur,
       p.code as plateforme,
       p.type_plateforme,
       h.statut_creation,
       h.cree_par_id,
       h.date_creation
from ba_habilitation_compte_plateforme h
join ba_utilisateur u on u.id = h.demandeur_id
join ba_plateforme p on p.id = h.plateforme_id
order by h.created_date desc;
```

Verifier les traitements de reinitialisation:

```sql
select r.id as demande_id,
       u.username as demandeur,
       p.code as plateforme,
       p.type_plateforme,
       r.statut_demande,
       t.statut_traitement,
       t.traite_par_id,
       t.date_traitement
from ba_reinitialisation_compte r
join ba_utilisateur u on u.id = r.demandeur_id
join ba_plateforme p on p.id = r.plateforme_id
left join ba_reinitialisation_compte_traitement t on t.demande_id = r.id
order by r.created_date desc;
```

## 7. Erreurs courantes

`Aucun circuit n'est lie a cette plateforme.`

La plateforme n'a pas de circuit. Il faut renseigner `ba_plateforme.circuit_id`.

`Aucune etape configuree pour ce circuit.`

Le circuit existe mais ne contient aucune etape utilisable par le module.

`Aucun signataire trouve pour l'etape`

Le circuit contient une etape, mais aucun utilisateur actif ne correspond au validateur attendu.

`Le compte doit deja etre cree avant une reinitialisation.`

La reinitialisation est demandee alors qu'aucun compte `CREE` n'existe pour le demandeur et la plateforme.

`Vous n'etes pas autorise a reinitialiser un compte pour ce type de plateforme.`

L'utilisateur connecte n'a pas le role de reinitialisation correspondant au type de plateforme.

## 8. Resume

La creation de compte part d'une fiche d'habilitation validee et aboutit a un compte plateforme `CREE`.

La reinitialisation part obligatoirement d'un compte deja `CREE`, repasse par un circuit d'avis, puis aboutit a un traitement `TRAITEE`.

Les roles de traitement sont separes par type de plateforme afin que les equipes reseaux et core banking ne voient et ne traitent que leurs propres plateformes.

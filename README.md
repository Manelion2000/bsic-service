# README - Exploitation du Circuit de Validation

Ce document explique comment utiliser le module **Circuit** dans `bsic-service`:
- creer un circuit,
- l'associer a une plateforme,
- l'utiliser dans le workflow des fiches d'habilitation.

## 1. Prerequis metier

Avant de creer une fiche d'habilitation, il faut que ces donnees existent:
- employe
- plateforme
- circuit

## 2. Regles importantes du circuit

Un circuit contient des etapes ordonnees.
Chaque etape porte:
- un departement (`departementId`)
- un ordre (`ordre`)
- un role validateur optionnel (`roleId`)
- `obligatoire`
- `actif`

Regles validees par le backend:
- libelle obligatoire
- au moins une etape
- departements existants
- role existant si renseigne
- pas de doublon d'ordre dans un meme circuit
- pas de doublon de departement dans un meme circuit
- DGA et DG obligatoires dans le circuit

## 3. Endpoints Circuit

Base URL: `/api/circuits`

- `POST /api/circuits` : creer un circuit
- `PUT /api/circuits/{id}` : modifier un circuit
- `GET /api/circuits/{id}` : detail d'un circuit
- `GET /api/circuits` : liste des circuits
- `GET /api/circuits/actifs` : liste des circuits actifs
- `DELETE /api/circuits/{id}` : desactivation/suppression logique

## 4. Creer un circuit

### Requete
`POST /api/circuits`

```json
{
  "libelle": "Circuit ouverture compte standard",
  "description": "Circuit de validation standard pour ouverture de comptes",
  "actif": true,
  "etapes": [
    {
      "departementId": "DEP-INIT",
      "roleId": "ROLE-CHEF-SERVICE",
      "ordre": 1,
      "obligatoire": true,
      "actif": true
    },
    {
      "departementId": "DEP-RH",
      "roleId": "ROLE-CHEF-RH",
      "ordre": 2,
      "obligatoire": true,
      "actif": true
    },
    {
      "departementId": "DEP-DGA",
      "roleId": null,
      "ordre": 4,
      "obligatoire": true,
      "actif": true
    },
    {
      "departementId": "DEP-DG",
      "roleId": null,
      "ordre": 5,
      "obligatoire": true,
      "actif": true
    }
  ]
}
```

### Reponse (exemple)

```json
{
  "id": "circuit-uuid",
  "code": "CIR-2026-001",
  "libelle": "Circuit ouverture compte standard",
  "description": "Circuit de validation standard pour ouverture de comptes",
  "actif": true,
  "etapes": [
    {
      "id": "etape-1",
      "ordre": 1,
      "obligatoire": true,
      "actif": true,
      "departementId": "DEP-INIT",
      "departementLibelle": "DEPARTEMENT DEMANDEUR",
      "roleId": "ROLE-CHEF-SERVICE",
      "roleLibelle": "CHEF_SERVICE"
    }
  ]
}
```

## 5. Modifier un circuit

### Requete
`PUT /api/circuits/{id}`

Le payload est le meme que la creation.
La configuration des etapes est remplacee par la nouvelle liste envoyee.

## 6. Associer un circuit a une plateforme

Endpoint plateforme existant:
- `POST /api/habilitations/plateformes`
- `PUT /api/habilitations/plateformes/{id}`

Le DTO plateforme supporte maintenant:
- `idCircuit`

### Exemple creation plateforme avec circuit

`POST /api/habilitations/plateformes`

```json
{
  "code": "WINDOWS",
  "nom": "Compte Windows",
  "description": "Gestion des acces Windows",
  "idCircuit": "circuit-uuid"
}
```

### Exemple mise a jour plateforme

`PUT /api/habilitations/plateformes/{id}`

```json
{
  "code": "AMPLITUDE",
  "nom": "Compte Amplitude",
  "description": "Gestion des acces Amplitude",
  "idCircuit": "circuit-uuid"
}
```

## 7. Utiliser le circuit dans le workflow fiche

Le workflow d'habilitation utilise ensuite le circuit de la plateforme pour creer les etapes de validation de la fiche.

Flux type:
1. creer/mettre a jour la plateforme avec `idCircuit`
2. creer la fiche d'habilitation
3. initialiser les etapes de fiche a partir du circuit
4. valider/rejeter etape par etape

Endpoints utiles:
- `POST /api/habilitations/fiches`
- `POST /api/habilitations/fiches/{id}/etapes-from-circuit`
- `PUT /api/habilitations/fiches-etapes/{id}/status`

## 8. Regle role-validateur en execution

Si une etape du circuit a `roleId` (roleValidateur):
- seuls les utilisateurs ayant ce role voient la demande dans `mes validations`
- seuls ces utilisateurs peuvent valider/rejeter l'etape

Sinon, le systeme applique le fallback existant (regles de l'etape de definition).

## 9. Erreurs metier frequentes

- `Deux etapes du circuit ne peuvent pas avoir le meme ordre.`
- `Un departement ne peut apparaitre qu'une seule fois dans un meme circuit.`
- `Le circuit doit obligatoirement contenir les etapes DGA et DG.`
- `Departement introuvable: ...`
- `Role introuvable: ...`

## 10. Notes techniques

- Les identifiants du projet sont en `String` (UUID/metier), pas en `Long`.
- `DELETE /api/circuits/{id}` fait une desactivation logique.
- Les etapes sont toujours retournees triees par `ordre`.

## 11. Guide Postman (pret a l'emploi)

Collection fournie:
- [postman-circuit-workflow.collection.json](/C:/eclipse-workspace/bsic-service/postman-circuit-workflow.collection.json)

### Import
1. Ouvrir Postman
2. `Import` -> selectionner `postman-circuit-workflow.collection.json`
3. Renseigner les variables de collection:
- `base_url`
- `employe_id`
- `departement_initiateur_id`, `departement_rh_id`, `departement_risque_id`, `departement_dga_id`, `departement_dg_id`
- `role_initiateur_id`, `role_rh_id`, `role_risque_id`

### Ordre recommande d'execution
1. `1 - Creer Circuit`
2. `2 - Lister Circuits`
3. `3 - Detail Circuit`
4. `5 - Creer Plateforme avec Circuit`
5. `6 - Creer Fiche`
6. `7 - Initialiser Etapes depuis Circuit`
7. `8 - Mes validations`
8. `9 - Valider une etape` (ou `10 - Rejeter une etape`)

Les requetes `1`, `5`, `6` enregistrent automatiquement `circuit_id`, `plateforme_id`, `fiche_id` dans les variables de collection.

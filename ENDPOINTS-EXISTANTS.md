# 📋 Endpoints Existants - BSIC Service

## 🏗️ Architecture Actuelle

### Modèles de Données Existant

#### BaCircuit (Entité)
```java
@Entity
public class BaCircuit extends BaAbstractAuditingEntity {
    private String id;
    private String code;           // unique
    private String libelle;         // length = 150
    private String description;      // length = 2000
    private BaPlateforme plateforme;  // @ManyToOne
}
```

#### BaCircuitEtapeConfig (Entité)
```java
@Entity
@Table(name = "ba_circuit_etape_config")
public class BaCircuitEtapeConfig extends BaAbstractAuditingEntity {
    private String id;
    private BaCircuit circuit;                    // @ManyToOne
    private BaEtapeDefinition etapeDefinition;      // @ManyToOne
    private Integer ordre;
}
```

#### BaCircuitDto
```java
public class BaCircuitDto {
    private String id;
    private String code;
    private String libelle;
    private String description;
    private String idPlateforme;
    private String nomPlateforme;
    private List<BaCircuitEtapeConfigDto> etapes;
}
```

#### BaCircuitEtapeConfigDto
```java
public class BaCircuitEtapeConfigDto {
    private String id;
    private String idCircuit;
    private String idEtapeDefinition;
    private String libelleEtape;
    private Integer ordre;
}
```

## 🚀 Endpoints Actuels (Implémentés)

### Circuits de Validation

#### GET /api/habilitations/circuits
- **Description**: Lister tous les circuits ou filtrer par plateforme
- **Paramètres**: 
  - `plateformeId` (optionnel): Filtrer par plateforme
- **Retour**: `List<BaCircuitDto>`

#### POST /api/habilitations/circuits
- **Description**: Créer un nouveau circuit
- **Body**: `BaCircuitDto`
- **Retour**: `ResponseEntity<BaCircuitDto>` (201 Created)

#### PUT /api/habilitations/circuits/{id}
- **Description**: Mettre à jour un circuit
- **Paramètres**: `id` (path)
- **Body**: `BaCircuitDto`
- **Retour**: `ResponseEntity<BaCircuitDto>`

#### GET /api/habilitations/circuits/{id}/etapes
- **Description**: Lister les étapes de configuration d'un circuit
- **Paramètres**: `id` (path)
- **Retour**: `List<BaCircuitEtapeConfigDto>`

#### PUT /api/habilitations/circuits/{id}/etapes
- **Description**: Mettre à jour les étapes de configuration d'un circuit
- **Paramètres**: `id` (path)
- **Body**: `List<BaCircuitEtapeConfigDto>`
- **Retour**: `List<BaCircuitEtapeConfigDto>`

### Plateformes (Référence)

#### GET /api/habilitations/plateformes
- **Description**: Lister toutes les plateformes
- **Retour**: `List<BaPlateformeDto>`

#### POST /api/habilitations/plateformes
- **Description**: Créer une plateforme
- **Body**: `BaPlateformeDto`
- **Retour**: `ResponseEntity<BaPlateformeDto>` (201 Created)

#### PUT /api/habilitations/plateformes/{id}
- **Description**: Mettre à jour une plateforme
- **Paramètres**: `id` (path)
- **Body**: `BaPlateformeDto`
- **Retour**: `ResponseEntity<BaPlateformeDto>`

### Étapes de Définition (Référence)

#### GET /api/habilitations/etapes
- **Description**: Lister toutes les étapes de définition
- **Paramètres**:
  - `type` (optionnel): Filtrer par type (DEPARTEMENT/SERVICE)
  - `departementId` (optionnel): Filtrer par département
  - `serviceId` (optionnel): Filtrer par service
- **Retour**: `List<BaEtapeDefinitionDto>`

#### POST /api/habilitations/etapes
- **Description**: Créer une étape de définition
- **Body**: `BaEtapeDefinitionDto`
- **Retour**: `ResponseEntity<BaEtapeDefinitionDto>` (201 Created)

#### PUT /api/habilitations/etapes/{id}
- **Description**: Mettre à jour une étape de définition
- **Paramètres**: `id` (path)
- **Body**: `BaEtapeDefinitionDto`
- **Retour**: `ResponseEntity<BaEtapeDefinitionDto>`

### Fiches d'Habilitation (Workflow)

#### GET /api/habilitations/fiches
- **Description**: Lister toutes les fiches d'habilitation
- **Retour**: `List<BaFicheHabilitationDto>`

#### GET /api/habilitations/fiches/{id}
- **Description**: Obtenir une fiche par ID
- **Paramètres**: `id` (path)
- **Retour**: `ResponseEntity<BaFicheHabilitationDto>`

#### POST /api/habilitations/fiches
- **Description**: Créer une nouvelle fiche d'habilitation
- **Body**: `BaFicheHabilitationDto`
- **Retour**: `ResponseEntity<BaFicheHabilitationDto>` (201 Created)

#### POST /api/habilitations/fiches/{id}/etapes-from-circuit
- **Description**: Initialiser les étapes d'une fiche à partir du circuit de sa plateforme
- **Paramètres**: `id` (path)
- **Retour**: `List<BaFicheHabilitationEtapeDto>`

## 🔄 Workflow Complet Actuel

1. **Configuration**:
   - Créer les plateformes (`/plateformes`)
   - Définir les étapes (`/etapes`)
   - Créer les circuits (`/circuits`)
   - Configurer les étapes des circuits (`/circuits/{id}/etapes`)

2. **Utilisation**:
   - Créer une fiche d'habilitation (`/fiches`)
   - Initialiser les étapes depuis le circuit (`/fiches/{id}/etapes-from-circuit`)
   - Suivre le workflow de validation

## 📊 Analyse de l'Existant

### ✅ Points Forts
- **Architecture complète**: Modèles, DTOs, endpoints implémentés
- **Relation claire**: Circuit ↔ Plateforme ↔ Étapes
- **Workflow intégré**: Initialisation automatique des fiches
- **Configuration flexible**: Ordre personnalisable des étapes

### 🔍 Points à Améliorer
1. **Gestion d'état des circuits** (actif/inactif)
2. **Templates prédéfinis** de circuits
3. **Validation des configurations** avant activation
4. **Export/Import** des configurations
5. **Historique** des modifications
6. **Tests** des circuits

## 🎯 Recommandations

### 1. Ajouter un statut aux circuits
```java
// Dans BaCircuit
@Column(name = "statut")
@Enumerated(EnumType.STRING)
private ECircuitStatut statut; // ACTIF, INACTIF, EN_CONFIGURATION
```

### 2. Créer des templates
```java
@Entity
public class BaCircuitTemplate extends BaAbstractAuditingEntity {
    private String id;
    private String nom;
    private String description;
    private String complexite; // SIMPLE, MOYENNE, COMPLEXE
    private Integer dureeEstimee; // en minutes
    private String configurationJson; // configuration sérialisée
}
```

### 3. Ajouter des endpoints de workflow
- `POST /api/habilitations/circuits/{id}/tester`
- `POST /api/habilitations/circuits/{id}/activer`
- `POST /api/habilitations/circuits/{id}/dupliquer`
- `GET /api/habilitations/circuits/templates`
- `POST /api/habilitations/circuits/from-template`

---

## 📝 Conclusion

Le backend BSIC dispose déjà d'une **base solide** pour la gestion des circuits avec :
- ✅ Modèles de données complets
- ✅ CRUD de base implémenté
- ✅ Intégration avec le workflow d'habilitation
- ✅ Configuration flexible des étapes

Les endpoints existants sont **fonctionnels** et couvrent les besoins essentiels. Les améliorations proposées visent à enrichir l'expérience utilisateur avec des fonctionnalités avancées de workflow et de gestion.

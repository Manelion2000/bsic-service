# 🚀 Nouveaux Endpoints BSIC - Circuits Avancés

## 📋 Résumé des Améliorations

### ✅ Modèles Ajoutés
1. **ECircuitStatut** - Gestion d'état des circuits
2. **ECircuitComplexite** - Niveaux de complexité  
3. **BaCircuitTemplate** - Templates prédéfinis
4. **BaCircuitTemplateDto** - DTO pour les templates

### ✅ Endpoints Ajoutés
- Gestion d'état des circuits (activer/désactiver)
- Duplication de circuits
- Test de configuration
- Templates de circuits
- Création depuis template

---

## 🔧 Endpoints Ajoutés

### 1. Gestion d'État des Circuits

#### POST /api/habilitations/circuits/{id}/activer
- **Description**: Activer un circuit
- **Paramètres**: `id` (path)
- **Retour**: `ResponseEntity<BaCircuitDto>`
- **Usage**: Permet de mettre un circuit en production

#### POST /api/habilitations/circuits/{id}/desactiver
- **Description**: Désactiver un circuit
- **Paramètres**: `id` (path)
- **Retour**: `ResponseEntity<BaCircuitDto>`
- **Usage**: Permet de désactiver temporairement un circuit

### 2. Duplication de Circuits

#### POST /api/habilitations/circuits/{id}/dupliquer
- **Description**: Dupliquer un circuit existant
- **Paramètres**: 
  - `id` (path): ID du circuit à dupliquer
  - `nouveauNom` (query): Nom du nouveau circuit
- **Retour**: `ResponseEntity<BaCircuitDto>` (201 Created)
- **Usage**: Créer rapidement un circuit similaire

### 3. Test de Configuration

#### POST /api/habilitations/circuits/{id}/tester
- **Description**: Tester la validité d'un circuit
- **Paramètres**: `id` (path)
- **Retour**: `ResponseEntity<String>`
  - `200 OK`: "Le circuit est valide et fonctionnel"
  - `400 Bad Request`: "Le circuit présente des erreurs de configuration"
- **Usage**: Valider un circuit avant activation

### 4. Templates de Circuits

#### GET /api/habilitations/circuits/templates
- **Description**: Lister tous les templates de circuits
- **Retour**: `List<BaCircuitTemplateDto>`
- **Usage**: Obtenir les templates disponibles pour création rapide

#### GET /api/habilitations/circuits/templates/{id}
- **Description**: Obtenir un template spécifique
- **Paramètres**: `id` (path)
- **Retour**: `ResponseEntity<BaCircuitTemplateDto>`
- **Usage**: Détails d'un template avant utilisation

#### POST /api/habilitations/circuits/from-template
- **Description**: Créer un circuit à partir d'un template
- **Paramètres**:
  - `templateId` (query): ID du template
  - `nomCircuit` (query): Nom du nouveau circuit
  - `plateformeId` (query): ID de la plateforme cible
- **Retour**: `ResponseEntity<BaCircuitDto>` (201 Created)
- **Usage**: Création rapide de circuit avec configuration prédéfinie

---

## 🏗️ Modèles de Données

### ECircuitStatut
```java
public enum ECircuitStatut {
    EN_CONFIGURATION,  // Circuit en cours de configuration
    ACTIF,           // Circuit actif et utilisable
    INACTIF          // Circuit désactivé
}
```

### ECircuitComplexite
```java
public enum ECircuitComplexite {
    SIMPLE,    // Circuit simple (1-2 étapes)
    MOYENNE,   // Circuit standard (3-5 étapes)
    COMPLEXE    // Circuit complexe (6+ étapes)
}
```

### BaCircuitTemplate
```java
@Entity
public class BaCircuitTemplate extends BaAbstractAuditingEntity {
    private String id;
    private String nom;                    // Nom du template
    private String description;             // Description détaillée
    private ECircuitComplexite complexite;   // Niveau de complexité
    private Integer dureeEstimee;         // Durée en minutes
    private Integer popularite;            // Pourcentage d'utilisation
    private String configurationJson;        // Configuration sérialisée
    private String tags;                   // Tags séparés par virgules
}
```

### BaCircuitTemplateDto
```java
public class BaCircuitTemplateDto {
    private String id;
    private String nom;
    private String description;
    private ECircuitComplexite complexite;
    private Integer dureeEstimee;
    private Integer popularite;
    private List<String> tags;
    private List<BaCircuitEtapeConfigDto> etapes;
}
```

---

## 🔄 Workflow Complet Amélioré

### 1. **Phase de Configuration**
```
1. Créer/Choisir un template
   GET /circuits/templates
   POST /circuits/from-template

2. Configurer les étapes
   PUT /circuits/{id}/etapes

3. Tester la configuration
   POST /circuits/{id}/tester

4. Activer le circuit
   POST /circuits/{id}/activer
```

### 2. **Phase d'Utilisation**
```
1. Créer une fiche d'habilitation
   POST /fiches

2. Initialiser depuis le circuit
   POST /fiches/{id}/etapes-from-circuit

3. Suivre le workflow
   PUT /fiches-etapes/{id}/status
```

### 3. **Phase de Maintenance**
```
1. Dupliquer pour modification
   POST /circuits/{id}/dupliquer

2. Désactiver l'ancien
   POST /circuits/{id}/desactiver

3. Activer le nouveau
   POST /circuits/{id}/activer
```

---

## 📊 Exemples d'Utilisation

### Création depuis Template
```bash
# 1. Lister les templates
curl -X GET "http://localhost:8080/api/habilitations/circuits/templates"

# 2. Créer depuis template
curl -X POST "http://localhost:8080/api/habilitations/circuits/from-template" \
  -d "templateId=template-standard&nomCircuit=Circuit Windows&plateformeId=plateforme-1"
```

### Test et Activation
```bash
# 1. Tester le circuit
curl -X POST "http://localhost:8080/api/habilitations/circuits/circuit-1/tester"

# 2. Activer le circuit
curl -X POST "http://localhost:8080/api/habilitations/circuits/circuit-1/activer"
```

### Duplication
```bash
# Dupliquer un circuit existant
curl -X POST "http://localhost:8080/api/habilitations/circuits/circuit-1/dupliquer" \
  -d "nouveauNom=Circuit Copie"
```

---

## 🎯 Avantages des Nouvelles Fonctionnalités

### 1. **Workflow Guidé**
- Templates prédéfinis pour création rapide
- Processus étape par étape validé
- Réduction des erreurs de configuration

### 2. **Gestion d'État**
- Contrôle précis de l'activation des circuits
- Possibilité de désactiver temporairement
- Historique des changements d'état

### 3. **Test et Validation**
- Validation automatique des configurations
- Détection des erreurs avant mise en production
- Feedback immédiat sur la validité

### 4. **Productivité**
- Duplication rapide de circuits similaires
- Templates réutilisables
- Gain de temps dans la configuration

---

## 📝 Conclusion

Les nouveaux endpoints transforment la gestion des circuits en un **workflow complet et professionnel** avec :

- ✅ **Création guidée** via templates
- ✅ **Validation automatique** des configurations  
- ✅ **Gestion d'état** précise
- ✅ **Productivité améliorée** avec duplication
- ✅ **Sécurité renforcée** avec tests

Le système BSIC dispose maintenant d'une **solution complète** pour la gestion des circuits de validation d'habilitation !

-- =====================================================
-- Initialisation des Templates de Circuits BSIC
-- =====================================================

-- Insertion des templates de circuits prédéfinis
INSERT INTO ba_circuit_template (id, nom, description, complexite, duree_estimee, popularite, configuration_json, tags, created_by, created_date, updated_by, updated_date)
VALUES 
-- Template Standard
(
    'tpl-standard', 
    'Circuit Standard', 
    'Workflow complet pour une mise en place standard avec validation hiérarchique complète', 
    'MOYENNE', 
    150, 
    85, 
    '[
        {
            "ordre": 1,
            "libelleEtape": "Validation par le Département Informatique et Technique",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 2
        },
        {
            "ordre": 2,
            "libelleEtape": "Validation par la Direction des Ressources Humaines",
            "typeEtape": "DEPARTEMENT", 
            "delaiValidation": 3
        },
        {
            "ordre": 3,
            "libelleEtape": "Validation par la Direction Générale Adjointe",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 2
        },
        {
            "ordre": 4,
            "libelleEtape": "Validation par la Direction Générale",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 1
        }
    ]', 
    'Standard,Hierarchique,Validation,DIT,DRH,DGA,DG', 
    'system', 
    CURRENT_TIMESTAMP, 
    'system', 
    CURRENT_TIMESTAMP
),

-- Template Simple
(
    'tpl-simple', 
    'Circuit Simple', 
    'Workflow rapide pour les besoins simples avec validation minimale à deux niveaux', 
    'SIMPLE', 
    75, 
    70, 
    '[
        {
            "ordre": 1,
            "libelleEtape": "Validation par le Manager direct",
            "typeEtape": "SERVICE",
            "delaiValidation": 1
        },
        {
            "ordre": 2,
            "libelleEtape": "Validation par la Direction des Ressources Humaines",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 2
        }
    ]', 
    'Simple,Rapide,Manager,DRH', 
    'system', 
    CURRENT_TIMESTAMP, 
    'system', 
    CURRENT_TIMESTAMP
),

-- Template Complet
(
    'tpl-complet', 
    'Circuit Complet', 
    'Workflow détaillé pour les organisations complexes avec validations multi-niveaux et contrôles renforcés', 
    'COMPLEXE', 
    240, 
    60, 
    '[
        {
            "ordre": 1,
            "libelleEtape": "Auto-validation par l''employé",
            "typeEtape": "SERVICE",
            "delaiValidation": 1
        },
        {
            "ordre": 2,
            "libelleEtape": "Validation par le Manager direct",
            "typeEtape": "SERVICE",
            "delaiValidation": 1
        },
        {
            "ordre": 3,
            "libelleEtape": "Validation par le Département Informatique et Technique",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 2
        },
        {
            "ordre": 4,
            "libelleEtape": "Validation par la Direction des Ressources Humaines",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 3
        },
        {
            "ordre": 5,
            "libelleEtape": "Validation par la Direction Générale Adjointe",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 2
        },
        {
            "ordre": 6,
            "libelleEtape": "Validation finale par la Direction Générale",
            "typeEtape": "DEPARTEMENT",
            "delaiValidation": 1
        }
    ]', 
    'Complet,Multi-niveaux,Entreprise,Auto-validation,Manager,DIT,DRH,DGA,DG', 
    'system', 
    CURRENT_TIMESTAMP, 
    'system', 
    CURRENT_TIMESTAMP
);

-- =====================================================
-- Insertion des étapes de définition correspondantes
-- =====================================================

-- Étapes pour template Standard
INSERT INTO ba_etape_definition (id, type, libelle, fonction_requise, created_by, created_date, updated_by, updated_date)
VALUES 
('etape-dit', 'DEPARTEMENT', 'Département Informatique et Technique', 'Chef de département DIT', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('etape-drh', 'DEPARTEMENT', 'Direction des Ressources Humaines', 'Directeur des RH', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('etape-dga', 'DEPARTEMENT', 'Direction Générale Adjointe', 'Directeur Général Adjoint', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('etape-dg', 'DEPARTEMENT', 'Direction Générale', 'Directeur Général', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('etape-manager', 'SERVICE', 'Manager Direct', 'Manager de service', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('etape-auto-validation', 'SERVICE', 'Auto-validation Employé', 'Employé demandeur', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

-- =====================================================
-- Création d''un circuit d''exemple pour Windows
-- =====================================================

-- Récupérer ou créer une plateforme Windows (supposons qu'elle existe)
-- INSERT INTO ba_plateforme (id, code, nom, created_by, created_date, updated_by, updated_date)
-- VALUES ('plt-windows', 'WINDOWS', 'Système Windows', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

-- Créer un circuit d'exemple
INSERT INTO ba_circuit (id, code, libelle, description, statut, plateforme_id, created_by, created_date, updated_by, updated_date)
VALUES 
('circuit-windows-standard', 'CIRCUIT_WINDOWS_STANDARD', 'Circuit Standard Windows', 'Circuit de validation standard pour les accès aux systèmes Windows', 'ACTIF', 'plt-windows', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

-- Configurer les étapes du circuit Windows
INSERT INTO ba_circuit_etape_config (id, circuit_id, etape_definition_id, ordre, created_by, created_date, updated_by, updated_date)
VALUES 
('config-windows-1', 'circuit-windows-standard', 'etape-dit', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('config-windows-2', 'circuit-windows-standard', 'etape-drh', 2, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('config-windows-3', 'circuit-windows-standard', 'etape-dga', 3, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
('config-windows-4', 'circuit-windows-standard', 'etape-dg', 4, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

-- =====================================================
-- Index pour optimisation
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_circuit_template_complexite ON ba_circuit_template(complexite);
CREATE INDEX IF NOT EXISTS idx_circuit_template_popularite ON ba_circuit_template(popularite DESC);
CREATE INDEX IF NOT EXISTS idx_circuit_statut ON ba_circuit(statut);
CREATE INDEX IF NOT EXISTS idx_circuit_plateforme ON ba_circuit(plateforme_id);
CREATE INDEX IF NOT EXISTS idx_circuit_etape_config_circuit ON ba_circuit_etape_config(circuit_id);
CREATE INDEX IF NOT EXISTS idx_circuit_etape_config_ordre ON ba_circuit_etape_config(ordre);

-- =====================================================
-- Commentaires
-- =====================================================

COMMENT ON TABLE ba_circuit_template IS 'Templates prédéfinis pour la création rapide de circuits de validation';
COMMENT ON TABLE ba_circuit IS 'Circuits de validation d''habilitation avec gestion d''état';
COMMENT ON COLUMN ba_circuit.statut IS 'État du circuit: EN_CONFIGURATION, ACTIF, INACTIF';
COMMENT ON COLUMN ba_circuit_template.complexite IS 'Complexité du template: SIMPLE, MOYENNE, COMPLEXE';
COMMENT ON COLUMN ba_circuit_template.duree_estimee IS 'Durée estimée en minutes';
COMMENT ON COLUMN ba_circuit_template.popularite IS 'Pourcentage d''utilisation du template';
COMMENT ON COLUMN ba_circuit_template.configuration_json IS 'Configuration JSON sérialisée du template';

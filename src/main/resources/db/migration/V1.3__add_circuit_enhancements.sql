-- =====================================================
-- Migration V1.3: Améliorations des Circuits BSIC
-- =====================================================

-- Ajout de la colonne statut à la table ba_circuit
ALTER TABLE ba_circuit 
ADD COLUMN IF NOT EXISTS statut VARCHAR(20) DEFAULT 'EN_CONFIGURATION';

-- Création de la table ba_circuit_template
CREATE TABLE IF NOT EXISTS ba_circuit_template (
    id VARCHAR(36) PRIMARY KEY,
    nom VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    complexite VARCHAR(20) NOT NULL,
    duree_estimee INT,
    popularite INT DEFAULT 0,
    configuration_json TEXT,
    tags VARCHAR(200),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Mise à jour des circuits existants pour leur donner un statut par défaut
UPDATE ba_circuit 
SET statut = 'ACTIF' 
WHERE statut IS NULL OR statut = '';

-- Création des index pour optimisation
CREATE INDEX IF NOT EXISTS idx_circuit_statut ON ba_circuit(statut);
CREATE INDEX IF NOT EXISTS idx_circuit_template_complexite ON ba_circuit_template(complexite);
CREATE INDEX IF NOT EXISTS idx_circuit_template_popularite ON ba_circuit_template(popularite DESC);

-- Insertion des contraintes de validation
ALTER TABLE ba_circuit 
ADD CONSTRAINT IF NOT EXISTS chk_circuit_statut 
CHECK (statut IN ('EN_CONFIGURATION', 'ACTIF', 'INACTIF'));

ALTER TABLE ba_circuit_template 
ADD CONSTRAINT IF NOT EXISTS chk_circuit_template_complexite 
CHECK (complexite IN ('SIMPLE', 'MOYENNE', 'COMPLEXE'));

-- =====================================================
-- Commentaires sur les nouvelles colonnes
-- =====================================================

COMMENT ON COLUMN ba_circuit.statut IS 'État du circuit: EN_CONFIGURATION, ACTIF, INACTIF';
COMMENT ON TABLE ba_circuit_template IS 'Templates prédéfinis pour la création rapide de circuits de validation';
COMMENT ON COLUMN ba_circuit_template.complexite IS 'Complexité du template: SIMPLE, MOYENNE, COMPLEXE';
COMMENT ON COLUMN ba_circuit_template.duree_estimee IS 'Durée estimée en minutes';
COMMENT ON COLUMN ba_circuit_template.popularite IS 'Pourcentage d''utilisation du template';
COMMENT ON COLUMN ba_circuit_template.configuration_json IS 'Configuration JSON sérialisée du template';
COMMENT ON COLUMN ba_circuit_template.tags IS 'Tags séparés par des virgules';

-- =====================================================
-- Données de test (optionnel - à décommenter pour environnement de dév)
-- =====================================================

-- Insertion des templates prédéfinis (voir data-init.sql pour le contenu complet)
-- Les données seront insérées via le script d'initialisation séparé

-- =====================================================
-- Validation de la migration
-- =====================================================

-- Vérification que la table ba_circuit_template a été créée
SELECT COUNT(*) as circuit_template_table_created 
FROM information_schema.tables 
WHERE table_schema = DATABASE() AND table_name = 'ba_circuit_template';

-- Vérification que la colonne statut a été ajoutée
SELECT COUNT(*) as circuit_statut_column_added 
FROM information_schema.columns 
WHERE table_schema = DATABASE() 
  AND table_name = 'ba_circuit' 
  AND column_name = 'statut';

-- =====================================================
-- Fin de la migration V1.3
-- =====================================================

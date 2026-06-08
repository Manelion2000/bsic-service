-- Circuit module refactor: etapes basees sur departement/role + contraintes d'unicite

ALTER TABLE ba_circuit
ADD COLUMN IF NOT EXISTS actif BOOLEAN DEFAULT TRUE;

ALTER TABLE ba_circuit_etape_config
ADD COLUMN IF NOT EXISTS departement_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS role_validateur_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS obligatoire BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS actif BOOLEAN DEFAULT TRUE;

ALTER TABLE ba_plateforme
ADD COLUMN IF NOT EXISTS circuit_id VARCHAR(255);

ALTER TABLE ba_circuit_etape_config
ADD CONSTRAINT IF NOT EXISTS fk_circuit_etape_departement
FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE ba_circuit_etape_config
ADD CONSTRAINT IF NOT EXISTS fk_circuit_etape_role_validateur
FOREIGN KEY (role_validateur_id) REFERENCES ba_role(id);

ALTER TABLE ba_plateforme
ADD CONSTRAINT IF NOT EXISTS fk_plateforme_circuit
FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_circuit_etape_ordre ON ba_circuit_etape_config(circuit_id, ordre);
CREATE UNIQUE INDEX IF NOT EXISTS uk_circuit_etape_departement ON ba_circuit_etape_config(circuit_id, departement_id);
CREATE INDEX IF NOT EXISTS idx_plateforme_circuit_id ON ba_plateforme(circuit_id);
CREATE INDEX IF NOT EXISTS idx_circuit_actif ON ba_circuit(actif);

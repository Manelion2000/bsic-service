ALTER TABLE ba_agence
ADD COLUMN IF NOT EXISTS departement_id VARCHAR(255);

ALTER TABLE ba_agence
ADD CONSTRAINT IF NOT EXISTS fk_agence_departement
FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

CREATE INDEX IF NOT EXISTS idx_agence_departement
ON ba_agence(departement_id);

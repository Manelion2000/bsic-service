ALTER TABLE ba_autorisation_sortie
ADD COLUMN IF NOT EXISTS type_structure VARCHAR(20),
ADD COLUMN IF NOT EXISTS direction_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS service_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS agence_id VARCHAR(255);

ALTER TABLE ba_autorisation_sortie
ADD CONSTRAINT IF NOT EXISTS fk_aut_sortie_direction
FOREIGN KEY (direction_id) REFERENCES ba_departement(id);

ALTER TABLE ba_autorisation_sortie
ADD CONSTRAINT IF NOT EXISTS fk_aut_sortie_service
FOREIGN KEY (service_id) REFERENCES ba_service(id);

ALTER TABLE ba_autorisation_sortie
ADD CONSTRAINT IF NOT EXISTS fk_aut_sortie_agence
FOREIGN KEY (agence_id) REFERENCES ba_agence(id);

CREATE INDEX IF NOT EXISTS idx_aut_sortie_type_structure ON ba_autorisation_sortie(type_structure);
CREATE INDEX IF NOT EXISTS idx_aut_sortie_direction ON ba_autorisation_sortie(direction_id);
CREATE INDEX IF NOT EXISTS idx_aut_sortie_service ON ba_autorisation_sortie(service_id);
CREATE INDEX IF NOT EXISTS idx_aut_sortie_agence ON ba_autorisation_sortie(agence_id);

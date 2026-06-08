-- Module autorisation de sortie

ALTER TABLE ba_autorisation_sortie
ADD COLUMN IF NOT EXISTS demandeur_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS validateur_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS statut_validation VARCHAR(20) DEFAULT 'EN_ATTENTE',
ADD COLUMN IF NOT EXISTS commentaire_validation VARCHAR(1500),
ADD COLUMN IF NOT EXISTS date_validation TIMESTAMP,
ADD COLUMN IF NOT EXISTS date_sortie DATE;

ALTER TABLE ba_autorisation_sortie
ADD CONSTRAINT IF NOT EXISTS fk_aut_sortie_demandeur
FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_autorisation_sortie
ADD CONSTRAINT IF NOT EXISTS fk_aut_sortie_validateur
FOREIGN KEY (validateur_id) REFERENCES ba_utilisateur(id);

CREATE INDEX IF NOT EXISTS idx_aut_sortie_demandeur ON ba_autorisation_sortie(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_aut_sortie_validateur ON ba_autorisation_sortie(validateur_id);
CREATE INDEX IF NOT EXISTS idx_aut_sortie_statut_validation ON ba_autorisation_sortie(statut_validation);

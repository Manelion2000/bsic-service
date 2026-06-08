-- Ajouter un second superieur direct pour la gestion hierarchique

ALTER TABLE ba_utilisateur
ADD COLUMN IF NOT EXISTS superieur_secondaire_id VARCHAR(255);

ALTER TABLE ba_utilisateur
ADD CONSTRAINT IF NOT EXISTS fk_user_superieur_secondaire
FOREIGN KEY (superieur_secondaire_id) REFERENCES ba_utilisateur(id);

CREATE INDEX IF NOT EXISTS idx_user_superieur_secondaire
ON ba_utilisateur(superieur_secondaire_id);

-- Les anciennes valeurs restent en base ; les nouvelles fiches n'exigent ni code ni plateforme.
ALTER TABLE ba_requete_oracle ALTER COLUMN code DROP NOT NULL;
ALTER TABLE ba_requete_oracle ALTER COLUMN plateforme_id DROP NOT NULL;
DROP INDEX idx_requete_oracle_code;
ALTER TABLE ba_requete_oracle ADD COLUMN date_validation date;
ALTER TABLE ba_requete_oracle ADD COLUMN auteur_id varchar(255);
ALTER TABLE ba_requete_oracle ADD COLUMN auteur varchar(255);
-- Reprendre uniquement l'auteur effectivement connu de l'édition courante.
UPDATE ba_requete_oracle q SET auteur_id=r.auteur_id,auteur=r.auteur
FROM ba_requete_revision r WHERE r.requete_id=q.id AND r.edition=q.edition;
-- Aucune date de validation ou exécution n'est inventée pour les fiches historiques.

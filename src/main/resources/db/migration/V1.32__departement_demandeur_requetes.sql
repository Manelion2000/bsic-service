ALTER TABLE ba_requete_oracle ADD COLUMN departement_demandeur_id varchar(255) REFERENCES ba_departement(id);
ALTER TABLE ba_requete_oracle ADD COLUMN departement_nom varchar(255);
CREATE INDEX idx_requete_oracle_departement ON ba_requete_oracle(departement_demandeur_id,statut,verifiee,titre,id);
-- Les anciennes requêtes restent sans département connu : aucun rattachement n'est supposé.

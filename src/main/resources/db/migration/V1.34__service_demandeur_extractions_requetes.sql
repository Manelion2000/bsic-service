-- Le service est facultatif : certains besoins concernent tout un département.
-- Ne pas déduire le service historique du rattachement actuel des agents.
ALTER TABLE ba_requete_oracle
    ADD COLUMN service_demandeur_id varchar(255) REFERENCES ba_service(id),
    ADD COLUMN service_nom varchar(255);

ALTER TABLE ba_extraction_demande
    ADD COLUMN service_id varchar(255) REFERENCES ba_service(id),
    ADD COLUMN service_nom varchar(255);

CREATE INDEX idx_requete_oracle_service ON ba_requete_oracle(service_demandeur_id);
CREATE INDEX idx_extraction_demande_service ON ba_extraction_demande(service_id);

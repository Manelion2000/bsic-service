-- Enrichir le référentiel existant : les identifiants et habilitations restent inchangés.
ALTER TABLE ba_plateforme ADD COLUMN fiche jsonb NOT NULL DEFAULT '{}';
ALTER TABLE ba_plateforme ADD COLUMN departement_proprietaire_id varchar(255) REFERENCES ba_departement(id);
ALTER TABLE ba_plateforme ADD COLUMN responsable_metier_id varchar(255) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_plateforme ADD COLUMN responsable_technique_id varchar(255) REFERENCES ba_utilisateur(id);
CREATE TABLE ba_plateforme_dependance (
    plateforme_id varchar(255) NOT NULL REFERENCES ba_plateforme(id),
    dependance_id varchar(255) NOT NULL REFERENCES ba_plateforme(id),
    PRIMARY KEY (plateforme_id,dependance_id),
    CHECK (plateforme_id <> dependance_id)
);
CREATE INDEX idx_plateforme_dependance_inverse ON ba_plateforme_dependance(dependance_id);
CREATE TABLE ba_plateforme_revision (
    id varchar(255) PRIMARY KEY,
    plateforme_id varchar(255) NOT NULL REFERENCES ba_plateforme(id),
    numero bigint NOT NULL,
    date timestamp(6) with time zone NOT NULL,
    auteur_id varchar(255) NOT NULL,
    auteur varchar(255) NOT NULL,
    action varchar(30) NOT NULL,
    motif varchar(1000) NOT NULL,
    snapshot jsonb NOT NULL,
    UNIQUE (plateforme_id,numero)
);
CREATE INDEX idx_plateforme_catalogue_statut_nom ON ba_plateforme(statut,nom,id);
INSERT INTO ba_role(id,code,libelle,created_by,created_date,statut,version) VALUES
 ('role-plateformes-consulter','PLATEFORMES_CONSULTER','Consulter le catalogue informatique','migration',now(),'A',1),
 ('role-plateformes-gerer','PLATEFORMES_GERER','Gérer le catalogue informatique','migration',now(),'A',1)
ON CONFLICT (code) DO NOTHING;
-- Pas de responsable, criticité ou cycle de vie supposé pour les fiches existantes.

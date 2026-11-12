
-- Bibliotheque commune, publications immuables, catalogue et dossiers. Aucun circuit active automatiquement.
ALTER TABLE ba_circuit ADD COLUMN lock_version bigint NOT NULL DEFAULT 0;
ALTER TABLE ba_circuit ADD COLUMN autonome boolean NOT NULL DEFAULT false;
ALTER TABLE ba_circuit ADD COLUMN definition jsonb;
CREATE TABLE ba_circuit_publication (id varchar(255) PRIMARY KEY,lock_version bigint NOT NULL DEFAULT 0,version bigint DEFAULT 1,statut varchar(255) DEFAULT 'A',created_by varchar(50) NOT NULL,created_date timestamptz NOT NULL,last_modified_by varchar(50),last_modified_date timestamptz,circuit_id varchar(255) NOT NULL REFERENCES ba_circuit(id),numero integer NOT NULL CHECK(numero>0),libelle varchar(150) NOT NULL,description varchar(2000),justification varchar(1500) NOT NULL,definition jsonb NOT NULL,UNIQUE(circuit_id,numero));
CREATE TABLE ba_demarche (id varchar(255) PRIMARY KEY,lock_version bigint NOT NULL DEFAULT 0,version bigint DEFAULT 1,statut varchar(255) DEFAULT 'A',created_by varchar(50) NOT NULL,created_date timestamptz NOT NULL,last_modified_by varchar(50),last_modified_date timestamptz,code varchar(50) NOT NULL UNIQUE,libelle varchar(150) NOT NULL,description varchar(2000),domaine varchar(30) NOT NULL,publication_id varchar(255) NOT NULL REFERENCES ba_circuit_publication(id),actif boolean NOT NULL DEFAULT false,champs jsonb NOT NULL);
CREATE TABLE ba_demarche_dossier (id varchar(255) PRIMARY KEY,lock_version bigint NOT NULL DEFAULT 0,version bigint DEFAULT 1,statut varchar(255) DEFAULT 'A',created_by varchar(50) NOT NULL,created_date timestamptz NOT NULL,last_modified_by varchar(50),last_modified_date timestamptz,demarche_id varchar(255) NOT NULL REFERENCES ba_demarche(id),publication_id varchar(255) NOT NULL REFERENCES ba_circuit_publication(id),demandeur_id varchar(255) NOT NULL REFERENCES ba_utilisateur(id),titre varchar(200) NOT NULL,etat varchar(20) NOT NULL,cle_creation varchar(100) NOT NULL,empreinte_creation varchar(64) NOT NULL,circuit_id varchar(255) REFERENCES ba_rh_circuit(id),valeurs jsonb NOT NULL,champs jsonb NOT NULL,operations jsonb NOT NULL,UNIQUE(demandeur_id,cle_creation));
CREATE TABLE ba_demarche_piece (id varchar(255) PRIMARY KEY,lock_version bigint NOT NULL DEFAULT 0,version bigint DEFAULT 1,statut varchar(255) DEFAULT 'A',created_by varchar(50) NOT NULL,created_date timestamptz NOT NULL,last_modified_by varchar(50),last_modified_date timestamptz,dossier_id varchar(255) NOT NULL REFERENCES ba_demarche_dossier(id),nom varchar(150) NOT NULL,mime varchar(100) NOT NULL,contenu bytea NOT NULL);
CREATE INDEX idx_demarche_dossier_demandeur ON ba_demarche_dossier(demandeur_id,created_date DESC);
CREATE INDEX idx_demarche_dossier_etat ON ba_demarche_dossier(etat,created_date DESC);
CREATE INDEX idx_demarche_piece_dossier ON ba_demarche_piece(dossier_id);
INSERT INTO ba_role(id,code,libelle,created_by,created_date,statut,version) VALUES
 ('role-processus-configurer','PROCESSUS_CONFIGURER','Configurer les circuits et demarches','migration',now(),'A',1),
 ('role-processus-demander','PROCESSUS_DEMANDER','Soumettre une demarche bancaire','migration',now(),'A',1),
 ('role-processus-valider','PROCESSUS_VALIDER','Valider une demarche dans son circuit','migration',now(),'A',1),
 ('role-processus-auditer','PROCESSUS_AUDITER','Auditer les demarches et decisions','migration',now(),'A',1);

ALTER TABLE ba_rh_circuit ADD COLUMN publication_id varchar(255) REFERENCES ba_circuit_publication(id);

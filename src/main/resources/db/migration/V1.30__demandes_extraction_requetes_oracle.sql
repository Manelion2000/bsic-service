CREATE TABLE ba_requete_oracle (
 id varchar(255) PRIMARY KEY,code varchar(50) NOT NULL,titre varchar(200) NOT NULL,objectif varchar(4000) NOT NULL,
 plateforme_id varchar(255) NOT NULL REFERENCES ba_plateforme(id),schema_cible varchar(128),sql_texte text NOT NULL,
 parametres jsonb NOT NULL DEFAULT '[]',edition integer NOT NULL CHECK(edition>0),verifiee boolean NOT NULL DEFAULT false,
 created_by varchar(50) NOT NULL,created_date timestamp(6) with time zone NOT NULL,last_modified_by varchar(50),
 last_modified_date timestamp(6) with time zone,statut varchar(255) DEFAULT 'A',version bigint DEFAULT 1
);
CREATE UNIQUE INDEX idx_requete_oracle_code ON ba_requete_oracle(lower(code));
CREATE INDEX idx_requete_oracle_catalogue ON ba_requete_oracle(statut,verifiee,titre,id);
CREATE TABLE ba_requete_revision (
 id varchar(255) PRIMARY KEY,requete_id varchar(255) NOT NULL REFERENCES ba_requete_oracle(id),edition integer NOT NULL,
 date timestamp(6) with time zone NOT NULL,auteur_id varchar(255) NOT NULL,auteur varchar(255) NOT NULL,
 motif varchar(1000) NOT NULL,contenu jsonb NOT NULL,UNIQUE(requete_id,edition)
);
CREATE TABLE ba_extraction_demande (
 id varchar(255) PRIMARY KEY,reference varchar(30) NOT NULL UNIQUE,titre varchar(200) NOT NULL,objectif varchar(4000) NOT NULL,
 date_demande date NOT NULL,echeance date,periode_debut date,periode_fin date,criteres varchar(4000),
 demandeur_id varchar(255) NOT NULL REFERENCES ba_utilisateur(id),departement_id varchar(255) NOT NULL REFERENCES ba_departement(id),
 departement_nom varchar(255) NOT NULL,demandeur_nom varchar(255) NOT NULL,auteur_id varchar(255) NOT NULL,auteur varchar(255) NOT NULL,
 etat varchar(30) NOT NULL CHECK(etat IN ('NOUVELLE','EN_COURS','TERMINEE','ANNULEE')),
 charge_de_id varchar(255) REFERENCES ba_utilisateur(id),revision_id varchar(255) REFERENCES ba_requete_revision(id),historique jsonb NOT NULL DEFAULT '[]',
 created_by varchar(50) NOT NULL,created_date timestamp(6) with time zone NOT NULL,last_modified_by varchar(50),last_modified_date timestamp(6) with time zone,
 statut varchar(255) DEFAULT 'A',version bigint DEFAULT 1,
 CHECK((periode_debut IS NULL AND periode_fin IS NULL) OR (periode_debut IS NOT NULL AND periode_fin IS NOT NULL AND periode_fin>=periode_debut)),
 CHECK(etat NOT IN ('EN_COURS','TERMINEE') OR (revision_id IS NOT NULL AND charge_de_id IS NOT NULL))
);
CREATE INDEX idx_extraction_demande_agent ON ba_extraction_demande(demandeur_id,date_demande,id);
CREATE INDEX idx_extraction_demande_departement ON ba_extraction_demande(departement_id,etat,date_demande,id);
CREATE TABLE ba_extraction_execution (
 id varchar(255) PRIMARY KEY,revision_id varchar(255) NOT NULL REFERENCES ba_requete_revision(id),demande_id varchar(255) REFERENCES ba_extraction_demande(id),
 execute_le timestamp(6) with time zone NOT NULL,enregistre_le timestamp(6) with time zone NOT NULL,auteur_id varchar(255) NOT NULL,auteur varchar(255) NOT NULL,
 environnement varchar(30) NOT NULL,resultat varchar(20) NOT NULL CHECK(resultat IN ('SUCCES','ECHEC')),
 reference_verification varchar(500) NOT NULL,lignes bigint CHECK(lignes>=0),commentaire varchar(1000)
);
CREATE INDEX idx_extraction_execution_revision ON ba_extraction_execution(revision_id,execute_le,enregistre_le,id);
CREATE INDEX idx_extraction_execution_demande ON ba_extraction_execution(demande_id,enregistre_le,id);
INSERT INTO ba_role(id,code,libelle,created_by,created_date,statut,version) VALUES
 ('role-extractions-demander','EXTRACTIONS_DEMANDER','Soumettre et suivre mes demandes d’extraction','migration',now(),'A',1),
 ('role-requetes-consulter','REQUETES_CONSULTER','Consulter les requêtes Oracle vérifiées et leurs essais','migration',now(),'A',1),
 ('role-extractions-gerer','EXTRACTIONS_GERER','Gérer les extractions et la bibliothèque Oracle','migration',now(),'A',1)
ON CONFLICT(code) DO NOTHING;
-- Aucun droit attribué automatiquement, aucune connexion ou exécution Oracle.

-- Ajouter le suivi de traitement, sans exposer l'objectif, le SQL ou les traces techniques.
CREATE OR REPLACE VIEW ba_suivi_demande AS
 SELECT 'DEMARCHE'::text AS module,d.id,d.demandeur_id,d.etat,c.created_date AS soumise_le,NULL::date AS debut,NULL::date AS fin,d.circuit_id
 FROM ba_demarche_dossier d JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A' AND d.etat<>'BROUILLON'
 UNION ALL
 SELECT 'ABSENCE',d.id,d.demandeur_id,CASE WHEN d.statut_validation='VALIDEE' THEN 'APPROUVEE' ELSE d.statut_validation END,COALESCE(c.created_date,d.created_date),d.date_debut,d.date_fin,d.circuit_id
 FROM ba_autorisation_absence d LEFT JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A'
 UNION ALL
 SELECT 'SORTIE',d.id,d.demandeur_id,CASE WHEN d.statut_validation='VALIDEE' THEN 'APPROUVEE' ELSE d.statut_validation END,COALESCE(c.created_date,d.created_date),d.date_sortie,d.date_sortie,d.circuit_id
 FROM ba_autorisation_sortie d LEFT JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A'
 UNION ALL
 SELECT 'CONGE',d.id,d.agent_id,d.etat,COALESCE(d.soumise_le,c.created_date),d.debut,d.fin,d.circuit_id
 FROM ba_conge_demande d JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A' AND d.etat<>'BROUILLON'
 UNION ALL
 SELECT 'REPRISE',d.id,d.demandeur_id,CASE WHEN d.statut_validation='VALIDEE' THEN 'APPROUVEE' ELSE d.statut_validation END,d.created_date,d.date_reprise,d.date_reprise,NULL::varchar
 FROM ba_reprise_service d WHERE d.statut='A'
 UNION ALL
 SELECT 'HABILITATION',d.id,d.employe_id,CASE d.statut_habilitation WHEN 'VALIDEE' THEN 'APPROUVEE' WHEN 'EN_COURS' THEN 'EN_ATTENTE' ELSE d.statut_habilitation END,d.created_date,NULL::date,NULL::date,NULL::varchar
 FROM ba_fiche_habilitation d WHERE d.statut='A'
 UNION ALL
 SELECT 'REINITIALISATION',d.id,d.demandeur_id,CASE d.statut_demande WHEN 'VALIDEE' THEN 'APPROUVEE' WHEN 'EN_COURS' THEN 'EN_ATTENTE' ELSE d.statut_demande END,d.created_date,NULL::date,NULL::date,NULL::varchar
 FROM ba_reinitialisation_compte d WHERE d.statut='A'
 UNION ALL
 SELECT 'EXTRACTION',d.id,d.demandeur_id,CASE WHEN d.etat IN ('NOUVELLE','EN_COURS') THEN 'EN_ATTENTE' ELSE d.etat END,d.created_date,d.date_demande,d.echeance,NULL::varchar
 FROM ba_extraction_demande d WHERE d.statut='A';

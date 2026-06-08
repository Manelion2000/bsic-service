CREATE TABLE IF NOT EXISTS ba_reinitialisation_compte (
    id VARCHAR(255) PRIMARY KEY,
    demandeur_id VARCHAR(255) NOT NULL,
    plateforme_id VARCHAR(255) NOT NULL,
    circuit_id VARCHAR(255) NOT NULL,
    motif VARCHAR(1500) NOT NULL,
    statut_demande VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    date_validation TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT,
    CONSTRAINT fk_reinit_demandeur FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT fk_reinit_plateforme FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id),
    CONSTRAINT fk_reinit_circuit FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id)
);

CREATE TABLE IF NOT EXISTS ba_reinitialisation_compte_etape (
    id VARCHAR(255) PRIMARY KEY,
    demande_id VARCHAR(255) NOT NULL,
    validateur_id VARCHAR(255),
    etape_definition_id VARCHAR(255),
    ordre INTEGER,
    statut_validation VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire VARCHAR(1500),
    date_validation TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT,
    CONSTRAINT fk_reinit_etape_demande FOREIGN KEY (demande_id) REFERENCES ba_reinitialisation_compte(id),
    CONSTRAINT fk_reinit_etape_validateur FOREIGN KEY (validateur_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT fk_reinit_etape_definition FOREIGN KEY (etape_definition_id) REFERENCES ba_etape_definition(id)
);

CREATE TABLE IF NOT EXISTS ba_reinitialisation_compte_traitement (
    id VARCHAR(255) PRIMARY KEY,
    demande_id VARCHAR(255) NOT NULL UNIQUE,
    statut_traitement VARCHAR(30) NOT NULL DEFAULT 'A_TRAITER',
    date_debut_traitement TIMESTAMP WITH TIME ZONE,
    date_traitement TIMESTAMP WITH TIME ZONE,
    traite_par_id VARCHAR(255),
    commentaire_traitement VARCHAR(1500),
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT,
    CONSTRAINT fk_reinit_traitement_demande FOREIGN KEY (demande_id) REFERENCES ba_reinitialisation_compte(id),
    CONSTRAINT fk_reinit_traitement_user FOREIGN KEY (traite_par_id) REFERENCES ba_utilisateur(id)
);

INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version)
SELECT 'role-hab-reinit-reseaux', 'HABILITATION_REINITIALISATION_COMPTE_RESEAUX',
       'Reinitialisation comptes plateformes reseaux', 'system', CURRENT_TIMESTAMP, 'A', 0
WHERE NOT EXISTS (SELECT 1 FROM ba_role WHERE code = 'HABILITATION_REINITIALISATION_COMPTE_RESEAUX');

INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version)
SELECT 'role-hab-reinit-core-banking', 'HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING',
       'Reinitialisation comptes plateformes core banking', 'system', CURRENT_TIMESTAMP, 'A', 0
WHERE NOT EXISTS (SELECT 1 FROM ba_role WHERE code = 'HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING');

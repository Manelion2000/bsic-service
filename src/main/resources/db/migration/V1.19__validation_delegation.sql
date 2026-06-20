CREATE TABLE IF NOT EXISTS ba_validation_delegation (
    id VARCHAR(255) PRIMARY KEY,
    delegant_id VARCHAR(255),
    delegue_id VARCHAR(255) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    departement_id VARCHAR(255),
    service_id VARCHAR(255),
    agence_id VARCHAR(255),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    motif VARCHAR(1000),
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP,
    statut VARCHAR(20),
    version BIGINT DEFAULT 1,
    CONSTRAINT fk_validation_delegation_delegant FOREIGN KEY (delegant_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT fk_validation_delegation_delegue FOREIGN KEY (delegue_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT fk_validation_delegation_departement FOREIGN KEY (departement_id) REFERENCES ba_departement(id),
    CONSTRAINT fk_validation_delegation_service FOREIGN KEY (service_id) REFERENCES ba_service(id),
    CONSTRAINT fk_validation_delegation_agence FOREIGN KEY (agence_id) REFERENCES ba_agence(id)
);

CREATE INDEX IF NOT EXISTS idx_validation_delegation_delegue
    ON ba_validation_delegation(delegue_id, actif, date_debut, date_fin);

CREATE INDEX IF NOT EXISTS idx_validation_delegation_role
    ON ba_validation_delegation(role_code);

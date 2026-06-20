CREATE TABLE IF NOT EXISTS ba_dga_pole_validateur (
    id VARCHAR(255) PRIMARY KEY,
    pole VARCHAR(30) NOT NULL,
    dga_id VARCHAR(255) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP,
    statut VARCHAR(20),
    version BIGINT DEFAULT 1,
    CONSTRAINT fk_dga_pole_validateur_dga FOREIGN KEY (dga_id) REFERENCES ba_utilisateur(id)
);

CREATE INDEX IF NOT EXISTS idx_dga_pole_validateur_pole
    ON ba_dga_pole_validateur(pole, actif, date_debut, date_fin);

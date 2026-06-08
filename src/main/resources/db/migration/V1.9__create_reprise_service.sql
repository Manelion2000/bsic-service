CREATE TABLE IF NOT EXISTS ba_reprise_service (
    id VARCHAR(255) PRIMARY KEY,
    demandeur_id VARCHAR(255) NOT NULL,
    signataire_id VARCHAR(255) NOT NULL,
    motif_absence VARCHAR(1000) NOT NULL,
    date_debut_conge DATE NOT NULL,
    date_fin_conge DATE NOT NULL,
    date_reprise DATE NOT NULL,
    statut_validation VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire_validation VARCHAR(1500),
    date_validation TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT,
    CONSTRAINT fk_reprise_service_demandeur FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT fk_reprise_service_signataire FOREIGN KEY (signataire_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT chk_reprise_service_statut CHECK (statut_validation IN ('EN_ATTENTE', 'VALIDEE', 'REJETEE'))
);

CREATE INDEX IF NOT EXISTS idx_reprise_service_demandeur ON ba_reprise_service(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_reprise_service_signataire ON ba_reprise_service(signataire_id);
CREATE INDEX IF NOT EXISTS idx_reprise_service_statut_validation ON ba_reprise_service(statut_validation);

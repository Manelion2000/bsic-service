CREATE TABLE ba_autorisation_absence (
    id VARCHAR(255) PRIMARY KEY,
    motif VARCHAR(1000) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours INTEGER NOT NULL,
    demandeur_id VARCHAR(255) NOT NULL REFERENCES ba_utilisateur(id),
    validateur_id VARCHAR(255) REFERENCES ba_utilisateur(id),
    statut_validation VARCHAR(20) NOT NULL,
    commentaire_validation VARCHAR(1500),
    date_validation TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT DEFAULT 1,
    CONSTRAINT ck_absence_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_absence_nombre_jours CHECK (nombre_jours > 0),
    CONSTRAINT ck_absence_statut CHECK (statut_validation IN ('EN_ATTENTE','VALIDEE','REJETEE','ANNULEE'))
);

CREATE INDEX idx_absence_demandeur_dates ON ba_autorisation_absence(demandeur_id, date_debut, date_fin);
CREATE INDEX idx_absence_validation ON ba_autorisation_absence(statut_validation, created_date DESC);

INSERT INTO ba_role(id, libelle, code, created_by, created_date, statut, version)
SELECT md5(random()::text || clock_timestamp()::text), 'Consultation des absences RH', 'RH_CONSULTATION_ABSENCES', 'flyway', CURRENT_TIMESTAMP, 'A', 1
WHERE NOT EXISTS (SELECT 1 FROM ba_role WHERE code = 'RH_CONSULTATION_ABSENCES');

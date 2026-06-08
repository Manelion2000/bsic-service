CREATE TABLE IF NOT EXISTS ba_habilitation_compte_plateforme (
    id VARCHAR(255) PRIMARY KEY,
    fiche_id VARCHAR(255) NOT NULL,
    plateforme_id VARCHAR(255) NOT NULL,
    demandeur_id VARCHAR(255) NOT NULL,
    statut_creation VARCHAR(30) NOT NULL DEFAULT 'A_CREER',
    date_debut_creation TIMESTAMP WITH TIME ZONE,
    date_creation TIMESTAMP WITH TIME ZONE,
    cree_par_id VARCHAR(255),
    commentaire_creation VARCHAR(1500),
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT,
    CONSTRAINT fk_hab_compte_fiche FOREIGN KEY (fiche_id) REFERENCES ba_fiche_habilitation(id),
    CONSTRAINT fk_hab_compte_plateforme FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id),
    CONSTRAINT fk_hab_compte_demandeur FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT fk_hab_compte_cree_par FOREIGN KEY (cree_par_id) REFERENCES ba_utilisateur(id),
    CONSTRAINT uq_hab_compte_fiche_plateforme UNIQUE (fiche_id, plateforme_id),
    CONSTRAINT chk_hab_compte_statut CHECK (statut_creation IN ('A_CREER', 'EN_COURS_CREATION', 'CREE'))
);

CREATE INDEX IF NOT EXISTS idx_hab_compte_statut_creation ON ba_habilitation_compte_plateforme(statut_creation);
CREATE INDEX IF NOT EXISTS idx_hab_compte_demandeur ON ba_habilitation_compte_plateforme(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_hab_compte_plateforme ON ba_habilitation_compte_plateforme(plateforme_id);

INSERT INTO ba_habilitation_compte_plateforme (
    id,
    fiche_id,
    plateforme_id,
    demandeur_id,
    statut_creation,
    created_by,
    created_date,
    statut,
    version
)
SELECT
    'hab-compte-' || f.id || '-' || p.id,
    f.id,
    p.id,
    f.employe_id,
    'A_CREER',
    COALESCE(f.last_modified_by, f.created_by, 'system'),
    CURRENT_TIMESTAMP,
    'A',
    0
FROM ba_fiche_habilitation f
JOIN ba_circuit c ON c.id = f.circuit_id
JOIN ba_plateforme p ON p.id = c.plateforme_id
WHERE f.statut_habilitation = 'VALIDEE'
  AND NOT EXISTS (
      SELECT 1
      FROM ba_habilitation_compte_plateforme existing
      WHERE existing.fiche_id = f.id
        AND existing.plateforme_id = p.id
  );

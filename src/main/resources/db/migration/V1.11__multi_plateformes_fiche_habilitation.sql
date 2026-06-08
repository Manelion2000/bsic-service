CREATE TABLE IF NOT EXISTS ba_fiche_habilitation_plateforme (
    id VARCHAR(255) PRIMARY KEY,
    fiche_id VARCHAR(255) NOT NULL,
    plateforme_id VARCHAR(255) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    statut VARCHAR(20),
    version BIGINT,
    CONSTRAINT fk_fiche_plateforme_fiche FOREIGN KEY (fiche_id) REFERENCES ba_fiche_habilitation(id),
    CONSTRAINT fk_fiche_plateforme_plateforme FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id),
    CONSTRAINT uq_fiche_plateforme UNIQUE (fiche_id, plateforme_id)
);

CREATE INDEX IF NOT EXISTS idx_fiche_plateforme_fiche ON ba_fiche_habilitation_plateforme(fiche_id);
CREATE INDEX IF NOT EXISTS idx_fiche_plateforme_plateforme ON ba_fiche_habilitation_plateforme(plateforme_id);

INSERT INTO ba_fiche_habilitation_plateforme (
    id,
    fiche_id,
    plateforme_id,
    created_by,
    created_date,
    statut,
    version
)
SELECT
    'fiche-plateforme-' || f.id || '-' || p.id,
    f.id,
    p.id,
    COALESCE(f.created_by, 'system'),
    COALESCE(f.created_date, CURRENT_TIMESTAMP),
    'A',
    0
FROM ba_fiche_habilitation f
JOIN ba_circuit c ON c.id = f.circuit_id
JOIN ba_plateforme p ON p.id = c.plateforme_id
WHERE NOT EXISTS (
    SELECT 1
    FROM ba_fiche_habilitation_plateforme fp
    WHERE fp.fiche_id = f.id
      AND fp.plateforme_id = p.id
);

ALTER TABLE circuit_etape
    ADD COLUMN IF NOT EXISTS role_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fonction_requise VARCHAR(100),
    ADD COLUMN IF NOT EXISTS obligatoire BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS actif BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS delai_validation_jours INTEGER,
    ADD COLUMN IF NOT EXISTS conditions VARCHAR(1500);

UPDATE circuit_etape
SET obligatoire = TRUE
WHERE obligatoire IS NULL;

UPDATE circuit_etape
SET actif = TRUE
WHERE actif IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'ba_circuit_etape_config'
    ) THEN
        INSERT INTO circuit_etape (
            id,
            created_by,
            created_date,
            statut,
            version,
            ordre,
            type,
            circuit_id,
            departement_id,
            role_id,
            obligatoire,
            actif
        )
        SELECT
            old_cfg.id,
            COALESCE(old_cfg.created_by, 'system'),
            COALESCE(old_cfg.created_date, CURRENT_TIMESTAMP),
            COALESCE(old_cfg.statut, 'A'),
            COALESCE(old_cfg.version, 0),
            old_cfg.ordre,
            'DEPARTEMENT',
            old_cfg.circuit_id,
            old_cfg.departement_id,
            old_cfg.role_validateur_id,
            COALESCE(old_cfg.obligatoire, TRUE),
            COALESCE(old_cfg.actif, TRUE)
        FROM ba_circuit_etape_config old_cfg
        WHERE old_cfg.circuit_id IS NOT NULL
          AND old_cfg.departement_id IS NOT NULL
          AND old_cfg.ordre IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM circuit_etape etape
              WHERE etape.circuit_id = old_cfg.circuit_id
                AND etape.ordre = old_cfg.ordre
          );
    END IF;
END $$;

UPDATE ba_circuit
SET statu = CASE
    WHEN COALESCE(actif, TRUE) THEN 'ACTIF'
    ELSE 'INACTIF'
END
WHERE statu IS NULL;

UPDATE ba_circuit
SET actif = CASE
    WHEN statu = 'ACTIF' THEN TRUE
    ELSE FALSE
END
WHERE statu IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_circuit_etape_circuit_ordre
    ON circuit_etape(circuit_id, ordre);

CREATE INDEX IF NOT EXISTS idx_circuit_etape_role
    ON circuit_etape(role_id);

UPDATE circuit_etape
SET role_id = NULL
WHERE role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM ba_role role
      WHERE role.id = circuit_etape.role_id
  );

ALTER TABLE circuit_etape
    DROP CONSTRAINT IF EXISTS fk_circuit_etape_role;

ALTER TABLE circuit_etape
    ADD CONSTRAINT fk_circuit_etape_role
    FOREIGN KEY (role_id) REFERENCES ba_role(id);

DROP TABLE IF EXISTS ba_circuit_etape_config;

ALTER TABLE ba_departement
    ADD COLUMN IF NOT EXISTS dga_pole VARCHAR(30);

ALTER TABLE ba_departement
    ADD COLUMN IF NOT EXISTS parent_departement_id VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ba_departement_parent'
    ) THEN
        ALTER TABLE ba_departement
            ADD CONSTRAINT fk_ba_departement_parent
                FOREIGN KEY (parent_departement_id)
                REFERENCES ba_departement (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ba_departement_parent
    ON ba_departement(parent_departement_id);

CREATE INDEX IF NOT EXISTS idx_ba_departement_dga_pole
    ON ba_departement(dga_pole);

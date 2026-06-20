ALTER TABLE ba_departement
    ADD COLUMN IF NOT EXISTS dga_validateur_id VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ba_departement_dga_validateur'
    ) THEN
        ALTER TABLE ba_departement
            ADD CONSTRAINT fk_ba_departement_dga_validateur
                FOREIGN KEY (dga_validateur_id)
                REFERENCES ba_utilisateur (id);
    END IF;
END $$;

ALTER TABLE ba_plateforme
    ADD COLUMN IF NOT EXISTS type_plateforme VARCHAR(30);

UPDATE ba_plateforme
SET type_plateforme = 'RESEAUX'
WHERE type_plateforme IS NULL;

ALTER TABLE ba_plateforme
    ALTER COLUMN type_plateforme SET DEFAULT 'RESEAUX';

INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version)
SELECT 'role-hab-creation-reseaux',
       'HABILITATION_CREATION_COMPTE_RESEAUX',
       'Creation comptes plateformes reseaux',
       'system',
       CURRENT_TIMESTAMP,
       'A',
       0
WHERE NOT EXISTS (
    SELECT 1 FROM ba_role WHERE code = 'HABILITATION_CREATION_COMPTE_RESEAUX'
);

INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version)
SELECT 'role-hab-creation-core-banking',
       'HABILITATION_CREATION_COMPTE_CORE_BANKING',
       'Creation comptes plateformes core banking',
       'system',
       CURRENT_TIMESTAMP,
       'A',
       0
WHERE NOT EXISTS (
    SELECT 1 FROM ba_role WHERE code = 'HABILITATION_CREATION_COMPTE_CORE_BANKING'
);

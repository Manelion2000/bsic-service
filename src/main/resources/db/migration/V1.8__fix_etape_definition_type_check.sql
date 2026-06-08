-- Corrige la contrainte PostgreSQL historique sur ba_etape_definition.type.
-- Le code Java utilise les valeurs enum EHabilitationEtapeType: DEPARTEMENT, SERVICE.

ALTER TABLE ba_etape_definition
DROP CONSTRAINT IF EXISTS ba_etape_definition_type_check;

ALTER TABLE ba_etape_definition
ADD CONSTRAINT ba_etape_definition_type_check
CHECK (type IN ('DEPARTEMENT', 'SERVICE'));

-- Fonctions organisationnelles conservees dans ba_utilisateur.fonction.
-- Ces roles techniques ne conferent aucun droit d'administration.
INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version)
VALUES ('role-ba-dg', 'BA_DG', 'Directeur general', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
       ('role-ba-dga', 'BA_DGA', 'Directeur general adjoint', 'flyway', CURRENT_TIMESTAMP, 'A', 1)
ON CONFLICT (code) DO NOTHING;

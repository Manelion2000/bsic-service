-- Conserver les references et justifications completes sans saturer le titre historique de 255 caracteres.
ALTER TABLE ba_log ALTER COLUMN details TYPE text;
ALTER TABLE ba_conge_droit ALTER COLUMN source TYPE varchar(1200);
ALTER TABLE ba_conge_mouvement ALTER COLUMN source TYPE varchar(1200);
ALTER TABLE ba_conge_mouvement ALTER COLUMN justification TYPE varchar(2500);

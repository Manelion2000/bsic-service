CREATE INDEX IF NOT EXISTS idx_hab_compte_plateforme_plateforme
    ON ba_habilitation_compte_plateforme (plateforme_id);

CREATE INDEX IF NOT EXISTS idx_hab_compte_plateforme_demandeur
    ON ba_habilitation_compte_plateforme (demandeur_id);

CREATE INDEX IF NOT EXISTS idx_hab_compte_plateforme_cree_par
    ON ba_habilitation_compte_plateforme (cree_par_id);

CREATE INDEX IF NOT EXISTS idx_hab_compte_plateforme_statut_creation
    ON ba_habilitation_compte_plateforme (statut_creation);

CREATE INDEX IF NOT EXISTS idx_hab_compte_plateforme_created_date
    ON ba_habilitation_compte_plateforme (created_date);

CREATE INDEX IF NOT EXISTS idx_reinit_compte_plateforme
    ON ba_reinitialisation_compte (plateforme_id);

CREATE INDEX IF NOT EXISTS idx_reinit_compte_demandeur
    ON ba_reinitialisation_compte (demandeur_id);

CREATE INDEX IF NOT EXISTS idx_reinit_compte_statut_demande
    ON ba_reinitialisation_compte (statut_demande);

CREATE INDEX IF NOT EXISTS idx_reinit_compte_created_date
    ON ba_reinitialisation_compte (created_date);

CREATE INDEX IF NOT EXISTS idx_reinit_traitement_statut
    ON ba_reinitialisation_compte_traitement (statut_traitement);

CREATE INDEX IF NOT EXISTS idx_reinit_traitement_traite_par
    ON ba_reinitialisation_compte_traitement (traite_par_id);

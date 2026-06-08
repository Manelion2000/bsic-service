CREATE INDEX IF NOT EXISTS idx_autorisation_sortie_demandeur
    ON ba_autorisation_sortie (demandeur_id);

CREATE INDEX IF NOT EXISTS idx_autorisation_sortie_validateur
    ON ba_autorisation_sortie (validateur_id);

CREATE INDEX IF NOT EXISTS idx_autorisation_sortie_statut_validation
    ON ba_autorisation_sortie (statut_validation);

CREATE INDEX IF NOT EXISTS idx_autorisation_sortie_date_sortie
    ON ba_autorisation_sortie (date_sortie);

CREATE INDEX IF NOT EXISTS idx_autorisation_sortie_created_date
    ON ba_autorisation_sortie (created_date);

CREATE INDEX IF NOT EXISTS idx_reprise_service_demandeur
    ON ba_reprise_service (demandeur_id);

CREATE INDEX IF NOT EXISTS idx_reprise_service_signataire
    ON ba_reprise_service (signataire_id);

CREATE INDEX IF NOT EXISTS idx_reprise_service_statut_validation
    ON ba_reprise_service (statut_validation);

CREATE INDEX IF NOT EXISTS idx_reprise_service_date_reprise
    ON ba_reprise_service (date_reprise);

CREATE INDEX IF NOT EXISTS idx_reprise_service_created_date
    ON ba_reprise_service (created_date);

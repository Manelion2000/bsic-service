-- Droits de consultation uniquement. Aucune attribution automatique à un compte.
INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version) VALUES
 ('role-suivi-departement', 'SUIVI_DEMANDES_DEPARTEMENT', 'Suivre les demandes de mon département', 'migration', now(), 'A', 1),
 ('role-suivi-global', 'SUIVI_DEMANDES_GLOBAL', 'Suivre les demandes de toute la banque', 'migration', now(), 'A', 1)
ON CONFLICT (code) DO NOTHING;

-- Projection de suivi : ni motifs, ni champs de formulaire, ni justificatifs.
-- Les brouillons et retraits de brouillons restent privés.
CREATE VIEW ba_suivi_demande AS
 SELECT 'DEMARCHE'::text AS module, d.id, d.demandeur_id, d.etat,
        c.created_date AS soumise_le, NULL::date AS debut, NULL::date AS fin, d.circuit_id
 FROM ba_demarche_dossier d JOIN ba_rh_circuit c ON c.id=d.circuit_id
 WHERE d.statut='A' AND d.etat<>'BROUILLON'
 UNION ALL
 SELECT 'ABSENCE', d.id, d.demandeur_id,
        CASE WHEN d.statut_validation='VALIDEE' THEN 'APPROUVEE' ELSE d.statut_validation END,
        COALESCE(c.created_date,d.created_date), d.date_debut, d.date_fin, d.circuit_id
 FROM ba_autorisation_absence d LEFT JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A'
 UNION ALL
 SELECT 'SORTIE', d.id, d.demandeur_id,
        CASE WHEN d.statut_validation='VALIDEE' THEN 'APPROUVEE' ELSE d.statut_validation END,
        COALESCE(c.created_date,d.created_date), d.date_sortie, d.date_sortie, d.circuit_id
 FROM ba_autorisation_sortie d LEFT JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A'
 UNION ALL
 SELECT 'CONGE', d.id, d.agent_id, d.etat, COALESCE(d.soumise_le,c.created_date), d.debut, d.fin, d.circuit_id
 FROM ba_conge_demande d JOIN ba_rh_circuit c ON c.id=d.circuit_id WHERE d.statut='A' AND d.etat<>'BROUILLON'
 UNION ALL
 SELECT 'REPRISE', d.id, d.demandeur_id,
        CASE WHEN d.statut_validation='VALIDEE' THEN 'APPROUVEE' ELSE d.statut_validation END,
        d.created_date, d.date_reprise, d.date_reprise, NULL::varchar
 FROM ba_reprise_service d WHERE d.statut='A'
 UNION ALL
 SELECT 'HABILITATION', d.id, d.employe_id,
        CASE d.statut_habilitation WHEN 'VALIDEE' THEN 'APPROUVEE' WHEN 'EN_COURS' THEN 'EN_ATTENTE' ELSE d.statut_habilitation END,
        d.created_date, NULL::date, NULL::date, NULL::varchar
 FROM ba_fiche_habilitation d WHERE d.statut='A'
 UNION ALL
 SELECT 'REINITIALISATION', d.id, d.demandeur_id,
        CASE d.statut_demande WHEN 'VALIDEE' THEN 'APPROUVEE' WHEN 'EN_COURS' THEN 'EN_ATTENTE' ELSE d.statut_demande END,
        d.created_date, NULL::date, NULL::date, NULL::varchar
 FROM ba_reinitialisation_compte d WHERE d.statut='A';

CREATE INDEX IF NOT EXISTS idx_utilisateur_suivi_departement ON ba_utilisateur(departement_id);
CREATE INDEX IF NOT EXISTS idx_service_suivi_departement ON ba_service(departement_id);

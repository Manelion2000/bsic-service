-- Les demandes historiques conservent leurs donnees. Une demande en attente
-- sans snapshot doit etre annulee et soumise a nouveau : son organigramme
-- au jour de la soumission ne peut pas etre reconstruit de facon fiable.
CREATE TABLE ba_rh_circuit (
    id varchar(255) PRIMARY KEY,
    demandeur_id varchar(255) NOT NULL REFERENCES ba_utilisateur(id),
    fonction_demandeur varchar(50) NOT NULL,
    departement_id varchar(255),
    dg_obligatoire boolean NOT NULL,
    politique_refus varchar(30) NOT NULL CHECK (politique_refus IN ('NON_CONFIGURE', 'DEFINITIF', 'AUTRES_VALIDATEURS')),
    statut varchar(20) NOT NULL CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'REJETEE', 'ANNULEE')),
    etape_courante integer NOT NULL CHECK (etape_courante >= 0),
    created_date timestamptz NOT NULL,
    lock_version bigint NOT NULL DEFAULT 0,
    etapes jsonb NOT NULL CHECK (jsonb_typeof(etapes) = 'array' AND jsonb_array_length(etapes) > 0),
    historique jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(historique) = 'array'),
    CHECK (etape_courante < jsonb_array_length(etapes))
);
ALTER TABLE ba_autorisation_absence ADD COLUMN circuit_id varchar(255) UNIQUE REFERENCES ba_rh_circuit(id);
ALTER TABLE ba_autorisation_sortie ADD COLUMN circuit_id varchar(255) UNIQUE REFERENCES ba_rh_circuit(id);
CREATE INDEX idx_rh_circuit_demandeur ON ba_rh_circuit(demandeur_id);

CREATE TABLE ba_user_signature (
    user_id varchar(255) PRIMARY KEY REFERENCES ba_utilisateur(id),
    contenu bytea NOT NULL,
    empreinte varchar(64) NOT NULL,
    updated_date timestamptz NOT NULL,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT signature_personnelle_taille CHECK (octet_length(contenu) BETWEEN 1 AND 2097152)
);

CREATE TABLE ba_acte_signature (
    id varchar(255) PRIMARY KEY,
    domaine varchar(30) NOT NULL,
    reference_id varchar(255) NOT NULL,
    etape integer NOT NULL,
    signataire_id varchar(255) NOT NULL REFERENCES ba_utilisateur(id),
    nom_signataire varchar(500) NOT NULL,
    fonction_signataire varchar(100),
    date_signature timestamptz NOT NULL,
    empreinte varchar(64) NOT NULL,
    contenu bytea NOT NULL,
    CONSTRAINT acte_signature_etape_unique UNIQUE (domaine, reference_id, etape),
    CONSTRAINT acte_signature_taille CHECK (octet_length(contenu) BETWEEN 1 AND 2097152)
);
COMMENT ON TABLE ba_acte_signature IS 'Visas figés lors de la validation, indépendants du remplacement ou retrait de la signature personnelle.';

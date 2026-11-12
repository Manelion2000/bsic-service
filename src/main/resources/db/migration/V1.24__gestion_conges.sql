-- Aucun droit, calendrier ou politique RH n'est active par cette migration.

CREATE TABLE ba_conge_politique (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    type_code varchar(50) not null,
    libelle varchar(150) not null,
    categorie varchar(30) not null,
    version_politique integer not null default 0,
    etat varchar(20) not null default 'BROUILLON',
    debut_effet date not null,
    fin_effet date,
    source_rh varchar(1000) not null,
    regles jsonb not null,
    confirme_par varchar(255),
    confirme_le timestamp(6) with time zone
);

CREATE TABLE ba_conge_calendrier (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    nom varchar(150) not null,
    debut_effet date not null,
    fin_effet date,
    source_rh varchar(1000) not null,
    confirme boolean not null default false,
    confirme_par varchar(255),
    confirme_le timestamp(6) with time zone,
    regles jsonb not null
);

CREATE TABLE ba_conge_affectation (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    agent_id varchar(255) not null,
    politique_id varchar(255) not null,
    calendrier_id varchar(255) not null,
    debut date not null,
    fin date,
    date_entree date,
    date_sortie date,
    justification varchar(1000) not null
);

CREATE TABLE ba_conge_droit (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    agent_id varchar(255) not null,
    politique_id varchar(255) not null,
    affectation_id varchar(255) not null,
    type_code varchar(50) not null,
    origine_debut date not null,
    origine_fin date not null,
    debut_disponibilite date not null,
    fin_disponibilite date not null,
    expiration date,
    origine_droit_id varchar(255),
    cloturee boolean not null default false,
    cle_origine varchar(180) not null unique
);

CREATE TABLE ba_conge_mouvement (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    droit_id varchar(255) not null,
    demande_id varchar(255),
    nature varchar(30) not null,
    quantite numeric(18,4) not null,
    cle varchar(200) not null unique,
    source varchar(1000) not null,
    justification varchar(1500) not null,
    auteur_id varchar(255) not null,
    date_operation timestamp(6) with time zone not null default CURRENT_TIMESTAMP
);

CREATE TABLE ba_conge_echeance (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    droit_id varchar(255) not null,
    date_acquisition date not null,
    quantite numeric(18,4) not null,
    cle varchar(200) not null unique,
    executee boolean not null default false
);

CREATE TABLE ba_conge_demande (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    agent_id varchar(255) not null,
    type_code varchar(50) not null,
    revision integer not null default 1,
    etat varchar(20) not null default 'BROUILLON',
    debut date not null,
    fin date not null,
    portion_debut varchar(20) not null,
    portion_fin varchar(20) not null,
    heure_debut time(6),
    heure_fin time(6),
    motif varchar(1000),
    remplace_demande_id varchar(255),
    reprise_anticipee date,
    reprise_constatee date,
    politique_id varchar(255),
    calendrier_id varchar(255),
    circuit_id varchar(255) unique,
    calcul jsonb,
    imputations jsonb not null default '[]'::jsonb,
    reservations jsonb not null default '[]'::jsonb,
    historique jsonb not null default '[]'::jsonb,
    soumise_le timestamp(6) with time zone,
    approuvee_le timestamp(6) with time zone,
    derogation_couverture varchar(1500),
    derogation_par varchar(255)
);

CREATE TABLE ba_conge_couverture (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    nom varchar(150) not null,
    debut date not null,
    fin date,
    source_rh varchar(1000) not null,
    confirme boolean not null default false,
    confirme_par varchar(255),
    confirme_le timestamp(6) with time zone,
    regles jsonb not null
);

CREATE TABLE ba_conge_souhait (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    agent_id varchar(255) not null,
    debut date not null,
    fin date not null
);

CREATE TABLE ba_conge_passation (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    demande_id varchar(255) not null unique,
    remplacant_id varchar(255),
    taches_et_echeances varchar(5000) not null,
    contacts_utiles varchar(1000),
    prise_en_charge_le timestamp(6) with time zone
);

CREATE TABLE ba_conge_piece (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    demande_id varchar(255) not null,
    nom_fichier varchar(150) not null,
    content_type varchar(100) not null,
    taille bigint not null,
    contenu bytea not null
);

CREATE TABLE ba_conge_notification (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    demande_id varchar(255) not null,
    destinataire_id varchar(255) not null,
    cle varchar(200) not null unique,
    evenement varchar(50) not null,
    envoyee boolean not null default false,
    essais integer not null default 0,
    prochain_essai timestamp(6) with time zone,
    lue_le timestamp(6) with time zone
);

CREATE TABLE ba_conge_anomalie (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    droit_id varchar(255),
    demande_id varchar(255),
    cle varchar(200) not null unique,
    description varchar(1000) not null,
    resolue boolean not null default false,
    resolue_par varchar(255)
);

CREATE TABLE ba_conge_operation (
    id varchar(255) primary key,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) default 'A',
    version bigint default 1,
    lock_version bigint not null default 0,
    cle varchar(200) not null unique,
    auteur_id varchar(255) not null,
    action varchar(50) not null,
    demande_id varchar(255),
    empreinte varchar(64) not null
);

ALTER TABLE ba_conge_affectation ADD FOREIGN KEY (agent_id) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_conge_affectation ADD FOREIGN KEY (politique_id) REFERENCES ba_conge_politique(id);
ALTER TABLE ba_conge_affectation ADD FOREIGN KEY (calendrier_id) REFERENCES ba_conge_calendrier(id);
ALTER TABLE ba_conge_droit ADD FOREIGN KEY (agent_id) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_conge_droit ADD FOREIGN KEY (politique_id) REFERENCES ba_conge_politique(id);
ALTER TABLE ba_conge_droit ADD FOREIGN KEY (affectation_id) REFERENCES ba_conge_affectation(id);
ALTER TABLE ba_conge_mouvement ADD FOREIGN KEY (droit_id) REFERENCES ba_conge_droit(id);
ALTER TABLE ba_conge_mouvement ADD FOREIGN KEY (demande_id) REFERENCES ba_conge_demande(id);
ALTER TABLE ba_conge_echeance ADD FOREIGN KEY (droit_id) REFERENCES ba_conge_droit(id);
ALTER TABLE ba_conge_demande ADD FOREIGN KEY (agent_id) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_conge_demande ADD FOREIGN KEY (remplace_demande_id) REFERENCES ba_conge_demande(id);
ALTER TABLE ba_conge_demande ADD FOREIGN KEY (politique_id) REFERENCES ba_conge_politique(id);
ALTER TABLE ba_conge_demande ADD FOREIGN KEY (calendrier_id) REFERENCES ba_conge_calendrier(id);
ALTER TABLE ba_conge_demande ADD FOREIGN KEY (circuit_id) REFERENCES ba_rh_circuit(id);
ALTER TABLE ba_conge_souhait ADD FOREIGN KEY (agent_id) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_conge_passation ADD FOREIGN KEY (demande_id) REFERENCES ba_conge_demande(id);
ALTER TABLE ba_conge_passation ADD FOREIGN KEY (remplacant_id) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_conge_piece ADD FOREIGN KEY (demande_id) REFERENCES ba_conge_demande(id);
ALTER TABLE ba_conge_notification ADD FOREIGN KEY (demande_id) REFERENCES ba_conge_demande(id);
ALTER TABLE ba_conge_notification ADD FOREIGN KEY (destinataire_id) REFERENCES ba_utilisateur(id);
ALTER TABLE ba_conge_anomalie ADD FOREIGN KEY (droit_id) REFERENCES ba_conge_droit(id);
ALTER TABLE ba_conge_anomalie ADD FOREIGN KEY (demande_id) REFERENCES ba_conge_demande(id);
CREATE UNIQUE INDEX uq_conge_politique_version ON ba_conge_politique(type_code, version_politique);
ALTER TABLE ba_conge_droit ADD COLUMN source varchar(1000) NOT NULL;
ALTER TABLE ba_conge_droit ADD COLUMN justification varchar(1000) NOT NULL;
ALTER TABLE ba_conge_demande ADD COLUMN reprise_demandee date;
ALTER TABLE ba_conge_demande ADD COLUMN portion_reprise_demandee varchar(20);
ALTER TABLE ba_conge_demande ADD COLUMN heure_reprise_demandee time(6);
ALTER TABLE ba_conge_demande ADD COLUMN retour_statut varchar(20);
ALTER TABLE ba_conge_demande ADD COLUMN restitutions jsonb NOT NULL DEFAULT '[]'::jsonb;
CREATE INDEX idx_conge_agent_etat_dates ON ba_conge_demande(agent_id, etat, debut, fin);
CREATE INDEX idx_conge_affectation_agent ON ba_conge_affectation(agent_id, debut, fin);
CREATE INDEX idx_conge_droit_agent ON ba_conge_droit(agent_id, type_code);
CREATE INDEX idx_conge_mouvement_droit ON ba_conge_mouvement(droit_id, date_operation);
CREATE INDEX idx_conge_echeance ON ba_conge_echeance(executee, date_acquisition);
CREATE UNIQUE INDEX uq_conge_remplacement_en_attente ON ba_conge_demande(remplace_demande_id)
    WHERE remplace_demande_id IS NOT NULL AND etat IN ('BROUILLON','EN_ATTENTE') AND statut = 'A';
ALTER TABLE ba_conge_demande ADD CHECK(fin >= debut);
ALTER TABLE ba_conge_droit ADD CHECK(origine_fin >= origine_debut AND fin_disponibilite >= debut_disponibilite);
ALTER TABLE ba_conge_mouvement ADD CHECK(quantite <> 0);
ALTER TABLE ba_conge_echeance ADD CHECK(quantite > 0);
ALTER TABLE ba_conge_affectation ADD CHECK(fin IS NULL OR fin >= debut);
ALTER TABLE ba_rh_circuit ADD COLUMN delegations_autorisees boolean NOT NULL DEFAULT false;
ALTER TABLE ba_rh_circuit ADD COLUMN delegations jsonb NOT NULL DEFAULT '[]'::jsonb;

INSERT INTO ba_role(id,code,libelle,created_by,created_date,statut,version) VALUES
('role-conges-demander', 'CONGES_DEMANDER', 'Demander un conge', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-valider', 'CONGES_VALIDER', 'Valider les conges a son etape', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-politiques', 'CONGES_POLITIQUES', 'Gerer les politiques et calendriers de conges', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-droits', 'CONGES_DROITS', 'Gerer et corriger les droits aux conges', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-pieces', 'CONGES_PIECES', 'Consulter les pieces et motifs sensibles de conges', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-exporter', 'CONGES_EXPORTER', 'Exporter les donnees autorisees de conges', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-auditer', 'CONGES_AUDITER', 'Auditer les operations de conges', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-planning', 'CONGES_PLANNING', 'Consulter le planning de son perimetre', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-annuler', 'CONGES_ANNULER', 'Controler les annulations et reprises de conges', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
('role-conges-derogation', 'CONGES_DEROGATION', 'Autoriser une derogation de couverture', 'flyway', CURRENT_TIMESTAMP, 'A', 1)
ON CONFLICT (code) DO NOTHING;
-- Aucune attribution automatique a ADMIN ou aux agents.

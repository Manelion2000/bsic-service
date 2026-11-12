-- Installation initiale du schema BSIC sur une base PostgreSQL vide.
-- Etat consolide jusqu'a 1.20, aligne sur les entites JPA de l'application.
-- Flyway execute ensuite V1.21 pour creer ba_autorisation_absence.
-- Les tables sont creees avant les cles etrangeres pour gerer les liens circulaires.
-- Aucune donnee utilisateur ni aucun mot de passe n'est insere.
create table ba_agence (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    adresse varchar(255),
    code varchar(255) unique,
    nom varchar(255),
    telephone varchar(255),
    telephone1 varchar(255),
    departement_id varchar(255),
    primary key (id)
);

create table ba_autorisation_sortie (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    commentaire_validation varchar(1500),
    date_sortie date not null,
    date_validation timestamp(6) with time zone,
    motif varchar(1000) not null,
    statut_validation varchar(20) not null check (statut_validation in ('EN_ATTENTE','VALIDEE','REJETEE')),
    type_structure varchar(20),
    agence_id varchar(255),
    demandeur_id varchar(255),
    direction_id varchar(255),
    service_id varchar(255),
    validateur_id varchar(255),
    primary key (id)
);

create table ba_dga_pole_validateur (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    actif boolean not null,
    date_debut date not null,
    date_fin date,
    pole varchar(30) not null check (pole in ('OPERATIONS','DEVELOPPEMENT')),
    dga_id varchar(255) not null,
    primary key (id)
);

create table ba_document_reunion (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    libelle varchar(255) not null unique,
    url varchar(255) not null unique,
    reunion_id varchar(255) unique,
    primary key (id)
);

create table ba_fiche_habilitation_etape (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    commentaire varchar(1500),
    date_validation timestamp(6) with time zone,
    ordre integer,
    statut_validation varchar(255) check (statut_validation in ('EN_ATTENTE','VALIDE','REJETE')),
    fiche_id varchar(255),
    validateur_id varchar(255),
    primary key (id)
);

create table ba_fiche_habilitation_plateforme (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    fiche_id varchar(255) not null,
    plateforme_id varchar(255) not null,
    primary key (id)
);

create table ba_habilitation_compte_plateforme (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    commentaire_creation varchar(1500),
    date_creation timestamp(6) with time zone,
    date_debut_creation timestamp(6) with time zone,
    statut_creation varchar(30) not null check (statut_creation in ('A_CREER','EN_COURS_CREATION','CREE')),
    cree_par_id varchar(255),
    demandeur_id varchar(255) not null,
    fiche_id varchar(255) not null,
    plateforme_id varchar(255) not null,
    primary key (id)
);

create table ba_log (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    action smallint check (action between 0 and 3),
    details varchar(255),
    ip_adresse varchar(255),
    sujet varchar(255),
    primary key (id)
);

create table ba_profil (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    description varchar(255),
    libelle varchar(60),
    primary key (id)
);

create table ba_profils_roles (
    profil_id varchar(255) not null,
    role_id varchar(255) not null,
    primary key (profil_id, role_id)
);

create table ba_reinitialisation_compte (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    date_validation timestamp(6) with time zone,
    motif varchar(1500) not null,
    statut_demande varchar(20) not null check (statut_demande in ('EN_COURS','VALIDEE','REJETEE')),
    circuit_id varchar(255) not null,
    demandeur_id varchar(255) not null,
    plateforme_id varchar(255) not null,
    primary key (id)
);

create table ba_reinitialisation_compte_etape (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    commentaire varchar(1500),
    date_validation timestamp(6) with time zone,
    ordre integer,
    statut_validation varchar(20) not null check (statut_validation in ('EN_ATTENTE','VALIDE','REJETE')),
    demande_id varchar(255) not null,
    validateur_id varchar(255),
    primary key (id)
);

create table ba_reinitialisation_compte_traitement (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    commentaire_traitement varchar(1500),
    date_debut_traitement timestamp(6) with time zone,
    date_traitement timestamp(6) with time zone,
    statut_traitement varchar(30) not null check (statut_traitement in ('A_TRAITER','EN_COURS_TRAITEMENT','TRAITEE')),
    demande_id varchar(255) not null unique,
    traite_par_id varchar(255),
    primary key (id)
);

create table ba_reprise_service (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    commentaire_validation varchar(1500),
    date_debut_conge date not null,
    date_fin_conge date not null,
    date_reprise date not null,
    date_validation timestamp(6) with time zone,
    motif_absence varchar(1000) not null,
    statut_validation varchar(20) not null check (statut_validation in ('EN_ATTENTE','VALIDEE','REJETEE')),
    demandeur_id varchar(255) not null,
    signataire_id varchar(255) not null,
    primary key (id)
);

create table ba_role (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    code varchar(255) unique,
    libelle varchar(255) unique,
    primary key (id)
);

create table ba_user_roles (
    user_id varchar(255) not null,
    role_id varchar(255) not null,
    primary key (user_id, role_id)
);

create table ba_utilisateur (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    activated boolean,
    email varchar(100),
    email_pro varchar(100),
    fonction varchar(50) check (fonction in ('DIRECTEUR','CHEF_SERVICE','DIRECTEUR_GENERAL','DIRECTEUR_GENERAL_ADJOINT','AGENT')),
    last_connexion_date timestamp(6) with time zone,
    account_locked boolean not null,
    matricule varchar(255) unique,
    nom varchar(50),
    password_hash varchar(254),
    prenom varchar(50),
    reset_date timestamp(6) with time zone,
    reset_key varchar(20),
    sexe varchar(255) check (sexe in ('MASCULIN','FEMININ')),
    telephone varchar(255) not null unique,
    telephone_fixe varchar(255),
    telephone_mobile varchar(255),
    username varchar(50) not null unique,
    agence_id varchar(255),
    departement_id varchar(255),
    profil_uuid varchar(255),
    service_id varchar(255),
    superieur_id varchar(255),
    superieur_secondaire_id varchar(255),
    primary key (id)
);

create table ba_validation_delegation (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    actif boolean not null,
    date_debut date not null,
    date_fin date not null,
    motif varchar(1000),
    role_code varchar(100) not null,
    agence_id varchar(255),
    delegant_id varchar(255),
    delegue_id varchar(255) not null,
    departement_id varchar(255),
    service_id varchar(255),
    primary key (id)
);

create table ba_circuit (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    actif boolean,
    code varchar(50) unique,
    description varchar(2000),
    libelle varchar(150),
    statu varchar(255) check (statu in ('EN_CONFIGURATION','ACTIF','INACTIF')),
    plateforme_id varchar(255),
    primary key (id)
);

create table ba_departement (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    code varchar(255) unique,
    dga_pole varchar(30) check (dga_pole in ('OPERATIONS','DEVELOPPEMENT')),
    nom varchar(255),
    dga_validateur_id varchar(255),
    parent_departement_id varchar(255),
    primary key (id)
);

create table ba_fiche_habilitation (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    fonction_employe varchar(100) check (fonction_employe in ('DIRECTEUR','CHEF_SERVICE','DIRECTEUR_GENERAL','DIRECTEUR_GENERAL_ADJOINT','AGENT')),
    justificatif_id varchar(255),
    motif varchar(1500),
    priorite varchar(255) check (priorite in ('BASSE','NORMALE','HAUTE')),
    statut_habilitation varchar(255) check (statut_habilitation in ('EN_COURS','VALIDEE','REJETEE')),
    telephone_mobile_employe varchar(30),
    circuit_id varchar(255),
    employe_id varchar(255),
    primary key (id)
);

create table ba_plateforme (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    code varchar(255) unique,
    description varchar(2000),
    nom varchar(255),
    type_plateforme varchar(30) check (type_plateforme in ('RESEAUX','CORE_BANKING')),
    circuit_id varchar(255),
    primary key (id)
);

create table ba_reunion (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    date_heure timestamp(6) with time zone,
    objet varchar(1500),
    proces_verbal_id varchar(255),
    titre varchar(255),
    valide_par_directeur boolean,
    agence_id varchar(255),
    departement_id varchar(255),
    service_id varchar(255),
    primary key (id)
);

create table ba_reunion_action (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    date_limite timestamp(6) with time zone,
    libelle varchar(1000),
    terminee boolean,
    responsable_id varchar(255),
    reunion_id varchar(255),
    primary key (id)
);

create table ba_reunion_participant (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    participant_externe varchar(255),
    employe_id varchar(255),
    reunion_id varchar(255),
    primary key (id)
);

create table ba_service (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    code varchar(255) unique,
    nom varchar(255),
    departement_id varchar(255),
    primary key (id)
);

create table circuit_etape (
    id varchar(255) not null,
    created_by varchar(50) not null,
    created_date timestamp(6) with time zone not null,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A' check (statut in ('A','D')),
    version bigint DEFAULT 1,
    actif boolean,
    conditions varchar(1500),
    delai_validation_jours integer,
    fonction_requise varchar(100),
    obligatoire boolean,
    ordre integer not null,
    type varchar(255) not null check (type in ('DEPARTEMENT','SERVICE')),
    circuit_id varchar(255) not null,
    departement_id varchar(255),
    role_id varchar(255),
    service_id varchar(255),
    primary key (id)
);

ALTER TABLE ba_agence ADD CONSTRAINT fk_ba_agence_departement_id FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE ba_autorisation_sortie ADD CONSTRAINT fk_ba_autorisation_sortie_agence_id FOREIGN KEY (agence_id) REFERENCES ba_agence(id);

ALTER TABLE ba_autorisation_sortie ADD CONSTRAINT fk_ba_autorisation_sortie_demandeur_id FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_autorisation_sortie ADD CONSTRAINT fk_ba_autorisation_sortie_direction_id FOREIGN KEY (direction_id) REFERENCES ba_departement(id);

ALTER TABLE ba_autorisation_sortie ADD CONSTRAINT fk_ba_autorisation_sortie_service_id FOREIGN KEY (service_id) REFERENCES ba_service(id);

ALTER TABLE ba_autorisation_sortie ADD CONSTRAINT fk_ba_autorisation_sortie_validateur_id FOREIGN KEY (validateur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_dga_pole_validateur ADD CONSTRAINT fk_ba_dga_pole_validateur_dga_id FOREIGN KEY (dga_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_document_reunion ADD CONSTRAINT fk_ba_document_reunion_reunion_id FOREIGN KEY (reunion_id) REFERENCES ba_reunion(id);

ALTER TABLE ba_fiche_habilitation_etape ADD CONSTRAINT fk_ba_fiche_habilitation_etape_fiche_id FOREIGN KEY (fiche_id) REFERENCES ba_fiche_habilitation(id);

ALTER TABLE ba_fiche_habilitation_etape ADD CONSTRAINT fk_ba_fiche_habilitation_etape_validateur_id FOREIGN KEY (validateur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_fiche_habilitation_plateforme ADD CONSTRAINT fk_ba_fiche_habilitation_plateforme_fiche_id FOREIGN KEY (fiche_id) REFERENCES ba_fiche_habilitation(id);

ALTER TABLE ba_fiche_habilitation_plateforme ADD CONSTRAINT fk_ba_fiche_habilitation_plateforme_plateforme_id FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id);

ALTER TABLE ba_habilitation_compte_plateforme ADD CONSTRAINT fk_ba_habilitation_compte_plateforme_cree_par_id FOREIGN KEY (cree_par_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_habilitation_compte_plateforme ADD CONSTRAINT fk_ba_habilitation_compte_plateforme_demandeur_id FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_habilitation_compte_plateforme ADD CONSTRAINT fk_ba_habilitation_compte_plateforme_fiche_id FOREIGN KEY (fiche_id) REFERENCES ba_fiche_habilitation(id);

ALTER TABLE ba_habilitation_compte_plateforme ADD CONSTRAINT fk_ba_habilitation_compte_plateforme_plateforme_id FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id);

ALTER TABLE ba_profils_roles ADD CONSTRAINT fk_ba_profils_roles_role_id FOREIGN KEY (role_id) REFERENCES ba_role(id);

ALTER TABLE ba_profils_roles ADD CONSTRAINT fk_ba_profils_roles_profil_id FOREIGN KEY (profil_id) REFERENCES ba_profil(id);

ALTER TABLE ba_reinitialisation_compte ADD CONSTRAINT fk_ba_reinitialisation_compte_circuit_id FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id);

ALTER TABLE ba_reinitialisation_compte ADD CONSTRAINT fk_ba_reinitialisation_compte_demandeur_id FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_reinitialisation_compte ADD CONSTRAINT fk_ba_reinitialisation_compte_plateforme_id FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id);

ALTER TABLE ba_reinitialisation_compte_etape ADD CONSTRAINT fk_ba_reinitialisation_compte_etape_demande_id FOREIGN KEY (demande_id) REFERENCES ba_reinitialisation_compte(id);

ALTER TABLE ba_reinitialisation_compte_etape ADD CONSTRAINT fk_ba_reinitialisation_compte_etape_validateur_id FOREIGN KEY (validateur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_reinitialisation_compte_traitement ADD CONSTRAINT fk_ba_reinitialisation_compte_traitement_demande_id FOREIGN KEY (demande_id) REFERENCES ba_reinitialisation_compte(id);

ALTER TABLE ba_reinitialisation_compte_traitement ADD CONSTRAINT fk_ba_reinitialisation_compte_traitement_traite_par_id FOREIGN KEY (traite_par_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_reprise_service ADD CONSTRAINT fk_ba_reprise_service_demandeur_id FOREIGN KEY (demandeur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_reprise_service ADD CONSTRAINT fk_ba_reprise_service_signataire_id FOREIGN KEY (signataire_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_user_roles ADD CONSTRAINT fk_ba_user_roles_role_id FOREIGN KEY (role_id) REFERENCES ba_role(id);

ALTER TABLE ba_user_roles ADD CONSTRAINT fk_ba_user_roles_user_id FOREIGN KEY (user_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_utilisateur ADD CONSTRAINT fk_ba_utilisateur_agence_id FOREIGN KEY (agence_id) REFERENCES ba_agence(id);

ALTER TABLE ba_utilisateur ADD CONSTRAINT fk_ba_utilisateur_departement_id FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE ba_utilisateur ADD CONSTRAINT fk_ba_utilisateur_profil_uuid FOREIGN KEY (profil_uuid) REFERENCES ba_profil(id);

ALTER TABLE ba_utilisateur ADD CONSTRAINT fk_ba_utilisateur_service_id FOREIGN KEY (service_id) REFERENCES ba_service(id);

ALTER TABLE ba_utilisateur ADD CONSTRAINT fk_ba_utilisateur_superieur_id FOREIGN KEY (superieur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_utilisateur ADD CONSTRAINT fk_ba_utilisateur_superieur_secondaire_id FOREIGN KEY (superieur_secondaire_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_validation_delegation ADD CONSTRAINT fk_ba_validation_delegation_agence_id FOREIGN KEY (agence_id) REFERENCES ba_agence(id);

ALTER TABLE ba_validation_delegation ADD CONSTRAINT fk_ba_validation_delegation_delegant_id FOREIGN KEY (delegant_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_validation_delegation ADD CONSTRAINT fk_ba_validation_delegation_delegue_id FOREIGN KEY (delegue_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_validation_delegation ADD CONSTRAINT fk_ba_validation_delegation_departement_id FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE ba_validation_delegation ADD CONSTRAINT fk_ba_validation_delegation_service_id FOREIGN KEY (service_id) REFERENCES ba_service(id);

ALTER TABLE ba_circuit ADD CONSTRAINT fk_ba_circuit_plateforme_id FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id);

ALTER TABLE ba_departement ADD CONSTRAINT fk_ba_departement_dga_validateur_id FOREIGN KEY (dga_validateur_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_departement ADD CONSTRAINT fk_ba_departement_parent_departement_id FOREIGN KEY (parent_departement_id) REFERENCES ba_departement(id);

ALTER TABLE ba_fiche_habilitation ADD CONSTRAINT fk_ba_fiche_habilitation_circuit_id FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id);

ALTER TABLE ba_fiche_habilitation ADD CONSTRAINT fk_ba_fiche_habilitation_employe_id FOREIGN KEY (employe_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_plateforme ADD CONSTRAINT fk_ba_plateforme_circuit_id FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id);

ALTER TABLE ba_reunion ADD CONSTRAINT fk_ba_reunion_agence_id FOREIGN KEY (agence_id) REFERENCES ba_agence(id);

ALTER TABLE ba_reunion ADD CONSTRAINT fk_ba_reunion_departement_id FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE ba_reunion ADD CONSTRAINT fk_ba_reunion_service_id FOREIGN KEY (service_id) REFERENCES ba_service(id);

ALTER TABLE ba_reunion_action ADD CONSTRAINT fk_ba_reunion_action_responsable_id FOREIGN KEY (responsable_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_reunion_action ADD CONSTRAINT fk_ba_reunion_action_reunion_id FOREIGN KEY (reunion_id) REFERENCES ba_reunion(id);

ALTER TABLE ba_reunion_participant ADD CONSTRAINT fk_ba_reunion_participant_employe_id FOREIGN KEY (employe_id) REFERENCES ba_utilisateur(id);

ALTER TABLE ba_reunion_participant ADD CONSTRAINT fk_ba_reunion_participant_reunion_id FOREIGN KEY (reunion_id) REFERENCES ba_reunion(id);

ALTER TABLE ba_service ADD CONSTRAINT fk_ba_service_departement_id FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE circuit_etape ADD CONSTRAINT fk_circuit_etape_circuit_id FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id);

ALTER TABLE circuit_etape ADD CONSTRAINT fk_circuit_etape_departement_id FOREIGN KEY (departement_id) REFERENCES ba_departement(id);

ALTER TABLE circuit_etape ADD CONSTRAINT fk_circuit_etape_role_id FOREIGN KEY (role_id) REFERENCES ba_role(id);

ALTER TABLE circuit_etape ADD CONSTRAINT fk_circuit_etape_service_id FOREIGN KEY (service_id) REFERENCES ba_service(id);
-- Table historique des modeles de circuits (V1.3).
CREATE TABLE ba_circuit_template (
    id VARCHAR(36) PRIMARY KEY,
    nom VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    complexite VARCHAR(20) NOT NULL CHECK (complexite IN ('SIMPLE', 'MOYENNE', 'COMPLEXE')),
    duree_estimee INTEGER,
    popularite INTEGER DEFAULT 0,
    configuration_json TEXT,
    tags VARCHAR(200),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Unicite metier introduite avant V1.20.
ALTER TABLE ba_habilitation_compte_plateforme
    ADD CONSTRAINT uq_hab_compte_fiche_plateforme UNIQUE (fiche_id, plateforme_id);
ALTER TABLE ba_fiche_habilitation_plateforme
    ADD CONSTRAINT uq_fiche_plateforme UNIQUE (fiche_id, plateforme_id);

-- Valeurs initiales des workflows.
ALTER TABLE ba_circuit ALTER COLUMN actif SET DEFAULT TRUE;
ALTER TABLE ba_circuit ALTER COLUMN statu SET DEFAULT 'EN_CONFIGURATION';
ALTER TABLE circuit_etape ALTER COLUMN actif SET DEFAULT TRUE;
ALTER TABLE circuit_etape ALTER COLUMN obligatoire SET DEFAULT TRUE;
ALTER TABLE ba_plateforme ALTER COLUMN type_plateforme SET DEFAULT 'RESEAUX';
ALTER TABLE ba_autorisation_sortie ALTER COLUMN statut_validation SET DEFAULT 'EN_ATTENTE';
ALTER TABLE ba_reprise_service ALTER COLUMN statut_validation SET DEFAULT 'EN_ATTENTE';
ALTER TABLE ba_habilitation_compte_plateforme ALTER COLUMN statut_creation SET DEFAULT 'A_CREER';
ALTER TABLE ba_reinitialisation_compte ALTER COLUMN statut_demande SET DEFAULT 'EN_COURS';
ALTER TABLE ba_reinitialisation_compte_etape ALTER COLUMN statut_validation SET DEFAULT 'EN_ATTENTE';
ALTER TABLE ba_reinitialisation_compte_traitement ALTER COLUMN statut_traitement SET DEFAULT 'A_TRAITER';
ALTER TABLE ba_validation_delegation ALTER COLUMN actif SET DEFAULT TRUE;
ALTER TABLE ba_dga_pole_validateur ALTER COLUMN actif SET DEFAULT TRUE;

-- Index historiques utiles aux recherches et au reporting.
CREATE INDEX idx_circuit_statu ON ba_circuit(statu);
CREATE INDEX idx_circuit_actif ON ba_circuit(actif);
CREATE INDEX idx_circuit_template_complexite ON ba_circuit_template(complexite);
CREATE INDEX idx_circuit_template_popularite ON ba_circuit_template(popularite DESC);
CREATE INDEX idx_plateforme_circuit_id ON ba_plateforme(circuit_id);
CREATE INDEX idx_aut_sortie_type_structure ON ba_autorisation_sortie(type_structure);
CREATE INDEX idx_aut_sortie_direction ON ba_autorisation_sortie(direction_id);
CREATE INDEX idx_aut_sortie_service ON ba_autorisation_sortie(service_id);
CREATE INDEX idx_aut_sortie_agence ON ba_autorisation_sortie(agence_id);
CREATE INDEX idx_ba_agence_departement ON ba_agence(departement_id);
CREATE INDEX idx_fiche_plateforme_fiche ON ba_fiche_habilitation_plateforme(fiche_id);
CREATE INDEX idx_fiche_plateforme_plateforme ON ba_fiche_habilitation_plateforme(plateforme_id);
CREATE INDEX idx_circuit_etape_circuit_ordre ON circuit_etape(circuit_id, ordre);
CREATE INDEX idx_circuit_etape_role ON circuit_etape(role_id);
CREATE INDEX idx_ba_departement_parent ON ba_departement(parent_departement_id);
CREATE INDEX idx_ba_departement_dga_pole ON ba_departement(dga_pole);
CREATE INDEX idx_validation_delegation_delegue ON ba_validation_delegation(delegue_id, actif, date_debut, date_fin);
CREATE INDEX idx_validation_delegation_role ON ba_validation_delegation(role_code);
CREATE INDEX idx_dga_pole_validateur_pole ON ba_dga_pole_validateur(pole, actif, date_debut, date_fin);

CREATE INDEX idx_hab_compte_plateforme_plateforme
    ON ba_habilitation_compte_plateforme (plateforme_id);

CREATE INDEX idx_hab_compte_plateforme_demandeur
    ON ba_habilitation_compte_plateforme (demandeur_id);

CREATE INDEX idx_hab_compte_plateforme_cree_par
    ON ba_habilitation_compte_plateforme (cree_par_id);

CREATE INDEX idx_hab_compte_plateforme_statut_creation
    ON ba_habilitation_compte_plateforme (statut_creation);

CREATE INDEX idx_hab_compte_plateforme_created_date
    ON ba_habilitation_compte_plateforme (created_date);

CREATE INDEX idx_reinit_compte_plateforme
    ON ba_reinitialisation_compte (plateforme_id);

CREATE INDEX idx_reinit_compte_demandeur
    ON ba_reinitialisation_compte (demandeur_id);

CREATE INDEX idx_reinit_compte_statut_demande
    ON ba_reinitialisation_compte (statut_demande);

CREATE INDEX idx_reinit_compte_created_date
    ON ba_reinitialisation_compte (created_date);

CREATE INDEX idx_reinit_traitement_statut
    ON ba_reinitialisation_compte_traitement (statut_traitement);

CREATE INDEX idx_reinit_traitement_traite_par
    ON ba_reinitialisation_compte_traitement (traite_par_id);

CREATE INDEX idx_autorisation_sortie_demandeur
    ON ba_autorisation_sortie (demandeur_id);

CREATE INDEX idx_autorisation_sortie_validateur
    ON ba_autorisation_sortie (validateur_id);

CREATE INDEX idx_autorisation_sortie_statut_validation
    ON ba_autorisation_sortie (statut_validation);

CREATE INDEX idx_autorisation_sortie_date_sortie
    ON ba_autorisation_sortie (date_sortie);

CREATE INDEX idx_autorisation_sortie_created_date
    ON ba_autorisation_sortie (created_date);

CREATE INDEX idx_reprise_service_demandeur
    ON ba_reprise_service (demandeur_id);

CREATE INDEX idx_reprise_service_signataire
    ON ba_reprise_service (signataire_id);

CREATE INDEX idx_reprise_service_statut_validation
    ON ba_reprise_service (statut_validation);

CREATE INDEX idx_reprise_service_date_reprise
    ON ba_reprise_service (date_reprise);

CREATE INDEX idx_reprise_service_created_date
    ON ba_reprise_service (created_date);
-- Roles techniques de base et roles ajoutes par V1.12 / V1.13.
-- Les identifiants fixes rendent l'initialisation des donnees deterministe.
INSERT INTO ba_role (id, code, libelle, created_by, created_date, statut, version)
VALUES
    ('role-ba-admin', 'BA_ADMIN', 'Administrateur', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
    ('role-ba-connect', 'BA_CONNECT', 'Connexion', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
    ('role-hab-creation-reseaux', 'HABILITATION_CREATION_COMPTE_RESEAUX', 'Creation comptes plateformes reseaux', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
    ('role-hab-creation-core-banking', 'HABILITATION_CREATION_COMPTE_CORE_BANKING', 'Creation comptes plateformes core banking', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
    ('role-hab-reinit-reseaux', 'HABILITATION_REINITIALISATION_COMPTE_RESEAUX', 'Reinitialisation comptes plateformes reseaux', 'flyway', CURRENT_TIMESTAMP, 'A', 1),
    ('role-hab-reinit-core-banking', 'HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING', 'Reinitialisation comptes plateformes core banking', 'flyway', CURRENT_TIMESTAMP, 'A', 1);

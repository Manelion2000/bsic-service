-- Table ba_role
INSERT INTO ba_role (created_by, created_date, last_modified_by, last_modified_date, id, code, libelle)
VALUES ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        '4ab',
        'BA_ADMIN', 'Administrateur'),
       ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        'e28',
        'BA_CONNECT', 'Connexion');

-- Table ba_profil
INSERT INTO ba_profil (statut, created_by, created_date, last_modified_by, last_modified_date, id, libelle)
VALUES ('A', 'yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        '1e', 'Administrateur'),
       ('A', 'yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        'ce9', 'Membre');

-- Table ba_profils_roles(admin)
INSERT INTO ba_profils_roles (profil_id, role_id)
VALUES ('1e', '4ab'),
       ('1e', 'e28');

-- Table ba_profils_roles(membre)
INSERT INTO ba_profils_roles (profil_id, role_id)
VALUES ('ce9', 'e28');


-- Utilisateurs
INSERT INTO ba_utilisateur (id, created_by, created_date, email, account_locked, nom, statut, password_hash, prenom,
                            telephone, username, profil_uuid, activated, sexe)
VALUES ('51a0379b-2f2f-4eaa-aad3-e41d88dc0db1', 'abakouan', '2021-02-05 15:16:06.0', 'abdramanbakouan@gmail.com',
        false, 'BAKOUAN', 'A',
        '$2a$10$5EykYe4F6osi7rrZEghdVueK.McdyIGjHnbjYJIUiIHkVb7EGMPES', 'ABDRAMAN', '+22672808708', 'abakouan',
        '1e', true, 'MASCULIN');

-- Table ba_departement
INSERT INTO ba_departement (created_by, created_date, last_modified_by, last_modified_date, statut, version, id, code, nom)
VALUES ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309', 'A', 1,
        'DEP-001', 'DIT', 'Departement de Informatique et de la Technologie'),
       ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309', 'A', 1,
        'DEP-002', 'DRHA', 'Departement Ressources Humaines');

-- Table ba_service (rattache a un departement)
INSERT INTO ba_service (created_by, created_date, last_modified_by, last_modified_date, statut, version, id, code, nom, departement_id)
VALUES ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309', 'A', 1,
        'SER-001', 'DBA', 'Service Base de Données', 'DEP-001'),
       ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309', 'A', 1,
        'SER-002', 'SER_GC', 'Service gestion des Carrière', 'DEP-002');

-- Table ba_agence
INSERT INTO ba_agence (created_by, created_date, last_modified_by, last_modified_date, statut, version, id, code, nom, adresse, telephone, telephone1)
VALUES ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309', 'A', 1,
        'AG-001', 'AG_OUAGA_P', 'SIEGE', 'Ouagadougou, Avenue Koulouba', '+22625328401', '+22625302122');
VALUES ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309', 'A', 1,
        'AG-001', 'AG_BOBO', 'SIEGE', 'Bobo_Dsso, secteur 25', '+22625328404', '+22625302123');

-- Migration: circuits (nouveau) et suppression plateforme_etape_config
DROP TABLE IF EXISTS ba_plateforme_etape_config;

CREATE TABLE IF NOT EXISTS ba_circuit (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    code VARCHAR(50) UNIQUE,
    libelle VARCHAR(150),
    description VARCHAR(2000),
    plateforme_id VARCHAR(255),
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    statut VARCHAR(5),
    version INTEGER,
    CONSTRAINT fk_circuit_plateforme FOREIGN KEY (plateforme_id) REFERENCES ba_plateforme(id)
);

CREATE TABLE IF NOT EXISTS ba_circuit_etape_config (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    circuit_id VARCHAR(255),
    etape_definition_id VARCHAR(255),
    ordre INTEGER,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    statut VARCHAR(5),
    version INTEGER,
    CONSTRAINT fk_circuit_etape_circuit FOREIGN KEY (circuit_id) REFERENCES ba_circuit(id),
    CONSTRAINT fk_circuit_etape_definition FOREIGN KEY (etape_definition_id) REFERENCES ba_etape_definition(id)
);

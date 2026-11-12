CREATE TABLE ba_plateforme_manuel (
    id varchar(255) PRIMARY KEY,
    plateforme_id varchar(255) NOT NULL REFERENCES ba_plateforme(id),
    titre varchar(150) NOT NULL,
    description varchar(2000),
    nom_fichier varchar(150) NOT NULL,
    content_type varchar(100) NOT NULL,
    taille bigint NOT NULL CHECK (taille>0 AND taille<=10485760),
    sha256 varchar(64) NOT NULL,
    auteur varchar(255) NOT NULL,
    contenu bytea NOT NULL,
    created_by varchar(50) NOT NULL,
    created_date timestamp(6) with time zone NOT NULL,
    last_modified_by varchar(50),
    last_modified_date timestamp(6) with time zone,
    statut varchar(255) DEFAULT 'A',
    version bigint DEFAULT 1
);
CREATE INDEX idx_plateforme_manuel_liste ON ba_plateforme_manuel(plateforme_id,statut,created_date,id);
CREATE UNIQUE INDEX idx_plateforme_manuel_contenu_actif ON ba_plateforme_manuel(plateforme_id,sha256) WHERE statut='A';
-- Fichiers privés, inclus dans les sauvegardes PostgreSQL ; aucune URL publique.

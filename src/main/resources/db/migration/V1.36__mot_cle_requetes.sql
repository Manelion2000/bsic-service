-- Aucun mot clé n'est inventé pour les requêtes historiques.
ALTER TABLE ba_requete_oracle
    ADD COLUMN mot_cle varchar(200) NOT NULL DEFAULT '';

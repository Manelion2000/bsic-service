ALTER TABLE ba_utilisateur ADD COLUMN credential_version bigint NOT NULL DEFAULT 0;
ALTER TABLE ba_utilisateur ADD COLUMN password_change_required boolean NOT NULL DEFAULT false;
ALTER TABLE ba_utilisateur ADD COLUMN temporary_password_used boolean NOT NULL DEFAULT false;
ALTER TABLE ba_utilisateur ADD COLUMN temporary_password_expires_at timestamp(6) with time zone;
-- Les comptes existants conservent leur mot de passe et leur parcours de connexion.
-- Les anciennes clés de récupération par mail ne permettent plus de changer un mot de passe.
UPDATE ba_utilisateur SET reset_key=NULL,reset_date=NULL WHERE reset_key IS NOT NULL OR reset_date IS NOT NULL;

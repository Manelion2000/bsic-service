package com.bakouan.app.security;

/**
 * Constants for Spring Security authorities.
 */
public final class BaRolesConstants {

    /**
     * L'administrateur de la plateforme.
     */
    public static final String BA_ADMIN = "BA_ADMIN";

    /**
     * Tout utilisateur disposant d'un compte(role commun).
     */
    public static final String BA_CONNECT = "BA_CONNECT";

    public static final String HABILITATION_CREATION_COMPTE_RESEAUX = "HABILITATION_CREATION_COMPTE_RESEAUX";

    public static final String HABILITATION_CREATION_COMPTE_CORE_BANKING = "HABILITATION_CREATION_COMPTE_CORE_BANKING";

    public static final String HABILITATION_REINITIALISATION_COMPTE_RESEAUX = "HABILITATION_REINITIALISATION_COMPTE_RESEAUX";

    public static final String HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING = "HABILITATION_REINITIALISATION_COMPTE_CORE_BANKING";

    private BaRolesConstants() {
    }


}

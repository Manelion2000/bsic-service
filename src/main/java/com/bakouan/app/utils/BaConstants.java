package com.bakouan.app.utils;

import org.springframework.core.io.ClassPathResource;

public class BaConstants {
    public static final String DEFAULT_USER = "dev@user";
    public static final String ROLE_PREFIX = "BA_";

    /**
     * Constantes des URLs.
     */
    public static class URL {
        public static final  String BASE_URL = "/api";
        public static final String PROFIL = "/profils";
        public static final String ROLE = "/roles";
        public static final String DOCUMENT = "/documents";
        public static final String CSRF_TOKEN = "/csrf";
        public static final String AUTHENTICATE = "/authenticate";
        public static final String LOGOUT = "/logout";
        public static final String USER = "/users";
        public static final String ANNUAIRE = "/annuaires";
        public static final String ANNUAIRE_LIST = "/annuaire";
        public static final String SERVICE = "/services";
        public static final String DEPARTEMENT = "/departements";
        public static final String AGENCE = "/agences";
        public static final String HABILITATION = "/habilitations";
        public static final String HABILITATION_ETAPES_DEF = "/etapes";
        public static final String HABILITATION_MES_DEMANDES = "/mes-demandes";
        public static final String HABILITATION_MES_VALIDATIONS = "/mes-validations";
        public static final String PLATEFORME = "/plateformes";
        public static final String FICHE_HABILITATION = "/fiches";
        public static final String FICHE_HABILITATION_ETAPE = "/fiches-etapes";
        public static final String FICHE_HABILITATION_ETAPES_INIT_CIRCUIT = "/fiches/{id}/etapes-from-circuit";
        public static final String REUNION = "/reunions";
        public static final String REUNION_PARTICIPANT = "/reunions-participants";
        public static final String REUNION_ACTION = "/reunions-actions";
        public static final String CIRCUIT_V2 = "/circuits";
        public static final String CIRCUIT_V2_ACTIFS = "/circuits/actifs";
        public static final String CIRCUIT_V2_ETAPES = "/circuits/{id}/etapes";
        public static final String AUTORISATION_SORTIE = "/autorisation-sorties";
        public static final String AUTORISATION_SORTIE_MES_DEMANDES = "/autorisation-sorties/mes-demandes";
        public static final String AUTORISATION_SORTIE_A_VALIDER = "/autorisation-sorties/a-valider";
        public static final String REPRISE_SERVICE = "/reprises-service";
        public static final String REPRISE_SERVICE_MES_DEMANDES = "/reprises-service/mes-demandes";
        public static final String REPRISE_SERVICE_A_VALIDER = "/reprises-service/a-valider";
    }

    /**
     * Classes des constantes liées à l'édition.
     */
    public static class REPORTS {
        public static final String LOGO_URL = new ClassPathResource("/images/logo.png").getPath();
        public static final String PARAM_TITLE = "BA_TITLE";

        /**
         * Racines des reports.
         */
        private static final String REPORT_ROOT = "reports/";
        public static final String REPORT_FICHE_HABILITATION = REPORT_ROOT + "fiche-habilitation.jrxml";
        public static final String REPORT_AUTORISATION_SORTIE = REPORT_ROOT + "autorisation-sortie.jrxml";
        public static final String REPORT_ATTESTATION_REPRISE_SERVICE = REPORT_ROOT + "attestation-reprise-service.jrxml";
    }

}

package com.bakouan.app.repositories;

import com.bakouan.app.dto.SuiviDemandesDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository @RequiredArgsConstructor
public class SuiviDemandesRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String BASE_FROM = """
         FROM ba_suivi_demande v JOIN ba_utilisateur u ON u.id=v.demandeur_id
         LEFT JOIN ba_service s ON s.id=u.service_id
         LEFT JOIN ba_departement dep ON dep.id=COALESCE(u.departement_id,s.departement_id)
         LEFT JOIN ba_rh_circuit c ON c.id=v.circuit_id
         LEFT JOIN ba_extraction_demande x ON v.module='EXTRACTION' AND x.id=v.id
        """;
    private static final String FROM = BASE_FROM + """
         LEFT JOIN LATERAL (
           SELECT count(*) AS total, count(*) FILTER (WHERE etat='APPROUVEE') AS validees,
                  min(ordre) FILTER (WHERE etat='EN_ATTENTE') AS courante
           FROM (
             SELECT 1 AS ordre, v.etat WHERE v.module='REPRISE'
             UNION ALL SELECT 1,v.etat FROM ba_autorisation_absence a WHERE v.module='ABSENCE' AND a.id=v.id AND a.validateur_id IS NOT NULL
             UNION ALL SELECT 1,v.etat FROM ba_autorisation_sortie a WHERE v.module='SORTIE' AND a.id=v.id AND a.validateur_id IS NOT NULL
             UNION ALL SELECT ordre,CASE statut_validation WHEN 'VALIDE' THEN 'APPROUVEE' WHEN 'REJETE' THEN 'REJETEE' ELSE statut_validation END
               FROM ba_fiche_habilitation_etape WHERE v.module='HABILITATION' AND fiche_id=v.id AND statut='A'
             UNION ALL SELECT ordre,CASE statut_validation WHEN 'VALIDE' THEN 'APPROUVEE' WHEN 'REJETE' THEN 'REJETEE' ELSE statut_validation END
               FROM ba_reinitialisation_compte_etape WHERE v.module='REINITIALISATION' AND demande_id=v.id AND statut='A'
           ) historique
         ) h ON c.id IS NULL
        """;
    private static final String SCOPE = """
         WHERE (:departement='' OR dep.id=:departement)
         AND (:habilitations OR v.module NOT IN ('HABILITATION','REINITIALISATION'))
        """;
    private static final String FILTER = """
         AND (:module='' OR v.module=:module) AND (:etat='' OR v.etat=:etat)
         AND (:recherche='' OR lower(concat_ws(' ',u.prenom,u.nom,u.matricule,v.id)) LIKE :recherche ESCAPE '\\')
        """;
    private static final String SELECT = """
         SELECT v.*, concat_ws(' ',u.prenom,u.nom) AS agent, dep.id AS departement_id,
                COALESCE(dep.nom,'Sans département') AS departement, COALESCE(s.nom,'Sans service') AS service,
                COALESCE(jsonb_array_length(c.etapes),h.total,0) AS etapes_total,
                CASE WHEN c.id IS NOT NULL THEN (SELECT count(*) FROM jsonb_array_elements(c.etapes) e
                     WHERE e->>'statut'='VALIDEE') ELSE COALESCE(h.validees,0) END AS etapes_validees,
                CASE WHEN v.module='EXTRACTION' THEN CASE x.etat WHEN 'NOUVELLE' THEN 'Nouvelle demande' WHEN 'EN_COURS' THEN 'Traitement informatique' ELSE NULL END
                     WHEN v.etat='EN_ATTENTE' THEN COALESCE(c.etapes->c.etape_courante->>'libelle',
                     CASE WHEN h.courante IS NOT NULL THEN CASE WHEN v.module='REPRISE' THEN 'Validation de la reprise'
                     ELSE 'Validation '||h.courante END END) END AS etape_active
        """;
    private static final RowMapper<Demande> ROW = (r,n) -> {
        Timestamp submitted=r.getTimestamp("soumise_le");
        return new Demande(r.getString("module"),r.getString("id"),r.getString("agent"),r.getString("departement_id"),
                r.getString("departement"),r.getString("service"),r.getString("etat"),submitted==null?null:submitted.toInstant(),
                r.getObject("debut",java.time.LocalDate.class),r.getObject("fin",java.time.LocalDate.class),
                r.getString("etape_active"),r.getInt("etapes_validees"),r.getInt("etapes_total"));
    };
    private MapSqlParameterSource scope(String department, boolean habilitations) {
        return new MapSqlParameterSource("departement",department==null?"":department).addValue("habilitations",habilitations);
    }
    public Page list(String department,boolean habilitations,String module,String state,String search,int page,int size) {
        String escaped=search.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
        var p=scope(department,habilitations).addValue("module",module).addValue("etat",state)
                .addValue("recherche",search.isEmpty()?"":"%"+escaped.toLowerCase(java.util.Locale.ROOT)+"%")
                .addValue("offset",(long)page*size).addValue("limit",size);
        long total=jdbc.queryForObject("SELECT count(*)"+BASE_FROM+SCOPE+FILTER,p,Long.class);
        List<Demande> rows=jdbc.query(SELECT+FROM+SCOPE+FILTER+" ORDER BY v.soumise_le DESC,v.module,v.id LIMIT :limit OFFSET :offset",p,ROW);
        return new Page(rows,total,page,size,(total+size-1)/size);
    }
    public Optional<Demande> detail(String department,boolean habilitations,String module,String id) {
        return jdbc.query(SELECT+FROM+SCOPE+" AND v.module=:module AND v.id=:id",scope(department,habilitations)
                .addValue("module",module).addValue("id",id),ROW).stream().findFirst();
    }
    public List<Structure> departments(String department) {
        return jdbc.query("SELECT id,nom FROM ba_departement WHERE (:departement='' OR id=:departement) AND statut='A' ORDER BY nom,id",
                scope(department,false),(r,n)->new Structure(r.getString("id"),r.getString("nom")));
    }
    public Optional<String> circuitId(String module,String id) {
        return jdbc.query("SELECT circuit_id FROM ba_suivi_demande WHERE module=:module AND id=:id",
                new MapSqlParameterSource("module",module).addValue("id",id),(r,n)->r.getString(1)).stream().filter(java.util.Objects::nonNull).findFirst();
    }
    /** Les anciens modules n'ont pas de BaRhCircuit : lire uniquement leurs étapes réellement enregistrées. */
    public List<LegacyStep> legacySteps(String module,String id) {
        String query=switch(module) {
            case "REPRISE" -> "SELECT 1 AS ordre,statut_validation AS etat,signataire_id AS titulaire,date_validation FROM ba_reprise_service WHERE id=:id";
            case "ABSENCE" -> "SELECT 1 AS ordre,statut_validation AS etat,validateur_id AS titulaire,date_validation FROM ba_autorisation_absence WHERE id=:id AND validateur_id IS NOT NULL";
            case "SORTIE" -> "SELECT 1 AS ordre,statut_validation AS etat,validateur_id AS titulaire,date_validation FROM ba_autorisation_sortie WHERE id=:id AND validateur_id IS NOT NULL";
            case "HABILITATION" -> "SELECT ordre,statut_validation AS etat,validateur_id AS titulaire,date_validation FROM ba_fiche_habilitation_etape WHERE fiche_id=:id AND statut='A'";
            case "REINITIALISATION" -> "SELECT ordre,statut_validation AS etat,validateur_id AS titulaire,date_validation FROM ba_reinitialisation_compte_etape WHERE demande_id=:id AND statut='A'";
            default -> null;
        };
        if(query==null)return List.of();
        return jdbc.query("SELECT e.*,concat_ws(' ',u.prenom,u.nom) AS nom FROM ("+query+") e LEFT JOIN ba_utilisateur u ON u.id=e.titulaire ORDER BY e.ordre,e.titulaire",
                new MapSqlParameterSource("id",id),(r,n)->new LegacyStep(r.getInt("ordre"),r.getString("etat"),r.getString("titulaire"),r.getString("nom"),r.getTimestamp("date_validation")));
    }
    public record LegacyStep(int ordre,String etat,String titulaire,String nom,Timestamp date) {}
}

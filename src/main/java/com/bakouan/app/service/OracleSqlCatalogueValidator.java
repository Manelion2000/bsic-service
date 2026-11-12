package com.bakouan.app.service;

import com.bakouan.app.dto.ExtractionDtos.Parametre;
import org.springframework.stereotype.Component;
import java.util.*;
import static com.bakouan.app.service.CongeSecurity.bad;

/** Contrôle du catalogue, sans analyse/exécution Oracle. Ce filtre n'est pas un moteur SQL. */
@Component
public class OracleSqlCatalogueValidator {
    public void validate(String sql,List<Parametre> parameters) {
        var binds=validateSyntax(sql);var declared=new HashSet<String>();
        for(var p:parameters)if(!declared.add(p.nom().toLowerCase(Locale.ROOT)))throw bad("Les noms de paramètres doivent être distincts.");
        if(!binds.equals(declared))throw bad("Décrivez exactement les paramètres :nom présents dans le SQL.");
    }
    public Set<String> validateSyntax(String sql) {
        var tokens=new ArrayList<String>();var binds=new HashSet<String>();int n=sql.length();boolean ended=false;
        for(int i=0;i<n;) {
            char c=sql.charAt(i);
            if(Character.isWhitespace(c)){i++;continue;}
            if(c=='-'&&i+1<n&&sql.charAt(i+1)=='-'){i+=2;while(i<n&&sql.charAt(i)!='\n'&&sql.charAt(i)!='\r')i++;continue;}
            if(c=='/'&&i+1<n&&sql.charAt(i+1)=='*'){int end=sql.indexOf("*/",i+2);if(end<0)throw bad("Commentaire SQL non terminé.");i=end+2;continue;}
            if(ended)throw bad("Enregistrez une seule requête d'extraction.");
            if(c==';'){ended=true;i++;continue;}
            if((c=='q'||c=='Q')&&i+2<n&&sql.charAt(i+1)=='\'') {
                char open=sql.charAt(i+2),close=switch(open){case '['->']';case '('->')';case '{'->'}';case '<'->'>';default->open;};
                int end=sql.indexOf(""+close+'\'',i+3);if(end<0)throw bad("Littéral SQL non terminé.");i=end+2;continue;
            }
            if(c=='\''||c=='"') {
                char quote=c;boolean closed=false;i++;
                while(i<n){if(sql.charAt(i++)==quote){if(i<n&&sql.charAt(i)==quote)i++;else{closed=true;break;}}}
                if(!closed)throw bad("Littéral ou identifiant SQL non terminé.");continue;
            }
            if(c==':') {
                int start=++i;while(i<n&&(Character.isLetterOrDigit(sql.charAt(i))||sql.charAt(i)=='_'))i++;
                String bind=sql.substring(start,i);if(!bind.matches("[A-Za-z][A-Za-z0-9_]{0,29}"))throw bad("Utilisez des paramètres nommés :nom_parametre.");
                binds.add(bind.toLowerCase(Locale.ROOT));continue;
            }
            if(Character.isLetter(c)||c=='_') {
                int start=i++;while(i<n&&(Character.isLetterOrDigit(sql.charAt(i))||"_$#".indexOf(sql.charAt(i))>=0))i++;
                tokens.add(sql.substring(start,i).toUpperCase(Locale.ROOT));continue;
            }
            i++;
        }
        if(tokens.isEmpty()||!Set.of("SELECT","WITH").contains(tokens.get(0))||!tokens.contains("SELECT"))throw bad("Le catalogue accepte des extractions SELECT, éventuellement avec WITH.");
        var forbidden=Set.of("INSERT","UPDATE","DELETE","MERGE","CREATE","ALTER","DROP","TRUNCATE","GRANT","REVOKE","EXECUTE","EXEC","BEGIN","DECLARE","COMMIT","ROLLBACK","LOCK","INTO","CALL","PROCEDURE","FUNCTION");
        if(tokens.stream().anyMatch(forbidden::contains))throw bad("Les scripts d'écriture, PL/SQL et SELECT FOR UPDATE ne sont pas admis dans ce catalogue d'extraction.");
        return Set.copyOf(binds);
    }
}

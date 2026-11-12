package com.bakouan.app.service;

import com.bakouan.app.dto.ExtractionDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class OracleSqlCatalogueValidatorTest {
    private final OracleSqlCatalogueValidator validator=new OracleSqlCatalogueValidator();
    @Test void selectCteAndNamedParameters(){validator.validate("WITH lignes AS (SELECT * FROM operations WHERE jour >= :date_debut) SELECT * FROM lignes; -- fin",List.of(new Parametre("date_debut",TypeParametre.DATE,"Date incluse")));}
    @Test void keywordsInCommentsLiteralsAndQuotedIdentifiersAreNotStatements(){validator.validate("/* DROP :fake */ SELECT 'UPDATE; :fake', q'[DELETE '' :fake]', \"UPDATE\" FROM dual -- INSERT",List.of());}
    @Test void writesLocksPlsqlAndStackedStatementsRejected(){for(String s:List.of("DELETE FROM comptes","SELECT * FROM comptes FOR UPDATE","WITH f AS (SELECT 1 FROM dual) DELETE FROM comptes","BEGIN null; END;","SELECT 1 INTO x FROM dual","SELECT 1 FROM dual; SELECT 2 FROM dual","WITH FUNCTION f RETURN NUMBER IS BEGIN RETURN 1; END; SELECT f FROM dual"))assertThatThrownBy(()->validator.validate(s,List.of())).isInstanceOf(ResponseStatusException.class);}
    @Test void parametersAreExactAndDistinct(){assertThatThrownBy(()->validator.validate("SELECT :date_debut FROM dual",List.of())).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->validator.validate("SELECT 1 FROM dual",List.of(new Parametre("unused",TypeParametre.TEXTE,"Inutilisé")))).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->validator.validate("SELECT :x FROM dual",List.of(new Parametre("x",TypeParametre.TEXTE,"Un"),new Parametre("X",TypeParametre.TEXTE,"Deux")))).isInstanceOf(ResponseStatusException.class);}
    @Test void incompleteTextAndPositionalBindingsRejected(){for(String s:List.of("/* comment","SELECT 'non termine FROM dual","SELECT q'[non termine FROM dual","SELECT :1 FROM dual",";SELECT 1 FROM dual","-- SELECT"))assertThatThrownBy(()->validator.validate(s,List.of())).isInstanceOf(ResponseStatusException.class);}
}

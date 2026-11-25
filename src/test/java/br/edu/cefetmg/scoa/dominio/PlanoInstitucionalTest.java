package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanoInstitucionalTest {
    @Test void selecaoExigePeriodosValidosUnicosEPreservaOrdemSemCompartilharLista() {
        var periodos=new ArrayList<>(List.of(2L,1L)); var selecao=new Selecao(1,periodos,List.of()); periodos.clear();
        assertEquals(List.of(2L,1L),selecao.periodos());
        assertThrows(IllegalArgumentException.class,()->new Selecao(1,List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Selecao(1,List.of(2L,2L),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Selecao(1,List.of(2L),List.of(3L,3L)));
    }
    @Test void registroExigePreviaVersaoEMotivoSemPermitirAssinaturaArbitraria() {
        var selecao=new Selecao(1,List.of(2L),List.of());
        assertThrows(IllegalArgumentException.class,()->new Registro(selecao,-1,"a".repeat(64),"Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Registro(selecao,0,"sem prévia","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Registro(selecao,0,"a".repeat(64)," "));
    }
}

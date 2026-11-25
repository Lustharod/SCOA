package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.RematriculaLote.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RematriculaLoteTest {
    @Test void selecaoPreservaPopulacaoSemDuplicidadesOuPeriodosIguais() {
        var ids=new ArrayList<>(List.of(7L,8L)); var s=new Selecao(1,2,3,ids); ids.clear(); assertEquals(List.of(7L,8L),s.matriculas());
        assertThrows(IllegalArgumentException.class,()->new Selecao(1,2,2,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Selecao(1,2,3,List.of(7L,7L)));
    }
    @Test void pedidoExigeChavePreviaEMotivo() {
        var s=new Selecao(1,2,3,List.of());
        assertThrows(IllegalArgumentException.class,()->new Pedido(s," ","a".repeat(64),"Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Pedido(s,"chave","sem prévia","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Pedido(s,"chave","a".repeat(64)," "));
    }
}

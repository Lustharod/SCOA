package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.persistencia.SegurancaJdbc;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SegurancaTest {
    @Test void salAleatorioEVerificacaoSemArmazenarSenha() {
        char[] senha="Fixture-unica-012345".toCharArray();
        String a=SegurancaJdbc.codificar(senha),b=SegurancaJdbc.codificar(senha);
        assertNotEquals(a,b); assertFalse(a.contains(new String(senha)));
        assertTrue(SegurancaJdbc.confere(senha,a)); assertFalse(SegurancaJdbc.confere("Incorreta-012345".toCharArray(),a));
    }
    @Test void hashLegadoOuMalformadoNaoAutentica() {
        for(String hash:new String[]{"demo","scoa1$1$x$x","scoa1$600000$x$x",""}) assertFalse(SegurancaJdbc.confere("senha".toCharArray(),hash));
        assertFalse(SegurancaJdbc.confere(null,null));
    }
    @Test void limitesDeSenha() {
        assertThrows(IllegalArgumentException.class,()->SegurancaJdbc.codificar("curta".toCharArray()));
        assertThrows(IllegalArgumentException.class,()->SegurancaJdbc.codificar(new char[257]));
    }
}

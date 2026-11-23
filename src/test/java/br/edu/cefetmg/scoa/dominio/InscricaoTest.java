package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class InscricaoTest {
    @ParameterizedTest @CsvSource({"7,7,,APROVADO","8,6,,APROVADO","6,6,,AGUARDA_PF","6,6,4,APROVADO","6,6,3,REPROVADO","6.9,7,,APROVADO","6,6,3.9,APROVADO"})
    void notas(String p1,String p2,String pf,String esperado) {
        assertEquals(esperado,RegraResultado.calcular(new BigDecimal(p1),new BigDecimal(p2),pf==null?null:new BigDecimal(pf),400,300).situacao());
    }
    @ParameterizedTest @ValueSource(strings={"-0.1","10.1","6.95","100"})
    void notasInvalidas(String nota) { assertThrows(IllegalArgumentException.class,()->RegraResultado.nota(new BigDecimal(nota))); }
    @Test void frequenciaExataSemArredondamento() {
        assertEquals("REPROVADO",RegraResultado.calcular(BigDecimal.TEN,BigDecimal.TEN,null,1000,749).situacao());
        assertEquals("APROVADO",RegraResultado.calcular(BigDecimal.TEN,BigDecimal.TEN,null,1000,750).situacao());
        assertThrows(IllegalArgumentException.class,()->RegraResultado.calcular(BigDecimal.TEN,BigDecimal.TEN,null,0,0));
    }
    @Test void naoAceitaPfDesnecessariaOuNotaAusente() {
        assertThrows(IllegalArgumentException.class,()->RegraResultado.calcular(BigDecimal.TEN,BigDecimal.TEN,BigDecimal.TEN,100,100));
        assertThrows(IllegalArgumentException.class,()->RegraResultado.calcular(null,BigDecimal.TEN,null,100,100));
    }
    @Test void frequenciaSemOverflow() {
        assertEquals("APROVADO",RegraResultado.calcular(BigDecimal.TEN,BigDecimal.TEN,null,Long.MAX_VALUE,Long.MAX_VALUE).situacao());
    }
    @ParameterizedTest @CsvSource({"-15,false","-14,true","-8,true","-7,false","0,false"})
    void limitesJanela(int deslocamento,boolean esperado) {
        var inicio=LocalDate.of(2027,2,16); var j=JanelaInscricao.doPeriodo(inicio);
        assertEquals(esperado,j.permite(inicio.plusDays(deslocamento)));
    }
    @Test void janelaAnoBissexto() {
        var j=JanelaInscricao.doPeriodo(LocalDate.of(2028,3,7));
        assertEquals(LocalDate.of(2028,2,22),j.abertura()); assertEquals(LocalDate.of(2028,2,29),j.fimExclusivo());
    }
    @ParameterizedTest @CsvSource({"6,false","7,true","13,true","14,false"})
    void segundaFaseDuraSeteDias(int deslocamento,boolean aberta) {
        var inicio=LocalDate.of(2027,2,16);
        assertEquals(aberta,JanelaInscricao.segundaFase(inicio).permite(inicio.plusDays(deslocamento)));
    }
    @ParameterizedTest @CsvSource({"13,true,true","14,false,true","45,false,true","46,false,false"})
    void cancelamentoTrocaDeFluxoNoDiaQuatorzeEAdmitePedidoAteDiaQuarentaECinco(
            int dia,boolean imediato,boolean admitePedido) {
        var inicio=LocalDate.of(2027,2,16);
        assertEquals(imediato,CancelamentoInscricao.imediato(inicio,inicio.plusDays(dia)));
        assertEquals(admitePedido,CancelamentoInscricao.permitePedido(inicio,inicio.plusDays(90),inicio.plusDays(dia)));
    }
    @Test void retificacaoRespeitaSeteDiasEInicioDaPrimeiraFase() {
        var fim=LocalDate.of(2027,6,20);
        assertFalse(RetificacaoResultado.noPrazoDoProfessor(fim,fim));
        assertTrue(RetificacaoResultado.noPrazoDoProfessor(fim,fim.plusDays(1)));
        assertTrue(RetificacaoResultado.noPrazoDoProfessor(fim,fim.plusDays(7)));
        assertFalse(RetificacaoResultado.noPrazoDoProfessor(fim,fim.plusDays(8)));
        var seguinte=fim.plusDays(30);
        assertTrue(RetificacaoResultado.antesDaInscricaoSeguinte(seguinte,seguinte.minusDays(15)));
        assertFalse(RetificacaoResultado.antesDaInscricaoSeguinte(seguinte,seguinte.minusDays(14)));
    }
}

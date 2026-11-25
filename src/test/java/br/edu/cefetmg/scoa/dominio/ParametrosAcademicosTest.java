package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;

class ParametrosAcademicosTest {
    @Test void basePreservaJanelasEPrazosAprovados() {
        var r=Regras.baseAprovada(); var inicio=LocalDate.of(2026,1,20); var fim=inicio.plusDays(100);
        assertEquals(JanelaInscricao.doPeriodo(inicio),r.primeiraFase(inicio)); assertEquals(JanelaInscricao.segundaFase(inicio),r.segundaFase(inicio));
        for(int d=-20;d<110;d++) {
            var data=inicio.plusDays(d);
            assertEquals(CancelamentoInscricao.imediato(inicio,data),r.cancelamentoImediato(inicio,data));
            assertEquals(CancelamentoInscricao.permitePedido(inicio,fim,data),r.permiteCancelamento(inicio,fim,data));
            assertEquals(TransferenciaInterna.noPrazo(inicio,fim,data),r.permiteTransferencia(inicio,fim,data));
            assertEquals(RetificacaoResultado.noPrazoDoProfessor(fim,data),r.permiteRetificacao(fim,data));
        }
    }
    @ParameterizedTest @CsvSource({"75,7,5,75,7,,APROVADO","80,7,5,75,8,,REPROVADO","75,8,5,100,7,5,APROVADO","75,8,7,100,7,5,REPROVADO"})
    void limiaresVariamSemMudarFormula(String freq,String dispensa,String finalMin,long presentes,String prova,String pf,String esperado) {
        var r=new Regras(14,7,7,7,13,45,45,7,15,new BigDecimal(freq),new BigDecimal(dispensa),new BigDecimal(finalMin));
        var n=new BigDecimal(prova); var resultado=RegraResultado.calcular(n,n,pf==null?null:new BigDecimal(pf),100,presentes,r);
        assertEquals(esperado,resultado.situacao());
    }
    @Test void atrasoIncluiLimiteSemAlterarTemposECarga() {
        assertEquals(TempoLetivo.Presenca.PRESENTE,TempoLetivo.M1.presencaPorChegada(LocalTime.of(7,10),10));
        assertEquals(TempoLetivo.Presenca.FALTA,TempoLetivo.M1.presencaPorChegada(LocalTime.of(7,10,1),10));
        assertEquals(TempoLetivo.Presenca.SEM_REGISTRO,TempoLetivo.M1.presencaPorChegada(null,10));
        assertEquals(72,DiarioAcademico.temposDaCarga(60));
    }
    @Test void rejeitaConfiguracoesIncoerentesSemArredondarValores() {
        assertThrows(IllegalArgumentException.class,()->new Regras(3,7,7,7,13,45,45,7,15,new BigDecimal("75"),new BigDecimal("7"),new BigDecimal("5")));
        assertThrows(IllegalArgumentException.class,()->new Regras(14,7,7,7,46,45,45,7,15,new BigDecimal("75"),new BigDecimal("7"),new BigDecimal("5")));
        assertThrows(IllegalArgumentException.class,()->new Regras(14,7,7,7,13,45,45,7,50,new BigDecimal("75"),new BigDecimal("7"),new BigDecimal("5")));
        assertThrows(IllegalArgumentException.class,()->new Regras(14,7,7,7,13,45,45,7,15,new BigDecimal("75.01"),new BigDecimal("7"),new BigDecimal("5")));
        assertThrows(IllegalArgumentException.class,()->new Regras(14,7,7,7,13,45,45,7,15,BigDecimal.ZERO,new BigDecimal("7"),new BigDecimal("5")));
        assertThrows(IllegalArgumentException.class,()->new Regras(14,7,7,7,13,45,45,7,15,new BigDecimal("75"),new BigDecimal("5"),new BigDecimal("7")));
    }
}

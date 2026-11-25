package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import java.time.*;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiarioAcademicoTest {
    @Test void cargaCadastradaUsaHorasDeSessentaMinutosSemAlterarOValorOriginal() {
        assertEquals(3600,DiarioAcademico.minutosDaCarga(60)); assertEquals(72,DiarioAcademico.temposDaCarga(60));
        assertEquals(36,DiarioAcademico.temposDaCarga(30)); assertEquals(108,DiarioAcademico.temposDaCarga(90));
        assertThrows(IllegalArgumentException.class,()->DiarioAcademico.temposDaCarga(1));
        assertThrows(IllegalArgumentException.class,()->DiarioAcademico.minutosDaCarga(0));
    }
    @Test void notaMantemEscalaAprovadaEVersionamentoNaoAdmiteValoresInvalidos() {
        assertThrows(IllegalArgumentException.class,()->new LancarNota(1,1,new BigDecimal("10.1"),-1,"Motivo"));
        assertThrows(IllegalArgumentException.class,()->new LancarNota(1,1,new BigDecimal("7.55"),-1,"Motivo"));
        assertThrows(IllegalArgumentException.class,()->new LancarNota(1,1,BigDecimal.TEN,-2,"Motivo"));
        assertThrows(IllegalArgumentException.class,()->new LancarFrequencia(1,1,TempoLetivo.Presenca.PRESENTE,0," "));
    }
    @Test void aulasAgrupadasPreservamTemposUnicosEEntradaImutavel() {
        var lista=new java.util.ArrayList<>(List.of(TempoLetivo.M1,TempoLetivo.M2));
        var pedido=new NovaAula(1,LocalDate.of(2026,11,25),lista,"Pauta","Conteúdo");
        lista.clear(); assertEquals(2,pedido.tempos().size());
        assertThrows(IllegalArgumentException.class,()->new NovaAula(1,LocalDate.now(),List.of(TempoLetivo.M1,TempoLetivo.M1),"Pauta","Conteúdo"));
    }
    @Test void eventoSimuladoUsaPrecisaoDoArmazenamentoSemAlterarSuaDataLocal() {
        var data=LocalDateTime.of(2026,11,25,7,15,0,123456789);
        var evento=new Chegada("Simulador","E-1",1,1,data);
        assertEquals(123456000,evento.instante().getNano()); assertEquals(data.toLocalDate(),evento.instante().toLocalDate());
    }
}

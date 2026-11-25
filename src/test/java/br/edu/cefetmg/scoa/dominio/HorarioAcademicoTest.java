package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HorarioAcademicoTest {
    @Test void horarioPodeExistirSemSalaSemUsarIdZero() {
        var h=new HorarioAula(null,1,(Long)null,new FaixaHorario(1,TempoLetivo.M1.inicio(),TempoLetivo.M1.fim()));
        assertNull(h.idSala());
        assertThrows(IllegalArgumentException.class,()->new HorarioAula(null,1,0,h.faixa()));
    }
    @Test void planejamentoUsaTemposDistintosEDiaValidoSemArredondarIntervalos() {
        var p=new HorarioAcademico.Planejar(1,1,List.of(TempoLetivo.M3,TempoLetivo.M4),"Motivo");
        assertEquals(100,TempoLetivo.minutosLetivos(p.tempos()));
        assertThrows(IllegalArgumentException.class,()->new HorarioAcademico.Planejar(1,0,p.tempos(),"Motivo"));
        assertThrows(IllegalArgumentException.class,()->new HorarioAcademico.Planejar(1,1,List.of(TempoLetivo.M1,TempoLetivo.M1),"Motivo"));
    }
}

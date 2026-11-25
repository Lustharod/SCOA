package br.edu.cefetmg.scoa.dominio;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.dominio.TempoLetivo.*;

class TempoLetivoTest {
    @ParameterizedTest @EnumSource(TempoLetivo.class)
    void cadaTempoTemCinquentaMinutosEToleranciaDeQuinzeInclusiva(TempoLetivo tempo) {
        assertEquals(50,Duration.between(tempo.inicio(),tempo.fim()).toMinutes());
        assertEquals(Presenca.PRESENTE,tempo.presencaPorChegada(tempo.inicio()));
        assertEquals(Presenca.PRESENTE,tempo.presencaPorChegada(tempo.inicio().plusMinutes(15)));
        assertEquals(Presenca.FALTA,tempo.presencaPorChegada(tempo.inicio().plusMinutes(15).plusSeconds(1)));
        assertEquals(Presenca.SEM_REGISTRO,tempo.presencaPorChegada(null));
    }
    @Test void turnosTemSeisSeisECincoTemposSemContarIntervalos() {
        assertEquals(6,doTurno(Turno.MANHA).size());
        assertEquals(6,doTurno(Turno.TARDE).size());
        assertEquals(5,doTurno(Turno.NOITE).size());
        assertEquals(300,minutosLetivos(doTurno(Turno.MANHA)));
        assertEquals(300,minutosLetivos(doTurno(Turno.TARDE)));
        assertEquals(250,minutosLetivos(doTurno(Turno.NOITE)));
        assertEquals(20,Duration.between(M3.fim(),M4.inicio()).toMinutes());
        assertEquals(20,Duration.between(T3.fim(),T4.inicio()).toMinutes());
        assertEquals(20,Duration.between(N3.fim(),N4.inicio()).toMinutes());
        assertEquals(LocalTime.of(22,30),N5.fim());
    }
    @Test void encontroPodeOcuparDoisQuatroOuOTurnoSemIncluirPausas() {
        assertEquals(100,minutosLetivos(List.of(M1,M2)));
        assertEquals(200,minutosLetivos(List.of(M1,M2,M3,M4)));
        assertThrows(IllegalArgumentException.class,()->minutosLetivos(List.of(M1,M1)));
        assertThrows(IllegalArgumentException.class,()->minutosLetivos(List.of()));
    }
    @Test void atrasoNoPrimeiroTempoNaoViraFaltaAutomaticaNosSeguintes() {
        assertEquals(Presenca.FALTA,M1.presencaPorChegada(LocalTime.of(7,16)));
        assertEquals(Presenca.PRESENTE,M2.presencaPorChegada(LocalTime.of(7,50)));
    }
}

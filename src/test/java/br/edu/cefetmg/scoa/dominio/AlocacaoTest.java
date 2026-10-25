package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AlocacaoTest {
    private static FaixaHorario faixa(int dia, int inicio, int fim) {
        return new FaixaHorario(dia, LocalTime.of(inicio, 0), LocalTime.of(fim, 0));
    }
    private static PeriodoLetivo periodo(String inicio, String fim) {
        return new PeriodoLetivo(LocalDate.parse(inicio), LocalDate.parse(fim));
    }
    private final PeriodoLetivo semestre = periodo("2026-08-01", "2026-12-20");
    private final TurmaResumo turma = new TurmaResumo(1L, "ES-01", 30, semestre);

    @Test void capacidadeIgualADaTurmaEPermitida() {
        assertDoesNotThrow(() -> RegraAlocacao.validarSala(Sala.nova("101", "A", 30), turma));
    }
    @Test void salaMenorQueTurmaERejeitada() {
        assertThrows(IllegalArgumentException.class, () -> RegraAlocacao.validarSala(Sala.nova("101", "A", 29), turma));
    }
    @Test void salaInativaERejeitada() {
        assertThrows(IllegalArgumentException.class, () -> RegraAlocacao.validarSala(new Sala(1L,"101","A",40,"INATIVO"), turma));
    }
    @ParameterizedTest @ValueSource(ints = {0, 8, -1})
    void diaInvalidoERejeitado(int dia) {
        assertThrows(IllegalArgumentException.class, () -> faixa(dia, 8, 10));
    }
    @Test void intervaloInvalidoERejeitado() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> faixa(1, 10, 10)),
                () -> assertThrows(IllegalArgumentException.class, () -> faixa(1, 10, 8)),
                () -> assertThrows(NullPointerException.class, () -> new FaixaHorario(1, null, LocalTime.NOON)),
                () -> assertThrows(IllegalArgumentException.class, () -> new FaixaHorario(1, LocalTime.of(8,0,0,1), LocalTime.NOON)));
    }
    @Test void horariosAdjacentesNaoConflitam() {
        assertFalse(RegraAlocacao.conflita(faixa(1,8,10), semestre, faixa(1,10,12), semestre));
    }
    @Test void diasDiferentesNaoConflitam() {
        assertFalse(RegraAlocacao.conflita(faixa(1,8,10), semestre, faixa(2,8,10), semestre));
    }
    @Test void intersecaoEContencaoDeHorariosConflitam() {
        assertTrue(RegraAlocacao.conflita(faixa(1,8,10), semestre, faixa(1,9,11), semestre));
        assertTrue(RegraAlocacao.conflita(faixa(1,8,12), semestre, faixa(1,9,10), semestre));
    }
    @Test void periodosDistintosNaoConflitam() {
        assertFalse(RegraAlocacao.conflita(faixa(1,8,10), semestre, faixa(1,8,10), periodo("2027-02-01","2027-06-30")));
    }
    @Test void periodosComIdsDiferentesMasDatasSobrepostasConflitam() {
        assertTrue(RegraAlocacao.conflita(faixa(1,8,10), semestre, faixa(1,8,10), periodo("2026-10-01","2027-02-01")));
    }
    @Test void intersecaoDeDatasSemODiaEscolhidoNaoConflita() {
        // A única data comum é domingo; o horário pedido é de segunda-feira.
        assertFalse(RegraAlocacao.conflita(faixa(1,8,10), periodo("2026-10-19","2026-10-25"),
                faixa(1,8,10), periodo("2026-10-25","2026-10-31")));
    }
    @Test void diaSemOcorrenciaNoPeriodoERejeitado() {
        TurmaResumo domingo = new TurmaResumo(1,"T",30,periodo("2026-10-25","2026-10-25"));
        assertThrows(IllegalArgumentException.class, () -> RegraAlocacao.validarDia(domingo, faixa(1,8,10)));
    }
    @Test void datasInvalidasEIdsInvalidosSaoRejeitados() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> periodo("2026-10-25","2026-10-24")),
                () -> assertThrows(IllegalArgumentException.class, () -> new HorarioAula(null,0,1,faixa(1,8,10))),
                () -> assertThrows(IllegalArgumentException.class, () -> new HorarioAula(null,1,4_294_967_296L,faixa(1,8,10))));
    }
}

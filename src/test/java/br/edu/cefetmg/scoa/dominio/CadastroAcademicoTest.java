package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CadastroAcademicoTest {
    private static final PeriodoLetivo DATAS=new PeriodoLetivo(LocalDate.of(2027,2,1),LocalDate.of(2027,6,30));
    @Test void cadastraPeriodoETurmaSemIdERemoveEspacos() {
        var periodo=new CadastroPeriodoLetivo(null,"  2027/1  ",DATAS,"ATIVO");
        var turma=new Turma(null,1,2,3,"  ES-01  ",30,"ATIVO");
        assertNull(periodo.id()); assertNull(turma.id());
        assertEquals("2027/1",periodo.descricao()); assertEquals("ES-01",turma.codigo());
    }
    @ParameterizedTest @ValueSource(strings={"", "  "})
    void camposObrigatorios(String vazio) {
        assertAll(() -> assertThrows(IllegalArgumentException.class,() -> new CadastroPeriodoLetivo(null,vazio,DATAS,"ATIVO")),
                () -> assertThrows(IllegalArgumentException.class,() -> new Turma(null,1,2,3,vazio,30,"ATIVO")),
                () -> assertThrows(IllegalArgumentException.class,() -> new Turma(null,1,2,3,"T",30,vazio)));
    }
    @Test void limitesDeTextoEUnicode() {
        assertDoesNotThrow(() -> new CadastroPeriodoLetivo(null,"📚".repeat(50),DATAS,"ATIVO"));
        assertDoesNotThrow(() -> new Turma(null,1,2,3,"A".repeat(40),30,"ATIVO"));
        assertThrows(IllegalArgumentException.class,() -> new CadastroPeriodoLetivo(null,"A".repeat(51),DATAS,"ATIVO"));
        assertThrows(IllegalArgumentException.class,() -> new Turma(null,1,2,3,"A".repeat(41),30,"ATIVO"));
        assertThrows(IllegalArgumentException.class,() -> new CadastroPeriodoLetivo(null,"P",DATAS,"A".repeat(21)));
    }
    @ParameterizedTest @ValueSource(ints={0,-1,65536})
    void capacidadeInvalida(int capacidade) {
        assertThrows(IllegalArgumentException.class,() -> new Turma(null,1,2,3,"T",capacidade,"ATIVO"));
    }
    @Test void idsInvalidos() {
        assertAll(() -> assertThrows(IllegalArgumentException.class,() -> new Turma(null,0,2,3,"T",30,"ATIVO")),
                () -> assertThrows(IllegalArgumentException.class,() -> new Turma(null,1,-1,3,"T",30,"ATIVO")),
                () -> assertThrows(IllegalArgumentException.class,() -> new Turma(null,1,2,4_294_967_296L,"T",30,"ATIVO")),
                () -> assertThrows(IllegalArgumentException.class,() -> new CadastroPeriodoLetivo(0L,"P",DATAS,"ATIVO")));
    }
    @Test void datasForaDoTipoDateSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,() -> new CadastroPeriodoLetivo(null,"P",
                new PeriodoLetivo(LocalDate.of(999,1,1),LocalDate.of(999,12,31)),"ATIVO"));
        assertThrows(IllegalArgumentException.class,() -> new CadastroPeriodoLetivo(null,"P",
                new PeriodoLetivo(LocalDate.of(9999,1,1),LocalDate.of(10000,1,1)),"ATIVO"));
    }
}

package br.edu.cefetmg.scoa.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ProfessoresDisciplinasTest {
    @Test void p0401ProfessorNormalizaPreservaIdsEUnicode() {
        var p = new Professor(4_294_967_295L, 4_294_967_295L, "  Docente  ", " ATIVO ");
        assertEquals("Docente", p.identificacao()); assertEquals("ATIVO", p.situacao());
        assertEquals(4_294_967_295L, p.id()); assertEquals(4_294_967_295L, p.idUsuario());
        assertEquals(100, new Professor(null, null, "📚".repeat(100), "ATIVO").identificacao().codePointCount(0,200));
    }
    @Test void p0401ProfessorRejeitaAusenciaExcessoEIdsInvalidos() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(null,null,null,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(null,null,"  ","ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(null,null,"A".repeat(101),"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(null,null,"A","")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(null,null,"A","X".repeat(21))),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(0L,null,"A","ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(null,-1L,"A","ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Professor(4_294_967_296L,null,"A","ATIVO")));
    }
    @ParameterizedTest @ValueSource(ints={1,60,65535})
    void p0405DisciplinaAceitaLimites(int carga) {
        var d = new Disciplina(4_294_967_295L," C "," Nome ",carga," ATIVO ");
        assertEquals("C",d.codigo()); assertEquals("Nome",d.nome()); assertEquals(carga,d.cargaHoraria());
        assertDoesNotThrow(() -> new Disciplina(null,"📚".repeat(30),"📚".repeat(150),carga,"ATIVO"));
    }
    @ParameterizedTest @ValueSource(ints={-1,0,65536})
    void p0405DisciplinaRecusaCargaInvalida(int carga) {
        assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"C","Nome",carga,"ATIVO"));
    }
    @Test void p0405DisciplinaRecusaDadosInvalidos() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,null,"Nome",60,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"  ","Nome",60,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"C"," ",60,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"C".repeat(31),"Nome",60,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"C","N".repeat(151),60,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(0L,"C","Nome",60,"ATIVO")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"C","Nome",60,"")),
            () -> assertThrows(IllegalArgumentException.class, () -> new Disciplina(null,"C","Nome",60,"S".repeat(21))));
    }
}

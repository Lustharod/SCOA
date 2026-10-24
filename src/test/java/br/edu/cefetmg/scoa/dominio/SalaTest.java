package br.edu.cefetmg.scoa.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SalaTest {
    @ParameterizedTest(name = "CT-JU-SALA-001 — capacidade válida {0}")
    @ValueSource(ints = {1, 30, 65_535})
    void aceitaCapacidadeDentroDosLimites(int capacidade) {
        Sala sala = Sala.nova("Sala 101", "Bloco A", capacidade);
        assertEquals(capacidade, sala.getCapacidade());
        assertNull(sala.getId());
        assertEquals("ATIVO", sala.getSituacao());
    }

    @ParameterizedTest(name = "CT-JU-SALA-002 — capacidade inválida {0}")
    @ValueSource(ints = {-1, 0, 65_536})
    void rejeitaCapacidadeForaDosLimites(int capacidade) {
        assertThrows(IllegalArgumentException.class, () -> Sala.nova("Sala 101", "Bloco A", capacidade));
    }

    @ParameterizedTest(name = "CT-JU-SALA-003 — identificação obrigatória")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejeitaIdentificacaoAusente(String identificacao) {
        assertThrows(IllegalArgumentException.class, () -> Sala.nova(identificacao, "Bloco A", 30));
    }

    @ParameterizedTest(name = "CT-JU-SALA-004 — localização obrigatória")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaLocalizacaoAusente(String localizacao) {
        assertThrows(IllegalArgumentException.class, () -> Sala.nova("Sala 101", localizacao, 30));
    }

    @Test
    void ctJuSala005RejeitaTextosQueExcedemOsCamposDoBanco() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> Sala.nova("A".repeat(51), "Bloco A", 30)),
                () -> assertThrows(IllegalArgumentException.class, () -> Sala.nova("Sala 101", "A".repeat(151), 30)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Sala(1L, "Sala 101", "Bloco A", 30, "A".repeat(21)))
        );
    }

    @Test
    void ctJuSala006AceitaLimitesDeTextoECaracteresUnicode() {
        Sala sala = new Sala(1L, "📚".repeat(50), "A".repeat(150), 30, "A".repeat(20));
        assertEquals(50, sala.getIdentificacao().codePointCount(0, sala.getIdentificacao().length()));
    }

    @Test
    void ctJuSala007RemoveEspacosDasExtremidades() {
        Sala sala = Sala.nova("  Sala 101  ", " Bloco A ", 30);
        assertEquals("Sala 101", sala.getIdentificacao());
        assertEquals("Bloco A", sala.getLocalizacao());
    }

    @ParameterizedTest(name = "CT-JU-SALA-008 — ID inválido {0}")
    @ValueSource(longs = {-1L, 0L, 4_294_967_296L})
    void rejeitaIdForaDoIntervaloDoBanco(long id) {
        assertThrows(IllegalArgumentException.class, () -> new Sala(id, "Sala 101", "Bloco A", 30, "ATIVO"));
    }

    @Test
    void ctJuSala009PreservaIdAcimaDoLimiteDeIntDoJava() {
        Sala sala = new Sala(4_294_967_295L, "Sala 101", "Bloco A", 30, "ATIVO");
        assertEquals(4_294_967_295L, sala.getId());
    }

    @ParameterizedTest(name = "CT-JU-SALA-010 — situação obrigatória")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaSituacaoAusente(String situacao) {
        assertThrows(IllegalArgumentException.class, () -> new Sala(1L, "Sala 101", "Bloco A", 30, situacao));
    }
}

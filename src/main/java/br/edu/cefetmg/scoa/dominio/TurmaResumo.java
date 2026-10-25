package br.edu.cefetmg.scoa.dominio;

import java.util.Objects;

/** Projeção das informações de turma necessárias à alocação, sem substituir seu cadastro completo. */
public record TurmaResumo(long id, String codigo, int capacidade, PeriodoLetivo periodo) {
    public TurmaResumo {
        HorarioAula.validarId(id);
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Informe o código da turma.");
        if (capacidade < 1 || capacidade > 65_535) throw new IllegalArgumentException("A capacidade da turma é inválida.");
        Objects.requireNonNull(periodo, "Informe o período da turma.");
    }
}

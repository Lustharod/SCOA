package br.edu.cefetmg.scoa.dominio;

import java.util.Objects;

public record HorarioAula(Long id, long idTurma, long idSala, FaixaHorario faixa) {
    public HorarioAula {
        if (id != null) validarId(id);
        validarId(idTurma);
        validarId(idSala);
        Objects.requireNonNull(faixa, "Informe o horário.");
    }

    public static void validarId(long id) {
        if (id < 1 || id > 4_294_967_295L) throw new IllegalArgumentException("O identificador informado é inválido.");
    }
}

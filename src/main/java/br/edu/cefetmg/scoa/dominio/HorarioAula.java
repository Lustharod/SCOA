package br.edu.cefetmg.scoa.dominio;

import java.util.Objects;

public record HorarioAula(Long id, long idTurma, Long idSala, FaixaHorario faixa) {
    public HorarioAula(Long id,long idTurma,int idSala,FaixaHorario faixa) {
        this(id,idTurma,Long.valueOf(idSala),faixa);
    }
    public HorarioAula {
        if (id != null) validarId(id);
        validarId(idTurma);
        if(idSala!=null) validarId(idSala);
        Objects.requireNonNull(faixa, "Informe o horário.");
    }

    public static void validarId(long id) {
        if (id < 1 || id > 4_294_967_295L) throw new IllegalArgumentException("O identificador informado é inválido.");
    }
}

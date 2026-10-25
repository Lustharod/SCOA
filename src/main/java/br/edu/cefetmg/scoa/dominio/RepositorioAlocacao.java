package br.edu.cefetmg.scoa.dominio;

import java.util.List;

public interface RepositorioAlocacao {
    List<TurmaResumo> listarTurmas();
    TurmaResumo buscarTurma(long idTurma);
    List<HorarioAula> listarHorarios(long idTurma);
    List<Sala> listarDisponiveis(long idTurma, FaixaHorario faixa);
    /** Verifica sala, turma e conflitos dentro da mesma transação da inclusão. */
    HorarioAula alocar(HorarioAula horario);
    void desalocar(long idHorario);
}

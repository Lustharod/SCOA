package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface RepositorioCadastroAcademico {
    CadastroPeriodoLetivo inserirPeriodo(CadastroPeriodoLetivo periodo);
    Optional<CadastroPeriodoLetivo> buscarPeriodo(long id);
    List<CadastroPeriodoLetivo> listarPeriodos();
    CadastroPeriodoLetivo alterarPeriodo(long id, UnaryOperator<CadastroPeriodoLetivo> alteracao);
    Turma inserirTurma(Turma turma);
    Optional<Turma> buscarTurma(long id);
    List<Turma> listarTurmas();
    Turma alterarTurma(long id, UnaryOperator<Turma> alteracao);
    List<ReferenciaAcademica> listarDisciplinas();
    List<ReferenciaAcademica> listarProfessores();
}

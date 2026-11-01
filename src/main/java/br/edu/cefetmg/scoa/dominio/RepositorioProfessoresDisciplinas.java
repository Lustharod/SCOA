package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface RepositorioProfessoresDisciplinas {
    Professor inserirProfessor(Professor professor);
    Optional<Professor> buscarProfessor(long id);
    List<Professor> listarProfessores();
    Professor alterarProfessor(long id, UnaryOperator<Professor> alteracao);
    Disciplina inserirDisciplina(Disciplina disciplina);
    Optional<Disciplina> buscarDisciplina(long id);
    List<Disciplina> listarDisciplinas();
    Disciplina alterarDisciplina(long id, UnaryOperator<Disciplina> alteracao);
    List<Disciplina> listarPreRequisitos(long disciplina);
    void adicionarPreRequisito(long disciplina, long requisito);
    void removerPreRequisito(long disciplina, long requisito);
}

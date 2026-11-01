package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface RepositorioBaseAcademica {
    Aluno inserirAluno(Aluno aluno);
    Optional<Aluno> buscarAluno(long id);
    List<Aluno> listarAlunos();
    Aluno alterarAluno(long id, UnaryOperator<Aluno> alteracao);
    Curso inserirCurso(Curso curso);
    Optional<Curso> buscarCurso(long id);
    List<Curso> listarCursos();
    Curso alterarCurso(long id, UnaryOperator<Curso> alteracao);
    EstruturaCurricular inserirEstrutura(EstruturaCurricular estrutura);
    Optional<EstruturaCurricular> buscarEstrutura(long id);
    List<EstruturaCurricular> listarEstruturas(long curso);
    EstruturaCurricular alterarEstrutura(long id, UnaryOperator<EstruturaCurricular> alteracao);
    List<Disciplina> listarComponentes(long estrutura);
    void adicionarComponente(long estrutura, long disciplina);
    void removerComponente(long estrutura, long disciplina);
}

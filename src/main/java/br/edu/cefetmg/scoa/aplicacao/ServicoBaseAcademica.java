package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import java.util.List;
import java.util.Objects;

public final class ServicoBaseAcademica {
    private final RepositorioBaseAcademica repositorio;
    public ServicoBaseAcademica(RepositorioBaseAcademica repositorio) { this.repositorio = Objects.requireNonNull(repositorio); }
    public Aluno cadastrarAluno(String identificacao) { return repositorio.inserirAluno(new Aluno(null, null, identificacao, "ATIVO")); }
    public Aluno buscarAluno(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarAluno(id).orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado."));
    }
    public List<Aluno> listarAlunos() { return List.copyOf(repositorio.listarAlunos()); }
    public Aluno atualizarAluno(long id, String identificacao) {
        HorarioAula.validarId(id);
        return repositorio.alterarAluno(id, a -> new Aluno(a.id(), a.idUsuario(), identificacao, a.situacao()));
    }
    public Aluno inativarAluno(long id) {
        HorarioAula.validarId(id);
        return repositorio.alterarAluno(id, a -> new Aluno(a.id(), a.idUsuario(), a.identificacao(), "INATIVO"));
    }
    public Curso cadastrarCurso(String nome) { return repositorio.inserirCurso(new Curso(null, nome, "ATIVO")); }
    public Curso buscarCurso(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarCurso(id).orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
    }
    public List<Curso> listarCursos() { return List.copyOf(repositorio.listarCursos()); }
    public Curso atualizarCurso(long id, String nome) {
        HorarioAula.validarId(id);
        return repositorio.alterarCurso(id, c -> new Curso(c.id(), nome, c.situacao()));
    }
    public EstruturaCurricular cadastrarEstrutura(long curso, String identificacao) {
        return repositorio.inserirEstrutura(new EstruturaCurricular(null, curso, identificacao, "ATIVO"));
    }
    public EstruturaCurricular buscarEstrutura(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarEstrutura(id).orElseThrow(() -> new IllegalArgumentException("Estrutura curricular não encontrada."));
    }
    public List<EstruturaCurricular> listarEstruturas(long curso) {
        buscarCurso(curso);
        return List.copyOf(repositorio.listarEstruturas(curso));
    }
    public EstruturaCurricular atualizarEstrutura(long id, String identificacao) {
        HorarioAula.validarId(id);
        return repositorio.alterarEstrutura(id, e -> new EstruturaCurricular(e.id(), e.idCurso(), identificacao, e.situacao()));
    }
    public List<Disciplina> listarComponentes(long estrutura) {
        buscarEstrutura(estrutura);
        return List.copyOf(repositorio.listarComponentes(estrutura));
    }
    public void adicionarComponente(long estrutura, long disciplina) {
        HorarioAula.validarId(estrutura); HorarioAula.validarId(disciplina);
        repositorio.adicionarComponente(estrutura, disciplina);
    }
    public void removerComponente(long estrutura, long disciplina) {
        HorarioAula.validarId(estrutura); HorarioAula.validarId(disciplina);
        repositorio.removerComponente(estrutura, disciplina);
    }
}

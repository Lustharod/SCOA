package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import java.util.List;
import java.util.Objects;

public final class ServicoProfessoresDisciplinas {
    private final RepositorioProfessoresDisciplinas repositorio;
    public ServicoProfessoresDisciplinas(RepositorioProfessoresDisciplinas repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }
    public Professor cadastrarProfessor(String identificacao) {
        return repositorio.inserirProfessor(new Professor(null, null, identificacao, "ATIVO"));
    }
    public Professor buscarProfessor(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarProfessor(id).orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));
    }
    public List<Professor> listarProfessores() { return List.copyOf(repositorio.listarProfessores()); }
    public Professor atualizarProfessor(long id, String identificacao) {
        HorarioAula.validarId(id);
        return repositorio.alterarProfessor(id, p -> new Professor(id, p.idUsuario(), identificacao, p.situacao()));
    }
    public Professor inativarProfessor(long id) {
        HorarioAula.validarId(id);
        return repositorio.alterarProfessor(id, p -> new Professor(id, p.idUsuario(), p.identificacao(), "INATIVO"));
    }
    public Disciplina cadastrarDisciplina(String codigo, String nome, int cargaHoraria) {
        return repositorio.inserirDisciplina(new Disciplina(null, codigo, nome, cargaHoraria, "ATIVO"));
    }
    public Disciplina buscarDisciplina(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarDisciplina(id).orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada."));
    }
    public List<Disciplina> listarDisciplinas() { return List.copyOf(repositorio.listarDisciplinas()); }
    public Disciplina atualizarDisciplina(long id, String codigo, String nome, int cargaHoraria) {
        HorarioAula.validarId(id);
        return repositorio.alterarDisciplina(id, d -> new Disciplina(id, codigo, nome, cargaHoraria, d.situacao()));
    }
    public List<Disciplina> listarPreRequisitos(long disciplina) {
        HorarioAula.validarId(disciplina);
        return List.copyOf(repositorio.listarPreRequisitos(disciplina));
    }
    public void adicionarPreRequisito(long disciplina, long requisito) {
        validarRelacao(disciplina, requisito);
        repositorio.adicionarPreRequisito(disciplina, requisito);
    }
    public void removerPreRequisito(long disciplina, long requisito) {
        validarRelacao(disciplina, requisito);
        repositorio.removerPreRequisito(disciplina, requisito);
    }
    private static void validarRelacao(long disciplina, long requisito) {
        HorarioAula.validarId(disciplina); HorarioAula.validarId(requisito);
        if (disciplina == requisito) throw new IllegalArgumentException("Uma disciplina não pode ser pré-requisito de si mesma.");
    }
}

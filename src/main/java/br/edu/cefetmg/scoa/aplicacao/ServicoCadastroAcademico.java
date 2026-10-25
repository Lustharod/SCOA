package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.CadastroPeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.PeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.ReferenciaAcademica;
import br.edu.cefetmg.scoa.dominio.RepositorioCadastroAcademico;
import br.edu.cefetmg.scoa.dominio.Turma;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class ServicoCadastroAcademico {
    private final RepositorioCadastroAcademico repositorio;
    public ServicoCadastroAcademico(RepositorioCadastroAcademico repositorio) {
        this.repositorio=Objects.requireNonNull(repositorio,"Informe o repositório acadêmico.");
    }
    public CadastroPeriodoLetivo cadastrarPeriodo(String descricao,LocalDate inicio,LocalDate fim) {
        return repositorio.inserirPeriodo(new CadastroPeriodoLetivo(null,descricao,new PeriodoLetivo(inicio,fim),"ATIVO"));
    }
    public CadastroPeriodoLetivo buscarPeriodo(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarPeriodo(id).orElseThrow(() -> new IllegalArgumentException("Período letivo não encontrado."));
    }
    public List<CadastroPeriodoLetivo> listarPeriodos() { return List.copyOf(repositorio.listarPeriodos()); }
    public CadastroPeriodoLetivo atualizarPeriodo(long id,String descricao,LocalDate inicio,LocalDate fim) {
        HorarioAula.validarId(id);
        return repositorio.alterarPeriodo(id,atual -> new CadastroPeriodoLetivo(id,descricao,new PeriodoLetivo(inicio,fim),atual.situacao()));
    }
    public CadastroPeriodoLetivo inativarPeriodo(long id) {
        HorarioAula.validarId(id);
        return repositorio.alterarPeriodo(id,atual -> new CadastroPeriodoLetivo(id,atual.descricao(),atual.datas(),"INATIVO"));
    }
    public Turma cadastrarTurma(long disciplina,long professor,long periodo,String codigo,int capacidade) {
        return repositorio.inserirTurma(new Turma(null,disciplina,professor,periodo,codigo,capacidade,"ATIVO"));
    }
    public Turma buscarTurma(long id) {
        HorarioAula.validarId(id);
        return repositorio.buscarTurma(id).orElseThrow(() -> new IllegalArgumentException("Turma não encontrada."));
    }
    public List<Turma> listarTurmas() { return List.copyOf(repositorio.listarTurmas()); }
    public Turma atualizarTurma(long id,long disciplina,long professor,long periodo,String codigo,int capacidade) {
        HorarioAula.validarId(id);
        return repositorio.alterarTurma(id,atual -> new Turma(id,disciplina,professor,periodo,codigo,capacidade,atual.situacao()));
    }
    public Turma inativarTurma(long id) {
        HorarioAula.validarId(id);
        return repositorio.alterarTurma(id,atual -> new Turma(id,atual.idDisciplina(),atual.idProfessor(),atual.idPeriodo(),
                atual.codigo(),atual.capacidade(),"INATIVO"));
    }
    public List<ReferenciaAcademica> listarDisciplinas() { return List.copyOf(repositorio.listarDisciplinas()); }
    public List<ReferenciaAcademica> listarProfessores() { return List.copyOf(repositorio.listarProfessores()); }
}

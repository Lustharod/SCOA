package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;
import br.edu.cefetmg.scoa.dominio.Planejamento.Periodo;
import br.edu.cefetmg.scoa.dominio.Planejamento.Resultado;
import java.sql.Connection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.UnaryOperator;

/** Adaptadores usados pela aplicação. Repositórios JDBC básicos conservam as regras de persistência. */
public final class RepositoriosAcademicosAutorizados {
    private RepositoriosAcademicosAutorizados() { }

    public static RepositorioSala salas(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioSalaJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioSala() {
            @Override public Sala salvar(Sala sala) { return acesso.manter("RepositorioSala","salvar",0,() -> repo.salvar(sala)); }
            @Override public Sala alterar(long id, UnaryOperator<Sala> modificacao) { return acesso.manter("RepositorioSala","alterar",id,() -> repo.alterar(id,modificacao)); }
            @Override public Optional<Sala> buscarPorId(long id) { return acesso.consultar(() -> repo.buscarPorId(id)); }
            @Override public List<Sala> listar() { return acesso.consultar(() -> repo.listar()); }
        };
    }

    public static RepositorioAlocacao alocacoes(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioAlocacaoJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioAlocacao() {
            @Override public List<TurmaResumo> listarTurmas() { return acesso.consultar(() -> repo.listarTurmas()); }
            @Override public TurmaResumo buscarTurma(long idTurma) { return acesso.consultar(() -> repo.buscarTurma(idTurma)); }
            @Override public List<HorarioAula> listarHorarios(long idTurma) { return acesso.consultar(() -> repo.listarHorarios(idTurma)); }
            @Override public List<Sala> listarDisponiveis(long idTurma, FaixaHorario faixa) { return acesso.consultar(() -> repo.listarDisponiveis(idTurma,faixa)); }
            @Override public HorarioAula alocar(HorarioAula horario) { return acesso.manter("RepositorioAlocacao","alocar",0,() -> repo.alocar(horario)); }
            @Override public void desalocar(long idHorario) { acesso.manter("RepositorioAlocacao","desalocar",idHorario,() -> { repo.desalocar(idHorario); return null; }); }
        };
    }

    public static RepositorioCadastroAcademico cadastros(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioCadastroAcademicoJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioCadastroAcademico() {
            @Override public CadastroPeriodoLetivo inserirPeriodo(CadastroPeriodoLetivo periodo) { return acesso.manter("RepositorioCadastroAcademico","inserirPeriodo",0,() -> repo.inserirPeriodo(periodo)); }
            @Override public Optional<CadastroPeriodoLetivo> buscarPeriodo(long id) { return acesso.consultar(() -> repo.buscarPeriodo(id)); }
            @Override public List<CadastroPeriodoLetivo> listarPeriodos() { return acesso.consultar(() -> repo.listarPeriodos()); }
            @Override public CadastroPeriodoLetivo alterarPeriodo(long id, UnaryOperator<CadastroPeriodoLetivo> alteracao) { return acesso.manter("RepositorioCadastroAcademico","alterarPeriodo",id,() -> repo.alterarPeriodo(id,alteracao)); }
            @Override public Turma inserirTurma(Turma turma) { return acesso.manter("RepositorioCadastroAcademico","inserirTurma",0,() -> repo.inserirTurma(turma)); }
            @Override public Optional<Turma> buscarTurma(long id) { return acesso.consultar(() -> repo.buscarTurma(id)); }
            @Override public List<Turma> listarTurmas() { return acesso.consultar(() -> repo.listarTurmas()); }
            @Override public Turma alterarTurma(long id, UnaryOperator<Turma> alteracao) { return acesso.manter("RepositorioCadastroAcademico","alterarTurma",id,() -> repo.alterarTurma(id,alteracao)); }
            @Override public List<ReferenciaAcademica> listarDisciplinas() { return acesso.consultar(() -> repo.listarDisciplinas()); }
            @Override public List<ReferenciaAcademica> listarProfessores() { return acesso.consultar(() -> repo.listarProfessores()); }
        };
    }

    public static RepositorioProfessoresDisciplinas professoresDisciplinas(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioProfessoresDisciplinasJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioProfessoresDisciplinas() {
            @Override public Professor inserirProfessor(Professor professor) { return acesso.manter("RepositorioProfessoresDisciplinas","inserirProfessor",0,() -> repo.inserirProfessor(professor)); }
            @Override public Optional<Professor> buscarProfessor(long id) { return acesso.consultar(() -> repo.buscarProfessor(id)); }
            @Override public List<Professor> listarProfessores() { return acesso.consultar(() -> repo.listarProfessores()); }
            @Override public Professor alterarProfessor(long id, UnaryOperator<Professor> alteracao) { return acesso.manter("RepositorioProfessoresDisciplinas","alterarProfessor",id,() -> repo.alterarProfessor(id,alteracao)); }
            @Override public Disciplina inserirDisciplina(Disciplina disciplina) { return acesso.manter("RepositorioProfessoresDisciplinas","inserirDisciplina",0,() -> repo.inserirDisciplina(disciplina)); }
            @Override public Optional<Disciplina> buscarDisciplina(long id) { return acesso.consultar(() -> repo.buscarDisciplina(id)); }
            @Override public List<Disciplina> listarDisciplinas() { return acesso.consultar(() -> repo.listarDisciplinas()); }
            @Override public Disciplina alterarDisciplina(long id, UnaryOperator<Disciplina> alteracao) { return acesso.manter("RepositorioProfessoresDisciplinas","alterarDisciplina",id,() -> repo.alterarDisciplina(id,alteracao)); }
            @Override public List<Disciplina> listarPreRequisitos(long disciplina) { return acesso.consultar(() -> repo.listarPreRequisitos(disciplina)); }
            @Override public void adicionarPreRequisito(long disciplina, long requisito) { acesso.manter("RepositorioProfessoresDisciplinas","adicionarPreRequisito",disciplina,() -> { repo.adicionarPreRequisito(disciplina,requisito); return null; }); }
            @Override public void removerPreRequisito(long disciplina, long requisito) { acesso.manter("RepositorioProfessoresDisciplinas","removerPreRequisito",disciplina,() -> { repo.removerPreRequisito(disciplina,requisito); return null; }); }
        };
    }

    public static RepositorioBaseAcademica baseAcademica(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioBaseAcademicaJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioBaseAcademica() {
            @Override public Aluno inserirAluno(Aluno aluno) { return acesso.manter("RepositorioBaseAcademica","inserirAluno",0,() -> repo.inserirAluno(aluno)); }
            @Override public Optional<Aluno> buscarAluno(long id) { return acesso.consultar(() -> repo.buscarAluno(id)); }
            @Override public List<Aluno> listarAlunos() { return acesso.consultar(() -> repo.listarAlunos()); }
            @Override public Aluno alterarAluno(long id, UnaryOperator<Aluno> alteracao) { return acesso.manter("RepositorioBaseAcademica","alterarAluno",id,() -> repo.alterarAluno(id,alteracao)); }
            @Override public Curso inserirCurso(Curso curso) { return acesso.manter("RepositorioBaseAcademica","inserirCurso",0,() -> repo.inserirCurso(curso)); }
            @Override public Optional<Curso> buscarCurso(long id) { return acesso.consultar(() -> repo.buscarCurso(id)); }
            @Override public List<Curso> listarCursos() { return acesso.consultar(() -> repo.listarCursos()); }
            @Override public Curso alterarCurso(long id, UnaryOperator<Curso> alteracao) { return acesso.manter("RepositorioBaseAcademica","alterarCurso",id,() -> repo.alterarCurso(id,alteracao)); }
            @Override public EstruturaCurricular inserirEstrutura(EstruturaCurricular estrutura) { return acesso.manter("RepositorioBaseAcademica","inserirEstrutura",0,() -> repo.inserirEstrutura(estrutura)); }
            @Override public Optional<EstruturaCurricular> buscarEstrutura(long id) { return acesso.consultar(() -> repo.buscarEstrutura(id)); }
            @Override public List<EstruturaCurricular> listarEstruturas(long curso) { return acesso.consultar(() -> repo.listarEstruturas(curso)); }
            @Override public EstruturaCurricular alterarEstrutura(long id, UnaryOperator<EstruturaCurricular> alteracao) { return acesso.manter("RepositorioBaseAcademica","alterarEstrutura",id,() -> repo.alterarEstrutura(id,alteracao)); }
            @Override public List<Disciplina> listarComponentes(long estrutura) { return acesso.consultar(() -> repo.listarComponentes(estrutura)); }
            @Override public void adicionarComponente(long estrutura, long disciplina) { acesso.manter("RepositorioBaseAcademica","adicionarComponente",estrutura,() -> { repo.adicionarComponente(estrutura,disciplina); return null; }); }
            @Override public void removerComponente(long estrutura, long disciplina) { acesso.manter("RepositorioBaseAcademica","removerComponente",estrutura,() -> { repo.removerComponente(estrutura,disciplina); return null; }); }
        };
    }

    public static RepositorioMatricula matriculas(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioMatriculaJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioMatricula() {
            @Override public Matricula criar(long aluno, long curso, LocalDate data, long periodo, String motivo) { return acesso.manter("RepositorioMatricula","criar",aluno,() -> repo.criar(aluno,curso,data,periodo,motivo)); }
            @Override public Optional<Matricula> buscar(long id) { return acesso.consultar(() -> repo.buscar(id)); }
            @Override public List<Matricula> listar(long aluno) { return acesso.consultar(() -> repo.listar(aluno)); }
            @Override public Matricula corrigir(long id, LocalDate data, long periodo, String motivo) { return acesso.manter("RepositorioMatricula","corrigir",id,() -> repo.corrigir(id,data,periodo,motivo)); }
            @Override public Matricula transicionar(long id, String destino, String motivo) { return acesso.manter("RepositorioMatricula","transicionar",id,() -> repo.transicionar(id,destino,motivo)); }
            @Override public List<EventoMatricula> historico(long id) { return acesso.consultar(() -> repo.historico(id)); }
            @Override public SituacaoBiblioteca consultarBiblioteca(long aluno) { return acesso.consultar(() -> repo.consultarBiblioteca(aluno)); }
        };
    }

    public static RepositorioInscricao inscricoes(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioInscricaoJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioInscricao() {
            @Override public Elegibilidade avaliar(long matricula, long turma) { return acesso.consultarMatricula(matricula,() -> repo.avaliar(matricula,turma)); }
            @Override public Inscricao inscrever(long matricula, long turma) { return acesso.manter("RepositorioInscricao","inscrever",matricula,() -> repo.inscrever(matricula,turma)); }
            @Override public List<Inscricao> listar(long matricula) { return acesso.consultarMatricula(matricula,() -> repo.listar(matricula)); }
            @Override public void vincularEstrutura(long matricula, long estrutura) { acesso.manter("RepositorioInscricao","vincularEstrutura",matricula,() -> { repo.vincularEstrutura(matricula,estrutura); return null; }); }
            @Override public RegraResultado.Resultado registrarResultado(long inscricao, BigDecimal p1, BigDecimal p2,
            BigDecimal pf, long totalMinutos, long presentesMinutos, String motivo) { return acesso.fecharResultado(inscricao,() -> repo.registrarResultado(inscricao,p1,p2,pf,totalMinutos,presentesMinutos,motivo)); }
        };
    }

    public static RepositorioEquivalencia equivalencias(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioEquivalenciaJdbc(c,sessao);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioEquivalencia() {
            @Override public Registro solicitar(Pedido pedido) { return repo.solicitar(pedido); }
            @Override public Registro decidir(long pedido, Decisao decisao) { return repo.decidir(pedido,decisao); }
            @Override public List<Registro> listar(long matricula) { return acesso.consultarProcesso(() -> repo.listar(matricula)); }
            @Override public PlanoBasico plano(long matricula) { return acesso.consultarProcesso(() -> repo.plano(matricula)); }
            @Override public List<Evento> historico(long matricula) { return acesso.consultarProcesso(() -> repo.historico(matricula)); }
        };
    }

    public static RepositorioPlanejamento planejamento(Connection c,SegurancaJdbc.Sessao sessao) {
        var repo=new RepositorioPlanejamentoJdbc(c);
        var acesso=new AcessoAcademicoJdbc(c,sessao);
        return new RepositorioPlanejamento() {
            @Override public List<Periodo> periodos() { return acesso.consultar(() -> repo.periodos()); }
            @Override public Resultado simular(long matricula, List<Long> selecionados, List<Long> prioridade) { return acesso.consultar(() -> repo.simular(matricula,selecionados,prioridade)); }
        };
    }
}

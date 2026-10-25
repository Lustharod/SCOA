package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.ServicoAlocacao;
import br.edu.cefetmg.scoa.aplicacao.ServicoCadastroAcademico;
import br.edu.cefetmg.scoa.dominio.CadastroPeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.Turma;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CadastroAcademicoJdbcIT {
    private Connection c;
    private ServicoCadastroAcademico servico;
    private ServicoAlocacao alocacao;
    private long professor,disciplina,sala;
    private CadastroPeriodoLetivo periodo;
    private Turma turma;
    private final List<Long> periodos=new ArrayList<>(), turmas=new ArrayList<>();
    private String etiqueta;
    private static final LocalDate INICIO=LocalDate.of(LocalDate.now().getYear()+1,2,1), FIM=INICIO.plusMonths(5);

    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); c.setAutoCommit(false);
        etiqueta="CTCAD-"+UUID.randomUUID().toString().substring(0,10);
        professor=inserir("INSERT INTO professor (identificacao,situacao) VALUES (?,'ATIVO')",etiqueta);
        disciplina=inserir("INSERT INTO disciplina (codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",etiqueta,etiqueta);
        servico=new ServicoCadastroAcademico(new RepositorioCadastroAcademicoJdbc(c));
        alocacao=new ServicoAlocacao(new RepositorioAlocacaoJdbc(c));
        periodo=novoPeriodo(); turma=novaTurma(periodo.id());
        sala=new RepositorioSalaJdbc(c).salvar(Sala.nova(etiqueta,"Bloco teste",40)).getId();
    }
    @AfterEach void limpar() throws Exception {
        if (c==null) return;
        try {
            if (!c.getAutoCommit()) c.rollback(); c.setAutoCommit(false);
            for (long id:turmas) executar("DELETE FROM horario_aula WHERE id_turma=?",id);
            for (long id:turmas) executar("DELETE FROM turma WHERE id_turma=?",id);
            for (long id:periodos) executar("DELETE FROM periodo_letivo WHERE id_periodo=?",id);
            if (sala!=0) executar("DELETE FROM sala WHERE id_sala=?",sala);
            if (disciplina!=0) executar("DELETE FROM disciplina WHERE id_disciplina=?",disciplina);
            if (professor!=0) executar("DELETE FROM professor WHERE id_professor=?",professor);
            c.commit();
        } finally { c.close(); }
    }
    @Test void cadastraConsultaListaEAtualizaTurma() {
        assertEquals(turma,servico.buscarTurma(turma.id()));
        assertTrue(servico.listarTurmas().contains(turma));
        var nova=servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"ES-NOVA",35);
        assertEquals(turma.id(),nova.id()); assertEquals(35,servico.buscarTurma(turma.id()).capacidade());
        assertEquals("ATIVO",nova.situacao());
        assertTrue(servico.listarDisciplinas().stream().anyMatch(r -> r.id()==disciplina));
        assertTrue(servico.listarProfessores().stream().anyMatch(r -> r.id()==professor));
    }
    @Test void atualizaEInativaPeriodoSemTurmas() {
        var p=novoPeriodo();
        assertTrue(servico.listarPeriodos().contains(p));
        var novo=servico.atualizarPeriodo(p.id(),"Novo semestre",INICIO.plusDays(2),FIM);
        assertEquals(INICIO.plusDays(2),servico.buscarPeriodo(p.id()).datas().inicio());
        assertEquals("INATIVO",servico.inativarPeriodo(novo.id()).situacao());
        assertThrows(IllegalArgumentException.class,() -> servico.cadastrarTurma(disciplina,professor,p.id(),"T",30));
    }
    @Test void periodoVinculadoPreservaDatasESituacaoMasPermiteDescricao() {
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarPeriodo(periodo.id(),"P",INICIO.plusDays(1),FIM));
        assertThrows(IllegalArgumentException.class,() -> servico.inativarPeriodo(periodo.id()));
        assertEquals(periodo,servico.buscarPeriodo(periodo.id()));
        assertEquals("Outra descrição",servico.atualizarPeriodo(periodo.id(),"Outra descrição",INICIO,FIM).descricao());
    }
    @Test void rejeitaVinculosInexistentesOuInativosSemCriarTurma() throws Exception {
        int antes=servico.listarTurmas().size();
        assertThrows(IllegalArgumentException.class,() -> servico.cadastrarTurma(4_294_967_295L,professor,periodo.id(),"T",30));
        assertThrows(IllegalArgumentException.class,() -> servico.cadastrarTurma(disciplina,4_294_967_295L,periodo.id(),"T",30));
        assertThrows(IllegalArgumentException.class,() -> servico.cadastrarTurma(disciplina,professor,4_294_967_295L,"T",30));
        executar("UPDATE professor SET situacao='INATIVO' WHERE id_professor=?",professor);
        assertThrows(IllegalArgumentException.class,() -> servico.cadastrarTurma(disciplina,professor,periodo.id(),"T",30));
        executar("UPDATE professor SET situacao='ATIVO' WHERE id_professor=?",professor);
        executar("UPDATE disciplina SET situacao='INATIVO' WHERE id_disciplina=?",disciplina);
        assertThrows(IllegalArgumentException.class,() -> servico.cadastrarTurma(disciplina,professor,periodo.id(),"T",30));
        assertEquals(antes,servico.listarTurmas().size());
    }
    @Test void alteracaoInvalidaPreservaDadosERollbackDoChamador() throws Exception {
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",0));
        assertEquals(turma,servico.buscarTurma(turma.id()));
        assertFalse(c.getAutoCommit()); c.rollback();
        assertThrows(IllegalArgumentException.class,() -> servico.buscarTurma(turma.id()));
    }
    @Test void recusaIdsAusentes() {
        assertThrows(IllegalArgumentException.class,() -> servico.buscarPeriodo(4_294_967_295L));
        assertThrows(IllegalArgumentException.class,() -> servico.buscarTurma(4_294_967_295L));
        assertThrows(IllegalArgumentException.class,() -> servico.inativarPeriodo(4_294_967_295L));
        assertThrows(IllegalArgumentException.class,() -> servico.inativarTurma(4_294_967_295L));
    }
    @Test void manutencaoDeTurmaPreservaAlocacoes() {
        var horario=alocacao.alocar(turma.id(),sala,1,LocalTime.of(8,0),LocalTime.of(10,0));
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",41));
        assertThrows(IllegalArgumentException.class,() -> servico.inativarTurma(turma.id()));
        var outro=novoPeriodo();
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarTurma(turma.id(),disciplina,professor,outro.id(),"T",30));
        assertEquals(40,servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",40).capacidade());
        alocacao.desalocar(horario.id());
        assertEquals(outro.id().longValue(),servico.atualizarTurma(turma.id(),disciplina,professor,outro.id(),"T",40).idPeriodo());
    }
    @Test void inativacaoPreservaRegistroEImpedeNovasAlocacoes() {
        assertEquals("INATIVO",servico.inativarTurma(turma.id()).situacao());
        assertEquals("INATIVO",servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",30).situacao());
        assertThrows(IllegalArgumentException.class,() -> alocacao.alocar(turma.id(),sala,1,LocalTime.of(8,0),LocalTime.of(10,0)));
        assertTrue(alocacao.listarHorarios(turma.id()).isEmpty());
    }
    @Test void preservaInscricoesExistentesECapacidadeMinima() throws Exception {
        long curso=inserir("INSERT INTO curso (nome,situacao) VALUES (?,'ATIVO')",etiqueta);
        for (int i=0;i<2;i++) {
            long aluno=inserir("INSERT INTO aluno (identificacao,situacao) VALUES (?,'ATIVO')",etiqueta+i);
            long matricula=inserir("INSERT INTO matricula_academica (id_aluno,id_curso,data_ingresso,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",aluno,curso);
            inserir("INSERT INTO inscricao_disciplina (id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",matricula,turma.id());
        }
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",1));
        assertThrows(IllegalArgumentException.class,() -> servico.inativarTurma(turma.id()));
        var outro=novoPeriodo();
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarTurma(turma.id(),disciplina,professor,outro.id(),"T",30));
        assertEquals(2,servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",2).capacidade());
    }
    @Test void turmaConcorrenteImpedeMudancaDasDatasDoPeriodo() throws Exception {
        var livre=novoPeriodo(); confirmar(); c.setAutoCommit(false); novaTurma(livre.id());
        CountDownLatch iniciou=new CountDownLatch(1);
        try (var executor=Executors.newSingleThreadExecutor()) {
            var futura=executor.submit(() -> {
                try (var outra=ApoioJdbc.abrir(false)) {
                    iniciou.countDown();
                    return new ServicoCadastroAcademico(new RepositorioCadastroAcademicoJdbc(outra))
                            .atualizarPeriodo(livre.id(),"P",INICIO.plusDays(1),FIM);
                }
            });
            try { assertTrue(iniciou.await(5,TimeUnit.SECONDS)); assertThrows(TimeoutException.class,() -> futura.get(200,TimeUnit.MILLISECONDS)); }
            finally { c.commit(); }
            assertInstanceOf(IllegalArgumentException.class,assertThrows(ExecutionException.class,() -> futura.get(10,TimeUnit.SECONDS)).getCause());
        }
        c.setAutoCommit(true); assertEquals(INICIO,servico.buscarPeriodo(livre.id()).datas().inicio());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean alterarPrimeiro) throws Exception {
        confirmar(); c.setAutoCommit(false);
        if (alterarPrimeiro) servico.atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",41);
        else alocacao.alocar(turma.id(),sala,1,LocalTime.of(8,0),LocalTime.of(10,0));
        CountDownLatch iniciou=new CountDownLatch(1);
        try (var executor=Executors.newSingleThreadExecutor()) {
            var futura=executor.submit(() -> {
                try (var outra=ApoioJdbc.abrir(false)) {
                    iniciou.countDown();
                    if (alterarPrimeiro) return (Object)new ServicoAlocacao(new RepositorioAlocacaoJdbc(outra))
                            .alocar(turma.id(),sala,1,LocalTime.of(8,0),LocalTime.of(10,0));
                    return new ServicoCadastroAcademico(new RepositorioCadastroAcademicoJdbc(outra))
                            .atualizarTurma(turma.id(),disciplina,professor,periodo.id(),"T",41);
                }
            });
            try { assertTrue(iniciou.await(5,TimeUnit.SECONDS)); assertThrows(TimeoutException.class,() -> futura.get(200,TimeUnit.MILLISECONDS)); }
            finally { c.commit(); }
            assertInstanceOf(IllegalArgumentException.class,assertThrows(ExecutionException.class,() -> futura.get(10,TimeUnit.SECONDS)).getCause());
        }
        c.setAutoCommit(true);
        assertEquals(alterarPrimeiro?41:30,servico.buscarTurma(turma.id()).capacidade());
        assertEquals(alterarPrimeiro?0:1,alocacao.listarHorarios(turma.id()).size());
    }
    private CadastroPeriodoLetivo novoPeriodo() {
        var novo=servico.cadastrarPeriodo(etiqueta,INICIO,FIM); periodos.add(novo.id()); return novo;
    }
    private Turma novaTurma(long idPeriodo) {
        var nova=servico.cadastrarTurma(disciplina,professor,idPeriodo,etiqueta,30); turmas.add(nova.id()); return nova;
    }
    private void confirmar() throws Exception { c.commit(); c.setAutoCommit(true); }
    private long inserir(String sql,Object... valores) throws Exception {
        try (var comando=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for (int i=0;i<valores.length;i++) comando.setObject(i+1,valores[i]);
            comando.executeUpdate(); try (var ids=comando.getGeneratedKeys()) { assertTrue(ids.next()); return ids.getLong(1); }
        }
    }
    private void executar(String sql,long id) throws Exception {
        try (var comando=c.prepareStatement(sql)) { comando.setLong(1,id); comando.executeUpdate(); }
    }
}

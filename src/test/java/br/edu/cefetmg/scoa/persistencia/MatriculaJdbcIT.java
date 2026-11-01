package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.*;
import br.edu.cefetmg.scoa.apresentacao.ConsoleMatricula;
import br.edu.cefetmg.scoa.dominio.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.lang.reflect.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MatriculaJdbcIT {
    private Connection c;
    private RepositorioMatriculaJdbc repo;
    private ServicoMatricula s;
    private long aluno,curso,periodo,usuario,biblioteca,item,exemplar,emprestimo;
    private final LocalDate data=LocalDate.of(2026,1,15);
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); c.setAutoCommit(false);
        String tag="P06-"+UUID.randomUUID();
        usuario=inserir("INSERT INTO usuario (identificacao,credencial_hash,situacao) VALUES (?,'fixture','ATIVO')",tag);
        aluno=inserir("INSERT INTO aluno (id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",usuario,tag);
        curso=inserir("INSERT INTO curso (nome,situacao) VALUES (?,'ATIVO')",tag);
        periodo=inserir("INSERT INTO periodo_letivo (descricao,data_inicio,data_fim,situacao) VALUES (?,'2026-02-01','2026-06-30','ATIVO')",tag);
        biblioteca=inserir("INSERT INTO usuario_biblioteca (id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",usuario);
        item=inserir("INSERT INTO item_acervo (titulo,tipo,classificacao,informacoes_bibliograficas,situacao) VALUES (?,'LIVRO','TESTE','fixture','ATIVO')",tag);
        exemplar=inserir("INSERT INTO exemplar (id_item_acervo,identificacao,situacao) VALUES (?,?,'ATIVO')",item,tag);
        emprestimo=inserir("INSERT INTO emprestimo (id_usuario_biblioteca,id_exemplar,data_emprestimo,data_prevista_devolucao,situacao) VALUES (?,?,NOW(),'2026-12-01','ATIVO')",biblioteca,exemplar);
        repo=new RepositorioMatriculaJdbc(c); s=new ServicoMatricula(repo);
    }
    @AfterEach void limpar() throws Exception {
        if(c==null) return;
        try {
            if(!c.getAutoCommit()) c.rollback(); c.setAutoCommit(false);
            sql("DELETE h FROM historico_matricula h JOIN matricula_academica m ON m.id_matricula=h.id_matricula WHERE m.id_aluno=?",aluno);
            sql("DELETE FROM matricula_academica WHERE id_aluno=?",aluno);
            sql("DELETE FROM pendencia_biblioteca WHERE id_emprestimo=?",emprestimo);
            sql("DELETE FROM emprestimo WHERE id_emprestimo=?",emprestimo);
            sql("DELETE FROM exemplar WHERE id_exemplar=?",exemplar); sql("DELETE FROM item_acervo WHERE id_item_acervo=?",item);
            sql("DELETE FROM usuario_biblioteca WHERE id_usuario_biblioteca=?",biblioteca);
            sql("DELETE FROM aluno WHERE id_aluno=?",aluno); sql("DELETE FROM usuario WHERE id_usuario=?",usuario);
            sql("DELETE FROM curso WHERE id_curso=?",curso); sql("DELETE FROM periodo_letivo WHERE id_periodo=?",periodo); c.commit();
        } finally { c.close(); }
    }
    private Matricula criar() { return s.criar(aluno,curso,data,periodo,"Ingresso autorizado 🙂"); }
    private long pendencia(String situacao,boolean impeditiva) throws Exception {
        return inserir("INSERT INTO pendencia_biblioteca (id_emprestimo,tipo_pendencia,valor,impeditiva,situacao) VALUES (?,'TESTE',0,?,?)",emprestimo,impeditiva,situacao);
    }
    @Test void p0602CriacaoEHistorico() {
        var m=criar(); assertEquals(m,s.buscar(m.id())); assertEquals(List.of(m),s.listar(aluno));
        var e=s.historico(m.id()).getFirst(); assertNull(e.situacaoAnterior()); assertEquals("ATIVO",e.situacaoNova());
        assertEquals(periodo,e.periodoNovo()); assertEquals(data,e.dataNova()); assertNotNull(e.ocorridoEm());
    }
    @ParameterizedTest @ValueSource(strings={"ATIVO","REGULARIZADO","LIBERADO"})
    void p0603PendenciaEncerradaOuImpeditiva(String situacao) throws Exception {
        pendencia(situacao,true);
        if("ATIVO".equals(situacao)) { assertThrows(IllegalArgumentException.class,this::criar); assertTrue(s.listar(aluno).isEmpty()); }
        else assertEquals("ATIVO",criar().situacao());
    }
    @Test void p0603NaoImpeditiva() throws Exception { pendencia("ATIVO",false); assertEquals("ATIVO",criar().situacao()); }
    @ParameterizedTest @ValueSource(strings={"SEM_CONTA","SEM_BIBLIOTECA","INATIVO","DESCONHECIDA"})
    void p0604IdentificacaoOuSituacaoIndisponivel(String caso) throws Exception {
        switch(caso) {
            case "SEM_CONTA" -> sql("UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",aluno);
            case "SEM_BIBLIOTECA" -> { }
            case "INATIVO" -> sql("UPDATE usuario_biblioteca SET situacao='INATIVO' WHERE id_usuario_biblioteca=?",biblioteca);
            default -> pendencia("DESCONHECIDA",false);
        }
        if(caso.equals("SEM_BIBLIOTECA")) {
            long outro=inserir("INSERT INTO usuario (identificacao,credencial_hash,situacao) VALUES ('P06 temporario','fixture','ATIVO')");
            sql("UPDATE aluno SET id_usuario=? WHERE id_aluno=?",outro,aluno);
        }
        assertEquals(SituacaoBiblioteca.Estado.INDISPONIVEL,s.consultarBiblioteca(aluno).estado());
        assertThrows(IllegalArgumentException.class,this::criar); assertTrue(s.listar(aluno).isEmpty());
    }
    @Test void p0605DuplicidadeEReingresso() {
        var primeira=criar(); assertThrows(IllegalArgumentException.class,this::criar);
        s.trancar(primeira.id(),"Suspensão"); assertThrows(IllegalArgumentException.class,this::criar);
        s.cancelar(primeira.id(),"Encerramento"); var segunda=criar(); assertNotEquals(primeira.id(),segunda.id());
        assertEquals("CANCELADO",s.buscar(primeira.id()).situacao()); assertEquals(2,s.listar(aluno).size());
        assertThrows(IllegalArgumentException.class,()->s.retornar(primeira.id(),"Não permitido"));
    }
    @Test void p0605LegadoDesconhecidoBloqueia() throws Exception {
        inserir("INSERT INTO matricula_academica (id_aluno,id_curso,data_ingresso,situacao) VALUES (?,?,?,'LEGADO')",aluno,curso,data);
        assertThrows(IllegalArgumentException.class,this::criar);
    }
    @Test void p0607TransicoesEHistorico() {
        var m=criar(); s.trancar(m.id(),"Motivo"); s.retornar(m.id(),"Retorno"); s.cancelar(m.id(),"Fim");
        assertEquals(4,s.historico(m.id()).size()); assertEquals("CANCELADO",s.buscar(m.id()).situacao());
        assertThrows(IllegalArgumentException.class,()->repo.transicionar(m.id(),"CONCLUIDO","Fim"));
        assertEquals(4,s.historico(m.id()).size());
    }
    @Test void p0608RetornoBloqueadoEPreservacao() throws Exception {
        var m=criar(); s.trancar(m.id(),"Motivo"); pendencia("ATIVO",true);
        assertThrows(IllegalArgumentException.class,()->s.retornar(m.id(),"Volta"));
        assertEquals("TRANCADO",s.buscar(m.id()).situacao()); assertEquals(2,s.historico(m.id()).size());
        sql("UPDATE pendencia_biblioteca SET situacao='LIBERADO' WHERE id_emprestimo=?",emprestimo);
        sql("UPDATE aluno SET situacao='INATIVO' WHERE id_aluno=?",aluno);
        assertThrows(IllegalArgumentException.class,()->s.retornar(m.id(),"Volta"));
        sql("UPDATE aluno SET situacao='ATIVO' WHERE id_aluno=?",aluno);
        sql("UPDATE curso SET situacao='INATIVO' WHERE id_curso=?",curso);
        assertThrows(IllegalArgumentException.class,()->s.retornar(m.id(),"Volta"));
    }
    @Test void p0608RetornoIndisponivelMantemHistorico() throws Exception {
        var m=criar(); s.trancar(m.id(),"Suspensão");
        sql("UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",aluno);
        assertThrows(IllegalArgumentException.class,()->s.retornar(m.id(),"Tentativa"));
        assertEquals("TRANCADO",s.buscar(m.id()).situacao()); assertEquals(2,s.historico(m.id()).size());
    }
    @Test void p0610DatasLimiteEMotivoUnicode() {
        var m=s.criar(aluno,curso,LocalDate.of(1000,1,1),periodo,"🙂".repeat(500));
        assertEquals(LocalDate.of(1000,1,1),s.buscar(m.id()).dataIngresso());
        assertEquals("🙂".repeat(500),s.historico(m.id()).getFirst().motivo());
        assertEquals(LocalDate.of(9999,12,31),s.corrigir(m.id(),LocalDate.of(9999,12,31),periodo,"Limite DATE").dataIngresso());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void p0609CorrecaoSemBibliotecaESemMudanca(boolean affectedRows) throws Exception {
        c.rollback(); c.close(); prepararComAffectedRows(affectedRows);
        var m=criar(); sql("UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",aluno);
        assertEquals(m,s.corrigir(m.id(),data,periodo,"Sem mudança"));
        s.cancelar(m.id(),"Fim"); var nova=s.corrigir(m.id(),LocalDate.of(1000,1,1),periodo,"Correção 🙂");
        assertEquals("CANCELADO",nova.situacao()); assertEquals(m.idAluno(),nova.idAluno()); assertEquals(m.idCurso(),nova.idCurso());
        assertEquals(data,s.historico(m.id()).getLast().dataAnterior()); assertEquals(4,s.historico(m.id()).size());
    }
    private void prepararComAffectedRows(boolean affectedRows) throws Exception {
        preparar(); // fixtures não confirmadas; trocar conexão exige confirmar e limpeza explícita no AfterEach
        c.commit(); c.close(); c=ApoioJdbc.abrir(affectedRows); c.setAutoCommit(false); repo=new RepositorioMatriculaJdbc(c); s=new ServicoMatricula(repo);
    }
    @ParameterizedTest @ValueSource(strings={"ALUNO","CURSO","PERIODO","AUSENTE"})
    void p0610ReferenciasInvalidas(String caso) throws Exception {
        switch(caso) {
            case "ALUNO" -> sql("UPDATE aluno SET situacao='INATIVO' WHERE id_aluno=?",aluno);
            case "CURSO" -> sql("UPDATE curso SET situacao='INATIVO' WHERE id_curso=?",curso);
            case "PERIODO" -> sql("UPDATE periodo_letivo SET situacao='INATIVO' WHERE id_periodo=?",periodo);
            default -> { assertThrows(IllegalArgumentException.class,()->s.criar(aluno,curso,data,4294967295L,"Ausente")); return; }
        }
        assertThrows(IllegalArgumentException.class,this::criar); assertTrue(s.listar(aluno).isEmpty());
    }
    @Test void p0611RollbackExterno() throws Exception {
        var m=criar(); assertFalse(c.getAutoCommit());
        assertThrows(IllegalArgumentException.class,()->s.corrigir(m.id(),data,periodo," "));
        assertEquals(m,s.buscar(m.id())); c.rollback(); assertTrue(repo.buscar(m.id()).isEmpty());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void p0611FalhaNoHistoricoDesfazMatricula(boolean propria) throws Exception {
        c.commit(); c.setAutoCommit(propria);
        if(!propria) sql("UPDATE curso SET nome='P06 alteração externa preservada' WHERE id_curso=?",curso);
        Connection falha=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,metodo,args)->{
            if(metodo.getName().equals("prepareStatement") && args[0].toString().startsWith("INSERT INTO historico_matricula")) throw new SQLException("Falha sintética de histórico");
            try { return metodo.invoke(c,args); } catch(InvocationTargetException e) { throw e.getCause(); }
        });
        assertThrows(PersistenciaException.class,()->new RepositorioMatriculaJdbc(falha).criar(aluno,curso,data,periodo,"Teste atômico"));
        assertTrue(s.listar(aluno).isEmpty()); assertEquals(propria,c.getAutoCommit());
        if(!propria) assertEquals("P06 alteração externa preservada",new RepositorioBaseAcademicaJdbc(c).buscarCurso(curso).orElseThrow().nome());
    }
    @Test void p0604FalhaSqlBibliotecaNaoLibera() {
        Connection falha=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,metodo,args)->{
            if(metodo.getName().equals("prepareStatement") && args[0].toString().contains("JOIN usuario_biblioteca")) throw new SQLException("Consulta indisponível sintética");
            try { return metodo.invoke(c,args); } catch(InvocationTargetException e) { throw e.getCause(); }
        });
        assertThrows(PersistenciaException.class,()->new RepositorioMatriculaJdbc(falha).criar(aluno,curso,data,periodo,"Teste indisponibilidade"));
        assertTrue(s.listar(aluno).isEmpty());
    }
    @Test void p0612CriacoesConcorrentes() throws Exception {
        c.commit(); var inicio=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> tarefa=()->{ try(var outra=ApoioJdbc.abrir(false)) { inicio.await(); try { new RepositorioMatriculaJdbc(outra).criar(aluno,curso,data,periodo,"Concorrência"); return true; } catch(IllegalArgumentException e) { return false; } } };
            var a=pool.submit(tarefa); var b=pool.submit(tarefa); inicio.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)); c.rollback(); assertEquals(1,s.listar(aluno).size());
        } finally { pool.shutdownNow(); }
    }
    @Test void p0612BibliotecaEscritorCoordenado() throws Exception {
        c.commit(); var pool=Executors.newSingleThreadExecutor();
        try(var escritor=ApoioJdbc.abrir(false)) {
            escritor.setAutoCommit(false);
            try(var p=escritor.prepareStatement("SELECT id_usuario_biblioteca FROM usuario_biblioteca WHERE id_usuario_biblioteca=? FOR UPDATE")) {
                p.setLong(1,biblioteca); try(var r=p.executeQuery()) { assertTrue(r.next()); }
            }
            var iniciou=new CountDownLatch(1);
            var tentativa=pool.submit(()->{ try(var outra=ApoioJdbc.abrir(false)) {
                iniciou.countDown();
                return assertThrows(IllegalArgumentException.class,()->new RepositorioMatriculaJdbc(outra).criar(aluno,curso,data,periodo,"Concorrente"));
            }});
            assertTrue(iniciou.await(5,TimeUnit.SECONDS));
            // A consulta de matrícula não pode ultrapassar a trava bibliotecária.
            assertThrows(TimeoutException.class,()->tentativa.get(200,TimeUnit.MILLISECONDS));
            try(var p=escritor.prepareStatement("INSERT INTO pendencia_biblioteca (id_emprestimo,tipo_pendencia,valor,impeditiva,situacao) VALUES (?,'TESTE',0,1,'ATIVO')")) {
                p.setLong(1,emprestimo); p.executeUpdate();
            }
            escritor.commit(); assertTrue(tentativa.get(10,TimeUnit.SECONDS).getMessage().contains("impeditiva"));
            c.rollback(); assertTrue(s.listar(aluno).isEmpty());
        } finally { pool.shutdownNow(); }
    }
    @Test void p0613PeriodoProtegidoELegado() throws Exception {
        long id=inserir("INSERT INTO matricula_academica (id_aluno,id_curso,data_ingresso,situacao) VALUES (?,?,?,'CANCELADO')",aluno,curso,data);
        assertNull(s.buscar(id).idPeriodoIngresso()); assertTrue(s.historico(id).isEmpty());
        s.corrigir(id,data,periodo,"Completar período");
        var periodos=new RepositorioCadastroAcademicoJdbc(c); var atual=periodos.buscarPeriodo(periodo).orElseThrow();
        assertThrows(IllegalArgumentException.class,()->periodos.alterarPeriodo(periodo,p->new CadastroPeriodoLetivo(p.id(),p.descricao(),p.datas(),"INATIVO")));
        assertEquals(atual.datas(),periodos.alterarPeriodo(periodo,p->new CadastroPeriodoLetivo(p.id(),"Descrição corrigida",p.datas(),p.situacao())).datas());
    }
    @Test void p0614ConsoleConfirmacaoCancelamentoEEof() {
        String cadastro="3\n"+aluno+"\n"+curso+"\n2026-01-15\n"+periodo+"\nIngresso\n";
        console(cadastro+"N\n0\n"); assertTrue(s.listar(aluno).isEmpty());
        console(cadastro); assertTrue(s.listar(aluno).isEmpty());
        console("3\nabc\n3\n0\n3\n"+aluno+"\n"+curso+"\n2026-02-30\n0\n"); assertTrue(s.listar(aluno).isEmpty());
        console(cadastro+"S\n0\n"); assertEquals(1,s.listar(aluno).size());
        long id=s.listar(aluno).getFirst().id();
        console("5\n"+id+"\nSem confirmação\n"); assertEquals("ATIVO",s.buscar(id).situacao());
        console("5\n"+id+"\nSuspensão\nS\n6\n"+id+"\nRetorno\nS\n0\n");
        assertEquals("ATIVO",s.buscar(id).situacao()); assertEquals(3,s.historico(id).size());
        console("4\n"+id+"\n2026-01-16\n"+periodo+"\nCorreção cancelada\nN\n0\n");
        assertEquals(data,s.buscar(id).dataIngresso());
        console("4\n"+id+"\n2026-01-16\n"+periodo+"\nCorreção confirmada\nS\n7\n"+id+"\nEncerramento\nS\n0\n");
        assertEquals("CANCELADO",s.buscar(id).situacao()); assertEquals(5,s.historico(id).size());
        console("1\n"+aluno+"\n2\n"+id+"\n8\n"+id+"\n9\n"+aluno+"\n0\n");
    }
    private void console(String texto) {
        var original=System.out;
        try(var saida=new PrintStream(new ByteArrayOutputStream())) { System.setOut(saida); new ConsoleMatricula(new Scanner(texto),s).executar(); }
        finally { System.setOut(original); }
    }
    private void sql(String sql,Object... valores) throws Exception { try(var p=c.prepareStatement(sql)) { preencher(p,valores); p.executeUpdate(); } }
    private long inserir(String sql,Object... valores) throws Exception {
        try(var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) { preencher(p,valores); p.executeUpdate(); try(var r=p.getGeneratedKeys()) { r.next(); return r.getLong(1); } }
    }
    private void preencher(PreparedStatement p,Object... valores) throws SQLException { for(int i=0;i<valores.length;i++) p.setObject(i+1,valores[i]); }
}

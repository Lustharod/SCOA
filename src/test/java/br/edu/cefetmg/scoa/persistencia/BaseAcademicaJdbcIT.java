package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.*;
import br.edu.cefetmg.scoa.apresentacao.ConsoleBaseAcademica;
import br.edu.cefetmg.scoa.dominio.*;
import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class BaseAcademicaJdbcIT {
    private Connection c;
    private RepositorioBaseAcademicaJdbc repo;
    private ServicoBaseAcademica s;
    private Aluno aluno;
    private Curso curso;
    private EstruturaCurricular estrutura;
    private Disciplina disciplina;
    private String etiqueta;
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); c.setAutoCommit(false);
        etiqueta="CTP05-"+UUID.randomUUID().toString().substring(0,10);
        repo=new RepositorioBaseAcademicaJdbc(c); s=new ServicoBaseAcademica(repo);
        aluno=s.cadastrarAluno(etiqueta+" Aluno"); curso=s.cadastrarCurso(etiqueta+" Curso");
        estrutura=s.cadastrarEstrutura(curso.id(),etiqueta+" Estrutura");
        disciplina=new ServicoProfessoresDisciplinas(new RepositorioProfessoresDisciplinasJdbc(c))
                .cadastrarDisciplina(etiqueta,etiqueta+" Disciplina",60);
    }
    @AfterEach void limpar() throws Exception {
        if(c==null) return;
        try {
            if(!c.getAutoCommit()) c.rollback(); c.setAutoCommit(false);
            // Somente fixtures desta execução; normalmente são desfeitas pelo rollback.
            sql("DELETE FROM estrutura_disciplina WHERE id_estrutura=?",estrutura.id());
            sql("DELETE FROM estrutura_curricular WHERE id_estrutura=?",estrutura.id());
            sql("DELETE FROM disciplina WHERE id_disciplina=?",disciplina.id());
            sql("DELETE FROM curso WHERE id_curso=?",curso.id());
            sql("DELETE FROM aluno WHERE id_aluno=?",aluno.id());
            c.commit();
        } finally { c.close(); }
    }
    @Test void p0502AlunoMantemContaIdentidadeESituacao() throws Exception {
        long conta=inserir("INSERT INTO usuario (identificacao,credencial_hash,situacao) VALUES (?,'sintetico','ATIVO')",etiqueta);
        sql("UPDATE aluno SET id_usuario=? WHERE id_aluno=?",conta,aluno.id());
        var novo=s.atualizarAluno(aluno.id(),"  "+etiqueta+" Corrigido  ");
        assertEquals(conta,novo.idUsuario()); assertEquals(aluno.id(),novo.id()); assertEquals("ATIVO",novo.situacao());
        assertEquals(novo,s.buscarAluno(aluno.id())); assertTrue(s.listarAlunos().contains(novo));
    }
    @Test void p0503InativacaoPreservaHistoricoENaoReativa() throws Exception {
        long matricula=inserir("INSERT INTO matricula_academica (id_aluno,id_curso,data_ingresso,situacao) VALUES (?,?,'2020-01-01','ATIVO')",aluno.id(),curso.id());
        assertEquals("INATIVO",s.inativarAluno(aluno.id()).situacao());
        assertEquals("INATIVO",s.inativarAluno(aluno.id()).situacao());
        assertEquals("INATIVO",s.atualizarAluno(aluno.id(),etiqueta+" Alterado").situacao());
        assertEquals(1,contar("SELECT COUNT(*) FROM matricula_academica WHERE id_matricula=? AND id_aluno=? AND situacao='ATIVO'",matricula,aluno.id()));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void p0504CursoEStruturaAtualizamSemMudancas(boolean affectedRows) throws Exception {
        c.rollback(); c.close(); c=ApoioJdbc.abrir(affectedRows); c.setAutoCommit(false);
        repo=new RepositorioBaseAcademicaJdbc(c);s=new ServicoBaseAcademica(repo);
        curso=s.cadastrarCurso(etiqueta); estrutura=s.cadastrarEstrutura(curso.id(),etiqueta);
        assertEquals(curso,s.atualizarCurso(curso.id(),curso.nome()));
        assertEquals(estrutura,s.atualizarEstrutura(estrutura.id(),estrutura.identificacao()));
        var novo=s.atualizarCurso(curso.id(),etiqueta+" Curso corrigido");
        assertEquals(novo,s.buscarCurso(curso.id()));assertTrue(s.listarCursos().contains(novo));
        sql("UPDATE curso SET situacao='INATIVO' WHERE id_curso=?",curso.id());
        assertEquals("INATIVO",s.atualizarCurso(curso.id(),etiqueta).situacao());
        sql("UPDATE estrutura_curricular SET situacao='INATIVO' WHERE id_estrutura=?",estrutura.id());
        assertEquals("INATIVO",s.atualizarEstrutura(estrutura.id(),etiqueta).situacao());
    }
    @Test void p0505EstruturaPreservaCursoESituacao() {
        var novo=s.atualizarEstrutura(estrutura.id(),etiqueta+" Revisão");
        assertEquals(curso.id().longValue(),novo.idCurso());assertEquals("ATIVO",novo.situacao());
        assertEquals(novo,s.buscarEstrutura(estrutura.id()));assertTrue(s.listarEstruturas(curso.id()).contains(novo));
        assertThrows(IllegalArgumentException.class,()->repo.alterarEstrutura(estrutura.id(),e->new EstruturaCurricular(e.id(),e.idCurso()+1,e.identificacao(),e.situacao())));
        assertEquals(novo,s.buscarEstrutura(estrutura.id()));
    }
    @Test void p0506ComponentesEP0507DuplicidadeEAusencia() {
        s.adicionarComponente(estrutura.id(),disciplina.id());
        assertEquals(List.of(disciplina),s.listarComponentes(estrutura.id()));
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),disciplina.id()));
        assertEquals(1,s.listarComponentes(estrutura.id()).size());
        s.removerComponente(estrutura.id(),disciplina.id());
        assertTrue(s.listarComponentes(estrutura.id()).isEmpty());
        assertThrows(IllegalArgumentException.class,()->s.removerComponente(estrutura.id(),disciplina.id()));
        assertTrue(new RepositorioProfessoresDisciplinasJdbc(c).buscarDisciplina(disciplina.id()).isPresent());
    }
    @Test void p0508ReferenciasAusentes() {
        long ausente=4294967295L;
        assertThrows(IllegalArgumentException.class,()->s.buscarAluno(ausente));
        assertThrows(IllegalArgumentException.class,()->s.atualizarAluno(ausente,"Aluno"));
        assertThrows(IllegalArgumentException.class,()->s.inativarAluno(ausente));
        assertThrows(IllegalArgumentException.class,()->s.buscarCurso(ausente));
        assertThrows(IllegalArgumentException.class,()->s.atualizarCurso(ausente,"Curso"));
        assertThrows(IllegalArgumentException.class,()->s.cadastrarEstrutura(ausente,"Estrutura"));
        assertThrows(IllegalArgumentException.class,()->s.listarEstruturas(ausente));
        assertThrows(IllegalArgumentException.class,()->s.buscarEstrutura(ausente));
        assertThrows(IllegalArgumentException.class,()->s.atualizarEstrutura(ausente,"Estrutura"));
        assertThrows(IllegalArgumentException.class,()->s.listarComponentes(ausente));
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),ausente));
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(ausente,disciplina.id()));
        assertTrue(s.listarComponentes(estrutura.id()).isEmpty());
    }
    @Test void p0508ReferenciasInativas() throws Exception {
        sql("UPDATE disciplina SET situacao='INATIVO' WHERE id_disciplina=?",disciplina.id());
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),disciplina.id()));
        sql("UPDATE disciplina SET situacao='ATIVO' WHERE id_disciplina=?",disciplina.id());
        sql("UPDATE curso SET situacao='INATIVO' WHERE id_curso=?",curso.id());
        assertThrows(IllegalArgumentException.class,()->s.cadastrarEstrutura(curso.id(),"Nova"));
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),disciplina.id()));
        sql("UPDATE curso SET situacao='ATIVO' WHERE id_curso=?",curso.id());
        sql("UPDATE estrutura_curricular SET situacao='INATIVO' WHERE id_estrutura=?",estrutura.id());
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),disciplina.id()));
    }
    @Test void p0509PlanoProtegeInclusaoERetirada() throws Exception {
        s.adicionarComponente(estrutura.id(),disciplina.id());
        long matricula=inserir("INSERT INTO matricula_academica (id_aluno,id_curso,data_ingresso,situacao) VALUES (?,?,'2020-01-01','ATIVO')",aluno.id(),curso.id());
        inserir("INSERT INTO plano_estudo (id_matricula,id_estrutura,data_atualizacao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",matricula,estrutura.id());
        assertThrows(IllegalArgumentException.class,()->s.removerComponente(estrutura.id(),disciplina.id()));
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),disciplina.id()));
        assertEquals(1,s.listarComponentes(estrutura.id()).size());
        assertEquals(estrutura.idCurso(),s.atualizarEstrutura(estrutura.id(),etiqueta+" Corrigida").idCurso());
    }
    @Test void p0510TransacaoExternaDesfazOperacoesEFalhaNaoDesfazAnterior() throws Exception {
        s.adicionarComponente(estrutura.id(),disciplina.id());
        assertThrows(IllegalArgumentException.class,()->s.atualizarAluno(aluno.id()," "));
        assertFalse(c.getAutoCommit()); assertEquals(1,s.listarComponentes(estrutura.id()).size());
        c.rollback();
        assertTrue(repo.buscarAluno(aluno.id()).isEmpty());assertTrue(repo.buscarCurso(curso.id()).isEmpty());
        assertTrue(repo.buscarEstrutura(estrutura.id()).isEmpty());
    }
    @Test void p0510TransacaoPropriaConfirmaERestauraAposFalha() throws Exception {
        c.commit(); c.setAutoCommit(true);
        s.adicionarComponente(estrutura.id(),disciplina.id()); assertTrue(c.getAutoCommit());
        assertThrows(IllegalArgumentException.class,()->s.adicionarComponente(estrutura.id(),disciplina.id()));
        assertTrue(c.getAutoCommit());
        try(var outra=ApoioJdbc.abrir(false)) {
            assertEquals(1,new ServicoBaseAcademica(new RepositorioBaseAcademicaJdbc(outra)).listarComponentes(estrutura.id()).size());
        }
    }
    @Test void p0511InclusoesConcorrentesNaoDuplicam() throws Exception {
        c.commit(); s.adicionarComponente(estrutura.id(),disciplina.id());
        var iniciou=new CountDownLatch(1);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var futuro=executor.submit(()->{
                try(var outra=ApoioJdbc.abrir(false)) {
                    iniciou.countDown();
                    new ServicoBaseAcademica(new RepositorioBaseAcademicaJdbc(outra)).adicionarComponente(estrutura.id(),disciplina.id());
                    return true;
                }
            });
            assertTrue(iniciou.await(5,TimeUnit.SECONDS));
            try { assertThrows(TimeoutException.class,()->futuro.get(200,TimeUnit.MILLISECONDS)); }
            finally { c.commit(); }
            var erro=assertThrows(ExecutionException.class,()->futuro.get(10,TimeUnit.SECONDS));
            assertInstanceOf(IllegalArgumentException.class,erro.getCause());
            assertEquals(1,s.listarComponentes(estrutura.id()).size());
        }
    }
    @Test void p0512ConsoleConfirmaFluxoCompleto() {
        String saida=console("2\n"+etiqueta+" Novo aluno\nS\n7\n"+etiqueta+" Novo curso\nS\n11\n"+curso.id()+"\n"+etiqueta+" Nova estrutura\nS\n"
                +"1\n3\n"+aluno.id()+"\n4\n"+aluno.id()+"\n"+etiqueta+" Corrigido\nS\n5\n"+aluno.id()+"\nS\n"
                +"6\n8\n"+curso.id()+"\n9\n"+curso.id()+"\n"+etiqueta+" Curso corrigido\nS\n10\n"+curso.id()+"\n12\n"+estrutura.id()+"\n13\n"+estrutura.id()+"\n"+etiqueta+" Estrutura corrigida\nS\n"
                +"15\n"+estrutura.id()+"\n"+disciplina.id()+"\nS\n14\n"+estrutura.id()+"\n16\n"+estrutura.id()+"\n"+disciplina.id()+"\nS\n0\n");
        assertTrue(saida.contains("Componente incluído."));assertTrue(saida.contains("Componente retirado."));
        assertEquals("INATIVO",s.buscarAluno(aluno.id()).situacao());assertEquals(etiqueta+" Corrigido",s.buscarAluno(aluno.id()).identificacao());
        assertEquals(etiqueta+" Curso corrigido",s.buscarCurso(curso.id()).nome());
        assertEquals(etiqueta+" Estrutura corrigida",s.buscarEstrutura(estrutura.id()).identificacao());
        assertEquals(2,s.listarEstruturas(curso.id()).size()); assertTrue(s.listarComponentes(estrutura.id()).isEmpty());
    }
    @Test void p0513ConsoleCancelaTodasAsGravacoes() {
        String saida=console("2\nCancelado\nN\n7\nCancelado\nN\n11\n"+curso.id()+"\nCancelada\nN\n4\n"+aluno.id()+"\nCancelado\nN\n5\n"+aluno.id()+"\nN\n9\n"+curso.id()+"\nCancelado\nN\n13\n"+estrutura.id()+"\nCancelada\nN\n15\n"+estrutura.id()+"\n"+disciplina.id()+"\nN\n16\n"+estrutura.id()+"\n"+disciplina.id()+"\nN\n0\n");
        assertEquals(9,saida.split("Operação cancelada\\.",-1).length-1);
        assertEquals(aluno,s.buscarAluno(aluno.id()));assertEquals(curso,s.buscarCurso(curso.id()));assertEquals(estrutura,s.buscarEstrutura(estrutura.id()));
        assertTrue(s.listarComponentes(estrutura.id()).isEmpty()); assertEquals(1,s.listarEstruturas(curso.id()).size());
    }
    @Test void p0513ConsoleRecusaInvalidosESaiNoFimDaEntrada() {
        String saida=console("3\nabc\n2\n \nS\n3\n4294967295\n99\n2\nSem confirmação\n");
        assertTrue(saida.contains("identificador válido"));assertTrue(saida.contains("Informe a identificação"));
        assertTrue(saida.contains("Aluno não encontrado"));assertTrue(saida.contains("Entrada encerrada"));
        assertEquals(aluno,s.buscarAluno(aluno.id()));
    }
    @Test void p0508RepositorioPreservaIdentidadesESituacoes() {
        assertThrows(IllegalArgumentException.class,()->repo.alterarAluno(aluno.id(),a->new Aluno(a.id(),1L,a.identificacao(),a.situacao())));
        s.inativarAluno(aluno.id());
        assertThrows(IllegalArgumentException.class,()->repo.alterarAluno(aluno.id(),a->new Aluno(a.id(),a.idUsuario(),a.identificacao(),"ATIVO")));
        assertThrows(IllegalArgumentException.class,()->repo.alterarCurso(curso.id(),a->new Curso(a.id(),a.nome(),"INATIVO")));
    }
    @Test void p0508SemUnicidadeInventada() {
        assertNotEquals(aluno.id(),s.cadastrarAluno(aluno.identificacao()).id());
        assertNotEquals(curso.id(),s.cadastrarCurso(curso.nome()).id());
        assertNotEquals(estrutura.id(),s.cadastrarEstrutura(curso.id(),estrutura.identificacao()).id());
    }
    private String console(String entrada) {
        var anterior=System.out; var bytes=new ByteArrayOutputStream();
        try(var impressao=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setOut(impressao); new ConsoleBaseAcademica(new Scanner(entrada),s).executar();
        } finally { System.setOut(anterior); }
        return bytes.toString(StandardCharsets.UTF_8);
    }
    private void sql(String sql,Object... args) throws Exception { try(var st=c.prepareStatement(sql)) { preencher(st,args);st.executeUpdate(); } }
    private long inserir(String sql,Object... args) throws Exception {
        try(var st=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) { preencher(st,args);st.executeUpdate();try(var r=st.getGeneratedKeys()){r.next();return r.getLong(1);} }
    }
    private long contar(String sql,Object... args) throws Exception {
        try(var st=c.prepareStatement(sql)){preencher(st,args);try(var r=st.executeQuery()){r.next();return r.getLong(1);}}
    }
    private static void preencher(PreparedStatement s,Object... args) throws Exception { for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]); }
}

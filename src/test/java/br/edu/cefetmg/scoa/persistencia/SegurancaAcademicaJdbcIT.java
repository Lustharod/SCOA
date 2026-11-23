package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.apresentacao.ConsoleProcessoAcademico;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixtures próprias e rollback: não usa nem altera as contas da validação manual. */
class SegurancaAcademicaJdbcIT {
    private Connection c;
    private SegurancaJdbc seguranca;
    private SegurancaJdbc.Sessao admin;
    private String tag;
    private final String senha=UUID.randomUUID().toString();
    private long aluno,professor,curso,cursoB;

    @BeforeEach void preparar() throws SQLException {
        c=ApoioJdbc.abrir(false);
        c.setAutoCommit(false);
        seguranca=new SegurancaJdbc(c);
        tag="IT13-"+UUID.randomUUID().toString().substring(0,16);
        long usuario=g(c,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",tag,SegurancaJdbc.codificar(senha.toCharArray()));
        long perfil=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,'Fixture','ATIVO')",tag);
        g(c,"INSERT INTO usuario_perfil VALUES (?,?)",usuario,perfil);
        var permissoes=q(c,"SELECT id_permissao FROM permissao WHERE codigo=?",r->r.getLong(1),SegurancaJdbc.ADMIN);
        long permissao=permissoes.isEmpty()?g(c,"INSERT INTO permissao(codigo,descricao) VALUES (?,'Fixture')",SegurancaJdbc.ADMIN):permissoes.getFirst();
        g(c,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,permissao);
        admin=seguranca.entrar(tag,senha.toCharArray());
        aluno=g(c,"INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        professor=g(c,"INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        curso=g(c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
        cursoB=g(c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag+"B");
    }
    @AfterEach void descartar() throws SQLException {
        if(c!=null) try(var conexao=c) { conexao.rollback(); }
    }
    private long criar(String acesso) { return seguranca.criarConta(admin,tag+acesso,senha.toCharArray(),acesso); }
    private SegurancaJdbc.Sessao entrar(String acesso) { return seguranca.entrar(tag+acesso,senha.toCharArray()); }
    private void vincular(long usuario) {
        seguranca.vincularContaAcademica(admin,usuario,"ALUNO",aluno);
        seguranca.vincularContaAcademica(admin,usuario,"PROFESSOR",professor);
        seguranca.vincularContaAcademica(admin,usuario,"COORDENADOR",curso);
        seguranca.vincularContaAcademica(admin,usuario,"COORDENADOR",cursoB);
    }
    private long eventos(long usuario) throws SQLException {
        return q(c,"SELECT COUNT(*) FROM evento_processo_academico WHERE tipo='ACESSO' AND id_processo=?",r->r.getLong(1),usuario).getFirst();
    }

    @Test void contaSemPermissoesAutenticaSemIdentidadeAcademicaOuPerfilVazio() throws SQLException {
        char[] segredo=senha.toCharArray();
        long id=seguranca.criarConta(admin,tag+"SEM_PERMISSOES",segredo,"SEM_PERMISSOES");
        assertArrayEquals(new char[segredo.length],segredo);
        var sessao=entrar("SEM_PERMISSOES");
        var conta=seguranca.minhaConta(sessao);
        assertEquals(id,conta.idConta());
        assertTrue(conta.vinculos().isEmpty());
        assertTrue(conta.permissoesTecnicas().isEmpty());
        assertTrue(q(c,"SELECT id_perfil FROM usuario_perfil WHERE id_usuario=?",r->r.getLong(1),id).isEmpty());
        assertThrows(IllegalArgumentException.class,()->SegurancaJdbc.exigirAluno(c,id,aluno));
        assertThrows(IllegalArgumentException.class,()->SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.ADMIN));
    }
    @ParameterizedTest @ValueSource(strings={"ALUNO","PROFESSOR","COORDENADOR"})
    void criacaoRecusaPapelAcademicoIndependenteSemPersistenciaParcial(String papel) throws SQLException {
        char[] segredo=senha.toCharArray();
        assertThrows(IllegalArgumentException.class,()->seguranca.criarConta(admin,tag+papel,segredo,papel));
        assertArrayEquals(new char[segredo.length],segredo);
        assertTrue(q(c,"SELECT id_usuario FROM usuario WHERE identificacao=?",r->r.getLong(1),tag+papel).isEmpty());
    }
    @Test void mesmaContaTemAlunoProfessorECoordenacaoDeVariosCursosSemGanharPermissoes() throws SQLException {
        long id=criar("SEM_PERMISSOES");
        var sessao=entrar("SEM_PERMISSOES");
        String credencial=q(c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),id).getFirst();
        vincular(id);
        var conta=seguranca.minhaConta(sessao);
        assertEquals(id,conta.idConta());
        assertEquals(List.of("ALUNO","PROFESSOR","COORDENADOR","COORDENADOR"),conta.vinculos().stream().map(SegurancaJdbc.VinculoAcademico::tipo).toList());
        assertEquals(List.of(aluno,professor,curso,cursoB),conta.vinculos().stream().map(SegurancaJdbc.VinculoAcademico::referencia).toList());
        assertTrue(conta.permissoesTecnicas().isEmpty());
        assertTrue(SegurancaJdbc.ehAluno(c,id,aluno));
        assertTrue(SegurancaJdbc.coordena(c,id,cursoB));
        assertEquals(credencial,q(c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),id).getFirst());
        assertEquals(5,eventos(id));
        assertThrows(IllegalArgumentException.class,()->seguranca.criarConta(sessao,tag+"intrusa",senha.toCharArray(),"ADMIN"));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","SOLICITANTE","ANALISTA","BIBLIOTECARIO"})
    void vinculoMantemExatamenteAsConcessoesTecnicasExistentes(String acesso) {
        long id=criar(acesso);
        var antes=seguranca.consultarConta(admin,id).permissoesTecnicas();
        var esperado=switch(acesso) {
            case "ADMIN" -> Set.of(SegurancaJdbc.ADMIN,SegurancaJdbc.SOLICITAR,SegurancaJdbc.DECIDIR);
            case "SOLICITANTE" -> Set.of(SegurancaJdbc.SOLICITAR);
            case "ANALISTA" -> Set.of(SegurancaJdbc.DECIDIR);
            default -> Set.of(SegurancaJdbc.REGISTRAR_PENDENCIA);
        };
        assertEquals(esperado,Set.copyOf(antes));
        vincular(id);
        assertEquals(antes,seguranca.minhaConta(entrar(acesso)).permissoesTecnicas());
    }
    @Test void nomeDePerfilLegadoNaoCriaPapelEConsultaNaoReescrevePerfisOuEventos() throws SQLException {
        long id=criar("SEM_PERMISSOES");
        long perfil=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,'Legado preservado','ATIVO')","SCOA ALUNO "+id);
        g(c,"INSERT INTO usuario_perfil VALUES (?,?)",id,perfil);
        assertTrue(seguranca.consultarConta(admin,id).vinculos().isEmpty());
        seguranca.vincularContaAcademica(admin,id,"PROFESSOR",professor);
        long quantidade=eventos(id);
        var consulta=seguranca.consultarConta(admin,id);
        assertEquals(List.of("PROFESSOR"),consulta.vinculos().stream().map(SegurancaJdbc.VinculoAcademico::tipo).toList());
        assertEquals("SCOA ALUNO "+id,q(c,"SELECT nome FROM perfil WHERE id_perfil=?",r->r.getString(1),perfil).getFirst());
        assertEquals(quantidade,eventos(id));
        assertEquals(perfil,q(c,"SELECT id_perfil FROM usuario_perfil WHERE id_usuario=?",r->r.getLong(1),id).getFirst());
    }
    @Test void repetirVinculosNaoDuplicaEventosERecusaTomadaDeCadastroDeOutraConta() throws SQLException {
        long id=criar("SEM_PERMISSOES");
        vincular(id);
        long quantidade=eventos(id);
        vincular(id);
        assertEquals(quantidade,eventos(id));
        long outro=criar("SOLICITANTE");
        for(String tipo:List.of("ALUNO","PROFESSOR")) {
            long ref="ALUNO".equals(tipo)?aluno:professor;
            assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(admin,outro,tipo,ref));
        }
        long novoAluno=g(c,"INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag+"Outro");
        long novoProfessor=g(c,"INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')",tag+"Outro");
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(admin,id,"ALUNO",novoAluno));
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(admin,id,"PROFESSOR",novoProfessor));
        assertTrue(seguranca.consultarConta(admin,outro).vinculos().isEmpty());
        assertEquals(quantidade,eventos(id));
    }
    @Test void contaComumConsultaSomenteSeusVinculosERevalidaSessaoEPermissao() throws SQLException {
        long id=criar("SEM_PERMISSOES");
        var sessao=entrar("SEM_PERMISSOES");
        assertThrows(IllegalArgumentException.class,()->seguranca.consultarConta(sessao,admin.usuario()));
        assertThrows(IllegalArgumentException.class,()->seguranca.consultarConta(sessao,id));
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(sessao,id,"ALUNO",aluno));
        assertThrows(IllegalArgumentException.class,()->seguranca.minhaConta(null));
        try(var outra=ApoioJdbc.abrir(false)) {
            assertThrows(IllegalArgumentException.class,()->new SegurancaJdbc(outra).minhaConta(sessao));
        }
        long tecnico=criar("ADMIN");
        var adminRevogavel=entrar("ADMIN");
        assertEquals(id,seguranca.consultarConta(adminRevogavel,id).idConta());
        g(c,"DELETE FROM usuario_perfil WHERE id_usuario=?",tecnico);
        assertThrows(IllegalArgumentException.class,()->seguranca.consultarConta(adminRevogavel,id));
        sessao.encerrar();
        assertThrows(IllegalArgumentException.class,()->seguranca.minhaConta(sessao));
    }
    @Test void contaInativaNaoAutenticaOuRecebeVinculoECadastroInativoPermaneceIdentificado() throws SQLException {
        long id=criar("SEM_PERMISSOES");
        var sessao=entrar("SEM_PERMISSOES");
        g(c,"UPDATE professor SET situacao='INATIVO' WHERE id_professor=?",professor);
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(admin,id,"PROFESSOR",professor));
        seguranca.vincularContaAcademica(admin,id,"ALUNO",aluno);
        g(c,"UPDATE aluno SET situacao='INATIVO' WHERE id_aluno=?",aluno);
        assertEquals("INATIVO",seguranca.minhaConta(sessao).vinculos().getFirst().situacao());
        g(c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",id);
        assertThrows(IllegalArgumentException.class,()->entrar("SEM_PERMISSOES"));
        assertThrows(IllegalArgumentException.class,()->seguranca.minhaConta(sessao));
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(admin,id,"COORDENADOR",curso));
        assertEquals("INATIVO",seguranca.consultarConta(admin,id).situacao());
    }
    @Test void falhaDeAuditoriaDesfazCriacaoEPermissoes() throws SQLException {
        var proxy=(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,metodo,args)->{
            if(metodo.getName().equals("prepareStatement") && ((String)args[0]).startsWith("INSERT INTO evento_processo_academico")) throw new SQLException("Falha sintética");
            try { return metodo.invoke(c,args); } catch(java.lang.reflect.InvocationTargetException erro) { throw erro.getCause(); }
        });
        var seg=new SegurancaJdbc(proxy);
        var sessao=seg.entrar(tag,senha.toCharArray());
        assertThrows(PersistenciaException.class,()->seg.criarConta(sessao,tag+"Falha",senha.toCharArray(),"ADMIN"));
        assertTrue(q(c,"SELECT id_usuario FROM usuario WHERE identificacao=?",r->r.getLong(1),tag+"Falha").isEmpty());
        assertFalse(c.getAutoCommit());
    }
    @Test void consoleDistingueContaDeReferenciaEMostraVinculosSemUsarNomeDoPerfil() {
        long id=criar("SEM_PERMISSOES");
        String saida=console(admin,"11\n"+id+"\nALUNO\n"+aluno+"\nS\n13\n"+id+"\n0\n");
        assertTrue(saida.contains("ID da conta de acesso (usuario.id_usuario)"));
        assertTrue(saida.contains("ID do cadastro de aluno (aluno.id_aluno)"));
        assertTrue(saida.contains("aluno.id_aluno="+aluno));
        assertTrue(saida.contains("Permissões técnicas concedidas: []"));
        String propria=console(entrar("SEM_PERMISSOES"),"12\n13\n"+admin.usuario()+"\n0\n");
        assertTrue(propria.contains("Conta de acesso: ID="+id));
        assertTrue(propria.contains("Sem permissão: SCOA_ACESSO_ADMIN"));
        assertFalse(propria.contains("Conta de acesso: ID="+admin.usuario()+" |"));
    }
    private String console(SegurancaJdbc.Sessao sessao,String opcoes) {
        var original=System.out;
        var bytes=new ByteArrayOutputStream();
        try(var saida=new PrintStream(bytes,true,StandardCharsets.UTF_8); var entrada=new Scanner(opcoes)) {
            System.setOut(saida);
            new ConsoleProcessoAcademico(entrada,c,sessao).executar();
        } finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}

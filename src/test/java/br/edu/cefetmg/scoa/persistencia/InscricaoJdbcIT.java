package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.aplicacao.ServicoInscricao;
import br.edu.cefetmg.scoa.apresentacao.ConsoleInscricao;
import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class InscricaoJdbcIT {
    private Connection c;
    private RepositorioInscricaoJdbc repo;
    private long aluno,curso,matricula,periodo,professor,disciplina,estrutura,turma,sala;
    private final List<Long> disciplinas=new ArrayList<>(),turmas=new ArrayList<>(),periodos=new ArrayList<>();
    private String tag;
    private long segundoAluno,segundaMatricula;
    private long inserir(String sql,Object... valores) throws Exception {
        try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]); s.executeUpdate();
            try(var r=s.getGeneratedKeys()) { return r.next()?r.getLong(1):0; }
        }
    }
    private void sql(String comando,Object... valores) throws Exception { inserir(comando,valores); }
    private LocalDate hoje() throws Exception {
        try(var s=c.createStatement();var r=s.executeQuery("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','-03:00'))")) { r.next(); return r.getDate(1).toLocalDate(); }
    }
    private long disciplina() throws Exception {
        long d=inserir("INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag,tag);
        disciplinas.add(d); return d;
    }
    private long periodo(LocalDate inicio,LocalDate fim) throws Exception {
        long p=inserir("INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",tag,inicio,fim);
        periodos.add(p); return p;
    }
    private long turma(long d,long p,int dia) throws Exception {
        long t=inserir("INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,1,'ATIVO')",d,professor,p,tag);
        turmas.add(t);
        inserir("INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,?,'08:00','09:00')",t,sala,dia);
        return t;
    }
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); tag="P07-"+UUID.randomUUID().toString().substring(0,20);
        aluno=inserir("INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        curso=inserir("INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
        professor=inserir("INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        sala=inserir("INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES (?,'fixture',10,'ATIVO')",tag);
        periodo=periodo(hoje().plusDays(10),hoje().plusDays(100)); disciplina=disciplina();
        turma=turma(disciplina,periodo,1);
        matricula=inserir("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",aluno,curso,periodo);
        estrutura=inserir("INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,tag);
        inserir("INSERT INTO estrutura_disciplina(id_estrutura,id_disciplina) VALUES (?,?)",estrutura,disciplina);
        repo=new RepositorioInscricaoJdbc(c); repo.vincularEstrutura(matricula,estrutura);
    }
    @AfterEach void limpar() throws Exception {
        if(c==null) return;
        try {
            if(!c.getAutoCommit()) c.rollback(); c.setAutoCommit(true);
            sql("DELETE r FROM resultado_inscricao r JOIN inscricao_disciplina i ON i.id_inscricao=r.id_inscricao JOIN turma t ON t.id_turma=i.id_turma WHERE t.codigo=?",tag);
            sql("DELETE i FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma WHERE t.codigo=?",tag);
            sql("DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",matricula);
            sql("DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",segundaMatricula);
            sql("DELETE FROM matricula_academica WHERE id_matricula=?",matricula);
            sql("DELETE FROM matricula_academica WHERE id_matricula=?",segundaMatricula);
            sql("DELETE FROM estrutura_disciplina WHERE id_estrutura=?",estrutura);
            sql("DELETE FROM estrutura_curricular WHERE id_estrutura=?",estrutura);
            for(long t:turmas) { sql("DELETE FROM horario_aula WHERE id_turma=?",t); sql("DELETE FROM turma WHERE id_turma=?",t); }
            for(long d:disciplinas) sql("DELETE FROM disciplina_prerequisito WHERE id_disciplina=? OR id_disciplina_prerequisito=?",d,d);
            for(long d:disciplinas) sql("DELETE FROM disciplina WHERE id_disciplina=?",d);
            for(long p:periodos) sql("DELETE FROM periodo_letivo WHERE id_periodo=?",p);
            sql("DELETE FROM aluno WHERE id_aluno=?",aluno); sql("DELETE FROM curso WHERE id_curso=?",curso);
            sql("DELETE FROM aluno WHERE id_aluno=?",segundoAluno);
            sql("DELETE FROM professor WHERE id_professor=?",professor); sql("DELETE FROM sala WHERE id_sala=?",sala);
        } finally { c.close(); }
    }
    @Test void inscricaoSemBibliotecaEValidacaoSemReserva() {
        assertTrue(repo.avaliar(matricula,turma).permite()); assertTrue(repo.listar(matricula).isEmpty());
        var i=repo.inscrever(matricula,turma); assertEquals("ATIVO",i.situacao());
        assertEquals(List.of(i),repo.listar(matricula)); assertFalse(repo.avaliar(matricula,turma).permite());
        assertThrows(IllegalArgumentException.class,()->repo.inscrever(matricula,turma));
    }
    @Test void revalidaTrancamento() throws Exception {
        assertTrue(repo.avaliar(matricula,turma).permite());
        sql("UPDATE matricula_academica SET situacao='TRANCADO' WHERE id_matricula=?",matricula);
        assertThrows(IllegalArgumentException.class,()->repo.inscrever(matricula,turma)); assertTrue(repo.listar(matricula).isEmpty());
    }
    @Test void janelaFechada() throws Exception {
        sql("UPDATE periodo_letivo SET data_inicio=? WHERE id_periodo=?",hoje().plusDays(7),periodo);
        assertFalse(repo.avaliar(matricula,turma).permite());
    }
    @Test void semEstruturaSemHorarioEEstadoDesconhecido() throws Exception {
        sql("DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",matricula);
        assertFalse(repo.avaliar(matricula,turma).permite()); repo.vincularEstrutura(matricula,estrutura);
        sql("DELETE FROM horario_aula WHERE id_turma=?",turma); assertFalse(repo.avaliar(matricula,turma).permite());
        sql("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'LEGADO')",matricula,turma);
        assertTrue(repo.avaliar(matricula,turma).motivos().stream().anyMatch(s->s.contains("desconhecida")));
    }
    @Test void prerequisitoExigeResultadoFormalEFechamento() throws Exception {
        long d=disciplina(); sql("INSERT INTO disciplina_prerequisito VALUES (?,?)",disciplina,d);
        assertFalse(repo.avaliar(matricula,turma).permite());
        long p=periodo(hoje().minusDays(100),hoje().minusDays(1)),t=turma(d,p,2);
        long i=inserir("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",matricula,t,hoje().minusDays(110));
        assertThrows(IllegalArgumentException.class,()->repo.registrarResultado(i,new BigDecimal("6"),new BigDecimal("6"),null,100,75,"Apuração"));
        var r=repo.registrarResultado(i,new BigDecimal("6"),new BigDecimal("6"),new BigDecimal("4"),100,75,"Apuração final sintética");
        assertEquals("APROVADO",r.situacao()); assertTrue(repo.avaliar(matricula,turma).permite());
        assertThrows(IllegalArgumentException.class,()->repo.registrarResultado(i,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Duplicado"));
    }
    @Test void reprovaFrequenciaESemFechamentoAntecipado() throws Exception {
        long i=repo.inscrever(matricula,turma).id();
        assertThrows(IllegalArgumentException.class,()->repo.registrarResultado(i,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Antecipado"));
        sql("UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",hoje().minusDays(100),hoje().minusDays(1),periodo);
        assertEquals("REPROVADO",repo.registrarResultado(i,BigDecimal.TEN,BigDecimal.TEN,null,1000,749,"Frequência apurada").situacao());
    }
    @Test void conflitoDeHorarioEProtecaoDasAlocacoes() throws Exception {
        repo.inscrever(matricula,turma);
        long d=disciplina(),t=turma(d,periodo,1); sql("INSERT INTO estrutura_disciplina VALUES (?,?)",estrutura,d);
        assertTrue(repo.avaliar(matricula,t).motivos().stream().anyMatch(s->s.contains("Conflito")));
        long h;
        try(var s=c.createStatement();var r=s.executeQuery("SELECT id_horario FROM horario_aula WHERE id_turma="+turma)) { r.next(); h=r.getLong(1); }
        assertThrows(IllegalArgumentException.class,()->new RepositorioAlocacaoJdbc(c).desalocar(h));
    }
    @Test void transacaoExternaERollback() throws Exception {
        c.setAutoCommit(false); repo.inscrever(matricula,turma);
        try(var outra=ApoioJdbc.abrir(false)) { assertTrue(new RepositorioInscricaoJdbc(outra).listar(matricula).isEmpty()); }
        c.rollback(); assertTrue(repo.listar(matricula).isEmpty()); assertFalse(c.getAutoCommit());
    }
    @Test void consoleDesisteEEofSemEscrita() {
        new ConsoleInscricao(new Scanner("1\n"+matricula+"\n"+turma+"\nN\n0\n"),new ServicoInscricao(repo)).executar();
        new ConsoleInscricao(new Scanner("1\n"+matricula+"\n"+turma+"\n"),new ServicoInscricao(repo)).executar();
        assertTrue(repo.listar(matricula).isEmpty());
    }
    @Test void duasConfirmacoesSoUmaInscricao() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var inicio=new CountDownLatch(1);
            Callable<Boolean> tarefa=()->{
                try(var outra=ApoioJdbc.abrir(false)) {
                    inicio.await();
                    try { new RepositorioInscricaoJdbc(outra).inscrever(matricula,turma); return true; }
                    catch(IllegalArgumentException e) { return false; }
                }
            };
            var a=pool.submit(tarefa);var b=pool.submit(tarefa); inicio.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)); assertEquals(1,repo.listar(matricula).size());
        }
    }
    @Test void doisAlunosDisputamUltimaVaga() throws Exception {
        segundoAluno=inserir("INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        segundaMatricula=inserir("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",segundoAluno,curso,periodo);
        repo.vincularEstrutura(segundaMatricula,estrutura);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var inicio=new CountDownLatch(1);
            var a=pool.submit(()->concorrer(matricula,inicio)); var b=pool.submit(()->concorrer(segundaMatricula,inicio)); inicio.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(1,repo.listar(matricula).size()+repo.listar(segundaMatricula).size());
        }
    }
    private boolean concorrer(long id,CountDownLatch inicio) throws Exception {
        try(var outra=ApoioJdbc.abrir(false)) {
            inicio.await();
            try { new RepositorioInscricaoJdbc(outra).inscrever(id,turma); return true; }
            catch(IllegalArgumentException e) { return false; }
        }
    }
    @Test void protegeEstruturaVinculada() throws Exception {
        long d=disciplina();
        assertThrows(IllegalArgumentException.class,()->new RepositorioBaseAcademicaJdbc(c).adicionarComponente(estrutura,d));
        assertThrows(IllegalArgumentException.class,()->repo.vincularEstrutura(matricula,estrutura));
    }
    @Test void equivalenciaLegadaNaoLiberaSemContrato() throws Exception {
        long d=disciplina(); sql("INSERT INTO disciplina_prerequisito VALUES (?,?)",disciplina,d);
        sql("INSERT INTO aproveitamento_equivalencia(id_matricula,id_disciplina_destino,decisao,data,resultado) VALUES (?,?,'DEFERIDO',CURRENT_DATE,'fixture sem produtor integrado')",matricula,d);
        try { assertThrows(IllegalArgumentException.class,()->repo.inscrever(matricula,turma)); assertTrue(repo.listar(matricula).isEmpty()); }
        finally { sql("DELETE FROM aproveitamento_equivalencia WHERE id_matricula=?",matricula); }
    }
    @Test void falhaDeEscritaDesfazResultadoEInscricao() throws Exception {
        long p=periodo(hoje().minusDays(100),hoje().minusDays(1)),t=turma(disciplina,p,2);
        long i=inserir("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",matricula,t,hoje().minusDays(110));
        // Falha posterior à inserção do resultado comprova atomicidade, não apenas recusa de validação.
        Connection falha=(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,metodo,args)->{
            try {
                Object retorno=metodo.invoke(c,args);
                if(metodo.getName().equals("prepareStatement") && ((String)args[0]).startsWith("UPDATE inscricao_disciplina")) {
                    return java.lang.reflect.Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(proxyStatement,m,a)->{
                        if(m.getName().equals("executeUpdate")) throw new SQLException("Falha sintética P07 após inserir resultado");
                        try { return m.invoke(retorno,a); } catch(java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                    });
                }
                return retorno;
            } catch(java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
        });
        {
            assertThrows(PersistenciaException.class,()->new RepositorioInscricaoJdbc(falha).registrarResultado(i,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Apuração"));
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM resultado_inscricao WHERE id_inscricao="+i)) { r.next(); assertEquals(0,r.getInt(1)); }
            assertEquals("ATIVO",repo.listar(matricula).getFirst().situacao());
        }
    }
}

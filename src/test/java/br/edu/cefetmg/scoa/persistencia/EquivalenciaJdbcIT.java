package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.Equivalencia.*;
import br.edu.cefetmg.scoa.aplicacao.ServicoEquivalencia;
import br.edu.cefetmg.scoa.apresentacao.ConsoleEquivalencia;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class EquivalenciaJdbcIT {
    private Connection c;
    private RepositorioEquivalenciaJdbc repo;
    private RepositorioInscricaoJdbc inscricoes;
    private long aluno,curso,matricula,estrutura,destino,seguinte,periodo,turma,professor,sala;
    private String tag;
    private long sql(String comando,Object... valores) throws SQLException {
        try(var s=c.prepareStatement(comando,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            s.executeUpdate(); try(var r=s.getGeneratedKeys()) { return r.next()?r.getLong(1):0; }
        }
    }
    private LocalDate hoje() throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','-03:00'))")) { r.next(); return r.getObject(1,LocalDate.class); }
    }
    private Pedido pedido() throws SQLException {
        return new Pedido(matricula,destino,"Instituição externa","Origem comprovada",60,"Conteúdo analisado","Protocolo "+tag+": conclusão, ementa e carga",hoje().minusYears(1),"Ementa destino analisada");
    }
    private Decisao decisao(boolean deferir) { return new Decisao(deferir,"Análise manual de ementa e carga horária; fundamento acadêmico registrado.","Funcionário declarado "+tag); }
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); tag="P08-"+UUID.randomUUID().toString().substring(0,16);
        aluno=sql("INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        curso=sql("INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
        professor=sql("INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        sala=sql("INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES (?,'fixture',10,'ATIVO')",tag);
        destino=sql("INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+"A",tag+"A");
        seguinte=sql("INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+"B",tag+"B");
        periodo=sql("INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",tag,hoje().plusDays(10),hoje().plusDays(100));
        matricula=sql("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",aluno,curso,periodo);
        estrutura=sql("INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,tag);
        sql("INSERT INTO estrutura_disciplina(id_estrutura,id_disciplina) VALUES (?,?),(?,?)",estrutura,destino,estrutura,seguinte);
        inscricoes=new RepositorioInscricaoJdbc(c); inscricoes.vincularEstrutura(matricula,estrutura);
        sql("INSERT INTO disciplina_prerequisito(id_disciplina,id_disciplina_prerequisito) VALUES (?,?)",seguinte,destino);
        turma=sql("INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,2,'ATIVO')",seguinte,professor,periodo,tag);
        sql("INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,'08:00','09:00')",turma,sala);
        repo=new RepositorioEquivalenciaJdbc(c);
    }
    @AfterEach void limpar() throws Exception {
        if(c==null) return;
        try {
            if(!c.getAutoCommit()) c.rollback(); c.setAutoCommit(true);
            sql("DELETE FROM pedido_equivalencia WHERE id_matricula IN (SELECT id_matricula FROM matricula_academica WHERE id_aluno=?)",aluno);
            sql("DELETE FROM aproveitamento_equivalencia WHERE id_matricula=?",matricula);
            sql("DELETE FROM item_plano_estudo WHERE id_plano IN (SELECT id_plano FROM plano_estudo WHERE id_matricula=?)",matricula);
            sql("DELETE FROM plano_estudo WHERE id_matricula=?",matricula);
            sql("DELETE r FROM resultado_inscricao r JOIN inscricao_disciplina i ON i.id_inscricao=r.id_inscricao WHERE i.id_matricula=?",matricula);
            sql("DELETE FROM inscricao_disciplina WHERE id_matricula=?",matricula);
            sql("DELETE v FROM vinculo_curricular_matricula v JOIN matricula_academica m ON m.id_matricula=v.id_matricula WHERE m.id_aluno=?",aluno);
            sql("DELETE FROM matricula_academica WHERE id_aluno=?",aluno);
            sql("DELETE FROM horario_aula WHERE id_turma=?",turma); sql("DELETE FROM turma WHERE id_turma=?",turma);
            sql("DELETE FROM disciplina_prerequisito WHERE id_disciplina=?",seguinte);
            sql("DELETE FROM estrutura_disciplina WHERE id_estrutura=?",estrutura); sql("DELETE FROM estrutura_curricular WHERE id_estrutura=?",estrutura);
            sql("DELETE FROM disciplina WHERE id_disciplina IN (?,?)",destino,seguinte);
            sql("DELETE FROM periodo_letivo WHERE id_periodo=?",periodo); sql("DELETE FROM aluno WHERE id_aluno=?",aluno);
            sql("DELETE FROM curso WHERE id_curso=?",curso); sql("DELETE FROM professor WHERE id_professor=?",professor); sql("DELETE FROM sala WHERE id_sala=?",sala);
        } finally { c.close(); }
    }
    @Test void deferimentoProduzHistoricoPlanoEPreRequisito() throws Exception {
        assertFalse(inscricoes.avaliar(matricula,turma).permite());
        var p=repo.solicitar(pedido()); assertEquals("EM_ANALISE",p.situacao());
        assertFalse(inscricoes.avaliar(matricula,turma).permite());
        assertEquals("PENDENTE",repo.plano(matricula).itens().getFirst().situacao());
        var d=repo.decidir(p.id(),decisao(true)); assertNotNull(d.decididoEm());
        assertEquals("RECONHECIDA",repo.plano(matricula).itens().getFirst().situacao());
        assertEquals(2,repo.historico(matricula).size()); assertTrue(inscricoes.avaliar(matricula,turma).permite());
        var i=inscricoes.inscrever(matricula,turma); assertEquals("ATIVO",i.situacao());
        assertEquals("EM_ANDAMENTO",repo.plano(matricula).itens().getLast().situacao());
        assertEquals(1,inscricoes.listar(matricula).size());
    }
    @Test void indeferimentoPreservaPedidoSemCumprimentoEPermiteNovoPedido() throws Exception {
        var p=repo.solicitar(pedido()); repo.decidir(p.id(),decisao(false));
        assertEquals("PENDENTE",repo.plano(matricula).itens().getFirst().situacao());
        assertFalse(inscricoes.avaliar(matricula,turma).permite());
        repo.solicitar(pedido()); assertEquals(2,repo.listar(matricula).size());
    }
    @Test void decisaoFinalNaoPodeSerReescrita() throws Exception {
        var p=repo.solicitar(pedido()); var d=repo.decidir(p.id(),decisao(true));
        assertThrows(IllegalArgumentException.class,()->repo.decidir(p.id(),decisao(false)));
        assertEquals(d,repo.listar(matricula).getFirst());
    }
    @Test void duplicidadeEFuturoNaoGravamPedido() throws Exception {
        var p=pedido(); var futuro=new Pedido(p.matricula(),p.destino(),p.instituicao(),p.origem(),p.cargaOrigem(),p.ementaOrigem(),p.documentos(),hoje().plusDays(1),p.ementaDestino());
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(futuro)); assertTrue(repo.listar(matricula).isEmpty());
        repo.solicitar(p); assertThrows(IllegalArgumentException.class,()->repo.solicitar(p));
        assertEquals(1,repo.listar(matricula).size());
    }
    @Test void revalidaMatriculaEDestinoNaDecisao() throws Exception {
        var p=repo.solicitar(pedido()); sql("UPDATE matricula_academica SET situacao='TRANCADO' WHERE id_matricula=?",matricula);
        assertThrows(IllegalArgumentException.class,()->repo.decidir(p.id(),decisao(true)));
        sql("UPDATE matricula_academica SET situacao='ATIVO' WHERE id_matricula=?",matricula);
        sql("UPDATE disciplina SET carga_horaria=80 WHERE id_disciplina=?",destino);
        assertThrows(IllegalArgumentException.class,()->repo.decidir(p.id(),decisao(true)));
        assertEquals("EM_ANALISE",repo.listar(matricula).getFirst().situacao());
    }
    @Test void inscricaoNoDestinoImpedeDeferimentoSemCancelar() throws Exception {
        var p=repo.solicitar(pedido()); sql("UPDATE turma SET id_disciplina=? WHERE id_turma=?",destino,turma);
        var i=inscricoes.inscrever(matricula,turma);
        assertThrows(IllegalArgumentException.class,()->repo.decidir(p.id(),decisao(true)));
        assertEquals(List.of(i),inscricoes.listar(matricula)); repo.decidir(p.id(),decisao(false));
    }
    @Test void reconhecidaNaoAdmiteNovaInscricaoNoDestino() throws Exception {
        var p=repo.solicitar(pedido()); repo.decidir(p.id(),decisao(true));
        sql("UPDATE turma SET id_disciplina=? WHERE id_turma=?",destino,turma);
        assertFalse(inscricoes.avaliar(matricula,turma).permite());
        assertThrows(IllegalArgumentException.class,()->inscricoes.inscrever(matricula,turma));
    }
    @Test void reconhecimentoNaoTransportaParaReingresso() throws Exception {
        repo.decidir(repo.solicitar(pedido()).id(),decisao(true));
        long outra=sql("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",aluno,curso,periodo);
        inscricoes.vincularEstrutura(outra,estrutura);
        assertFalse(inscricoes.avaliar(outra,turma).permite()); assertEquals("PENDENTE",repo.plano(outra).itens().getFirst().situacao());
    }
    @Test void legadoNaoViraReconhecimentoNemERegravado() throws Exception {
        sql("INSERT INTO aproveitamento_equivalencia(id_matricula,id_disciplina_destino,decisao,data,resultado) VALUES (?,?,'DEFERIDO',CURRENT_DATE,'legado')",matricula,destino);
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(pedido()));
        assertThrows(IllegalArgumentException.class,()->repo.plano(matricula));
        assertEquals("LEGADO_NAO_CONCILIADO",repo.historico(matricula).getFirst().tipo());
        assertThrows(IllegalArgumentException.class,()->inscricoes.avaliar(matricula,turma));
    }
    @Test void planoLegadoNaoEAdotadoSilenciosamente() throws Exception {
        sql("INSERT INTO plano_estudo(id_matricula,id_estrutura,data_atualizacao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",matricula,estrutura);
        assertThrows(IllegalArgumentException.class,()->repo.plano(matricula));
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(pedido()));
    }
    @Test void transacaoExternaPodeDesfazerPedidoEDecisao() throws Exception {
        c.setAutoCommit(false); var p=repo.solicitar(pedido()); repo.decidir(p.id(),decisao(true));
        assertFalse(c.getAutoCommit()); assertEquals("RECONHECIDA",repo.plano(matricula).itens().getFirst().situacao());
        c.rollback(); c.setAutoCommit(true); assertTrue(repo.listar(matricula).isEmpty());
        assertEquals("PENDENTE",repo.plano(matricula).itens().getFirst().situacao());
    }
    @Test void falhaDepoisDeUpdateDesfazDecisao() throws Exception {
        var p=repo.solicitar(pedido());
        Connection proxy=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,metodo,args)->{
            try {
                Object result=metodo.invoke(c,args);
                if(metodo.getName().equals("prepareStatement") && args[0].toString().startsWith("UPDATE pedido_equivalencia")) {
                    var original=(PreparedStatement)result;
                    return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(o,m,a)->{
                        try { Object valor=m.invoke(original,a); if(m.getName().equals("executeUpdate")) throw new SQLException("Falha após atualização"); return valor; }
                        catch(InvocationTargetException e) { throw e.getCause(); }
                    });
                }
                return result;
            } catch(InvocationTargetException e) { throw e.getCause(); }
        });
        assertThrows(PersistenciaException.class,()->new RepositorioEquivalenciaJdbc(proxy).decidir(p.id(),decisao(true)));
        assertEquals("EM_ANALISE",repo.listar(matricula).getFirst().situacao());
        assertEquals(1,repo.historico(matricula).size()); assertEquals("PENDENTE",repo.plano(matricula).itens().getFirst().situacao());
    }
    @Test void duasDecisoesConcorrentesSomenteUmaFinaliza() throws Exception {
        var p=repo.solicitar(pedido()); var inicio=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> tarefa=()->{ try(var outra=ApoioJdbc.abrir(false)) { inicio.await();
                try { new RepositorioEquivalenciaJdbc(outra).decidir(p.id(),decisao(true)); return true; }
                catch(IllegalArgumentException e) { return false; }
            }};
            var a=executor.submit(tarefa); var b=executor.submit(tarefa); inicio.countDown();
            assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));
        }
        assertEquals("DEFERIDO",repo.listar(matricula).getFirst().situacao()); assertEquals(2,repo.historico(matricula).size());
    }
    @Test void deferimentoConcorrenteComInscricaoNoDestinoNaoProduzAmbos() throws Exception {
        var p=repo.solicitar(pedido()); sql("UPDATE turma SET id_disciplina=? WHERE id_turma=?",destino,turma);
        var inicio=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{ try(var outra=ApoioJdbc.abrir(false)) { inicio.await();
                try { new RepositorioEquivalenciaJdbc(outra).decidir(p.id(),decisao(true)); return true; }
                catch(IllegalArgumentException e) { return false; }
            }});
            var b=executor.submit(()->{ try(var outra=ApoioJdbc.abrir(false)) { inicio.await();
                try { new RepositorioInscricaoJdbc(outra).inscrever(matricula,turma); return true; }
                catch(IllegalArgumentException e) { return false; }
            }});
            inicio.countDown(); assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));
        }
        assertNotEquals("DEFERIDO".equals(repo.listar(matricula).getFirst().situacao()),!inscricoes.listar(matricula).isEmpty());
    }
    @Test void reprovacaoAnteriorPermaneceAposReconhecimento() throws Exception {
        sql("UPDATE turma SET id_disciplina=? WHERE id_turma=?",destino,turma);
        var i=inscricoes.inscrever(matricula,turma);
        sql("UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",hoje().minusDays(100),hoje().minusDays(1),periodo);
        inscricoes.registrarResultado(i.id(),BigDecimal.TEN,BigDecimal.TEN,null,100,70,"Frequência informada em minutos; fixture");
        assertEquals("PENDENTE",repo.plano(matricula).itens().getFirst().situacao());
        repo.decidir(repo.solicitar(pedido()).id(),decisao(true));
        assertEquals("REPROVADO",inscricoes.listar(matricula).getFirst().situacao());
        assertEquals(3,repo.historico(matricula).size());
        assertEquals("RECONHECIDA",repo.plano(matricula).itens().getFirst().situacao());
    }
    @Test void aprovacaoAnteriorEConclusaoNoPlanoSemPedidoArtificial() throws Exception {
        sql("UPDATE turma SET id_disciplina=? WHERE id_turma=?",destino,turma);
        var i=inscricoes.inscrever(matricula,turma);
        sql("UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",hoje().minusDays(100),hoje().minusDays(1),periodo);
        inscricoes.registrarResultado(i.id(),BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Apuração em minutos; fixture");
        assertEquals("CONCLUIDA",repo.plano(matricula).itens().getFirst().situacao());
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(pedido()));
        assertTrue(repo.listar(matricula).isEmpty()); assertEquals("RESULTADO",repo.historico(matricula).getFirst().tipo());
    }
    @Test void planoPreservaCumprimentoAoTrancarSemAutorizarNovasOperacoes() throws Exception {
        repo.decidir(repo.solicitar(pedido()).id(),decisao(true));
        sql("UPDATE matricula_academica SET situacao='TRANCADO' WHERE id_matricula=?",matricula);
        assertEquals("TRANCADO",repo.plano(matricula).situacaoMatricula());
        assertEquals("RECONHECIDA",repo.plano(matricula).itens().getFirst().situacao());
        assertThrows(IllegalArgumentException.class,()->inscricoes.inscrever(matricula,turma));
    }
    @Test void destinoForaDaEstruturaOuInativoNaoAdmitePedido() throws Exception {
        sql("DELETE FROM estrutura_disciplina WHERE id_estrutura=? AND id_disciplina=?",estrutura,destino);
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(pedido()));
        sql("INSERT INTO estrutura_disciplina(id_estrutura,id_disciplina) VALUES (?,?)",estrutura,destino);
        sql("UPDATE disciplina SET situacao='INATIVO' WHERE id_disciplina=?",destino);
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(pedido())); assertTrue(repo.listar(matricula).isEmpty());
    }
    @Test void falhaDeDecisaoPreservaAlteracoesAnterioresDaTransacaoExterna() throws Exception {
        var p=repo.solicitar(pedido()); c.setAutoCommit(false);
        sql("UPDATE aluno SET identificacao=? WHERE id_aluno=?",tag+"-alterado",aluno);
        sql("UPDATE matricula_academica SET situacao='TRANCADO' WHERE id_matricula=?",matricula);
        assertThrows(IllegalArgumentException.class,()->repo.decidir(p.id(),decisao(true)));
        assertFalse(c.getAutoCommit());
        try(var s=c.prepareStatement("SELECT identificacao FROM aluno WHERE id_aluno=?")) {
            s.setLong(1,aluno); try(var r=s.executeQuery()) { r.next(); assertEquals(tag+"-alterado",r.getString(1)); }
        }
        c.rollback(); c.setAutoCommit(true); assertEquals("EM_ANALISE",repo.listar(matricula).getFirst().situacao());
    }
    @Test void estadoLegadoDeInscricaoNaoGeraConclusaoNoPlano() throws Exception {
        sql("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'APROVADO')",matricula,turma);
        assertThrows(IllegalArgumentException.class,()->repo.plano(matricula));
    }
    @Test void consoleDesistenciaEEofNaoGravam() throws Exception {
        new ConsoleEquivalencia(new Scanner("1\n"+matricula+"\n"+destino+"\nInstituição\nOrigem\n60\nEmenta\nProtocolo\n2020-01-01\nDestino\nN\n0\n"),new ServicoEquivalencia(repo)).executar();
        new ConsoleEquivalencia(new Scanner("1\n"+matricula+"\n"),new ServicoEquivalencia(repo)).executar();
        assertTrue(repo.listar(matricula).isEmpty());
    }
}

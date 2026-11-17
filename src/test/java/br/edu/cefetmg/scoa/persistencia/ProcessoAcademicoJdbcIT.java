package br.edu.cefetmg.scoa.persistencia;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import static org.junit.jupiter.api.Assertions.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;
import br.edu.cefetmg.scoa.apresentacao.ConsoleProcessoAcademico;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

class ProcessoAcademicoJdbcIT {
    private Connection c;
    private SegurancaJdbc.Sessao sessao;
    private RepositorioProcessoAcademicoJdbc processos;
    private RepositorioEquivalenciaJdbc eq;
    private RepositorioInscricaoJdbc ins;
    private long aluno,curso,e,m,a,b,p,t,prof,sala;
    private String tag;
    private boolean commitFixture;
    private LocalDate hoje;
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); sessao=AcessoTeste.sessao(c); c.setAutoCommit(false);
        processos=new RepositorioProcessoAcademicoJdbc(c); eq=new RepositorioEquivalenciaJdbc(c,sessao); ins=new RepositorioInscricaoJdbc(c);
        tag="IT10-"+UUID.randomUUID().toString().substring(0,12);
        hoje=q(c,"SELECT DATE(UTC_TIMESTAMP() - INTERVAL 3 HOUR)",r->r.getObject(1,LocalDate.class)).getFirst();
        aluno=g(c,"INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        curso=g(c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
        a=g(c,"INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+"A",tag+"A");
        b=g(c,"INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+"B",tag+"B");
        prof=g(c,"INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        sala=g(c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES (?,'Fixture',10,'ATIVO')",tag);
        p=g(c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",tag,hoje.plusDays(10),hoje.plusDays(90));
        m=g(c,"INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,?,?,'ATIVO')",aluno,curso,hoje,p);
        e=g(c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,tag);
        g(c,"INSERT INTO estrutura_disciplina(id_estrutura,id_disciplina) VALUES (?,?),(?,?)",e,a,e,b);
        g(c,"INSERT INTO disciplina_prerequisito VALUES (?,?)",b,a);
        ins.vincularEstrutura(m,e);
        t=g(c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,10,'ATIVO')",b,prof,p,tag);
        g(c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,'08:00','09:00')",t,sala);
    }
    @AfterEach void limpar() throws Exception {
        if(c==null) return;
        try {
            c.rollback();
            if(commitFixture) {
                g(c,"DELETE ip FROM inscricao_preservada_revisao ip JOIN revisao_reconhecimento r ON r.id_revisao=ip.id_revisao JOIN pedido_equivalencia p ON p.id_pedido=r.id_pedido WHERE p.id_matricula=?",m);
                g(c,"DELETE r FROM revisao_reconhecimento r JOIN pedido_equivalencia p ON p.id_pedido=r.id_pedido WHERE p.id_matricula=?",m);
                g(c,"DELETE er FROM evidencia_requisito_inscricao er JOIN inscricao_disciplina i ON i.id_inscricao=er.id_inscricao WHERE i.id_matricula=?",m);
                g(c,"DELETE FROM pedido_equivalencia WHERE id_matricula=?",m);
                g(c,"DELETE FROM inscricao_disciplina WHERE id_matricula=?",m);
                g(c,"DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",m);
                g(c,"DELETE FROM matricula_academica WHERE id_matricula=?",m);
                g(c,"DELETE FROM horario_aula WHERE id_turma=?",t); g(c,"DELETE FROM turma WHERE id_turma=?",t);
                g(c,"DELETE FROM disciplina_prerequisito WHERE id_disciplina=?",b);
                g(c,"DELETE FROM estrutura_disciplina WHERE id_estrutura=?",e); g(c,"DELETE FROM estrutura_curricular WHERE id_estrutura=?",e);
                g(c,"DELETE FROM disciplina WHERE id_disciplina IN (?,?)",a,b);
                g(c,"DELETE FROM periodo_letivo WHERE id_periodo=?",p); g(c,"DELETE FROM aluno WHERE id_aluno=?",aluno); g(c,"DELETE FROM curso WHERE id_curso=?",curso);
                g(c,"DELETE FROM professor WHERE id_professor=?",prof); g(c,"DELETE FROM sala WHERE id_sala=?",sala); c.commit();
            }
        } finally { c.close(); }
    }
    @AfterAll static void limparAcesso() throws Exception { AcessoTeste.limpar(); }
    private Pedido material() { return new Pedido(m,a,"Instituição sintética","Origem sintética",60,"Ementa","Referências "+tag,hoje.minusYears(1),"Destino"); }
    private long reconhecer() { var pedido=eq.solicitar(material()); eq.decidir(pedido.id(),new Decisao(true,"Parecer sintético","Nome não usado")); return pedido.id(); }
    private long legado() throws SQLException { return g(c,"INSERT INTO aproveitamento_equivalencia(id_matricula,id_disciplina_destino,decisao,data,resultado) VALUES (?,?,'DEFERIDO',?,'Registro sintético sem promoção automática')",m,a,hoje.minusYears(1)); }
    private long abrir(long pedido) { return processos.solicitarRevisao(sessao,pedido,"Revisão sintética"); }
    private void decidir(long revisao,String acao) { var previa=processos.previa(sessao,revisao); processos.decidirRevisao(sessao,revisao,acao,"Parecer sintético "+acao,previa.assinatura()); }
    private void janela() throws SQLException { g(c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",hoje.plusDays(10),hoje.plusDays(90),p); }
    @Test void sessaoAusenteEncerradaOuDeOutraConexaoNaoAutoriza() throws Exception {
        long pedido=reconhecer(); assertThrows(IllegalArgumentException.class,()->processos.solicitarRevisao(null,pedido,"Motivo"));
        try(var outra=ApoioJdbc.abrir(false)) { var alheia=AcessoTeste.sessao(outra); assertThrows(IllegalArgumentException.class,()->processos.solicitarRevisao(alheia,pedido,"Motivo")); }
        sessao.encerrar(); assertThrows(IllegalArgumentException.class,()->abrir(pedido));
    }
    @Test void solicitanteNaoDecideEPermissaoRevogadaTemEfeitoImediato() throws Exception {
        var seg=new SegurancaJdbc(c); String login=tag+"U"; seg.criarConta(sessao,login,"Senha-fixture-01234".toCharArray(),"SOLICITANTE");
        var limitada=seg.entrar(login,"Senha-fixture-01234".toCharArray()); long pedido=reconhecer(); long revisao=processos.solicitarRevisao(limitada,pedido,"Motivo");
        assertThrows(IllegalArgumentException.class,()->processos.previa(limitada,revisao));
        g(c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",limitada.usuario());
        assertThrows(IllegalArgumentException.class,()->processos.solicitarConciliacao(limitada,"PLANO",1,m,e,"Docs"));
    }
    @Test void consoleListaApenasComPermissaoESessaoAtiva() {
        long pedido = reconhecer();
        long revisao = abrir(pedido);
        var seguranca = new SegurancaJdbc(c);
        String login = tag + "U";
        seguranca.criarConta(sessao, login, "Senha-fixture-01234".toCharArray(), "SOLICITANTE");
        var solicitante = seguranca.entrar(login, "Senha-fixture-01234".toCharArray());
        String linha = "REVISAO " + revisao + " pedido=" + pedido + " situação=ABERTA parecer=null";
        String listar = "9\n" + m + "\n";

        String semSessao = executarConsole(null, listar + "0\n");
        assertTrue(semSessao.contains("Entre com uma sessão válida no menu 18."));
        assertFalse(semSessao.contains(linha));
        String negado = executarConsole(solicitante, listar + "0\n");
        assertTrue(negado.contains("Sem permissão: SCOA_PROCESSO_DECIDIR"));
        assertFalse(negado.contains(linha));

        String autorizado = executarConsole(sessao, listar + "3\n" + listar + "0\n");
        assertTrue(autorizado.contains(linha));
        assertEquals(1, autorizado.lines().filter(l -> l.contains(linha)).count());
        assertTrue(autorizado.indexOf("Entre com uma sessão válida no menu 18.")
                > autorizado.indexOf("Sessão encerrada."));
    }

    private String executarConsole(SegurancaJdbc.Sessao sessaoInicial, String opcoes) {
        var original = System.out;
        var bytes = new ByteArrayOutputStream();
        try (var saida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
             var entrada = new Scanner(opcoes)) {
            System.setOut(saida);
            new ConsoleProcessoAcademico(entrada, c, sessaoInicial).executar();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void senhaErradaELegadoNaoAutenticam() {
        assertThrows(IllegalArgumentException.class,()->new SegurancaJdbc(c).entrar(tag,"Incorreta-012345".toCharArray()));
        assertThrows(IllegalArgumentException.class,()->new RepositorioEquivalenciaJdbc(c).solicitar(material()));
    }
    @Test void revisaoSuspendeInscricaoESimulacaoSemAlterarDecisaoOriginal() throws Exception {
        long pedido=reconhecer(); janela(); assertTrue(ins.avaliar(m,t).permite()); abrir(pedido);
        assertTrue(ins.avaliar(m,t).motivos().toString().contains("sob revisão"));
        assertThrows(IllegalArgumentException.class,()->ins.inscrever(m,t));
        var plano=eq.plano(m); assertEquals("SOB_REVISAO",plano.itens().getFirst().situacao());
        var sim=new RepositorioPlanejamentoJdbc(c).simular(m,List.of(p),List.of());
        assertNull(sim.itens().getLast().sugestao()); assertTrue(sim.itens().getLast().tentativas().toString().contains("sob revisão"));
        assertEquals("DEFERIDO",eq.listar(m).getFirst().situacao());
    }
    @Test void rejeicaoRestauraUsoEPermiteNovaRevisao() throws Exception {
        long pedido=reconhecer(); long rev=abrir(pedido); decidir(rev,"REJEITAR");
        assertEquals("VIGENTE",RepositorioProcessoAcademicoJdbc.estado(c,pedido)); assertTrue(abrir(pedido)>rev);
    }
    @Test void complementacaoETratamentoMantemSuspensaoEEventos() throws Exception {
        long pedido=reconhecer(); long rev=abrir(pedido); decidir(rev,"COMPLEMENTAR");
        assertEquals("COMPLEMENTACAO",processos.previa(sessao,rev).situacao()); decidir(rev,"TRATAR");
        assertEquals("SOB_REVISAO",RepositorioProcessoAcademicoJdbc.estado(c,pedido));
        assertEquals(3,q(c,"SELECT id_evento FROM evento_processo_academico WHERE tipo='REVISAO' AND id_processo=?",r->r.getLong(1),rev).size());
    }
    @Test void revogacaoPreservaInscricaoRastreadaEHistorico() throws Exception {
        long pedido=reconhecer(); janela(); long inscricao=ins.inscrever(m,t).id();
        assertEquals(pedido,q(c,"SELECT id_pedido FROM evidencia_requisito_inscricao WHERE id_inscricao=?",r->r.getLong(1),inscricao).getFirst());
        long rev=abrir(pedido); assertEquals("COMPROVADO",processos.previa(sessao,rev).impactos().getFirst().fundamento()); decidir(rev,"REVOGAR_PRESERVANDO");
        assertEquals("ATIVO",ins.listar(m).getFirst().situacao()); assertEquals("DEFERIDO",eq.listar(m).getFirst().situacao());
        assertEquals("REVOGADA",RepositorioProcessoAcademicoJdbc.estado(c,pedido));
        assertEquals(inscricao,q(c,"SELECT id_inscricao FROM inscricao_preservada_revisao WHERE id_revisao=?",r->r.getLong(1),rev).getFirst());
        assertThrows(IllegalArgumentException.class,()->decidir(rev,"REJEITAR"));
        assertTrue(eq.historico(m).stream().anyMatch(x->x.tipo().equals("REVISAO")));
    }
    @Test void historicoSemProvaNaoGanhaCausalidade() throws Exception {
        long pedido=reconhecer(); g(c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'CANCELADO')",m,t,hoje);
        long rev=abrir(pedido); assertEquals("POTENCIAL_HISTORICO",processos.previa(sessao,rev).impactos().getFirst().fundamento());
        decidir(rev,"REVOGAR_PRESERVANDO"); assertEquals("CANCELADO",ins.listar(m).getFirst().situacao());
    }
    @Test void previaDesatualizadaNaoDecide() throws Exception {
        long rev=abrir(reconhecer()); var previa=processos.previa(sessao,rev);
        g(c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",m,t,hoje);
        assertThrows(IllegalArgumentException.class,()->processos.decidirRevisao(sessao,rev,"REVOGAR_PRESERVANDO","Parecer",previa.assinatura()));
        assertEquals("ABERTA",processos.previa(sessao,rev).situacao());
    }
    @Test void duplicidadeEParecerVazioNaoCriamDecisao() {
        long pedido=reconhecer(); long rev=abrir(pedido);
        assertThrows(IllegalArgumentException.class,()->abrir(pedido));
        assertThrows(IllegalArgumentException.class,()->processos.decidirRevisao(sessao,rev,"REJEITAR"," ","x"));
        assertThrows(IllegalArgumentException.class,()->processos.decidirRevisao(sessao,rev,"CANCELAR","Parecer","x"));
    }
    @Test void resultadoFormalAlternativoPermiteNovoUsoSemReconhecimentoSuspenso() throws Exception {
        long pedido=reconhecer(); abrir(pedido);
        long passado=g(c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ENCERRADO')",tag+"Antigo",hoje.minusDays(100),hoje.minusDays(10));
        long antiga=g(c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,10,'ATIVO')",a,prof,passado,tag+"Antiga");
        long inscricao=g(c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'APROVADO')",m,antiga,hoje.minusDays(90));
        g(c,"INSERT INTO resultado_inscricao(id_inscricao,p1,p2,total_minutos,presentes_minutos,media_inicial,situacao,motivo) VALUES (?,8,8,60,60,8,'APROVADO','Fixture')",inscricao);
        long nova=ins.inscrever(m,t).id();
        assertEquals("RESULTADO",q(c,"SELECT tipo FROM evidencia_requisito_inscricao WHERE id_inscricao=?",r->r.getString(1),nova).getFirst());
        assertEquals("CONCLUIDA",eq.plano(m).itens().getFirst().situacao());
    }
    @Test void suspensaoNaoBloqueiaMatriculaInteira() throws Exception {
        abrir(reconhecer()); g(c,"DELETE FROM disciplina_prerequisito WHERE id_disciplina=?",b);
        assertEquals("ATIVO",ins.inscrever(m,t).situacao());
    }
    @Test void reconhecimentoRevogadoNaoImpedeCursarAPropriaDisciplina() throws Exception {
        long pedido=reconhecer();
        long origem=g(c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,10,'ATIVO')",a,prof,p,tag+"Origem");
        g(c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,2,'08:00','09:00')",origem,sala);
        assertFalse(ins.avaliar(m,origem).permite()); decidir(abrir(pedido),"REVOGAR_PRESERVANDO");
        assertTrue(ins.avaliar(m,origem).permite());
    }
    @Test void sessaoRevalidaPermissaoAposRetiradaDaConcessao() throws Exception {
        var seg=new SegurancaJdbc(c); String login=tag+"Analista";
        long usuario=seg.criarConta(sessao,login,"Senha-fixture-01234".toCharArray(),"ANALISTA");
        var analista=seg.entrar(login,"Senha-fixture-01234".toCharArray()); long rev=abrir(reconhecer());
        assertNotNull(processos.previa(analista,rev));
        g(c,"DELETE FROM usuario_perfil WHERE id_usuario=?",usuario);
        assertThrows(IllegalArgumentException.class,()->processos.previa(analista,rev));
    }
    @Test void conciliacaoEquivalenciaExigeMaterialEPreservaOrigem() throws Exception {
        long origem=legado(); String hash=RepositorioProcessoAcademicoJdbc.origem(c,"EQUIVALENCIA",origem,m);
        assertThrows(IllegalArgumentException.class,()->eq.plano(m));
        long conc=processos.solicitarConciliacao(sessao,"EQUIVALENCIA",origem,m,e,"Docs sintéticos");
        assertThrows(IllegalArgumentException.class,()->processos.decidirConciliacao(sessao,conc,true,"Parecer",null));
        processos.decidirConciliacao(sessao,conc,true,"Análise documental sintética",material());
        assertEquals(hash,RepositorioProcessoAcademicoJdbc.origem(c,"EQUIVALENCIA",origem,m));
        assertEquals("RECONHECIDA",eq.plano(m).itens().getFirst().situacao());
        assertThrows(IllegalArgumentException.class,()->processos.solicitarConciliacao(sessao,"EQUIVALENCIA",origem,m,e,"Docs"));
    }
    @Test void conciliacaoPlanoNaoConcluiDisciplinasEVinculaContextoExplicito() throws Exception {
        g(c,"DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",m);
        long plano=g(c,"INSERT INTO plano_estudo(id_matricula,id_estrutura,data_atualizacao,situacao) VALUES (?,?,?,'ATIVO')",m,e,hoje);
        g(c,"INSERT INTO item_plano_estudo(id_plano,id_disciplina,situacao) VALUES (?,?,'CONCLUIDA')",plano,a);
        long conc=processos.solicitarConciliacao(sessao,"PLANO",plano,m,e,"Contexto revisado");
        processos.decidirConciliacao(sessao,conc,true,"Plano não comprova conclusão",null);
        assertEquals("PENDENTE",eq.plano(m).itens().getFirst().situacao());
        g(c,"UPDATE item_plano_estudo SET situacao='PENDENTE' WHERE id_plano=?",plano);
        assertThrows(IllegalArgumentException.class,()->eq.plano(m));
    }
    @Test void origemAlteradaBloqueiaAprovacaoSemGravacaoParcial() throws Exception {
        long origem=legado(); long conc=processos.solicitarConciliacao(sessao,"EQUIVALENCIA",origem,m,e,"Docs");
        g(c,"UPDATE aproveitamento_equivalencia SET resultado='Alterado' WHERE id_aproveitamento=?",origem);
        assertThrows(IllegalArgumentException.class,()->processos.decidirConciliacao(sessao,conc,true,"Parecer",material()));
        assertTrue(eq.listar(m).isEmpty()); assertEquals("ABERTA",q(c,"SELECT situacao FROM conciliacao_legado WHERE id_conciliacao=?",r->r.getString(1),conc).getFirst());
    }
    @Test void rejeicaoConciliacaoNaoPromoveLegado() throws Exception {
        long conc=processos.solicitarConciliacao(sessao,"EQUIVALENCIA",legado(),m,e,"Docs");
        processos.decidirConciliacao(sessao,conc,false,"Documentação insuficiente",null);
        assertThrows(IllegalArgumentException.class,()->eq.plano(m)); assertTrue(eq.listar(m).isEmpty());
    }
    @Test void transacaoExternaContinuaPertencendoAoChamador() throws Exception {
        long pedido=reconhecer(); var ponto=c.setSavepoint(); long rev=abrir(pedido); assertFalse(c.getAutoCommit()); c.rollback(ponto);
        assertEquals("VIGENTE",RepositorioProcessoAcademicoJdbc.estado(c,pedido));
        assertTrue(q(c,"SELECT id_revisao FROM revisao_reconhecimento WHERE id_revisao=?",r->r.getLong(1),rev).isEmpty());
    }
    @Test void aberturaConcorrenteComInscricaoRevalidaSobMesmoControle() throws Exception {
        long pedido=reconhecer(); janela(); c.commit(); c.setAutoCommit(true); commitFixture=true;
        c.setAutoCommit(false); long rev=abrir(pedido);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var iniciou=new CountDownLatch(1);
            var tentativa=executor.submit(()->{ try(var outra=ApoioJdbc.abrir(false)) {
                iniciou.countDown();
                try { new RepositorioInscricaoJdbc(outra).inscrever(m,t); return true; }
                catch(IllegalArgumentException bloqueada) { return false; }
            } });
            assertTrue(iniciou.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->tentativa.get(200,TimeUnit.MILLISECONDS));
            c.commit(); assertFalse(tentativa.get(10,TimeUnit.SECONDS));
        }
        assertTrue(ins.listar(m).isEmpty()); assertEquals("ABERTA",processos.previa(sessao,rev).situacao());
        c.setAutoCommit(false);
    }
    @Test void doisAnalistasNaoEncerramAMesmaRevisaoDuasVezes() throws Exception {
        long rev=abrir(reconhecer()); c.commit(); c.setAutoCommit(true); commitFixture=true;
        var previa=processos.previa(sessao,rev); c.setAutoCommit(false); controle(c);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var iniciou=new CountDownLatch(1);
            var segundo=executor.submit(()->{ try(var outra=ApoioJdbc.abrir(false)) {
                var s=AcessoTeste.sessao(outra); iniciou.countDown();
                try { new RepositorioProcessoAcademicoJdbc(outra).decidirRevisao(s,rev,"REVOGAR_PRESERVANDO","Segundo parecer",previa.assinatura()); return true; }
                catch(IllegalArgumentException bloqueada) { return false; }
            } });
            assertTrue(iniciou.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->segundo.get(200,TimeUnit.MILLISECONDS));
            processos.decidirRevisao(sessao,rev,"REJEITAR","Primeiro parecer",previa.assinatura()); c.commit();
            assertFalse(segundo.get(10,TimeUnit.SECONDS));
        }
        assertEquals("REJEITADA",processos.previa(sessao,rev).situacao());
    }
    @Test void falhaDeAuditoriaDesfazAberturaInteira() throws Exception {
        long pedido=reconhecer();
        var proxy=(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,metodo,args)->{
            if(metodo.getName().equals("prepareStatement") && ((String)args[0]).startsWith("INSERT INTO evento_processo_academico")) throw new SQLException("Falha sintética de auditoria");
            try { return metodo.invoke(c,args); } catch(java.lang.reflect.InvocationTargetException erro) { throw erro.getCause(); }
        });
        var s=AcessoTeste.sessao(proxy);
        assertThrows(PersistenciaException.class,()->new RepositorioProcessoAcademicoJdbc(proxy).solicitarRevisao(s,pedido,"Motivo"));
        assertEquals("VIGENTE",RepositorioProcessoAcademicoJdbc.estado(c,pedido));
    }
}

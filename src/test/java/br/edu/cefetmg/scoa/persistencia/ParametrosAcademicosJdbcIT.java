package br.edu.cefetmg.scoa.persistencia;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import static org.junit.jupiter.api.Assertions.*;

/** Alterações de datas por SQL simulam passagem de tempo na fixture, não operações do console. */
class ParametrosAcademicosJdbcIT {
    private CenarioAcademicoTeste f;
    @BeforeEach void preparar() throws Exception { f=new CenarioAcademicoTeste(); }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioParametrosAcademicosJdbc repo() { return new RepositorioParametrosAcademicosJdbc(f.c,f.sessao(f.admin)); }
    private Regras diferentes() { return new Regras(20,10,2,5,5,30,30,3,10,new BigDecimal("80"),new BigDecimal("8"),new BigDecimal("6")); }
    private Versao criar() { return repo().criar(new Criar(diferentes(),"Versão sintética conferida")); }
    private void datas(int inicio,int fim) throws Exception { g(f.c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",f.hoje.plusDays(inicio),f.hoje.plusDays(fim),f.periodo); }
    private Periodo selecionar(Long versao) { var p=repo().consultarPeriodo(f.periodo); return repo().vincular(new Vincular(p.id(),p.versaoVinculo(),versao,p.assinatura(),"Seleção sintética explícita")); }
    private Versao configurar() throws Exception { datas(30,130); var v=criar(); selecionar(v.id()); return v; }
    private long seguinte() throws Exception { return g(f.c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",f.tag.substring(0,32)+"-próximo",f.hoje.plusDays(60),f.hoje.plusDays(160)); }
    private void biblioteca() throws Exception { g(f.c,"INSERT INTO usuario_biblioteca(id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",f.alunoConta); }
    @Test void somenteConcessaoEspecificaOuAdminPermitemAdministrarRegras() throws Exception {
        f.conceder(f.alunoConta,AcessoAcademicoJdbc.MANTER);
        var sem=new RepositorioParametrosAcademicosJdbc(f.c,f.sessao(f.alunoConta));
        assertThrows(IllegalArgumentException.class,sem::versoes);
        assertThrows(IllegalArgumentException.class,()->sem.consultarPeriodo(f.periodo));
        assertThrows(IllegalArgumentException.class,()->sem.criar(new Criar(diferentes(),"Motivo")));
        f.conceder(f.alunoConta,AcessoAcademicoJdbc.PARAMETROS_MANTER);
        var v=sem.criar(new Criar(diferentes(),"Concessão explícita")); assertEquals(f.alunoConta,v.autor());
        assertTrue(new SegurancaJdbc(f.c).minhaConta(f.sessao(f.alunoConta)).vinculos().stream().allMatch(x->"ALUNO".equals(x.tipo())));
    }
    @Test void novaVersaoNaoAdotaAutomaticamentePeriodosAntigosOuFuturos() throws Exception {
        var antes=repo().consultarPeriodo(f.periodo); var v=criar();
        assertEquals(antes,repo().consultarPeriodo(f.periodo)); assertEquals(1,v.numero()); assertEquals(diferentes(),v.regras());
        datas(30,130); var futuro=repo().consultarPeriodo(f.periodo); assertNull(futuro.versaoParametros()); assertEquals(Regras.baseAprovada(),futuro.regras());
        assertTrue(repo().historico(f.periodo).isEmpty()); assertEquals(f.admin,repo().versoes().getFirst().autor());
    }
    @Test void vinculoFuturoVersionaHistoricoEPreservaInscricoes() throws Exception {
        datas(30,130); var v=criar(); var p=selecionar(v.id()); assertEquals(1,p.versaoVinculo()); assertEquals(v.id(),p.versaoParametros());
        assertEquals(1,p.inscricoes()); assertEquals(1,p.turmas()); assertEquals(diferentes(),p.regras());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
        var h=repo().historico(f.periodo).getFirst(); assertNull(h.anterior()); assertEquals(v.id(),h.nova()); assertEquals(f.admin,h.autor());
        selecionar(v.id()); assertEquals(1,repo().historico(f.periodo).size());
        selecionar(null); assertEquals(2,repo().historico(f.periodo).size()); assertEquals(Regras.baseAprovada(),ParametroPeriodoJdbc.porTurma(f.c,f.turma).regras());
    }
    @ParameterizedTest @ValueSource(ints={-100,0})
    void periodoIniciadoOuHojeNaoPodeTrocarSelecao(int inicio) throws Exception {
        var v=criar(); datas(inicio,100); assertThrows(IllegalArgumentException.class,()->selecionar(v.id())); assertTrue(repo().historico(f.periodo).isEmpty());
    }
    @Test void periodoInativoERecusado() throws Exception {
        datas(30,130); var v=criar(); g(f.c,"UPDATE periodo_letivo SET situacao='INATIVO' WHERE id_periodo=?",f.periodo);
        assertThrows(IllegalArgumentException.class,()->selecionar(v.id()));
    }
    @Test void previaAntigaNaoSobrescreveSelecaoOuMudancaDeContexto() throws Exception {
        datas(30,130); var v=criar(); var p=repo().consultarPeriodo(f.periodo);
        g(f.c,"UPDATE periodo_letivo SET descricao=? WHERE id_periodo=?",f.tag.substring(0,32)+"-alterado",f.periodo);
        assertThrows(IllegalArgumentException.class,()->repo().vincular(new Vincular(p.id(),p.versaoVinculo(),v.id(),p.assinatura(),"Tela antiga")));
        var atual=selecionar(v.id());
        assertThrows(IllegalArgumentException.class,()->repo().vincular(new Vincular(p.id(),p.versaoVinculo(),null,p.assinatura(),"Tela antiga")));
        assertEquals(atual,repo().consultarPeriodo(f.periodo));
    }
    @Test void integridadeDaVersaoEConferidaEmTodaLeitura() throws Exception {
        var v=configurar(); g(f.c,"UPDATE versao_parametros_academicos SET tolerancia_atraso=9 WHERE id_versao=?",v.id());
        assertThrows(IllegalArgumentException.class,()->repo().versoes()); assertThrows(IllegalArgumentException.class,()->repo().consultarPeriodo(f.periodo));
    }
    @Test void periodosComLancamentosNaoPodemTrocarRegraMesmoComDataFuturaIncoerente() throws Exception {
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Resultado sintético");
        datas(30,130); var v=criar(); assertThrows(IllegalArgumentException.class,()->selecionar(v.id()));
        assertNull(q(f.c,"SELECT id_versao_parametros FROM resultado_inscricao WHERE id_inscricao=?",r->new Long[]{r.getObject(1,Long.class)},f.inscricao).getFirst()[0]);
    }
    @Test void falhaNaAuditoriaDesfazVersaoEVinculoComHistorico() throws Exception {
        var falha=f.falharAuditoria(); var s=f.sessao(falha,f.admin); var repoFalha=new RepositorioParametrosAcademicosJdbc(falha,s);
        assertThrows(PersistenciaException.class,()->repoFalha.criar(new Criar(diferentes(),"Falha sintética"))); assertTrue(repo().versoes().isEmpty());
        datas(30,130); var v=criar(); var p=repo().consultarPeriodo(f.periodo);
        assertThrows(PersistenciaException.class,()->repoFalha.vincular(new Vincular(p.id(),p.versaoVinculo(),v.id(),p.assinatura(),"Falha sintética")));
        assertEquals(p,repo().consultarPeriodo(f.periodo)); assertTrue(repo().historico(f.periodo).isEmpty());
    }
    @Test void resultadoUsaSelecaoCongeladaMesmoAposCriarOutraVersao() throws Exception {
        var v=configurar(); datas(-100,-1);
        repo().criar(new Criar(Regras.baseAprovada(),"Nova versão para outros períodos"));
        var r=new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,100,75,"Aplicação da versão selecionada");
        assertEquals("REPROVADO",r.situacao());
        var origem=q(f.c,"SELECT id_versao_parametros,fonte_parametros FROM resultado_inscricao WHERE id_inscricao=?",x->List.of(x.getLong(1),x.getString(2)),f.inscricao).getFirst();
        assertEquals(List.of(v.id(),"VERSAO"),origem);
        assertThrows(IllegalArgumentException.class,()->selecionar(null));
    }
    @Test void resultadoAntigoNaoERecalculadoPorCriarVersao() throws Exception {
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("7"),new BigDecimal("7"),null,100,75,"Base aprovada");
        String antes=q(f.c,"SELECT CONCAT(p1,'|',p2,'|',situacao,'|',fonte_parametros) FROM resultado_inscricao WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst();
        criar(); assertEquals(antes,q(f.c,"SELECT CONCAT(p1,'|',p2,'|',situacao,'|',fonte_parametros) FROM resultado_inscricao WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
        assertTrue(antes.contains("APROVADO|BASE_APROVADA"));
    }
    @Test void retificacaoReutilizaRegraDaOrigemERegistraProveniencia() throws Exception {
        var v=configurar(); datas(-100,-1); seguinte(); g(f.c,"INSERT INTO coordenacao_curso(id_curso,id_usuario) VALUES (?,?)",f.curso,f.outroConta);
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,100,75,"Frequência insuficiente");
        repo().criar(new Criar(Regras.baseAprovada(),"Versão posterior não aplicável à origem"));
        var prof=new RepositorioRetificacaoJdbc(f.c,f.sessao(f.docente)); var proposta=new RetificacaoResultado(new BigDecimal("7"),new BigDecimal("7"),new BigDecimal("5"),100,80);
        assertEquals(new BigDecimal("6.0"),prof.preparar(f.inscricao,proposta).mediaFinal());
        long id=prof.solicitar(f.inscricao,proposta,"Correção com regras congeladas");
        new RepositorioRetificacaoJdbc(f.c,f.sessao(f.outroConta)).decidir(id,true,"Conferido");
        assertEquals(v.id(),q(f.c,"SELECT id_versao_parametros FROM retificacao_resultado WHERE id_pedido=?",r->r.getLong(1),id).getFirst());
        assertEquals("APROVADO",q(f.c,"SELECT situacao FROM resultado_inscricao WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
        assertEquals(v.id(),q(f.c,"SELECT id_versao_parametros FROM resultado_inscricao WHERE id_inscricao=?",r->r.getLong(1),f.inscricao).getFirst());
    }
    @Test void retificacaoRespeitaPrazoDocenteConfigurado() throws Exception {
        configurar(); datas(-100,-4); seguinte();
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Base para retificação");
        var prof=new RepositorioRetificacaoJdbc(f.c,f.sessao(f.docente));
        assertThrows(IllegalArgumentException.class,()->prof.solicitar(f.inscricao,new RetificacaoResultado(BigDecimal.TEN,BigDecimal.TEN,null,100,100),"Fora dos três dias"));
    }
    @Test void janelaDeInscricaoEExibicaoDoPlanejamentoUsamMesmaSelecao() throws Exception {
        configurar(); datas(18,118); biblioteca();
        var a=new RepositorioInscricaoJdbc(f.c).avaliar(f.outraMatricula,f.turma);
        assertTrue(a.motivos().stream().noneMatch(m->m.contains("Fora da janela")));
        var p=new RepositorioPlanejamentoJdbc(f.c).periodos().stream().filter(x->x.id()==f.periodo).findFirst().orElseThrow();
        assertEquals(diferentes(),p.regras()); assertTrue(p.regras().primeiraFase(p.inicio()).permite(f.hoje));
        datas(9,109); assertTrue(new RepositorioInscricaoJdbc(f.c).avaliar(f.outraMatricula,f.turma).motivos().stream().anyMatch(m->m.contains("Fora da janela")));
    }
    @Test void cancelamentoRespeitaPrazoConfiguradoDeTrintaDias() throws Exception {
        configurar(); datas(-31,69);
        assertThrows(IllegalArgumentException.class,()->new RepositorioCancelamentoJdbc(f.c,f.sessao(f.alunoConta)).solicitar(f.inscricao,"Prazo encerrado"));
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
    }
    @Test void cancelamentoConfiguradoMantemLiberacaoAteLimite() throws Exception {
        configurar(); datas(-5,95);
        assertEquals("AUTOMATICO",new RepositorioCancelamentoJdbc(f.c,f.sessao(f.alunoConta)).solicitar(f.inscricao,"No limite D+5").situacao());
        assertFalse(q(f.c,"SELECT vaga_retida FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getBoolean(1),f.inscricao).getFirst());
    }
    @Test void transferenciaUsaPrazoConfiguradoDaSelecao() throws Exception {
        configurar(); datas(-31,69); biblioteca();
        long curso=g(f.c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",f.tag+"-destino");
        long estrutura=g(f.c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,f.tag);
        var t=new RepositorioTransferenciaInternaJdbc(f.c,f.sessao(f.admin));
        assertThrows(IllegalArgumentException.class,()->t.efetivar(new TransferenciaInterna.Pedido(f.matricula,curso,estrutura,f.periodo,"Fora de D+30")));
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM matricula_academica WHERE id_matricula=?",r->r.getString(1),f.matricula).getFirst());
    }
    @Test void importacaoSimuladaUsaToleranciaDaTurma() throws Exception {
        configurar(); datas(-100,-1); var dia=f.hoje.minusDays(20);
        g(f.c,"UPDATE disciplina SET carga_horaria=5 WHERE id_disciplina=?",f.disciplina);
        for(int d=1;d<=7;d++) g(f.c,"INSERT INTO horario_aula(id_turma,dia_semana,hora_inicio,hora_fim) VALUES (?,?,'07:00','08:40')",f.turma,d);
        var diario=new RepositorioDiarioAcademicoJdbc(f.c,f.sessao(f.docente));
        var a=diario.registrarAulas(new NovaAula(f.turma,dia,List.of(TempoLetivo.M1,TempoLetivo.M2),"Pauta","Conteúdo"));
        var saida=diario.importarSimulados(List.of(new Chegada("Fixture","1",a.getFirst().id(),f.inscricao,dia.atTime(7,10)),new Chegada("Fixture","2",a.getLast().id(),f.inscricao,dia.atTime(8,1))));
        assertTrue(saida.stream().allMatch(x->"PROCESSADO".equals(x.situacao())));
        assertEquals(List.of("PRESENTE","FALTA"),diario.consultarDocente(f.inscricao).frequencias().stream().map(Frequencia::situacao).toList());
    }
    @Test void diarioConcordaComFechamentoNaFrequenciaConfigurada() throws Exception {
        datas(30,130);
        var v=repo().criar(new Criar(new Regras(14,7,7,7,13,45,45,7,15,new BigDecimal("90"),new BigDecimal("7"),new BigDecimal("5")),"Frequência sintética de 90%"));
        selecionar(v.id()); datas(-100,-1); var dia=f.hoje.minusDays(20);
        g(f.c,"UPDATE disciplina SET carga_horaria=5 WHERE id_disciplina=?",f.disciplina);
        for(int d=1;d<=7;d++) g(f.c,"INSERT INTO horario_aula(id_turma,dia_semana,hora_inicio,hora_fim) VALUES (?,?,'07:00','12:20')",f.turma,d);
        var diario=new RepositorioDiarioAcademicoJdbc(f.c,f.sessao(f.docente));
        var aulas=diario.registrarAulas(new NovaAula(f.turma,dia,TempoLetivo.doTurno(TempoLetivo.Turno.MANHA),"Pauta","Conteúdo"));
        for(var tipo:List.of(Prova.P1,Prova.P2)) {
            var p=diario.registrarAvaliacao(new NovaAvaliacao(f.turma,tipo,dia,"Avaliação"));
            diario.lancarNota(new LancarNota(p.id(),f.inscricao,BigDecimal.TEN,-1,"Lançamento"));
        }
        for(int n=0;n<aulas.size();n++) diario.lancarFrequencia(new LancarFrequencia(aulas.get(n).id(),f.inscricao,n==0?TempoLetivo.Presenca.FALTA:TempoLetivo.Presenca.PRESENTE,-1,"Chamada"));
        var previa=diario.consultarDocente(f.inscricao); assertTrue(previa.pendencias().isEmpty()); assertEquals("REPROVADO",previa.previa().situacao());
        assertEquals(previa.previa(),diario.fechar(f.inscricao,"Cinco de seis tempos não atingem 90%"));
    }
    @Test void aberturaConfiguradaDoProximoPeriodoTambemLimitaRetificacao() throws Exception {
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Resultado original");
        long proximo=seguinte(); var v=criar(); var p=repo().consultarPeriodo(proximo);
        repo().vincular(new Vincular(p.id(),p.versaoVinculo(),v.id(),p.assinatura(),"Nova regra do próximo período"));
        g(f.c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",f.hoje.plusDays(18),f.hoje.plusDays(118),proximo);
        var docente=new RepositorioRetificacaoJdbc(f.c,f.sessao(f.docente));
        var erro=assertThrows(IllegalArgumentException.class,()->docente.solicitar(f.inscricao,new RetificacaoResultado(BigDecimal.TEN,BigDecimal.TEN,null,100,100),"Inscrição seguinte já aberta"));
        assertTrue(erro.getMessage().contains("primeira fase"));
    }
}

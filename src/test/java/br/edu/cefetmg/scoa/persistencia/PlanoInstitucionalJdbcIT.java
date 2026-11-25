package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class PlanoInstitucionalJdbcIT {
    private CenarioAcademicoTeste f;
    private RepositorioPlanoInstitucionalJdbc repo;
    private long periodo,b,turma;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste(); repo=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.admin));
        g(f.c,"UPDATE inscricao_disciplina SET situacao='CANCELADO' WHERE id_inscricao=?",f.inscricao);
        periodo=g(f.c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",f.tag.substring(0,30)+"-seguinte",f.hoje.plusDays(10),f.hoje.plusDays(100));
        b=f.disciplina("B"); g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",f.estrutura,b);
        g(f.c,"INSERT INTO disciplina_prerequisito VALUES (?,?)",b,f.disciplina);
        long sala=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Sala sintética','Bloco',20,'ATIVO')");
        turma=ofertar(f.disciplina,sala,"07:00","07:50"); ofertar(b,sala,"07:50","08:40");
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private long ofertar(long disciplina,long sala,String inicio,String fim) throws Exception {
        long turma=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Oferta sintética',20,'ATIVO')",disciplina,f.professor,periodo);
        g(f.c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,?,?)",turma,sala,inicio,fim); return turma;
    }
    private Selecao selecao() { return new Selecao(f.matricula,List.of(periodo),List.of()); }
    private Versao registrar() {
        var p=repo.preparar(selecao()); return repo.registrar(new Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),"Plano sintético"));
    }
    private void aprovarOrigem() throws Exception {
        g(f.c,"UPDATE inscricao_disciplina SET situacao='APROVADO' WHERE id_inscricao=?",f.inscricao);
        g(f.c,"INSERT INTO resultado_inscricao(id_inscricao,p1,p2,media_inicial,media_final,total_minutos,presentes_minutos,situacao,motivo) VALUES (?,8,8,8,8,100,100,'APROVADO','Fixture de resultado')",f.inscricao);
    }
    @Test void previaEConsultaSemPlanoNaoCriamVersoesOuInscricoes() throws Exception {
        var p=repo.preparar(selecao()); assertEquals(0,p.versaoAnterior()); assertEquals(2,p.componentes().size());
        assertTrue(repo.atualSecretaria(f.matricula).isEmpty());
        var aluno=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.alunoConta));
        assertTrue(aluno.atualAluno(f.matricula).isEmpty()); assertTrue(aluno.historicoAluno(f.matricula).isEmpty());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM plano_institucional",r->r.getInt(1)).getFirst());
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM inscricao_disciplina WHERE id_matricula=?",r->r.getInt(1),f.matricula).getFirst());
    }
    @Test void versaoRegistraAutoriaContextoComponentesEPreRequisitoSemConfundirSugestaoComConclusao() {
        var v=registrar(); assertEquals(1,v.numero()); assertNull(v.anterior()); assertEquals(f.admin,v.autor());
        assertEquals(List.of(periodo),v.periodos()); assertTrue(v.prioridades().isEmpty());
        var a=v.componentes().stream().filter(i->i.disciplina()==f.disciplina).findFirst().orElseThrow();
        var itemB=v.componentes().stream().filter(i->i.disciplina()==b).findFirst().orElseThrow();
        assertEquals(turma,a.turma()); assertEquals("PENDENTE",a.situacao()); assertNull(itemB.turma());
        assertEquals(List.of(f.disciplina),itemB.requisitos());
        assertEquals(Atualidade.ATUAL,repo.atualSecretaria(f.matricula).orElseThrow().atualidade());
    }
    @Test void atualizacaoGeraOutraVersaoSemReescreverNomeCargaOuEvidenciaAnteriores() throws Exception {
        var v1=registrar(); String original=v1.componentes().getFirst().nome();
        g(f.c,"UPDATE disciplina SET nome='Nome revisado',carga_horaria=90 WHERE id_disciplina=?",f.disciplina);
        assertEquals(Atualidade.PENDENTE_ATUALIZACAO,repo.atualSecretaria(f.matricula).orElseThrow().atualidade());
        var v2=registrar(); assertEquals(2,v2.numero()); assertEquals(v1.id(),v2.anterior());
        var historico=repo.historicoSecretaria(f.matricula); assertEquals(2,historico.size());
        assertEquals(original,historico.getFirst().componentes().getFirst().nome()); assertEquals(60,historico.getFirst().componentes().getFirst().horas());
        assertEquals("Nome revisado",historico.getLast().componentes().getFirst().nome()); assertEquals(90,historico.getLast().componentes().getFirst().horas());
    }
    @Test void dadosAlteradosAposPreviaOuOutroRegistroRecusamConfirmacaoAntiga() throws Exception {
        var p=repo.preparar(selecao());
        g(f.c,"UPDATE turma SET capacidade=1 WHERE id_turma=?",turma);
        assertThrows(IllegalArgumentException.class,()->repo.registrar(new Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),"Prévia antiga")));
        var nova=repo.preparar(selecao());
        repo.registrar(new Registro(nova.selecao(),nova.versaoAnterior(),nova.assinatura(),"Primeiro operador"));
        assertThrows(IllegalArgumentException.class,()->repo.registrar(new Registro(nova.selecao(),nova.versaoAnterior(),nova.assinatura(),"Segundo operador")));
        assertEquals(1,repo.historicoSecretaria(f.matricula).size());
    }
    @Test void resultadoFormalLiberaPreRequisitoEAConsultaApontaPlanoAntigoSemAtualizaLo() throws Exception {
        var v1=registrar(); aprovarOrigem();
        var consulta=repo.atualSecretaria(f.matricula).orElseThrow();
        assertEquals(Atualidade.PENDENTE_ATUALIZACAO,consulta.atualidade()); assertEquals(v1.id(),consulta.versao().id());
        assertEquals(1,repo.historicoSecretaria(f.matricula).size());
        var v2=registrar(); assertEquals("CONCLUIDA",v2.componentes().stream().filter(i->i.disciplina()==f.disciplina).findFirst().orElseThrow().situacao());
        assertNotNull(v2.componentes().stream().filter(i->i.disciplina()==b).findFirst().orElseThrow().turma());
    }
    @Test void reconhecimentoSobRevisaoAtualizaDiagnosticoSomenteEmNovaVersao() throws Exception {
        long d=f.disciplina("R"); long pedido=f.reconhecer(d,null); var v1=registrar();
        assertEquals("RECONHECIDA",v1.componentes().stream().filter(i->i.disciplina()==d).findFirst().orElseThrow().situacao());
        g(f.c,"INSERT INTO revisao_reconhecimento(id_pedido,solicitante,justificativa,situacao) VALUES (?,?,'Fixture de revisão','ABERTA')",pedido,f.admin);
        assertEquals(Atualidade.PENDENTE_ATUALIZACAO,repo.atualSecretaria(f.matricula).orElseThrow().atualidade());
        assertEquals("SOB_REVISAO",registrar().componentes().stream().filter(i->i.disciplina()==d).findFirst().orElseThrow().situacao());
        assertEquals("RECONHECIDA",repo.historicoSecretaria(f.matricula).getFirst().componentes().stream().filter(i->i.disciplina()==d).findFirst().orElseThrow().situacao());
    }
    @Test void alunoNaoConsultaPlanoOuHistoricoAlheiosNemGeraPlanoComSeuPapel() {
        registrar(); var aluno=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.alunoConta));
        var outro=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.outroConta));
        assertTrue(aluno.atualAluno(f.matricula).isPresent()); assertEquals(1,aluno.historicoAluno(f.matricula).size());
        assertThrows(IllegalArgumentException.class,()->outro.atualAluno(f.matricula));
        assertThrows(IllegalArgumentException.class,()->outro.historicoAluno(f.matricula));
        assertThrows(IllegalArgumentException.class,()->aluno.preparar(selecao()));
        assertThrows(IllegalArgumentException.class,()->aluno.atualSecretaria(f.matricula));
    }
    @Test void permissaoDeConsultaNaoConcedeManutencaoERevogacaoERevalidada() throws Exception {
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.CONSULTAR);
        var leitura=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.outroDocente)); var p=leitura.preparar(selecao());
        var pedido=new Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),"Plano");
        assertThrows(IllegalArgumentException.class,()->leitura.registrar(pedido));
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.MANTER); leitura.registrar(pedido);
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.outroDocente);
        assertThrows(IllegalArgumentException.class,()->leitura.historicoSecretaria(f.matricula));
    }
    @Test void mudancaDeContextoImpedeUsoDefinitivoSemDestruirVersaoRegistrada() throws Exception {
        var v=registrar(); g(f.c,"UPDATE estrutura_curricular SET situacao='INATIVO' WHERE id_estrutura=?",f.estrutura);
        var consulta=repo.atualSecretaria(f.matricula).orElseThrow();
        assertEquals(Atualidade.INCONSISTENTE,consulta.atualidade()); assertEquals(v.id(),consulta.versao().id());
        assertThrows(IllegalArgumentException.class,this::registrar); assertEquals(1,repo.historicoSecretaria(f.matricula).size());
    }
    @Test void legadoSemConciliacaoOuReconhecimentoComDataFuturaNaoGeraPlano() throws Exception {
        long d=f.disciplina("F"); long pedido=f.reconhecer(d,null);
        g(f.c,"UPDATE pedido_equivalencia SET conclusao_origem=? WHERE id_pedido=?",f.hoje.plusDays(1),pedido);
        assertThrows(IllegalArgumentException.class,this::registrar);
        g(f.c,"UPDATE pedido_equivalencia SET conclusao_origem=? WHERE id_pedido=?",f.hoje.minusDays(1),pedido);
        g(f.c,"INSERT INTO plano_estudo(id_matricula,id_estrutura,data_atualizacao,situacao) VALUES (?,?,?,'LEGADO')",f.matricula,f.estrutura,f.hoje);
        assertThrows(IllegalArgumentException.class,this::registrar); assertTrue(repo.historicoSecretaria(f.matricula).isEmpty());
    }
    @Test void faltaDeEstruturaOuComponentesNaoCriaPlanoVazio() throws Exception {
        g(f.c,"DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",f.matricula);
        assertThrows(IllegalArgumentException.class,this::registrar);
        g(f.c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",f.matricula,f.estrutura);
        g(f.c,"DELETE FROM estrutura_disciplina WHERE id_estrutura=?",f.estrutura);
        assertThrows(IllegalArgumentException.class,this::registrar); assertTrue(repo.atualSecretaria(f.matricula).isEmpty());
    }
    @Test void vinculoDaContaEAtividadeSaoRevalidadosNaConsultaPropria() throws Exception {
        registrar(); var aluno=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.alunoConta));
        g(f.c,"UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",f.aluno);
        assertThrows(IllegalArgumentException.class,()->aluno.atualAluno(f.matricula));
        g(f.c,"UPDATE aluno SET id_usuario=? WHERE id_aluno=?",f.alunoConta,f.aluno);
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.alunoConta);
        assertThrows(IllegalArgumentException.class,()->aluno.historicoAluno(f.matricula));
        assertThrows(IllegalArgumentException.class,()->new RepositorioPlanoInstitucionalJdbc(f.c,null).preparar(selecao()));
    }
    @Test void falhaDeAuditoriaDesfazCabecalhoComponentesEEventoSemAlterarLegado() throws Exception {
        var p=repo.preparar(selecao()); var proxy=f.falharAuditoria();
        var falho=new RepositorioPlanoInstitucionalJdbc(proxy,f.sessao(proxy,f.admin));
        var erro=assertThrows(PersistenciaException.class,()->falho.registrar(new Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),"Fixture")));
        assertTrue(erro.getCause().getMessage().contains("Falha sintética de auditoria"));
        assertTrue(repo.historicoSecretaria(f.matricula).isEmpty());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM componente_plano_institucional",r->r.getInt(1)).getFirst());
        assertTrue(q(f.c,"SELECT id_evento FROM evento_processo_academico WHERE tipo='PLANO_INSTITUCIONAL'",r->r.getLong(1)).isEmpty());
        assertFalse(f.c.getAutoCommit());
    }
}

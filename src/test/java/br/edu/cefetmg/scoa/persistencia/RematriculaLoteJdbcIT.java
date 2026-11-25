package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RematriculaLote.*;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.Registro;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class RematriculaLoteJdbcIT {
    private CenarioAcademicoTeste f;
    private RepositorioRematriculaLoteJdbc repo;
    private RepositorioPlanoInstitucionalJdbc planos;
    private long destino,b,turma,inscricaoOutro;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste(); var sessao=f.sessao(f.admin);
        repo=new RepositorioRematriculaLoteJdbc(f.c,sessao); planos=new RepositorioPlanoInstitucionalJdbc(f.c,sessao);
        inscricaoOutro=f.inscrever(f.outraMatricula,"ATIVO");
        var insc=new RepositorioInscricaoJdbc(f.c);
        for(long id:List.of(f.inscricao,inscricaoOutro)) insc.registrarResultado(id,new BigDecimal("8"),new BigDecimal("8"),null,100,100,"Fixture de resultado anterior");
        for(long usuario:List.of(f.alunoConta,f.outroConta)) g(f.c,"INSERT INTO usuario_biblioteca(id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",usuario);
        destino=g(f.c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",f.tag.substring(0,25)+"-destino",f.hoje.plusDays(10),f.hoje.plusDays(100));
        b=f.disciplina("B"); g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",f.estrutura,b); g(f.c,"INSERT INTO disciplina_prerequisito VALUES (?,?)",b,f.disciplina);
        long sala=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Sala sintética','Bloco',20,'ATIVO')");
        turma=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Oferta do lote',20,'ATIVO')",b,f.professor,destino);
        g(f.c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,'07:00','07:50')",turma,sala);
        atualizarPlanos();
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private void atualizarPlanos() {
        for(long m:List.of(f.matricula,f.outraMatricula)) {
            var p=planos.preparar(new br.edu.cefetmg.scoa.dominio.PlanoInstitucional.Selecao(m,List.of(destino),List.of()));
            planos.registrar(new Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),"Plano do lote"));
        }
    }
    private Selecao selecao() { return new Selecao(f.curso,f.periodo,destino,List.of()); }
    private Pedido pedido() { var p=repo.preparar(selecao()); return new Pedido(p.selecao(),p.chave(),p.assinatura(),"Processamento sintético"); }
    private int renovadas() throws Exception { return q(f.c,"SELECT COUNT(*) FROM rematricula_academica WHERE id_periodo=?",r->r.getInt(1),destino).getFirst(); }
    @Test void previaNaoRenovaNaoInscreveENaoRegistraLote() throws Exception {
        var p=repo.preparar(selecao()); assertEquals(2,p.candidatos().size()); assertTrue(p.candidatos().stream().allMatch(c->"ELEGIVEL".equals(c.situacao())));
        assertEquals(0,renovadas()); assertEquals(0,q(f.c,"SELECT COUNT(*) FROM lote_rematricula",r->r.getInt(1)).getFirst());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM inscricao_disciplina WHERE id_turma=?",r->r.getInt(1),turma).getFirst());
    }
    @Test void loteRenovaMesmoVinculoComAutoriaEPropostasSemInscreverAutomaticamente() throws Exception {
        var r=repo.executar(pedido()); assertEquals(2,r.renovadas()); assertEquals(0,r.manuais()); assertEquals(f.admin,r.autor());
        assertEquals(2,renovadas()); assertEquals(2,repo.consultar(r.id()).candidatos().size());
        assertTrue(r.candidatos().stream().allMatch(c->c.propostas().stream().anyMatch(p->p.turma()!=null && p.turma()==turma && p.situacao().equals("DISPONIVEL"))));
        assertTrue(q(f.c,"SELECT id_usuario FROM rematricula_academica WHERE id_periodo=?",x->x.getLong(1),destino).stream().allMatch(u->u==f.admin));
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM inscricao_disciplina WHERE id_turma=?",x->x.getInt(1),turma).getFirst());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM matricula_academica WHERE id_matricula=?",x->x.getString(1),f.matricula).getFirst());
    }
    @Test void nenhumaOfertaDisponivelEncaminhaTodaRematriculaParaAnaliseManual() throws Exception {
        g(f.c,"DELETE FROM horario_aula WHERE id_turma=?",turma); atualizarPlanos();
        var r=repo.executar(pedido()); assertEquals(0,r.renovadas()); assertEquals(2,r.manuais()); assertEquals(0,renovadas());
        assertTrue(r.candidatos().stream().allMatch(c->c.mensagem().contains("Nenhuma turma sugerida disponível")));
    }
    @Test void faltaDeRespostaBibliotecariaEmUmAlunoNaoImpedeOutro() throws Exception {
        g(f.c,"DELETE FROM usuario_biblioteca WHERE id_usuario=?",f.outroConta);
        var r=repo.executar(pedido()); assertEquals(1,r.renovadas()); assertEquals(1,r.manuais()); assertEquals(1,renovadas());
        assertTrue(r.candidatos().stream().filter(c->c.matricula()==f.outraMatricula).findFirst().orElseThrow().mensagem().contains("Biblioteca"));
    }
    @Test void ausenciaDePlanoOuResultadoPendenteGeraOcorrenciaSemInventarValores() throws Exception {
        g(f.c,"DELETE FROM componente_plano_institucional WHERE id_plano IN (SELECT id_plano FROM plano_institucional WHERE id_matricula=?)",f.outraMatricula);
        g(f.c,"DELETE FROM plano_institucional WHERE id_matricula=?",f.outraMatricula);
        var r=repo.executar(pedido()); assertEquals(1,r.renovadas()); assertEquals(1,r.manuais());
        assertTrue(r.candidatos().stream().filter(c->c.matricula()==f.outraMatricula).findFirst().orElseThrow().mensagem().contains("Plano institucional"));
    }
    @Test void resultadoAnteriorSemFechamentoFormalImpedeRenovacaoDaqueleAluno() throws Exception {
        g(f.c,"DELETE FROM resultado_inscricao WHERE id_inscricao=?",inscricaoOutro);
        g(f.c,"UPDATE inscricao_disciplina SET situacao='ATIVO' WHERE id_inscricao=?",inscricaoOutro);
        var r=repo.executar(pedido()); assertEquals(1,r.renovadas()); assertEquals(1,r.manuais());
        assertTrue(r.candidatos().stream().filter(c->c.matricula()==f.outraMatricula).findFirst().orElseThrow().mensagem().contains("Resultados"));
    }
    @Test void planoPendenteDeAtualizacaoNaoERenovadoSilenciosamente() throws Exception {
        g(f.c,"UPDATE disciplina SET nome='Nome revisado' WHERE id_disciplina=?",b);
        var r=repo.executar(pedido()); assertEquals(2,r.manuais()); assertEquals(0,renovadas());
        assertTrue(r.candidatos().stream().allMatch(c->c.mensagem().contains("Plano pendente")));
    }
    @Test void repetirChaveRetornaMesmoLoteENovoProcessamentoNaoDuplicaRenovacoes() throws Exception {
        var p=pedido(); var primeiro=repo.executar(p); var repetido=repo.executar(p);
        assertEquals(primeiro,repetido); assertEquals(2,renovadas());
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM lote_rematricula",r->r.getInt(1)).getFirst());
        var novo=repo.executar(pedido()); assertNotEquals(primeiro.id(),novo.id()); assertEquals(2,novo.jaRenovadas()); assertEquals(0,novo.renovadas()); assertEquals(2,renovadas());
    }
    @Test void mesmaChaveComOutrosDadosRecusaSemAlterarProcessamentoOriginal() {
        var p=pedido(); var r=repo.executar(p);
        assertThrows(IllegalArgumentException.class,()->repo.executar(new Pedido(p.selecao(),p.chave(),p.assinatura(),"Outro motivo")));
        assertEquals(r,repo.consultar(r.id()));
    }
    @Test void dadosMudadosDesdePreviaExigemNovaConferenciaAntesDeQualquerRenovacao() throws Exception {
        var p=pedido(); g(f.c,"UPDATE usuario_biblioteca SET situacao='INATIVO' WHERE id_usuario=?",f.outroConta);
        assertThrows(IllegalArgumentException.class,()->repo.executar(p));
        assertEquals(0,renovadas()); assertEquals(0,q(f.c,"SELECT COUNT(*) FROM lote_rematricula",r->r.getInt(1)).getFirst());
    }
    @Test void selecaoExplicitaLimitaAlunosERecusaMatriculaDeOutroCurso() throws Exception {
        var s=new Selecao(f.curso,f.periodo,destino,List.of(f.matricula)); var p=repo.preparar(s);
        var r=repo.executar(new Pedido(s,p.chave(),p.assinatura(),"Seleção explícita")); assertEquals(1,r.renovadas()); assertEquals(1,r.candidatos().size());
        assertThrows(IllegalArgumentException.class,()->repo.preparar(new Selecao(f.curso,f.periodo,destino,List.of(Long.MAX_VALUE))));
    }
    @Test void sessaoPermissoesERevogacaoSaoRevalidadasSemPoderDerivadoDoPapel() throws Exception {
        var p=pedido(); var aluno=new RepositorioRematriculaLoteJdbc(f.c,f.sessao(f.alunoConta));
        assertThrows(IllegalArgumentException.class,()->aluno.preparar(selecao())); assertThrows(IllegalArgumentException.class,()->aluno.executar(p));
        assertThrows(IllegalArgumentException.class,()->new RepositorioRematriculaLoteJdbc(f.c,null).consultar(1));
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.CONSULTAR); var leitura=new RepositorioRematriculaLoteJdbc(f.c,f.sessao(f.outroDocente));
        leitura.preparar(selecao()); assertThrows(IllegalArgumentException.class,()->leitura.executar(p));
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.MANTER); var r=leitura.executar(p);
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.outroDocente);
        assertThrows(IllegalArgumentException.class,()->leitura.consultar(r.id()));
    }
    @Test void origemNaoEncerradaOuDestinoAnteriorRecusamLoteSemRegistro() {
        assertThrows(IllegalArgumentException.class,()->repo.preparar(new Selecao(f.curso,destino,f.periodo,List.of())));
    }
    @Test void falhaDeAuditoriaDesfazLoteRenovacoesPropostasEHistorico() throws Exception {
        var p=pedido(); var proxy=f.falharAuditoria();
        var falho=new RepositorioRematriculaLoteJdbc(proxy,f.sessao(proxy,f.admin));
        var e=assertThrows(PersistenciaException.class,()->falho.executar(p));
        assertTrue(e.getCause().getMessage().contains("Falha sintética de auditoria"));
        assertEquals(0,renovadas()); assertEquals(0,q(f.c,"SELECT COUNT(*) FROM lote_rematricula",r->r.getInt(1)).getFirst());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM proposta_lote_rematricula",r->r.getInt(1)).getFirst());
        assertFalse(f.c.getAutoCommit());
    }
}

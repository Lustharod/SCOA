package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.TransferenciaInterna.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class TransferenciaInternaJdbcIT {
    private CenarioAcademicoTeste f;
    private long curso,estrutura,periodo,ativa,outraDisciplina;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste();
        g(f.c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",f.hoje.minusDays(200),f.hoje.minusDays(100),f.periodo);
        g(f.c,"UPDATE matricula_academica SET data_ingresso=? WHERE id_matricula=?",f.hoje.minusDays(210),f.matricula);
        g(f.c,"UPDATE inscricao_disciplina SET data_inscricao=? WHERE id_inscricao=?",f.hoje.minusDays(210),f.inscricao);
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,100,100,"Fixture de conclusão formal na origem");
        g(f.c,"INSERT INTO usuario_biblioteca(id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",f.alunoConta);
        curso=g(f.c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",f.tag+"-destino");
        estrutura=g(f.c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,f.tag+"-destino");
        g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",estrutura,f.disciplina);
        // Mesmo nome e código não fazem uma disciplina diferente tornar-se idêntica.
        outraDisciplina=g(f.c,"INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) SELECT codigo,nome,carga_horaria,situacao FROM disciplina WHERE id_disciplina=?",f.disciplina);
        g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",estrutura,outraDisciplina);
        periodo=g(f.c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",
                f.tag+"-atual",f.hoje,f.hoje.plusDays(100));
        long pendente=f.disciplina("EM_CURSO");
        g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",f.estrutura,pendente);
        long turma=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Atual',20,'ATIVO')",pendente,f.professor,periodo);
        ativa=g(f.c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",f.matricula,turma,f.hoje.minusDays(10));
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private Pedido pedido() { return new Pedido(f.matricula,curso,estrutura,periodo,"Transferência autorizada sintética"); }
    private RepositorioTransferenciaInternaJdbc repo() { return new RepositorioTransferenciaInternaJdbc(f.c,f.sessao(f.admin)); }

    @Test void transferenciaPreservaOrigemAbreDestinoECancelaAtivasComRastreabilidade() throws Exception {
        var registro=repo().efetivar(pedido());
        assertEquals("EFETIVADA",registro.situacao()); assertEquals(f.admin,registro.autor());
        assertNotEquals(f.matricula,registro.destino());
        assertEquals("TRANSFERIDO",q(f.c,"SELECT situacao FROM matricula_academica WHERE id_matricula=?",r->r.getString(1),f.matricula).getFirst());
        assertEquals(f.aluno,q(f.c,"SELECT id_aluno FROM matricula_academica WHERE id_matricula=?",r->r.getLong(1),registro.destino()).getFirst());
        assertEquals(f.alunoConta,q(f.c,"SELECT id_usuario FROM aluno WHERE id_aluno=?",r->r.getLong(1),f.aluno).getFirst());
        assertEquals("CANCELADO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),ativa).getFirst());
        assertEquals("APROVADO",q(f.c,"SELECT situacao FROM resultado_inscricao WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
        assertEquals(1,repo().listar(f.matricula).size());
        assertTrue(registro.historico().contains(Long.toString(ativa)));
    }
    @Test void somenteIdIdenticoComAprovacaoFormalGeraReconhecimentoAutomatico() throws Exception {
        var transferencia=repo().efetivar(pedido());
        var reconhecimentos=q(f.c,"SELECT id_pedido,id_disciplina_destino FROM pedido_equivalencia WHERE id_matricula=?",r->new long[]{r.getLong(1),r.getLong(2)},transferencia.destino());
        assertEquals(1,reconhecimentos.size()); assertEquals(f.disciplina,reconhecimentos.getFirst()[1]);
        var vinculo=q(f.c,"SELECT id_inscricao_origem,JSON_UNQUOTE(JSON_EXTRACT(resultado_origem,'$.situacao')) FROM aproveitamento_transferencia WHERE id_transferencia=?",r->new Object[]{r.getLong(1),r.getString(2)},transferencia.id()).getFirst();
        assertEquals(f.inscricao,vinculo[0]); assertEquals("APROVADO",vinculo[1]);
        var plano=new RepositorioEquivalenciaJdbc(f.c).plano(transferencia.destino());
        assertEquals("RECONHECIDA",plano.itens().stream().filter(d->d.disciplina()==f.disciplina).findFirst().orElseThrow().situacao());
        assertEquals("PENDENTE",plano.itens().stream().filter(d->d.disciplina()==outraDisciplina).findFirst().orElseThrow().situacao());
        var h=new RepositorioAtendimentoAlunoJdbc(f.c,f.sessao(f.alunoConta)).historico();
        assertEquals(2,h.matriculas().size()); assertTrue(h.reconhecimentos().getFirst().vigente());
    }
    @ParameterizedTest @CsvSource({"0,false","13,false","14,true","45,true"})
    void cancelamentoMantemRegraDeVagasEAdmiteAteDiaQuarentaECinco(int dia,boolean retida) throws Exception {
        g(f.c,"UPDATE periodo_letivo SET data_inicio=? WHERE id_periodo=?",f.hoje.minusDays(dia),periodo);
        var registro=repo().efetivar(pedido());
        assertNotNull(registro.destino());
        assertEquals(retida,q(f.c,"SELECT vaga_retida FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getBoolean(1),ativa).getFirst());
    }
    @Test void foraDoPrazoNaoCancelaInscricaoNemMudaVinculo() throws Exception {
        g(f.c,"UPDATE periodo_letivo SET data_inicio=? WHERE id_periodo=?",f.hoje.minusDays(46),periodo);
        assertThrows(IllegalArgumentException.class,()->repo().efetivar(pedido()));
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),ativa).getFirst());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM matricula_academica WHERE id_matricula=?",r->r.getString(1),f.matricula).getFirst());
        assertTrue(q(f.c,"SELECT id_matricula FROM matricula_academica WHERE id_aluno=? AND id_curso=?",r->r.getLong(1),f.aluno,curso).isEmpty());
    }
    @Test void repetirMesmaTransferenciaRetornaProtocoloSemDuplicarVinculosOuReconhecimentos() throws Exception {
        var primeiro=repo().efetivar(pedido()); var segundo=repo().efetivar(pedido());
        assertEquals(primeiro.id(),segundo.id()); assertEquals(primeiro.destino(),segundo.destino());
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM transferencia_academica WHERE id_matricula=?",r->r.getInt(1),f.matricula).getFirst());
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM aproveitamento_transferencia WHERE id_transferencia=?",r->r.getInt(1),primeiro.id()).getFirst());
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM evento_processo_academico WHERE tipo='TRANSFERENCIA' AND id_processo=?",r->r.getInt(1),primeiro.id()).getFirst());
    }
    @Test void bibliotecaIndisponivelPreservaTodaAOrigem() throws Exception {
        g(f.c,"DELETE FROM usuario_biblioteca WHERE id_usuario=?",f.alunoConta);
        assertThrows(IllegalArgumentException.class,()->repo().efetivar(pedido()));
        assertTrue(repo().listar(f.matricula).isEmpty());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),ativa).getFirst());
    }
    @Test void estruturaDeOutroCursoOuDestinoJaVinculadoNaoEfetivam() throws Exception {
        assertThrows(IllegalArgumentException.class,()->repo().efetivar(new Pedido(f.matricula,curso,f.estrutura,periodo,"Motivo")));
        g(f.c,"INSERT INTO matricula_academica(id_aluno,id_curso,id_periodo_ingresso,data_ingresso,situacao) VALUES (?,?,?,CURRENT_DATE,'ATIVO')",f.aluno,curso,periodo);
        assertThrows(IllegalArgumentException.class,()->repo().efetivar(pedido()));
        assertTrue(repo().listar(f.matricula).isEmpty());
    }
    @Test void contaDeAlunoOuCoordenacaoSemPermissaoTecnicaNaoEfetiva() throws Exception {
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.alunoConta);
        var repo=new RepositorioTransferenciaInternaJdbc(f.c,f.sessao(f.alunoConta));
        assertThrows(IllegalArgumentException.class,()->repo.efetivar(pedido()));
        assertThrows(IllegalArgumentException.class,()->repo.listar(f.matricula));
    }
    @Test void falhaNaAuditoriaDesfazDestinoCancelamentosEReconhecimentos() throws Exception {
        var proxy=f.falharAuditoria();
        var repo=new RepositorioTransferenciaInternaJdbc(proxy,f.sessao(proxy,f.admin));
        var erro=assertThrows(PersistenciaException.class,()->repo.efetivar(pedido()));
        assertTrue(erro.getCause().getMessage().contains("Falha sintética de auditoria"));
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM matricula_academica WHERE id_matricula=?",r->r.getString(1),f.matricula).getFirst());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),ativa).getFirst());
        assertTrue(repo().listar(f.matricula).isEmpty());
        assertTrue(q(f.c,"SELECT id_matricula FROM matricula_academica WHERE id_aluno=? AND id_curso=?",r->r.getLong(1),f.aluno,curso).isEmpty());
        assertFalse(f.c.getAutoCommit());
    }
    @Test void aproveitamentoAutomaticoHabilitaPrerequisitoComProvaRastreada() throws Exception {
        g(f.c,"DELETE FROM inscricao_disciplina WHERE id_inscricao=?",ativa);
        g(f.c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",f.hoje.plusDays(10),f.hoje.plusDays(100),periodo);
        var t=repo().efetivar(pedido());
        g(f.c,"INSERT INTO disciplina_prerequisito VALUES (?,?)",outraDisciplina,f.disciplina);
        long turma=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Destino',20,'ATIVO')",outraDisciplina,f.professor,periodo);
        long sala=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Sala','Bloco',20,'ATIVO')");
        g(f.c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,'08:40','09:30')",turma,sala);
        var inscricao=new RepositorioInscricaoJdbc(f.c).inscreverComoAluno(f.sessao(f.alunoConta),t.destino(),turma);
        long pedido=q(f.c,"SELECT id_pedido FROM aproveitamento_transferencia WHERE id_transferencia=?",r->r.getLong(1),t.id()).getFirst();
        assertEquals(pedido,q(f.c,"SELECT id_pedido FROM evidencia_requisito_inscricao WHERE id_inscricao=?",r->r.getLong(1),inscricao.id()).getFirst());
    }
    @Test void retificacaoQueDerrubaAprovacaoUsadaNaTransferenciaExigeAnaliseDeImpacto() throws Exception {
        g(f.c,"DELETE FROM inscricao_disciplina WHERE id_inscricao=?",ativa);
        g(f.c,"UPDATE periodo_letivo SET data_fim=? WHERE id_periodo=?",f.hoje.minusDays(1),f.periodo);
        g(f.c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",f.hoje.plusDays(30),f.hoje.plusDays(130),periodo);
        repo().efetivar(pedido());
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.admin);
        var retificacao=new RepositorioRetificacaoJdbc(f.c,f.sessao(f.docente));
        long id=retificacao.solicitar(f.inscricao,new RetificacaoResultado(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,100,100),"Proposta sintética");
        var coordenador=new RepositorioRetificacaoJdbc(f.c,f.sessao(f.admin));
        var erro=assertThrows(IllegalArgumentException.class,()->coordenador.decidir(id,true,"Parecer sintético"));
        assertTrue(erro.getMessage().contains("transferência"));
        assertEquals("APROVADO",q(f.c,"SELECT situacao FROM resultado_inscricao WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
    }
}

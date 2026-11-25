package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.TransferenciaExterna.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class TransferenciaExternaJdbcIT {
    private CenarioAcademicoTeste f;
    private long periodo,curso,estrutura,ativa,biblioteca;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste();
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,100,100,"Conclusão sintética anterior");
        biblioteca=g(f.c,"INSERT INTO usuario_biblioteca(id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",f.alunoConta);
        periodo=g(f.c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",f.tag+"-ext",f.hoje.minusDays(10),f.hoje.plusDays(90));
        curso=g(f.c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",f.tag+"-destino");
        estrutura=g(f.c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,f.tag+"-destino");
        g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",estrutura,f.disciplina);
        long turma=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Atual',20,'ATIVO')",f.disciplina,f.professor,periodo);
        ativa=g(f.c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",f.matricula,turma,f.hoje.minusDays(10));
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioTransferenciaExternaJdbc repo() { return new RepositorioTransferenciaExternaJdbc(f.c,f.sessao(f.admin)); }
    private Pedido entrada() { return new Pedido(UUID.randomUUID().toString(),Tipo.ENTRADA,f.aluno,curso,estrutura,null,periodo,"Instituição fictícia de origem","Histórico externo REF-01","Entrada autorizada sintética"); }
    private Pedido saida() { return new Pedido(UUID.randomUUID().toString(),Tipo.SAIDA,null,null,null,f.matricula,periodo,"Instituição fictícia de destino","Autorização REF-02","Saída autorizada sintética"); }
    private Atualizacao decisao(Processo p,Situacao situacao) { return new Atualizacao(p.versao(),situacao,p.documentos(),"Fundamento sintético da decisão"); }
    private Processo efetivar(Processo p) { return repo().atualizar(p.id(),decisao(p,Situacao.EFETIVADA)); }
    private String situacaoMatricula() throws Exception { return q(f.c,"SELECT situacao FROM matricula_academica WHERE id_matricula=?",r->r.getString(1),f.matricula).getFirst(); }
    private void semEfeito(Processo p) throws Exception {
        var atual=repo().consultar(p.id()); assertEquals(Situacao.EM_ANALISE,atual.situacao()); assertEquals(p.versao(),atual.versao());
        assertNull(atual.transferencia()); assertEquals("ATIVO",situacaoMatricula());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),ativa).getFirst());
    }
    @Test void registroEAcompanhamentoPreservamContextoAutoriaEHistoricoSemEfetivar() throws Exception {
        var p=repo().registrar(saida()); assertEquals(Situacao.EM_ANALISE,p.situacao()); assertEquals(f.aluno,p.aluno());
        assertEquals(f.curso,p.curso()); assertEquals(f.admin,p.autor()); assertNull(p.transferencia());
        var atualizado=repo().atualizar(p.id(),new Atualizacao(0,Situacao.EM_ANALISE,"REF-02; REF-03","Conferência documental complementada"));
        assertEquals(1,atualizado.versao()); assertEquals(2,repo().historico(p.id()).size());
        assertEquals(p.documentos(),repo().historico(p.id()).getFirst().documentos());
        assertEquals("REF-02; REF-03",repo().historico(p.id()).getLast().documentos());
        assertEquals(1,repo().listar(f.aluno).size()); semEfeito(atualizado);
        assertTrue(q(f.c,"SELECT id_transferencia FROM transferencia_academica WHERE id_matricula=?",r->r.getLong(1),f.matricula).isEmpty());
    }
    @Test void entradaAbreVinculoNoDestinoMantendoContaOutrosVinculosEHistoricosSemReconhecimentoPresumido() throws Exception {
        String credencial=q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst();
        int concessoes=q(f.c,"SELECT COUNT(*) FROM usuario_perfil WHERE id_usuario=?",r->r.getInt(1),f.alunoConta).getFirst();
        var p=efetivar(repo().registrar(entrada()));
        assertNotNull(p.transferencia()); assertNotEquals(f.matricula,p.matriculaEfetivada());
        var destino=new RepositorioMatriculaJdbc(f.c).buscar(p.matriculaEfetivada()).orElseThrow();
        assertEquals(f.aluno,destino.idAluno()); assertEquals(curso,destino.idCurso()); assertEquals("ATIVO",situacaoMatricula());
        assertEquals(estrutura,q(f.c,"SELECT id_estrutura FROM vinculo_curricular_matricula WHERE id_matricula=?",r->r.getLong(1),destino.id()).getFirst());
        assertEquals(credencial,q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst());
        assertEquals(concessoes,q(f.c,"SELECT COUNT(*) FROM usuario_perfil WHERE id_usuario=?",r->r.getInt(1),f.alunoConta).getFirst());
        assertEquals(f.alunoConta,q(f.c,"SELECT id_usuario FROM aluno WHERE id_aluno=?",r->r.getLong(1),f.aluno).getFirst());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM pedido_equivalencia WHERE id_matricula=?",r->r.getInt(1),destino.id()).getFirst());
        assertEquals(1,new RepositorioMatriculaJdbc(f.c).historico(destino.id()).size());
        assertEquals("EXTERNA_ENTRADA",q(f.c,"SELECT tipo FROM transferencia_academica WHERE id_transferencia=?",r->r.getString(1),p.transferencia()).getFirst());
    }
    @Test void saidaPreservaOrigemResultadosContaEHistoricoProprioSemNovoVinculo() throws Exception {
        var p=efetivar(repo().registrar(saida()));
        assertEquals(f.matricula,p.matriculaEfetivada()); assertEquals("TRANSFERIDO",situacaoMatricula());
        assertEquals("CANCELADO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),ativa).getFirst());
        assertEquals("APROVADO",q(f.c,"SELECT situacao FROM resultado_inscricao WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
        assertEquals(1,new RepositorioMatriculaJdbc(f.c).listar(f.aluno).size());
        assertTrue(q(f.c,"SELECT id_matricula_destino IS NULL FROM transferencia_academica WHERE id_transferencia=?",r->r.getBoolean(1),p.transferencia()).getFirst());
        var proprio=new RepositorioAtendimentoAlunoJdbc(f.c,f.sessao(f.alunoConta)).historico();
        assertEquals(1,proprio.matriculas().size());
        assertEquals(1,new RepositorioMatriculaJdbc(f.c).historico(f.matricula).size());
    }
    @ParameterizedTest @CsvSource({"0,false","13,false","14,true","45,true"})
    void saidaRespeitaLimitesDeRetencaoDeVagas(int dias,boolean retida) throws Exception {
        g(f.c,"UPDATE periodo_letivo SET data_inicio=? WHERE id_periodo=?",f.hoje.minusDays(dias),periodo);
        efetivar(repo().registrar(saida()));
        assertEquals(retida,q(f.c,"SELECT vaga_retida FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getBoolean(1),ativa).getFirst());
    }
    @Test void pendenciaBibliotecariaBloqueiaSaidaAteRegularizacaoPreservandoRegistro() throws Exception {
        long item=g(f.c,"INSERT INTO item_acervo(titulo,tipo,classificacao,informacoes_bibliograficas,situacao) VALUES (?,'LIVRO','TESTE','Fixture','ATIVO')",f.tag);
        long exemplar=g(f.c,"INSERT INTO exemplar(id_item_acervo,identificacao,situacao) VALUES (?,?,'ATIVO')",item,f.tag);
        long emprestimo=g(f.c,"INSERT INTO emprestimo(id_usuario_biblioteca,id_exemplar,data_emprestimo,data_prevista_devolucao,situacao) VALUES (?,?,CURRENT_DATE,CURRENT_DATE,'ATIVO')",biblioteca,exemplar);
        long pendencia=g(f.c,"INSERT INTO pendencia_biblioteca(id_emprestimo,tipo_pendencia,valor,impeditiva,situacao) VALUES (?,'ATRASO',0,TRUE,'ATIVO')",emprestimo);
        var p=repo().registrar(saida());
        var erro=assertThrows(IllegalArgumentException.class,()->efetivar(p)); assertTrue(erro.getMessage().contains("biblioteca")); semEfeito(p);
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM pendencia_biblioteca WHERE id_pendencia=?",r->r.getString(1),pendencia).getFirst());
        g(f.c,"UPDATE pendencia_biblioteca SET situacao='REGULARIZADO' WHERE id_pendencia=?",pendencia);
        assertEquals(Situacao.EFETIVADA,efetivar(p).situacao());
        assertEquals("REGULARIZADO",q(f.c,"SELECT situacao FROM pendencia_biblioteca WHERE id_pendencia=?",r->r.getString(1),pendencia).getFirst());
    }
    @Test void bibliotecaAusenteNaoPermiteSaidaOuEntrada() throws Exception {
        g(f.c,"DELETE FROM usuario_biblioteca WHERE id_usuario_biblioteca=?",biblioteca);
        var saida=repo().registrar(saida()); var entrada=repo().registrar(entrada());
        assertThrows(IllegalArgumentException.class,()->efetivar(saida)); assertThrows(IllegalArgumentException.class,()->efetivar(entrada));
        semEfeito(saida); semEfeito(entrada);
    }
    @Test void aposQuarentaECincoDiasNaoMudaVinculoNemProcesso() throws Exception {
        g(f.c,"UPDATE periodo_letivo SET data_inicio=? WHERE id_periodo=?",f.hoje.minusDays(46),periodo);
        var p=repo().registrar(saida()); assertThrows(IllegalArgumentException.class,()->efetivar(p)); semEfeito(p);
        var e=repo().registrar(entrada()); assertThrows(IllegalArgumentException.class,()->efetivar(e)); semEfeito(e);
    }
    @Test void periodoEncerradoOuInativoBloqueiaMesmoAntesDeQuarentaECincoDias() throws Exception {
        var p=repo().registrar(saida());
        g(f.c,"UPDATE periodo_letivo SET data_fim=? WHERE id_periodo=?",f.hoje.minusDays(1),periodo);
        assertThrows(IllegalArgumentException.class,()->efetivar(p));
        g(f.c,"UPDATE periodo_letivo SET data_fim=?,situacao='INATIVO' WHERE id_periodo=?",f.hoje.plusDays(10),periodo);
        assertThrows(IllegalArgumentException.class,()->efetivar(p)); semEfeito(p);
    }
    @Test void outraInscricaoAtivaForaDoPrazoImpedeSaida() throws Exception {
        g(f.c,"UPDATE inscricao_disciplina SET situacao='ATIVO' WHERE id_inscricao=?",f.inscricao);
        var p=repo().registrar(saida()); assertThrows(IllegalArgumentException.class,()->efetivar(p)); semEfeito(p);
    }
    @Test void pedidoCancelamentoPendenteExigeTratamentoAnterior() throws Exception {
        g(f.c,"INSERT INTO cancelamento_inscricao(id_inscricao,solicitante,justificativa,situacao,solicitado_em) VALUES (?,?,'Fixture','PENDENTE',UTC_TIMESTAMP())",ativa,f.alunoConta);
        var p=repo().registrar(saida()); assertThrows(IllegalArgumentException.class,()->efetivar(p)); semEfeito(p);
    }
    @Test void contextoAtivoAlteradoEntreRegistroEDecisaoRevalidaDestinoEPessoa() throws Exception {
        var p=repo().registrar(entrada());
        g(f.c,"UPDATE estrutura_curricular SET situacao='INATIVO' WHERE id_estrutura=?",estrutura);
        assertThrows(IllegalArgumentException.class,()->efetivar(p));
        g(f.c,"UPDATE estrutura_curricular SET situacao='ATIVO' WHERE id_estrutura=?",estrutura);
        g(f.c,"UPDATE aluno SET situacao='INATIVO' WHERE id_aluno=?",f.aluno);
        assertThrows(IllegalArgumentException.class,()->efetivar(p)); semEfeito(p);
    }
    @Test void destinoJaAtivoNaoCriaMatriculaDuplicada() throws Exception {
        new RepositorioMatriculaJdbc(f.c).criar(f.aluno,curso,f.hoje,periodo,"Vínculo legítimo prévio");
        var p=repo().registrar(entrada()); assertThrows(IllegalArgumentException.class,()->efetivar(p));
        assertEquals(2,new RepositorioMatriculaJdbc(f.c).listar(f.aluno).size()); semEfeito(p);
    }
    @Test void registroRepetidoRetornaMesmoProtocoloEColisaoDeChaveERecusada() throws Exception {
        Pedido pedido=entrada(); var p=repo().registrar(pedido); assertEquals(p.id(),repo().registrar(pedido).id());
        Pedido colisao=new Pedido(pedido.chave(),Tipo.ENTRADA,f.aluno,curso,estrutura,null,periodo,"Outra instituição",pedido.documentos(),pedido.motivo());
        assertThrows(IllegalArgumentException.class,()->repo().registrar(colisao)); assertEquals(1,repo().listar(f.aluno).size());
    }
    @Test void repetirDecisaoNaoDuplicaMatriculaProcessoOuHistorico() throws Exception {
        var pedido=entrada(); var p=repo().registrar(pedido); var decisao=decisao(p,Situacao.EFETIVADA);
        var primeiro=repo().atualizar(p.id(),decisao); var segundo=repo().atualizar(p.id(),decisao);
        assertEquals(primeiro,segundo); assertEquals(primeiro,repo().registrar(pedido));
        assertEquals(2,repo().historico(p.id()).size()); assertEquals(2,new RepositorioMatriculaJdbc(f.c).listar(f.aluno).size());
    }
    @Test void versaoAntigaNaoSobrescreveEAConclusaoNaoEReaberta() throws Exception {
        var p=repo().registrar(saida());
        var atualizado=repo().atualizar(p.id(),new Atualizacao(0,Situacao.EM_ANALISE,p.documentos(),"Em conferência"));
        assertThrows(IllegalArgumentException.class,()->repo().atualizar(p.id(),decisao(p,Situacao.EFETIVADA)));
        var terminal=repo().atualizar(p.id(),decisao(atualizado,Situacao.INDEFERIDA));
        assertThrows(IllegalArgumentException.class,()->repo().atualizar(p.id(),decisao(terminal,Situacao.EM_ANALISE)));
        assertNull(terminal.transferencia()); assertEquals("ATIVO",situacaoMatricula());
    }
    @Test void alunoGestorOuCoordenadorSemPermissaoTecnicaNaoAdministraProcessos() throws Exception {
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.alunoConta);
        new SegurancaJdbc(f.c).vincularGestorAcademico(f.sessao(f.admin),f.alunoConta);
        var repo=new RepositorioTransferenciaExternaJdbc(f.c,f.sessao(f.alunoConta));
        assertThrows(IllegalArgumentException.class,()->repo.registrar(saida()));
        assertThrows(IllegalArgumentException.class,()->repo.listar(f.aluno));
    }
    @Test void consultarEManterExigemConcessoesIndependentesEAtuais() throws Exception {
        f.conceder(f.outroConta,AcessoAcademicoJdbc.CONSULTAR);
        var p=repo().registrar(saida()); var leitura=new RepositorioTransferenciaExternaJdbc(f.c,f.sessao(f.outroConta));
        assertEquals(p,leitura.consultar(p.id()));
        assertThrows(IllegalArgumentException.class,()->leitura.atualizar(p.id(),decisao(p,Situacao.EFETIVADA)));
        f.conceder(f.outroConta,AcessoAcademicoJdbc.MANTER);
        var antes=leitura.atualizar(p.id(),new Atualizacao(0,Situacao.EM_ANALISE,p.documentos(),"Conferido por outro operador"));
        assertEquals(f.outroConta,leitura.historico(p.id()).getLast().autor());
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.outroConta);
        assertThrows(IllegalArgumentException.class,()->leitura.atualizar(p.id(),decisao(antes,Situacao.EFETIVADA)));
        assertThrows(IllegalArgumentException.class,()->leitura.consultar(p.id()));
    }
    @Test void falhaNaAuditoriaDesfazRegistroInicial() throws Exception {
        var proxy=f.falharAuditoria(); var repo=new RepositorioTransferenciaExternaJdbc(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->repo.registrar(saida())); assertTrue(repo().listar(f.aluno).isEmpty());
    }
    @Test void falhaNaAuditoriaDesfazEntradaESeuAndamento() throws Exception {
        var p=repo().registrar(entrada()); var proxy=f.falharAuditoria();
        var repo=new RepositorioTransferenciaExternaJdbc(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->repo.atualizar(p.id(),decisao(p,Situacao.EFETIVADA)));
        assertEquals(1,new RepositorioMatriculaJdbc(f.c).listar(f.aluno).size());
        assertEquals(1,repo().historico(p.id()).size()); semEfeito(p);
    }
    @Test void falhaNaAuditoriaDesfazSaidaCancelamentosEHistorico() throws Exception {
        var p=repo().registrar(saida()); int antes=new RepositorioMatriculaJdbc(f.c).historico(f.matricula).size();
        var proxy=f.falharAuditoria(); var repo=new RepositorioTransferenciaExternaJdbc(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->repo.atualizar(p.id(),decisao(p,Situacao.EFETIVADA)));
        assertEquals(antes,new RepositorioMatriculaJdbc(f.c).historico(f.matricula).size()); semEfeito(p);
        assertTrue(q(f.c,"SELECT id_transferencia FROM transferencia_academica WHERE id_matricula=?",r->r.getLong(1),f.matricula).isEmpty());
    }
}

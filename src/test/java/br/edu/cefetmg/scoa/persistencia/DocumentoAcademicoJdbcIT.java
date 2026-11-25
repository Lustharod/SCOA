package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.DocumentoAcademico.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class DocumentoAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    @BeforeEach void preparar() throws Exception { f=new CenarioAcademicoTeste(); }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioDocumentoAcademicoJdbc aluno() { return new RepositorioDocumentoAcademicoJdbc(f.c,f.sessao(f.alunoConta)); }
    private RepositorioDocumentoAcademicoJdbc outro() { return new RepositorioDocumentoAcademicoJdbc(f.c,f.sessao(f.outroConta)); }
    private RepositorioDocumentoAcademicoJdbc secretaria() { return new RepositorioDocumentoAcademicoJdbc(f.c,f.sessao(f.admin)); }
    private Registro solicitar() { return aluno().solicitar(new Novo(f.matricula,Tipo.HISTORICO_ESCOLAR)); }
    private Atualizacao atualizar(Registro r,Situacao situacao) {
        return new Atualizacao(r.protocolo(),r.versao(),situacao,"Parecer sintético",
                situacao==Situacao.ATENDIDO?"Documento sintético entregue":null);
    }
    @Test void solicitaOsDoisDocumentosComAutoriaDataEAndamentoInicial() throws Exception {
        var h=solicitar(); var d=aluno().solicitar(new Novo(f.matricula,Tipo.DECLARACAO_MATRICULA));
        assertNotEquals(h.protocolo(),d.protocolo());
        assertEquals(f.alunoConta,h.solicitante()); assertEquals(f.hoje,h.data()); assertNotNull(h.solicitadoEm());
        assertEquals("SOLICITADO",h.situacao()); assertEquals(0,h.versao()); assertNull(h.atendente());
        assertEquals(2,aluno().minhasSolicitacoes().size());
        var a=aluno().meuAndamento(h.protocolo()).getFirst();
        assertNull(a.anterior()); assertEquals(f.alunoConta,a.autor());
        assertEquals(2,q(f.c,"SELECT COUNT(*) FROM auditoria WHERE entidade='SOLICITACAO_DOCUMENTO' AND id_usuario=?",r->r.getInt(1),f.alunoConta).getFirst());
    }
    @Test void alunoNaoSolicitaNemConsultaProtocoloDeOutraPessoa() {
        var h=solicitar();
        assertEquals(1,outro().minhasMatriculas().size());
        assertEquals(f.outraMatricula,outro().minhasMatriculas().getFirst().matricula());
        assertThrows(IllegalArgumentException.class,()->outro().solicitar(new Novo(f.matricula,Tipo.HISTORICO_ESCOLAR)));
        assertTrue(outro().minhasSolicitacoes().isEmpty());
        assertThrows(IllegalArgumentException.class,()->outro().meuAndamento(h.protocolo()));
        assertThrows(IllegalArgumentException.class,()->outro().meuAndamento(Long.MAX_VALUE));
    }
    @Test void secretariaConcluiComHistoricoImutavelEEntregaVisivelAoSolicitante() {
        var h=solicitar();
        var em=secretaria().atualizar(atualizar(h,Situacao.EM_ATENDIMENTO));
        var fim=secretaria().atualizar(atualizar(em,Situacao.ATENDIDO));
        assertEquals(2,fim.versao()); assertEquals(f.admin,fim.atendente()); assertNotNull(fim.atualizadoEm());
        var andamento=aluno().meuAndamento(h.protocolo());
        assertEquals(3,andamento.size());
        assertEquals("SOLICITADO",andamento.get(1).anterior());
        assertEquals("EM_ATENDIMENTO",andamento.get(2).anterior());
        assertEquals(fim.referenciaEntrega(),andamento.get(2).referenciaEntrega());
        assertEquals(fim,aluno().minhasSolicitacoes().getFirst());
        assertThrows(IllegalArgumentException.class,()->secretaria().atualizar(atualizar(fim,Situacao.INDEFERIDO)));
    }
    @Test void indeferimentoMantemParecerETerminaProcesso() {
        var h=solicitar(); var fim=secretaria().atualizar(atualizar(h,Situacao.INDEFERIDO));
        assertEquals("INDEFERIDO",fim.situacao()); assertNull(fim.referenciaEntrega());
        assertEquals("Parecer sintético",aluno().meuAndamento(h.protocolo()).getLast().parecer());
        assertThrows(IllegalArgumentException.class,()->secretaria().atualizar(atualizar(fim,Situacao.ATENDIDO)));
    }
    @Test void versaoAntigaNaoSobrescreveOutroAtendimento() {
        var h=solicitar(); secretaria().atualizar(atualizar(h,Situacao.EM_ATENDIMENTO));
        var erro=assertThrows(IllegalArgumentException.class,()->secretaria().atualizar(atualizar(h,Situacao.INDEFERIDO)));
        assertTrue(erro.getMessage().contains("alterada"));
        assertEquals(2,aluno().meuAndamento(h.protocolo()).size());
        assertEquals("EM_ATENDIMENTO",aluno().minhasSolicitacoes().getFirst().situacao());
    }
    @Test void consultarEAtenderExigemConcessoesIndependentesERevalidadas() throws Exception {
        var h=solicitar(); var repo=new RepositorioDocumentoAcademicoJdbc(f.c,f.sessao(f.outroDocente));
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.CONSULTAR);
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.MANTER);
        assertThrows(IllegalArgumentException.class,repo::filaSecretaria);
        assertThrows(IllegalArgumentException.class,()->repo.atualizar(atualizar(h,Situacao.EM_ATENDIMENTO)));
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.DOCUMENTOS_CONSULTAR);
        assertEquals(1,repo.filaSecretaria().size());
        assertThrows(IllegalArgumentException.class,()->repo.atualizar(atualizar(h,Situacao.EM_ATENDIMENTO)));
        f.conceder(f.outroDocente,AcessoAcademicoJdbc.DOCUMENTOS_ATENDER);
        var em=repo.atualizar(atualizar(h,Situacao.EM_ATENDIMENTO));
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.outroDocente);
        assertThrows(IllegalArgumentException.class,repo::filaSecretaria);
        assertThrows(IllegalArgumentException.class,()->repo.atualizar(atualizar(em,Situacao.ATENDIDO)));
    }
    @Test void papeisAcademicosSozinhosNaoAbremFilaNemAtendimento() throws Exception {
        var h=solicitar();
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.alunoConta);
        assertThrows(IllegalArgumentException.class,aluno()::filaSecretaria);
        assertThrows(IllegalArgumentException.class,()->aluno().andamentoSecretaria(h.protocolo()));
        assertThrows(IllegalArgumentException.class,()->aluno().atualizar(atualizar(h,Situacao.ATENDIDO)));
    }
    @Test void sessaoEIdentidadeSaoRevalidadasAntesDeSolicitar() throws Exception {
        assertThrows(IllegalArgumentException.class,new RepositorioDocumentoAcademicoJdbc(f.c,null)::minhasMatriculas);
        var repo=aluno();
        g(f.c,"UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",f.aluno);
        assertThrows(IllegalArgumentException.class,()->repo.solicitar(new Novo(f.matricula,Tipo.HISTORICO_ESCOLAR)));
        g(f.c,"UPDATE aluno SET id_usuario=? WHERE id_aluno=?",f.alunoConta,f.aluno);
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.alunoConta);
        assertThrows(IllegalArgumentException.class,repo::minhasSolicitacoes);
    }
    @Test void historicoDoVinculoTransferidoContinuaSolicitavelSemAlterarMatricula() throws Exception {
        g(f.c,"UPDATE matricula_academica SET situacao='TRANSFERIDO' WHERE id_matricula=?",f.matricula);
        g(f.c,"UPDATE aluno SET situacao='INATIVO' WHERE id_aluno=?",f.aluno);
        assertEquals(f.matricula,solicitar().matricula());
        assertEquals("TRANSFERIDO",aluno().minhasMatriculas().getFirst().situacao());
    }
    @Test void legadoPreservaTipoSituacaoEAutoriaDesconhecidaSemInventarAndamento() throws Exception {
        long id=g(f.c,"INSERT INTO solicitacao_documento(id_matricula,tipo_documento,data_solicitacao,situacao) VALUES (?,'Diploma legado',?,'PENDENTE_ANTIGO')",f.matricula,f.hoje.minusDays(50));
        var legado=aluno().minhasSolicitacoes().getFirst();
        assertNull(legado.solicitante()); assertNull(legado.solicitadoEm()); assertEquals("Diploma legado",legado.tipo());
        assertEquals("PENDENTE_ANTIGO",legado.situacao()); assertTrue(aluno().meuAndamento(id).isEmpty());
        assertThrows(IllegalArgumentException.class,()->secretaria().atualizar(atualizar(legado,Situacao.ATENDIDO)));
    }
    @Test void falhaDeAuditoriaDesfazSolicitacaoEAndamentoInicial() throws Exception {
        var proxy=f.falharAuditoria();
        var repo=new RepositorioDocumentoAcademicoJdbc(proxy,f.sessao(proxy,f.alunoConta));
        var erro=assertThrows(PersistenciaException.class,()->repo.solicitar(new Novo(f.matricula,Tipo.HISTORICO_ESCOLAR)));
        assertTrue(erro.getCause().getMessage().contains("Falha sintética de auditoria"));
        assertTrue(aluno().minhasSolicitacoes().isEmpty());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM andamento_documento",r->r.getInt(1)).getFirst());
    }
    @Test void falhaDeAuditoriaDesfazAtendimentoSemDuplicarTextoPrivadoNoLog() throws Exception {
        var h=solicitar(); var proxy=f.falharAuditoria();
        var repo=new RepositorioDocumentoAcademicoJdbc(proxy,f.sessao(proxy,f.admin));
        var erro=assertThrows(PersistenciaException.class,()->repo.atualizar(atualizar(h,Situacao.ATENDIDO)));
        assertTrue(erro.getCause().getMessage().contains("Falha sintética de auditoria"));
        assertEquals(h,aluno().minhasSolicitacoes().getFirst()); assertEquals(1,aluno().meuAndamento(h.protocolo()).size());
        secretaria().atualizar(atualizar(h,Situacao.ATENDIDO));
        var logs=q(f.c,"SELECT detalhes FROM auditoria WHERE entidade='SOLICITACAO_DOCUMENTO' AND id_registro=?",r->r.getString(1),h.protocolo());
        assertTrue(logs.stream().noneMatch(l->l.contains("Parecer sintético") || l.contains("Documento sintético entregue")));
        assertFalse(f.c.getAutoCommit());
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class AtendimentoAlunoJdbcIT {
    private CenarioAcademicoTeste f;
    @BeforeEach void preparar() throws Exception { f=new CenarioAcademicoTeste(); }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioAtendimentoAlunoJdbc meu() { return new RepositorioAtendimentoAlunoJdbc(f.c,f.sessao(f.alunoConta)); }

    @Test void historicoIncluiSomenteVinculosDaPessoaSemPresumirResultado() throws Exception {
        f.inscrever(f.outraMatricula,"ATIVO");
        f.inscrever(f.matricula,"CANCELADO");
        var historico=meu().historico();
        assertEquals(f.aluno,historico.aluno());
        assertEquals(1,historico.matriculas().size());
        assertEquals(2,historico.disciplinas().size());
        assertTrue(historico.disciplinas().stream().allMatch(d->d.matricula()==f.matricula));
        assertTrue(historico.disciplinas().stream().allMatch(d->d.mediaFinal()==null && d.minutosPresentes()==null));
        assertTrue(historico.disciplinas().stream().anyMatch(d->d.situacaoInscricao().equals("CANCELADO")));
        assertFalse(f.c.getAutoCommit());
    }
    @Test void resultadoFormalEDadoAusenteContinuamDistintos() throws Exception {
        g(f.c,"UPDATE inscricao_disciplina SET situacao='APROVADO' WHERE id_inscricao=?",f.inscricao);
        g(f.c,"""
                INSERT INTO resultado_inscricao(id_inscricao,p1,p2,media_inicial,total_minutos,presentes_minutos,situacao,motivo)
                VALUES (?,8,8,8,100,100,'APROVADO','Fixture de resultado sem média final informada')
                """,f.inscricao);
        var item=meu().historico().disciplinas().getFirst();
        assertTrue(item.resultadoCoerente());
        assertNull(item.mediaFinal());
        assertEquals(100L,item.minutosPresentes());
        g(f.c,"UPDATE inscricao_disciplina SET situacao='REPROVADO' WHERE id_inscricao=?",f.inscricao);
        assertFalse(meu().historico().disciplinas().getFirst().resultadoCoerente());
    }
    @Test void reconhecimentosVigentesSuspensosRevogadosELegadosNaoSeConfundem() throws Exception {
        long vigente=f.reconhecer(f.disciplina("V"),null);
        long suspenso=f.reconhecer(f.disciplina("S"),"ABERTA");
        long revogado=f.reconhecer(f.disciplina("R"),"REVOGADA");
        g(f.c,"""
                INSERT INTO aproveitamento_equivalencia(id_matricula,id_disciplina_destino,decisao,data,resultado)
                VALUES (?,?,'DEFERIDO',CURRENT_DATE,'Legado preservado')
                """,f.matricula,f.disciplina);
        var historico=meu().historico();
        assertEquals(3,historico.reconhecimentos().size());
        assertTrue(historico.reconhecimentos().stream().filter(p->p.pedido()==vigente).findFirst().orElseThrow().vigente());
        assertFalse(historico.reconhecimentos().stream().filter(p->p.pedido()==suspenso).findFirst().orElseThrow().vigente());
        assertFalse(historico.reconhecimentos().stream().filter(p->p.pedido()==revogado).findFirst().orElseThrow().vigente());
        assertEquals(1,historico.legados().size());
        assertEquals(3,q(f.c,"SELECT COUNT(*) FROM pedido_equivalencia WHERE id_matricula=?",r->r.getInt(1),f.matricula).getFirst());
    }
    @Test void reconhecimentoFuturoOuEstruturaDivergenteNaoEApresentadoComoVigente() throws Exception {
        long id=f.reconhecer(f.disciplina("F"),null);
        g(f.c,"UPDATE pedido_equivalencia SET conclusao_origem=? WHERE id_pedido=?",f.hoje.plusDays(1),id);
        assertFalse(meu().historico().reconhecimentos().getFirst().vigente());
        g(f.c,"UPDATE pedido_equivalencia SET conclusao_origem=? WHERE id_pedido=?",f.hoje.minusDays(1),id);
        g(f.c,"DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",f.matricula);
        assertFalse(meu().historico().reconhecimentos().getFirst().vigente());
    }
    @Test void contaSemAlunoOuSemSessaoNaoAcessaAtendimento() {
        assertThrows(IllegalArgumentException.class,new RepositorioAtendimentoAlunoJdbc(f.c,null)::historico);
        assertThrows(IllegalArgumentException.class,new RepositorioAtendimentoAlunoJdbc(f.c,f.sessao(f.admin))::manifestacoes);
    }
    @Test void historicoVazioNaoCriaMatriculaOuPlano() throws Exception {
        var repo=new RepositorioAtendimentoAlunoJdbc(f.c,f.sessao(f.outroConta));
        g(f.c,"DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",f.outraMatricula);
        g(f.c,"DELETE FROM matricula_academica WHERE id_matricula=?",f.outraMatricula);
        var h=repo.historico();
        assertTrue(h.matriculas().isEmpty()); assertTrue(h.disciplinas().isEmpty());
        assertTrue(q(f.c,"SELECT id_matricula FROM matricula_academica WHERE id_aluno=?",r->r.getLong(1),f.outroAluno).isEmpty());
    }
    @Test void registroDeManifestacaoGuardaAutorEAuditoriaSemCopiarConteudoParaLog() throws Exception {
        var registro=meu().registrar(new NovaManifestacao(TipoManifestacao.SUGESTAO,"Sugestão sintética ' aspas"));
        assertEquals(f.alunoConta,registro.autor());
        assertEquals("SUGESTAO",registro.tipo());
        assertEquals(1,meu().manifestacoes().size());
        var evento=q(f.c,"SELECT id_usuario,detalhes FROM auditoria WHERE entidade='MANIFESTACAO_ALUNO' AND id_registro=?",
                r->new Object[]{r.getLong(1),r.getString(2)},registro.id()).getFirst();
        assertEquals(f.alunoConta,evento[0]);
        assertFalse(evento[1].toString().contains(registro.conteudo()));
        assertTrue(new RepositorioAtendimentoAlunoJdbc(f.c,f.sessao(f.outroConta)).manifestacoes().isEmpty());
    }
    @Test void manifestacaoLegadaMantemAutorDesconhecidoETipoOriginal() throws Exception {
        g(f.c,"INSERT INTO manifestacao_aluno(id_aluno,tipo_manifestacao,conteudo,data_hora) VALUES (?,'TIPO_LEGADO','Original',CURRENT_TIMESTAMP())",f.aluno);
        var registro=meu().manifestacoes().getFirst();
        assertNull(registro.autor()); assertEquals("TIPO_LEGADO",registro.tipo()); assertEquals("Original",registro.conteudo());
    }
    @Test void revogacaoDoVinculoEInativacaoSaoRevalidadasAoEnviar() throws Exception {
        var repo=meu();
        g(f.c,"UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",f.aluno);
        assertThrows(IllegalArgumentException.class,()->repo.registrar(new NovaManifestacao(TipoManifestacao.SUGESTAO,"Texto")));
        g(f.c,"UPDATE aluno SET id_usuario=? WHERE id_aluno=?",f.alunoConta,f.aluno);
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.alunoConta);
        assertThrows(IllegalArgumentException.class,repo::historico);
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",r->r.getInt(1),f.aluno).getFirst());
    }
    @Test void alunoInativoAindaConsultaSeuHistoricoSemReceberPermissaoTecnica() throws Exception {
        g(f.c,"UPDATE aluno SET situacao='INATIVO' WHERE id_aluno=?",f.aluno);
        assertEquals(f.aluno,meu().historico().aluno());
        assertThrows(IllegalArgumentException.class,()->RepositoriosAcademicosAutorizados.baseAcademica(f.c,f.sessao(f.alunoConta)).listarAlunos());
    }
    @Test void falhaDeAuditoriaDesfazManifestacao() throws Exception {
        var proxy=f.falharAuditoria();
        var repo=new RepositorioAtendimentoAlunoJdbc(proxy,f.sessao(proxy,f.alunoConta));
        assertThrows(PersistenciaException.class,()->repo.registrar(new NovaManifestacao(TipoManifestacao.RECLAMACAO,"Texto sintético")));
        assertTrue(meu().manifestacoes().isEmpty());
        assertFalse(f.c.getAutoCommit());
    }
}

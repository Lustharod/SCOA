package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.GestaoAcademica.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class GestaoAcademicaJdbcIT {
    private CenarioAcademicoTeste f;
    private SegurancaJdbc.Sessao admin,coord,gestor,docente;
    private long cursoB,matriculaB,disciplinaB,professorB,turmaB,avaliacao;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste(); admin=f.sessao(f.admin); coord=f.sessao(f.outroDocente); gestor=f.sessao(f.outroConta); docente=f.sessao(f.docente);
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.outroDocente);
        new SegurancaJdbc(f.c).vincularGestorAcademico(admin,f.outroConta);
        cursoB=g(f.c,"INSERT INTO curso(nome,situacao) VALUES ('Curso B sintético','ATIVO')");
        long estruturaB=g(f.c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,'Estrutura B','ATIVO')",cursoB);
        disciplinaB=f.disciplina("B"); g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?),(?,?)",estruturaB,f.disciplina,estruturaB,disciplinaB);
        matriculaB=g(f.c,"INSERT INTO matricula_academica(id_aluno,id_curso,id_periodo_ingresso,data_ingresso,situacao) VALUES (?,?,?,?,'ATIVO')",f.outroAluno,cursoB,f.periodo,f.hoje.minusDays(100));
        g(f.c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",matriculaB,estruturaB);
        long inscricaoB=g(f.c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",matriculaB,f.turma,f.hoje.minusDays(100));
        professorB=q(f.c,"SELECT id_professor FROM professor WHERE id_usuario=?",r->r.getLong(1),f.outroDocente).getFirst();
        turmaB=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Turma B',20,'ATIVO')",disciplinaB,professorB,f.periodo);
        var notas=new RepositorioDiarioAcademicoJdbc(f.c,docente);
        avaliacao=notas.registrarAvaliacao(new NovaAvaliacao(f.turma,Prova.P1,f.hoje.minusDays(10),"Prova compartilhada")).id();
        notas.lancarNota(new LancarNota(avaliacao,f.inscricao,new BigDecimal("8"),-1,"Fixture do curso A"));
        notas.lancarNota(new LancarNota(avaliacao,inscricaoB,new BigDecimal("4"),-1,"Fixture do curso B"));
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioGestaoAcademicaJdbc coord() { return new RepositorioGestaoAcademicaJdbc(f.c,coord); }
    private RepositorioGestaoAcademicaJdbc gestor() { return new RepositorioGestaoAcademicaJdbc(f.c,gestor); }
    private NovaMeta nova(long curso,long professor) { return new NovaMeta(curso,professor,f.periodo,TipoMeta.META,"Conferir lançamentos",f.hoje.plusDays(2)); }
    @Test void gestorAlcancaTodosOsCursosECoordenadorSomenteSeusCursosSemAdminImplicito() {
        assertEquals(List.of(f.curso),coord().cursos().stream().map(CursoDisponivel::id).toList());
        assertEquals(Set.of(f.curso,cursoB),new HashSet<>(gestor().cursos().stream().map(CursoDisponivel::id).toList()));
        assertTrue(new RepositorioGestaoAcademicaJdbc(f.c,admin).cursos().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new RepositorioGestaoAcademicaJdbc(f.c,admin).diario(f.curso,f.turma));
        assertThrows(IllegalArgumentException.class,()->coord().docentes(cursoB));
    }
    @Test void docenteForaDoCursoNaoEExpostoNemRecebeMetaPelaCoordenacao() {
        assertEquals(List.of(f.professor),coord().docentes(f.curso).stream().map(Docente::id).toList());
        assertThrows(IllegalArgumentException.class,()->coord().registrarMeta(nova(f.curso,professorB)));
        assertNotNull(gestor().registrarMeta(nova(cursoB,professorB)));
    }
    @Test void vinculoInstitucionalUsaContaExistenteSemConcederPermissoesTecnicas() {
        var seguranca=new SegurancaJdbc(f.c); var conta=seguranca.minhaConta(gestor);
        assertTrue(conta.permissoesTecnicas().isEmpty()); assertEquals(f.outroConta,conta.idConta());
        assertTrue(conta.vinculos().stream().anyMatch(v->v.tipo().equals("ALUNO")));
        var vinculo=conta.vinculos().stream().filter(v->v.tipo().equals("GESTOR")).findFirst().orElseThrow();
        assertEquals(vinculo.referencia(),seguranca.vincularGestorAcademico(admin,f.outroConta));
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularGestorAcademico(docente,f.docente));
        assertThrows(IllegalArgumentException.class,()->RepositoriosAcademicosAutorizados.baseAcademica(f.c,gestor).listarAlunos());
    }
    @Test void desativacaoDoGestorECoordenacaoRevogadaSaoRevalidadas() throws Exception {
        var repo=gestor(); new SegurancaJdbc(f.c).definirSituacaoGestor(admin,f.outroConta,"INATIVO");
        assertTrue(repo.cursos().isEmpty()); assertThrows(IllegalArgumentException.class,()->repo.diario(cursoB,f.turma));
        new SegurancaJdbc(f.c).definirSituacaoGestor(admin,f.outroConta,"ATIVO"); assertEquals(2,repo.cursos().size());
        g(f.c,"DELETE FROM coordenacao_curso WHERE id_usuario=?",f.outroDocente);
        assertThrows(IllegalArgumentException.class,()->coord().diario(f.curso,f.turma));
    }
    @Test void contaInativaOuSessaoInvalidaNaoAcessaGestaoMesmoComVinculo() throws Exception {
        assertThrows(IllegalArgumentException.class,new RepositorioGestaoAcademicaJdbc(f.c,null)::cursos);
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.outroConta);
        assertThrows(IllegalArgumentException.class,gestor()::cursos);
    }
    @Test void metaTemAutoriaVersaoHistoricoERecusaAtualizacaoAntiga() {
        var m=coord().registrarMeta(nova(f.curso,f.professor)); assertEquals(f.outroDocente,m.autor()); assertEquals(0,m.versao());
        var novo=coord().atualizarMeta(new AtualizarMeta(m.id(),0,"Descrição revisada",f.hoje.plusDays(3),SituacaoMeta.EM_ANDAMENTO,"Replanejamento"));
        assertEquals(1,novo.versao());
        assertThrows(IllegalArgumentException.class,()->coord().atualizarMeta(new AtualizarMeta(m.id(),0,"Tela antiga",f.hoje.plusDays(4),SituacaoMeta.CONCLUIDA,"Motivo")));
        var h=coord().historicoMeta(m.id()); assertEquals(2,h.size()); assertNull(h.getFirst().anterior()); assertTrue(h.getLast().anterior().contains("Conferir lançamentos"));
        assertTrue(h.getLast().posterior().contains("Descrição revisada"));
    }
    @Test void metaDeOutroCursoNaoPodeSerAlteradaOuLidaPelaCoordenacao() {
        var m=gestor().registrarMeta(nova(cursoB,professorB));
        assertThrows(IllegalArgumentException.class,()->coord().historicoMeta(m.id()));
        assertThrows(IllegalArgumentException.class,()->coord().atualizarMeta(new AtualizarMeta(m.id(),0,"Alteração indevida",f.hoje,SituacaoMeta.CANCELADA,"Motivo")));
        assertThrows(IllegalArgumentException.class,()->coord().metas(cursoB,null));
    }
    @Test void metaConcluidaPreservaHistoricoESituacaoFinal() {
        var m=coord().registrarMeta(nova(f.curso,f.professor));
        coord().atualizarMeta(new AtualizarMeta(m.id(),0,m.descricao(),m.prazo(),SituacaoMeta.CONCLUIDA,"Conferência concluída"));
        assertThrows(IllegalArgumentException.class,()->coord().atualizarMeta(new AtualizarMeta(m.id(),1,m.descricao(),m.prazo(),SituacaoMeta.ABERTA,"Reabrir")));
        assertEquals("CONCLUIDA",coord().metas(f.curso,null).getFirst().situacao());
    }
    @Test void turmaCompartilhadaRevelaSomenteAlunosDoCursoSelecionado() {
        var a=coord().diario(f.curso,f.turma); assertEquals(1,a.alunos().size()); assertEquals(f.matricula,a.alunos().getFirst().matricula());
        assertEquals(new BigDecimal("8.00"),a.alunos().getFirst().lancamentos().notas().getFirst().valor());
        assertFalse(a.toString().contains("identificacao="+f.tag+"-outro"));
        var b=gestor().diario(cursoB,f.turma); assertEquals(1,b.alunos().size()); assertEquals(matriculaB,b.alunos().getFirst().matricula());
        assertEquals(new BigDecimal("4.00"),b.alunos().getFirst().lancamentos().notas().getFirst().valor());
        assertThrows(IllegalArgumentException.class,()->coord().diario(cursoB,f.turma));
        assertThrows(IllegalArgumentException.class,()->coord().diario(f.curso,turmaB));
    }
    @Test void avaliacoesRespeitamCursoTurmaPeriodoEDisciplinaSemAlterarDados() {
        var lista=coord().avaliacoes(new Filtro(f.curso,f.periodo,f.disciplina,f.turma,null)); assertEquals(1,lista.size()); assertEquals(avaliacao,lista.getFirst().avaliacao().id());
        assertTrue(coord().avaliacoes(new Filtro(f.curso,4_294_967_295L,null,null,null)).isEmpty());
        assertThrows(IllegalArgumentException.class,()->coord().avaliacoes(new Filtro(cursoB,null,null,null,null)));
    }
    @Test void auditoriaPreservaDadosOriginaisERegistraRetratoConclusaoEAtor() throws Exception {
        var d=coord().diario(f.curso,f.turma);
        var a=coord().auditar(new NovaAuditoria(f.curso,f.turma,d.assinatura(),ConclusaoAuditoria.REQUER_TRATAMENTO,"Conferir P2 e chamada pendentes"));
        assertEquals(f.outroDocente,a.autor()); assertEquals("REQUER_TRATAMENTO",a.conclusao()); assertEquals(d.assinatura(),a.assinatura());
        assertTrue(a.dadosAvaliados().contains(f.tag)); assertFalse(a.dadosAvaliados().contains("identificacao="+f.tag+"-outro"));
        assertEquals(1,coord().auditorias(f.curso,f.turma).size());
        assertEquals(new BigDecimal("8.00"),q(f.c,"SELECT valor FROM nota WHERE id_avaliacao=? AND id_inscricao=?",r->r.getBigDecimal(1),avaliacao,f.inscricao).getFirst());
        assertTrue(q(f.c,"SELECT id_inscricao FROM resultado_inscricao WHERE id_inscricao=?",r->r.getLong(1),f.inscricao).isEmpty());
    }
    @Test void alteracaoAposConsultaRecusaAuditoriaAntigaSemCriarConclusao() throws Exception {
        var d=coord().diario(f.curso,f.turma);
        new RepositorioDiarioAcademicoJdbc(f.c,docente).lancarNota(new LancarNota(avaliacao,f.inscricao,new BigDecimal("9"),0,"Correção durante análise"));
        assertThrows(IllegalArgumentException.class,()->coord().auditar(new NovaAuditoria(f.curso,f.turma,d.assinatura(),ConclusaoAuditoria.SEM_RESSALVAS,"Observação")));
        assertTrue(coord().auditorias(f.curso,f.turma).isEmpty());
    }
    @Test void monitoramentoNaoCriaPlanoNemPresumeResultadoAusenteEFiltraCurso() throws Exception {
        var linhas=coord().monitorar(new Filtro(f.curso,null,null,f.turma,null)); assertEquals(1,linhas.size()); assertEquals(f.matricula,linhas.getFirst().matricula());
        assertEquals(0,linhas.getFirst().cumpridos()); assertEquals(1,linhas.getFirst().emAndamento()); assertEquals("SEM_PLANO",linhas.getFirst().plano());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM plano_institucional",r->r.getInt(1)).getFirst());
        assertThrows(IllegalArgumentException.class,()->coord().monitorar(new Filtro(cursoB,null,null,null,null)));
    }
    @Test void monitoramentoComDadosInconsistentesNaoInventaContagensZero() throws Exception {
        g(f.c,"DELETE FROM vinculo_curricular_matricula WHERE id_matricula=?",f.matricula);
        var linha=coord().monitorar(new Filtro(f.curso,null,null,null,f.aluno)).getFirst();
        assertNull(linha.componentes()); assertNull(linha.cumpridos()); assertEquals("DADOS_INSUFICIENTES",linha.plano());
    }
    @Test void gestaoNaoGanhaAutoriaDocentePelaAuditoriaOuPeloVinculoInstitucional() {
        var diario=new RepositorioDiarioAcademicoJdbc(f.c,gestor);
        assertThrows(IllegalArgumentException.class,()->diario.lancarNota(new LancarNota(avaliacao,f.inscricao,BigDecimal.TEN,0,"Alteração pela gestão")));
        assertThrows(IllegalArgumentException.class,()->diario.fechar(f.inscricao,"Fechar pela gestão"));
    }
    @Test void dadosLegadosDeMetaEAuditoriaSaoPreservadosSemAtribuirCursoOuTipo() throws Exception {
        long id=g(f.c,"INSERT INTO prazo_meta_docente(id_professor,descricao,prazo,situacao) VALUES (?,'Original',?,'LEGADO')",f.professor,f.hoje);
        assertThrows(IllegalArgumentException.class,()->gestor().atualizarMeta(new AtualizarMeta(id,0,"Reinterpretar",f.hoje,SituacaoMeta.ABERTA,"Motivo")));
        assertNull(q(f.c,"SELECT tipo FROM prazo_meta_docente WHERE id_prazo_meta=?",r->Optional.ofNullable(r.getString(1)),id).getFirst().orElse(null));
        assertTrue(coord().metas(f.curso,null).isEmpty());
    }
    @Test void falhaNaAuditoriaTecnicaDesfazMetaEAuditoriaDoDiario() throws Exception {
        var proxy=f.falharAuditoria(); var falho=new RepositorioGestaoAcademicaJdbc(proxy,f.sessao(proxy,f.outroDocente));
        assertThrows(PersistenciaException.class,()->falho.registrarMeta(nova(f.curso,f.professor)));
        assertTrue(coord().metas(f.curso,null).isEmpty());
        var d=coord().diario(f.curso,f.turma);
        assertThrows(PersistenciaException.class,()->falho.auditar(new NovaAuditoria(f.curso,f.turma,d.assinatura(),ConclusaoAuditoria.COM_RESSALVAS,"Conferir pendências")));
        assertTrue(coord().auditorias(f.curso,f.turma).isEmpty());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM diario_classe WHERE id_turma=?",r->r.getInt(1),f.turma).getFirst()); assertFalse(f.c.getAutoCommit());
    }
    @Test void falhaDeAuditoriaAoVincularGestorNaoConcedePapelOuPermissao() throws Exception {
        var proxy=f.falharAuditoria(); var seguranca=new SegurancaJdbc(proxy);
        assertThrows(PersistenciaException.class,()->seguranca.vincularGestorAcademico(f.sessao(proxy,f.admin),f.alunoConta));
        assertTrue(q(f.c,"SELECT id_gestor FROM gestao_academica WHERE id_usuario=?",r->r.getLong(1),f.alunoConta).isEmpty());
        assertTrue(new SegurancaJdbc(f.c).minhaConta(f.sessao(f.alunoConta)).permissoesTecnicas().isEmpty());
    }
}

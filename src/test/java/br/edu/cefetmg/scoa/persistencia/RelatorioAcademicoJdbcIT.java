package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class RelatorioAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    @BeforeEach void preparar() throws Exception { f=new CenarioAcademicoTeste(); }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioRelatorioAcademicoJdbc repo(long usuario) { return new RepositorioRelatorioAcademicoJdbc(f.c,f.sessao(usuario)); }
    private Filtro censo() { return new Filtro(Tipo.CENSO_DEMONSTRATIVO,f.curso,f.periodo,null,null); }
    private Filtro historico() { return new Filtro(Tipo.HISTORICO_PROPRIO,null,null,null,null); }
    private Filtro diario() { return new Filtro(Tipo.DIARIO_DOCENTE,null,null,f.turma,null); }
    private Geracao pedido(Previa p) { return new Geracao(UUID.randomUUID().toString(),p.filtro(),p.assinatura()); }
    private Registro gerar(long usuario,Filtro filtro) { var r=repo(usuario); return r.gerar(pedido(r.preparar(filtro))); }
    private int total() throws Exception { return q(f.c,"SELECT COUNT(*) FROM relatorio_academico",r->r.getInt(1)).getFirst(); }
    @Test void previaNaoGravaRelatorioNemArquivosEIdentificaPopulacaoSemResultadoPresumido() throws Exception {
        var p=repo(f.admin).preparar(censo()); assertTrue(p.valida()); assertEquals(0,total());
        assertEquals(2,p.documento().tabelas().getFirst().linhas().size());
        var inscricao=p.documento().tabelas().get(1).linhas().getFirst();
        assertEquals("SEM_RESULTADO",inscricao.get(5)); assertEquals("",inscricao.get(6));
        assertTrue(p.documento().avisos().stream().anyMatch(a->a.contains("DEMONSTRATIVO")));
    }
    @Test void geracaoArquivaDoisFormatosReaisMesmoRetratoComAutoriaEIntegridade() throws Exception {
        var repo=repo(f.admin); var p=repo.preparar(censo()); var r=repo.gerar(pedido(p));
        assertEquals(f.admin,r.autor()); assertEquals(p.documento(),r.documento()); assertEquals(p.assinatura(),r.assinatura());
        assertEquals(r,repo.consultar(r.id())); assertEquals(1,total());
        assertEquals(f.admin,q(f.c,"SELECT id_usuario FROM auditoria WHERE entidade='RELATORIO_ACADEMICO' AND id_registro=?",x->x.getLong(1),r.id()).getFirst());
        var pdf=repo.arquivo(r.id(),Formato.PDF); var xlsx=repo.arquivo(r.id(),Formato.XLSX);
        assertEquals("%PDF-",new String(pdf.conteudo(),0,5,java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals(80,Byte.toUnsignedInt(xlsx.conteudo()[0])); assertEquals(75,Byte.toUnsignedInt(xlsx.conteudo()[1]));
        assertEquals(64,pdf.sha256().length()); assertNotEquals(pdf.sha256(),xlsx.sha256());
        assertArrayEquals(pdf.conteudo(),repo.arquivo(r.id(),Formato.PDF).conteudo());
    }
    @Test void registroNaoEReescritoAposMudancaDeDadosERetornaMesmosArquivos() throws Exception {
        var repo=repo(f.alunoConta); var p=repo.preparar(historico()); var pedido=pedido(p); var r=repo.gerar(pedido);
        byte[] original=repo.arquivo(r.id(),Formato.PDF).conteudo();
        g(f.c,"UPDATE aluno SET identificacao='Nome posterior' WHERE id_aluno=?",f.aluno);
        assertNotEquals(p.assinatura(),repo.preparar(historico()).assinatura());
        assertEquals(r,repo.consultar(r.id())); assertEquals(r,repo.gerar(pedido));
        assertArrayEquals(original,repo.arquivo(r.id(),Formato.PDF).conteudo()); assertEquals(1,total());
    }
    @Test void dadosMudadosEntrePreviaEConfirmacaoRecusamGeracaoSemParcial() throws Exception {
        var repo=repo(f.admin); var p=repo.preparar(censo());
        g(f.c,"UPDATE aluno SET identificacao='Nome alterado na origem' WHERE id_aluno=?",f.aluno);
        assertThrows(IllegalArgumentException.class,()->repo.gerar(pedido(p))); assertEquals(0,total());
    }
    @Test void colisaoDeReferenciaNaoRetornaRelatorioDeOutraPessoa() throws Exception {
        var repo=repo(f.alunoConta); var p=repo.preparar(historico()); var pedido=pedido(p); repo.gerar(pedido);
        var outra=repo(f.outroConta); var segunda=outra.preparar(historico());
        assertThrows(IllegalArgumentException.class,()->outra.gerar(new Geracao(pedido.chave(),historico(),segunda.assinatura())));
        assertEquals(1,total());
    }
    @Test void alunoSomenteConsultaEExportaSeusDadosSemAceitarIdDeOutraPessoa() throws Exception {
        var r=gerar(f.alunoConta,historico());
        assertFalse(r.documento().toString().contains(f.tag+"-outro"));
        assertThrows(IllegalArgumentException.class,()->repo(f.outroConta).consultar(r.id()));
        assertThrows(IllegalArgumentException.class,()->repo(f.admin).arquivo(r.id(),Formato.PDF));
        assertTrue(repo(f.outroConta).meusRelatorios().isEmpty());
        assertThrows(IllegalArgumentException.class,()->repo(f.alunoConta).preparar(new Filtro(Tipo.PLANO_PROPRIO,null,null,null,f.outraMatricula)));
    }
    @Test void professorResponsavelObtemDiarioCompletoSemDarPoderAoAdminOuOutroProfessor() {
        var r=gerar(f.docente,diario()); var notas=r.documento().tabelas().stream().filter(t->t.titulo().equals("Notas originais")).findFirst().orElseThrow();
        assertEquals(0,notas.linhas().size());
        assertThrows(IllegalArgumentException.class,()->repo(f.admin).preparar(diario()));
        assertThrows(IllegalArgumentException.class,()->repo(f.outroDocente).consultar(r.id()));
    }
    @Test void diarioDistingueResultadoVigenteDeLancamentosOriginaisSemAlterarResultado() throws Exception {
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,100,100,"Resultado formal sintético");
        var r=gerar(f.docente,diario());
        var vigente=r.documento().tabelas().stream().filter(t->t.titulo().equals("Resultados vigentes")).findFirst().orElseThrow().linhas().getFirst();
        assertEquals("APROVADO",vigente.get(8)); assertEquals(0,new BigDecimal("8").compareTo(new BigDecimal(vigente.get(1))));
        assertEquals("APROVADO",q(f.c,"SELECT situacao FROM resultado_inscricao WHERE id_inscricao=?",x->x.getString(1),f.inscricao).getFirst());
    }
    @Test void coordenacaoDeTurmaCompartilhadaNaoRecebeAlunosDeOutroCursoNemNoArquivo() throws Exception {
        long cursoB=g(f.c,"INSERT INTO curso(nome,situacao) VALUES ('Curso B','ATIVO')");
        long estruturaB=g(f.c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,'B','ATIVO')",cursoB);
        g(f.c,"INSERT INTO estrutura_disciplina VALUES (?,?)",estruturaB,f.disciplina);
        g(f.c,"UPDATE matricula_academica SET id_curso=? WHERE id_matricula=?",cursoB,f.outraMatricula);
        g(f.c,"UPDATE vinculo_curricular_matricula SET id_estrutura=? WHERE id_matricula=?",estruturaB,f.outraMatricula);
        f.inscrever(f.outraMatricula,"ATIVO");
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.outroDocente);
        var filtro=new Filtro(Tipo.DIARIO_GESTAO,f.curso,null,f.turma,null); var r=gerar(f.outroDocente,filtro);
        var alunos=r.documento().tabelas().stream().filter(t->t.titulo().equals("Resumo por inscrição")).findFirst().orElseThrow().linhas();
        assertEquals(1,alunos.size()); assertEquals(Long.toString(f.matricula),alunos.getFirst().getFirst());
        assertFalse(r.documento().toString().contains(f.tag+"-outro"));
        assertThrows(IllegalArgumentException.class,()->repo(f.outroDocente).preparar(new Filtro(Tipo.DIARIO_GESTAO,cursoB,null,f.turma,null)));
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(repo(f.outroDocente).arquivo(r.id(),Formato.PDF).conteudo())) {
            assertFalse(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).contains(f.tag+"-outro"));
        }
    }
    @Test void gestorAcompanhaTodosCursosMasNaoGanhaRelatorioMecSemConcessaoTecnica() throws Exception {
        new SegurancaJdbc(f.c).vincularGestorAcademico(f.sessao(f.admin),f.outroConta);
        assertTrue(repo(f.outroConta).preparar(new Filtro(Tipo.DIARIO_GESTAO,f.curso,null,f.turma,null)).valida());
        assertThrows(IllegalArgumentException.class,()->repo(f.outroConta).preparar(censo()));
        f.conceder(f.outroConta,AcessoAcademicoJdbc.RELATORIOS_GERAR);
        assertTrue(repo(f.outroConta).preparar(censo()).valida());
    }
    @Test void permissaoRevogadaBloqueiaAteArquivoJaGeradoSemApagarRegistro() throws Exception {
        f.conceder(f.outroConta,AcessoAcademicoJdbc.RELATORIOS_GERAR); var repo=repo(f.outroConta);
        var r=repo.gerar(pedido(repo.preparar(censo())));
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.outroConta);
        assertThrows(IllegalArgumentException.class,()->repo.arquivo(r.id(),Formato.XLSX)); assertTrue(repo.meusRelatorios().isEmpty()); assertEquals(1,total());
    }
    @Test void trocaDoProfessorOuContaInativaImpedeAcessoAoArquivoAnterior() throws Exception {
        var repo=repo(f.docente); var r=repo.gerar(pedido(repo.preparar(diario())));
        long outro=q(f.c,"SELECT id_professor FROM professor WHERE id_usuario=?",x->x.getLong(1),f.outroDocente).getFirst();
        g(f.c,"UPDATE turma SET id_professor=? WHERE id_turma=?",outro,f.turma);
        assertThrows(IllegalArgumentException.class,()->repo.arquivo(r.id(),Formato.PDF));
        var aluno=gerar(f.alunoConta,historico()); g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.alunoConta);
        assertThrows(IllegalArgumentException.class,()->repo(f.alunoConta).arquivo(aluno.id(),Formato.PDF));
    }
    @Test void enadeDemonstrativoNaoInventaClassificacaoMesmoComTodosComponentesCumpridos() throws Exception {
        new RepositorioInscricaoJdbc(f.c).registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,100,100,"Aprovação sintética");
        var filtro=new Filtro(Tipo.ENADE_DEMONSTRATIVO,f.curso,f.periodo,null,null); var p=repo(f.admin).preparar(filtro);
        assertTrue(p.valida()); var primeira=p.documento().tabelas().getLast().linhas().getFirst();
        assertEquals("1",primeira.get(4)); assertEquals("SEM_CLASSIFICACAO_OFICIAL",primeira.getLast());
        assertTrue(p.documento().avisos().stream().anyMatch(a->a.contains("Não define aptidão")));
    }
    @Test void incoerenciaDeResultadoImpedeGeracaoSemEstimarValores() throws Exception {
        g(f.c,"UPDATE inscricao_disciplina SET situacao='APROVADO' WHERE id_inscricao=?",f.inscricao);
        var repo=repo(f.admin); var p=repo.preparar(censo()); assertFalse(p.valida()); assertFalse(p.inconsistencias().isEmpty());
        assertThrows(IllegalArgumentException.class,()->repo.gerar(pedido(p))); assertEquals(0,total());
    }
    @Test void reconhecimentoFuturoImpedeRelatorioDeTrajetoriaSemReclassificarOrigem() throws Exception {
        long d=f.disciplina("R"); long id=f.reconhecer(d,null);
        g(f.c,"UPDATE pedido_equivalencia SET conclusao_origem=? WHERE id_pedido=?",f.hoje.plusDays(1),id);
        var filtro=new Filtro(Tipo.ENADE_DEMONSTRATIVO,f.curso,f.periodo,null,null); var p=repo(f.admin).preparar(filtro);
        assertFalse(p.valida()); assertThrows(IllegalArgumentException.class,()->repo(f.admin).gerar(pedido(p))); assertEquals(0,total());
    }
    @Test void planoInexistenteNaoEAutomaticamenteCriadoParaExportar() throws Exception {
        var repo=repo(f.alunoConta); var p=repo.preparar(new Filtro(Tipo.PLANO_PROPRIO,null,null,null,f.matricula));
        assertFalse(p.valida()); assertThrows(IllegalArgumentException.class,()->repo.gerar(pedido(p)));
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM plano_institucional",x->x.getInt(1)).getFirst()); assertEquals(0,total());
    }
    @Test void planoRegistradoDesatualizadoEExportadoComoRetratoSemAtualizacaoAutomatica() throws Exception {
        g(f.c,"UPDATE inscricao_disciplina SET situacao='CANCELADO' WHERE id_inscricao=?",f.inscricao);
        long periodo=g(f.c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES ('Futuro',?,?,'ATIVO')",f.hoje.plusDays(10),f.hoje.plusDays(100));
        var planos=new RepositorioPlanoInstitucionalJdbc(f.c,f.sessao(f.admin));
        var p=planos.preparar(new PlanoInstitucional.Selecao(f.matricula,List.of(periodo),List.of()));
        planos.registrar(new PlanoInstitucional.Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),"Plano sintético"));
        g(f.c,"UPDATE disciplina SET nome='Nome posterior' WHERE id_disciplina=?",f.disciplina);
        var r=gerar(f.alunoConta,new Filtro(Tipo.PLANO_PROPRIO,null,null,null,f.matricula));
        assertTrue(r.documento().parametros().stream().anyMatch(v->v.valor().equals("PENDENTE_ATUALIZACAO")));
        assertFalse(r.documento().tabelas().getFirst().linhas().getFirst().contains("Nome posterior"));
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM plano_institucional",x->x.getInt(1)).getFirst());
    }
    @Test void filtroNaoMisturaOutrosCursosESemPopulacaoContinuaExplicito() throws Exception {
        long curso=g(f.c,"INSERT INTO curso(nome,situacao) VALUES ('Curso sem população','ATIVO')");
        var p=repo(f.admin).preparar(new Filtro(Tipo.CENSO_DEMONSTRATIVO,curso,f.periodo,null,null));
        assertTrue(p.valida()); assertTrue(p.documento().tabelas().getFirst().linhas().isEmpty());
        assertTrue(p.documento().avisos().stream().anyMatch(a->a.contains("Nenhum registro")));
    }
    @Test void falhaNaAuditoriaDesfazRelatorioEArquivosSemAlterarDadosAcademicos() throws Exception {
        Connection proxy=f.falharAuditoria(); var repo=new RepositorioRelatorioAcademicoJdbc(proxy,f.sessao(proxy,f.admin)); var p=repo.preparar(censo());
        assertThrows(PersistenciaException.class,()->repo.gerar(pedido(p))); assertEquals(0,total());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",x->x.getString(1),f.inscricao).getFirst());
    }
    @Test void arquivoOuDadosDivergentesNaoSaoDisponibilizados() throws Exception {
        var r=gerar(f.alunoConta,historico()); var repo=repo(f.alunoConta);
        g(f.c,"UPDATE relatorio_academico SET pdf=? WHERE id_relatorio=?",new byte[]{1,2,3},r.id());
        assertThrows(IllegalArgumentException.class,()->repo.arquivo(r.id(),Formato.PDF));
        g(f.c,"UPDATE relatorio_academico SET dados_xml='alterado' WHERE id_relatorio=?",r.id());
        assertThrows(IllegalArgumentException.class,()->repo.consultar(r.id()));
    }
    @Test void camposInstitucionaisAusentesNaoGeramDocumentoConcluido() throws Exception {
        g(f.c,"UPDATE curso SET nome='' WHERE id_curso=?",f.curso);
        var p=repo(f.admin).preparar(censo()); assertFalse(p.valida());
        assertThrows(IllegalArgumentException.class,()->repo(f.admin).gerar(pedido(p))); assertEquals(0,total());
    }
    @Test void simboloNaoSuportadoEApontadoNaPreviaSemTrocarTextoOuArquivarParcial() throws Exception {
        g(f.c,"UPDATE aluno SET identificacao='漢字' WHERE id_aluno=?",f.aluno);
        var p=repo(f.admin).preparar(censo()); assertFalse(p.valida());
        assertThrows(IllegalArgumentException.class,()->repo(f.admin).gerar(pedido(p))); assertEquals(0,total());
        assertEquals("漢字",q(f.c,"SELECT identificacao FROM aluno WHERE id_aluno=?",x->x.getString(1),f.aluno).getFirst());
    }
}

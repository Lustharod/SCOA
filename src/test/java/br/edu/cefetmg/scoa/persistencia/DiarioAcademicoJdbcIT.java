package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class DiarioAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    private RepositorioDiarioAcademicoJdbc repo;
    private LocalDate dia;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste(); repo=new RepositorioDiarioAcademicoJdbc(f.c,f.sessao(f.docente)); dia=f.hoje.minusDays(20);
        g(f.c,"UPDATE disciplina SET carga_horaria=5 WHERE id_disciplina=?",f.disciplina);
        long sala=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Sala sintética','Bloco',20,'ATIVO')");
        for(int d=1;d<=7;d++) g(f.c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,?,'07:00','12:20')",f.turma,sala,d);
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private List<Aula> aulas() { return repo.registrarAulas(new NovaAula(f.turma,dia,TempoLetivo.doTurno(TempoLetivo.Turno.MANHA),"Pauta sintética","Conteúdo sintético")); }
    private Avaliacao prova(Prova tipo) { return repo.registrarAvaliacao(new NovaAvaliacao(f.turma,tipo,dia,"Avaliação sintética")); }
    private void nota(Avaliacao a,String valor) { repo.lancarNota(new LancarNota(a.id(),f.inscricao,new BigDecimal(valor),-1,"Lançamento sintético")); }
    private void preencher(List<Aula> aulas,int presentes) {
        for(int i=0;i<aulas.size();i++) repo.lancarFrequencia(new LancarFrequencia(aulas.get(i).id(),f.inscricao,
                i<presentes?TempoLetivo.Presenca.PRESENTE:TempoLetivo.Presenca.FALTA,-1,"Chamada sintética"));
    }
    @Test void sessaoEProfessorResponsavelSaoObrigatoriosSemSubstituicaoPorAdmin() {
        for(var sessao:Arrays.asList(null,f.sessao(f.admin),f.sessao(f.alunoConta),f.sessao(f.outroDocente))) {
            var negado=new RepositorioDiarioAcademicoJdbc(f.c,sessao);
            assertThrows(IllegalArgumentException.class,()->negado.aulas(f.turma));
            assertThrows(IllegalArgumentException.class,()->negado.registrarAvaliacao(new NovaAvaliacao(f.turma,Prova.P1,dia,"Descrição")));
        }
    }
    @Test void aulasAgrupamTemposSemContarIntervaloEExigemHorarioDaTurma() throws Exception {
        var lista=aulas(); assertEquals(6,lista.size());
        assertEquals(300,repo.consultarDocente(f.inscricao).minutosRegistrados());
        assertEquals(0,repo.consultarDocente(f.inscricao).minutosPresentes());
        assertThrows(IllegalArgumentException.class,()->repo.registrarAulas(new NovaAula(f.turma,dia,List.of(TempoLetivo.T1),"Pauta","Conteúdo")));
        assertEquals(6,repo.aulas(f.turma).size());
        assertEquals(6,q(f.c,"SELECT COUNT(*) FROM auditoria WHERE entidade='AULA' AND id_usuario=?",r->r.getInt(1),f.docente).getFirst());
    }
    @Test void segundoTempoDuplicadoDesfazTodoGrupoSemApagarAulaAnterior() {
        repo.registrarAulas(new NovaAula(f.turma,dia,List.of(TempoLetivo.M2),"Pauta","Conteúdo"));
        assertThrows(IllegalArgumentException.class,()->repo.registrarAulas(new NovaAula(f.turma,dia,List.of(TempoLetivo.M3,TempoLetivo.M2),"Pauta","Conteúdo")));
        assertEquals(List.of("M2"),repo.aulas(f.turma).stream().map(Aula::tempo).toList());
    }
    @Test void aulaFuturaForaDoPeriodoOuCargaFracionariaNaoERegistrada() throws Exception {
        assertThrows(IllegalArgumentException.class,()->repo.registrarAulas(new NovaAula(f.turma,f.hoje.plusDays(1),List.of(TempoLetivo.M1),"Pauta","Conteúdo")));
        assertThrows(IllegalArgumentException.class,()->repo.registrarAulas(new NovaAula(f.turma,f.hoje.minusDays(200),List.of(TempoLetivo.M1),"Pauta","Conteúdo")));
        g(f.c,"UPDATE disciplina SET carga_horaria=1 WHERE id_disciplina=?",f.disciplina);
        assertThrows(IllegalArgumentException.class,this::aulas); assertTrue(repo.aulas(f.turma).isEmpty());
    }
    @Test void provasSeparadasNaoSeDuplicamENotasNaoViraramZeroPorAusencia() {
        prova(Prova.P1); prova(Prova.P2); prova(Prova.PF);
        assertThrows(IllegalArgumentException.class,()->prova(Prova.P1));
        var b=repo.consultarDocente(f.inscricao);
        assertEquals(3,b.notas().size()); assertTrue(b.notas().stream().allMatch(n->n.valor()==null));
        assertNull(b.previa()); assertFalse(b.pendencias().isEmpty());
        assertThrows(IllegalArgumentException.class,()->repo.fechar(f.inscricao,"Conferência"));
    }
    @Test void edicaoDeNotaPreservaHistoricoERecusaVersaoAntiga() throws Exception {
        var p1=prova(Prova.P1); nota(p1,"8.0");
        repo.lancarNota(new LancarNota(p1.id(),f.inscricao,new BigDecimal("9.0"),0,"Correção sintética"));
        assertThrows(IllegalArgumentException.class,()->repo.lancarNota(new LancarNota(p1.id(),f.inscricao,BigDecimal.ZERO,0,"Tela antiga")));
        var n=repo.consultarDocente(f.inscricao).notas().getFirst(); assertEquals(1,n.versao()); assertEquals(new BigDecimal("9.00"),n.valor());
        var eventos=q(f.c,"SELECT descricao FROM evento_processo_academico WHERE tipo='DIARIO_NOTA' AND id_processo=? ORDER BY id_evento",r->r.getString(1),n.id());
        assertEquals(2,eventos.size()); assertTrue(eventos.getLast().contains("anterior=8.00"));
    }
    @Test void notasEFrequenciaDeOutraTurmaSaoRecusadas() throws Exception {
        var p=prova(Prova.P1); var a=aulas().getFirst();
        long t=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Outra',20,'ATIVO')",f.disciplina,f.professor,f.periodo);
        long i=g(f.c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",f.matricula,t,dia);
        assertThrows(IllegalArgumentException.class,()->repo.lancarNota(new LancarNota(p.id(),i,BigDecimal.TEN,-1,"Motivo")));
        assertThrows(IllegalArgumentException.class,()->repo.lancarFrequencia(new LancarFrequencia(a.id(),i,TempoLetivo.Presenca.PRESENTE,-1,"Motivo")));
    }
    @Test void alunoConsultaSomenteSeusLancamentosSemReceberAutoriaDocente() {
        nota(prova(Prova.P1),"8"); aulas();
        var aluno=new RepositorioDiarioAcademicoJdbc(f.c,f.sessao(f.alunoConta));
        assertEquals(new BigDecimal("8.00"),aluno.consultarAluno(f.inscricao).notas().getFirst().valor());
        var outro=new RepositorioDiarioAcademicoJdbc(f.c,f.sessao(f.outroConta));
        assertThrows(IllegalArgumentException.class,()->outro.consultarAluno(f.inscricao));
        assertThrows(IllegalArgumentException.class,()->aluno.consultarDocente(f.inscricao));
    }
    @Test void fecharExigeTodaCargaENotasEFrequenciasInformadas() {
        var aulas=aulas(); nota(prova(Prova.P1),"8"); nota(prova(Prova.P2),"8");
        preencher(aulas.subList(0,5),5);
        assertThrows(IllegalArgumentException.class,()->repo.fechar(f.inscricao,"Conferência"));
        preencher(aulas.subList(5,6),1);
        assertEquals("APROVADO",repo.fechar(f.inscricao,"Fechamento pelo diário").situacao());
        assertThrows(IllegalArgumentException.class,()->repo.lancarNota(new LancarNota(repo.avaliacoes(f.turma).getFirst().id(),f.inscricao,BigDecimal.ZERO,0,"Alterar depois")));
        assertThrows(IllegalArgumentException.class,()->repo.lancarFrequencia(new LancarFrequencia(aulas.getFirst().id(),f.inscricao,TempoLetivo.Presenca.FALTA,0,"Alterar depois")));
        assertThrows(IllegalArgumentException.class,()->repo.fechar(f.inscricao,"Repetir"));
    }
    @Test void sessentaHorasEncerramComSetentaEDoisTemposETresMilSeiscentosMinutos() throws Exception {
        g(f.c,"UPDATE disciplina SET carga_horaria=60 WHERE id_disciplina=?",f.disciplina);
        var aulas=new ArrayList<Aula>();
        for(int d=0;d<12;d++) aulas.addAll(repo.registrarAulas(new NovaAula(f.turma,dia.plusDays(d),TempoLetivo.doTurno(TempoLetivo.Turno.MANHA),"Pauta","Conteúdo")));
        preencher(aulas,72); nota(prova(Prova.P1),"7"); nota(prova(Prova.P2),"7");
        var b=repo.consultarDocente(f.inscricao); assertEquals(72,b.frequencias().size()); assertEquals(3600,b.minutosPrevistos()); assertTrue(b.pendencias().isEmpty());
        repo.fechar(f.inscricao,"Carga integral");
        assertEquals(3600,q(f.c,"SELECT total_minutos FROM resultado_inscricao WHERE id_inscricao=?",r->r.getInt(1),f.inscricao).getFirst());
        assertEquals(60,q(f.c,"SELECT carga_horaria FROM disciplina WHERE id_disciplina=?",r->r.getInt(1),f.disciplina).getFirst());
    }
    @ParameterizedTest @CsvSource({"8,REPROVADO","9,APROVADO"})
    void limiteDeSetentaECincoPorCentoPreservaRegraAprovada(int presentes,String esperado) throws Exception {
        g(f.c,"UPDATE disciplina SET carga_horaria=10 WHERE id_disciplina=?",f.disciplina);
        var lista=new ArrayList<>(aulas()); lista.addAll(repo.registrarAulas(new NovaAula(f.turma,dia.plusDays(1),TempoLetivo.doTurno(TempoLetivo.Turno.MANHA),"Pauta","Conteúdo")));
        preencher(lista,presentes); nota(prova(Prova.P1),"8"); nota(prova(Prova.P2),"8");
        assertEquals(esperado,repo.fechar(f.inscricao,"Conferência de frequência").situacao());
    }
    @Test void pfMantemCalculoAprovadoEPendenciaNaoEEncerradaPrematuramente() {
        var a=aulas(); preencher(a,6); nota(prova(Prova.P1),"6"); nota(prova(Prova.P2),"6");
        assertEquals("AGUARDA_PF",repo.consultarDocente(f.inscricao).previa().situacao());
        assertThrows(IllegalArgumentException.class,()->repo.fechar(f.inscricao,"Ainda falta PF"));
        nota(prova(Prova.PF),"4");
        assertEquals(new BigDecimal("5.0"),repo.fechar(f.inscricao,"Após PF").mediaFinal());
    }
    @Test void diarioImpedeFechamentoManualComTotaisIndependentes() {
        prova(Prova.P1);
        var protegido=RepositoriosAcademicosAutorizados.inscricoes(f.c,f.sessao(f.docente));
        var e=assertThrows(IllegalArgumentException.class,()->protegido.registrarResultado(f.inscricao,BigDecimal.TEN,BigDecimal.TEN,null,100,100,"Totais independentes"));
        assertTrue(e.getMessage().contains("menu 24"));
    }
    @Test void retirarPfInaplicavelPreservaIdValorAnteriorEHistoricoAntesDeFechar() throws Exception {
        preencher(aulas(),6); nota(prova(Prova.P1),"8"); nota(prova(Prova.P2),"8");
        var pf=prova(Prova.PF); nota(pf,"6");
        assertThrows(IllegalArgumentException.class,()->repo.fechar(f.inscricao,"PF indevida"));
        long id=repo.consultarDocente(f.inscricao).notas().stream().filter(n->n.avaliacao().id()==pf.id()).findFirst().orElseThrow().id();
        repo.retirarNota(new RetirarNota(pf.id(),f.inscricao,0,"PF lançada por engano"));
        assertThrows(IllegalArgumentException.class,()->repo.retirarNota(new RetirarNota(pf.id(),f.inscricao,0,"Tela antiga")));
        assertEquals(id,q(f.c,"SELECT id_nota FROM nota WHERE id_avaliacao=? AND id_inscricao=?",r->r.getLong(1),pf.id(),f.inscricao).getFirst());
        assertEquals(new BigDecimal("6.00"),q(f.c,"SELECT valor FROM nota WHERE id_nota=?",r->r.getBigDecimal(1),id).getFirst());
        assertTrue(q(f.c,"SELECT descricao FROM evento_processo_academico WHERE tipo='DIARIO_NOTA' AND id_processo=? AND acao='RETIRAR'",r->r.getString(1),id).getFirst().contains("anterior=6.00"));
        assertEquals("APROVADO",repo.fechar(f.inscricao,"Sem PF aplicável").situacao());
        assertThrows(IllegalArgumentException.class,()->repo.retirarNota(new RetirarNota(pf.id(),f.inscricao,1,"Após fechamento")));
    }
    @Test void segundaFaseDesconsideraTemposAnterioresSemCriarFaltasOuReduzirCargaDaDisciplina() throws Exception {
        g(f.c,"UPDATE disciplina SET carga_horaria=10 WHERE id_disciplina=?",f.disciplina);
        aulas();
        var posteriores=repo.registrarAulas(new NovaAula(f.turma,dia.plusDays(1),TempoLetivo.doTurno(TempoLetivo.Turno.MANHA),"Pauta","Conteúdo"));
        g(f.c,"UPDATE inscricao_disciplina SET data_inscricao=? WHERE id_inscricao=?",dia.plusDays(1),f.inscricao);
        preencher(posteriores,5); nota(prova(Prova.P1),"8"); nota(prova(Prova.P2),"8");
        var b=repo.consultarDocente(f.inscricao);
        assertEquals(600,b.minutosPrevistos()); assertEquals(600,b.minutosRegistrados());
        assertEquals(300,b.minutosConsiderados()); assertEquals(250,b.minutosPresentes());
        assertEquals(6,b.frequencias().stream().filter(t->!t.considerada()).count());
        assertTrue(b.frequencias().stream().filter(t->!t.considerada()).allMatch(t->t.situacao()==null));
        assertTrue(b.pendencias().isEmpty()); assertEquals("APROVADO",repo.fechar(f.inscricao,"Segunda fase").situacao());
        assertEquals(300,q(f.c,"SELECT total_minutos FROM resultado_inscricao WHERE id_inscricao=?",r->r.getInt(1),f.inscricao).getFirst());
    }
    @Test void chegadaSimuladaDeduplicaRespeitaQuinzeMinutosENaoSobrescreveCorrecaoManual() {
        var a=aulas();
        var e1=new Chegada("Fixture","1",a.get(0).id(),f.inscricao,dia.atTime(7,15));
        var e2=new Chegada("Fixture","2",a.get(1).id(),f.inscricao,dia.atTime(8,6));
        var lote=repo.importarSimulados(List.of(e1,e1,e2));
        assertEquals(List.of("PROCESSADO","REPETIDO","PROCESSADO"),lote.stream().map(Processamento::situacao).toList());
        var b=repo.consultarDocente(f.inscricao);
        assertEquals("PRESENTE",b.frequencias().get(0).situacao()); assertEquals("FALTA",b.frequencias().get(1).situacao()); assertNull(b.frequencias().get(2).situacao());
        repo.lancarFrequencia(new LancarFrequencia(e1.aula(),f.inscricao,TempoLetivo.Presenca.FALTA,0,"Correção manual"));
        assertEquals("REPETIDO",repo.importarSimulados(List.of(e1)).getFirst().situacao());
        assertEquals("FALTA",repo.consultarDocente(f.inscricao).frequencias().getFirst().situacao());
        assertEquals("RECUSADO",repo.importarSimulados(List.of(new Chegada("Fixture","1",e2.aula(),f.inscricao,e2.instante()))).getFirst().situacao());
    }
    @Test void loteContinuaAposRecusaSemRegistrarEventoInvalidoOuReescreverManual() {
        var a=aulas();
        preencher(a.subList(0,1),1);
        var manual=new Chegada("Fixture","manual",a.get(0).id(),f.inscricao,dia.atTime(7,0));
        var dataErrada=new Chegada("Fixture","errado",a.get(1).id(),f.inscricao,dia.plusDays(1).atTime(7,50));
        var valido=new Chegada("Fixture","ok",a.get(2).id(),f.inscricao,dia.atTime(8,40));
        assertEquals(List.of("RECUSADO","RECUSADO","PROCESSADO"),repo.importarSimulados(List.of(manual,dataErrada,valido)).stream().map(Processamento::situacao).toList());
    }
    @Test void dadosLegadosSaoPreservadosSemInventarTempoTipoOuAutoria() throws Exception {
        long aula=g(f.c,"INSERT INTO aula(id_turma,data,pauta,conteudo) VALUES (?,?,'Pauta original','Conteúdo original')",f.turma,dia);
        long aval=g(f.c,"INSERT INTO avaliacao(id_turma,identificacao,data,descricao) VALUES (?,'Prova antiga',?,'Original')",f.turma,dia);
        g(f.c,"INSERT INTO nota(id_avaliacao,id_inscricao,valor) VALUES (?,?,8.5)",aval,f.inscricao);
        assertNull(repo.aulas(f.turma).getFirst().tempo());
        assertThrows(IllegalArgumentException.class,()->repo.lancarFrequencia(new LancarFrequencia(aula,f.inscricao,TempoLetivo.Presenca.PRESENTE,-1,"Migrar por suposição")));
        assertThrows(IllegalArgumentException.class,()->repo.lancarNota(new LancarNota(aval,f.inscricao,BigDecimal.TEN,0,"Migrar por suposição")));
        assertTrue(repo.consultarDocente(f.inscricao).pendencias().stream().anyMatch(p->p.contains("legada")));
    }
    @Test void revogacaoDaAutoriaOuContaInativaRecusamNovoLancamento() throws Exception {
        var a=prova(Prova.P1);
        g(f.c,"UPDATE professor SET situacao='INATIVO' WHERE id_professor=?",f.professor);
        assertThrows(IllegalArgumentException.class,()->nota(a,"8"));
        g(f.c,"UPDATE professor SET situacao='ATIVO' WHERE id_professor=?",f.professor);
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.docente);
        assertThrows(IllegalArgumentException.class,()->nota(a,"8"));
    }
    @Test void falhaDeAuditoriaDesfazNotaHistoricoEFrequenciaSimulada() throws Exception {
        var a=aulas(); var p=prova(Prova.P1);
        var proxy=f.falharAuditoria(); var negado=new RepositorioDiarioAcademicoJdbc(proxy,f.sessao(proxy,f.docente));
        assertThrows(PersistenciaException.class,()->negado.lancarNota(new LancarNota(p.id(),f.inscricao,BigDecimal.TEN,-1,"Fixture")));
        assertNull(repo.consultarDocente(f.inscricao).notas().getFirst().valor());
        assertThrows(PersistenciaException.class,()->negado.importarSimulados(List.of(new Chegada("Fixture","rollback",a.getFirst().id(),f.inscricao,dia.atTime(7,0)))));
        assertTrue(q(f.c,"SELECT chave FROM evento_acesso_simulado WHERE chave='rollback'",r->r.getString(1)).isEmpty());
        assertNull(repo.consultarDocente(f.inscricao).frequencias().getFirst().situacao());
        assertTrue(q(f.c,"SELECT id_evento FROM evento_processo_academico WHERE tipo='DIARIO_NOTA'",r->r.getLong(1)).isEmpty());
        assertFalse(f.c.getAutoCommit());
    }
}

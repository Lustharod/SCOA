package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.HorarioAcademico.*;
import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class HorarioAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    private long sala;
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste();
        g(f.c,"DELETE FROM inscricao_disciplina WHERE id_inscricao=?",f.inscricao);
        g(f.c,"UPDATE periodo_letivo SET data_inicio=?,data_fim=? WHERE id_periodo=?",f.hoje.minusDays(7),f.hoje.plusDays(90),f.periodo);
        g(f.c,"INSERT INTO usuario_biblioteca(id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",f.alunoConta);
        sala=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Sala sintética','Bloco',30,'ATIVO')");
    }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioHorarioAcademicoJdbc repo() { return new RepositorioHorarioAcademicoJdbc(f.c,f.sessao(f.admin)); }
    private Planejar plano(TempoLetivo... tempos) { return new Planejar(f.turma,f.hoje.getDayOfWeek().getValue(),List.of(tempos),"Definição sintética de tempos"); }
    private Registro horario() { return repo().planejar(plano(TempoLetivo.M1)).getFirst(); }
    private Registro atual(long id) { return repo().listar(f.turma).stream().filter(h->h.horario().id()==id).findFirst().orElseThrow(); }
    @Test void planejaTemposSemSalaSemReservarIntervaloComAutoriaEHistorico() throws Exception {
        var lista=repo().planejar(plano(TempoLetivo.M1,TempoLetivo.M2,TempoLetivo.M3,TempoLetivo.M4));
        assertEquals(4,lista.size()); assertTrue(lista.stream().allMatch(r->r.horario().idSala()==null && r.ativo() && r.versao()==0 && r.autor()==f.admin));
        assertEquals(TempoLetivo.M4.inicio(),lista.getLast().horario().faixa().inicio());
        for(var r:lista) assertEquals(1,repo().historico(r.horario().id()).size());
        assertEquals(4,new RepositorioAlocacaoJdbc(f.c).listarHorarios(f.turma).size());
    }
    @Test void grupoComConflitoDesfazTemposNovosPreservandoHorarioAnterior() throws Exception {
        var antigo=repo().planejar(plano(TempoLetivo.M2)).getFirst();
        assertThrows(IllegalArgumentException.class,()->repo().planejar(plano(TempoLetivo.M1,TempoLetivo.M2)));
        assertEquals(List.of(antigo),repo().listar(f.turma)); assertEquals(1,repo().historico(antigo.horario().id()).size());
    }
    @Test void salaEEscolhidaDepoisSemAlterarIdsDiasOuTempos() {
        var h=horario(); assertTrue(repo().salasDisponiveis(h.horario().id()).stream().anyMatch(s->s.getId()==sala));
        var alocado=repo().atribuirSala(h.horario().id(),0,sala,"Sala conferida");
        assertEquals(h.horario().id(),alocado.horario().id()); assertEquals(h.horario().faixa(),alocado.horario().faixa());
        assertEquals(sala,alocado.horario().idSala()); assertEquals(1,alocado.versao());
        assertEquals(2,repo().historico(h.horario().id()).size());
        assertEquals(alocado,repo().atribuirSala(h.horario().id(),1,sala,"Conferência repetida sem mudança"));
        assertEquals(2,repo().historico(h.horario().id()).size());
    }
    @Test void retirarSalaMantemHorarioEAuditoriaSemApagarId() throws Exception {
        var h=horario(); var alocado=repo().atribuirSala(h.horario().id(),0,sala,"Alocação");
        var semSala=repo().retirarSala(h.horario().id(),alocado.versao(),"Sala ficará para nova definição");
        assertNull(semSala.horario().idSala()); assertTrue(semSala.ativo()); assertEquals(h.horario().faixa(),semSala.horario().faixa());
        assertEquals(3,repo().historico(h.horario().id()).size());
        assertEquals(sala,repo().salasDisponiveis(h.horario().id()).getFirst().getId());
    }
    @Test void cancelarPreservaRegistroRetiraSalaEDesconsideraHorarioNasConsultasAtivas() throws Exception {
        var h=horario(); var alocado=repo().atribuirSala(h.horario().id(),0,sala,"Alocação");
        var cancelado=repo().cancelar(h.horario().id(),alocado.versao(),"Oferta de horário corrigida");
        assertFalse(cancelado.ativo()); assertNull(cancelado.horario().idSala()); assertEquals(1,repo().listar(f.turma).size());
        assertTrue(new RepositorioAlocacaoJdbc(f.c).listarHorarios(f.turma).isEmpty());
        assertThrows(IllegalArgumentException.class,()->repo().atribuirSala(h.horario().id(),2,sala,"Reabrir"));
        var novo=horario(); assertNotEquals(h.horario().id(),novo.horario().id());
    }
    @Test void capacidadeSituacaoESobreposicaoDaSalaSaoRevalidadasAoGravar() throws Exception {
        var h=horario(); long pequena=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Pequena','Bloco',1,'ATIVO')");
        assertThrows(IllegalArgumentException.class,()->repo().atribuirSala(h.horario().id(),0,pequena,"Conferência"));
        long outra=g(f.c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,'Outra',20,'ATIVO')",f.disciplina,f.professor,f.periodo);
        var outro=repo().planejar(new Planejar(outra,plano(TempoLetivo.M1).dia(),List.of(TempoLetivo.M1),"Fixture de outra turma")).getFirst();
        repo().atribuirSala(outro.horario().id(),0,sala,"Outra turma alocada");
        assertFalse(repo().salasDisponiveis(h.horario().id()).stream().anyMatch(s->s.getId()==sala));
        assertThrows(IllegalArgumentException.class,()->repo().atribuirSala(h.horario().id(),0,sala,"Prévias não reservam sala"));
        assertNull(atual(h.horario().id()).horario().idSala());
        g(f.c,"UPDATE sala SET situacao='INATIVO' WHERE id_sala=?",pequena);
        assertThrows(IllegalArgumentException.class,()->repo().atribuirSala(h.horario().id(),0,pequena,"Sala inativa"));
    }
    @Test void versaoAntigaNaoSobrescreveSalaNemHistorico() {
        var h=horario(); var alocado=repo().atribuirSala(h.horario().id(),0,sala,"Sala conferida");
        assertThrows(IllegalArgumentException.class,()->repo().retirarSala(h.horario().id(),0,"Tela antiga"));
        assertEquals(alocado,atual(h.horario().id())); assertEquals(2,repo().historico(h.horario().id()).size());
    }
    @Test void alunoSeInscreveComHorarioDefinidoESalaPendenteEConsultaNaoOcultaTempos() throws Exception {
        var h=horario();
        var inscricao=new RepositorioInscricaoJdbc(f.c).inscreverComoAluno(f.sessao(f.alunoConta),f.matricula,f.turma);
        assertNotNull(inscricao.id());
        var lista=new RepositorioConsultasAlunoJdbc(f.c,f.sessao(f.alunoConta)).horarios();
        assertEquals(1,lista.size()); assertEquals(h.horario().faixa(),lista.getFirst().faixa()); assertNull(lista.getFirst().sala());
    }
    @Test void salaPodeMudarAposInscricaoEAulaSemMudarTemposOuLancamentos() throws Exception {
        var h=horario(); long insc=f.inscrever(f.matricula,"ATIVO");
        var diario=new RepositorioDiarioAcademicoJdbc(f.c,f.sessao(f.docente));
        var aula=diario.registrarAulas(new DiarioAcademico.NovaAula(f.turma,f.hoje,List.of(TempoLetivo.M1),"Pauta","Conteúdo")).getFirst();
        assertThrows(IllegalArgumentException.class,()->repo().cancelar(h.horario().id(),0,"Cancelar tempo"));
        assertThrows(IllegalArgumentException.class,()->repo().planejar(plano(TempoLetivo.M2)));
        var comSala=repo().atribuirSala(h.horario().id(),0,sala,"Escolha posterior da sala");
        var semSala=repo().retirarSala(h.horario().id(),comSala.versao(),"Sala em manutenção");
        assertTrue(semSala.ativo()); assertEquals(h.horario().faixa(),semSala.horario().faixa());
        assertEquals(aula,diario.aulas(f.turma).getFirst());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),insc).getFirst());
    }
    @Test void horarioSemSalaImpedeMudancaIndependenteDeVinculosOuSituacaoDaTurma() {
        horario(); var repo=new RepositorioCadastroAcademicoJdbc(f.c);
        assertThrows(IllegalArgumentException.class,()->repo.alterarTurma(f.turma,t->new Turma(t.id(),t.idDisciplina(),t.idProfessor(),t.idPeriodo(),t.codigo(),t.capacidade(),"INATIVO")));
    }
    @Test void consultaEMudancaNaoRecebemPermissaoPeloVinculoAcademico() {
        var repo=new RepositorioHorarioAcademicoJdbc(f.c,f.sessao(f.docente));
        assertThrows(IllegalArgumentException.class,()->repo.listar(f.turma)); assertThrows(IllegalArgumentException.class,()->repo.planejar(plano(TempoLetivo.M1)));
    }
    @Test void revogacaoTecnicaOuContaInativaBloqueiaSessaoExistente() throws Exception {
        f.conceder(f.outroConta,AcessoAcademicoJdbc.CONSULTAR); f.conceder(f.outroConta,AcessoAcademicoJdbc.MANTER);
        var repo=new RepositorioHorarioAcademicoJdbc(f.c,f.sessao(f.outroConta)); var h=repo.planejar(plano(TempoLetivo.M1)).getFirst();
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.outroConta);
        assertThrows(IllegalArgumentException.class,()->repo.retirarSala(h.horario().id(),0,"Revogado")); assertThrows(IllegalArgumentException.class,()->repo.listar(f.turma));
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.admin);
        assertThrows(IllegalArgumentException.class,()->repo().listar(f.turma));
    }
    @Test void falhaNaAuditoriaDesfazGrupoInteiro() {
        Connection proxy=f.falharAuditoria(); var repo=new RepositorioHorarioAcademicoJdbc(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->repo.planejar(plano(TempoLetivo.M1,TempoLetivo.M2))); assertTrue(repo().listar(f.turma).isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"atribuir","retirar","cancelar"})
    void falhaNaAuditoriaDesfazSalaCancelamentoVersaoEHistorico(String operacao) {
        var h=horario();
        if(!"atribuir".equals(operacao)) h=repo().atribuirSala(h.horario().id(),0,sala,"Alocação anterior");
        final var antes=h; int eventos=repo().historico(h.horario().id()).size();
        var proxy=f.falharAuditoria(); var repo=new RepositorioHorarioAcademicoJdbc(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->{
            switch(operacao) {
                case "atribuir" -> repo.atribuirSala(antes.horario().id(),antes.versao(),sala,"Conferência");
                case "retirar" -> repo.retirarSala(antes.horario().id(),antes.versao(),"Conferência");
                default -> repo.cancelar(antes.horario().id(),antes.versao(),"Conferência");
            }
        });
        assertEquals(antes,atual(h.horario().id())); assertEquals(eventos,repo().historico(h.horario().id()).size());
    }
    @Test void caminhoLegadoAutenticadoPreservaHistoricoECancelamentoNaoExcluiId() throws Exception {
        var repo=RepositoriosAcademicosAutorizados.alocacoes(f.c,f.sessao(f.admin));
        var h=repo.alocar(new HorarioAula(null,f.turma,sala,new FaixaHorario(plano(TempoLetivo.M1).dia(),TempoLetivo.M1.inicio(),TempoLetivo.M1.fim())));
        assertEquals(1,repo().historico(h.id()).size()); repo.desalocar(h.id());
        assertTrue(repo.listarHorarios(f.turma).isEmpty()); assertFalse(atual(h.id()).ativo());
        assertEquals(2,repo().historico(h.id()).size());
    }
    @Test void dadosLegadosMantemIdSalaEAutoriaDesconhecidaSemHistoricoInventado() throws Exception {
        var h=new RepositorioAlocacaoJdbc(f.c).alocar(new HorarioAula(null,f.turma,sala,new FaixaHorario(plano(TempoLetivo.M1).dia(),TempoLetivo.M1.inicio(),TempoLetivo.M1.fim())));
        var registro=atual(h.id()); assertEquals(h,registro.horario()); assertNull(registro.autor()); assertNull(registro.atualizadoEm());
        assertTrue(repo().historico(h.id()).isEmpty());
    }
}

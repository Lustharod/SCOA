package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.ServicoSala;
import br.edu.cefetmg.scoa.dominio.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class AcessoAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    @BeforeEach void preparar() throws Exception { f=new CenarioAcademicoTeste(); }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }

    @Test void todosOsContratosLegadosRecusamConsultaSemSessaoAntesDeLerDados() {
        List<Runnable> operacoes=List.of(
            ()->RepositoriosAcademicosAutorizados.salas(f.c,null).listar(),
            ()->RepositoriosAcademicosAutorizados.alocacoes(f.c,null).listarTurmas(),
            ()->RepositoriosAcademicosAutorizados.cadastros(f.c,null).listarPeriodos(),
            ()->RepositoriosAcademicosAutorizados.professoresDisciplinas(f.c,null).listarProfessores(),
            ()->RepositoriosAcademicosAutorizados.baseAcademica(f.c,null).listarAlunos(),
            ()->RepositoriosAcademicosAutorizados.matriculas(f.c,null).listar(f.aluno),
            ()->RepositoriosAcademicosAutorizados.inscricoes(f.c,null).listar(f.matricula),
            ()->RepositoriosAcademicosAutorizados.equivalencias(f.c,null).historico(f.matricula),
            ()->RepositoriosAcademicosAutorizados.planejamento(f.c,null).periodos());
        operacoes.forEach(o->assertThrows(IllegalArgumentException.class,o::run));
    }
    @Test void vinculosAcademicosNaoConcedemPermissaoDeCadastro() throws Exception {
        var sessao=f.sessao(f.alunoConta);
        g(f.c,"INSERT INTO coordenacao_curso VALUES (?,?)",f.curso,f.alunoConta);
        var repo=RepositoriosAcademicosAutorizados.baseAcademica(f.c,sessao);
        assertThrows(IllegalArgumentException.class,repo::listarAlunos);
        assertThrows(IllegalArgumentException.class,()->repo.inserirAluno(new Aluno(null,null,"Indevido","ATIVO")));
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM aluno WHERE identificacao='Indevido'",r->r.getInt(1)).getFirst());
    }
    @Test void permissoesDeConsultaEManutencaoSaoIndependentesERevalidadas() throws Exception {
        f.conceder(f.outroConta,AcessoAcademicoJdbc.CONSULTAR);
        var repo=RepositoriosAcademicosAutorizados.baseAcademica(f.c,f.sessao(f.outroConta));
        assertFalse(repo.listarAlunos().isEmpty());
        assertThrows(IllegalArgumentException.class,()->repo.inserirAluno(new Aluno(null,null,"Novo","ATIVO")));
        f.conceder(f.outroConta,AcessoAcademicoJdbc.MANTER);
        var novo=repo.inserirAluno(new Aluno(null,null,"Novo","ATIVO"));
        assertNotNull(novo.id());
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.outroConta);
        assertThrows(IllegalArgumentException.class,repo::listarAlunos);
    }
    @Test void administradorMantemCadastroComAutoriaESemGanharAutoriaDocente() throws Exception {
        var sessao=f.sessao(f.admin);
        var sala=new ServicoSala(RepositoriosAcademicosAutorizados.salas(f.c,sessao)).cadastrar("Teste autorizado","Bloco",30);
        assertEquals(f.admin,q(f.c,"SELECT id_usuario FROM auditoria WHERE entidade='RepositorioSala' AND id_registro=?",r->r.getLong(1),sala.getId()).getFirst());
        assertThrows(IllegalArgumentException.class,()->fechar(sessao));
    }
    @Test void inscricoesDoAlunoSaoRestritasASuaMatricula() {
        var repo=RepositoriosAcademicosAutorizados.inscricoes(f.c,f.sessao(f.alunoConta));
        assertEquals(1,repo.listar(f.matricula).size());
        assertThrows(IllegalArgumentException.class,()->repo.listar(f.outraMatricula));
        assertThrows(IllegalArgumentException.class,()->repo.avaliar(f.outraMatricula,f.turma));
        assertThrows(IllegalArgumentException.class,()->repo.vincularEstrutura(f.matricula,f.estrutura));
        assertThrows(IllegalArgumentException.class,()->repo.inscrever(f.matricula,f.turma));
    }
    @Test void primeiroFechamentoExigeDocenteDaTurmaEPreservaResultadoUnico() throws Exception {
        assertThrows(IllegalArgumentException.class,()->fechar(f.sessao(f.outroDocente)));
        assertThrows(IllegalArgumentException.class,()->fechar(f.sessao(f.alunoConta)));
        var sessao=f.sessao(f.docente);
        assertEquals("APROVADO",fechar(sessao).situacao());
        assertEquals("APROVADO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
        assertEquals(f.docente,q(f.c,"SELECT id_usuario FROM auditoria WHERE entidade='RESULTADO_INSCRICAO' AND id_registro=?",r->r.getLong(1),f.inscricao).getFirst());
        assertThrows(IllegalArgumentException.class,()->fechar(sessao));
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM resultado_inscricao WHERE id_inscricao=?",r->r.getInt(1),f.inscricao).getFirst());
    }
    @Test void trocaDoDocenteOuInativacaoDaContaTemEfeitoNaSessaoExistente() throws Exception {
        var sessao=f.sessao(f.docente);
        g(f.c,"UPDATE professor SET id_usuario=NULL WHERE id_professor=?",f.professor);
        assertThrows(IllegalArgumentException.class,()->fechar(sessao));
        g(f.c,"UPDATE professor SET id_usuario=? WHERE id_professor=?",f.docente,f.professor);
        g(f.c,"UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?",f.docente);
        assertThrows(IllegalArgumentException.class,()->fechar(sessao));
    }
    @Test void sessaoEncerradaOuDeOutraConexaoNaoAutoriza() throws Exception {
        var sessao=f.sessao(f.admin);
        var proxy=f.falharAuditoria();
        assertThrows(IllegalArgumentException.class,()->RepositoriosAcademicosAutorizados.salas(proxy,sessao).listar());
        sessao.encerrar();
        assertThrows(IllegalArgumentException.class,()->RepositoriosAcademicosAutorizados.salas(f.c,sessao).listar());
    }
    @Test void falhaNaAuditoriaDesfazCadastroEPreservaTransacaoDoChamador() throws Exception {
        long anterior=g(f.c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Anterior','B',10,'ATIVO')");
        var proxy=f.falharAuditoria();
        var repo=RepositoriosAcademicosAutorizados.salas(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->repo.salvar(Sala.nova("Desfazer","B",10)));
        assertTrue(q(f.c,"SELECT id_sala FROM sala WHERE identificacao='Desfazer'",r->r.getLong(1)).isEmpty());
        assertFalse(q(f.c,"SELECT id_sala FROM sala WHERE id_sala=?",r->r.getLong(1),anterior).isEmpty());
        assertFalse(f.c.getAutoCommit());
    }
    @Test void falhaNaAuditoriaDesfazPrimeiroResultadoEEstadoDaInscricao() throws Exception {
        var proxy=f.falharAuditoria();
        var repo=RepositoriosAcademicosAutorizados.inscricoes(proxy,f.sessao(proxy,f.docente));
        assertThrows(PersistenciaException.class,()->repo.registrarResultado(f.inscricao,new BigDecimal("8"),new BigDecimal("8"),null,120,110,"Apuração sintética"));
        assertTrue(q(f.c,"SELECT id_inscricao FROM resultado_inscricao WHERE id_inscricao=?",r->r.getLong(1),f.inscricao).isEmpty());
        assertEquals("ATIVO",q(f.c,"SELECT situacao FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getString(1),f.inscricao).getFirst());
    }
    private RegraResultado.Resultado fechar(SegurancaJdbc.Sessao sessao) {
        return RepositoriosAcademicosAutorizados.inscricoes(f.c,sessao).registrarResultado(f.inscricao,
                new BigDecimal("8"),new BigDecimal("8"),null,120,110,"Apuração sintética");
    }
}

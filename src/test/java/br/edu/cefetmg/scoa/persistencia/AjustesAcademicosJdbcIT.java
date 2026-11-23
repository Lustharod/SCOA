package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.RetificacaoResultado;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import static org.junit.jupiter.api.Assertions.*;

class AjustesAcademicosJdbcIT {
    private static final String SENHA=UUID.randomUUID().toString();
    private static final RetificacaoResultado APROVACAO=new RetificacaoResultado(BigDecimal.TEN,BigDecimal.TEN,null,120,110);
    private static final RetificacaoResultado REPROVACAO=new RetificacaoResultado(BigDecimal.TEN,BigDecimal.TEN,null,120,70);
    private static final String MOTIVO="Apuração sintética v0.12";
    private Connection c;
    private String tag;
    private String hash;
    private LocalDate hoje;
    private long curso,ingresso,matricula,matriculaB,disciplina,professor,sala,biblioteca;
    private Conta aluno,coordenador,docente,outraConta;
    private RepositorioInscricaoJdbc inscricoes;
    private record Conta(long id,SegurancaJdbc.Sessao sessao) { }

    @BeforeEach
    void preparar() throws SQLException {
        c=ApoioJdbc.abrir(false);
        c.setAutoCommit(false);
        tag="IT12-"+UUID.randomUUID().toString().substring(0,16);
        hash=SegurancaJdbc.codificar(SENHA.toCharArray());
        hoje=dataAcademica(agoraUtc(c));
        aluno=conta("aluno");
        coordenador=conta("coord");
        docente=conta("docente");
        outraConta=conta("outra");
        curso=g(c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
        g(c,"INSERT INTO coordenacao_curso(id_curso,id_usuario) VALUES (?,?)",curso,coordenador.id());
        ingresso=periodo(-300,-200);
        matricula=matricular(aluno.id());
        matriculaB=matricular(outraConta.id());
        disciplina=disciplina("A");
        long estrutura=g(c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,tag);
        g(c,"INSERT INTO estrutura_disciplina(id_estrutura,id_disciplina) VALUES (?,?)",estrutura,disciplina);
        g(c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?),(?,?)",
                matricula,estrutura,matriculaB,estrutura);
        professor=g(c,"INSERT INTO professor(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",docente.id(),tag);
        sala=g(c,"INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES (?,'fixture',10,'ATIVO')",tag);
        biblioteca=g(c,"INSERT INTO usuario_biblioteca(id_usuario,tipo,situacao) VALUES (?,'ALUNO','ATIVO')",aluno.id());
        inscricoes=new RepositorioInscricaoJdbc(c);
    }

    @AfterEach
    void descartar() throws SQLException {
        if(c!=null) try(var conexao=c) { conexao.rollback(); }
    }

    @Test
    void segundaFaseReutilizaVagaMasNaoReinscreveAlunoNaDisciplina() throws SQLException {
        long periodo=periodo(-7,80);
        long turma=turma(periodo,disciplina,"A");
        long alternativa=turma(periodo,disciplina,"B");
        long id=inscricoes.inscrever(matricula,turma).id();
        var cancelamento=cancelamentos(aluno);
        assertThrows(IllegalArgumentException.class,()->cancelamentos(outraConta).solicitar(id,MOTIVO));
        var pedido=cancelamento.solicitar(id,MOTIVO);
        assertEquals("AUTOMATICO",pedido.situacao());
        assertEquals(aluno.id(),pedido.solicitante());
        assertFalse(vagaRetida(id));
        assertEquals(pedido, cancelamento.historico(id).getFirst());
        assertThrows(IllegalArgumentException.class,()->inscricoes.inscrever(matricula,alternativa));
        assertEquals("ATIVO",inscricoes.inscrever(matriculaB,turma).situacao());
        assertFalse(c.getAutoCommit());
    }

    @Test
    void decisaoTardiaExigeCoordenacaoPreservaHistoricoERetemVaga() throws SQLException {
        long turma=turma(periodo(-14,80),disciplina,"T");
        long id=inscricaoDireta(matricula,turma);
        var pedido=cancelamentos(aluno).solicitar(id,MOTIVO);
        assertEquals("PENDENTE",pedido.situacao());
        assertThrows(IllegalArgumentException.class,()->cancelamentos(aluno).solicitar(id,MOTIVO));
        assertThrows(IllegalArgumentException.class,()->cancelamentos(docente).decidir(pedido.id(),true,MOTIVO));
        assertEquals("ATIVO",inscricoes.listar(matricula).getFirst().situacao());
        assertEquals("INDEFERIDO",cancelamentos(coordenador).decidir(pedido.id(),false,MOTIVO).situacao());
        assertEquals("ATIVO",inscricoes.listar(matricula).getFirst().situacao());
        var novo=cancelamentos(aluno).solicitar(id,MOTIVO);
        var deferimento=cancelamentos(coordenador).decidir(novo.id(),true,MOTIVO);
        assertEquals("DEFERIDO",deferimento.situacao());
        assertEquals(coordenador.id(),deferimento.coordenador());
        assertTrue(vagaRetida(id));
        assertEquals(0,inscricoes.avaliar(matriculaB,turma).vagas());
        assertEquals(2,cancelamentos(aluno).historico(id).size());
    }

    @Test
    void coordenacaoAprovaRetificacaoEResultadoHabilitaPrerequisito() throws SQLException {
        long origem=periodo(-120,-1);
        long alvo=periodo(30,140);
        long id=inscricaoDireta(matricula,turma(origem,disciplina,"Origem"));
        inscricoes.registrarResultado(id,REPROVACAO.p1(),REPROVACAO.p2(),null,120,70,MOTIVO);
        var coord=retificacoes(coordenador);
        // O prazo deve vir do calendário, inclusive sem mapeamento manual de sucessor.
        assertThrows(IllegalArgumentException.class,()->retificacoes(aluno).solicitar(id,APROVACAO,MOTIVO));
        long pedido=retificacoes(docente).solicitar(id,APROVACAO,MOTIVO);
        assertThrows(IllegalArgumentException.class,()->retificacoes(docente).decidir(pedido,true,MOTIVO));
        assertEquals("REPROVADO",inscricoes.listar(matricula).getFirst().situacao());
        coord.decidir(pedido,true,MOTIVO);
        var historico=coord.historico(id).getFirst();
        assertEquals("DEFERIDA",historico.situacao());
        assertTrue(historico.anterior().contains("REPROVADO"));
        assertEquals(110,historico.proposta().presentes());
        assertEquals("APROVADO",inscricoes.listar(matricula).getFirst().situacao());
        long seguinte=disciplina("C");
        g(c,"INSERT INTO estrutura_disciplina SELECT id_estrutura,? FROM vinculo_curricular_matricula WHERE id_matricula=?",seguinte,matricula);
        g(c,"INSERT INTO disciplina_prerequisito VALUES (?,?)",seguinte,disciplina);
        var avaliacao=inscricoes.avaliar(matricula,turma(alvo,seguinte,"Dependente"));
        assertTrue(avaliacao.motivos().stream().noneMatch(m->m.contains("Pré-requisito")));
    }

    @Test
    void retificacaoQueDerrubaPrerequisitoProtegeInscricaoPosterior() throws SQLException {
        long origem=periodo(-120,-1);
        long id=inscricaoDireta(matricula,turma(origem,disciplina,"Origem"));
        inscricoes.registrarResultado(id,APROVACAO.p1(),APROVACAO.p2(),null,120,110,MOTIVO);
        var coord=retificacoes(coordenador);
        long sucessor=periodo(40,140);
        coord.definirSucessor(curso,origem,sucessor);
        long pedido=retificacoes(docente).solicitar(id,REPROVACAO,MOTIVO);
        long posterior=inscricaoDireta(matricula,turma(sucessor,disciplina("D"),"Posterior"));
        g(c,"INSERT INTO evidencia_requisito_inscricao(id_inscricao,id_requisito,tipo,id_resultado) VALUES (?,?,'RESULTADO',?)",
                posterior,disciplina,id);
        assertThrows(IllegalArgumentException.class,()->coord.decidir(pedido,true,MOTIVO));
        assertEquals("PENDENTE",coord.historico(id).getFirst().situacao());
        assertEquals("APROVADO",inscricoes.listar(matricula).getFirst().situacao());
    }

    @Test
    void bibliotecaImpedeRenovacaoEConfirmacaoExigeRenovacao() throws SQLException {
        long periodo=periodo(10,100);
        long turma=turma(periodo,disciplina,"T");
        long acervo=g(c,"INSERT INTO item_acervo(titulo,tipo,classificacao,informacoes_bibliograficas,situacao) VALUES (?,'LIVRO','TESTE','fixture','ATIVO')",tag);
        long exemplar=g(c,"INSERT INTO exemplar(id_item_acervo,identificacao,situacao) VALUES (?,?,'ATIVO')",acervo,tag);
        long emprestimo=g(c,"INSERT INTO emprestimo(id_usuario_biblioteca,id_exemplar,data_emprestimo,data_prevista_devolucao,situacao) VALUES (?,?,CURRENT_DATE,CURRENT_DATE,'ATIVO')",
                biblioteca,exemplar);
        var mat=new RepositorioMatriculaJdbc(c);
        assertThrows(IllegalArgumentException.class,()->inscricoes.inscreverComoAluno(aluno.sessao(),matricula,turma));
        assertThrows(IllegalArgumentException.class,()->mat.rematricular(outraConta.sessao(),matricula,periodo));
        assertThrows(IllegalArgumentException.class,()->new RepositorioPendenciaBibliotecaJdbc(c,aluno.sessao())
                .registrar(emprestimo,"ATRASO",BigDecimal.ZERO,MOTIVO));
        long pendencia=g(c,"INSERT INTO pendencia_biblioteca(id_emprestimo,tipo_pendencia,valor,impeditiva,situacao) VALUES (?,'ATRASO',0,FALSE,'ATIVO')",emprestimo);
        assertThrows(IllegalArgumentException.class,()->mat.rematricular(aluno.sessao(),matricula,periodo));
        assertTrue(inscricoes.listar(matricula).isEmpty());
        g(c,"UPDATE pendencia_biblioteca SET situacao='REGULARIZADO' WHERE id_pendencia=?",pendencia);
        mat.rematricular(aluno.sessao(),matricula,periodo);
        assertTrue(inscricoes.listar(matricula).isEmpty());
        assertThrows(IllegalArgumentException.class,()->mat.rematricular(aluno.sessao(),matricula,periodo));
        assertEquals("ATIVO",inscricoes.inscreverComoAluno(aluno.sessao(),matricula,turma).situacao());
    }
    @Test
    void retificacaoRevalidaInicioDaInscricaoNaDecisao() throws SQLException {
        long origem=periodo(-120,-1);
        long id=inscricaoDireta(matricula,turma(origem,disciplina,"Origem"));
        inscricoes.registrarResultado(id,APROVACAO.p1(),APROVACAO.p2(),null,120,110,MOTIVO);
        long seguinte=periodo(30,140);
        long pedido=retificacoes(docente).solicitar(id,REPROVACAO,MOTIVO);
        // Simula a passagem do calendário apenas nos dados não confirmados desta fixture.
        g(c,"UPDATE periodo_letivo SET data_inicio=? WHERE id_periodo=?",hoje.plusDays(14),seguinte);
        assertThrows(IllegalArgumentException.class,()->retificacoes(coordenador).decidir(pedido,true,MOTIVO));
        assertEquals("APROVADO",inscricoes.listar(matricula).getFirst().situacao());
        assertEquals("PENDENTE",retificacoes(coordenador).historico(id).getFirst().situacao());
    }

    private Conta conta(String papel) throws SQLException {
        String login=tag+papel;
        long id=g(c,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",login,hash);
        return new Conta(id,new SegurancaJdbc(c).entrar(login,SENHA.toCharArray()));
    }
    @Test
    void provisionamentoAcademicoExigeAdminEPreservaVinculoAnterior() throws SQLException {
        var seguranca=new SegurancaJdbc(c);
        long perfil=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,'Fixture','ATIVO')",tag);
        var permissoes=q(c,"SELECT id_permissao FROM permissao WHERE codigo=?",r->r.getLong(1),SegurancaJdbc.ADMIN);
        long permissao=permissoes.isEmpty()
                ?g(c,"INSERT INTO permissao(codigo,descricao) VALUES (?,'Admin de fixture')",SegurancaJdbc.ADMIN)
                :permissoes.getFirst();
        g(c,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,permissao);
        g(c,"INSERT INTO usuario_perfil VALUES (?,?)",coordenador.id(),perfil);
        long nova=seguranca.criarConta(coordenador.sessao(),tag+"-nova",SENHA.toCharArray(),"SEM_PERMISSOES");
        long idAluno=q(c,"SELECT id_aluno FROM matricula_academica WHERE id_matricula=?",r->r.getLong(1),matricula).getFirst();
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(aluno.sessao(),nova,"ALUNO",idAluno));
        assertThrows(IllegalArgumentException.class,()->seguranca.vincularContaAcademica(coordenador.sessao(),nova,"ALUNO",idAluno));
        assertTrue(SegurancaJdbc.ehAluno(c,aluno.id(),idAluno));
        g(c,"UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",idAluno);
        seguranca.vincularContaAcademica(coordenador.sessao(),nova,"ALUNO",idAluno);
        assertTrue(SegurancaJdbc.ehAluno(c,nova,idAluno));
        assertThrows(IllegalArgumentException.class,
                ()->seguranca.vincularContaAcademica(coordenador.sessao(),nova,"PROFESSOR",professor));
    }
    private long matricular(long usuario) throws SQLException {
        long id=g(c,"INSERT INTO aluno(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",usuario,tag);
        return g(c,"INSERT INTO matricula_academica(id_aluno,id_curso,id_periodo_ingresso,data_ingresso,situacao) VALUES (?,?,?,?,'ATIVO')",
                id,curso,ingresso,hoje.minusDays(300));
    }
    private long periodo(int inicio,int fim) throws SQLException {
        return g(c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",
                tag,hoje.plusDays(inicio),hoje.plusDays(fim));
    }
    private long disciplina(String codigo) throws SQLException {
        return g(c,"INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+codigo,tag);
    }
    private long turma(long periodo,long disciplina,String codigo) throws SQLException {
        long id=g(c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,1,'ATIVO')",
                disciplina,professor,periodo,tag+codigo);
        g(c,"INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,'08:00','09:00')",id,sala);
        return id;
    }
    private long inscricaoDireta(long matricula,long turma) throws SQLException {
        return g(c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",
                matricula,turma,hoje);
    }
    private boolean vagaRetida(long id) throws SQLException {
        return q(c,"SELECT vaga_retida FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getBoolean(1),id).getFirst();
    }
    private RepositorioCancelamentoJdbc cancelamentos(Conta conta) {
        return new RepositorioCancelamentoJdbc(c,conta.sessao());
    }
    private RepositorioRetificacaoJdbc retificacoes(Conta conta) {
        return new RepositorioRetificacaoJdbc(c,conta.sessao());
    }
}

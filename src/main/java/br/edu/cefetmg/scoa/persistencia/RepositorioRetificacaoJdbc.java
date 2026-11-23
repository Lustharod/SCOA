package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.Matricula;
import br.edu.cefetmg.scoa.dominio.RetificacaoResultado;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

public final class RepositorioRetificacaoJdbc {
    public record Pedido(long id,long inscricao,long solicitante,String motivo,LocalDateTime solicitadoEm,
                         String anterior,RetificacaoResultado proposta,String situacao,Long coordenador,
                         String parecer,LocalDateTime decididoEm) { }

    private final Connection conexao;
    private final SegurancaJdbc.Sessao sessao;
    private final RepositorioInscricaoJdbc inscricoes;

    public RepositorioRetificacaoJdbc(Connection conexao,SegurancaJdbc.Sessao sessao) {
        this.conexao=conexao;
        this.sessao=sessao;
        this.inscricoes=new RepositorioInscricaoJdbc(conexao);
    }

    public long solicitar(long inscricao,RetificacaoResultado proposta,String motivo) {
        String justificativa=Matricula.validarMotivo(motivo);
        proposta.calcular();
        return TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            var contexto=inscricoes.bloquearInscricao(inscricao);
            if(!professorDaTurma(usuario,contexto.professor()))
                throw new IllegalArgumentException("Somente o professor da turma pode solicitar retificação.");
            var agora=agoraUtc(conexao);
            var hoje=dataAcademica(agora);
            if(!RetificacaoResultado.noPrazoDoProfessor(contexto.fim(),hoje))
                throw new IllegalArgumentException("O professor pode solicitar nos sete dias seguintes ao fim do período.");
            exigirAntesDaInscricaoSeguinte(contexto,hoje);
            if(!q(conexao,"SELECT id_pedido FROM retificacao_resultado WHERE pedido_aberto=? FOR UPDATE",
                    r->r.getLong(1),inscricao).isEmpty())
                throw new IllegalArgumentException("Já existe uma retificação pendente para esta inscrição.");
            String anterior=snapshot(contexto);
            return g(conexao,"""
                    INSERT INTO retificacao_resultado
                        (id_inscricao,solicitante,motivo,solicitado_em,anterior,p1,p2,pf,total_minutos,presentes_minutos)
                    VALUES (?,?,?,?,CAST(? AS JSON),?,?,?,?,?)
                    """,inscricao,usuario,justificativa,agora,anterior,proposta.p1(),proposta.p2(),proposta.pf(),
                    proposta.total(),proposta.presentes());
        });
    }

    public void decidir(long id,boolean deferir,String parecer) {
        HorarioAula.validarId(id);
        String fundamento=Matricula.validarMotivo(parecer);
        TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            var pedidos=q(conexao,"SELECT * FROM retificacao_resultado WHERE id_pedido=? FOR UPDATE",
                    RepositorioRetificacaoJdbc::ler,id);
            if(pedidos.isEmpty()) throw new IllegalArgumentException("Pedido de retificação não encontrado.");
            var pedido=pedidos.getFirst();
            var contexto=inscricoes.bloquearInscricao(pedido.inscricao());
            exigirCoordenacao(usuario,contexto.curso());
            if(!"PENDENTE".equals(pedido.situacao()))
                throw new IllegalArgumentException("O pedido já foi decidido.");
            var agora=agoraUtc(conexao);
            if(deferir) aprovar(pedido,contexto,dataAcademica(agora));
            g(conexao,"UPDATE retificacao_resultado SET situacao=?,coordenador=?,parecer=?,decidido_em=? WHERE id_pedido=?",
                    deferir?"DEFERIDA":"INDEFERIDA",usuario,fundamento,agora,id);
            return null;
        });
    }

    public List<Pedido> historico(long inscricao) {
        return TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            var contexto=inscricoes.bloquearInscricao(inscricao);
            if(!professorDaTurma(usuario,contexto.professor())) exigirCoordenacao(usuario,contexto.curso());
            return q(conexao,"SELECT * FROM retificacao_resultado WHERE id_inscricao=? ORDER BY id_pedido FOR SHARE",
                    RepositorioRetificacaoJdbc::ler,inscricao);
        });
    }

    public void definirSucessor(long curso,long periodo,long sucessor) {
        HorarioAula.validarId(curso);
        HorarioAula.validarId(periodo);
        HorarioAula.validarId(sucessor);
        TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            exigirCoordenacao(usuario,curso);
            var origem=q(conexao,"SELECT data_fim FROM periodo_letivo WHERE id_periodo=? FOR SHARE",
                    r->r.getObject(1,LocalDate.class),periodo);
            if(origem.isEmpty()) throw new IllegalArgumentException("Período não encontrado.");
            var seguinte=proximoPeriodo(origem.getFirst());
            if(seguinte.id()!=sucessor)
                throw new IllegalArgumentException("O próximo período do calendário é "+seguinte.id()+".");
            g(conexao,"""
                    INSERT INTO sucessao_periodo_curso(id_curso,id_periodo,id_sucessor) VALUES (?,?,?)
                    ON DUPLICATE KEY UPDATE id_sucessor=?
                    """,curso,periodo,sucessor,sucessor);
            return null;
        });
    }

    private void aprovar(Pedido pedido,RepositorioInscricaoJdbc.ContextoInscricao contexto,LocalDate hoje) throws SQLException {
        if(!hoje.isAfter(contexto.fim())) throw new IllegalArgumentException("O período de origem ainda está em andamento.");
        exigirAntesDaInscricaoSeguinte(contexto,hoje);
        if(!q(conexao,"SELECT CAST(? AS JSON)=CAST(? AS JSON)",r->r.getBoolean(1),snapshot(contexto),pedido.anterior()).getFirst())
            throw new IllegalArgumentException("O resultado mudou desde o pedido. Confira os valores atuais.");
        var nova=pedido.proposta();
        var apuracao=nova.calcular();
        if("APROVADO".equals(contexto.inscricao().situacao()) && "REPROVADO".equals(apuracao.situacao()))
            exigirSemDependencia(contexto);
        g(conexao,"""
                UPDATE resultado_inscricao
                SET p1=?,p2=?,pf=?,total_minutos=?,presentes_minutos=?,media_inicial=?,media_final=?,situacao=?,motivo=?
                WHERE id_inscricao=?
                """,nova.p1(),nova.p2(),nova.pf(),nova.total(),nova.presentes(),apuracao.mediaInicial(),apuracao.mediaFinal(),
                apuracao.situacao(),pedido.motivo(),pedido.inscricao());
        g(conexao,"UPDATE inscricao_disciplina SET situacao=? WHERE id_inscricao=?",apuracao.situacao(),pedido.inscricao());
    }

    private void exigirSemDependencia(RepositorioInscricaoJdbc.ContextoInscricao contexto) throws SQLException {
        var dependentes=q(conexao,"""
                SELECT i.id_inscricao FROM inscricao_disciplina i
                JOIN evidencia_requisito_inscricao e ON e.id_inscricao=i.id_inscricao
                WHERE e.id_resultado=? AND i.situacao<>'CANCELADO' FOR SHARE
                """,r->r.getLong(1),contexto.inscricao().id());
        var semEvidencia=q(conexao,"""
                SELECT i.id_inscricao FROM inscricao_disciplina i
                JOIN turma t ON t.id_turma=i.id_turma
                JOIN disciplina_prerequisito pr ON pr.id_disciplina=t.id_disciplina
                JOIN turma origem ON origem.id_turma=? AND origem.id_disciplina=pr.id_disciplina_prerequisito
                LEFT JOIN evidencia_requisito_inscricao e
                    ON e.id_inscricao=i.id_inscricao AND e.id_requisito=pr.id_disciplina_prerequisito
                WHERE i.id_matricula=? AND i.data_inscricao>? AND i.situacao<>'CANCELADO'
                    AND e.id_inscricao IS NULL FOR SHARE
                """,r->r.getLong(1),contexto.inscricao().turma(),contexto.inscricao().matricula(),contexto.fim());
        if(!dependentes.isEmpty() || !semEvidencia.isEmpty())
            throw new IllegalArgumentException("Há inscrição posterior que usou ou pode ter usado este pré-requisito. A correção exige análise de impacto.");
    }

    private void exigirAntesDaInscricaoSeguinte(RepositorioInscricaoJdbc.ContextoInscricao contexto,LocalDate hoje) throws SQLException {
        var seguinte=proximoPeriodo(contexto.fim());
        if(!RetificacaoResultado.antesDaInscricaoSeguinte(seguinte.inicio(),hoje))
            throw new IllegalArgumentException("A primeira fase de inscrição do próximo período já começou.");
    }

    private record ProximoPeriodo(long id,LocalDate inicio) { }
    private ProximoPeriodo proximoPeriodo(LocalDate fim) throws SQLException {
        var seguintes=q(conexao,"""
                SELECT id_periodo,data_inicio FROM periodo_letivo
                WHERE situacao='ATIVO' AND data_inicio>?
                ORDER BY data_inicio,id_periodo LIMIT 1 FOR SHARE
                """,r->new ProximoPeriodo(r.getLong(1),r.getObject(2,LocalDate.class)),fim);
        if(seguintes.isEmpty())
            throw new IllegalArgumentException("Cadastre o próximo período letivo para conferir o prazo de retificação.");
        return seguintes.getFirst();
    }

    private String snapshot(RepositorioInscricaoJdbc.ContextoInscricao contexto) throws SQLException {
        var resultados=q(conexao,"""
                SELECT CAST(JSON_OBJECT('p1',p1,'p2',p2,'pf',pf,'mediaInicial',media_inicial,
                    'mediaFinal',media_final,'total',total_minutos,'presentes',presentes_minutos,
                    'situacao',situacao,'motivo',motivo,'registradoEm',registrado_em) AS CHAR)
                FROM resultado_inscricao WHERE id_inscricao=? AND situacao=? FOR UPDATE
                """,r->r.getString(1),contexto.inscricao().id(),contexto.inscricao().situacao());
        if(resultados.isEmpty()) throw new IllegalArgumentException("A inscrição precisa ter um resultado final válido.");
        return resultados.getFirst();
    }

    private boolean professorDaTurma(long usuario,long professor) throws SQLException {
        return !q(conexao,"SELECT id_professor FROM professor WHERE id_professor=? AND id_usuario=? AND situacao='ATIVO' FOR SHARE",
                r->r.getLong(1),professor,usuario).isEmpty();
    }

    private void exigirCoordenacao(long usuario,long curso) throws SQLException {
        if(!SegurancaJdbc.coordena(conexao,usuario,curso))
            throw new IllegalArgumentException("A operação cabe à coordenação do curso.");
    }

    private static Pedido ler(ResultSet r) throws SQLException {
        return new Pedido(r.getLong("id_pedido"),r.getLong("id_inscricao"),r.getLong("solicitante"),r.getString("motivo"),
                r.getObject("solicitado_em",LocalDateTime.class),r.getString("anterior"),
                new RetificacaoResultado(r.getBigDecimal("p1"),r.getBigDecimal("p2"),r.getBigDecimal("pf"),
                        r.getLong("total_minutos"),r.getLong("presentes_minutos")),r.getString("situacao"),
                r.getObject("coordenador",Long.class),r.getString("parecer"),r.getObject("decidido_em",LocalDateTime.class));
    }
}

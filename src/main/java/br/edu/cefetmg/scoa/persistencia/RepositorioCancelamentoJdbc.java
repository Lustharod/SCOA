package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.CancelamentoInscricao;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.Matricula;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

public final class RepositorioCancelamentoJdbc {
    private final Connection conexao;
    private final SegurancaJdbc.Sessao sessao;
    private final RepositorioInscricaoJdbc inscricoes;

    public RepositorioCancelamentoJdbc(Connection conexao,SegurancaJdbc.Sessao sessao) {
        this.conexao=conexao;
        this.sessao=sessao;
        this.inscricoes=new RepositorioInscricaoJdbc(conexao);
    }

    public CancelamentoInscricao solicitar(long inscricao,String justificativa) {
        String motivo=Matricula.validarMotivo(justificativa);
        return TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            var contexto=inscricoes.bloquearInscricao(inscricao);
            SegurancaJdbc.exigirAluno(conexao,usuario,contexto.aluno());
            exigirAtiva(contexto);
            var agora=agoraUtc(conexao);
            var hoje=dataAcademica(agora);
            if(!CancelamentoInscricao.permitePedido(contexto.inicio(),contexto.fim(),hoje))
                throw new IllegalArgumentException("Prazo para solicitar cancelamento encerrado.");
            if(!q(conexao,"SELECT id_pedido FROM cancelamento_inscricao WHERE pedido_aberto=? FOR UPDATE",
                    r->r.getLong(1),inscricao).isEmpty())
                throw new IllegalArgumentException("Já existe um pedido pendente para esta inscrição.");

            boolean imediato=CancelamentoInscricao.imediato(contexto.inicio(),hoje);
            String situacao=imediato?"AUTOMATICO":"PENDENTE";
            long id=g(conexao,"""
                    INSERT INTO cancelamento_inscricao(id_inscricao,solicitante,justificativa,solicitado_em,situacao,decidido_em)
                    VALUES (?,?,?,?,?,?)
                    """,inscricao,usuario,motivo,agora,situacao,imediato?agora:null);
            if(imediato) cancelar(inscricao,false);
            return new CancelamentoInscricao(id,inscricao,usuario,motivo,agora,situacao,null,null,imediato?agora:null);
        });
    }

    public CancelamentoInscricao decidir(long pedido,boolean deferir,String parecer) {
        HorarioAula.validarId(pedido);
        String texto=Matricula.validarMotivo(parecer);
        return TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            var pedidos=q(conexao,"SELECT * FROM cancelamento_inscricao WHERE id_pedido=? FOR UPDATE",
                    RepositorioCancelamentoJdbc::ler,pedido);
            if(pedidos.isEmpty()) throw new IllegalArgumentException("Pedido de cancelamento não encontrado.");
            var atual=pedidos.getFirst();
            var contexto=inscricoes.bloquearInscricao(atual.inscricao());
            if(!SegurancaJdbc.coordena(conexao,usuario,contexto.curso()))
                throw new IllegalArgumentException("A decisão cabe à coordenação do curso.");
            if(!"PENDENTE".equals(atual.situacao())) throw new IllegalArgumentException("O pedido já foi decidido.");
            var agora=agoraUtc(conexao);
            if(deferir) {
                exigirAtiva(contexto);
                if(dataAcademica(agora).isAfter(contexto.fim()))
                    throw new IllegalArgumentException("O período letivo encerrou. A inscrição já não pode ser cancelada.");
                cancelar(atual.inscricao(),true);
            }
            String situacao=deferir?"DEFERIDO":"INDEFERIDO";
            g(conexao,"UPDATE cancelamento_inscricao SET situacao=?,coordenador=?,parecer=?,decidido_em=? WHERE id_pedido=?",
                    situacao,usuario,texto,agora,pedido);
            return new CancelamentoInscricao(pedido,atual.inscricao(),atual.solicitante(),atual.justificativa(),
                    atual.solicitadoEm(),situacao,usuario,texto,agora);
        });
    }

    public List<CancelamentoInscricao> historico(long inscricao) {
        return TransacaoJdbc.executar(conexao,()->{
            controle(conexao);
            long usuario=SegurancaJdbc.exigirSessao(conexao,sessao);
            var contexto=inscricoes.bloquearInscricao(inscricao);
            if(!SegurancaJdbc.ehAluno(conexao,usuario,contexto.aluno()) &&
                    !SegurancaJdbc.coordena(conexao,usuario,contexto.curso()))
                throw new IllegalArgumentException("Histórico restrito ao próprio aluno e à coordenação do curso.");
            return q(conexao,"SELECT * FROM cancelamento_inscricao WHERE id_inscricao=? ORDER BY id_pedido FOR SHARE",
                    RepositorioCancelamentoJdbc::ler,inscricao);
        });
    }

    private static void exigirAtiva(RepositorioInscricaoJdbc.ContextoInscricao contexto) {
        if(!"ATIVO".equals(contexto.inscricao().situacao()))
            throw new IllegalArgumentException("Somente uma inscrição ativa pode ser cancelada.");
    }

    private void cancelar(long inscricao,boolean reterVaga) throws SQLException {
        g(conexao,"UPDATE inscricao_disciplina SET situacao='CANCELADO',vaga_retida=? WHERE id_inscricao=?",
                reterVaga,inscricao);
    }

    private static CancelamentoInscricao ler(ResultSet r) throws SQLException {
        return new CancelamentoInscricao(r.getLong("id_pedido"),r.getLong("id_inscricao"),r.getLong("solicitante"),
                r.getString("justificativa"),r.getObject("solicitado_em",LocalDateTime.class),r.getString("situacao"),
                r.getObject("coordenador",Long.class),r.getString("parecer"),r.getObject("decidido_em",LocalDateTime.class));
    }
}

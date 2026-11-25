package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import java.time.*;
import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

public final class RepositorioParametrosAcademicosJdbc implements RepositorioParametrosAcademicos {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioParametrosAcademicosJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    @Override public List<Versao> versoes() {
        return acesso.consultarParametros(()->TransacaoJdbc.executar(c,()->q(c,"SELECT * FROM versao_parametros_academicos ORDER BY numero FOR SHARE",ParametroPeriodoJdbc::ler)));
    }
    @Override public Versao criar(Criar pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manterParametros("CRIAR_VERSAO",0,()->TransacaoJdbc.executar(c,()->{
            int numero=q(c,"SELECT COALESCE(MAX(numero),0) FROM versao_parametros_academicos",r->r.getInt(1)).getFirst();
            if(numero==Integer.MAX_VALUE) throw new IllegalArgumentException("Limite de versões atingido.");
            var r=pedido.regras(); long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            long id=g(c,"""
                    INSERT INTO versao_parametros_academicos(numero,antecedencia_primeira,duracao_primeira,inicio_segunda,duracao_segunda,
                        cancelamento_imediato_ate,cancelamento_pedido_ate,transferencia_ate,retificacao_docente_dias,tolerancia_atraso,
                        frequencia_percentual,media_dispensa_pf,media_final_minima,motivo,id_usuario_autor,registrado_em_utc,assinatura)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,numero+1,r.antecedenciaPrimeira(),r.duracaoPrimeira(),r.inicioSegunda(),r.duracaoSegunda(),r.cancelamentoImediatoAte(),
                    r.cancelamentoPedidoAte(),r.transferenciaAte(),r.retificacaoDocenteDias(),r.toleranciaAtraso(),r.frequenciaPercentual(),
                    r.mediaDispensaPf(),r.mediaFinalMinima(),pedido.motivo(),autor,agora,hash(r.canonico()));
            return ParametroPeriodoJdbc.versao(c,id);
        }));
    }
    @Override public Periodo consultarPeriodo(long periodo) {
        HorarioAula.validarId(periodo);
        return acesso.consultarParametros(()->TransacaoJdbc.executar(c,()->periodo(periodo)));
    }
    private Periodo periodo(long id) throws SQLException {
        var lista=q(c,"SELECT * FROM periodo_letivo WHERE id_periodo=? FOR UPDATE",r->new Object[]{r.getString("descricao"),r.getObject("data_inicio",LocalDate.class),r.getObject("data_fim",LocalDate.class),r.getString("situacao"),r.getInt("versao_vinculo_parametros"),r.getObject("id_versao_parametros",Long.class)},id);
        if(lista.isEmpty()) throw new IllegalArgumentException("Período não encontrado.");
        var p=lista.getFirst(); var contexto=ParametroPeriodoJdbc.porPeriodo(c,id);
        long turmas=q(c,"SELECT COUNT(*) FROM turma WHERE id_periodo=?",r->r.getLong(1),id).getFirst();
        long inscricoes=q(c,"SELECT COUNT(*) FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma WHERE t.id_periodo=?",r->r.getLong(1),id).getFirst();
        boolean futuro=((LocalDate)p[1]).isAfter(dataAcademica(agoraUtc(c)));
        String assinatura=hash(id+"|"+Arrays.toString(p)+"|"+contexto.regras().canonico()+"|"+turmas+"|"+inscricoes+"|"+futuro);
        return new Periodo(id,(String)p[0],(LocalDate)p[1],(LocalDate)p[2],(String)p[3],(int)p[4],(Long)p[5],contexto.regras(),turmas,inscricoes,futuro,assinatura);
    }
    @Override public Periodo vincular(Vincular pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manterParametros("VINCULAR_PERIODO",pedido.periodo(),()->TransacaoJdbc.executar(c,()->{
            var anterior=periodo(pedido.periodo());
            if(!anterior.futuro() || !"ATIVO".equals(anterior.situacao()))
                throw new IllegalArgumentException("Somente períodos ativos ainda não iniciados podem selecionar regras. Períodos já iniciados preservam sua seleção.");
            if(anterior.versaoVinculo()!=pedido.versaoVinculo() || !anterior.assinatura().equals(pedido.assinatura()))
                throw new IllegalArgumentException("O período mudou desde a prévia; confira novamente.");
            if(pedido.versaoParametros()!=null) ParametroPeriodoJdbc.versao(c,pedido.versaoParametros());
            if(Objects.equals(anterior.versaoParametros(),pedido.versaoParametros())) return anterior;
            if(!q(c,"SELECT a.id_aula FROM aula a JOIN turma t ON t.id_turma=a.id_turma WHERE t.id_periodo=? LIMIT 1 FOR SHARE",r->r.getLong(1),pedido.periodo()).isEmpty()
                    || !q(c,"SELECT a.id_avaliacao FROM avaliacao a JOIN turma t ON t.id_turma=a.id_turma WHERE t.id_periodo=? LIMIT 1 FOR SHARE",r->r.getLong(1),pedido.periodo()).isEmpty()
                    || !q(c,"SELECT r.id_inscricao FROM resultado_inscricao r JOIN inscricao_disciplina i ON i.id_inscricao=r.id_inscricao JOIN turma t ON t.id_turma=i.id_turma WHERE t.id_periodo=? LIMIT 1 FOR SHARE",r->r.getLong(1),pedido.periodo()).isEmpty())
                throw new IllegalArgumentException("O período possui lançamentos acadêmicos; confira a preparação dos dados antes de selecionar regras futuras.");
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            g(c,"UPDATE periodo_letivo SET id_versao_parametros=?,versao_vinculo_parametros=versao_vinculo_parametros+1 WHERE id_periodo=?",pedido.versaoParametros(),pedido.periodo());
            g(c,"INSERT INTO historico_parametros_periodo(id_periodo,versao_vinculo,id_versao_anterior,id_versao_nova,motivo,id_usuario_autor,registrado_em_utc) VALUES (?,?,?,?,?,?,?)",
                    pedido.periodo(),anterior.versaoVinculo()+1,anterior.versaoParametros(),pedido.versaoParametros(),pedido.motivo(),autor,agora);
            return periodo(pedido.periodo());
        }));
    }
    @Override public List<Historico> historico(long periodo) {
        HorarioAula.validarId(periodo);
        return acesso.consultarParametros(()->TransacaoJdbc.executar(c,()->{
            periodo(periodo);
            return q(c,"SELECT * FROM historico_parametros_periodo WHERE id_periodo=? ORDER BY versao_vinculo FOR SHARE",r->new Historico(r.getLong("id_evento"),periodo,r.getInt("versao_vinculo"),r.getObject("id_versao_anterior",Long.class),r.getObject("id_versao_nova",Long.class),r.getString("motivo"),r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class)),periodo);
        }));
    }
}

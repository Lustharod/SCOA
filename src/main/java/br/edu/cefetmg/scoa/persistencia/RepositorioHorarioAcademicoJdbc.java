package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.HorarioAcademico.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

public final class RepositorioHorarioAcademicoJdbc implements RepositorioHorarioAcademico {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioHorarioAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    private TurmaResumo turma(long id,boolean manter) throws SQLException {
        var turmas=q(c,"""
                SELECT t.codigo,t.capacidade,p.data_inicio,p.data_fim,t.situacao,p.situacao FROM turma t
                JOIN periodo_letivo p ON p.id_periodo=t.id_periodo WHERE t.id_turma=? FOR UPDATE
                """,r->new Object[]{r.getString(1),r.getInt(2),r.getObject(3,LocalDate.class),r.getObject(4,LocalDate.class),r.getString(5),r.getString(6)},id);
        if(turmas.isEmpty()) throw new IllegalArgumentException("Turma não encontrada.");
        var t=turmas.getFirst();
        if(manter && (!"ATIVO".equals(t[4]) || !"ATIVO".equals(t[5]) || ((LocalDate)t[3]).isBefore(dataAcademica(agoraUtc(c)))))
            throw new IllegalArgumentException("Alteração exige turma e período ativos ainda não encerrados.");
        return new TurmaResumo(id,(String)t[0],(int)t[1],new PeriodoLetivo((LocalDate)t[2],(LocalDate)t[3]));
    }
    @Override public List<Registro> planejar(Planejar pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manter("HORARIO_ACADEMICO","PLANEJAR",pedido.turma(),()->TransacaoJdbc.executar(c,()->{
            var t=turma(pedido.turma(),true); semRegistrosAcademicos(pedido.turma());
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c); var novos=new ArrayList<Registro>();
            for(var tempo:pedido.tempos()) {
                var faixa=new FaixaHorario(pedido.dia(),tempo.inicio(),tempo.fim()); RegraAlocacao.validarDia(t,faixa);
                if(!q(c,"SELECT id_horario FROM horario_aula WHERE id_turma=? AND ativo=TRUE AND dia_semana=? AND hora_inicio<? AND hora_fim>? FOR UPDATE",
                        r->r.getLong(1),pedido.turma(),pedido.dia(),tempo.fim(),tempo.inicio()).isEmpty())
                    throw new IllegalArgumentException("A turma já possui horário sobreposto ao tempo "+tempo+".");
                long id=g(c,"""
                        INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim,id_usuario_autor,atualizado_em_utc)
                        VALUES (?,NULL,?,?,?,?,?)
                        """,pedido.turma(),pedido.dia(),tempo.inicio(),tempo.fim(),autor,agora);
                var atual=buscar(id,true); historia(null,atual,"PLANEJAR",pedido.motivo(),autor,agora); novos.add(atual);
            }
            return List.copyOf(novos);
        }));
    }
    @Override public List<Registro> listar(long turma) {
        HorarioAula.validarId(turma);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->{
            turma(turma,false);
            return q(c,"SELECT * FROM horario_aula WHERE id_turma=? ORDER BY dia_semana,hora_inicio,id_horario FOR SHARE",RepositorioHorarioAcademicoJdbc::ler,turma);
        }));
    }
    @Override public List<Sala> salasDisponiveis(long id) {
        HorarioAula.validarId(id);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->{
            var p=buscar(id,false); if(!p.ativo()) throw new IllegalArgumentException("Horário cancelado.");
            var t=turma(p.horario().idTurma(),true); var resultado=new ArrayList<Sala>();
            for(var sala:new RepositorioSalaJdbc(c).listar()) if("ATIVO".equals(sala.getSituacao()) && sala.getCapacidade()>=t.capacidade()
                    && !conflitoSala(p,sala.getId(),t.periodo())) resultado.add(sala);
            return List.copyOf(resultado);
        }));
    }
    @Override public Registro atribuirSala(long id,int versao,long sala,String motivo) {
        HorarioAula.validarId(sala); return alterar(id,versao,sala,"ATRIBUIR_SALA",motivo);
    }
    @Override public Registro retirarSala(long id,int versao,String motivo) { return alterar(id,versao,null,"RETIRAR_SALA",motivo); }
    @Override public Registro cancelar(long id,int versao,String motivo) { return alterar(id,versao,null,"CANCELAR_HORARIO",motivo); }
    void cancelarPorAlocacao(long id) { alterar(id,null,null,"CANCELAR_HORARIO","Cancelamento explícito do horário e sua alocação pelo menu legado."); }
    HorarioAula alocarLegado(HorarioAula pedido) {
        return acesso.manter("HORARIO_ACADEMICO","ALOCAR",pedido.idTurma(),()->TransacaoJdbc.executar(c,()->{
            var h=new RepositorioAlocacaoJdbc(c).alocar(pedido);
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            g(c,"UPDATE horario_aula SET id_usuario_autor=?,atualizado_em_utc=? WHERE id_horario=?",autor,agora,h.id());
            historia(null,buscar(h.id(),true),"ALOCAR","Definição de horário com sala pelo menu legado.",autor,agora); return h;
        }));
    }
    private Registro alterar(long id,Integer versao,Long sala,String operacao,String motivo) {
        HorarioAula.validarId(id); String razao=Matricula.validarMotivo(motivo);
        if(versao!=null && (versao<0 || versao==Integer.MAX_VALUE)) throw new IllegalArgumentException("Versão inválida.");
        return acesso.manter("HORARIO_ACADEMICO",operacao,id,()->TransacaoJdbc.executar(c,()->{
            var previa=buscar(id,false); var salas=new TreeSet<Long>();
            if(previa.horario().idSala()!=null) salas.add(previa.horario().idSala()); if(sala!=null) salas.add(sala);
            var repoSala=new RepositorioSalaJdbc(c); var bloqueadas=new HashMap<Long,Sala>();
            for(long ref:salas) bloqueadas.put(ref,repoSala.buscarBloqueada(ref));
            var t=turma(previa.horario().idTurma(),true); var atual=buscar(id,true);
            if(!atual.equals(previa) || (versao!=null && atual.versao()!=versao))
                throw new IllegalArgumentException("Horário alterado desde a consulta; confira a versão atual.");
            if(!atual.ativo()) throw new IllegalArgumentException("Horário cancelado é preservado e não pode ser reativado por este fluxo.");
            boolean cancelar="CANCELAR_HORARIO".equals(operacao);
            if(cancelar) semRegistrosAcademicos(t.id());
            else if(sala!=null) {
                RegraAlocacao.validarSala(bloqueadas.get(sala),t);
                if(conflitoSala(atual,sala,t.periodo())) throw new IllegalArgumentException("Sala ocupada nesse horário e período; confira as alternativas.");
            }
            if(!cancelar && Objects.equals(atual.horario().idSala(),sala)) return atual;
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            g(c,"UPDATE horario_aula SET id_sala=?,ativo=?,versao=versao+1,id_usuario_autor=?,atualizado_em_utc=? WHERE id_horario=?",
                    cancelar?null:sala,!cancelar,autor,agora,id);
            var novo=buscar(id,true); historia(atual,novo,operacao,razao,autor,agora); return novo;
        }));
    }
    private boolean conflitoSala(Registro registro,long sala,PeriodoLetivo periodo) throws SQLException {
        var h=registro.horario();
        var outros=q(c,"""
                SELECT p.data_inicio,p.data_fim FROM horario_aula h JOIN turma t ON t.id_turma=h.id_turma
                JOIN periodo_letivo p ON p.id_periodo=t.id_periodo WHERE h.ativo=TRUE AND h.id_sala=?
                    AND h.id_horario<>? AND h.dia_semana=? AND h.hora_inicio<? AND h.hora_fim>?
                    AND p.data_inicio<=? AND p.data_fim>=? FOR SHARE
                """,r->new PeriodoLetivo(r.getObject(1,LocalDate.class),r.getObject(2,LocalDate.class)),
                sala,h.id(),h.faixa().diaSemana(),h.faixa().fim(),h.faixa().inicio(),periodo.fim(),periodo.inicio());
        return outros.stream().anyMatch(p->periodo.compartilhaDia(p,h.faixa().diaSemana()));
    }
    private void semRegistrosAcademicos(long turma) throws SQLException {
        if(!q(c,"SELECT id_inscricao FROM inscricao_disciplina WHERE id_turma=? LIMIT 1 FOR UPDATE",r->r.getLong(1),turma).isEmpty()
                || !q(c,"SELECT id_aula FROM aula WHERE id_turma=? LIMIT 1 FOR UPDATE",r->r.getLong(1),turma).isEmpty())
            throw new IllegalArgumentException("Turma com inscrições ou aulas: preserve dias e tempos; é possível manter a sala separadamente.");
    }
    @Override public List<Evento> historico(long id) {
        HorarioAula.validarId(id);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->{
            buscar(id,false); return q(c,"SELECT * FROM historico_horario_academico WHERE id_horario=? ORDER BY versao FOR SHARE",
                    r->new Evento(r.getLong("id_evento"),id,r.getInt("versao"),r.getString("operacao"),r.getString("anterior"),
                        r.getString("novo"),r.getString("motivo"),r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class)),id);
        }));
    }
    private void historia(Registro antes,Registro depois,String operacao,String motivo,long autor,LocalDateTime agora) throws SQLException {
        g(c,"""
                INSERT INTO historico_horario_academico(id_horario,versao,operacao,anterior,novo,motivo,id_usuario_autor,registrado_em_utc)
                VALUES (?,?,?,?,?,?,?,?)
                """,depois.horario().id(),depois.versao(),operacao,antes==null?null:antes.toString(),depois.toString(),motivo,autor,agora);
    }
    private Registro buscar(long id,boolean bloquear) throws SQLException {
        return q(c,"SELECT * FROM horario_aula WHERE id_horario=?"+(bloquear?" FOR UPDATE":""),RepositorioHorarioAcademicoJdbc::ler,id)
                .stream().findFirst().orElseThrow(()->new IllegalArgumentException("Horário não encontrado."));
    }
    private static Registro ler(ResultSet r) throws SQLException {
        return new Registro(new HorarioAula(r.getLong("id_horario"),r.getLong("id_turma"),r.getObject("id_sala",Long.class),
                new FaixaHorario(r.getInt("dia_semana"),r.getObject("hora_inicio",LocalTime.class),r.getObject("hora_fim",LocalTime.class))),
                r.getBoolean("ativo"),r.getInt("versao"),r.getObject("id_usuario_autor",Long.class),r.getObject("atualizado_em_utc",LocalDateTime.class));
    }
}

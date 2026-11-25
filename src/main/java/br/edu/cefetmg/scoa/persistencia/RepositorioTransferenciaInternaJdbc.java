package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.TransferenciaInterna.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Mudança de vínculo, cancelamentos, reconhecimentos e auditoria formam uma transação. */
public final class RepositorioTransferenciaInternaJdbc implements RepositorioTransferenciaInterna {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioTransferenciaInternaJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    @Override public List<Registro> listar(long origem) {
        HorarioAula.validarId(origem);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->q(c,
                "SELECT * FROM transferencia_academica WHERE id_matricula=? ORDER BY id_transferencia FOR SHARE",
                RepositorioTransferenciaInternaJdbc::ler,origem)));
    }
    @Override public Registro efetivar(Pedido pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manter("TRANSFERENCIA_MATRICULA_ORIGEM","EFETIVAR_INTERNA",pedido.origem(),
                ()->TransacaoJdbc.executar(c,()->efetivarBloqueada(pedido)));
    }
    private record Origem(long aluno,long curso,String situacao) { }
    private Registro efetivarBloqueada(Pedido pedido) throws SQLException {
        long autor=SegurancaJdbc.exigirSessao(c,sessao);
        var repetidas=q(c,"""
                SELECT * FROM transferencia_academica WHERE id_matricula=? AND tipo='INTERNA'
                AND situacao='EFETIVADA' AND id_matricula_destino IS NOT NULL ORDER BY id_transferencia FOR UPDATE
                """,RepositorioTransferenciaInternaJdbc::ler,pedido.origem());
        if(!repetidas.isEmpty()) {
            var anterior=repetidas.getLast();
            var destino=q(c,"""
                    SELECT t.id_transferencia FROM transferencia_academica t
                    WHERE t.id_transferencia=? AND t.id_curso_destino=? AND t.id_estrutura_destino=?
                    AND t.id_periodo_transferencia=? AND t.motivo=? FOR SHARE
                    """,r->r.getLong(1),anterior.id(),pedido.cursoDestino(),pedido.estruturaDestino(),pedido.periodo(),pedido.motivo());
            if(!destino.isEmpty()) return anterior;
            throw new IllegalArgumentException("O vínculo de origem já foi transferido. Consulte o processo existente.");
        }
        var previa=q(c,"SELECT id_aluno FROM matricula_academica WHERE id_matricula=?",r->r.getLong(1),pedido.origem());
        if(previa.isEmpty()) throw new IllegalArgumentException("Matrícula de origem não encontrada.");
        if(q(c,"SELECT id_aluno FROM aluno WHERE id_aluno=? AND situacao='ATIVO' FOR UPDATE",r->r.getLong(1),previa.getFirst()).isEmpty())
            throw new IllegalArgumentException("O aluno precisa estar ativo.");
        var origem=q(c,"SELECT id_aluno,id_curso,situacao FROM matricula_academica WHERE id_matricula=? FOR UPDATE",
                r->new Origem(r.getLong(1),r.getLong(2),r.getString(3)),pedido.origem()).getFirst();
        if(origem.aluno()!=previa.getFirst() || !"ATIVO".equals(origem.situacao()))
            throw new IllegalArgumentException("A origem precisa ser um vínculo ativo e estável.");
        if(origem.curso()==pedido.cursoDestino()) throw new IllegalArgumentException("Selecione outro curso para transferência interna.");
        var estruturas=q(c,"SELECT id_curso,situacao FROM estrutura_curricular WHERE id_estrutura=? FOR SHARE",
                r->new Object[]{r.getLong(1),r.getString(2)},pedido.estruturaDestino());
        if(estruturas.isEmpty() || (long)estruturas.getFirst()[0]!=pedido.cursoDestino() || !"ATIVO".equals(estruturas.getFirst()[1]))
            throw new IllegalArgumentException("A estrutura de destino deve estar ativa e pertencer ao curso escolhido.");
        var datas=q(c,"SELECT data_inicio,data_fim FROM periodo_letivo WHERE id_periodo=? AND situacao='ATIVO' FOR SHARE",
                r->new LocalDate[]{r.getObject(1,LocalDate.class),r.getObject(2,LocalDate.class)},pedido.periodo());
        var agora=agoraUtc(c); var hoje=dataAcademica(agora);
        if(datas.isEmpty() || !ParametroPeriodoJdbc.porPeriodo(c,pedido.periodo()).regras().permiteTransferencia(datas.getFirst()[0],datas.getFirst()[1],hoje))
            throw new IllegalArgumentException("A transferência exige período ativo e prazo configurado, sem ultrapassar seu fim.");
        var ativos=q(c,"""
                SELECT i.id_inscricao,t.id_turma,p.data_inicio,p.data_fim
                FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma
                JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                WHERE i.id_matricula=? AND i.situacao='ATIVO' ORDER BY i.id_inscricao FOR UPDATE
                """,r->new Object[]{r.getLong(1),r.getLong(2),r.getObject(3,LocalDate.class),r.getObject(4,LocalDate.class)},pedido.origem());
        for(var i:ativos) {
            if(!ParametroPeriodoJdbc.porTurma(c,(long)i[1]).regras().permiteTransferencia((LocalDate)i[2],(LocalDate)i[3],hoje))
                throw new IllegalArgumentException("Há inscrição ativa fora do prazo de cancelamento da transferência; resolva a pendência acadêmica.");
            if(!q(c,"SELECT id_inscricao FROM resultado_inscricao WHERE id_inscricao=? FOR SHARE",r->r.getLong(1),i[0]).isEmpty())
                throw new IllegalArgumentException("Há inscrição ativa com resultado divergente; conferência acadêmica necessária.");
            if(!q(c,"SELECT id_pedido FROM cancelamento_inscricao WHERE id_inscricao=? AND situacao='PENDENTE' FOR SHARE",r->r.getLong(1),i[0]).isEmpty())
                throw new IllegalArgumentException("Há pedido de cancelamento pendente; conclua sua análise antes da transferência.");
        }
        // O serviço já validado consulta biblioteca e recusa duplicidade de vínculo no destino.
        var destino=new RepositorioMatriculaJdbc(c).criar(origem.aluno(),pedido.cursoDestino(),hoje,pedido.periodo(),pedido.motivo());
        g(c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",destino.id(),pedido.estruturaDestino());
        long id=g(c,"""
                INSERT INTO transferencia_academica(id_matricula,tipo,data,situacao,historico,
                id_matricula_destino,id_curso_destino,id_estrutura_destino,id_periodo_transferencia,id_usuario_autor,motivo,registrado_em)
                VALUES (?,'INTERNA',?,'EFETIVADA','Em efetivação transacional',?,?,?,?,?,?,?)
                """,pedido.origem(),hoje,destino.id(),pedido.cursoDestino(),pedido.estruturaDestino(),pedido.periodo(),autor,pedido.motivo(),agora);
        for(var i:ativos) {
            boolean reter=!ParametroPeriodoJdbc.porTurma(c,(long)i[1]).regras().cancelamentoImediato((LocalDate)i[2],hoje);
            g(c,"UPDATE inscricao_disciplina SET situacao='CANCELADO',vaga_retida=? WHERE id_inscricao=?",reter,i[0]);
        }
        g(c,"UPDATE matricula_academica SET situacao='TRANSFERIDO' WHERE id_matricula=?",pedido.origem());
        g(c,"""
                INSERT INTO historico_matricula(id_matricula,operacao,motivo,situacao_anterior,situacao_nova,
                    data_anterior,data_nova,periodo_anterior,periodo_novo)
                SELECT id_matricula,'TRANSFERENCIA',?,'ATIVO','TRANSFERIDO',data_ingresso,data_ingresso,
                    id_periodo_ingresso,id_periodo_ingresso FROM matricula_academica WHERE id_matricula=?
                """,pedido.motivo(),pedido.origem());
        int reconhecidos=reconhecerIdenticas(id,pedido,destino.id(),autor,hoje);
        String historico="Origem "+pedido.origem()+" preservada como TRANSFERIDO; destino "+destino.id()+
                ". Inscrições canceladas por transferência: "+ativos.stream().map(i->i[0].toString()).toList()+
                ". Disciplinas idênticas com aprovação formal aproveitadas: "+reconhecidos+".";
        g(c,"UPDATE transferencia_academica SET historico=? WHERE id_transferencia=?",historico,id);
        evento(c,"TRANSFERENCIA",id,autor,"EFETIVAR_INTERNA",historico);
        return q(c,"SELECT * FROM transferencia_academica WHERE id_transferencia=?",RepositorioTransferenciaInternaJdbc::ler,id).getFirst();
    }
    private int reconhecerIdenticas(long transferencia,Pedido pedido,long destino,long autor,LocalDate hoje) throws SQLException {
        var aprovadas=q(c,"""
                SELECT i.id_inscricao,d.id_disciplina,d.nome,d.carga_horaria,p.data_fim,
                    CAST(JSON_OBJECT('inscricao',i.id_inscricao,'p1',r.p1,'p2',r.p2,'pf',r.pf,
                        'mediaInicial',r.media_inicial,'mediaFinal',r.media_final,'presentes',r.presentes_minutos,
                        'total',r.total_minutos,'situacao',r.situacao) AS CHAR) AS snapshot
                FROM inscricao_disciplina i JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao
                JOIN turma t ON t.id_turma=i.id_turma JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                JOIN disciplina d ON d.id_disciplina=t.id_disciplina
                JOIN estrutura_disciplina e ON e.id_disciplina=d.id_disciplina AND e.id_estrutura=?
                WHERE i.id_matricula=? AND i.situacao='APROVADO' AND r.situacao='APROVADO'
                    AND d.situacao='ATIVO' AND p.data_fim<?
                ORDER BY p.data_fim DESC,i.id_inscricao DESC FOR SHARE
                """,r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getInt(4),r.getObject(5,LocalDate.class),r.getString(6)},
                pedido.estruturaDestino(),pedido.origem(),hoje);
        var usadas=new HashSet<Long>();
        for(var a:aprovadas) if(usadas.add((long)a[1])) {
            String fundamento="Transferência interna "+transferencia+"; matrícula de origem "+pedido.origem()+"; resultado formal "+a[0]+"; mesma disciplina cadastrada "+a[1]+".";
            long reconhecimento=g(c,"""
                    INSERT INTO pedido_equivalencia(id_matricula,id_estrutura,id_disciplina_destino,instituicao_origem,
                        disciplina_origem,carga_horaria_origem,ementa_origem,documentos,conclusao_origem,ementa_destino,
                        carga_horaria_destino,situacao,parecer,responsavel,decidido_em)
                    VALUES (?,?,?,'SCOA - transferência interna',?,?,?, ?,?,?,?,'DEFERIDO',?,?,CURRENT_TIMESTAMP(6))
                    """,destino,pedido.estruturaDestino(),a[1],a[2],a[3],"Mesma disciplina cadastrada.",fundamento,a[4],
                    "Mesma disciplina cadastrada.",a[3],"Aproveitamento automático de disciplina idêntica, conforme regra de transferência.","Usuário autenticado "+autor);
            g(c,"""
                    INSERT INTO aproveitamento_transferencia(id_transferencia,id_pedido,id_inscricao_origem,id_disciplina,resultado_origem)
                    VALUES (?,?,?,?,CAST(? AS JSON))
                    """,transferencia,reconhecimento,a[0],a[1],a[5]);
            evento(c,"PEDIDO",reconhecimento,autor,"APROVEITAR_IDENTICA",fundamento);
        }
        return usadas.size();
    }
    private static Registro ler(ResultSet r) throws SQLException {
        return new Registro(r.getLong("id_transferencia"),r.getLong("id_matricula"),r.getObject("id_matricula_destino",Long.class),
                r.getObject("id_curso_destino",Long.class),r.getString("tipo"),r.getString("situacao"),r.getObject("data",LocalDate.class),
                r.getObject("id_usuario_autor",Long.class),r.getString("motivo"),r.getString("historico"),r.getObject("registrado_em",LocalDateTime.class));
    }
}

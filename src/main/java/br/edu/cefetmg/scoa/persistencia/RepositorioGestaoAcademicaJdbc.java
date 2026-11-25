package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.GestaoAcademica.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.Aula;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.Avaliacao;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Gestão por curso. Turma compartilhada não autoriza revelar alunos de outro curso. */
public final class RepositorioGestaoAcademicaJdbc implements RepositorioGestaoAcademica {
    private final Connection c; private final SegurancaJdbc.Sessao sessao;
    private final AcessoGestaoAcademicaJdbc acesso;
    public RepositorioGestaoAcademicaJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoGestaoAcademicaJdbc(c,sessao);
    }
    @Override public List<CursoDisponivel> cursos() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); var cursos=new ArrayList<CursoDisponivel>();
            for(long id:acesso.cursos()) cursos.add(q(c,"SELECT id_curso,nome,situacao FROM curso WHERE id_curso=? FOR SHARE",r->new CursoDisponivel(r.getLong(1),r.getString(2),r.getString(3)),id).getFirst());
            return List.copyOf(cursos);
        });
    }
    @Override public List<Docente> docentes(long curso) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); acesso.curso(curso); var lista=new ArrayList<Docente>();
            for(var d:q(c,"SELECT id_professor,identificacao,situacao FROM professor ORDER BY id_professor FOR SHARE",r->new Docente(r.getLong(1),r.getString(2),r.getString(3)))) {
                try { acesso.docente(curso,d.id()); lista.add(d); } catch(IllegalArgumentException e) { /* fora do escopo */ }
            }
            return List.copyOf(lista);
        });
    }
    @Override public Meta registrarMeta(NovaMeta pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=acesso.docente(pedido.curso(),pedido.professor());
            if(q(c,"SELECT id_professor FROM professor WHERE id_professor=? AND situacao='ATIVO' FOR SHARE",r->r.getLong(1),pedido.professor()).isEmpty())
                throw new IllegalArgumentException("Selecione um docente ativo.");
            if(pedido.periodo()!=null && q(c,"SELECT id_periodo FROM periodo_letivo WHERE id_periodo=? FOR SHARE",r->r.getLong(1),pedido.periodo()).isEmpty())
                throw new IllegalArgumentException("Período não encontrado.");
            long id=g(c,"""
                    INSERT INTO prazo_meta_docente(id_professor,id_periodo,descricao,prazo,situacao,id_curso,tipo,
                        id_usuario_autor,registrado_em_utc) VALUES (?,?,?,?,'ABERTA',?,?,?,?)
                    """,pedido.professor(),pedido.periodo(),pedido.descricao(),pedido.prazo(),pedido.curso(),pedido.tipo().name(),autor,agoraUtc(c));
            var meta=carregarMeta(id); historico(meta,null,autor,"Registro de "+pedido.tipo());
            auditarTecnico("PRAZO_META_DOCENTE",id,autor,"REGISTRAR"); return meta;
        });
    }
    @Override public Meta atualizarMeta(AtualizarMeta pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); SegurancaJdbc.exigirSessao(c,sessao); var anterior=carregarMeta(pedido.id());
            if(anterior.curso()==0 || anterior.autor()==null) throw new IllegalArgumentException("Meta legada sem escopo/autoria. Preserve o registro e faça conferência específica.");
            long autor=acesso.docente(anterior.curso(),anterior.professor());
            if(anterior.versao()!=pedido.versao()) throw new IllegalArgumentException("Meta alterada. Consulte a versão atual antes de confirmar.");
            if(!Set.of("ABERTA","EM_ANDAMENTO").contains(anterior.situacao())) throw new IllegalArgumentException("A meta já foi concluída ou cancelada. Seu histórico permanece preservado.");
            g(c,"UPDATE prazo_meta_docente SET descricao=?,prazo=?,situacao=?,versao=versao+1,id_usuario_autor=?,atualizado_em_utc=? WHERE id_prazo_meta=?",
                    pedido.descricao(),pedido.prazo(),pedido.situacao().name(),autor,agoraUtc(c),pedido.id());
            var atual=carregarMeta(pedido.id()); historico(atual,anterior.toString(),autor,pedido.motivo());
            auditarTecnico("PRAZO_META_DOCENTE",atual.id(),autor,"ATUALIZAR"); return atual;
        });
    }
    @Override public List<Meta> metas(long curso,Long professor) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); acesso.curso(curso); if(professor!=null) acesso.docente(curso,professor);
            return q(c,"SELECT * FROM prazo_meta_docente WHERE id_curso=? AND (? IS NULL OR id_professor=?) ORDER BY prazo,id_prazo_meta FOR SHARE",
                    RepositorioGestaoAcademicaJdbc::lerMeta,curso,professor,professor);
        });
    }
    @Override public List<HistoricoMeta> historicoMeta(long meta) {
        HorarioAula.validarId(meta);
        return TransacaoJdbc.executar(c,()->{
            controle(c); SegurancaJdbc.exigirSessao(c,sessao); var atual=carregarMeta(meta); acesso.curso(atual.curso());
            return q(c,"SELECT * FROM historico_prazo_meta WHERE id_prazo_meta=? ORDER BY versao FOR SHARE",r->new HistoricoMeta(r.getLong("id_historico"),r.getInt("versao"),
                    r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class),r.getString("anterior"),r.getString("posterior"),r.getString("motivo")),meta);
        });
    }
    private void historico(Meta atual,String anterior,long autor,String motivo) throws SQLException {
        g(c,"INSERT INTO historico_prazo_meta(id_prazo_meta,versao,id_usuario_autor,registrado_em_utc,anterior,posterior,motivo) VALUES (?,?,?,?,?,?,?)",
                atual.id(),atual.versao(),autor,agoraUtc(c),anterior,atual.toString(),motivo);
    }
    private Meta carregarMeta(long id) throws SQLException {
        var metas=q(c,"SELECT * FROM prazo_meta_docente WHERE id_prazo_meta=? FOR UPDATE",RepositorioGestaoAcademicaJdbc::lerMeta,id);
        if(metas.isEmpty()) throw new IllegalArgumentException("Meta não encontrada."); return metas.getFirst();
    }
    private static Meta lerMeta(ResultSet r) throws SQLException {
        var data=r.getObject("atualizado_em_utc",LocalDateTime.class);
        if(data==null) data=r.getObject("registrado_em_utc",LocalDateTime.class);
        return new Meta(r.getLong("id_prazo_meta"),r.getLong("id_curso"),r.getLong("id_professor"),r.getObject("id_periodo",Long.class),r.getString("tipo"),
                r.getString("descricao"),r.getObject("prazo",LocalDate.class),r.getString("situacao"),r.getInt("versao"),r.getObject("id_usuario_autor",Long.class),data);
    }
    private void filtro(Filtro f) throws SQLException {
        Objects.requireNonNull(f); acesso.curso(f.curso()); if(f.turma()!=null) acesso.turma(f.curso(),f.turma());
    }
    @Override public List<AvaliacaoGestao> avaliacoes(Filtro f) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); filtro(f);
            return q(c,"SELECT a.*,t.id_disciplina,t.id_professor,t.id_periodo FROM avaliacao a JOIN turma t ON t.id_turma=a.id_turma WHERE "+AcessoGestaoAcademicaJdbc.TURMA_NO_CURSO+"""
                     AND (? IS NULL OR t.id_periodo=?) AND (? IS NULL OR t.id_disciplina=?)
                     AND (? IS NULL OR t.id_turma=?) ORDER BY a.data,a.id_avaliacao FOR SHARE
                    """,r->new AvaliacaoGestao(r.getLong("id_turma"),r.getLong("id_disciplina"),r.getLong("id_professor"),r.getLong("id_periodo"),lerAvaliacao(r)),
                    f.curso(),f.curso(),f.periodo(),f.periodo(),f.disciplina(),f.disciplina(),f.turma(),f.turma());
        });
    }
    @Override public Diario diario(long curso,long turma) {
        return TransacaoJdbc.executar(c,()->{ controle(c); acesso.turma(curso,turma); return diarioInterno(curso,turma); });
    }
    private Diario diarioInterno(long curso,long turma) throws SQLException {
        var aulas=q(c,"SELECT * FROM aula WHERE id_turma=? ORDER BY data,tempo_letivo,id_aula FOR SHARE",r->new Aula(r.getLong("id_aula"),turma,r.getObject("data",LocalDate.class),
                r.getString("tempo_letivo"),r.getString("pauta"),r.getString("conteudo")),turma);
        var avaliacoes=q(c,"SELECT * FROM avaliacao WHERE id_turma=? ORDER BY data,id_avaliacao FOR SHARE",RepositorioGestaoAcademicaJdbc::lerAvaliacao,turma);
        var alunos=q(c,"""
                SELECT i.id_inscricao,i.id_matricula,i.situacao,a.identificacao FROM inscricao_disciplina i
                JOIN matricula_academica m ON m.id_matricula=i.id_matricula JOIN aluno a ON a.id_aluno=m.id_aluno
                WHERE i.id_turma=? AND m.id_curso=? ORDER BY i.id_inscricao FOR SHARE
                """,r->{
                    long inscricao=r.getLong("id_inscricao");
                    var vigente=q(c,"SELECT * FROM resultado_inscricao WHERE id_inscricao=? FOR SHARE",x->new ResultadoVigente(x.getBigDecimal("p1"),x.getBigDecimal("p2"),x.getBigDecimal("pf"),
                            x.getBigDecimal("media_inicial"),x.getBigDecimal("media_final"),x.getLong("total_minutos"),x.getLong("presentes_minutos"),x.getString("situacao"),x.getString("motivo")),inscricao);
                    return new AlunoDiario(r.getLong("id_matricula"),inscricao,r.getString("identificacao"),r.getString("situacao"),
                            new RepositorioDiarioAcademicoJdbc(c,sessao).consultarGestao(curso,inscricao),vigente.isEmpty()?null:vigente.getFirst());
                },turma,curso);
        return new Diario(curso,turma,aulas,avaliacoes,alunos,hash(curso+"\n"+turma+"\n"+aulas+"\n"+avaliacoes+"\n"+alunos));
    }
    @Override public Auditoria auditar(NovaAuditoria pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=acesso.turma(pedido.curso(),pedido.turma()); var dados=diarioInterno(pedido.curso(),pedido.turma());
            if(!dados.assinatura().equals(pedido.assinatura())) throw new IllegalArgumentException("O diário mudou desde a consulta. Confira os dados atuais antes de concluir a auditoria.");
            var diarios=q(c,"SELECT id_diario FROM diario_classe WHERE id_turma=? FOR UPDATE",r->r.getLong(1),pedido.turma());
            long diario=diarios.isEmpty()?g(c,"INSERT INTO diario_classe(id_turma,situacao) VALUES (?,'EM_ACOMPANHAMENTO')",pedido.turma()):diarios.getFirst();
            long id=g(c,"INSERT INTO auditoria_diario(id_diario,id_usuario_responsavel,data_hora,observacoes,id_curso,conclusao,assinatura,dados_avaliados) VALUES (?,?,?,?,?,?,?,?)",
                    diario,autor,agoraUtc(c),pedido.observacoes(),pedido.curso(),pedido.conclusao().name(),pedido.assinatura(),dados.toString());
            auditarTecnico("AUDITORIA_DIARIO",id,autor,"REGISTRAR");
            return auditoriasInterno(pedido.curso(),pedido.turma()).stream().filter(a->a.id()==id).findFirst().orElseThrow();
        });
    }
    @Override public List<Auditoria> auditorias(long curso,long turma) {
        return TransacaoJdbc.executar(c,()->{ controle(c); acesso.turma(curso,turma); return auditoriasInterno(curso,turma); });
    }
    private List<Auditoria> auditoriasInterno(long curso,long turma) throws SQLException {
        return q(c,"SELECT a.* FROM auditoria_diario a JOIN diario_classe d ON d.id_diario=a.id_diario WHERE d.id_turma=? AND a.id_curso=? ORDER BY a.id_auditoria_diario FOR SHARE",
                r->new Auditoria(r.getLong("id_auditoria_diario"),r.getLong("id_diario"),r.getObject("id_curso",Long.class),r.getLong("id_usuario_responsavel"),
                        r.getObject("data_hora",LocalDateTime.class),r.getString("conclusao"),r.getString("observacoes"),r.getString("assinatura"),r.getString("dados_avaliados")),turma,curso);
    }
    @Override public List<Monitoramento> monitorar(Filtro f) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); filtro(f);
            var matriculas=q(c,"""
                    SELECT m.id_matricula,m.id_aluno,m.situacao,a.identificacao FROM matricula_academica m JOIN aluno a ON a.id_aluno=m.id_aluno
                    WHERE m.id_curso=? AND (? IS NULL OR m.id_aluno=?) AND (
                      (? IS NULL AND ? IS NULL AND ? IS NULL) OR EXISTS (
                        SELECT 1 FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma WHERE i.id_matricula=m.id_matricula
                        AND (? IS NULL OR t.id_periodo=?) AND (? IS NULL OR t.id_disciplina=?) AND (? IS NULL OR t.id_turma=?)))
                    ORDER BY m.id_matricula FOR SHARE
                    """,r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getString(4)},f.curso(),f.aluno(),f.aluno(),f.periodo(),f.disciplina(),f.turma(),
                    f.periodo(),f.periodo(),f.disciplina(),f.disciplina(),f.turma(),f.turma());
            var linhas=new ArrayList<Monitoramento>();
            for(var m:matriculas) {
                long matricula=(long)m[0];
                try {
                    if(!q(c,"SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND situacao='DEFERIDO' AND (conclusao_origem>? OR decidido_em IS NULL OR decidido_em>CURRENT_TIMESTAMP(6)) FOR SHARE",
                            r->r.getLong(1),matricula,dataAcademica(agoraUtc(c))).isEmpty()) throw new IllegalArgumentException("Reconhecimento sem data de conclusão/decisão válida.");
                    var base=new RepositorioEquivalenciaJdbc(c).plano(matricula);
                    var plano=new RepositorioPlanoInstitucionalJdbc(c,sessao).atualGestao(f.curso(),matricula);
                    int cumpridos=(int)base.itens().stream().filter(i->Set.of("CONCLUIDA","RECONHECIDA").contains(i.situacao())).count();
                    linhas.add(new Monitoramento(matricula,(long)m[1],(String)m[3],(String)m[2],base.itens().size(),cumpridos,
                            (int)base.itens().stream().filter(i->"RECONHECIDA".equals(i.situacao())).count(),
                            (int)base.itens().stream().filter(i->"SOB_REVISAO".equals(i.situacao())).count(),
                            (int)base.itens().stream().filter(i->"EM_ANDAMENTO".equals(i.situacao())).count(),
                            plano.isEmpty()?"SEM_PLANO":plano.orElseThrow().atualidade().name(),plano.isEmpty()?"Não há plano institucional registrado.":plano.orElseThrow().aviso()));
                } catch(IllegalArgumentException e) {
                    linhas.add(new Monitoramento(matricula,(long)m[1],(String)m[3],(String)m[2],null,null,null,null,null,"DADOS_INSUFICIENTES",e.getMessage()));
                }
            }
            return List.copyOf(linhas);
        });
    }
    private static Avaliacao lerAvaliacao(ResultSet r) throws SQLException {
        return new Avaliacao(r.getLong("id_avaliacao"),r.getLong("id_turma"),r.getString("tipo_prova"),r.getObject("data",LocalDate.class),r.getString("descricao"));
    }
    private void auditarTecnico(String entidade,long id,long autor,String acao) throws SQLException {
        g(c,"INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes) VALUES (?,'ACADEMICO',?,?,?,?,?)",
                autor,entidade,id,acao,agoraUtc(c),"Operação de gestão acadêmica no escopo autorizado; dados específicos preservados no registro.");
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.RepositorioAtendimentoAluno;
import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Todas as consultas derivam o aluno da sessão; nenhum ID de aluno é recebido do cliente. */
public final class RepositorioAtendimentoAlunoJdbc implements RepositorioAtendimentoAluno {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    public RepositorioAtendimentoAlunoJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao;
    }
    private record Identidade(long usuario,long aluno,String nome) { }
    @Override public void verificarAcesso() {
        TransacaoJdbc.executar(c,()->{ controle(c); identidade(); return null; });
    }
    private Identidade identidade() throws SQLException {
        long usuario=SegurancaJdbc.exigirSessao(c,sessao);
        var alunos=q(c,"SELECT id_aluno,identificacao FROM aluno WHERE id_usuario=? FOR SHARE",
                r->new Identidade(usuario,r.getLong(1),r.getString(2)),usuario);
        if(alunos.size()!=1) throw new IllegalArgumentException("A conta deve estar vinculada a um cadastro de aluno.");
        return alunos.getFirst();
    }
    @Override public Historico historico() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); var aluno=identidade();
            var agora=agoraUtc(c); var hoje=dataAcademica(agora);
            // V008 armazena decidido_em no relógio da sessão SQL, como seus consumidores legados.
            var agoraDecisao=q(c,"SELECT CURRENT_TIMESTAMP(6)",r->r.getObject(1,LocalDateTime.class)).getFirst();
            var matriculas=q(c,"""
                    SELECT m.id_matricula,c.nome,m.situacao,m.data_ingresso
                    FROM matricula_academica m JOIN curso c ON c.id_curso=m.id_curso
                    WHERE m.id_aluno=? ORDER BY m.id_matricula FOR SHARE
                    """,r->new Vinculo(r.getLong(1),r.getString(2),r.getString(3),r.getObject(4,LocalDate.class)),aluno.aluno());
            var disciplinas=q(c,"""
                    SELECT m.id_matricula,c.nome AS curso,p.id_periodo,p.descricao,p.data_inicio,p.data_fim,
                           d.id_disciplina,d.codigo,d.nome AS disciplina,d.carga_horaria,i.id_inscricao,t.codigo AS turma,
                           i.situacao AS inscricao,r.situacao AS resultado,r.media_inicial,r.media_final,
                           r.total_minutos,r.presentes_minutos
                    FROM matricula_academica m JOIN curso c ON c.id_curso=m.id_curso
                    JOIN inscricao_disciplina i ON i.id_matricula=m.id_matricula
                    JOIN turma t ON t.id_turma=i.id_turma JOIN disciplina d ON d.id_disciplina=t.id_disciplina
                    JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                    LEFT JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao
                    WHERE m.id_aluno=? ORDER BY m.id_matricula,p.data_inicio,p.id_periodo,d.nome,i.id_inscricao FOR SHARE
                    """,r->new DisciplinaCursada(r.getLong("id_matricula"),r.getString("curso"),r.getLong("id_periodo"),
                            r.getString("descricao"),r.getObject("data_inicio",LocalDate.class),r.getObject("data_fim",LocalDate.class),
                            r.getLong("id_disciplina"),r.getString("codigo"),r.getString("disciplina"),r.getInt("carga_horaria"),
                            r.getLong("id_inscricao"),r.getString("turma"),r.getString("inscricao"),r.getString("resultado"),
                            r.getBigDecimal("media_inicial"),r.getBigDecimal("media_final"),
                            r.getObject("total_minutos",Long.class),r.getObject("presentes_minutos",Long.class)),aluno.aluno());
            var reconhecimentos=q(c,"""
                    SELECT p.*,c.nome AS curso,d.nome AS disciplina,v.id_estrutura AS estrutura_atual
                    FROM pedido_equivalencia p JOIN matricula_academica m ON m.id_matricula=p.id_matricula
                    JOIN curso c ON c.id_curso=m.id_curso JOIN disciplina d ON d.id_disciplina=p.id_disciplina_destino
                    LEFT JOIN vinculo_curricular_matricula v ON v.id_matricula=m.id_matricula
                    WHERE m.id_aluno=? ORDER BY m.id_matricula,p.id_pedido FOR SHARE
                    """,r->{
                        long id=r.getLong("id_pedido"); String decisao=r.getString("situacao");
                        String revisao=RepositorioProcessoAcademicoJdbc.estado(c,id);
                        LocalDate conclusao=r.getObject("conclusao_origem",LocalDate.class);
                        LocalDateTime decidido=r.getObject("decidido_em",LocalDateTime.class);
                        Long estrutura=r.getObject("estrutura_atual",Long.class);
                        boolean vigente="DEFERIDO".equals(decisao) && "VIGENTE".equals(revisao)
                                && !conclusao.isAfter(hoje) && decidido!=null && !decidido.isAfter(agoraDecisao)
                                && estrutura!=null && estrutura==r.getLong("id_estrutura");
                        return new Reconhecimento(r.getLong("id_matricula"),r.getString("curso"),id,
                                r.getLong("id_disciplina_destino"),r.getString("disciplina"),r.getInt("carga_horaria_destino"),
                                r.getString("instituicao_origem"),r.getString("disciplina_origem"),conclusao,decisao,revisao,vigente,decidido);
                    },aluno.aluno());
            var legados=q(c,"""
                    SELECT m.id_matricula,c.nome,a.id_aproveitamento,d.nome AS disciplina,a.decisao
                    FROM aproveitamento_equivalencia a JOIN matricula_academica m ON m.id_matricula=a.id_matricula
                    JOIN curso c ON c.id_curso=m.id_curso JOIN disciplina d ON d.id_disciplina=a.id_disciplina_destino
                    WHERE m.id_aluno=? ORDER BY m.id_matricula,a.id_aproveitamento FOR SHARE
                    """,r->new Legado(r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getString(5)),aluno.aluno());
            return new Historico(aluno.aluno(),aluno.nome(),matriculas,disciplinas,reconhecimentos,legados);
        });
    }
    @Override public Manifestacao registrar(NovaManifestacao pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); var aluno=identidade(); var agora=agoraUtc(c);
            long id=g(c,"""
                    INSERT INTO manifestacao_aluno(id_aluno,id_usuario_autor,tipo_manifestacao,conteudo,data_hora)
                    VALUES (?,?,?,?,?)
                    """,aluno.aluno(),aluno.usuario(),pedido.tipo().name(),pedido.conteudo(),agora);
            g(c,"""
                    INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes)
                    VALUES (?,'ACADEMICO','MANIFESTACAO_ALUNO',?,'REGISTRAR',?,?)
                    """,aluno.usuario(),id,agora,"Manifestação registrada; tipo "+pedido.tipo()+". Conteúdo preservado no registro privado.");
            return new Manifestacao(id,aluno.aluno(),aluno.usuario(),pedido.tipo().name(),pedido.conteudo(),agora);
        });
    }
    @Override public List<Manifestacao> manifestacoes() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); var aluno=identidade();
            return q(c,"""
                    SELECT * FROM manifestacao_aluno WHERE id_aluno=?
                    ORDER BY data_hora,id_manifestacao FOR SHARE
                    """,RepositorioAtendimentoAlunoJdbc::ler,aluno.aluno());
        });
    }
    private static Manifestacao ler(ResultSet r) throws SQLException {
        return new Manifestacao(r.getLong("id_manifestacao"),r.getLong("id_aluno"),r.getObject("id_usuario_autor",Long.class),
                r.getString("tipo_manifestacao"),r.getString("conteudo"),r.getObject("data_hora",LocalDateTime.class));
    }
}

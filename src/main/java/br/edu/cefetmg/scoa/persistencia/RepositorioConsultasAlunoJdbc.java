package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.ConsultasAluno;
import br.edu.cefetmg.scoa.dominio.FaixaHorario;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.q;

public final class RepositorioConsultasAlunoJdbc implements ConsultasAluno {
    private static final String TURMA = """
            SELECT m.id_matricula, c.nome AS curso, p.id_periodo, p.descricao AS periodo,
                   d.nome AS disciplina, t.codigo AS turma,
            """;
    private static final String INSCRICOES = """
            FROM matricula_academica m
            JOIN curso c ON c.id_curso = m.id_curso
            JOIN inscricao_disciplina i ON i.id_matricula = m.id_matricula
            JOIN turma t ON t.id_turma = i.id_turma
            JOIN disciplina d ON d.id_disciplina = t.id_disciplina
            JOIN periodo_letivo p ON p.id_periodo = t.id_periodo
            """;

    private final Connection conexao;
    private final SegurancaJdbc.Sessao sessao;

    public RepositorioConsultasAlunoJdbc(Connection conexao, SegurancaJdbc.Sessao sessao) {
        this.conexao = Objects.requireNonNull(conexao);
        this.sessao = sessao;
    }

    @Override
    public List<Horario> horarios() {
        return TransacaoJdbc.executar(conexao, () -> q(conexao, TURMA + """
                h.id_horario, h.dia_semana, h.hora_inicio, h.hora_fim, s.identificacao AS sala
                """ + INSCRICOES + """
                LEFT JOIN horario_aula h ON h.id_turma = t.id_turma AND h.ativo=TRUE
                LEFT JOIN sala s ON s.id_sala = h.id_sala
                WHERE m.id_aluno = ? AND i.situacao = 'ATIVO'
                ORDER BY p.data_inicio, p.id_periodo, h.dia_semana, h.hora_inicio,
                         d.nome, i.id_inscricao, h.id_horario
                """, r -> new Horario(turma(r), faixa(r), r.getString("sala")), alunoAutenticado()));
    }

    @Override
    public List<ResultadoFinal> resultadosFinais() {
        return TransacaoJdbc.executar(conexao, () -> q(conexao, TURMA + """
                r.p1, r.p2, r.pf, r.media_inicial, r.media_final,
                r.presentes_minutos, r.total_minutos, r.situacao
                """ + INSCRICOES + """
                JOIN resultado_inscricao r ON r.id_inscricao = i.id_inscricao
                WHERE m.id_aluno = ? AND i.situacao = r.situacao
                ORDER BY p.data_inicio DESC, p.id_periodo, d.nome, i.id_inscricao
                """, r -> new ResultadoFinal(turma(r), r.getBigDecimal("p1"), r.getBigDecimal("p2"),
                        r.getBigDecimal("pf"), r.getBigDecimal("media_inicial"), r.getBigDecimal("media_final"),
                        r.getLong("presentes_minutos"), r.getLong("total_minutos"), r.getString("situacao")),
                alunoAutenticado()));
    }

    private long alunoAutenticado() throws SQLException {
        long usuario = SegurancaJdbc.exigirSessao(conexao, sessao);
        var alunos = q(conexao, "SELECT id_aluno FROM aluno WHERE id_usuario = ? FOR SHARE",
                r -> r.getLong(1), usuario);
        if (alunos.isEmpty()) throw new IllegalArgumentException("A conta autenticada não está vinculada a um aluno.");
        return alunos.getFirst();
    }

    private static TurmaInscrita turma(ResultSet r) throws SQLException {
        return new TurmaInscrita(r.getLong("id_matricula"), r.getString("curso"), r.getLong("id_periodo"),
                r.getString("periodo"), r.getString("disciplina"), r.getString("turma"));
    }

    private static FaixaHorario faixa(ResultSet r) throws SQLException {
        if (r.getObject("id_horario") == null) return null;
        return new FaixaHorario(r.getInt("dia_semana"), r.getObject("hora_inicio", LocalTime.class),
                r.getObject("hora_fim", LocalTime.class));
    }
}

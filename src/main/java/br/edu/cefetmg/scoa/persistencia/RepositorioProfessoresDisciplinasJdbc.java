package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import java.sql.*;
import java.util.*;
import java.util.function.UnaryOperator;

public final class RepositorioProfessoresDisciplinasJdbc implements RepositorioProfessoresDisciplinas {
    private final Connection conexao;
    public RepositorioProfessoresDisciplinasJdbc(Connection conexao) { this.conexao = Objects.requireNonNull(conexao); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    private <T> List<T> consultar(String sql, Leitor<T> leitor, Object... valores) {
        try (var s = conexao.prepareStatement(sql)) {
            preencher(s, valores);
            try (var r = s.executeQuery()) {
                List<T> lista = new ArrayList<>();
                while (r.next()) lista.add(leitor.ler(r));
                return List.copyOf(lista);
            }
        } catch (SQLException e) { throw new PersistenciaException("Não foi possível consultar professores e disciplinas.", e); }
    }
    private static void preencher(PreparedStatement s, Object... valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) s.setObject(i + 1, valores[i]);
    }
    private long inserir(String sql, Object... valores) throws SQLException {
        try (var s = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(s, valores); s.executeUpdate();
            try (var r = s.getGeneratedKeys()) {
                if (!r.next()) throw new SQLException("Identificador não retornado pelo banco.");
                return r.getLong(1);
            }
        }
    }
    private int executar(String sql, Object... valores) throws SQLException {
        try (var s = conexao.prepareStatement(sql)) { preencher(s, valores); return s.executeUpdate(); }
    }
    private static Professor lerProfessor(ResultSet r) throws SQLException {
        long usuario = r.getLong("id_usuario"); Long idUsuario = r.wasNull() ? null : usuario;
        return new Professor(r.getLong("id_professor"), idUsuario, r.getString("identificacao"), r.getString("situacao"));
    }
    private static Disciplina lerDisciplina(ResultSet r) throws SQLException {
        return new Disciplina(r.getLong("id_disciplina"), r.getString("codigo"), r.getString("nome"), r.getInt("carga_horaria"), r.getString("situacao"));
    }
    @Override public Professor inserirProfessor(Professor p) {
        Objects.requireNonNull(p);
        if (p.id() != null || p.idUsuario() != null) throw new IllegalArgumentException("Professor novo não deve ter ID ou conta vinculada neste cadastro.");
        return TransacaoJdbc.executar(conexao, () -> {
            long id = inserir("INSERT INTO professor (identificacao,situacao) VALUES (?,?)", p.identificacao(), p.situacao());
            return new Professor(id, null, p.identificacao(), p.situacao());
        });
    }
    @Override public Optional<Professor> buscarProfessor(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM professor WHERE id_professor=?", RepositorioProfessoresDisciplinasJdbc::lerProfessor, id).stream().findFirst();
    }
    @Override public List<Professor> listarProfessores() {
        return consultar("SELECT * FROM professor ORDER BY id_professor", RepositorioProfessoresDisciplinasJdbc::lerProfessor);
    }
    @Override public Professor alterarProfessor(long id, UnaryOperator<Professor> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao, () -> {
            var atual = consultar("SELECT * FROM professor WHERE id_professor=? FOR UPDATE", RepositorioProfessoresDisciplinasJdbc::lerProfessor, id)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));
            var novo = Objects.requireNonNull(alteracao.apply(atual));
            if (!Objects.equals(novo.id(), id) || !Objects.equals(atual.idUsuario(), novo.idUsuario()))
                throw new IllegalArgumentException("Preserve o identificador e a conta vinculada ao professor.");
            if (!atual.situacao().equals(novo.situacao()) && !"ATIVO".equals(novo.situacao())) {
                var compromissos = consultar("SELECT t.id_turma FROM turma t JOIN periodo_letivo p ON p.id_periodo=t.id_periodo "
                        + "WHERE t.id_professor=? AND t.situacao='ATIVO' AND p.data_fim>=CURRENT_DATE LIMIT 1 FOR UPDATE", r -> r.getLong(1), id);
                if (!compromissos.isEmpty()) throw new IllegalArgumentException("Professor possui turma ativa em período atual ou futuro. Reorganize as turmas antes de inativá-lo.");
            }
            executar("UPDATE professor SET identificacao=?,situacao=? WHERE id_professor=?", novo.identificacao(), novo.situacao(), id);
            return novo;
        });
    }
    @Override public Disciplina inserirDisciplina(Disciplina d) {
        Objects.requireNonNull(d);
        if (d.id() != null) throw new IllegalArgumentException("Disciplina nova não deve ter ID.");
        return TransacaoJdbc.executar(conexao, () -> {
            long id = inserir("INSERT INTO disciplina (codigo,nome,carga_horaria,situacao) VALUES (?,?,?,?)", d.codigo(), d.nome(), d.cargaHoraria(), d.situacao());
            return new Disciplina(id, d.codigo(), d.nome(), d.cargaHoraria(), d.situacao());
        });
    }
    @Override public Optional<Disciplina> buscarDisciplina(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM disciplina WHERE id_disciplina=?", RepositorioProfessoresDisciplinasJdbc::lerDisciplina, id).stream().findFirst();
    }
    @Override public List<Disciplina> listarDisciplinas() {
        return consultar("SELECT * FROM disciplina ORDER BY id_disciplina", RepositorioProfessoresDisciplinasJdbc::lerDisciplina);
    }
    @Override public Disciplina alterarDisciplina(long id, UnaryOperator<Disciplina> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao, () -> {
            var atual = consultar("SELECT * FROM disciplina WHERE id_disciplina=? FOR UPDATE", RepositorioProfessoresDisciplinasJdbc::lerDisciplina, id)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada."));
            var nova = Objects.requireNonNull(alteracao.apply(atual));
            if (!Objects.equals(nova.id(), id) || !atual.situacao().equals(nova.situacao()))
                throw new IllegalArgumentException("Preserve o identificador e a situação da disciplina.");
            executar("UPDATE disciplina SET codigo=?,nome=?,carga_horaria=? WHERE id_disciplina=?", nova.codigo(), nova.nome(), nova.cargaHoraria(), id);
            return nova;
        });
    }
    @Override public List<Disciplina> listarPreRequisitos(long disciplina) {
        buscarDisciplina(disciplina).orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada."));
        return consultar("SELECT d.* FROM disciplina_prerequisito p JOIN disciplina d ON d.id_disciplina=p.id_disciplina_prerequisito "
                + "WHERE p.id_disciplina=? ORDER BY d.id_disciplina", RepositorioProfessoresDisciplinasJdbc::lerDisciplina, disciplina);
    }
    /** Bloqueios em ordem estável, mantidos até o commit inclusive do chamador.
     * Todas as escritas do grafo devem passar por este protocolo. Não usa snapshot antigo para validar ciclos. */
    private Map<Long, Set<Long>> bloquearGrafo(long disciplina, long requisito) {
        HorarioAula.validarId(disciplina); HorarioAula.validarId(requisito);
        if (disciplina == requisito) throw new IllegalArgumentException("Uma disciplina não pode ser pré-requisito de si mesma.");
        var ids = consultar("SELECT id_disciplina FROM disciplina ORDER BY id_disciplina FOR UPDATE", r -> r.getLong(1));
        if (!ids.contains(disciplina) || !ids.contains(requisito)) throw new IllegalArgumentException("Disciplina ou pré-requisito não encontrado.");
        Map<Long, Set<Long>> grafo = new HashMap<>();
        for (var aresta : consultar("SELECT id_disciplina,id_disciplina_prerequisito FROM disciplina_prerequisito "
                + "ORDER BY id_disciplina,id_disciplina_prerequisito FOR UPDATE", r -> new long[]{r.getLong(1), r.getLong(2)})) {
            grafo.computeIfAbsent(aresta[0], k -> new HashSet<>()).add(aresta[1]);
        }
        return grafo;
    }
    @Override public void adicionarPreRequisito(long disciplina, long requisito) {
        TransacaoJdbc.executar(conexao, () -> {
            var grafo = bloquearGrafo(disciplina, requisito);
            if (grafo.getOrDefault(disciplina, Set.of()).contains(requisito))
                throw new IllegalArgumentException("Este pré-requisito já está registrado.");
            Set<Long> visitados = new HashSet<>(); Deque<Long> fila = new ArrayDeque<>(); fila.add(requisito);
            while (!fila.isEmpty()) {
                long atual = fila.removeFirst();
                if (atual == disciplina) throw new IllegalArgumentException("O pré-requisito formaria um ciclo entre disciplinas.");
                if (visitados.add(atual)) fila.addAll(grafo.getOrDefault(atual, Set.of()));
            }
            executar("INSERT INTO disciplina_prerequisito (id_disciplina,id_disciplina_prerequisito) VALUES (?,?)", disciplina, requisito);
            return null;
        });
    }
    @Override public void removerPreRequisito(long disciplina, long requisito) {
        TransacaoJdbc.executar(conexao, () -> {
            var grafo = bloquearGrafo(disciplina, requisito);
            if (!grafo.getOrDefault(disciplina, Set.of()).contains(requisito))
                throw new IllegalArgumentException("Pré-requisito não encontrado para esta disciplina.");
            executar("DELETE FROM disciplina_prerequisito WHERE id_disciplina=? AND id_disciplina_prerequisito=?", disciplina, requisito);
            return null;
        });
    }
}

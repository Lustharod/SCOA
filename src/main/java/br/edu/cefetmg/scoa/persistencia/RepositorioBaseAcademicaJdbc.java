package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import java.sql.*;
import java.util.*;
import java.util.function.UnaryOperator;

public final class RepositorioBaseAcademicaJdbc implements RepositorioBaseAcademica {
    private final Connection conexao;
    public RepositorioBaseAcademicaJdbc(Connection conexao) { this.conexao = Objects.requireNonNull(conexao); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    private static void preencher(PreparedStatement s, Object... valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) s.setObject(i + 1, valores[i]);
    }
    private <T> List<T> consultar(String sql, Leitor<T> leitor, Object... valores) {
        try (var s = conexao.prepareStatement(sql)) {
            preencher(s, valores);
            try (var r = s.executeQuery()) {
                List<T> lista = new ArrayList<>();
                while (r.next()) lista.add(leitor.ler(r));
                return List.copyOf(lista);
            }
        } catch (SQLException e) { throw new PersistenciaException("Não foi possível consultar a base acadêmica.", e); }
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
    private static Aluno aluno(ResultSet r) throws SQLException {
        long usuario = r.getLong("id_usuario"); Long conta = r.wasNull() ? null : usuario;
        return new Aluno(r.getLong("id_aluno"), conta, r.getString("identificacao"), r.getString("situacao"));
    }
    private static Curso curso(ResultSet r) throws SQLException {
        return new Curso(r.getLong("id_curso"), r.getString("nome"), r.getString("situacao"));
    }
    private static EstruturaCurricular estrutura(ResultSet r) throws SQLException {
        return new EstruturaCurricular(r.getLong("id_estrutura"), r.getLong("id_curso"), r.getString("identificacao"), r.getString("situacao"));
    }
    private static Disciplina disciplina(ResultSet r) throws SQLException {
        return new Disciplina(r.getLong("id_disciplina"), r.getString("codigo"), r.getString("nome"), r.getInt("carga_horaria"), r.getString("situacao"));
    }
    @Override public Aluno inserirAluno(Aluno a) {
        Objects.requireNonNull(a);
        if (a.id() != null || a.idUsuario() != null || !"ATIVO".equals(a.situacao()))
            throw new IllegalArgumentException("Aluno novo deve ser ativo, sem ID ou conta vinculada neste cadastro.");
        return TransacaoJdbc.executar(conexao, () -> new Aluno(
                inserir("INSERT INTO aluno (identificacao,situacao) VALUES (?,?)", a.identificacao(), a.situacao()), null, a.identificacao(), a.situacao()));
    }
    @Override public Optional<Aluno> buscarAluno(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM aluno WHERE id_aluno=?", RepositorioBaseAcademicaJdbc::aluno, id).stream().findFirst();
    }
    @Override public List<Aluno> listarAlunos() {
        return consultar("SELECT * FROM aluno ORDER BY id_aluno", RepositorioBaseAcademicaJdbc::aluno);
    }
    @Override public Aluno alterarAluno(long id, UnaryOperator<Aluno> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao, () -> {
            var atual = consultar("SELECT * FROM aluno WHERE id_aluno=? FOR UPDATE", RepositorioBaseAcademicaJdbc::aluno, id)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado."));
            var novo = Objects.requireNonNull(alteracao.apply(atual));
            if (!Objects.equals(novo.id(), atual.id()) || !Objects.equals(novo.idUsuario(), atual.idUsuario())
                    || (!atual.situacao().equals(novo.situacao()) && !"INATIVO".equals(novo.situacao())))
                throw new IllegalArgumentException("Preserve identidade e conta do aluno; somente a inativação altera a situação.");
            executar("UPDATE aluno SET identificacao=?,situacao=? WHERE id_aluno=?", novo.identificacao(), novo.situacao(), id);
            return novo;
        });
    }
    @Override public Curso inserirCurso(Curso c) {
        Objects.requireNonNull(c);
        if (c.id() != null || !"ATIVO".equals(c.situacao())) throw new IllegalArgumentException("Curso novo deve ser ativo e sem ID.");
        return TransacaoJdbc.executar(conexao, () -> new Curso(
                inserir("INSERT INTO curso (nome,situacao) VALUES (?,?)", c.nome(), c.situacao()), c.nome(), c.situacao()));
    }
    @Override public Optional<Curso> buscarCurso(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM curso WHERE id_curso=?", RepositorioBaseAcademicaJdbc::curso, id).stream().findFirst();
    }
    @Override public List<Curso> listarCursos() { return consultar("SELECT * FROM curso ORDER BY id_curso", RepositorioBaseAcademicaJdbc::curso); }
    @Override public Curso alterarCurso(long id, UnaryOperator<Curso> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao, () -> {
            var atual = consultar("SELECT * FROM curso WHERE id_curso=? FOR UPDATE", RepositorioBaseAcademicaJdbc::curso, id)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
            var novo = Objects.requireNonNull(alteracao.apply(atual));
            if (!Objects.equals(novo.id(), atual.id()) || !novo.situacao().equals(atual.situacao()))
                throw new IllegalArgumentException("Preserve a identidade e a situação do curso.");
            executar("UPDATE curso SET nome=? WHERE id_curso=?", novo.nome(), id);
            return novo;
        });
    }
    @Override public EstruturaCurricular inserirEstrutura(EstruturaCurricular e) {
        Objects.requireNonNull(e);
        if (e.id() != null || !"ATIVO".equals(e.situacao())) throw new IllegalArgumentException("Estrutura nova deve ser ativa e sem ID.");
        return TransacaoJdbc.executar(conexao, () -> {
            exigirCursoAtivo(e.idCurso());
            return new EstruturaCurricular(inserir("INSERT INTO estrutura_curricular (id_curso,identificacao,situacao) VALUES (?,?,?)",
                    e.idCurso(), e.identificacao(), e.situacao()), e.idCurso(), e.identificacao(), e.situacao());
        });
    }
    private void exigirCursoAtivo(long id) {
        var c = consultar("SELECT * FROM curso WHERE id_curso=? FOR UPDATE", RepositorioBaseAcademicaJdbc::curso, id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
        if (!"ATIVO".equals(c.situacao())) throw new IllegalArgumentException("O curso deve estar ativo.");
    }
    private EstruturaCurricular bloquearEstrutura(long id) {
        return consultar("SELECT * FROM estrutura_curricular WHERE id_estrutura=? FOR UPDATE", RepositorioBaseAcademicaJdbc::estrutura, id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Estrutura curricular não encontrada."));
    }
    @Override public Optional<EstruturaCurricular> buscarEstrutura(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM estrutura_curricular WHERE id_estrutura=?", RepositorioBaseAcademicaJdbc::estrutura, id).stream().findFirst();
    }
    @Override public List<EstruturaCurricular> listarEstruturas(long curso) {
        HorarioAula.validarId(curso);
        return consultar("SELECT * FROM estrutura_curricular WHERE id_curso=? ORDER BY id_estrutura", RepositorioBaseAcademicaJdbc::estrutura, curso);
    }
    @Override public EstruturaCurricular alterarEstrutura(long id, UnaryOperator<EstruturaCurricular> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao, () -> {
            var atual = bloquearEstrutura(id);
            var novo = Objects.requireNonNull(alteracao.apply(atual));
            if (!Objects.equals(novo.id(), atual.id()) || novo.idCurso() != atual.idCurso() || !novo.situacao().equals(atual.situacao()))
                throw new IllegalArgumentException("Preserve a identidade, o curso e a situação da estrutura.");
            executar("UPDATE estrutura_curricular SET identificacao=? WHERE id_estrutura=?", novo.identificacao(), id);
            return novo;
        });
    }
    @Override public List<Disciplina> listarComponentes(long estrutura) {
        HorarioAula.validarId(estrutura);
        return consultar("SELECT d.* FROM estrutura_disciplina e JOIN disciplina d ON d.id_disciplina=e.id_disciplina WHERE e.id_estrutura=? ORDER BY d.id_disciplina",
                RepositorioBaseAcademicaJdbc::disciplina, estrutura);
    }
    /** Futuras escritas de planos devem bloquear a estrutura antes de vinculá-la. */
    private void exigirComposicaoEditavel(long id) {
        var e = bloquearEstrutura(id);
        if (!"ATIVO".equals(e.situacao())) throw new IllegalArgumentException("A estrutura deve estar ativa.");
        exigirCursoAtivo(e.idCurso());
        if (!consultar("SELECT id_matricula FROM vinculo_curricular_matricula WHERE id_estrutura=? LIMIT 1 FOR UPDATE", r -> r.getLong(1), id).isEmpty())
            throw new IllegalArgumentException("Estrutura vinculada a matrícula. Preserve a composição; utilize uma nova estrutura.");
        if (!consultar("SELECT id_plano FROM plano_estudo WHERE id_estrutura=? LIMIT 1 FOR UPDATE", r -> r.getLong(1), id).isEmpty())
            throw new IllegalArgumentException("Estrutura vinculada a plano de estudo. Preserve a composição; utilize uma nova estrutura.");
    }
    @Override public void adicionarComponente(long estrutura, long disciplina) {
        HorarioAula.validarId(estrutura); HorarioAula.validarId(disciplina);
        TransacaoJdbc.executar(conexao, () -> {
            exigirComposicaoEditavel(estrutura);
            var d = consultar("SELECT * FROM disciplina WHERE id_disciplina=? FOR UPDATE", RepositorioBaseAcademicaJdbc::disciplina, disciplina)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Disciplina não encontrada."));
            if (!"ATIVO".equals(d.situacao())) throw new IllegalArgumentException("A disciplina deve estar ativa.");
            if (!consultar("SELECT id_disciplina FROM estrutura_disciplina WHERE id_estrutura=? AND id_disciplina=? FOR UPDATE", r -> r.getLong(1), estrutura, disciplina).isEmpty())
                throw new IllegalArgumentException("Disciplina já incluída na estrutura.");
            executar("INSERT INTO estrutura_disciplina (id_estrutura,id_disciplina) VALUES (?,?)", estrutura, disciplina);
            return null;
        });
    }
    @Override public void removerComponente(long estrutura, long disciplina) {
        HorarioAula.validarId(estrutura); HorarioAula.validarId(disciplina);
        TransacaoJdbc.executar(conexao, () -> {
            exigirComposicaoEditavel(estrutura);
            if (executar("DELETE FROM estrutura_disciplina WHERE id_estrutura=? AND id_disciplina=?", estrutura, disciplina) == 0)
                throw new IllegalArgumentException("Componente não encontrado nesta estrutura.");
            return null;
        });
    }
}

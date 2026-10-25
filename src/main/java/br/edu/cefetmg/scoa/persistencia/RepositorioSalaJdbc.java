package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.RepositorioSala;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** A conexão pertence ao chamador, que define a transação e a fecha ao terminar. */
public final class RepositorioSalaJdbc implements RepositorioSala {
    private static final String COLUNAS = "id_sala, identificacao, localizacao, capacidade, situacao";
    private final Connection conexao;

    public RepositorioSalaJdbc(Connection conexao) {
        this.conexao = Objects.requireNonNull(conexao, "Informe uma conexão.");
    }

    @Override
    public Sala salvar(Sala sala) {
        Objects.requireNonNull(sala, "Informe uma sala.");
        if (sala.getId() == null) return inserir(sala);
        throw new IllegalArgumentException("Para alterar uma sala existente, use a operação alterar.");
    }

    @Override
    public Sala alterar(long id, UnaryOperator<Sala> modificacao) {
        Sala.validarId(id);
        Objects.requireNonNull(modificacao, "Informe a alteração.");
        return TransacaoJdbc.executar(conexao, () -> {
            Sala atual = buscarBloqueada(id);
            Sala nova = Objects.requireNonNull(modificacao.apply(atual), "A alteração deve retornar uma sala.");
            if (!Long.valueOf(id).equals(nova.getId())) throw new IllegalArgumentException("A alteração deve preservar o ID da sala.");
            validarCompromissos(nova);
            return atualizar(nova);
        });
    }

    Sala buscarBloqueada(long id) throws SQLException {
        try (var comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM sala WHERE id_sala = ? FOR UPDATE")) {
            comando.setLong(1, id);
            try (var registro = comando.executeQuery()) {
                if (!registro.next()) throw new SalaNaoEncontradaException(id);
                return ler(registro);
            }
        }
    }

    private void validarCompromissos(Sala sala) throws SQLException {
        String sql = "SELECT t.capacidade FROM horario_aula h JOIN turma t ON t.id_turma=h.id_turma "
                + "JOIN periodo_letivo p ON p.id_periodo=t.id_periodo "
                + "WHERE h.id_sala=? AND p.data_fim>=CURRENT_DATE FOR UPDATE";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, sala.getId());
            try (var registros = comando.executeQuery()) {
                while (registros.next()) {
                    if (!"ATIVO".equals(sala.getSituacao()))
                        throw new IllegalArgumentException("Remova as alocações de períodos atuais ou futuros antes de inativar a sala.");
                    if (sala.getCapacidade() < registros.getInt(1))
                        throw new IllegalArgumentException("A capacidade não pode ser inferior à de uma turma já alocada.");
                }
            }
        }
    }

    private Sala atualizar(Sala sala) {
        String sql = "UPDATE sala SET identificacao = ?, localizacao = ?, capacidade = ?, situacao = ? WHERE id_sala = ?";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            preencher(comando, sala);
            comando.setLong(5, sala.getId());
            // A existência foi confirmada e bloqueada. Zero também é sucesso
            // para dados idênticos quando useAffectedRows=true.
            comando.executeUpdate();
            return sala;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível atualizar a sala.", e);
        }
    }

    private Sala inserir(Sala sala) {
        String sql = "INSERT INTO sala (identificacao, localizacao, capacidade, situacao) VALUES (?, ?, ?, ?)";
        try (PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(comando, sala);
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) throw new SQLException("O banco não retornou o identificador da sala.");
                return new Sala(chaves.getLong(1), sala.getIdentificacao(), sala.getLocalizacao(),
                        sala.getCapacidade(), sala.getSituacao());
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível cadastrar a sala.", e);
        }
    }

    @Override
    public Optional<Sala> buscarPorId(long id) {
        Sala.validarId(id);
        try (PreparedStatement comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM sala WHERE id_sala = ?")) {
            comando.setLong(1, id);
            try (ResultSet registros = comando.executeQuery()) {
                return registros.next() ? Optional.of(ler(registros)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível consultar a sala.", e);
        }
    }

    @Override
    public List<Sala> listar() {
        List<Sala> salas = new ArrayList<>();
        try (PreparedStatement comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM sala ORDER BY id_sala");
             ResultSet registros = comando.executeQuery()) {
            while (registros.next()) salas.add(ler(registros));
            return List.copyOf(salas);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível listar as salas.", e);
        }
    }

    private static void preencher(PreparedStatement comando, Sala sala) throws SQLException {
        comando.setString(1, sala.getIdentificacao());
        comando.setString(2, sala.getLocalizacao());
        comando.setInt(3, sala.getCapacidade());
        comando.setString(4, sala.getSituacao());
    }

    private static Sala ler(ResultSet registro) throws SQLException {
        return new Sala(registro.getLong("id_sala"), registro.getString("identificacao"),
                registro.getString("localizacao"), registro.getInt("capacidade"), registro.getString("situacao"));
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.FaixaHorario;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.PeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.RegraAlocacao;
import br.edu.cefetmg.scoa.dominio.RepositorioAlocacao;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.TurmaResumo;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Os bloqueios de sala e turma serializam a verificação e a gravação entre instâncias. */
public final class RepositorioAlocacaoJdbc implements RepositorioAlocacao {
    private static final String TURMA = "SELECT t.id_turma,t.codigo,t.capacidade,p.data_inicio,p.data_fim "
            + "FROM turma t JOIN periodo_letivo p ON p.id_periodo=t.id_periodo";
    private final Connection conexao;
    private final RepositorioSalaJdbc salas;

    public RepositorioAlocacaoJdbc(Connection conexao) {
        this.conexao = Objects.requireNonNull(conexao, "Informe uma conexão.");
        salas = new RepositorioSalaJdbc(conexao);
    }

    @Override public List<TurmaResumo> listarTurmas() {
        List<TurmaResumo> resultado = new ArrayList<>();
        try (var comando = conexao.prepareStatement(TURMA + " ORDER BY t.id_turma"); var registros = comando.executeQuery()) {
            while (registros.next()) resultado.add(lerTurma(registros));
            return List.copyOf(resultado);
        } catch (SQLException erro) { throw new PersistenciaException("Não foi possível listar as turmas.", erro); }
    }

    @Override public TurmaResumo buscarTurma(long idTurma) {
        HorarioAula.validarId(idTurma);
        try { return buscarTurma(idTurma, false); }
        catch (SQLException erro) { throw new PersistenciaException("Não foi possível consultar a turma.", erro); }
    }

    @Override public List<HorarioAula> listarHorarios(long idTurma) {
        HorarioAula.validarId(idTurma);
        List<HorarioAula> resultado = new ArrayList<>();
        try {
            buscarTurma(idTurma, false);
            try (var comando = conexao.prepareStatement("SELECT * FROM horario_aula WHERE id_turma=? ORDER BY dia_semana,hora_inicio,id_horario")) {
                comando.setLong(1, idTurma);
                try (var registros = comando.executeQuery()) {
                    while (registros.next()) resultado.add(lerHorario(registros));
                }
            }
            return List.copyOf(resultado);
        } catch (SQLException erro) { throw new PersistenciaException("Não foi possível consultar os horários.", erro); }
    }

    @Override public List<Sala> listarDisponiveis(long idTurma, FaixaHorario faixa) {
        HorarioAula.validarId(idTurma);
        Objects.requireNonNull(faixa, "Informe o horário.");
        try {
            TurmaResumo turma = buscarTurma(idTurma, false);
            RegraAlocacao.validarDia(turma, faixa);
            List<Sala> disponiveis = new ArrayList<>();
            for (Sala sala : salas.listar()) {
                if ("ATIVO".equals(sala.getSituacao()) && sala.getCapacidade() >= turma.capacidade()
                        && !temConflito(new HorarioAula(null, idTurma, sala.getId(), faixa), turma.periodo(), false))
                    disponiveis.add(sala);
            }
            // Consulta informativa. A disponibilidade é revalidada com bloqueios ao alocar.
            return List.copyOf(disponiveis);
        } catch (SQLException erro) { throw new PersistenciaException("Não foi possível consultar a disponibilidade.", erro); }
    }

    @Override public HorarioAula alocar(HorarioAula horario) {
        Objects.requireNonNull(horario, "Informe a alocação.");
        if (horario.id() != null) throw new IllegalArgumentException("A nova alocação não deve ter ID.");
        return TransacaoJdbc.executar(conexao, () -> {
            // Mesma ordem em todas as inclusões: sala e, depois, turma/período.
            Sala sala = salas.buscarBloqueada(horario.idSala());
            TurmaResumo turma = buscarTurma(horario.idTurma(), true);
            RegraAlocacao.validarSala(sala, turma);
            RegraAlocacao.validarDia(turma, horario.faixa());
            if (temConflito(horario, turma.periodo(), true))
                throw new IllegalArgumentException("A sala ou a turma já possui alocação nesse horário e período.");
            String sql = "INSERT INTO horario_aula (id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,?,?,?)";
            try (var comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                comando.setLong(1, horario.idTurma());
                comando.setLong(2, horario.idSala());
                comando.setInt(3, horario.faixa().diaSemana());
                comando.setTime(4, Time.valueOf(horario.faixa().inicio()));
                comando.setTime(5, Time.valueOf(horario.faixa().fim()));
                comando.executeUpdate();
                try (var chaves = comando.getGeneratedKeys()) {
                    if (!chaves.next()) throw new SQLException("O banco não retornou o ID da alocação.");
                    return new HorarioAula(chaves.getLong(1), horario.idTurma(), horario.idSala(), horario.faixa());
                }
            }
        });
    }

    @Override public void desalocar(long idHorario) {
        HorarioAula.validarId(idHorario);
        TransacaoJdbc.executar(conexao, () -> {
            long idSala;
            try (var comando = conexao.prepareStatement("SELECT id_sala FROM horario_aula WHERE id_horario=?")) {
                comando.setLong(1, idHorario);
                try (var registro = comando.executeQuery()) {
                    if (!registro.next()) throw new IllegalArgumentException("Alocação não encontrada.");
                    idSala = registro.getLong(1);
                }
            }
            salas.buscarBloqueada(idSala);
            try (var comando = conexao.prepareStatement("DELETE FROM horario_aula WHERE id_horario=?")) {
                comando.setLong(1, idHorario);
                if (comando.executeUpdate() == 0) throw new IllegalArgumentException("Alocação não encontrada.");
            }
            return null;
        });
    }

    private TurmaResumo buscarTurma(long id, boolean bloquear) throws SQLException {
        try (var comando = conexao.prepareStatement(TURMA + " WHERE t.id_turma=?" + (bloquear ? " FOR UPDATE" : ""))) {
            comando.setLong(1, id);
            try (var registro = comando.executeQuery()) {
                if (!registro.next()) throw new IllegalArgumentException("Turma não encontrada.");
                return lerTurma(registro);
            }
        }
    }

    private boolean temConflito(HorarioAula pedido, PeriodoLetivo periodo, boolean bloquear) throws SQLException {
        String sql = "SELECT h.*,p.data_inicio,p.data_fim FROM horario_aula h "
                + "JOIN turma t ON t.id_turma=h.id_turma JOIN periodo_letivo p ON p.id_periodo=t.id_periodo "
                + "WHERE (h.id_sala=? OR h.id_turma=?) AND h.dia_semana=? "
                + "AND h.hora_inicio<? AND h.hora_fim>? AND p.data_inicio<=? AND p.data_fim>=?"
                + (bloquear ? " FOR UPDATE" : "");
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, pedido.idSala());
            comando.setLong(2, pedido.idTurma());
            comando.setInt(3, pedido.faixa().diaSemana());
            comando.setTime(4, Time.valueOf(pedido.faixa().fim()));
            comando.setTime(5, Time.valueOf(pedido.faixa().inicio()));
            comando.setDate(6, Date.valueOf(periodo.fim()));
            comando.setDate(7, Date.valueOf(periodo.inicio()));
            try (var registros = comando.executeQuery()) {
                while (registros.next()) {
                    PeriodoLetivo outro = new PeriodoLetivo(registros.getDate("data_inicio").toLocalDate(), registros.getDate("data_fim").toLocalDate());
                    if (RegraAlocacao.conflita(pedido.faixa(), periodo, lerHorario(registros).faixa(), outro)) return true;
                }
            }
        }
        return false;
    }

    private static TurmaResumo lerTurma(ResultSet registro) throws SQLException {
        return new TurmaResumo(registro.getLong("id_turma"), registro.getString("codigo"), registro.getInt("capacidade"),
                new PeriodoLetivo(registro.getDate("data_inicio").toLocalDate(), registro.getDate("data_fim").toLocalDate()));
    }

    private static HorarioAula lerHorario(ResultSet registro) throws SQLException {
        return new HorarioAula(registro.getLong("id_horario"), registro.getLong("id_turma"), registro.getLong("id_sala"),
                new FaixaHorario(registro.getInt("dia_semana"), registro.getTime("hora_inicio").toLocalTime(), registro.getTime("hora_fim").toLocalTime()));
    }
}

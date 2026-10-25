package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.FaixaHorario;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.RepositorioAlocacao;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.TurmaResumo;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public final class ServicoAlocacao {
    private final RepositorioAlocacao repositorio;
    public ServicoAlocacao(RepositorioAlocacao repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "Informe um repositório de alocação.");
    }

    public List<TurmaResumo> listarTurmas() { return List.copyOf(repositorio.listarTurmas()); }
    public TurmaResumo buscarTurma(long turma) {
        HorarioAula.validarId(turma);
        return repositorio.buscarTurma(turma);
    }
    public List<HorarioAula> listarHorarios(long turma) {
        HorarioAula.validarId(turma);
        return List.copyOf(repositorio.listarHorarios(turma));
    }
    public List<Sala> listarDisponiveis(long turma, int dia, LocalTime inicio, LocalTime fim) {
        HorarioAula.validarId(turma);
        return List.copyOf(repositorio.listarDisponiveis(turma, new FaixaHorario(dia, inicio, fim)));
    }
    public HorarioAula alocar(long turma, long sala, int dia, LocalTime inicio, LocalTime fim) {
        return repositorio.alocar(new HorarioAula(null, turma, sala, new FaixaHorario(dia, inicio, fim)));
    }
    public void desalocar(long horario) {
        HorarioAula.validarId(horario);
        repositorio.desalocar(horario);
    }
}

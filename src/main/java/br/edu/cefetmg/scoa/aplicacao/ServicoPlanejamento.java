package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.Planejamento.Periodo;
import br.edu.cefetmg.scoa.dominio.Planejamento.Resultado;
import br.edu.cefetmg.scoa.dominio.RepositorioPlanejamento;
import java.util.List;
import java.util.Objects;

public final class ServicoPlanejamento {
    private final RepositorioPlanejamento repositorio;

    public ServicoPlanejamento(RepositorioPlanejamento repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    public List<Periodo> periodos() {
        return repositorio.periodos();
    }

    public Resultado simular(long matricula, List<Long> selecionados, List<Long> prioridade) {
        return repositorio.simular(matricula, selecionados, prioridade);
    }
}

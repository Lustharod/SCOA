package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.Planejamento.Periodo;
import br.edu.cefetmg.scoa.dominio.Planejamento.Resultado;
import java.util.List;

public interface RepositorioPlanejamento {
    List<Periodo> periodos();
    Resultado simular(long matricula, List<Long> selecionados, List<Long> prioridade);
}

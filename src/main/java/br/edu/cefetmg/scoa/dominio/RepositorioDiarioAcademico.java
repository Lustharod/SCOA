package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;

public interface RepositorioDiarioAcademico {
    List<Aula> registrarAulas(NovaAula pedido);
    List<Aula> aulas(long turma);
    Avaliacao registrarAvaliacao(NovaAvaliacao pedido);
    List<Avaliacao> avaliacoes(long turma);
    void lancarNota(LancarNota pedido);
    void retirarNota(RetirarNota pedido);
    void lancarFrequencia(LancarFrequencia pedido);
    Boletim consultarDocente(long inscricao);
    Boletim consultarAluno(long inscricao);
    RegraResultado.Resultado fechar(long inscricao,String motivo);
    List<Processamento> importarSimulados(List<Chegada> eventos);
}

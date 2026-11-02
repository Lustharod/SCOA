package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.util.List;

public interface RepositorioInscricao {
    Elegibilidade avaliar(long matricula, long turma);
    Inscricao inscrever(long matricula, long turma);
    List<Inscricao> listar(long matricula);
    void vincularEstrutura(long matricula, long estrutura);
    RegraResultado.Resultado registrarResultado(long inscricao, BigDecimal p1, BigDecimal p2,
            BigDecimal pf, long totalMinutos, long presentesMinutos, String motivo);
}

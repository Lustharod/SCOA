package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.util.List;

public interface ConsultasAluno {
    record TurmaInscrita(long matricula, String curso, long periodo, String descricaoPeriodo,
                        String disciplina, String turma) { }

    record Horario(TurmaInscrita turma, FaixaHorario faixa, String sala) { }

    record ResultadoFinal(TurmaInscrita turma, BigDecimal p1, BigDecimal p2, BigDecimal pf,
                          BigDecimal mediaInicial, BigDecimal mediaFinal,
                          long minutosPresentes, long minutosTotais, String situacao) { }

    List<Horario> horarios();
    List<ResultadoFinal> resultadosFinais();
}

package br.edu.cefetmg.scoa.dominio;

/** RF-MA-08 / RN-MA-03: disponibilidade e capacidade são verificadas novamente ao gravar. */
public final class RegraAlocacao {
    private RegraAlocacao() { }

    public static void validarSala(Sala sala, TurmaResumo turma) {
        if (!"ATIVO".equals(sala.getSituacao())) throw new IllegalArgumentException("A sala está inativa e não pode ser alocada.");
        if (sala.getCapacidade() < turma.capacidade()) throw new IllegalArgumentException("A sala não tem capacidade suficiente para a turma.");
    }

    public static void validarDia(TurmaResumo turma, FaixaHorario faixa) {
        if (!turma.periodo().compartilhaDia(turma.periodo(), faixa.diaSemana()))
            throw new IllegalArgumentException("O dia escolhido não ocorre dentro do período letivo da turma.");
    }

    public static boolean conflita(FaixaHorario solicitada, PeriodoLetivo periodo,
                                   FaixaHorario existente, PeriodoLetivo outroPeriodo) {
        return solicitada.sobrepoe(existente) && periodo.compartilhaDia(outroPeriodo, solicitada.diaSemana());
    }
}

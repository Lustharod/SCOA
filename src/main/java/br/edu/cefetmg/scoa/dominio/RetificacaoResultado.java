package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RetificacaoResultado(BigDecimal p1,BigDecimal p2,BigDecimal pf,long total,long presentes) {
    public RegraResultado.Resultado calcular() {
        return calcular(ParametrosAcademicos.Regras.baseAprovada());
    }
    public RegraResultado.Resultado calcular(ParametrosAcademicos.Regras regras) {
        var apuracao=RegraResultado.calcular(p1,p2,pf,total,presentes,regras);
        if("AGUARDA_PF".equals(apuracao.situacao()))
            throw new IllegalArgumentException("PF pendente. Informe uma apuração final completa.");
        return apuracao;
    }

    public static boolean noPrazoDoProfessor(LocalDate fimPeriodo,LocalDate data) {
        return data.isAfter(fimPeriodo) && !data.isAfter(fimPeriodo.plusDays(7));
    }

    public static boolean antesDaInscricaoSeguinte(LocalDate inicioSeguinte,LocalDate data) {
        return data.isBefore(JanelaInscricao.doPeriodo(inicioSeguinte).abertura());
    }
}

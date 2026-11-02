package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regra de notas do projeto; tempos de frequência são medidos na mesma unidade (minutos). */
public final class RegraResultado {
    private RegraResultado() { }
    public record Resultado(BigDecimal mediaInicial, BigDecimal mediaFinal, String situacao) { }
    public static BigDecimal nota(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.compareTo(BigDecimal.TEN) > 0
                || valor.stripTrailingZeros().scale() > 1)
            throw new IllegalArgumentException("Informe nota de 0 a 10 com no máximo uma casa decimal.");
        return valor;
    }
    public static Resultado calcular(BigDecimal p1, BigDecimal p2, BigDecimal pf, long total, long presentes) {
        nota(p1); nota(p2); if (pf != null) nota(pf);
        if (total <= 0 || presentes < 0 || presentes > total)
            throw new IllegalArgumentException("Frequência: informe minutos totais positivos e presentes entre zero e o total.");
        BigDecimal inicial = p1.add(p2).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
        if (inicial.compareTo(BigDecimal.valueOf(7)) >= 0 && pf != null)
            throw new IllegalArgumentException("A média inicial dispensa PF. Preserve somente as provas aplicáveis.");
        boolean frequencia = BigDecimal.valueOf(presentes).multiply(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(75))) >= 0;
        BigDecimal finalMedia = inicial.compareTo(BigDecimal.valueOf(7)) >= 0 ? inicial
                : pf == null ? null : inicial.add(pf).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
        String situacao = !frequencia ? "REPROVADO" : finalMedia == null ? "AGUARDA_PF"
                : finalMedia.compareTo(inicial.compareTo(BigDecimal.valueOf(7)) >= 0
                    ? BigDecimal.valueOf(7) : BigDecimal.valueOf(5)) >= 0 ? "APROVADO" : "REPROVADO";
        return new Resultado(inicial, finalMedia, situacao);
    }
}

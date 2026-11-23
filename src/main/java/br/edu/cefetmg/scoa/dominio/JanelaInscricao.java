package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.util.Objects;

public record JanelaInscricao(LocalDate abertura, LocalDate fimExclusivo) {
    public static JanelaInscricao doPeriodo(LocalDate inicio) {
        Objects.requireNonNull(inicio);
        return new JanelaInscricao(inicio.minusDays(14), inicio.minusDays(7));
    }
    public static JanelaInscricao segundaFase(LocalDate inicio) {
        return new JanelaInscricao(inicio.plusDays(7), inicio.plusDays(14));
    }

    @Override public String toString() {
        return abertura + " a " + fimExclusivo.minusDays(1);
    }
    public boolean permite(LocalDate data) {
        return !data.isBefore(abertura) && data.isBefore(fimExclusivo);
    }
}

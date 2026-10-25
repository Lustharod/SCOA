package br.edu.cefetmg.scoa.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

public record PeriodoLetivo(LocalDate inicio, LocalDate fim) {
    public PeriodoLetivo {
        Objects.requireNonNull(inicio, "Informe o início do período.");
        Objects.requireNonNull(fim, "Informe o fim do período.");
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("O período letivo tem datas inválidas.");
    }

    public boolean compartilhaDia(PeriodoLetivo outro, int diaSemana) {
        LocalDate primeiro = inicio.isAfter(outro.inicio) ? inicio : outro.inicio;
        LocalDate ultimo = fim.isBefore(outro.fim) ? fim : outro.fim;
        if (primeiro.isAfter(ultimo)) return false;
        return !primeiro.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(diaSemana))).isAfter(ultimo);
    }
}

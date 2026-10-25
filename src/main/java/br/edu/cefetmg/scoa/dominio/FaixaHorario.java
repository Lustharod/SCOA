package br.edu.cefetmg.scoa.dominio;

import java.time.LocalTime;
import java.util.Objects;

/** Semana ISO: 1=segunda-feira e 7=domingo; intervalo [início, fim). */
public record FaixaHorario(int diaSemana, LocalTime inicio, LocalTime fim) {
    public FaixaHorario {
        if (diaSemana < 1 || diaSemana > 7) throw new IllegalArgumentException("O dia deve estar entre 1 (segunda) e 7 (domingo).");
        Objects.requireNonNull(inicio, "Informe a hora inicial.");
        Objects.requireNonNull(fim, "Informe a hora final.");
        if (!fim.isAfter(inicio)) throw new IllegalArgumentException("A hora final deve ser posterior à inicial, no mesmo dia.");
        if (inicio.getNano() != 0 || fim.getNano() != 0) throw new IllegalArgumentException("Use horários com precisão de segundos.");
    }

    public boolean sobrepoe(FaixaHorario outra) {
        return diaSemana == outra.diaSemana && inicio.isBefore(outra.fim) && fim.isAfter(outra.inicio);
    }
}

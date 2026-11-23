package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CancelamentoInscricao(long id, long inscricao, long solicitante, String justificativa,
                                   LocalDateTime solicitadoEm, String situacao, Long coordenador,
                                   String parecer, LocalDateTime decididoEm) {
    public static boolean imediato(LocalDate inicio, LocalDate data) {
        return data.isBefore(JanelaInscricao.segundaFase(inicio).fimExclusivo());
    }

    public static boolean permitePedido(LocalDate inicio, LocalDate fim, LocalDate data) {
        return !data.isAfter(inicio.plusDays(45)) && !data.isAfter(fim);
    }
}

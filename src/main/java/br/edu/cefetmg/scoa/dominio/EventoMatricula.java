package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventoMatricula(long id, long idMatricula, LocalDateTime ocorridoEm, String operacao,
        String motivo, String situacaoAnterior, String situacaoNova, LocalDate dataAnterior,
        LocalDate dataNova, Long periodoAnterior, Long periodoNovo) { }

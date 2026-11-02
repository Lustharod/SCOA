package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;

public record Inscricao(long id, long matricula, long turma, LocalDate data, String situacao) { }

package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Efetivação interna; transferência externa continua exigindo contrato próprio. */
public final class TransferenciaInterna {
    private TransferenciaInterna() { }
    public record Pedido(long origem,long cursoDestino,long estruturaDestino,long periodo,String motivo) {
        public Pedido {
            HorarioAula.validarId(origem); HorarioAula.validarId(cursoDestino);
            HorarioAula.validarId(estruturaDestino); HorarioAula.validarId(periodo);
            motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record Registro(long id,long origem,Long destino,Long cursoDestino,String tipo,String situacao,
            LocalDate data,Long autor,String motivo,String historico,LocalDateTime registradoEm) { }
    public static boolean noPrazo(LocalDate inicio,LocalDate fim,LocalDate hoje) {
        return !hoje.isAfter(inicio.plusDays(45)) && !hoje.isAfter(fim);
    }
}

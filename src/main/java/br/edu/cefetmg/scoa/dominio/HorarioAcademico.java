package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDateTime;
import java.util.*;

public final class HorarioAcademico {
    private HorarioAcademico() { }
    public record Planejar(long turma,int dia,List<TempoLetivo> tempos,String motivo) {
        public Planejar {
            HorarioAula.validarId(turma);
            if(dia<1 || dia>7) throw new IllegalArgumentException("Use dia de 1=segunda a 7=domingo.");
            TempoLetivo.minutosLetivos(tempos); tempos=List.copyOf(tempos); motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record Registro(HorarioAula horario,boolean ativo,int versao,Long autor,LocalDateTime atualizadoEm) { }
    public record Evento(long id,long horario,int versao,String operacao,String anterior,String novo,String motivo,
            long autor,LocalDateTime registradoEm) { }
}

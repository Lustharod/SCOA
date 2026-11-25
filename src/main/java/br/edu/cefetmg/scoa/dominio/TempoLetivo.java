package br.edu.cefetmg.scoa.dominio;

import java.time.LocalTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/** Grade aprovada pelo autor em 25/11/2026. Intervalos não são tempos letivos. */
public enum TempoLetivo {
    M1(Turno.MANHA,"07:00","07:50"), M2(Turno.MANHA,"07:50","08:40"),
    M3(Turno.MANHA,"08:40","09:30"), M4(Turno.MANHA,"09:50","10:40"),
    M5(Turno.MANHA,"10:40","11:30"), M6(Turno.MANHA,"11:30","12:20"),
    T1(Turno.TARDE,"12:40","13:30"), T2(Turno.TARDE,"13:30","14:20"),
    T3(Turno.TARDE,"14:20","15:10"), T4(Turno.TARDE,"15:30","16:20"),
    T5(Turno.TARDE,"16:20","17:10"), T6(Turno.TARDE,"17:10","18:00"),
    N1(Turno.NOITE,"18:00","18:50"), N2(Turno.NOITE,"18:50","19:40"),
    N3(Turno.NOITE,"19:40","20:30"), N4(Turno.NOITE,"20:50","21:40"),
    N5(Turno.NOITE,"21:40","22:30");

    public enum Turno { MANHA, TARDE, NOITE }
    public enum Presenca { PRESENTE, FALTA, SEM_REGISTRO }
    private final Turno turno;
    private final LocalTime inicio,fim;
    TempoLetivo(Turno turno,String inicio,String fim) {
        this.turno=turno; this.inicio=LocalTime.parse(inicio); this.fim=LocalTime.parse(fim);
    }
    public Turno turno() { return turno; }
    public LocalTime inicio() { return inicio; }
    public LocalTime fim() { return fim; }
    public int minutos() { return 50; }
    /** A chegada informada é avaliada para este tempo, não para o turno inteiro. */
    public Presenca presencaPorChegada(LocalTime chegada) {
        if(chegada==null) return Presenca.SEM_REGISTRO;
        return chegada.isAfter(inicio.plusMinutes(15))?Presenca.FALTA:Presenca.PRESENTE;
    }
    public static List<TempoLetivo> doTurno(Turno turno) {
        if(turno==null) throw new IllegalArgumentException("Informe o turno.");
        return java.util.Arrays.stream(values()).filter(t->t.turno==turno).toList();
    }
    public static int minutosLetivos(Collection<TempoLetivo> tempos) {
        if(tempos==null || tempos.isEmpty()) throw new IllegalArgumentException("Selecione ao menos um tempo.");
        var distintos=EnumSet.noneOf(TempoLetivo.class);
        for(var tempo:tempos) if(tempo==null || !distintos.add(tempo))
            throw new IllegalArgumentException("Selecione tempos válidos, sem repetição.");
        return distintos.size()*50;
    }
}

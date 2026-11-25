package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class DiarioAcademico {
    private DiarioAcademico() { }
    public enum Prova { P1, P2, PF }
    public record NovaAula(long turma,LocalDate data,List<TempoLetivo> tempos,String pauta,String conteudo) {
        public NovaAula {
            HorarioAula.validarId(turma); Objects.requireNonNull(data,"Informe a data.");
            TempoLetivo.minutosLetivos(tempos); tempos=List.copyOf(tempos);
            pauta=texto(pauta,255); conteudo=texto(conteudo,10000);
        }
    }
    public record NovaAvaliacao(long turma,Prova tipo,LocalDate data,String descricao) {
        public NovaAvaliacao {
            HorarioAula.validarId(turma); Objects.requireNonNull(tipo); Objects.requireNonNull(data);
            descricao=texto(descricao,10000);
        }
    }
    public record Aula(long id,long turma,LocalDate data,String tempo,String pauta,String conteudo) { }
    public record Avaliacao(long id,long turma,String tipo,LocalDate data,String descricao) { }
    public record Nota(Avaliacao avaliacao,Long id,BigDecimal valor,Integer versao,Long autor) { }
    public record Frequencia(Aula aula,Long id,String situacao,String origem,Integer versao,Long autor,boolean considerada) { }
    public record LancarNota(long avaliacao,long inscricao,BigDecimal valor,int versao,String motivo) {
        public LancarNota {
            HorarioAula.validarId(avaliacao); HorarioAula.validarId(inscricao); RegraResultado.nota(valor);
            if(versao< -1) throw new IllegalArgumentException("Versão inválida."); motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record LancarFrequencia(long aula,long inscricao,TempoLetivo.Presenca presenca,int versao,String motivo) {
        public LancarFrequencia {
            HorarioAula.validarId(aula); HorarioAula.validarId(inscricao); Objects.requireNonNull(presenca);
            if(versao< -1) throw new IllegalArgumentException("Versão inválida."); motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record RetirarNota(long avaliacao,long inscricao,int versao,String motivo) {
        public RetirarNota {
            HorarioAula.validarId(avaliacao); HorarioAula.validarId(inscricao);
            if(versao<0) throw new IllegalArgumentException("Consulte um lançamento existente.");
            motivo=Matricula.validarMotivo(motivo);
        }
    }
    /** Evento fictício por aula/tempo, com horário local de São Paulo e identidade de deduplicação. */
    public record Chegada(String origem,String chave,long aula,long inscricao,LocalDateTime instante) {
        public Chegada {
            origem=texto(origem,80); chave=texto(chave,100);
            HorarioAula.validarId(aula); HorarioAula.validarId(inscricao); Objects.requireNonNull(instante);
            instante=instante.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        }
    }
    public record Processamento(String chave,String situacao,String mensagem) { }
    public record Boletim(long inscricao,long turma,long minutosPrevistos,long minutosRegistrados,long minutosConsiderados,long minutosPresentes,
                          List<Nota> notas,List<Frequencia> frequencias,List<String> pendencias,
                          RegraResultado.Resultado previa,boolean encerrado) {
        public Boletim { notas=List.copyOf(notas); frequencias=List.copyOf(frequencias); pendencias=List.copyOf(pendencias); }
    }
    public static long minutosDaCarga(int horas) {
        if(horas<=0) throw new IllegalArgumentException("A carga horária deve ser positiva.");
        return Math.multiplyExact((long)horas,60);
    }
    public static long temposDaCarga(int horas) {
        long minutos=minutosDaCarga(horas);
        if(minutos%50!=0) throw new IllegalArgumentException("A carga cadastrada não forma tempos inteiros de 50 minutos. Confira o cadastro; não será arredondada.");
        return minutos/50;
    }
    private static String texto(String valor,int max) {
        if(valor==null || valor.isBlank()) throw new IllegalArgumentException("Preencha os campos obrigatórios.");
        valor=valor.strip();
        if(valor.codePointCount(0,valor.length())>max) throw new IllegalArgumentException("Texto excede "+max+" caracteres.");
        return valor;
    }
}

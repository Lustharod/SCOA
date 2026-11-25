package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDateTime;
import java.util.*;

public final class RematriculaLote {
    private RematriculaLote() { }
    public record Selecao(long curso,long origem,long destino,List<Long> matriculas) {
        public Selecao {
            HorarioAula.validarId(curso); HorarioAula.validarId(origem); HorarioAula.validarId(destino);
            if(origem==destino) throw new IllegalArgumentException("Selecione períodos diferentes.");
            if(matriculas==null || new HashSet<>(matriculas).size()!=matriculas.size()) throw new IllegalArgumentException("Informe matrículas sem repetição.");
            for(Long m:matriculas) { if(m==null) throw new IllegalArgumentException("Matrícula inválida."); HorarioAula.validarId(m); }
            matriculas=List.copyOf(matriculas);
        }
    }
    public record Proposta(long disciplina,Long turma,String situacao,String mensagem) { }
    public record Candidato(long matricula,String identificacao,Long plano,String situacao,String mensagem,List<Proposta> propostas) {
        public Candidato { propostas=List.copyOf(propostas); }
    }
    public record Previa(Selecao selecao,String chave,String assinatura,List<Candidato> candidatos) {
        public Previa { candidatos=List.copyOf(candidatos); }
    }
    public record Pedido(Selecao selecao,String chave,String assinatura,String motivo) {
        public Pedido {
            Objects.requireNonNull(selecao); chave=Equivalencia.texto(chave,"chave do processamento",80);
            if(assinatura==null || !assinatura.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Confira uma prévia válida do lote.");
            motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record Resultado(long id,String chave,long curso,long origem,long destino,long autor,LocalDateTime data,List<Candidato> candidatos) {
        public Resultado { candidatos=List.copyOf(candidatos); }
        public long renovadas() { return candidatos.stream().filter(c->"RENOVADA".equals(c.situacao())).count(); }
        public long manuais() { return candidatos.stream().filter(c->"MANUAL".equals(c.situacao())).count(); }
        public long jaRenovadas() { return candidatos.stream().filter(c->"JA_RENOVADA".equals(c.situacao())).count(); }
    }
}

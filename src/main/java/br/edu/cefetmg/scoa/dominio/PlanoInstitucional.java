package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class PlanoInstitucional {
    private PlanoInstitucional() { }
    public record Selecao(long matricula,List<Long> periodos,List<Long> prioridades) {
        public Selecao {
            HorarioAula.validarId(matricula);
            periodos=ids(periodos); prioridades=ids(prioridades);
            if(periodos.isEmpty()) throw new IllegalArgumentException("Selecione pelo menos um período.");
        }
    }
    public record Componente(long disciplina,String codigo,String nome,int horas,String cadastro,String situacao,
            String evidencia,List<Long> requisitos,Long turma,Long periodo,String descricaoPeriodo,
            LocalDate inicio,LocalDate fim,String diagnostico) {
        public Componente { requisitos=List.copyOf(requisitos); }
    }
    public record Previa(Selecao selecao,int versaoAnterior,long estrutura,String assinatura,List<Componente> componentes) {
        public Previa { componentes=List.copyOf(componentes); }
    }
    public record Registro(Selecao selecao,int versaoAnterior,String assinatura,String motivo) {
        public Registro {
            Objects.requireNonNull(selecao);
            if(versaoAnterior<0) throw new IllegalArgumentException("Versão anterior inválida.");
            if(assinatura==null || !assinatura.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Confira uma prévia válida antes de registrar.");
            motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record Versao(long id,long matricula,long estrutura,int numero,Long anterior,long autor,
            LocalDateTime data,String motivo,String assinatura,List<Long> periodos,List<Long> prioridades,List<Componente> componentes) {
        public Versao { periodos=List.copyOf(periodos); prioridades=List.copyOf(prioridades); componentes=List.copyOf(componentes); }
    }
    public enum Atualidade { ATUAL, PENDENTE_ATUALIZACAO, INCONSISTENTE }
    public record Consulta(Versao versao,Atualidade atualidade,String aviso) { }
    private static List<Long> ids(List<Long> valores) {
        if(valores==null) throw new IllegalArgumentException("Informe a seleção.");
        if(new HashSet<>(valores).size()!=valores.size()) throw new IllegalArgumentException("Não repita identificadores.");
        for(Long id:valores) { if(id==null) throw new IllegalArgumentException("ID inválido."); HorarioAula.validarId(id); }
        return List.copyOf(valores);
    }
}

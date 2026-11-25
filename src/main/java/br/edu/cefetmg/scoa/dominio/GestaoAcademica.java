package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class GestaoAcademica {
    private GestaoAcademica() { }
    public record CursoDisponivel(long id,String nome,String situacao) { }
    public record Docente(long id,String identificacao,String situacao) { }
    public enum TipoMeta { META, PRAZO }
    public enum SituacaoMeta { ABERTA, EM_ANDAMENTO, CONCLUIDA, CANCELADA }
    public record NovaMeta(long curso,long professor,Long periodo,TipoMeta tipo,String descricao,LocalDate prazo) {
        public NovaMeta {
            HorarioAula.validarId(curso); HorarioAula.validarId(professor); if(periodo!=null) HorarioAula.validarId(periodo);
            Objects.requireNonNull(tipo); validarPrazo(prazo); descricao=Equivalencia.texto(descricao,"descrição",255);
        }
    }
    public record Meta(long id,long curso,long professor,Long periodo,String tipo,String descricao,LocalDate prazo,String situacao,int versao,Long autor,LocalDateTime data) {
        public boolean atrasada(LocalDate hoje) { return Set.of("ABERTA","EM_ANDAMENTO").contains(situacao) && prazo.isBefore(hoje); }
    }
    public record AtualizarMeta(long id,int versao,String descricao,LocalDate prazo,SituacaoMeta situacao,String motivo) {
        public AtualizarMeta {
            HorarioAula.validarId(id); if(versao<0) throw new IllegalArgumentException("Versão inválida.");
            descricao=Equivalencia.texto(descricao,"descrição",255); validarPrazo(prazo); Objects.requireNonNull(situacao); motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record HistoricoMeta(long id,int versao,long autor,LocalDateTime data,String anterior,String posterior,String motivo) { }
    public record Filtro(long curso,Long periodo,Long disciplina,Long turma,Long aluno) {
        public Filtro { HorarioAula.validarId(curso); for(Long id:Arrays.asList(periodo,disciplina,turma,aluno)) if(id!=null) HorarioAula.validarId(id); }
    }
    public record AvaliacaoGestao(long turma,long disciplina,long professor,long periodo,DiarioAcademico.Avaliacao avaliacao) { }
    public record ResultadoVigente(BigDecimal p1,BigDecimal p2,BigDecimal pf,BigDecimal mediaInicial,BigDecimal mediaFinal,
            long total,long presentes,String situacao,String motivo) { }
    public record AlunoDiario(long matricula,long inscricao,String identificacao,String situacao,DiarioAcademico.Boletim lancamentos,ResultadoVigente resultado) { }
    public record Diario(long curso,long turma,List<DiarioAcademico.Aula> aulas,List<DiarioAcademico.Avaliacao> avaliacoes,List<AlunoDiario> alunos,String assinatura) {
        public Diario { aulas=List.copyOf(aulas); avaliacoes=List.copyOf(avaliacoes); alunos=List.copyOf(alunos); }
    }
    public enum ConclusaoAuditoria { SEM_RESSALVAS, COM_RESSALVAS, REQUER_TRATAMENTO }
    public record NovaAuditoria(long curso,long turma,String assinatura,ConclusaoAuditoria conclusao,String observacoes) {
        public NovaAuditoria {
            HorarioAula.validarId(curso); HorarioAula.validarId(turma); Objects.requireNonNull(conclusao);
            if(assinatura==null || !assinatura.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Confira o diário antes de concluir a auditoria.");
            observacoes=Equivalencia.texto(observacoes,"observações da auditoria",10000);
        }
    }
    public record Auditoria(long id,long diario,Long curso,long autor,LocalDateTime data,String conclusao,String observacoes,String assinatura,String dadosAvaliados) { }
    public record Monitoramento(long matricula,long aluno,String identificacao,String situacao,Integer componentes,Integer cumpridos,
            Integer reconhecidos,Integer sobRevisao,Integer emAndamento,String plano,String aviso) { }
    private static void validarPrazo(LocalDate prazo) {
        if(prazo==null || prazo.getYear()<1000 || prazo.getYear()>9999) throw new IllegalArgumentException("Informe um prazo válido entre os anos 1000 e 9999.");
    }
}

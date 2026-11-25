package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Consulta escolar não emite documento oficial nem reclassifica registros históricos. */
public final class AtendimentoAluno {
    private AtendimentoAluno() { }
    public enum TipoManifestacao { SUGESTAO, RECLAMACAO }
    public record NovaManifestacao(TipoManifestacao tipo,String conteudo) {
        public NovaManifestacao {
            if(tipo==null) throw new IllegalArgumentException("Escolha sugestão ou reclamação.");
            conteudo=Equivalencia.texto(conteudo,"conteúdo da manifestação",10000);
        }
    }
    public record Manifestacao(long id,long aluno,Long autor,String tipo,String conteudo,LocalDateTime data) { }
    public record Vinculo(long matricula,String curso,String situacao,LocalDate ingresso) { }
    public record DisciplinaCursada(long matricula,String curso,long periodo,String descricaoPeriodo,
            LocalDate inicio,LocalDate fim,long disciplina,String codigo,String nome,int cargaHoraria,
            long inscricao,String turma,String situacaoInscricao,String situacaoResultado,
            BigDecimal mediaInicial,BigDecimal mediaFinal,Long minutosTotais,Long minutosPresentes) {
        public boolean resultadoCoerente() {
            return situacaoResultado!=null && situacaoResultado.equals(situacaoInscricao)
                    && java.util.Set.of("APROVADO","REPROVADO").contains(situacaoResultado);
        }
    }
    public record Reconhecimento(long matricula,String curso,long pedido,long disciplina,String nome,
            int cargaHoraria,String instituicao,String origem,LocalDate conclusao,String decisao,
            String revisao,boolean vigente,LocalDateTime decididoEm) { }
    public record Legado(long matricula,String curso,long referencia,String disciplina,String decisao) { }
    public record Historico(long aluno,String identificacao,List<Vinculo> matriculas,
            List<DisciplinaCursada> disciplinas,List<Reconhecimento> reconhecimentos,List<Legado> legados) {
        public Historico {
            matriculas=List.copyOf(matriculas); disciplinas=List.copyOf(disciplinas);
            reconhecimentos=List.copyOf(reconhecimentos); legados=List.copyOf(legados);
        }
    }
}

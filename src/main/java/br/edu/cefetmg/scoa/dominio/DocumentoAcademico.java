package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Solicitação e atendimento; a emissão/assinatura do documento é uma etapa distinta. */
public final class DocumentoAcademico {
    private DocumentoAcademico() { }
    public enum Tipo { DECLARACAO_MATRICULA, HISTORICO_ESCOLAR }
    public enum Situacao { SOLICITADO, EM_ATENDIMENTO, ATENDIDO, INDEFERIDO }
    public record Novo(long matricula,Tipo tipo) {
        public Novo { HorarioAula.validarId(matricula); Objects.requireNonNull(tipo,"Selecione o documento."); }
    }
    public record Atualizacao(long protocolo,int versao,Situacao situacao,String parecer,String referenciaEntrega) {
        public Atualizacao {
            HorarioAula.validarId(protocolo);
            if(versao<0) throw new IllegalArgumentException("Versão inválida.");
            Objects.requireNonNull(situacao,"Selecione a situação.");
            if(situacao==Situacao.SOLICITADO) throw new IllegalArgumentException("Selecione um andamento da secretaria.");
            parecer=texto(parecer,4000,"Informe um parecer de até 4.000 caracteres.");
            referenciaEntrega=referenciaEntrega==null?null:referenciaEntrega.strip();
            if(referenciaEntrega!=null && referenciaEntrega.isEmpty()) referenciaEntrega=null;
            if(referenciaEntrega!=null && referenciaEntrega.codePointCount(0,referenciaEntrega.length())>500)
                throw new IllegalArgumentException("A referência de entrega admite até 500 caracteres.");
            if(situacao==Situacao.ATENDIDO && referenciaEntrega==null)
                throw new IllegalArgumentException("Informe a referência do documento entregue.");
            if(situacao!=Situacao.ATENDIDO && referenciaEntrega!=null)
                throw new IllegalArgumentException("Registre a referência de entrega ao concluir como atendido.");
        }
    }
    public record Registro(long protocolo,long matricula,String curso,String tipo,String situacao,LocalDate data,
                           Long solicitante,LocalDateTime solicitadoEm,int versao,Long atendente,
                           LocalDateTime atualizadoEm,String parecer,String referenciaEntrega) { }
    public record Andamento(long id,long protocolo,int versao,String anterior,String situacao,long autor,
                            LocalDateTime data,String parecer,String referenciaEntrega) { }
    public static void exigirTransicao(String anterior,Situacao proxima) {
        if(!Situacao.SOLICITADO.name().equals(anterior) && !Situacao.EM_ATENDIMENTO.name().equals(anterior))
            throw new IllegalArgumentException("Solicitação concluída ou legada: não admite alteração por este fluxo.");
        if(proxima==Situacao.SOLICITADO || proxima.name().equals(anterior))
            throw new IllegalArgumentException("Escolha um novo andamento.");
    }
    private static String texto(String valor,int max,String mensagem) {
        if(valor==null || valor.isBlank()) throw new IllegalArgumentException(mensagem);
        valor=valor.strip();
        if(valor.codePointCount(0,valor.length())>max) throw new IllegalArgumentException(mensagem);
        return valor;
    }
}

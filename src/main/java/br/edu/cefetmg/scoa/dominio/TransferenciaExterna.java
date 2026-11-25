package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

/** Processo externo e sua efetivação explícita, sem duplicar pessoa ou conta. */
public final class TransferenciaExterna {
    private TransferenciaExterna() { }
    public enum Tipo { ENTRADA, SAIDA }
    public enum Situacao { EM_ANALISE, EFETIVADA, INDEFERIDA, CANCELADA }
    public record Pedido(String chave,Tipo tipo,Long aluno,Long curso,Long estrutura,Long origem,
                         long periodo,String instituicao,String documentos,String motivo) {
        public Pedido {
            chave=texto(chave,100,"Chave do processo");
            if(!chave.matches("[A-Za-z0-9._:-]+")) throw new IllegalArgumentException("Use letras ASCII, números, ponto, hífen, sublinhado ou dois-pontos na chave.");
            Objects.requireNonNull(tipo,"Tipo necessário."); HorarioAula.validarId(periodo);
            if(tipo==Tipo.ENTRADA) {
                id(aluno); id(curso); id(estrutura);
                if(origem!=null) throw new IllegalArgumentException("Entrada não possui matrícula de origem no SCOA.");
            } else {
                id(origem);
                if(aluno!=null || curso!=null || estrutura!=null)
                    throw new IllegalArgumentException("Na saída, pessoa e curso são obtidos da matrícula de origem.");
            }
            instituicao=texto(instituicao,255,"Instituição externa");
            documentos=texto(documentos,10000,"Referências dos documentos");
            motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record Processo(long id,String chave,Tipo tipo,long aluno,long curso,Long estrutura,Long origem,
            long periodo,String instituicao,String documentos,String motivo,Situacao situacao,int versao,
            Long transferencia,Long matriculaEfetivada,long autor,LocalDateTime registradoEm,LocalDateTime atualizadoEm) { }
    public record Atualizacao(int versao,Situacao situacao,String documentos,String observacao) {
        public Atualizacao {
            if(versao<0 || versao==Integer.MAX_VALUE) throw new IllegalArgumentException("Versão inválida.");
            Objects.requireNonNull(situacao,"Situação necessária.");
            documentos=texto(documentos,10000,"Referências dos documentos");
            observacao=texto(observacao,10000,"Observação ou fundamento da decisão");
        }
    }
    public record Andamento(long id,long processo,int versao,String anterior,Situacao nova,String documentos,
            String observacao,long autor,LocalDateTime registradoEm) { }
    private static void id(Long valor) {
        if(valor==null) throw new IllegalArgumentException("Referência acadêmica obrigatória ausente.");
        HorarioAula.validarId(valor);
    }
    private static String texto(String valor,int limite,String campo) {
        if(valor==null || valor.isBlank() || valor.strip().length()>limite)
            throw new IllegalArgumentException(campo+": informe de 1 a "+limite+" caracteres.");
        return valor.strip();
    }
}

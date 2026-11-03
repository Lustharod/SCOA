package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Evidências declaradas do processo manual; documentos são referências, não anexos. */
public final class Equivalencia {
    private Equivalencia() { }
    public static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Informe " + campo + ".");
        valor = valor.strip();
        if (valor.codePointCount(0, valor.length()) > limite)
            throw new IllegalArgumentException(campo + ": limite de " + limite + " caracteres.");
        return valor;
    }
    public record Pedido(long matricula, long destino, String instituicao, String origem,
            int cargaOrigem, String ementaOrigem, String documentos, LocalDate conclusao,
            String ementaDestino) {
        public Pedido {
            HorarioAula.validarId(matricula); HorarioAula.validarId(destino);
            instituicao = texto(instituicao,"instituição de origem",255);
            origem = texto(origem,"disciplina de origem",255);
            if (cargaOrigem <= 0) throw new IllegalArgumentException("Carga horária de origem deve ser positiva.");
            ementaOrigem = texto(ementaOrigem,"ementa de origem",10000);
            documentos = texto(documentos,"referências de conclusão, ementa e carga horária",1000);
            if (conclusao == null || conclusao.getYear()<1000 || conclusao.getYear()>9999)
                throw new IllegalArgumentException("Informe a data de conclusão de origem entre os anos 1000 e 9999.");
            ementaDestino = texto(ementaDestino,"ementa de destino considerada",10000);
        }
    }
    public record Decisao(boolean deferir, String parecer, String responsavel) {
        public Decisao {
            parecer = texto(parecer,"parecer sobre ementa e carga horária",2000);
            responsavel = texto(responsavel,"funcionário administrativo responsável",255);
        }
    }
    public record Registro(long id, Pedido pedido, long estrutura, int cargaDestino,
            String situacao, String parecer, String responsavel,
            LocalDateTime registradoEm, LocalDateTime decididoEm) { }
    public record ItemPlano(long disciplina, String nome, String situacao, String evidencia) { }
    public record PlanoBasico(long matricula, long estrutura, String situacaoMatricula, List<ItemPlano> itens) {
        public PlanoBasico { itens=List.copyOf(itens); }
    }
    public record Evento(String tipo, long referencia, long disciplina, String situacao,
            String procedencia, LocalDateTime registradoEm) { }
}

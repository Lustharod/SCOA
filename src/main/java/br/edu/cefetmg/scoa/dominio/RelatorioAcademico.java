package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDateTime;
import java.util.*;

public final class RelatorioAcademico {
    private RelatorioAcademico() { }
    public enum Tipo { CENSO_DEMONSTRATIVO, ENADE_DEMONSTRATIVO, DIARIO_DOCENTE, DIARIO_GESTAO, HISTORICO_PROPRIO, PLANO_PROPRIO }
    public enum Formato { PDF, XLSX }
    public record Filtro(Tipo tipo,Long curso,Long periodo,Long turma,Long matricula) {
        public Filtro {
            Objects.requireNonNull(tipo);
            for(Long id:Arrays.asList(curso,periodo,turma,matricula)) if(id!=null) HorarioAula.validarId(id);
            boolean valido=switch(tipo) {
                case CENSO_DEMONSTRATIVO,ENADE_DEMONSTRATIVO -> curso!=null && periodo!=null && turma==null && matricula==null;
                case DIARIO_DOCENTE -> curso==null && periodo==null && turma!=null && matricula==null;
                case DIARIO_GESTAO -> curso!=null && periodo==null && turma!=null && matricula==null;
                case HISTORICO_PROPRIO -> curso==null && periodo==null && turma==null && matricula==null;
                case PLANO_PROPRIO -> curso==null && periodo==null && turma==null && matricula!=null;
            };
            if(!valido) throw new IllegalArgumentException("Parâmetros incompatíveis com o relatório selecionado.");
        }
    }
    public record Parametro(String nome,String valor) {
        public Parametro { Objects.requireNonNull(nome); Objects.requireNonNull(valor); }
    }
    public record Tabela(String titulo,List<String> colunas,List<List<String>> linhas) {
        public Tabela {
            Objects.requireNonNull(titulo); colunas=List.copyOf(colunas);
            if(colunas.isEmpty()) throw new IllegalArgumentException("Tabela sem colunas.");
            var copia=new ArrayList<List<String>>();
            for(var linha:linhas) {
                if(linha.size()!=colunas.size()) throw new IllegalArgumentException("Linha incompatível com as colunas.");
                copia.add(List.copyOf(linha));
            }
            linhas=List.copyOf(copia);
        }
    }
    public record Documento(String titulo,List<Parametro> parametros,List<String> avisos,List<Tabela> tabelas) {
        public Documento {
            Objects.requireNonNull(titulo); parametros=List.copyOf(parametros); avisos=List.copyOf(avisos); tabelas=List.copyOf(tabelas);
        }
    }
    public record Previa(Filtro filtro,Documento documento,String assinatura,List<String> inconsistencias) {
        public Previa { inconsistencias=List.copyOf(inconsistencias); }
        public boolean valida() { return inconsistencias.isEmpty(); }
    }
    public record Geracao(String chave,Filtro filtro,String assinatura) {
        public Geracao {
            chave=Equivalencia.texto(chave,"referência única",100); Objects.requireNonNull(filtro);
            if(!chave.matches("[A-Za-z0-9._:-]+")) throw new IllegalArgumentException("Referência deve usar letras ASCII, números, ponto, hífen, sublinhado ou dois-pontos.");
            if(assinatura==null || !assinatura.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Confira a prévia antes da geração.");
        }
    }
    public record Registro(long id,String chave,Filtro filtro,long autor,LocalDateTime geradoEm,String assinatura,Documento documento) { }
    public record Arquivo(String nome,String mime,String sha256,byte[] conteudo) {
        public Arquivo { conteudo=conteudo.clone(); }
        @Override public byte[] conteudo() { return conteudo.clone(); }
    }
}

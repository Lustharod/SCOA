package br.edu.cefetmg.scoa.web;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.nio.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

/** Entrada do navegador limitada e tipada; nunca interpreta SQL, nomes de métodos ou caminhos. */
public final class FormularioWeb {
    private final Map<String,String> valores;
    public FormularioWeb(Map<String,String> valores) { this.valores=Map.copyOf(valores); }
    public static FormularioWeb decodificar(byte[] bytes) {
        if(bytes.length>131072) throw new IllegalArgumentException("O formulário excede o limite de tamanho.");
        try {
            String texto=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            var mapa=new LinkedHashMap<String,String>();
            if(!texto.isEmpty()) for(String parte:texto.split("&",-1)) {
                var par=parte.split("=",2); String chave=decodificarParte(par[0]);
                String valor=decodificarParte(par.length==2?par[1]:"");
                if(!chave.matches("[a-zA-Z][a-zA-Z0-9_]{0,59}") || valor.length()>40000 || mapa.size()>=100 || mapa.putIfAbsent(chave,valor)!=null)
                    throw new IllegalArgumentException("Formulário inválido ou com campo repetido.");
            }
            return new FormularioWeb(mapa);
        } catch(CharacterCodingException e) { throw new IllegalArgumentException("Use texto UTF-8 válido."); }
    }
    private static String decodificarParte(String s) throws CharacterCodingException {
        var bytes=new ByteArrayOutputStream();
        for(int i=0;i<s.length();) {
            char ch=s.charAt(i);
            if(ch=='%') {
                if(i+2>=s.length())throw new IllegalArgumentException("Codificação incompleta no formulário.");
                int a=Character.digit(s.charAt(i+1),16),b=Character.digit(s.charAt(i+2),16);
                if(a<0||b<0)throw new IllegalArgumentException("Codificação inválida no formulário.");bytes.write(a*16+b);i+=3;
            } else if(ch=='+') {bytes.write(' ');i++;}
            else {int cp=s.codePointAt(i);bytes.writeBytes(new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8));i+=Character.charCount(cp);}
        }
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
    }
    public Map<String,String> valores() { return valores; }
    public Map<String,String> conferiveis() { var copia=new LinkedHashMap<>(valores);copia.remove("csrf");copia.remove("senha");return Collections.unmodifiableMap(copia); }
    public String texto(String nome) { return valores.getOrDefault(nome,"").strip(); }
    public String obrigatorio(String nome) { String s=texto(nome); if(s.isEmpty()) throw new IllegalArgumentException("Preencha o campo "+HtmlAcademico.rotulo(nome)+"."); return s; }
    public long id(String nome) { try { long n=Long.parseLong(obrigatorio(nome)); if(n<=0) throw new NumberFormatException(); return n; } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe uma referência válida em "+HtmlAcademico.rotulo(nome)+"."); } }
    public Long opcional(String nome) { return texto(nome).isEmpty()?null:id(nome); }
    public int numero(String nome) { try { return Integer.parseInt(obrigatorio(nome)); } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro em "+HtmlAcademico.rotulo(nome)+"."); } }
    public BigDecimal decimal(String nome) { try { return new BigDecimal(obrigatorio(nome).replace(',','.')); } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um valor decimal em "+HtmlAcademico.rotulo(nome)+"."); } }
    public BigDecimal decimalOpcional(String nome) { return texto(nome).isEmpty()?null:decimal(nome); }
    public LocalDate data(String nome) { try { return LocalDate.parse(obrigatorio(nome)); } catch(java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Informe uma data válida em "+HtmlAcademico.rotulo(nome)+"."); } }
    public List<Long> ids(String nome) {
        if(texto(nome).isEmpty()) return List.of();
        try { var ids=Arrays.stream(texto(nome).split("[,;\\s]+",-1)).map(Long::valueOf).toList(); if(ids.stream().anyMatch(n->n<=0)) throw new NumberFormatException(); return ids; }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Use referências numéricas separadas por vírgula em "+HtmlAcademico.rotulo(nome)+"."); }
    }
    public <T extends Enum<T>> T escolha(String nome,Class<T> tipo) { try { return Enum.valueOf(tipo,obrigatorio(nome)); } catch(IllegalArgumentException e) { throw new IllegalArgumentException("Selecione uma opção válida em "+HtmlAcademico.rotulo(nome)+"."); } }
    public boolean sim(String nome) { if(!Set.of("SIM","NAO").contains(texto(nome))) throw new IllegalArgumentException("Selecione Sim ou Não."); return "SIM".equals(texto(nome)); }
}

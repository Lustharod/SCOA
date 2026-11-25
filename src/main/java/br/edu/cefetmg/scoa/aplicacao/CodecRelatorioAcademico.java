package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.io.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** XML de dados, sem serialização de objetos Java ou acesso a entidades externas. */
public final class CodecRelatorioAcademico {
    private CodecRelatorioAcademico() { }
    public static String escrever(Documento d) {
        try {
            var out=new StringWriter(); var w=XMLOutputFactory.newFactory().createXMLStreamWriter(out);
            w.writeStartDocument("UTF-8","1.0"); w.writeStartElement("relatorio");
            elemento(w,"titulo",d.titulo());
            for(var p:d.parametros()) { w.writeStartElement("parametro"); elemento(w,"nome",p.nome()); elemento(w,"valor",p.valor()); w.writeEndElement(); }
            for(var aviso:d.avisos()) elemento(w,"aviso",aviso);
            for(var t:d.tabelas()) {
                w.writeStartElement("tabela"); elemento(w,"titulo",t.titulo());
                for(String c:t.colunas()) elemento(w,"coluna",c);
                for(var linha:t.linhas()) { w.writeStartElement("linha"); for(String c:linha) elemento(w,"celula",c); w.writeEndElement(); }
                w.writeEndElement();
            }
            w.writeEndElement(); w.writeEndDocument(); w.close(); return out.toString();
        } catch(XMLStreamException e) { throw new IllegalArgumentException("Não foi possível preservar os dados do relatório.",e); }
    }
    private static void elemento(XMLStreamWriter w,String nome,String texto) throws XMLStreamException {
        // Rejeita caracteres proibidos em XML 1.0 em vez de alterar o conteúdo.
        if(texto.codePoints().anyMatch(c->!(c==9 || c==10 || c==13 || c>=32 && c<=0xd7ff || c>=0xe000 && c<=0xfffd || c>=0x10000 && c<=0x10ffff)))
            throw new IllegalArgumentException("Texto contém caractere de controle inválido; confira a origem.");
        w.writeStartElement(nome);
        var partes=texto.split("\r",-1);
        for(int i=0;i<partes.length;i++) { if(i>0) w.writeEntityRef("#13"); w.writeCharacters(partes[i]); }
        w.writeEndElement();
    }
    public static Documento ler(String xml) {
        try {
            var f=DocumentBuilderFactory.newInstance(); f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            f.setXIncludeAware(false); f.setExpandEntityReferences(false);
            var builder=f.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
                @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            });
            var root=builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            if(!"relatorio".equals(root.getTagName())) throw new IllegalArgumentException("Formato de relatório desconhecido.");
            var parametros=new ArrayList<Parametro>(); var avisos=new ArrayList<String>(); var tabelas=new ArrayList<Tabela>(); String titulo="";
            for(var e:filhos(root)) switch(e.getTagName()) {
                case "titulo" -> titulo=e.getTextContent();
                case "parametro" -> parametros.add(new Parametro(texto(e,"nome"),texto(e,"valor")));
                case "aviso" -> avisos.add(e.getTextContent());
                case "tabela" -> {
                    var colunas=new ArrayList<String>(); var linhas=new ArrayList<List<String>>();
                    for(var c:filhos(e)) {
                        if("coluna".equals(c.getTagName())) colunas.add(c.getTextContent());
                        if("linha".equals(c.getTagName())) linhas.add(filhos(c).stream().map(Element::getTextContent).toList());
                    }
                    tabelas.add(new Tabela(texto(e,"titulo"),colunas,linhas));
                }
                default -> throw new IllegalArgumentException("Campo de relatório desconhecido.");
            }
            return new Documento(titulo,parametros,avisos,tabelas);
        } catch(Exception e) { throw new IllegalArgumentException("Dados arquivados do relatório não puderam ser interpretados.",e); }
    }
    private static List<Element> filhos(Element e) {
        var lista=new ArrayList<Element>(); for(Node n=e.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element f) lista.add(f); return lista;
    }
    private static String texto(Element e,String tag) { return filhos(e).stream().filter(f->f.getTagName().equals(tag)).findFirst().orElseThrow().getTextContent(); }
}

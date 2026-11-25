package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;

/** Formatos reais, baseados no mesmo retrato; nenhum conteúdo é convertido em fórmula. */
public final class ExportadorRelatorioAcademico {
    private ExportadorRelatorioAcademico() { }
    public static List<String> validar(Documento dados) {
        var textos=new ArrayList<String>(); textos.add(dados.titulo());
        for(var p:dados.parametros()) { textos.add(p.nome()); textos.add(p.valor()); }
        textos.addAll(dados.avisos());
        for(var t:dados.tabelas()) { textos.add(t.titulo()); textos.addAll(t.colunas()); for(var linha:t.linhas()) textos.addAll(linha); }
        var problemas=new LinkedHashSet<String>();
        try(var doc=new PDDocument();var stream=PDType0Font.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            if(stream==null) throw new IOException("Fonte ausente."); var fonte=PDType0Font.load(doc,stream,true);
            for(String texto:textos) {
                if(texto.length()>32767) problemas.add("XLSX: campo excede o limite de 32.767 caracteres; o texto não será truncado.");
                try { for(String linha:texto.replace('\r','\n').split("\n",-1)) fonte.getStringWidth(linha); }
                catch(IllegalArgumentException e) { problemas.add("PDF: há símbolo não suportado pela fonte incorporada. O texto original foi preservado; nenhuma substituição automática será feita."); }
            }
        } catch(IOException e) { problemas.add("Verificação dos formatos indisponível; confira a dependência e a fonte antes de gerar."); }
        return List.copyOf(problemas);
    }
    public static byte[] pdf(Documento dados,String referencia,long autor,LocalDateTime utc,String assinatura) {
        try(var doc=new PDDocument();var out=new ByteArrayOutputStream();
                var fonte=PDType0Font.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            if(fonte==null) throw new IOException("Fonte incorporável da dependência PDFBox não encontrada.");
            var font=PDType0Font.load(doc,fonte,true);
            var info=doc.getDocumentInformation(); info.setTitle(dados.titulo()); info.setCreator("SCOA"); info.setAuthor("Conta "+autor);
            var calendario=GregorianCalendar.from(utc.atZone(ZoneOffset.UTC)); info.setCreationDate(calendario); info.setModificationDate(calendario);
            try(var p=new Paginador(doc,font)) {
                p.paragrafo("SCOA | "+dados.titulo(),17,true);
                p.paragrafo("Referência: "+referencia+" | conta autora: "+autor,10,false);
                var local=utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("America/Sao_Paulo"));
                p.paragrafo("Gerado em "+java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").format(local)+" (UTC"+local.getOffset()+", America/Sao_Paulo)",9,false);
                p.paragrafo("SHA-256 dos dados: "+assinatura,8,false);
                for(var meta:dados.parametros()) p.paragrafo(meta.nome()+": "+meta.valor(),10,false);
                for(String aviso:dados.avisos()) p.paragrafo(aviso,10,true);
                for(var tabela:dados.tabelas()) {
                    p.paragrafo(tabela.titulo()+" ("+tabela.linhas().size()+" registros)",13,true);
                    if(tabela.linhas().isEmpty()) p.paragrafo("Sem registros nesta seção.",10,false);
                    int n=0;
                    for(var linha:tabela.linhas()) {
                        p.manterJunto(64);
                        p.paragrafo("Registro "+(++n),10,true);
                        for(int i=0;i<linha.size();i++) p.paragrafo(tabela.colunas().get(i)+": "+(linha.get(i).isEmpty()?"[não informado]":linha.get(i)),10,false);
                        p.espaco(5);
                    }
                }
            }
            int paginas=doc.getNumberOfPages();
            for(int i=0;i<paginas;i++) try(var rodape=new PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)) {
                rodape.beginText(); rodape.setFont(font,8); rodape.newLineAtOffset(38,24);
                rodape.showText("SCOA | "+referencia+" | página "+(i+1)+" de "+paginas); rodape.endText();
            }
            doc.save(out); return out.toByteArray();
        } catch(IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("PDF não gerado. Confira os dados e os símbolos suportados pela fonte incorporada; nenhum texto foi substituído ou truncado.",e);
        }
    }
    private static final class Paginador implements AutoCloseable {
        private final PDDocument doc; private final PDType0Font fonte;
        private PDPageContentStream conteudo; private float y;
        Paginador(PDDocument doc,PDType0Font fonte) throws IOException { this.doc=doc; this.fonte=fonte; pagina(); }
        private void pagina() throws IOException {
            if(conteudo!=null) conteudo.close(); var page=new PDPage(PDRectangle.A4); doc.addPage(page);
            conteudo=new PDPageContentStream(doc,page); y=page.getMediaBox().getHeight()-40;
            conteudo.setNonStrokingColor(27/255f,60/255f,90/255f);
        }
        void espaco(float pontos) { y-=pontos; }
        void manterJunto(float pontos) throws IOException { if(y-pontos<44) pagina(); }
        void paragrafo(String texto,float tamanho,boolean destaque) throws IOException {
            for(String bloco:texto.replace("\r\n","\n").replace('\r','\n').split("\n",-1)) {
                // Quebra por pontos de código, sem cortar pares substitutos ou conteúdo extenso.
                var linha=new StringBuilder();
                for(int cp:bloco.codePoints().toArray()) {
                    String token=new String(Character.toChars(cp));
                    String proxima=linha+token;
                    if(fonte.getStringWidth(proxima)/1000*tamanho>PDRectangle.A4.getWidth()-76 && !linha.isEmpty()) {
                        int separador=linha.lastIndexOf(" ");
                        if(separador>0) {
                            imprimir(linha.substring(0,separador),tamanho,destaque);
                            String resto=linha.substring(separador+1); linha.setLength(0); linha.append(resto);
                        } else { imprimir(linha.toString(),tamanho,destaque); linha.setLength(0); }
                    }
                    linha.append(token);
                }
                imprimir(linha.toString(),tamanho,destaque);
            }
            espaco(destaque?6:2);
        }
        private void imprimir(String texto,float tamanho,boolean destaque) throws IOException {
            if(y-tamanho<44) pagina();
            if(destaque) conteudo.setNonStrokingColor(27/255f,80/255f,122/255f);
            else conteudo.setNonStrokingColor(35/255f,42/255f,50/255f);
            conteudo.beginText(); conteudo.setFont(fonte,tamanho); conteudo.newLineAtOffset(38,y);
            conteudo.showText(texto); conteudo.endText(); y-=tamanho*1.4f;
        }
        @Override public void close() throws IOException { if(conteudo!=null) conteudo.close(); }
    }
    public static byte[] xlsx(Documento dados,String referencia,long autor,LocalDateTime utc,String assinatura) {
        try(var livro=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            livro.getProperties().getCoreProperties().setCreator("SCOA"); livro.getProperties().getCoreProperties().setTitle(dados.titulo());
            var cabecalho=livro.createCellStyle(); var negrito=livro.createFont(); negrito.setBold(true); negrito.setColor(IndexedColors.WHITE.getIndex());
            cabecalho.setFont(negrito); cabecalho.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var texto=livro.createCellStyle(); texto.setWrapText(true); texto.setVerticalAlignment(VerticalAlignment.TOP);
            var meta=new ArrayList<List<String>>();
            meta.add(List.of("Relatório",dados.titulo())); meta.add(List.of("Referência",referencia)); meta.add(List.of("Conta autora",Long.toString(autor)));
            meta.add(List.of("Gerado em UTC",utc.toString())); meta.add(List.of("SHA-256 dos dados",assinatura));
            for(var p:dados.parametros()) meta.add(List.of(p.nome(),p.valor()));
            for(String aviso:dados.avisos()) meta.add(List.of("Aviso",aviso));
            planilha(livro,"Identificação",List.of("Campo","Valor"),meta,cabecalho,texto);
            int n=0;
            for(var t:dados.tabelas()) planilha(livro,WorkbookUtil.createSafeSheetName(String.format(Locale.ROOT,"%02d %s",++n,t.titulo())),t.colunas(),t.linhas(),cabecalho,texto);
            livro.write(out); return out.toByteArray();
        } catch(IOException e) { throw new IllegalArgumentException("Não foi possível gerar a planilha; nenhum arquivo parcial foi concluído.",e); }
    }
    private static void planilha(XSSFWorkbook livro,String nome,List<String> colunas,List<List<String>> linhas,CellStyle cabecalho,CellStyle texto) {
        var sheet=livro.createSheet(nome); var header=sheet.createRow(0);
        for(int i=0;i<colunas.size();i++) { var cell=header.createCell(i,CellType.STRING); cell.setCellValue(colunas.get(i)); cell.setCellStyle(cabecalho); }
        int n=0;
        for(var linha:linhas) {
            var row=sheet.createRow(++n);
            for(int i=0;i<linha.size();i++) {
                String valor=linha.get(i);
                if(valor.length()>32767) throw new IllegalArgumentException("Campo excede o limite do XLSX; refine o relatório sem truncar os dados.");
                var cell=row.createCell(i,CellType.STRING); cell.setCellValue(valor); cell.setCellStyle(texto);
            }
        }
        sheet.createFreezePane(0,1); sheet.setAutoFilter(new CellRangeAddress(0,n,0,colunas.size()-1));
        for(int i=0;i<colunas.size();i++) sheet.setColumnWidth(i,Math.min(65,Math.max(18,colunas.get(i).length()+5))*256);
    }
}

package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExportadorRelatorioAcademicoTest {
    private static final LocalDateTime DATA=LocalDateTime.of(2026,11,25,18,0);
    private static final String HASH="a".repeat(64);
    private Documento documento(List<List<String>> linhas) {
        return new Documento("Diário de João & ação acadêmica",List.of(new Parametro("Filtros","Curso 1 < período 2 >")),
                List.of("DEMONSTRATIVO DO PROTÓTIPO; sem assinatura oficial."),List.of(new Tabela("Notas e situação",List.of("Identificação","Valor","Observação"),linhas)));
    }
    @Test void codecPreservaAcentosReservadosQuebrasDeLinhaEListas() {
        var d=documento(List.of(List.of("João <&> \"texto\"","0.00","Linha 1\r\nLinha 2\rFim")));
        assertEquals(d,CodecRelatorioAcademico.ler(CodecRelatorioAcademico.escrever(d)));
        assertThrows(UnsupportedOperationException.class,()->d.tabelas().getFirst().linhas().getFirst().set(0,"Alteração"));
    }
    @Test void codecRecusaEntidadesExternasEControlesSemReinterpretarConteudo() {
        assertThrows(IllegalArgumentException.class,()->CodecRelatorioAcademico.ler("<!DOCTYPE relatorio [<!ENTITY x SYSTEM 'file:///nao-ler'>]><relatorio><titulo>&x;</titulo></relatorio>"));
        assertThrows(IllegalArgumentException.class,()->CodecRelatorioAcademico.escrever(documento(List.of(List.of("Nome","0","texto\u0000inválido")))));
    }
    @Test void pdfRealPreservaAcentosValoresEPaginasAteUltimoRegistro() throws Exception {
        var linhas=new ArrayList<List<String>>();
        for(int i=1;i<=48;i++) linhas.add(List.of("Aluno sintético "+i,i==1?"0.00":"SEM_REGISTRO","Conteúdo de avaliação com acentuação e acompanhamento acadêmico. ".repeat(3)+"MARCADOR_FINAL_"+i));
        var d=documento(linhas); byte[] pdf=ExportadorRelatorioAcademico.pdf(d,"QA-RELATORIO",7,DATA,HASH);
        byte[] xlsx=ExportadorRelatorioAcademico.xlsx(d,"QA-RELATORIO",7,DATA,HASH);
        var pasta=Path.of("target","qa-relatorios"); Files.createDirectories(pasta);
        Files.write(pasta.resolve("relatorio_sintetico.pdf"),pdf); Files.write(pasta.resolve("relatorio_sintetico.xlsx"),xlsx);
        try(var doc=Loader.loadPDF(pdf)) {
            assertTrue(doc.getNumberOfPages()>1); String texto=new PDFTextStripper().getText(doc);
            assertTrue(texto.contains("João")); assertTrue(texto.contains("0.00")); assertTrue(texto.contains("SEM_REGISTRO"));
            for(int i=1;i<=48;i++) assertTrue(texto.contains("MARCADOR_FINAL_"+i),"Registro não exportado: "+i);
            assertEquals(d.titulo(),doc.getDocumentInformation().getTitle());
            var renderer=new PDFRenderer(doc);
            ImageIO.write(renderer.renderImageWithDPI(0,110),"PNG",pasta.resolve("pdf_primeira.png").toFile());
            ImageIO.write(renderer.renderImageWithDPI(doc.getNumberOfPages()-1,110),"PNG",pasta.resolve("pdf_ultima.png").toFile());
        }
    }
    @Test void xlsxMantemTextosQueParecemFormulasIdsComZerosECamposExtensos() throws Exception {
        var valores=List.of("=HYPERLINK(\"https://exemplo.invalid\")","00123","ação\r\n"+"x".repeat(10000));
        byte[] dados=ExportadorRelatorioAcademico.xlsx(documento(List.of(valores)),"REF-1",7,DATA,HASH);
        try(var livro=new XSSFWorkbook(new ByteArrayInputStream(dados))) {
            assertEquals(2,livro.getNumberOfSheets()); var sheet=livro.getSheetAt(1);
            for(int i=0;i<valores.size();i++) { var c=sheet.getRow(1).getCell(i); assertEquals(CellType.STRING,c.getCellType()); assertEquals(valores.get(i),c.getStringCellValue()); }
            assertNotNull(sheet.getPaneInformation());
        }
    }
    @Test void formatosRecusamConteudoQueNaoPodemPreservarSemTruncarOuTrocarSimbolos() {
        assertThrows(IllegalArgumentException.class,()->ExportadorRelatorioAcademico.pdf(documento(List.of(List.of("漢字","0","Texto"))),"REF-1",7,DATA,HASH));
        assertThrows(IllegalArgumentException.class,()->ExportadorRelatorioAcademico.xlsx(documento(List.of(List.of("Nome","0","x".repeat(32768)))),"REF-1",7,DATA,HASH));
    }
    @Test void arquivoNaoExpoeArrayMutavelDoOriginalArquivado() {
        byte[] bytes={1,2,3}; var a=new Arquivo("a.pdf","application/pdf",HASH,bytes); bytes[0]=9;
        byte[] copia=a.conteudo(); copia[1]=9; assertArrayEquals(new byte[]{1,2,3},a.conteudo());
    }
}

package br.edu.cefetmg.scoa.web;

import java.nio.charset.StandardCharsets;
import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.SegurancaJdbc;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApresentacaoWebTest {
    public record Exemplo(String identificacao,String conteudo) { }
    private FormularioWeb formulario(String s) { return FormularioWeb.decodificar(s.getBytes(StandardCharsets.UTF_8)); }
    @Test void entradaUtf8PreservaPortuguesCaracteresEspeciaisESenhaSemExibirSegredos() {
        var f=formulario("nome=Coordena%C3%A7%C3%A3o+acad%C3%AAmica&senha=+segredo%2B%26%3D+&csrf=segredo-csrf");
        assertEquals("Coordenação acadêmica",f.texto("nome")); assertEquals(" segredo+&= ",f.valores().get("senha"));
        assertFalse(f.conferiveis().containsKey("senha"));assertFalse(f.conferiveis().containsKey("csrf"));
    }
    @Test void recusaCamposRepetidosCodificacaoInvalidaECorpoExcessivo() {
        for(String s:List.of("id=1&id=2","id=%GG","id=%C3%28","id=%F0%80%80%80","id=%","=valor","id=1&&outro=2"))
            assertThrows(IllegalArgumentException.class,()->formulario(s),s);
        assertThrows(IllegalArgumentException.class,()->FormularioWeb.decodificar(new byte[131073]));
    }
    @Test void referenciasValoresEDatasInvalidasGeramErroDeCampo() {
        var f=formulario("id=-2&numero=abc&data=2026-02-30&decimal=texto&ids=1,x");
        assertThrows(IllegalArgumentException.class,()->f.id("id"));assertThrows(IllegalArgumentException.class,()->f.numero("numero"));
        assertThrows(IllegalArgumentException.class,()->f.data("data"));assertThrows(IllegalArgumentException.class,()->f.decimal("decimal"));
        assertThrows(IllegalArgumentException.class,()->f.ids("ids"));assertNull(f.opcional("vazio"));
    }
    @Test void textoDeUsuarioNuncaFormaHtmlExecutavelMesmoEmTabelasAninhadas() {
        String ataque="<script>alert('x')</script><img src=x onerror=alert(1)> & \"";
        var html=HtmlAcademico.dados(List.of(new Exemplo(ataque,ataque)));
        assertFalse(html.contains("<script>"));assertFalse(html.contains("<img"));assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&amp;"));assertTrue(html.contains("&quot;"));assertTrue(HtmlAcademico.pagina(ataque,""," ").contains("<html lang=pt-BR>"));
    }
    @Test void salaNovaSemIdEApresentadaSemFalhaOuReferenciaInventada() {
        var html=HtmlAcademico.dados(Sala.nova("Sala A","Bloco B",20));assertTrue(html.contains("Sala A"));assertTrue(html.contains("Não informado"));
    }
    @Test void catalogoExigeVinculoAcademicoMesmoQuandoContaTemAdminTecnico() {
        var catalogo=AcoesAcademicasWeb.criar();
        var admin=new SegurancaJdbc.ContaAcesso(1,"Admin","ATIVO",List.of(),List.of(SegurancaJdbc.ADMIN));
        var ids=catalogo.disponiveis(admin).stream().map(AcoesAcademicasWeb.Acao::id).toList();
        assertFalse(ids.stream().anyMatch(x->x.startsWith("aluno-")||x.startsWith("docente-")||x.startsWith("gestao-")||x.startsWith("coordenacao-")));
        assertTrue(ids.contains("parametros-criar"));assertTrue(ids.contains("admin-concessao"));
        var aluno=new SegurancaJdbc.ContaAcesso(2,"Aluno","ATIVO",List.of(new SegurancaJdbc.VinculoAcademico("ALUNO",100,"Aluno","ATIVO")),List.of());
        assertTrue(catalogo.disponiveis(aluno).stream().allMatch(x->x.id().startsWith("aluno-")||x.id().startsWith("relatorio-")||x.id().equals("relatorios-arquivados")));
    }
    @Test void relatorioArquivadoApresentaLinksDosArquivosPreservados() {
        var d=new RelatorioAcademico.Documento("Histórico",List.of(),List.of(),List.of());
        var r=new RelatorioAcademico.Registro(12,"REF",new RelatorioAcademico.Filtro(RelatorioAcademico.Tipo.HISTORICO_PROPRIO,null,null,null,null),2,java.time.LocalDateTime.of(2026,11,25,20,0),"a".repeat(64),d);
        var html=HtmlAcademico.dados(List.of(r));assertTrue(html.contains("/arquivo?relatorio=12&amp;formato=PDF"));assertTrue(html.contains("formato=XLSX"));
    }
    @Test void boletimMostraPendenciasSemInventarNotaOuFalta() {
        var a=new DiarioAcademico.Avaliacao(3,2,"P1",java.time.LocalDate.of(2026,11,20),"Prova");
        var b=new DiarioAcademico.Boletim(1,2,300,0,0,0,List.of(new DiarioAcademico.Nota(a,null,null,null,null)),List.of(),List.of("P2 pendente"),null,false);
        var html=HtmlAcademico.dados(b);assertTrue(html.contains("Sem lançamento"));assertTrue(html.contains("P2 pendente"));assertFalse(html.contains("Prévia do resultado"));assertTrue(html.contains("não equivale a nota zero ou falta"));
    }
    @Test void avaliacaoLegadaSemTipoOuDataEExibidaSemAtribuicaoInventada() {
        var a=new DiarioAcademico.Avaliacao(3,2,null,null,"Legado preservado");
        var b=new DiarioAcademico.Boletim(1,2,300,0,0,0,List.of(new DiarioAcademico.Nota(a,null,null,null,null)),List.of(),List.of("Conferir legado"),null,false);
        var html=HtmlAcademico.dados(b);assertTrue(html.contains("Não identificado"));assertTrue(html.contains("Não informada"));assertTrue(html.contains("Conferir legado"));
    }
}

package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelatorioAcademicoTest {
    @Test void filtrosNaoAceitamPessoaInformadaParaConsultaPropriaNemContextoIncompativel() {
        assertDoesNotThrow(()->new Filtro(Tipo.HISTORICO_PROPRIO,null,null,null,null));
        assertThrows(IllegalArgumentException.class,()->new Filtro(Tipo.HISTORICO_PROPRIO,1L,null,null,null));
        assertThrows(IllegalArgumentException.class,()->new Filtro(Tipo.CENSO_DEMONSTRATIVO,1L,null,null,null));
        assertThrows(IllegalArgumentException.class,()->new Filtro(Tipo.DIARIO_DOCENTE,null,null,0L,null));
    }
    @Test void geracaoExigePreviaEReferenciasEArquivamentoExigeTabelasCoerentes() {
        var f=new Filtro(Tipo.HISTORICO_PROPRIO,null,null,null,null);
        assertThrows(IllegalArgumentException.class,()->new Geracao("REF",f,"assinatura inválida"));
        assertThrows(IllegalArgumentException.class,()->new Geracao("referência inválida",f,"a".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->new Tabela("Tabela",List.of("Coluna"),List.of(List.of("1","2"))));
    }
}

package br.edu.cefetmg.scoa.dominio;

import org.junit.jupiter.api.Test;
import br.edu.cefetmg.scoa.dominio.DocumentoAcademico.*;
import static org.junit.jupiter.api.Assertions.*;

class DocumentoAcademicoTest {
    @Test void entregaExigeReferenciaEParecerComLimitesUnicode() {
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(1,0,Situacao.ATENDIDO,"Parecer",null));
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(1,0,Situacao.INDEFERIDO," ",null));
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(1,0,Situacao.EM_ATENDIMENTO,"Parecer","Entrega"));
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(1,0,Situacao.ATENDIDO,"x".repeat(4001),"Entrega"));
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(1,0,Situacao.ATENDIDO,"Parecer","x".repeat(501)));
        var a=new Atualizacao(1,0,Situacao.ATENDIDO," 😀 ","😀".repeat(500));
        assertEquals("😀",a.parecer()); assertEquals(500,a.referenciaEntrega().codePointCount(0,a.referenciaEntrega().length()));
    }
    @Test void somenteProcessoAbertoAdmiteNovoAndamento() {
        for(var proxima:new Situacao[]{Situacao.EM_ATENDIMENTO,Situacao.ATENDIDO,Situacao.INDEFERIDO})
            assertDoesNotThrow(()->DocumentoAcademico.exigirTransicao("SOLICITADO",proxima));
        assertThrows(IllegalArgumentException.class,()->DocumentoAcademico.exigirTransicao("ATENDIDO",Situacao.INDEFERIDO));
        assertThrows(IllegalArgumentException.class,()->DocumentoAcademico.exigirTransicao("INDEFERIDO",Situacao.ATENDIDO));
        assertThrows(IllegalArgumentException.class,()->DocumentoAcademico.exigirTransicao("LEGADO",Situacao.ATENDIDO));
        assertThrows(IllegalArgumentException.class,()->DocumentoAcademico.exigirTransicao("EM_ATENDIMENTO",Situacao.EM_ATENDIMENTO));
    }
}

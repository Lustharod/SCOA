package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.TransferenciaExterna.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferenciaExternaTest {
    @Test void saidaObtemPessoaECursoDaMatriculaEEntradaExigeReferenciasAcademicas() {
        assertThrows(IllegalArgumentException.class,()->new Pedido("EXT-1",Tipo.SAIDA,2L,null,null,3L,1,"Instituição","Doc","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Pedido("EXT-1",Tipo.ENTRADA,2L,3L,null,null,1,"Instituição","Doc","Motivo"));
        var p=new Pedido(" EXT-1 ",Tipo.SAIDA,null,null,null,3L,1," Instituição "," Doc "," Motivo ");
        assertEquals("EXT-1",p.chave()); assertEquals("Instituição",p.instituicao()); assertNull(p.aluno());
    }
    @Test void referenciasEContextoNaoAceitamDadosAusentesOuIdsDeForaDoDominio() {
        assertThrows(IllegalArgumentException.class,()->new Pedido("EXT-1",Tipo.ENTRADA,2L,3L,4L,null,1,"Instituição"," ","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Pedido("EXT-1",Tipo.SAIDA,null,null,null,Long.MAX_VALUE,1,"Instituição","Doc","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new Pedido("inválida",Tipo.SAIDA,null,null,null,3L,1,"Instituição","Doc","Motivo"));
    }
    @Test void acompanhamentoExigeVersaoDocumentosEFundamentoSemPerderLimiteDoMotivo() {
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(-1,Situacao.EM_ANALISE,"Doc","Obs"));
        assertThrows(IllegalArgumentException.class,()->new Atualizacao(0,Situacao.EFETIVADA,"Doc"," "));
        var p=new Pedido("EXT-1",Tipo.ENTRADA,2L,3L,4L,null,1,"Instituição","Doc","m".repeat(500));
        assertEquals(500,p.motivo().length());
    }
}

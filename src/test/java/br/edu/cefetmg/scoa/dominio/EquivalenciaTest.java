package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.Equivalencia.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class EquivalenciaTest {
    @Test void pedidoExigeDocumentos() {
        assertThrows(IllegalArgumentException.class,()->new Pedido(1,2,"Instituição","Origem",60,"Ementa"," ",LocalDate.of(2020,1,1),"Destino"));
    }
    @Test void pedidoExigeCargaPositiva() {
        assertThrows(IllegalArgumentException.class,()->new Pedido(1,2,"Instituição","Origem",0,"Ementa","Protocolo",LocalDate.of(2020,1,1),"Destino"));
    }
    @Test void pedidoExigeConclusao() {
        assertThrows(IllegalArgumentException.class,()->new Pedido(1,2,"Instituição","Origem",60,"Ementa","Protocolo",null,"Destino"));
    }
    @Test void decisaoExigeParecerMesmoIndeferida() {
        assertThrows(IllegalArgumentException.class,()->new Decisao(false," ","Funcionário"));
    }
    @Test void decisaoExigeResponsavel() {
        assertThrows(IllegalArgumentException.class,()->new Decisao(true,"Compatibilidade analisada",null));
    }
    @Test void limiteContaUnicodeSemTruncar() {
        assertEquals("😀".repeat(255),new Decisao(true,"Parecer","😀".repeat(255)).responsavel());
        assertThrows(IllegalArgumentException.class,()->new Decisao(true,"Parecer","😀".repeat(256)));
    }
}

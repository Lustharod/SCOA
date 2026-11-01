package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatriculaTest {
    @Test void p0601LimitesEMotivos() {
        assertEquals("á🙂",Matricula.validarMotivo("  á🙂  "));
        assertEquals(1000,Matricula.validarMotivo("🙂".repeat(500)).length());
        assertThrows(IllegalArgumentException.class,()->Matricula.validarMotivo("🙂".repeat(501)));
        assertThrows(IllegalArgumentException.class,()->Matricula.validarMotivo(" "));
        assertThrows(IllegalArgumentException.class,()->Matricula.validarMotivo(null));
        assertThrows(IllegalArgumentException.class,()->Matricula.validarData(null));
        assertThrows(IllegalArgumentException.class,()->Matricula.validarData(LocalDate.of(999,12,31)));
        assertThrows(IllegalArgumentException.class,()->Matricula.validarData(LocalDate.of(10000,1,1)));
        assertEquals(LocalDate.of(9999,12,31),Matricula.validarData(LocalDate.of(9999,12,31)));
        assertThrows(IllegalArgumentException.class,()->new Matricula(null,0,1,LocalDate.now(),1L,"ATIVO"));
        assertThrows(IllegalArgumentException.class,()->new Matricula(null,1,1,LocalDate.now(),0L,"ATIVO"));
    }
    @Test void p0607MatrizCompletaDeTransicoes() {
        for(String origem:new String[]{"ATIVO","TRANCADO","CANCELADO","CONCLUIDO","LEGADO"})
            for(String destino:new String[]{"ATIVO","TRANCADO","CANCELADO","CONCLUIDO","LEGADO"}) {
                boolean permitido=(origem.equals("ATIVO")&&(destino.equals("TRANCADO")||destino.equals("CANCELADO")))
                        ||(origem.equals("TRANCADO")&&(destino.equals("ATIVO")||destino.equals("CANCELADO")));
                if(permitido) assertDoesNotThrow(()->Matricula.validarTransicao(origem,destino));
                else assertThrows(IllegalArgumentException.class,()->Matricula.validarTransicao(origem,destino));
            }
    }
    @Test void p0604RespostaInvalidaNaoLibera() {
        new SituacaoBiblioteca(SituacaoBiblioteca.Estado.REGULAR,"ok").exigirRegular();
        for(var estado:new SituacaoBiblioteca.Estado[]{SituacaoBiblioteca.Estado.IMPEDITIVA,SituacaoBiblioteca.Estado.INDISPONIVEL})
            assertThrows(IllegalArgumentException.class,()->new SituacaoBiblioteca(estado,"origem").exigirRegular());
    }
}

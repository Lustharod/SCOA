package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.GestaoAcademica.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GestaoAcademicaTest {
    @Test void prazoVencidoEDiagnosticoNaoMudaSituacaoRegistrada() {
        var hoje=LocalDate.of(2026,11,25);
        var m=new Meta(1,1,1,null,"META","Descrição",hoje.minusDays(1),"ABERTA",0,1L,hoje.atStartOfDay());
        assertTrue(m.atrasada(hoje)); assertEquals("ABERTA",m.situacao());
        var concluida=new Meta(1,1,1,null,"META","Descrição",hoje.minusDays(1),"CONCLUIDA",1,1L,hoje.atStartOfDay());
        assertFalse(concluida.atrasada(hoje)); assertFalse(m.atrasada(hoje.minusDays(1)));
    }
    @Test void metaExigeContextoDescricaoEPrazoValidoSemArredondamentoOuDataPresumida() {
        assertThrows(IllegalArgumentException.class,()->new NovaMeta(0,1,null,TipoMeta.META,"Descrição",LocalDate.now()));
        assertThrows(IllegalArgumentException.class,()->new NovaMeta(1,1,null,TipoMeta.PRAZO," ",LocalDate.now()));
        assertThrows(IllegalArgumentException.class,()->new NovaMeta(1,1,null,TipoMeta.PRAZO,"Descrição",LocalDate.of(999,1,1)));
        assertThrows(IllegalArgumentException.class,()->new AtualizarMeta(1,-1,"Descrição",LocalDate.now(),SituacaoMeta.ABERTA,"Motivo"));
    }
    @Test void auditoriaExigeRetratoConsultadoEObservacaoMasNaoRecebeNotasParaAlterar() {
        assertThrows(IllegalArgumentException.class,()->new NovaAuditoria(1,1,"sem prévia",ConclusaoAuditoria.COM_RESSALVAS,"Observações"));
        assertThrows(IllegalArgumentException.class,()->new NovaAuditoria(1,1,"a".repeat(64),ConclusaoAuditoria.SEM_RESSALVAS," "));
    }
}

package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtendimentoAlunoTest {
    @Test void manifestacaoExigeTipoEConteudoSemAceitarSoEspacos() {
        assertThrows(IllegalArgumentException.class,()->new NovaManifestacao(null,"Texto"));
        assertThrows(IllegalArgumentException.class,()->new NovaManifestacao(TipoManifestacao.SUGESTAO," \t"));
        assertEquals("Sugestão",new NovaManifestacao(TipoManifestacao.SUGESTAO,"  Sugestão  ").conteudo());
    }
    @Test void limiteDeConteudoContaCaracteresUnicodeSemTruncar() {
        String texto="😀".repeat(10000);
        assertEquals(texto,new NovaManifestacao(TipoManifestacao.RECLAMACAO,texto).conteudo());
        assertThrows(IllegalArgumentException.class,()->new NovaManifestacao(TipoManifestacao.SUGESTAO,texto+"a"));
    }
    @Test void historicoNaoPermiteAlteracaoPeloChamador() {
        var matriculas=new ArrayList<Vinculo>();
        var historico=new Historico(1,"Aluno",matriculas,List.of(),List.of(),List.of());
        matriculas.add(new Vinculo(1,"Curso","ATIVO",null));
        assertTrue(historico.matriculas().isEmpty());
        assertThrows(UnsupportedOperationException.class,()->historico.matriculas().clear());
    }
}

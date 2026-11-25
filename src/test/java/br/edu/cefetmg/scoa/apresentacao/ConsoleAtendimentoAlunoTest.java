package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoAtendimentoAluno;
import br.edu.cefetmg.scoa.dominio.RepositorioAtendimentoAluno;
import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleAtendimentoAlunoTest {
    private static final class Repo implements RepositorioAtendimentoAluno {
        int envios; boolean permitir=true;
        public void verificarAcesso() { if(!permitir) throw new IllegalArgumentException("Entre com uma sessão válida."); }
        public Historico historico() { verificarAcesso(); return new Historico(1,"Aluno de teste",List.of(),List.of(),List.of(),List.of()); }
        public Manifestacao registrar(NovaManifestacao p) { envios++; return new Manifestacao(27,1,2L,p.tipo().name(),p.conteudo(),LocalDateTime.now()); }
        public List<Manifestacao> manifestacoes() { verificarAcesso(); return List.of(); }
    }
    private static String executar(Repo repo,String entrada) {
        var bytes=new ByteArrayOutputStream(); var anterior=System.out;
        try(var saida=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setOut(saida);
            new ConsoleAtendimentoAluno(new Scanner(entrada),new ServicoAtendimentoAluno(repo)).executar();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally { System.setOut(anterior); }
    }
    @Test void confirmacaoEnviaUmaManifestacaoEExibeProtocolo() {
        var repo=new Repo();
        var texto=executar(repo,"2\n1\nSugestão sintética\nS\n0\n");
        assertEquals(1,repo.envios); assertTrue(texto.contains("Protocolo: 27"));
    }
    @Test void desistirOuEncerrarEntradaNaoEnviaConteudo() {
        var repo=new Repo();
        executar(repo,"2\n2\nReclamação sintética\nN\n0\n");
        executar(repo,"2\n2\nReclamação sintética\n");
        assertEquals(0,repo.envios);
    }
    @Test void sessaoInvalidaERecusadaAntesDePedirConteudo() {
        var repo=new Repo(); repo.permitir=false;
        var texto=executar(repo,"2\n0\n");
        assertTrue(texto.contains("sessão válida"));
        assertFalse(texto.contains("Conteúdo:")); assertEquals(0,repo.envios);
    }
    @Test void historicoSemRegistrosNaoApresentaNotasOuMatriculasInventadas() {
        var texto=executar(new Repo(),"1\n0\n");
        assertTrue(texto.contains("Nenhuma matrícula institucional"));
        assertTrue(texto.contains("não é documento oficial"));
        assertTrue(texto.contains("Ainda não existem disciplinas"));
    }
}

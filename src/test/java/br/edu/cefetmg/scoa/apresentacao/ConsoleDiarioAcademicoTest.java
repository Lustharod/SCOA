package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoDiarioAcademico;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleDiarioAcademicoTest {
    private static final class Repo implements RepositorioDiarioAcademico {
        int notas,retiradas,consultasAluno,consultasDocente;
        LancarNota ultimaNota;
        RetirarNota ultimaRetirada;
        final Boletim b=new Boletim(41,11,300,300,300,250,
                List.of(new Nota(new Avaliacao(7,11,"P1",LocalDate.of(2026,11,1),"Prova"),8L,new BigDecimal("8.0"),3,9L)),
                List.of(),List.of("P2 pendente."),null,false);
        public Boletim consultarAluno(long id) { consultasAluno++; return b; }
        public Boletim consultarDocente(long id) { consultasDocente++; return b; }
        public void lancarNota(LancarNota p) { notas++; ultimaNota=p; }
        public void retirarNota(RetirarNota p) { retiradas++; ultimaRetirada=p; }
        public List<Aula> registrarAulas(NovaAula p) { throw new UnsupportedOperationException(); }
        public List<Aula> aulas(long turma) { return List.of(); }
        public Avaliacao registrarAvaliacao(NovaAvaliacao p) { throw new UnsupportedOperationException(); }
        public List<Avaliacao> avaliacoes(long turma) { return List.of(); }
        public void lancarFrequencia(LancarFrequencia p) { throw new UnsupportedOperationException(); }
        public RegraResultado.Resultado fechar(long inscricao,String motivo) { throw new UnsupportedOperationException(); }
        public List<Processamento> importarSimulados(List<Chegada> eventos) { return List.of(); }
    }
    private static String executar(Repo repo,String entrada) {
        var bytes=new ByteArrayOutputStream(); var anterior=System.out;
        try(var saida=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setOut(saida); new ConsoleDiarioAcademico(new Scanner(entrada),new ServicoDiarioAcademico(repo)).executar();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally { System.setOut(anterior); }
    }
    @Test void cancelarOuEncerrarEntradaNaoAlteraNota() {
        var repo=new Repo(); executar(repo,"3\n41\n7\n9.0\nCorreção\nN\n0\n");
        executar(repo,"3\n41\n7\n9.0\nCorreção\n"); assertEquals(0,repo.notas);
    }
    @Test void notaERetiradaEnviamAVersaoConsultadaComConfirmacao() {
        var repo=new Repo(); executar(repo,"3\n41\n7\n9.0\nCorreção\nS\n0\n");
        assertEquals(1,repo.notas); assertEquals(3,repo.ultimaNota.versao()); assertEquals(41,repo.ultimaNota.inscricao());
        executar(repo,"9\n41\n7\nPF indevida\nS\n0\n");
        assertEquals(1,repo.retiradas); assertEquals(3,repo.ultimaRetirada.versao());
    }
    @Test void consultaDoAlunoUsaRotaPropriaEExibePendenciaSemResultadoInventado() {
        var repo=new Repo(); var tela=executar(repo,"7\n41\n0\n");
        assertEquals(1,repo.consultasAluno); assertEquals(0,repo.consultasDocente);
        assertTrue(tela.contains("P2 pendente")); assertFalse(tela.contains("Apuração dos lançamentos:"));
    }
}

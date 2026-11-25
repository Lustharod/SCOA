package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoPlanoInstitucional;
import br.edu.cefetmg.scoa.dominio.RepositorioPlanoInstitucional;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsolePlanoInstitucionalTest {
    private static final class Repo implements RepositorioPlanoInstitucional {
        int previas,registros,consultasProprias;
        public Previa preparar(Selecao s) { previas++; return new Previa(s,0,2,"a".repeat(64),List.of()); }
        public Versao registrar(Registro p) {
            registros++; return new Versao(8,p.selecao().matricula(),2,1,null,4,LocalDateTime.now(),p.motivo(),p.assinatura(),p.selecao().periodos(),p.selecao().prioridades(),List.of());
        }
        public Optional<Consulta> atualSecretaria(long m) { return Optional.empty(); }
        public Optional<Consulta> atualAluno(long m) { consultasProprias++; return Optional.empty(); }
        public List<Versao> historicoSecretaria(long m) { return List.of(); }
        public List<Versao> historicoAluno(long m) { return List.of(); }
    }
    private static String executar(Repo repo,String texto) {
        var bytes=new ByteArrayOutputStream(); var anterior=System.out;
        try(var saida=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setOut(saida); new ConsolePlanoInstitucional(new Scanner(texto),new ServicoPlanoInstitucional(repo)).executar();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally { System.setOut(anterior); }
    }
    @Test void cancelamentoOuFimDeEntradaPreservamPreviaSemRegistrarVersao() {
        var repo=new Repo(); executar(repo,"1\n1\n3\n\nMotivo\nN\n0\n");
        executar(repo,"1\n1\n3\n\nMotivo\n"); assertEquals(0,repo.registros); assertEquals(2,repo.previas);
        executar(repo,"1\n1\n3\n\nMotivo\nS\n0\n"); assertEquals(1,repo.registros);
    }
    @Test void consultaSemPlanoNaoPreparaOuRegistraAutomaticamente() {
        var repo=new Repo(); var tela=executar(repo,"4\n1\n0\n");
        assertEquals(1,repo.consultasProprias); assertEquals(0,repo.previas); assertEquals(0,repo.registros);
        assertTrue(tela.contains("Nenhum plano institucional registrado"));
    }
}

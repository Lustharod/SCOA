package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.dominio.ConsultasAluno;
import br.edu.cefetmg.scoa.dominio.FaixaHorario;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleConsultasAlunoTest {
    @Test
    void apresentaHorarioResultadoEInformacoesAindaIndisponiveis() {
        var consultas = new ConsultasAluno() {
            @Override
            public List<Horario> horarios() {
                return List.of(
                        new Horario(new TurmaInscrita(1, "Curso", 2, "Semestre", "Álgebra", "A"),
                                new FaixaHorario(1, LocalTime.of(8, 0), LocalTime.of(9, 0)), "Laboratório"),
                        new Horario(new TurmaInscrita(1, "Curso", 2, "Semestre", "Cálculo", "B"), null, null));
            }

            @Override
            public List<ResultadoFinal> resultadosFinais() {
                return List.of(new ResultadoFinal(new TurmaInscrita(1, "Curso", 1, "Anterior", "Lógica", "C"),
                        new BigDecimal("9.0"), new BigDecimal("7.0"), null,
                        new BigDecimal("8.0"), null, 110, 120, "APROVADO"));
            }
        };

        String saida = executar("1\n2\n0\n", consultas);
        assertTrue(saida.contains("segunda-feira, 08:00–09:00 | Sala Laboratório"));
        assertTrue(saida.contains("Turma B | Horário ainda não definido."));
        assertTrue(saida.contains("P1 9.0 | P2 7.0 | PF —"));
        assertTrue(saida.contains("Presença 110/120 min"));
    }

    @Test
    void informaNegacaoSemEncerrarSubmenu() {
        var consultas = new ConsultasAluno() {
            @Override
            public List<Horario> horarios() {
                throw new IllegalArgumentException("Entre com uma sessão válida no menu 18.");
            }

            @Override
            public List<ResultadoFinal> resultadosFinais() {
                return List.of();
            }
        };

        String saida = executar("1\n2\n", consultas);
        assertTrue(saida.contains("Entre com uma sessão válida no menu 18."));
        assertTrue(saida.contains("Nenhum resultado final registrado."));
    }

    private static String executar(String comandos, ConsultasAluno consultas) {
        var anterior = System.out;
        var bytes = new ByteArrayOutputStream();
        try (var saida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
             var scanner = new Scanner(comandos)) {
            System.setOut(saida);
            new ConsoleConsultasAluno(scanner, consultas).executar();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(anterior);
        }
    }
}

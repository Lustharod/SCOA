package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.dominio.ConsultasAluno;
import br.edu.cefetmg.scoa.dominio.ConsultasAluno.TurmaInscrita;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public final class ConsoleConsultasAluno {
    private final Scanner entrada;
    private final ConsultasAluno consultas;

    public ConsoleConsultasAluno(Scanner entrada, ConsultasAluno consultas) {
        this.entrada = entrada;
        this.consultas = consultas;
    }

    public void executar() {
        while (true) {
            System.out.println("\nConsultas do aluno | 1 Horários | 2 Notas e frequência finais | 0 Voltar");
            System.out.print("Opção: ");
            if (!entrada.hasNextLine()) return;
            try {
                switch (entrada.nextLine().strip()) {
                    case "0" -> { return; }
                    case "1" -> mostrarHorarios();
                    case "2" -> mostrarResultadosFinais();
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch (IllegalArgumentException | PersistenciaException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void mostrarHorarios() {
        var horarios = consultas.horarios();
        if (horarios.isEmpty()) System.out.println("Nenhuma inscrição ativa encontrada.");
        for (var horario : horarios) {
            var faixa = horario.faixa();
            if (faixa == null) {
                System.out.println(identificar(horario.turma()) + " | Horário ainda não definido.");
            } else {
                String dia = DayOfWeek.of(faixa.diaSemana()).getDisplayName(TextStyle.FULL,
                        Locale.forLanguageTag("pt-BR"));
                System.out.printf("%s | %s, %s–%s | Sala %s%n", identificar(horario.turma()),
                        dia, faixa.inicio(), faixa.fim(), Objects.toString(horario.sala(), "a definir"));
            }
        }
    }

    private void mostrarResultadosFinais() {
        var resultados = consultas.resultadosFinais();
        if (resultados.isEmpty()) System.out.println("Nenhum resultado final registrado.");
        for (var resultado : resultados) {
            System.out.printf("%s | %s | P1 %s | P2 %s | PF %s | Média inicial %s | Média final %s | Presença %d/%d min%n",
                    identificar(resultado.turma()), resultado.situacao(), resultado.p1(), resultado.p2(),
                    Objects.toString(resultado.pf(), "—"), resultado.mediaInicial(),
                    Objects.toString(resultado.mediaFinal(), "—"), resultado.minutosPresentes(), resultado.minutosTotais());
        }
    }

    private static String identificar(TurmaInscrita turma) {
        return "Período %d (%s) | Matrícula %d (%s) | %s | Turma %s".formatted(
                turma.periodo(), turma.descricaoPeriodo(), turma.matricula(), turma.curso(), turma.disciplina(), turma.turma());
    }
}

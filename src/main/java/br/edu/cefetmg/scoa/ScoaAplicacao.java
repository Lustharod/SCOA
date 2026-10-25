package br.edu.cefetmg.scoa;

import br.edu.cefetmg.scoa.aplicacao.ServicoSala;
import br.edu.cefetmg.scoa.aplicacao.ServicoAlocacao;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import br.edu.cefetmg.scoa.persistencia.ConexaoMySql;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import br.edu.cefetmg.scoa.persistencia.RepositorioSalaJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioAlocacaoJdbc;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Interface de console dos fluxos de salas e alocação implementados nesta etapa. */
public final class ScoaAplicacao {
    private ScoaAplicacao() { }

    public static void main(String[] args) {
        try (var conexao = ConexaoMySql.abrir(); var entrada = new Scanner(System.in)) {
            ServicoSala servico = new ServicoSala(new RepositorioSalaJdbc(conexao));
            ServicoAlocacao alocacao = new ServicoAlocacao(new RepositorioAlocacaoJdbc(conexao));
            while (true) {
                System.out.println("\nSCOA — Salas");
                System.out.println("1 Cadastrar | 2 Listar | 3 Consultar | 4 Atualizar | 5 Inativar | 0 Sair");
                System.out.println("6 Turmas | 7 Horários da turma | 8 Salas disponíveis | 9 Alocar sala | 10 Desalocar");
                String opcao = ler(entrada, "Opção: ");
                try {
                    switch (opcao) {
                        case "0" -> { return; }
                        case "1" -> {
                            Sala sala = servico.cadastrar(ler(entrada, "Identificação: "),
                                    ler(entrada, "Localização: "), inteiro(entrada, "Capacidade: "));
                            System.out.println("Sala cadastrada. Número: " + sala.getId());
                        }
                        case "2" -> {
                            var salas = servico.listar();
                            if (salas.isEmpty()) System.out.println("Nenhuma sala cadastrada.");
                            salas.forEach(ScoaAplicacao::mostrar);
                        }
                        case "3" -> mostrar(servico.buscar(identificador(entrada)));
                        case "4" -> {
                            long id = identificador(entrada);
                            Sala sala = servico.atualizar(id, ler(entrada, "Nova identificação: "),
                                    ler(entrada, "Nova localização: "), inteiro(entrada, "Nova capacidade: "));
                            System.out.println("Sala atualizada.");
                            mostrar(sala);
                        }
                        case "5" -> {
                            servico.inativar(identificador(entrada));
                            System.out.println("Sala inativada.");
                        }
                        case "6" -> alocacao.listarTurmas().forEach(t -> System.out.printf(
                                "%d | %s | capacidade %d | %s a %s%n", t.id(), t.codigo(), t.capacidade(), t.periodo().inicio(), t.periodo().fim()));
                        case "7" -> alocacao.listarHorarios(numero(entrada, "ID da turma: ")).forEach(h -> System.out.printf(
                                "%d | turma %d | sala %d | dia %d | %s–%s%n", h.id(), h.idTurma(), h.idSala(), h.faixa().diaSemana(), h.faixa().inicio(), h.faixa().fim()));
                        case "8", "9" -> {
                            long turma = numero(entrada, "ID da turma: ");
                            var dados = alocacao.buscarTurma(turma);
                            System.out.printf("Turma %s | capacidade %d | período %s a %s%n", dados.codigo(),
                                    dados.capacidade(), dados.periodo().inicio(), dados.periodo().fim());
                            var horarios = alocacao.listarHorarios(turma);
                            if (horarios.isEmpty()) System.out.println("A turma ainda não possui horários alocados.");
                            horarios.forEach(h -> System.out.printf("Alocação %d | sala %d | dia %d | %s–%s%n",
                                    h.id(),h.idSala(),h.faixa().diaSemana(),h.faixa().inicio(),h.faixa().fim()));
                            int dia = inteiro(entrada, "Dia da semana (1=segunda a 7=domingo): ");
                            LocalTime inicio = hora(entrada, "Início (HH:mm): ");
                            LocalTime fim = hora(entrada, "Fim (HH:mm): ");
                            var disponiveis = alocacao.listarDisponiveis(turma, dia, inicio, fim);
                            if (disponiveis.isEmpty()) System.out.println("Nenhuma sala disponível para esse horário e capacidade.");
                            disponiveis.forEach(ScoaAplicacao::mostrar);
                            if ("9".equals(opcao) && !disponiveis.isEmpty()) {
                                long sala = numero(entrada, "ID da sala selecionada: ");
                                var horario = alocacao.alocar(turma, sala, dia, inicio, fim);
                                System.out.println("Alocação registrada. Número: " + horario.id());
                            }
                        }
                        case "10" -> {
                            alocacao.desalocar(numero(entrada, "ID da alocação: "));
                            System.out.println("Alocação removida.");
                        }
                        default -> System.out.println("Escolha uma das opções do menu.");
                    }
                } catch (IllegalArgumentException | SalaNaoEncontradaException | PersistenciaException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Não foi possível abrir o banco. Confira o servidor e as configurações de conexão.");
        }
    }

    private static String ler(Scanner entrada, String mensagem) {
        System.out.print(mensagem);
        return entrada.hasNextLine() ? entrada.nextLine() : "0";
    }

    private static int inteiro(Scanner entrada, String mensagem) {
        try { return Integer.parseInt(ler(entrada, mensagem).strip()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }

    private static long identificador(Scanner entrada) {
        return numero(entrada, "Número da sala: ");
    }

    private static long numero(Scanner entrada, String mensagem) {
        try { return Long.parseLong(ler(entrada, mensagem).strip()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um identificador válido."); }
    }

    private static LocalTime hora(Scanner entrada, String mensagem) {
        try { return LocalTime.parse(ler(entrada, mensagem).strip()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Informe um horário válido, como 08:30."); }
    }

    private static void mostrar(Sala sala) {
        System.out.printf("%d | %s | %s | capacidade %d | %s%n", sala.getId(), sala.getIdentificacao(),
                sala.getLocalizacao(), sala.getCapacidade(), sala.getSituacao());
    }
}

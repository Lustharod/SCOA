package br.edu.cefetmg.scoa;

import br.edu.cefetmg.scoa.aplicacao.ServicoSala;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import br.edu.cefetmg.scoa.persistencia.ConexaoMySql;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import br.edu.cefetmg.scoa.persistencia.RepositorioSalaJdbc;
import java.sql.SQLException;
import java.util.Scanner;

/** Interface de console para exercitar a primeira funcionalidade durante o desenvolvimento. */
public final class ScoaAplicacao {
    private ScoaAplicacao() { }

    public static void main(String[] args) {
        try (var conexao = ConexaoMySql.abrir(); var entrada = new Scanner(System.in)) {
            ServicoSala servico = new ServicoSala(new RepositorioSalaJdbc(conexao));
            while (true) {
                System.out.println("\nSCOA — Salas");
                System.out.println("1 Cadastrar | 2 Listar | 3 Consultar | 4 Atualizar | 5 Inativar | 0 Sair");
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
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro para a capacidade."); }
    }

    private static long identificador(Scanner entrada) {
        try { return Long.parseLong(ler(entrada, "Número da sala: ").strip()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um número válido para a sala."); }
    }

    private static void mostrar(Sala sala) {
        System.out.printf("%d | %s | %s | capacidade %d | %s%n", sala.getId(), sala.getIdentificacao(),
                sala.getLocalizacao(), sala.getCapacidade(), sala.getSituacao());
    }
}

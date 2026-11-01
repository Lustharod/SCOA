package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoProfessoresDisciplinas;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.Scanner;

public final class ConsoleProfessoresDisciplinas {
    private final Scanner entrada;
    private final ServicoProfessoresDisciplinas servico;
    public ConsoleProfessoresDisciplinas(Scanner entrada, ServicoProfessoresDisciplinas servico) {
        this.entrada = entrada; this.servico = servico;
    }
    public void executar() {
        while (true) {
            System.out.println("\nSCOA — Professores e disciplinas");
            System.out.println("1 Listar professores | 2 Criar professor | 3 Consultar professor | 4 Atualizar professor | 5 Inativar professor");
            System.out.println("6 Listar disciplinas | 7 Criar disciplina | 8 Consultar disciplina | 9 Atualizar disciplina");
            System.out.println("10 Consultar pré-requisitos | 11 Adicionar pré-requisito | 12 Remover pré-requisito | 0 Voltar");
            try {
                switch (ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> {
                        var lista = servico.listarProfessores();
                        if (lista.isEmpty()) System.out.println("Nenhum professor cadastrado.");
                        lista.forEach(ConsoleProfessoresDisciplinas::mostrar);
                    }
                    case "2" -> { String nome = ler("Identificação do professor: "); if (confirmar()) mostrar(servico.cadastrarProfessor(nome)); }
                    case "3" -> mostrar(servico.buscarProfessor(numero("ID do professor: ")));
                    case "4" -> {
                        long id = numero("ID do professor: "); mostrar(servico.buscarProfessor(id));
                        String nome = ler("Identificação do professor: ");
                        if (confirmar()) mostrar(servico.atualizarProfessor(id, nome));
                    }
                    case "5" -> {
                        long id = numero("ID do professor: "); mostrar(servico.buscarProfessor(id));
                        if (confirmar()) mostrar(servico.inativarProfessor(id));
                    }
                    case "6" -> {
                        var lista = servico.listarDisciplinas();
                        if (lista.isEmpty()) System.out.println("Nenhuma disciplina cadastrada.");
                        lista.forEach(ConsoleProfessoresDisciplinas::mostrar);
                    }
                    case "7" -> editarDisciplina(null);
                    case "8" -> mostrar(servico.buscarDisciplina(numero("ID da disciplina: ")));
                    case "9" -> {
                        long id = numero("ID da disciplina: "); mostrar(servico.buscarDisciplina(id)); editarDisciplina(id);
                    }
                    case "10" -> {
                        long id = numero("ID da disciplina: "); mostrar(servico.buscarDisciplina(id));
                        var lista = servico.listarPreRequisitos(id);
                        if (lista.isEmpty()) System.out.println("Nenhum pré-requisito registrado.");
                        lista.forEach(ConsoleProfessoresDisciplinas::mostrar);
                    }
                    case "11" -> editarPreRequisito(true);
                    case "12" -> editarPreRequisito(false);
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch (IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
        }
    }
    private String ler(String mensagem) {
        System.out.print(mensagem);
        return entrada.hasNextLine() ? entrada.nextLine().strip() : "0";
    }
    private void editarDisciplina(Long id) {
        String codigo = ler("Código: "), nome = ler("Nome: ");
        long carga = numero("Carga horária: ");
        if (carga < 1 || carga > 65_535) throw new IllegalArgumentException("Carga horária fora dos limites do cadastro.");
        if (confirmar()) mostrar(id == null ? servico.cadastrarDisciplina(codigo, nome, (int)carga)
                : servico.atualizarDisciplina(id, codigo, nome, (int)carga));
    }
    private void editarPreRequisito(boolean adicionar) {
        long id = numero("ID da disciplina: "), requisito = numero("ID do pré-requisito: ");
        mostrar(servico.buscarDisciplina(id)); mostrar(servico.buscarDisciplina(requisito));
        if (confirmar()) {
            if (adicionar) servico.adicionarPreRequisito(id, requisito);
            else servico.removerPreRequisito(id, requisito);
            System.out.println(adicionar ? "Pré-requisito registrado." : "Pré-requisito removido.");
        }
    }
    private long numero(String mensagem) {
        try { return Long.parseLong(ler(mensagem)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }
    private boolean confirmar() {
        boolean sim = "S".equalsIgnoreCase(ler("Confirmar operação? (S/N): "));
        if (!sim) System.out.println("Operação cancelada.");
        return sim;
    }
    private static void mostrar(Professor p) { System.out.printf("Professor %d | %s | %s%n", p.id(), p.identificacao(), p.situacao()); }
    private static void mostrar(Disciplina d) { System.out.printf("Disciplina %d | %s | %s | carga horária %d | %s%n", d.id(), d.codigo(), d.nome(), d.cargaHoraria(), d.situacao()); }
}

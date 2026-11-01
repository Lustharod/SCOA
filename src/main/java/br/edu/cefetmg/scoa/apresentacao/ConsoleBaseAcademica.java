package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoBaseAcademica;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class ConsoleBaseAcademica {
    private final Scanner entrada;
    private final ServicoBaseAcademica servico;
    public ConsoleBaseAcademica(Scanner entrada, ServicoBaseAcademica servico) { this.entrada = entrada; this.servico = servico; }
    public void executar() {
        while (true) {
            System.out.println("\nSCOA — Alunos, cursos e estrutura curricular");
            System.out.println("1 Listar alunos | 2 Criar aluno | 3 Consultar aluno | 4 Atualizar aluno | 5 Inativar aluno");
            System.out.println("6 Listar cursos | 7 Criar curso | 8 Consultar curso | 9 Atualizar curso");
            System.out.println("10 Listar estruturas do curso | 11 Criar estrutura | 12 Consultar estrutura | 13 Atualizar identificação da estrutura");
            System.out.println("14 Consultar componentes | 15 Adicionar disciplina | 16 Retirar disciplina | 0 Voltar");
            try {
                switch (ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> { var lista = servico.listarAlunos(); if (lista.isEmpty()) System.out.println("Nenhum aluno cadastrado."); lista.forEach(ConsoleBaseAcademica::mostrar); }
                    case "2" -> { String nome = ler("Identificação do aluno: "); if (confirmar("Criar aluno: " + nome)) mostrar(servico.cadastrarAluno(nome)); }
                    case "3" -> mostrar(servico.buscarAluno(numero("ID do aluno: ")));
                    case "4" -> {
                        long id = numero("ID do aluno: "); mostrar(servico.buscarAluno(id)); String nome = ler("Nova identificação: ");
                        if (confirmar("Atualizar aluno " + id + " para: " + nome)) mostrar(servico.atualizarAluno(id, nome));
                    }
                    case "5" -> { long id = numero("ID do aluno: "); mostrar(servico.buscarAluno(id)); if (confirmar("Inativar aluno " + id)) mostrar(servico.inativarAluno(id)); }
                    case "6" -> { var lista = servico.listarCursos(); if (lista.isEmpty()) System.out.println("Nenhum curso cadastrado."); lista.forEach(ConsoleBaseAcademica::mostrar); }
                    case "7" -> { String nome = ler("Nome do curso: "); if (confirmar("Criar curso: " + nome)) mostrar(servico.cadastrarCurso(nome)); }
                    case "8" -> mostrar(servico.buscarCurso(numero("ID do curso: ")));
                    case "9" -> {
                        long id = numero("ID do curso: "); mostrar(servico.buscarCurso(id)); String nome = ler("Novo nome: ");
                        if (confirmar("Atualizar curso " + id + " para: " + nome)) mostrar(servico.atualizarCurso(id, nome));
                    }
                    case "10" -> {
                        long curso = numero("ID do curso: "); mostrar(servico.buscarCurso(curso)); var lista = servico.listarEstruturas(curso);
                        if (lista.isEmpty()) System.out.println("Nenhuma estrutura cadastrada para esse curso."); lista.forEach(ConsoleBaseAcademica::mostrar);
                    }
                    case "11" -> {
                        long curso = numero("ID do curso: "); mostrar(servico.buscarCurso(curso)); String nome = ler("Identificação da estrutura: ");
                        if (confirmar("Criar estrutura no curso " + curso + ": " + nome)) mostrar(servico.cadastrarEstrutura(curso, nome));
                    }
                    case "12" -> mostrar(servico.buscarEstrutura(numero("ID da estrutura: ")));
                    case "13" -> {
                        long id = numero("ID da estrutura: "); mostrar(servico.buscarEstrutura(id)); String nome = ler("Nova identificação: ");
                        if (confirmar("Atualizar estrutura " + id + " para: " + nome)) mostrar(servico.atualizarEstrutura(id, nome));
                    }
                    case "14" -> {
                        long id = numero("ID da estrutura: "); mostrar(servico.buscarEstrutura(id)); var lista = servico.listarComponentes(id);
                        if (lista.isEmpty()) System.out.println("Nenhuma disciplina componente."); lista.forEach(d -> System.out.printf("Disciplina %d | %s | %s | %s%n", d.id(), d.codigo(), d.nome(), d.situacao()));
                    }
                    case "15" -> editarComponente(true);
                    case "16" -> editarComponente(false);
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch (IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
            catch (NoSuchElementException e) { System.out.println("Entrada encerrada. Operação não confirmada cancelada."); return; }
        }
    }
    private String ler(String mensagem) {
        System.out.print(mensagem);
        if (!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().strip();
    }
    private long numero(String mensagem) {
        try { long valor = Long.parseLong(ler(mensagem)); HorarioAula.validarId(valor); return valor; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um identificador válido."); }
    }
    private boolean confirmar(String resumo) {
        System.out.println(resumo);
        if ("S".equalsIgnoreCase(ler("Confirma? (S/N): "))) return true;
        System.out.println("Operação cancelada."); return false;
    }
    private void editarComponente(boolean adicionar) {
        long estrutura = numero("ID da estrutura: "); mostrar(servico.buscarEstrutura(estrutura));
        long disciplina = numero("ID da disciplina (consulte no menu principal 12): ");
        if (confirmar((adicionar ? "Incluir" : "Retirar") + " disciplina " + disciplina + " na estrutura " + estrutura)) {
            if (adicionar) servico.adicionarComponente(estrutura, disciplina); else servico.removerComponente(estrutura, disciplina);
            System.out.println(adicionar ? "Componente incluído." : "Componente retirado.");
        }
    }
    private static void mostrar(Aluno a) { System.out.printf("Aluno %d | %s | %s%n", a.id(), a.identificacao(), a.situacao()); }
    private static void mostrar(Curso c) { System.out.printf("Curso %d | %s | %s%n", c.id(), c.nome(), c.situacao()); }
    private static void mostrar(EstruturaCurricular e) { System.out.printf("Estrutura %d | curso %d | %s | %s%n", e.id(), e.idCurso(), e.identificacao(), e.situacao()); }
}

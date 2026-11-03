package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoEquivalencia;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class ConsoleEquivalencia {
    private final Scanner entrada;
    private final ServicoEquivalencia servico;
    public ConsoleEquivalencia(Scanner entrada,ServicoEquivalencia servico) { this.entrada=entrada; this.servico=servico; }
    private String ler(String texto) {
        System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip();
    }
    private long numero(String texto) {
        try { return Long.parseLong(ler(texto)); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar registro? (S/N): ")); }
    public void executar() {
        while(true) {
            try {
                System.out.println("\nEquivalência manual | 1 Registrar pedido | 2 Listar pedidos | 3 Registrar decisão | 4 Plano básico | 5 Histórico acadêmico | 0 Voltar");
                switch(ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> {
                        long m=numero("ID da matrícula: "),d=numero("ID da disciplina de destino: ");
                        String instituicao=ler("Instituição de origem: "),origem=ler("Disciplina de origem: ");
                        int carga=Math.toIntExact(numero("Carga horária de origem (horas): "));
                        String ementa=ler("Ementa de origem considerada: ");
                        String documentos=ler("Referências dos documentos de conclusão, ementa e carga horária: ");
                        LocalDate conclusao=LocalDate.parse(ler("Conclusão de origem (AAAA-MM-DD): "));
                        String destino=ler("Ementa de destino considerada: ");
                        var pedido=new Pedido(m,d,instituicao,origem,carga,ementa,documentos,conclusao,destino);
                        System.out.println(pedido);
                        if(confirmar()) System.out.println("Pedido registrado: "+servico.solicitar(pedido));
                        else System.out.println("Registro cancelado.");
                    }
                    case "2" -> servico.listar(numero("ID da matrícula: ")).forEach(System.out::println);
                    case "3" -> {
                        long m=numero("ID da matrícula: "); var pedidos=servico.listar(m); pedidos.forEach(System.out::println);
                        long id=numero("ID do pedido: ");
                        if(pedidos.stream().noneMatch(p->p.id()==id)) throw new IllegalArgumentException("Pedido não pertence à matrícula consultada.");
                        String opcao=ler("Decisão manual (D=deferir / I=indeferir): ").toUpperCase(Locale.ROOT);
                        if(!Set.of("D","I").contains(opcao)) throw new IllegalArgumentException("Escolha D ou I.");
                        var decisao=new Decisao("D".equals(opcao),ler("Parecer sobre ementa e carga horária: "),ler("Funcionário administrativo responsável: "));
                        System.out.println("Responsável informado de forma declaratória. "+decisao);
                        System.out.println("Deferimento reflete no histórico e plano e pode cumprir pré-requisitos. Inscrições e resultados anteriores são preservados.");
                        if(confirmar()) System.out.println("Decisão registrada: "+servico.decidir(id,decisao));
                        else System.out.println("Decisão cancelada.");
                    }
                    case "4" -> {
                        System.out.println("Plano básico atual; não distribui disciplinas por períodos. EM_ANDAMENTO ainda não é cumprimento.");
                        var plano=servico.plano(numero("ID da matrícula: "));
                        System.out.printf("Matrícula %d | estrutura %d | situação %s%n",plano.matricula(),plano.estrutura(),plano.situacaoMatricula());
                        plano.itens().forEach(System.out::println);
                    }
                    case "5" -> servico.historico(numero("ID da matrícula: ")).forEach(System.out::println);
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch(NoSuchElementException e) { return; }
            catch(DateTimeParseException e) { System.out.println("Informe uma data válida, como 2026-01-31."); }
            catch(ArithmeticException e) { System.out.println("Carga horária fora do limite permitido."); }
            catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
        }
    }
}

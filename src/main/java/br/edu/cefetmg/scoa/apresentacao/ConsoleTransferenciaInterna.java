package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoTransferenciaInterna;
import br.edu.cefetmg.scoa.dominio.TransferenciaInterna.Pedido;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class ConsoleTransferenciaInterna {
    private final Scanner entrada;
    private final ServicoTransferenciaInterna servico;
    public ConsoleTransferenciaInterna(Scanner entrada,ServicoTransferenciaInterna servico) {
        this.entrada=entrada; this.servico=servico;
    }
    public void executar() {
        while(true) try {
            System.out.println("\nTransferência interna | 1 Efetivar mudança de curso | 2 Consultar processos da origem | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> {
                    long origem=id("ID da matrícula de origem: ");
                    long curso=id("ID do curso de destino: ");
                    long estrutura=id("ID da estrutura curricular do destino: ");
                    long periodo=id("ID do período da transferência: ");
                    var pedido=new Pedido(origem,curso,estrutura,periodo,ler("Motivo e referência da autorização acadêmica: "));
                    System.out.println("A origem será preservada como TRANSFERIDO e um novo vínculo será aberto no destino.");
                    System.out.println("Inscrições ativas serão canceladas até D+45, com a regra vigente de retenção de vagas.");
                    System.out.println("Somente aprovações formais da mesma disciplina cadastrada serão aproveitadas automaticamente.");
                    System.out.println(pedido);
                    if("S".equalsIgnoreCase(ler("Confirmar transferência? (S/N): ")))
                        System.out.println(servico.efetivar(origem,curso,estrutura,periodo,pedido.motivo()));
                    else System.out.println("Transferência cancelada antes da efetivação.");
                }
                case "2" -> servico.listar(id("ID da matrícula de origem: ")).forEach(System.out::println);
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private String ler(String texto) {
        System.out.print(texto);
        if(!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().strip();
    }
    private long id(String texto) {
        try {
            long id=Long.parseLong(ler(texto));
            br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(id); return id;
        } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID positivo válido."); }
    }
}

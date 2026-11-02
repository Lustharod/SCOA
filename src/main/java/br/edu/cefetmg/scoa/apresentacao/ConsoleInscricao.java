package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoInscricao;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.math.BigDecimal;
import java.util.*;

public final class ConsoleInscricao {
    private final Scanner entrada;
    private final ServicoInscricao servico;
    public ConsoleInscricao(Scanner entrada,ServicoInscricao servico) { this.entrada=entrada; this.servico=servico; }
    private String ler(String texto) {
        System.out.print(texto);
        if(!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().strip();
    }
    private long id(String texto) {
        try { return Long.parseLong(ler(texto)); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }
    private BigDecimal nota(String texto) {
        try { return new BigDecimal(ler(texto).replace(',','.')); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe uma nota válida."); }
    }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    public void executar() {
        while(true) {
            try {
                System.out.println("\nInscrição em disciplinas | 1 Inscrever | 2 Listar por matrícula | 3 Vincular estrutura curricular | 4 Registrar resultado final | 0 Voltar");
                switch(ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> {
                        long matricula=id("ID da matrícula: "),turma=id("ID da turma: ");
                        var e=servico.propor(matricula,turma);
                        System.out.printf("Matrícula %d | turma %d | vagas %d | janela %s a %s%n",matricula,turma,e.vagas(),e.janela().abertura(),e.janela().fimExclusivo().minusDays(1));
                        if(!e.permite()) { e.motivos().forEach(System.out::println); break; }
                        if(confirmar()) System.out.println("Inscrição registrada: "+servico.confirmar(matricula,turma));
                        else System.out.println("Operação cancelada.");
                    }
                    case "2" -> {
                        var lista=servico.listar(id("ID da matrícula: "));
                        if(lista.isEmpty()) System.out.println("Nenhuma inscrição registrada.");
                        lista.forEach(System.out::println);
                    }
                    case "3" -> {
                        long matricula=id("ID da matrícula: "),estrutura=id("ID da estrutura do curso: ");
                        System.out.printf("Vincular matrícula %d à estrutura %d. Não cria plano nem concede créditos.%n",matricula,estrutura);
                        if(confirmar()) { servico.vincularEstrutura(matricula,estrutura); System.out.println("Estrutura vinculada."); }
                        else System.out.println("Operação cancelada.");
                    }
                    case "4" -> {
                        long inscricao=id("ID da inscrição encerrada no calendário: ");
                        BigDecimal p1=nota("P1 (0 a 10): "),p2=nota("P2 (0 a 10): ");
                        String texto=ler("PF (vazio se não se aplica): ");
                        BigDecimal pf=texto.isEmpty()?null:new BigDecimal(texto.replace(',','.'));
                        long total=id("Total de minutos letivos efetivamente apurados: "),presentes=id("Minutos de presença: ");
                        String motivo=ler("Referência da apuração e motivo do fechamento: ");
                        var resultado=br.edu.cefetmg.scoa.dominio.RegraResultado.calcular(p1,p2,pf,total,presentes);
                        System.out.println("Inscrição "+inscricao+" | "+resultado+" | presença "+presentes+"/"+total+" minutos | "+motivo);
                        if(confirmar()) System.out.println("Resultado registrado: "+servico.registrarResultado(inscricao,p1,p2,pf,total,presentes,motivo));
                        else System.out.println("Operação cancelada.");
                    }
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch(NoSuchElementException e) { return; }
            catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
        }
    }
}

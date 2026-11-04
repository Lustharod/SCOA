package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.persistencia.*;
import br.edu.cefetmg.scoa.dominio.JanelaInscricao;
import java.util.*;

public final class ConsolePlanejamento {
    private final Scanner entrada;
    private final RepositorioPlanejamentoJdbc repo;
    public ConsolePlanejamento(Scanner entrada,RepositorioPlanejamentoJdbc repo) { this.entrada=entrada; this.repo=repo; }
    private String ler(String texto) { System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private List<Long> ids(String texto,boolean vazio) {
        String linha=ler(texto); if(vazio && linha.isEmpty()) return List.of();
        try { return Arrays.stream(linha.split(",",-1)).map(String::strip).map(Long::parseLong).toList(); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe IDs inteiros separados por vírgula."); }
    }
    public void executar() {
        while(true) try {
            System.out.println("\nPlanejamento por ofertas | 1 Listar períodos | 2 Simular distribuição | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> repo.periodos().forEach(System.out::println);
                case "2" -> {
                    long matricula;
                    try { matricula=Long.parseLong(ler("ID da matrícula: ")); }
                    catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um ID inteiro."); }
                    repo.periodos().forEach(System.out::println);
                    var periodos=ids("IDs dos períodos (separados por vírgula): ",false);
                    System.out.println("A prioridade antecipa disciplinas; as demais seguem por ID. Não há teto de carga automático.");
                    var prioridade=ids("IDs das disciplinas em ordem de prioridade (Enter = ordem por ID): ",true);
                    var resultado=repo.simular(matricula,periodos,prioridade);
                    System.out.printf("Simulação | matrícula %d | estrutura %d%n",resultado.matricula(),resultado.estrutura());
                    System.out.println("Somente pré-requisitos já cumpridos. Sugestões não liberam dependentes. Vagas observadas sem reserva; inscrição exige revalidação no menu 15.");
                    for(var item:resultado.itens()) {
                        System.out.printf("Disciplina %d | %s | %dh | %s | %s%n",item.disciplina().id(),item.disciplina().nome(),item.disciplina().horas(),item.disciplina().situacao(),item.motivo());
                        for(var tentativa:item.tentativas()) {
                            var o=tentativa.oferta(); var janela=JanelaInscricao.doPeriodo(o.periodo().inicio());
                            System.out.printf("  Turma %d | período %d (%s, %s a %s) | vagas observadas %d | horários %s | janela %s a %s%n",
                                    o.turma(),o.periodo().id(),o.periodo().descricao(),o.periodo().inicio(),o.periodo().fim(),o.vagas(),o.horarios(),janela.abertura(),janela.fimExclusivo().minusDays(1));
                            System.out.println("  "+(tentativa.impedimentos().isEmpty()?"SUGERIDA; janela não é avaliada como autorização atual.":String.join(" ",tentativa.impedimentos())));
                        }
                    }
                    System.out.println("Horas somente das sugestões por período (exclui inscrições em andamento): "+resultado.horasSugeridas());
                    System.out.println("Consulta encerrada sem gravar plano ou inscrição. A estratégia por prioridade não garante a melhor combinação.");
                }
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
          catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
}

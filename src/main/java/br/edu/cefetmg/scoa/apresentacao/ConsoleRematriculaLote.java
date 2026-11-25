package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoRematriculaLote;
import br.edu.cefetmg.scoa.dominio.RematriculaLote.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.*;

public final class ConsoleRematriculaLote {
    private final Scanner entrada; private final ServicoRematriculaLote servico;
    public ConsoleRematriculaLote(Scanner entrada,ServicoRematriculaLote servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nRematrícula em lote | 1 Preparar e processar | 2 Consultar lote | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> {
                    long curso=id("ID do curso: "),origem=id("ID do período anterior encerrado: "),destino=id("ID do período de destino: ");
                    String selecionadas=ler("IDs das matrículas separados por vírgula (Enter = todas as ativas do curso): ");
                    List<Long> matriculas;
                    try { matriculas=selecionadas.isEmpty()?List.of():Arrays.stream(selecionadas.split(",")).map(String::strip).map(Long::parseLong).toList(); }
                    catch(NumberFormatException e) { throw new IllegalArgumentException("Informe IDs numéricos separados por vírgula."); }
                    var previa=servico.preparar(new Selecao(curso,origem,destino,matriculas));
                    System.out.println("Chave do processamento: "+previa.chave()); previa.candidatos().forEach(ConsoleRematriculaLote::candidato);
                    System.out.println("O lote renova o vínculo. Cada inscrição em turma continua dependendo de confirmação separada. Sem turma sugerida disponível, toda a rematrícula fica para análise manual.");
                    String motivo=ler("Motivo do processamento: ");
                    if("S".equalsIgnoreCase(ler("Processar a prévia exibida? (S/N): "))) mostrar(servico.executar(previa,motivo));
                    else System.out.println("Processamento cancelado; nenhuma renovação foi feita.");
                }
                case "2" -> mostrar(servico.consultar(id("ID do lote: ")));
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private static void mostrar(Resultado r) {
        System.out.printf("Lote %d | chave %s | curso %d | destino %d | %s UTC%nRenovadas %d | análise manual %d | já renovadas %d%n",
                r.id(),r.chave(),r.curso(),r.destino(),r.data(),r.renovadas(),r.manuais(),r.jaRenovadas());
        r.candidatos().forEach(ConsoleRematriculaLote::candidato);
    }
    private static void candidato(Candidato c) {
        System.out.printf("Matrícula %d | %s | %s%n%s%n",c.matricula(),c.identificacao(),c.situacao(),c.mensagem());
        c.propostas().forEach(p->System.out.printf("  Disciplina %d | turma %s | %s | %s%n",p.disciplina(),Objects.toString(p.turma(),"sem sugestão"),p.situacao(),p.mensagem()));
    }
    private long id(String prompt) {
        try { return Long.parseLong(ler(prompt)); } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um ID numérico positivo."); }
    }
    private String ler(String prompt) { System.out.print(prompt); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
}

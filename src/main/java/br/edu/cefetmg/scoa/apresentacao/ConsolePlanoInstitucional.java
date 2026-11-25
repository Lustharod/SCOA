package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoPlanoInstitucional;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.*;

public final class ConsolePlanoInstitucional {
    private final Scanner entrada; private final ServicoPlanoInstitucional servico;
    public ConsolePlanoInstitucional(Scanner entrada,ServicoPlanoInstitucional servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nPlano institucional | 1 Preparar e registrar versão (secretaria) | 2 Consultar atual (secretaria) | 3 Histórico (secretaria) | 4 Meu plano | 5 Meu histórico de versões | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> registrar();
                case "2" -> consulta(servico.atualSecretaria(id()));
                case "3" -> servico.historicoSecretaria(id()).forEach(ConsolePlanoInstitucional::versao);
                case "4" -> consulta(servico.atualAluno(id()));
                case "5" -> servico.historicoAluno(id()).forEach(ConsolePlanoInstitucional::versao);
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void registrar() {
        long matricula=id();
        var previa=servico.preparar(new Selecao(matricula,lista("IDs dos períodos para planejamento (separados por vírgula): "),
                lista("Prioridade das disciplinas por ID (Enter para ordem padrão): ")));
        System.out.println("Prévia da próxima versão "+(previa.versaoAnterior()+1)+" | estrutura "+previa.estrutura());
        previa.componentes().forEach(ConsolePlanoInstitucional::componente);
        System.out.println("Sugestões não reservam vagas. Inscrição em turma depende de confirmação própria e nova validação.");
        String motivo=ler("Motivo da geração/atualização: ");
        if("S".equalsIgnoreCase(ler("Registrar esta versão? (S/N): "))) versao(servico.registrar(previa,motivo));
        else System.out.println("Registro cancelado; nenhuma versão criada.");
    }
    private static void consulta(Optional<Consulta> consulta) {
        if(consulta.isEmpty()) { System.out.println("Nenhum plano institucional registrado. A consulta não cria um plano."); return; }
        var c=consulta.orElseThrow(); System.out.println(c.atualidade()+": "+c.aviso()); versao(c.versao());
    }
    private static void versao(Versao v) {
        System.out.printf("Plano %d | matrícula %d | estrutura %d | versão %d | registrado em %s UTC%n",v.id(),v.matricula(),v.estrutura(),v.numero(),v.data());
        System.out.println("Motivo: "+v.motivo()); v.componentes().forEach(ConsolePlanoInstitucional::componente);
    }
    private static void componente(Componente c) {
        System.out.printf("%s - %s | %d horas | %s | turma sugerida %s | período %s%n",c.codigo(),c.nome(),c.horas(),c.situacao(),Objects.toString(c.turma(),"sem sugestão"),Objects.toString(c.descricaoPeriodo(),"sem sugestão"));
        System.out.println("  Fundamento: "+c.evidencia()); System.out.println("  Planejamento: "+c.diagnostico());
    }
    private List<Long> lista(String prompt) {
        String texto=ler(prompt); if(texto.isEmpty()) return List.of();
        try { return Arrays.stream(texto.split(",")).map(String::strip).map(Long::parseLong).toList(); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe IDs numéricos separados por vírgula."); }
    }
    private long id() {
        try { return Long.parseLong(ler("ID da matrícula institucional: ")); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um ID numérico positivo."); }
    }
    private String ler(String prompt) { System.out.print(prompt); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
}

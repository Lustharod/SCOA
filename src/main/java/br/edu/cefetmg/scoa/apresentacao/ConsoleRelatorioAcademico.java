package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoRelatorioAcademico;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class ConsoleRelatorioAcademico {
    private final Scanner entrada;
    private final ServicoRelatorioAcademico servico;
    public ConsoleRelatorioAcademico(Scanner entrada,ServicoRelatorioAcademico servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nRelatórios | 1 Conferir e gerar | 2 Consultar protocolo | 3 Meus relatórios autorizados | 4 Salvar PDF/XLSX arquivados | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> gerar();
                case "2" -> mostrar(servico.consultar(id("Protocolo: ")).documento());
                case "3" -> servico.meusRelatorios().forEach(r->System.out.printf("Protocolo %d | %s | referência %s | autor %d | UTC %s%n",r.id(),r.documento().titulo(),r.chave(),r.autor(),r.geradoEm()));
                case "4" -> salvar(id("Protocolo: "));
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void gerar() {
        System.out.println("1 Censo demonstrativo | 2 ENADE demonstrativo | 3 Diário do professor | 4 Diário da gestão | 5 Meu histórico | 6 Meu plano");
        Tipo tipo=switch(ler("Tipo: ")) {
            case "1" -> Tipo.CENSO_DEMONSTRATIVO; case "2" -> Tipo.ENADE_DEMONSTRATIVO; case "3" -> Tipo.DIARIO_DOCENTE;
            case "4" -> Tipo.DIARIO_GESTAO; case "5" -> Tipo.HISTORICO_PROPRIO; case "6" -> Tipo.PLANO_PROPRIO;
            default -> throw new IllegalArgumentException("Escolha um tipo exibido.");
        };
        Long curso=null,periodo=null,turma=null,matricula=null;
        switch(tipo) {
            case CENSO_DEMONSTRATIVO,ENADE_DEMONSTRATIVO -> { curso=id("ID do curso: "); periodo=id("ID do período: "); }
            case DIARIO_DOCENTE -> turma=id("ID da turma sob sua responsabilidade: ");
            case DIARIO_GESTAO -> { curso=id("ID do curso acompanhado: "); turma=id("ID da turma: "); }
            case PLANO_PROPRIO -> matricula=id("ID de sua matrícula institucional: ");
            case HISTORICO_PROPRIO -> System.out.println("A pessoa será identificada pela conta autenticada.");
        }
        var filtro=new Filtro(tipo,curso,periodo,turma,matricula); var previa=servico.preparar(filtro); mostrar(previa.documento());
        if(!previa.valida()) { System.out.println("Geração pendente de conferência:"); previa.inconsistencias().forEach(System.out::println); return; }
        System.out.println("A geração preservará o retrato e os dois formatos. Cancelar não gera protocolo.");
        if(!"S".equalsIgnoreCase(ler("Confirmar geração? (S/N): "))) { System.out.println("Geração cancelada antes da gravação."); return; }
        var r=servico.gerar(new Geracao(UUID.randomUUID().toString(),filtro,previa.assinatura()));
        System.out.printf("Relatório gerado: protocolo %d | referência %s | UTC %s%n",r.id(),r.chave(),r.geradoEm());
        if("S".equalsIgnoreCase(ler("Salvar agora os dois arquivos? (S/N): "))) salvar(r.id());
    }
    private void salvar(long id) {
        String caminho=ler("Pasta de destino (Enter = exportacoes): "); Path pasta=Path.of(caminho.isBlank()?"exportacoes":caminho).toAbsolutePath().normalize();
        try {
            Files.createDirectories(pasta);
            for(var formato:Formato.values()) {
                var arquivo=servico.arquivo(id,formato); Path destino=pasta.resolve(arquivo.nome());
                if(Files.exists(destino)) {
                    System.out.println(Arrays.equals(Files.readAllBytes(destino),arquivo.conteudo())?"Cópia original já presente: "+destino:"Arquivo existente diferente preservado; escolha outra pasta: "+destino);
                    continue;
                }
                Files.write(destino,arquivo.conteudo(),StandardOpenOption.CREATE_NEW);
                System.out.println("Salvo: "+destino+" | SHA-256 "+arquivo.sha256());
            }
        } catch(IOException | SecurityException e) { System.out.println("Cópia local não concluída. Relatório "+id+" permanece arquivado; escolha outra pasta na opção 4, sem repetir a geração."); }
    }
    private static void mostrar(Documento d) {
        System.out.println("\n"+d.titulo()); d.parametros().forEach(p->System.out.println(p.nome()+": "+p.valor())); d.avisos().forEach(System.out::println);
        for(var t:d.tabelas()) { System.out.println("\n"+t.titulo()+" | "+t.linhas().size()+" registros"); System.out.println(String.join(" | ",t.colunas())); t.linhas().forEach(l->System.out.println(String.join(" | ",l))); }
    }
    private String ler(String texto) { System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private long id(String texto) { try { long id=Long.parseLong(ler(texto)); HorarioAula.validarId(id); return id; } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID positivo válido."); } }
}

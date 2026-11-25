package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoDocumentoAcademico;
import br.edu.cefetmg.scoa.dominio.DocumentoAcademico.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.*;

public final class ConsoleDocumentoAcademico {
    private final Scanner entrada;
    private final ServicoDocumentoAcademico servico;
    public ConsoleDocumentoAcademico(Scanner entrada,ServicoDocumentoAcademico servico) {
        this.entrada=Objects.requireNonNull(entrada); this.servico=Objects.requireNonNull(servico);
    }
    public void executar() {
        while(true) try {
            System.out.println("\nDocumentos acadêmicos | 1 Solicitar | 2 Minhas solicitações | 3 Meu andamento | 4 Fila da secretaria | 5 Atualizar atendimento | 6 Andamento na secretaria | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> solicitar();
                case "2" -> mostrar(servico.minhasSolicitacoes());
                case "3" -> andamento(servico.meuAndamento(numero("Protocolo: ")));
                case "4" -> mostrar(servico.filaSecretaria());
                case "5" -> atualizar();
                case "6" -> andamento(servico.andamentoSecretaria(numero("Protocolo: ")));
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void solicitar() {
        var matriculas=servico.minhasMatriculas();
        if(matriculas.isEmpty()) { System.out.println("Nenhuma matrícula institucional vinculada à sua conta."); return; }
        matriculas.forEach(m->System.out.printf("Matrícula %d | %s | %s%n",m.matricula(),m.curso(),m.situacao()));
        long matricula=numero("ID da matrícula institucional: ");
        if(matriculas.stream().noneMatch(m->m.matricula()==matricula)) throw new IllegalArgumentException("Selecione uma das suas matrículas.");
        Tipo tipo=switch(ler("1 Declaração de matrícula | 2 Histórico escolar: ")) {
            case "1" -> Tipo.DECLARACAO_MATRICULA;
            case "2" -> Tipo.HISTORICO_ESCOLAR;
            default -> throw new IllegalArgumentException("Escolha 1 ou 2.");
        };
        if(confirmar()) {
            var registro=servico.solicitar(matricula,tipo);
            System.out.println("Solicitação registrada. Protocolo: "+registro.protocolo()+". Acompanhe o atendimento pela secretaria.");
        } else System.out.println("Solicitação cancelada antes do envio.");
    }
    private void atualizar() {
        var fila=servico.filaSecretaria(); mostrar(fila);
        if(fila.isEmpty()) return;
        long protocolo=numero("Protocolo a atualizar: ");
        var atual=fila.stream().filter(r->r.protocolo()==protocolo).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Selecione um protocolo da fila."));
        Situacao situacao=switch(ler("1 Em atendimento | 2 Atendido | 3 Indeferido: ")) {
            case "1" -> Situacao.EM_ATENDIMENTO;
            case "2" -> Situacao.ATENDIDO;
            case "3" -> Situacao.INDEFERIDO;
            default -> throw new IllegalArgumentException("Escolha 1, 2 ou 3.");
        };
        var pedido=new Atualizacao(protocolo,atual.versao(),situacao,ler("Parecer visível ao solicitante: "),
                situacao==Situacao.ATENDIDO?ler("Referência do documento entregue: "):null);
        if(confirmar()) mostrar(List.of(servico.atualizar(pedido)));
        else System.out.println("Atualização cancelada.");
    }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    private static void mostrar(List<Registro> lista) {
        if(lista.isEmpty()) System.out.println("Nenhuma solicitação registrada.");
        for(var r:lista) {
            System.out.printf("Protocolo %d | matrícula %d | %s | %s | %s | solicitado em %s%n",
                    r.protocolo(),r.matricula(),r.curso(),r.tipo(),r.situacao(),r.data());
            if(r.parecer()!=null) System.out.println("Parecer: "+r.parecer());
            if(r.referenciaEntrega()!=null) System.out.println("Referência de entrega: "+r.referenciaEntrega());
        }
    }
    private static void andamento(List<Andamento> lista) {
        if(lista.isEmpty()) System.out.println("Sem andamentos detalhados registrados. Dados legados não foram reconstruídos.");
        lista.forEach(a->System.out.printf("%s UTC | %s | %s%n",a.data(),a.situacao(),Objects.toString(a.parecer(),"Solicitação recebida.")));
    }
    private long numero(String prompt) {
        try { return Long.parseLong(ler(prompt)); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um ID numérico válido."); }
    }
    private String ler(String prompt) {
        System.out.print(prompt);
        if(!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().strip();
    }
}

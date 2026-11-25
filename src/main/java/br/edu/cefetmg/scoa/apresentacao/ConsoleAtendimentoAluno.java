package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoAtendimentoAluno;
import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

public final class ConsoleAtendimentoAluno {
    private final Scanner entrada;
    private final ServicoAtendimentoAluno servico;
    public ConsoleAtendimentoAluno(Scanner entrada,ServicoAtendimentoAluno servico) {
        this.entrada=Objects.requireNonNull(entrada); this.servico=Objects.requireNonNull(servico);
    }
    public void executar() {
        while(true) try {
            System.out.println("\nAtendimento ao aluno | 1 Meu histórico escolar | 2 Registrar sugestão/reclamação | 3 Minhas manifestações | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> mostrarHistorico(servico.historico());
                case "2" -> registrar();
                case "3" -> {
                    var lista=servico.manifestacoes();
                    if(lista.isEmpty()) System.out.println("Nenhuma manifestação registrada.");
                    lista.forEach(m->System.out.printf("Protocolo %d | %s | %s%n%s%n",m.id(),m.tipo(),m.data(),m.conteudo()));
                }
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void registrar() {
        servico.verificarAcesso();
        TipoManifestacao tipo=switch(ler("1 Sugestão | 2 Reclamação: ")) {
            case "1" -> TipoManifestacao.SUGESTAO;
            case "2" -> TipoManifestacao.RECLAMACAO;
            default -> throw new IllegalArgumentException("Escolha 1 ou 2.");
        };
        var pedido=new NovaManifestacao(tipo,ler("Conteúdo: "));
        System.out.println(pedido.tipo()+"\n"+pedido.conteudo());
        if("S".equalsIgnoreCase(ler("Confirmar envio? (S/N): "))) {
            var registro=servico.registrar(pedido.tipo(),pedido.conteudo());
            System.out.println("Manifestação registrada. Protocolo: "+registro.id());
        } else System.out.println("Envio cancelado.");
    }
    private String ler(String mensagem) {
        System.out.print(mensagem);
        if(!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().strip();
    }
    private static void mostrarHistorico(Historico h) {
        System.out.println("Histórico escolar de "+h.identificacao()+". Consulta dos registros disponíveis; não é documento oficial.");
        if(h.matriculas().isEmpty()) System.out.println("Nenhuma matrícula institucional registrada.");
        for(var m:h.matriculas()) {
            System.out.printf("Matrícula %d | %s | %s | ingresso %s%n",m.matricula(),m.curso(),m.situacao(),m.ingresso());
            for(var d:h.disciplinas()) if(d.matricula()==m.matricula()) {
                System.out.printf("  Período %s | %s a %s | %s - %s | %d h | turma %s | inscrição %s%n",
                        d.descricaoPeriodo(),d.inicio(),d.fim(),d.codigo(),d.nome(),d.cargaHoraria(),d.turma(),d.situacaoInscricao());
                if(d.situacaoResultado()==null)
                    System.out.println("    Resultado final ainda não registrado. Nenhuma nota ou frequência foi presumida.");
                else if(!d.resultadoCoerente())
                    System.out.println("    Resultado e inscrição divergentes; informação preservada para conferência acadêmica.");
                else System.out.printf("    Resultado %s | média inicial %s | média final %s | presença %s/%s min%n",
                        d.situacaoResultado(),valor(d.mediaInicial()),valor(d.mediaFinal()),valor(d.minutosPresentes()),valor(d.minutosTotais()));
            }
            for(var p:h.reconhecimentos()) if(p.matricula()==m.matricula())
                System.out.printf("  Reconhecimento %d | %s | %s | revisão %s | vigente %s%n    Origem: %s / %s; conclusão %s%n",
                        p.pedido(),p.nome(),p.decisao(),p.revisao(),p.vigente()?"sim":"não",p.instituicao(),p.origem(),p.conclusao());
            for(var l:h.legados()) if(l.matricula()==m.matricula())
                System.out.printf("  Registro legado %d | %s | decisão original %s. Não constitui reconhecimento automático; consulte o processo formal.%n",
                        l.referencia(),l.disciplina(),l.decisao());
        }
        if(h.disciplinas().isEmpty() && h.reconhecimentos().isEmpty() && h.legados().isEmpty())
            System.out.println("Ainda não existem disciplinas ou reconhecimentos para apresentar.");
    }
    private static String valor(Object valor) { return Objects.toString(valor,"não informado"); }
}

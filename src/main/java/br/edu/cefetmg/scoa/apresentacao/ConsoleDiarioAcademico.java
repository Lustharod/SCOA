package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoDiarioAcademico;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ConsoleDiarioAcademico {
    private final Scanner entrada;
    private final ServicoDiarioAcademico servico;
    public ConsoleDiarioAcademico(Scanner entrada,ServicoDiarioAcademico servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nDiário | 1 Registrar aulas realizadas | 2 Cadastrar P1/P2/PF | 3 Lançar nota | 4 Lançar presença/falta | 5 Conferir diário do aluno (docente) | 6 Encerrar resultado | 7 Meus lançamentos (aluno) | 8 Importar chegadas simuladas | 9 Retirar nota lançada por engano | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> aulas();
                case "2" -> avaliacao();
                case "3" -> nota();
                case "4" -> frequencia();
                case "5" -> mostrar(servico.consultarDocente(id("ID da inscrição: ")));
                case "6" -> {
                    long inscricao=id("ID da inscrição: "); mostrar(servico.consultarDocente(inscricao));
                    String motivo=ler("Fundamento do primeiro fechamento: ");
                    if(confirmar()) System.out.println("Resultado registrado: "+servico.fechar(inscricao,motivo));
                }
                case "7" -> mostrar(servico.consultarAluno(id("ID da sua inscrição em disciplina: ")));
                case "8" -> simular();
                case "9" -> retirarNota();
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(DateTimeException e) { System.out.println("Informe datas AAAA-MM-DD e horários HH:mm válidos."); }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void aulas() {
        long turma=id("ID da turma: "); servico.aulas(turma).forEach(System.out::println);
        LocalDate data=LocalDate.parse(ler("Data das aulas realizadas (AAAA-MM-DD): "));
        for(var turno:TempoLetivo.Turno.values()) {
            System.out.print(turno+": ");
            TempoLetivo.doTurno(turno).forEach(t->System.out.print(t+" "+t.inicio()+"-"+t.fim()+"; ")); System.out.println();
        }
        var tempos=Arrays.stream(ler("Tempos separados por vírgula (ex.: M1,M2): ").split(","))
                .map(s->TempoLetivo.valueOf(s.strip().toUpperCase(Locale.ROOT))).toList();
        var pedido=new NovaAula(turma,data,tempos,ler("Pauta: "),ler("Conteúdo ministrado: "));
        if(confirmar()) servico.registrarAulas(pedido).forEach(a->System.out.println("Aula registrada: "+a));
    }
    private void avaliacao() {
        long turma=id("ID da turma: "); servico.avaliacoes(turma).forEach(System.out::println);
        var pedido=new NovaAvaliacao(turma,Prova.valueOf(ler("Prova (P1/P2/PF): ").toUpperCase(Locale.ROOT)),
                LocalDate.parse(ler("Data da avaliação (AAAA-MM-DD): ")),ler("Descrição: "));
        if(confirmar()) System.out.println("Avaliação cadastrada: "+servico.registrarAvaliacao(pedido));
    }
    private void nota() {
        long inscricao=id("ID da inscrição: "); var b=servico.consultarDocente(inscricao); mostrar(b);
        long avaliacao=id("ID da avaliação: ");
        var atual=b.notas().stream().filter(n->n.avaliacao().id()==avaliacao).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Selecione uma avaliação da turma exibida."));
        var pedido=new LancarNota(avaliacao,inscricao,new BigDecimal(ler("Nota de 0 a 10 (ex.: 7.5): ").replace(',','.')),
                atual.versao()==null?-1:atual.versao(),ler("Motivo do lançamento/alteração: "));
        if(confirmar()) { servico.lancarNota(pedido); System.out.println("Nota registrada."); }
    }
    private void frequencia() {
        long inscricao=id("ID da inscrição: "); var b=servico.consultarDocente(inscricao); mostrar(b);
        long aula=id("ID da aula/tempo: ");
        var atual=b.frequencias().stream().filter(f->f.aula().id()==aula).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Selecione uma aula da turma exibida."));
        var presenca=switch(ler("1 Presente | 2 Falta | 3 Deixar sem registro para conferência: ")) {
            case "1" -> TempoLetivo.Presenca.PRESENTE;
            case "2" -> TempoLetivo.Presenca.FALTA;
            case "3" -> TempoLetivo.Presenca.SEM_REGISTRO;
            default -> throw new IllegalArgumentException("Escolha 1, 2 ou 3.");
        };
        var pedido=new LancarFrequencia(aula,inscricao,presenca,atual.versao()==null?-1:atual.versao(),ler("Motivo: "));
        if(confirmar()) { servico.lancarFrequencia(pedido); System.out.println("Frequência registrada para este tempo."); }
    }
    private void retirarNota() {
        long inscricao=id("ID da inscrição: "); var b=servico.consultarDocente(inscricao); mostrar(b);
        long avaliacao=id("ID da avaliação cuja nota será retirada: ");
        var atual=b.notas().stream().filter(n->n.avaliacao().id()==avaliacao && n.valor()!=null).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Selecione uma avaliação com nota vigente."));
        var pedido=new RetirarNota(avaliacao,inscricao,atual.versao(),ler("Motivo da retirada: "));
        if(confirmar()) { servico.retirarNota(pedido); System.out.println("Lançamento retirado da apuração."); }
    }
    private void simular() {
        System.out.println("Importação fictícia, sem equipamento real. Cada linha corresponde a uma aula/tempo.");
        var eventos=new ArrayList<Chegada>();
        while(true) {
            String chave=ler("Chave única do evento (Enter para concluir o lote): "); if(chave.isEmpty()) break;
            String origem=ler("Identificação da origem simulada: ");
            long aula=id("ID da aula: "),inscricao=id("ID da inscrição: ");
            LocalDate dia=LocalDate.parse(ler("Data da chegada (AAAA-MM-DD): "));
            LocalTime hora=LocalTime.parse(ler("Hora da chegada (HH:mm): "));
            eventos.add(new Chegada(origem,chave,aula,inscricao,LocalDateTime.of(dia,hora)));
        }
        if(!eventos.isEmpty() && confirmar()) servico.importarSimulados(eventos).forEach(System.out::println);
    }
    private static void mostrar(Boletim b) {
        System.out.printf("Inscrição %d | turma %d | carga prevista %d min | aulas registradas %d min | considerados para sua frequência %d min | presentes apurados %d min%n",
                b.inscricao(),b.turma(),b.minutosPrevistos(),b.minutosRegistrados(),b.minutosConsiderados(),b.minutosPresentes());
        b.notas().forEach(n->System.out.printf("Avaliação %d | %s | %s | nota %s%n",n.avaliacao().id(),n.avaliacao().tipo(),n.avaliacao().data(),Objects.toString(n.valor(),"sem registro")));
        b.frequencias().forEach(f->System.out.printf("Aula %d | %s | %s | %s%n",f.aula().id(),f.aula().data(),f.aula().tempo(),
                f.considerada()?Objects.toString(f.situacao(),"SEM REGISTRO"):"Anterior à inscrição; fora do cálculo"));
        b.pendencias().forEach(p->System.out.println("Pendente: "+p));
        if(b.previa()!=null) System.out.println("Apuração dos lançamentos: "+b.previa());
        if(b.encerrado()) System.out.println("O resultado foi encerrado. Estes são os lançamentos originais; consulte o resultado vigente no menu 19 e eventual retificação no menu 20.");
    }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    private long id(String prompt) {
        try { long id=Long.parseLong(ler(prompt)); HorarioAula.validarId(id); return id; }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um ID numérico positivo."); }
    }
    private String ler(String prompt) {
        System.out.print(prompt); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip();
    }
}

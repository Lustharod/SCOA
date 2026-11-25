package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoHorarioAcademico;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.HorarioAcademico.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.*;

public final class ConsoleHorarioAcademico {
    private final Scanner entrada;
    private final ServicoHorarioAcademico servico;
    public ConsoleHorarioAcademico(Scanner entrada,ServicoHorarioAcademico servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nHorário da turma e sala | 1 Planejar tempos sem sala | 2 Listar horários | 3 Atribuir ou trocar sala | 4 Retirar sala mantendo horário | 5 Cancelar horário | 6 Histórico | 0 Voltar");
            String op=ler("Opção: ");
            switch(op) {
                case "0" -> { return; }
                case "1" -> {
                    long turma=id("ID da turma: "); int dia=inteiro("Dia (1=segunda a 7=domingo): ");
                    for(var t:TempoLetivo.values()) System.out.printf("%s %s–%s | ",t,t.inicio(),t.fim());
                    System.out.println(); var tempos=new ArrayList<TempoLetivo>();
                    for(String nome:ler("Tempos separados por vírgula (ex.: M1,M2): ").split("[,;\\s]+")) {
                        try { tempos.add(TempoLetivo.valueOf(nome.toUpperCase(Locale.ROOT))); }
                        catch(IllegalArgumentException e) { throw new IllegalArgumentException("Selecione os tempos exibidos."); }
                    }
                    var pedido=new Planejar(turma,dia,tempos,ler("Motivo: ")); System.out.println(pedido);
                    if(confirmar()) servico.planejar(pedido).forEach(ConsoleHorarioAcademico::mostrar);
                }
                case "2" -> servico.listar(id("ID da turma: ")).forEach(ConsoleHorarioAcademico::mostrar);
                case "3","4","5" -> {
                    var horarios=servico.listar(id("ID da turma: ")); horarios.forEach(ConsoleHorarioAcademico::mostrar);
                    long id=id("ID do horário: ");
                    var p=horarios.stream().filter(h->h.horario().id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Horário não pertence à turma exibida."));
                    Long sala=null;
                    if("3".equals(op)) {
                        servico.salasDisponiveis(id).forEach(s->System.out.printf("Sala %d | %s | capacidade %d%n",s.getId(),s.getIdentificacao(),s.getCapacidade()));
                        sala=id("ID da sala escolhida: ");
                    }
                    String motivo=ler("Motivo: ");
                    if("4".equals(op)) System.out.println("Dias e tempos serão mantidos; a sala ficará pendente.");
                    if("5".equals(op)) System.out.println("Horário cancelado será preservado. Turmas com inscrições/aulas recusam essa mudança de tempos.");
                    if(confirmar()) mostrar(switch(op) {
                        case "3" -> servico.atribuirSala(id,p.versao(),sala,motivo);
                        case "4" -> servico.retirarSala(id,p.versao(),motivo);
                        default -> servico.cancelar(id,p.versao(),motivo);
                    });
                }
                case "6" -> servico.historico(id("ID do horário: ")).forEach(System.out::println);
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private static void mostrar(Registro r) {
        var h=r.horario();
        System.out.printf("Horário %d | turma %d | dia %d | %s–%s | sala %s | %s | versão %d | autor %s | UTC %s%n",
                h.id(),h.idTurma(),h.faixa().diaSemana(),h.faixa().inicio(),h.faixa().fim(),
                h.idSala()==null?"pendente":h.idSala(),r.ativo()?"ativo":"cancelado",r.versao(),r.autor(),r.atualizadoEm());
    }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    private String ler(String texto) { System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private long id(String texto) {
        try { long id=Long.parseLong(ler(texto)); HorarioAula.validarId(id); return id; }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID positivo válido."); }
    }
    private int inteiro(String texto) {
        try { return Integer.parseInt(ler(texto)); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe número inteiro válido."); }
    }
}

package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoMatricula;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class ConsoleMatricula {
    private final Scanner entrada;
    private final ServicoMatricula servico;
    public ConsoleMatricula(Scanner entrada,ServicoMatricula servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) {
            System.out.println("\nSCOA — Matrícula institucional");
            System.out.println("1 Listar por aluno | 2 Consultar | 3 Criar | 4 Corrigir ingresso");
            System.out.println("5 Trancar | 6 Retornar | 7 Cancelar matrícula | 8 Histórico | 9 Consultar biblioteca | 0 Voltar");
            try {
                String opcao=ler("Opção: ");
                switch(opcao) {
                    case "0" -> { return; }
                    case "1" -> { var lista=servico.listar(numero("ID do aluno: ")); if(lista.isEmpty()) System.out.println("Nenhuma matrícula."); lista.forEach(ConsoleMatricula::mostrar); }
                    case "2" -> mostrar(servico.buscar(numero("ID da matrícula: ")));
                    case "3" -> {
                        long aluno=numero("ID do aluno: "),curso=numero("ID do curso: ");
                        LocalDate data=data(); long periodo=numero("ID do período de ingresso (menu principal 11): ");
                        String motivo=ler("Motivo: ");
                        if(confirmar("Criar matrícula: aluno " + aluno + ", curso " + curso + ", ingresso " + data + ", período " + periodo + ", motivo: " + motivo))
                            mostrar(servico.criar(aluno,curso,data,periodo,motivo));
                    }
                    case "4" -> {
                        long id=numero("ID da matrícula: "); mostrar(servico.buscar(id));
                        LocalDate data=data(); long periodo=numero("Novo ID do período de ingresso: "); String motivo=ler("Motivo: ");
                        if(confirmar("Corrigir ingresso para " + data + ", período " + periodo + ", motivo: " + motivo)) mostrar(servico.corrigir(id,data,periodo,motivo));
                    }
                    case "5","6","7" -> {
                        long id=numero("ID da matrícula: "); mostrar(servico.buscar(id)); String motivo=ler("Motivo: ");
                        String acao=switch(opcao) { case "5" -> "Trancar"; case "6" -> "Retornar"; default -> "Cancelar matrícula definitivamente"; };
                        if(confirmar(acao + " " + id + ", motivo: " + motivo)) mostrar(switch(opcao) {
                            case "5" -> servico.trancar(id,motivo); case "6" -> servico.retornar(id,motivo); default -> servico.cancelar(id,motivo);
                        });
                    }
                    case "8" -> {
                        var eventos=servico.historico(numero("ID da matrícula: "));
                        if(eventos.isEmpty()) System.out.println("Sem eventos nesta versão; nenhum histórico anterior foi reconstruído.");
                        eventos.forEach(e->System.out.printf("Evento %d | %s | %s | %s → %s%nIngresso %s / período %s → %s / período %s%nMotivo: %s%n",
                                e.id(),e.ocorridoEm(),e.operacao(),e.situacaoAnterior(),e.situacaoNova(),e.dataAnterior(),e.periodoAnterior(),e.dataNova(),e.periodoNovo(),e.motivo()));
                    }
                    case "9" -> { var resposta=servico.consultarBiblioteca(numero("ID do aluno: ")); System.out.println(resposta.estado() + ": " + resposta.detalhe()); }
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
            catch(NoSuchElementException e) { System.out.println("Entrada encerrada. Operação não confirmada cancelada."); return; }
        }
    }
    private String ler(String mensagem) { System.out.print(mensagem); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private long numero(String mensagem) {
        try { long id=Long.parseLong(ler(mensagem)); HorarioAula.validarId(id); return id; }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um identificador válido."); }
    }
    private LocalDate data() {
        try { return Matricula.validarData(LocalDate.parse(ler("Data de ingresso (AAAA-MM-DD): "))); }
        catch(DateTimeParseException e) { throw new IllegalArgumentException("Informe uma data válida no formato AAAA-MM-DD."); }
    }
    private boolean confirmar(String resumo) {
        System.out.println(resumo); if("S".equalsIgnoreCase(ler("Confirma? (S/N): "))) return true;
        System.out.println("Operação cancelada."); return false;
    }
    private static void mostrar(Matricula m) {
        System.out.printf("Matrícula %d | aluno %d | curso %d | %s%nIngresso %s | período %s%n",m.id(),m.idAluno(),m.idCurso(),m.situacao(),m.dataIngresso(),m.idPeriodoIngresso()==null?"não informado (legado)":m.idPeriodoIngresso());
    }
}

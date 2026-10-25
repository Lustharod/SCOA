package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoCadastroAcademico;
import br.edu.cefetmg.scoa.dominio.CadastroPeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.Turma;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class ConsoleCadastrosAcademicos {
    private final Scanner entrada;
    private final ServicoCadastroAcademico servico;
    public ConsoleCadastrosAcademicos(Scanner entrada,ServicoCadastroAcademico servico) {
        this.entrada=entrada; this.servico=servico;
    }
    public void executar() {
        while (true) {
            System.out.println("\nSCOA — Cadastros acadêmicos");
            System.out.println("1 Listar períodos | 2 Criar período | 3 Atualizar período | 4 Inativar período");
            System.out.println("5 Disciplinas e professores | 6 Criar turma | 7 Consultar turma | 8 Atualizar turma | 9 Inativar turma | 0 Voltar");
            try {
                switch (ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> periodos();
                    case "2" -> {
                        String descricao=ler("Descrição: ");
                        LocalDate inicio=data("Início (AAAA-MM-DD): "), fim=data("Fim (AAAA-MM-DD): ");
                        if (confirmar()) mostrar(servico.cadastrarPeriodo(descricao,inicio,fim));
                    }
                    case "3" -> {
                        long id=numero("ID do período: "); mostrar(servico.buscarPeriodo(id));
                        String descricao=ler("Descrição: ");
                        LocalDate inicio=data("Início (AAAA-MM-DD): "), fim=data("Fim (AAAA-MM-DD): ");
                        if (confirmar()) mostrar(servico.atualizarPeriodo(id,descricao,inicio,fim));
                    }
                    case "4" -> {
                        long id=numero("ID do período: "); mostrar(servico.buscarPeriodo(id));
                        if (confirmar()) mostrar(servico.inativarPeriodo(id));
                    }
                    case "5" -> referencias();
                    case "6" -> editarTurma(0);
                    case "8" -> {
                        long id=numero("ID da turma: ");
                        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(id);
                        editarTurma(id);
                    }
                    case "7" -> mostrar(servico.buscarTurma(numero("ID da turma: ")));
                    case "9" -> {
                        long id=numero("ID da turma: "); mostrar(servico.buscarTurma(id));
                        if (confirmar()) mostrar(servico.inativarTurma(id));
                    }
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch (IllegalArgumentException | PersistenciaException erro) { System.out.println(erro.getMessage()); }
        }
    }
    private void editarTurma(long id) {
        if (id!=0) mostrar(servico.buscarTurma(id));
        referencias(); periodos();
        long disciplina=numero("ID da disciplina: "), professor=numero("ID do professor: "), periodo=numero("ID do período: ");
        String codigo=ler("Código da turma: ");
        long capacidade=numero("Capacidade: ");
        if (capacidade<1 || capacidade>65_535) throw new IllegalArgumentException("Capacidade inválida.");
        if (confirmar()) mostrar(id==0?servico.cadastrarTurma(disciplina,professor,periodo,codigo,(int)capacidade)
                :servico.atualizarTurma(id,disciplina,professor,periodo,codigo,(int)capacidade));
    }
    private void periodos() {
        var periodos=servico.listarPeriodos();
        if (periodos.isEmpty()) System.out.println("Nenhum período cadastrado.");
        periodos.forEach(ConsoleCadastrosAcademicos::mostrar);
    }
    private void referencias() {
        System.out.println("Disciplinas ativas:");
        servico.listarDisciplinas().forEach(r -> System.out.printf("%d | %s%n",r.id(),r.descricao()));
        System.out.println("Professores ativos:");
        servico.listarProfessores().forEach(r -> System.out.printf("%d | %s%n",r.id(),r.descricao()));
    }
    private String ler(String mensagem) { System.out.print(mensagem); return entrada.hasNextLine()?entrada.nextLine().strip():"0"; }
    private long numero(String mensagem) {
        try { return Long.parseLong(ler(mensagem)); }
        catch (NumberFormatException erro) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }
    private LocalDate data(String mensagem) {
        try { return LocalDate.parse(ler(mensagem)); }
        catch (DateTimeParseException erro) { throw new IllegalArgumentException("Informe uma data válida no formato AAAA-MM-DD."); }
    }
    private boolean confirmar() {
        boolean sim="S".equalsIgnoreCase(ler("Confirmar operação? (S/N): "));
        if (!sim) System.out.println("Operação cancelada.");
        return sim;
    }
    private static void mostrar(CadastroPeriodoLetivo p) {
        System.out.printf("Período %d | %s | %s a %s | %s%n",p.id(),p.descricao(),p.datas().inicio(),p.datas().fim(),p.situacao());
    }
    private static void mostrar(Turma t) {
        System.out.printf("Turma %d | %s | disciplina %d | professor %d | período %d | capacidade %d | %s%n",
                t.id(),t.codigo(),t.idDisciplina(),t.idProfessor(),t.idPeriodo(),t.capacidade(),t.situacao());
    }
}

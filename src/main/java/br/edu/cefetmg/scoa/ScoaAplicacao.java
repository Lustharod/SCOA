package br.edu.cefetmg.scoa;

import br.edu.cefetmg.scoa.aplicacao.ServicoSala;
import br.edu.cefetmg.scoa.aplicacao.ServicoBaseAcademica;
import br.edu.cefetmg.scoa.apresentacao.ConsoleBaseAcademica;
import br.edu.cefetmg.scoa.persistencia.RepositorioBaseAcademicaJdbc;
import br.edu.cefetmg.scoa.aplicacao.ServicoAlocacao;
import br.edu.cefetmg.scoa.aplicacao.ServicoCadastroAcademico;
import br.edu.cefetmg.scoa.aplicacao.ServicoProfessoresDisciplinas;
import br.edu.cefetmg.scoa.apresentacao.ConsoleProfessoresDisciplinas;
import br.edu.cefetmg.scoa.persistencia.RepositorioProfessoresDisciplinasJdbc;
import br.edu.cefetmg.scoa.apresentacao.ConsoleCadastrosAcademicos;
import br.edu.cefetmg.scoa.persistencia.RepositorioCadastroAcademicoJdbc;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import br.edu.cefetmg.scoa.persistencia.ConexaoMySql;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import br.edu.cefetmg.scoa.persistencia.RepositoriosAcademicosAutorizados;
import br.edu.cefetmg.scoa.persistencia.RepositorioSalaJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioAlocacaoJdbc;
import br.edu.cefetmg.scoa.aplicacao.ServicoPlanejamento;
import br.edu.cefetmg.scoa.aplicacao.ServicoInscricao;
import br.edu.cefetmg.scoa.apresentacao.ConsoleInscricao;
import br.edu.cefetmg.scoa.apresentacao.ConsolePlanejamento;
import br.edu.cefetmg.scoa.persistencia.RepositorioPlanejamentoJdbc;
import br.edu.cefetmg.scoa.apresentacao.ConsoleConsultasAluno;
import br.edu.cefetmg.scoa.apresentacao.ConsoleAjustesAcademicos;
import br.edu.cefetmg.scoa.persistencia.RepositorioConsultasAlunoJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioInscricaoJdbc;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Interface de console dos fluxos de salas e alocação implementados nesta etapa. */
public final class ScoaAplicacao {
    private ScoaAplicacao() { }

    public static void main(String[] args) {
        try (var conexao = ConexaoMySql.abrir(); var entrada = new Scanner(System.in)) {
            br.edu.cefetmg.scoa.persistencia.SegurancaJdbc.Sessao sessao=null;
            while (true) {
                ServicoSala servico = new ServicoSala(RepositoriosAcademicosAutorizados.salas(conexao,sessao));
                ServicoAlocacao alocacao = new ServicoAlocacao(RepositoriosAcademicosAutorizados.alocacoes(conexao,sessao));
                System.out.println("\nSCOA — Módulo Acadêmico");
                System.out.println("1 Cadastrar | 2 Listar | 3 Consultar | 4 Atualizar | 5 Inativar | 0 Sair");
                System.out.println("6 Turmas | 7 Horários da turma | 8 Salas disponíveis | 9 Alocar sala | 10 Desalocar");
                System.out.println("11 Cadastros de períodos e turmas | 12 Professores, disciplinas e pré-requisitos");
                System.out.println("13 Alunos, cursos e estrutura curricular | 14 Matrícula institucional | 15 Inscrição em disciplinas");
                System.out.println("18 Acesso, conciliação e revisão de reconhecimentos");
                System.out.println("16 Equivalência manual, plano básico e histórico acadêmico | 17 Planejamento por ofertas");
                System.out.println("19 Consultas do aluno | 20 Ajustes acadêmicos | 21 Histórico escolar e manifestações | 22 Transferência interna");
                System.out.println("23 Solicitações de documentos e atendimento da secretaria");
                System.out.println("24 Diário: aulas por tempo, avaliações, notas e frequência");
                System.out.println("25 Plano de estudo institucional e versões");
                System.out.println("26 Rematrícula institucional em lote");
                System.out.println("27 Gestão acadêmica: metas, auditoria, avaliações e monitoramento");
                String opcao = ler(entrada, "Opção: ");
                try {
                    switch (opcao) {
                        case "0" -> { return; }
                        case "1" -> {
                            Sala sala = servico.cadastrar(ler(entrada, "Identificação: "),
                                    ler(entrada, "Localização: "), inteiro(entrada, "Capacidade: "));
                            System.out.println("Sala cadastrada. Número: " + sala.getId());
                        }
                        case "2" -> {
                            var salas = servico.listar();
                            if (salas.isEmpty()) System.out.println("Nenhuma sala cadastrada.");
                            salas.forEach(ScoaAplicacao::mostrar);
                        }
                        case "3" -> mostrar(servico.buscar(identificador(entrada)));
                        case "4" -> {
                            long id = identificador(entrada);
                            Sala sala = servico.atualizar(id, ler(entrada, "Nova identificação: "),
                                    ler(entrada, "Nova localização: "), inteiro(entrada, "Nova capacidade: "));
                            System.out.println("Sala atualizada.");
                            mostrar(sala);
                        }
                        case "5" -> {
                            servico.inativar(identificador(entrada));
                            System.out.println("Sala inativada.");
                        }
                        case "6" -> alocacao.listarTurmas().forEach(t -> System.out.printf(
                                "%d | %s | capacidade %d | %s a %s%n", t.id(), t.codigo(), t.capacidade(), t.periodo().inicio(), t.periodo().fim()));
                        case "7" -> alocacao.listarHorarios(numero(entrada, "ID da turma: ")).forEach(h -> System.out.printf(
                                "%d | turma %d | sala %d | dia %d | %s–%s%n", h.id(), h.idTurma(), h.idSala(), h.faixa().diaSemana(), h.faixa().inicio(), h.faixa().fim()));
                        case "8", "9" -> {
                            long turma = numero(entrada, "ID da turma: ");
                            var dados = alocacao.buscarTurma(turma);
                            System.out.printf("Turma %s | capacidade %d | período %s a %s%n", dados.codigo(),
                                    dados.capacidade(), dados.periodo().inicio(), dados.periodo().fim());
                            var horarios = alocacao.listarHorarios(turma);
                            if (horarios.isEmpty()) System.out.println("A turma ainda não possui horários alocados.");
                            horarios.forEach(h -> System.out.printf("Alocação %d | sala %d | dia %d | %s–%s%n",
                                    h.id(),h.idSala(),h.faixa().diaSemana(),h.faixa().inicio(),h.faixa().fim()));
                            int dia = inteiro(entrada, "Dia da semana (1=segunda a 7=domingo): ");
                            LocalTime inicio = hora(entrada, "Início (HH:mm): ");
                            LocalTime fim = hora(entrada, "Fim (HH:mm): ");
                            var disponiveis = alocacao.listarDisponiveis(turma, dia, inicio, fim);
                            if (disponiveis.isEmpty()) System.out.println("Nenhuma sala disponível para esse horário e capacidade.");
                            disponiveis.forEach(ScoaAplicacao::mostrar);
                            if ("9".equals(opcao) && !disponiveis.isEmpty()) {
                                long sala = numero(entrada, "ID da sala selecionada: ");
                                var horario = alocacao.alocar(turma, sala, dia, inicio, fim);
                                System.out.println("Alocação registrada. Número: " + horario.id());
                            }
                        }
                        case "10" -> {
                            alocacao.desalocar(numero(entrada, "ID da alocação: "));
                            System.out.println("Alocação removida.");
                        }
                        case "11" -> new ConsoleCadastrosAcademicos(entrada,
                                new ServicoCadastroAcademico(RepositoriosAcademicosAutorizados.cadastros(conexao,sessao))).executar();
                        case "12" -> new ConsoleProfessoresDisciplinas(entrada,
                                new ServicoProfessoresDisciplinas(RepositoriosAcademicosAutorizados.professoresDisciplinas(conexao,sessao))).executar();
                        case "18" -> sessao=new br.edu.cefetmg.scoa.apresentacao.ConsoleProcessoAcademico(entrada,conexao,sessao).executar();
                        case "19" -> new ConsoleConsultasAluno(entrada,
                                new RepositorioConsultasAlunoJdbc(conexao, sessao)).executar();
                        case "20" -> new ConsoleAjustesAcademicos(entrada,conexao,sessao).executar();
                        case "27" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleGestaoAcademica(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoGestaoAcademica(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioGestaoAcademicaJdbc(conexao,sessao))).executar();
                        case "26" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleRematriculaLote(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoRematriculaLote(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioRematriculaLoteJdbc(conexao,sessao))).executar();
                        case "25" -> new br.edu.cefetmg.scoa.apresentacao.ConsolePlanoInstitucional(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoPlanoInstitucional(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioPlanoInstitucionalJdbc(conexao,sessao))).executar();
                        case "24" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleDiarioAcademico(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoDiarioAcademico(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioDiarioAcademicoJdbc(conexao,sessao))).executar();
                        case "23" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleDocumentoAcademico(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoDocumentoAcademico(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioDocumentoAcademicoJdbc(conexao,sessao))).executar();
                        case "22" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleTransferenciaInterna(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoTransferenciaInterna(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioTransferenciaInternaJdbc(conexao,sessao))).executar();
                        case "21" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleAtendimentoAluno(entrada,
                                new br.edu.cefetmg.scoa.aplicacao.ServicoAtendimentoAluno(
                                    new br.edu.cefetmg.scoa.persistencia.RepositorioAtendimentoAlunoJdbc(conexao,sessao))).executar();
                        case "17" -> new ConsolePlanejamento(entrada,
                                new ServicoPlanejamento(RepositoriosAcademicosAutorizados.planejamento(conexao,sessao))).executar();
                        case "16" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleEquivalencia(entrada, new br.edu.cefetmg.scoa.aplicacao.ServicoEquivalencia(RepositoriosAcademicosAutorizados.equivalencias(conexao,sessao))).executar();
                        case "15" -> {
                            var repo=new RepositorioInscricaoJdbc(conexao);
                            var sessaoAtual=sessao;
                            new ConsoleInscricao(entrada,new ServicoInscricao(RepositoriosAcademicosAutorizados.inscricoes(conexao,sessao)),
                                    (matricula,turma)->repo.inscreverComoAluno(sessaoAtual,matricula,turma)).executar();
                        }
                        case "14" -> new br.edu.cefetmg.scoa.apresentacao.ConsoleMatricula(entrada, new br.edu.cefetmg.scoa.aplicacao.ServicoMatricula(RepositoriosAcademicosAutorizados.matriculas(conexao,sessao))).executar();
                        case "13" -> new ConsoleBaseAcademica(entrada,
                                new ServicoBaseAcademica(RepositoriosAcademicosAutorizados.baseAcademica(conexao,sessao))).executar();
                        default -> System.out.println("Escolha uma das opções do menu.");
                    }
                } catch (IllegalArgumentException | SalaNaoEncontradaException | PersistenciaException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Não foi possível abrir o banco. Confira o servidor e as configurações de conexão.");
        }
    }

    private static String ler(Scanner entrada, String mensagem) {
        System.out.print(mensagem);
        return entrada.hasNextLine() ? entrada.nextLine() : "0";
    }

    private static int inteiro(Scanner entrada, String mensagem) {
        try { return Integer.parseInt(ler(entrada, mensagem).strip()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }

    private static long identificador(Scanner entrada) {
        return numero(entrada, "Número da sala: ");
    }

    private static long numero(Scanner entrada, String mensagem) {
        try { return Long.parseLong(ler(entrada, mensagem).strip()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Informe um identificador válido."); }
    }

    private static LocalTime hora(Scanner entrada, String mensagem) {
        try { return LocalTime.parse(ler(entrada, mensagem).strip()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Informe um horário válido, como 08:30."); }
    }

    private static void mostrar(Sala sala) {
        System.out.printf("%d | %s | %s | capacidade %d | %s%n", sala.getId(), sala.getIdentificacao(),
                sala.getLocalizacao(), sala.getCapacidade(), sala.getSituacao());
    }
}

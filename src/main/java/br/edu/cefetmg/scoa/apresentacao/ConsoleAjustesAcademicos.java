package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import br.edu.cefetmg.scoa.persistencia.RepositorioCancelamentoJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioInscricaoJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioMatriculaJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioPendenciaBibliotecaJdbc;
import br.edu.cefetmg.scoa.persistencia.RepositorioRetificacaoJdbc;
import br.edu.cefetmg.scoa.persistencia.SegurancaJdbc.Sessao;
import br.edu.cefetmg.scoa.dominio.RetificacaoResultado;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class ConsoleAjustesAcademicos {
    private final Scanner entrada;
    private final RepositorioCancelamentoJdbc cancelamentos;
    private final RepositorioInscricaoJdbc inscricoes;
    private final RepositorioRetificacaoJdbc retificacoes;
    private final RepositorioMatriculaJdbc matriculas;
    private final RepositorioPendenciaBibliotecaJdbc biblioteca;
    private final Sessao sessao;

    public ConsoleAjustesAcademicos(Scanner entrada,Connection conexao,Sessao sessao) {
        this.entrada=entrada;
        this.cancelamentos=new RepositorioCancelamentoJdbc(conexao,sessao);
        this.inscricoes=new RepositorioInscricaoJdbc(conexao);
        this.retificacoes=new RepositorioRetificacaoJdbc(conexao,sessao);
        this.matriculas=new RepositorioMatriculaJdbc(conexao);
        this.biblioteca=new RepositorioPendenciaBibliotecaJdbc(conexao,sessao);
        this.sessao=sessao;
    }

    public void executar() {
        while(true) {
            try {
                System.out.println("\nAjustes acadêmicos | 1 Solicitar cancelamento | 2 Decidir cancelamento");
                System.out.println("3 Histórico de cancelamento | 4 Confirmar turmas como aluno");
                System.out.println("5 Solicitar retificação | 6 Decidir retificação | 7 Histórico de retificação");
                System.out.println("8 Conferir próximo período do curso | 9 Rematricular | 10 Registrar pendência bibliotecária | 0 Voltar");
                switch(ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> {
                        long inscricao=id(ler("ID da inscrição: "));
                        String motivo=ler("Motivo do pedido: ");
                        if(confirmar()) System.out.println(cancelamentos.solicitar(inscricao,motivo));
                    }
                    case "2" -> {
                        long pedido=id(ler("ID do pedido: "));
                        boolean deferir=decisao();
                        String parecer=ler("Parecer: ");
                        if(confirmar()) System.out.println(cancelamentos.decidir(pedido,deferir,parecer));
                    }
                    case "3" -> {
                        var historico=cancelamentos.historico(id(ler("ID da inscrição: ")));
                        if(historico.isEmpty()) System.out.println("Nenhum pedido de cancelamento registrado.");
                        historico.forEach(System.out::println);
                    }
                    case "4" -> confirmarTurmas();
                    case "5" -> solicitarRetificacao();
                    case "6" -> {
                        long pedido=id(ler("ID do pedido: "));
                        boolean deferir=decisao();
                        String parecer=ler("Parecer: ");
                        if(confirmar()) {
                            retificacoes.decidir(pedido,deferir,parecer);
                            System.out.println("Decisão de retificação registrada.");
                        }
                    }
                    case "7" -> {
                        var historico=retificacoes.historico(id(ler("ID da inscrição: ")));
                        if(historico.isEmpty()) System.out.println("Nenhum pedido de retificação registrado.");
                        historico.forEach(System.out::println);
                    }
                    case "8" -> {
                        long curso=id(ler("ID do curso: "));
                        long periodo=id(ler("ID do período de origem: "));
                        long sucessor=id(ler("ID do próximo período: "));
                        if(confirmar()) {
                            retificacoes.definirSucessor(curso,periodo,sucessor);
                            System.out.println("Próximo período do curso registrado.");
                        }
                    }
                    case "9" -> {
                        long matricula=id(ler("ID da sua matrícula: "));
                        long periodo=id(ler("ID do período para renovação: "));
                        if(confirmar()) {
                            matriculas.rematricular(sessao,matricula,periodo);
                            System.out.println("Matrícula institucional renovada. Confirme as turmas na opção 4 durante a inscrição.");
                        }
                    }
                    case "10" -> {
                        long emprestimo=id(ler("ID do empréstimo: "));
                        String tipo=ler("Tipo de pendência: ");
                        BigDecimal valor=decimal(ler("Valor (0 para pendência sem cobrança): "));
                        String motivo=ler("Justificativa: ");
                        if(confirmar())
                            System.out.println("Pendência registrada: "+biblioteca.registrar(emprestimo,tipo,valor,motivo));
                    }
                    default -> System.out.println("Escolha uma opção do menu.");
                }
            } catch(NoSuchElementException e) {
                return;
            } catch(IllegalArgumentException | PersistenciaException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void solicitarRetificacao() {
        long inscricao=id(ler("ID da inscrição: "));
        BigDecimal p1=decimal(ler("P1 corrigida: ")),p2=decimal(ler("P2 corrigida: "));
        String provaFinal=ler("PF corrigida (vazio se não se aplica): ");
        BigDecimal pf=provaFinal.isEmpty()?null:decimal(provaFinal);
        long total=inteiro(ler("Total de minutos letivos: "));
        long presentes=inteiro(ler("Minutos de presença: "));
        String motivo=ler("Justificativa da retificação: ");
        var proposta=new RetificacaoResultado(p1,p2,pf,total,presentes);
        System.out.println("Resultado proposto conforme as regras do período: "+retificacoes.preparar(inscricao,proposta));
        if(confirmar()) System.out.println("Pedido registrado: "+retificacoes.solicitar(inscricao,proposta,motivo));
    }

    private boolean decisao() {
        return switch(ler("Decisão (D=deferir/I=indeferir): ").toUpperCase(java.util.Locale.ROOT)) {
            case "D" -> true;
            case "I" -> false;
            default -> throw new IllegalArgumentException("Informe D ou I.");
        };
    }

    private void confirmarTurmas() {
        long matricula=id(ler("ID da sua matrícula: "));
        var turmas=Arrays.stream(ler("IDs das turmas (separados por vírgula): ").split(",",-1))
                .map(String::strip).map(ConsoleAjustesAcademicos::id).distinct().toList();
        System.out.println("Turmas a confirmar em operações independentes: "+turmas);
        if(!confirmar()) return;
        for(long turma:turmas) {
            try {
                System.out.println("Turma "+turma+" | "+inscricoes.inscreverComoAluno(sessao,matricula,turma));
            } catch(IllegalArgumentException | PersistenciaException e) {
                System.out.println("Turma "+turma+" | "+e.getMessage());
            }
        }
    }

    private String ler(String rotulo) {
        System.out.print(rotulo);
        if(!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().strip();
    }

    private boolean confirmar() {
        return "S".equalsIgnoreCase(ler("Confirmar? (S/N): "));
    }

    private static long id(String texto) {
        long id=inteiro(texto);
        if(id<=0 || id>0xffff_ffffL) throw new IllegalArgumentException("Informe um ID positivo válido.");
        return id;
    }

    private static long inteiro(String texto) {
        try { return Long.parseLong(texto); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um número inteiro válido."); }
    }

    private static BigDecimal decimal(String texto) {
        try { return new BigDecimal(texto.replace(',','.')); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um valor decimal válido."); }
    }
}

package br.edu.cefetmg.scoa.apresentacao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import br.edu.cefetmg.scoa.aplicacao.ServicoConsultaProcessosAcademicos;
import br.edu.cefetmg.scoa.persistencia.*;
import br.edu.cefetmg.scoa.persistencia.SegurancaJdbc.Sessao;
import br.edu.cefetmg.scoa.dominio.Equivalencia.Pedido;

/** Fluxo v0.10. Senhas nunca são lidas pelo Scanner, impressas ou salvas no histórico. */
public final class ConsoleProcessoAcademico {
    private final Scanner entrada; private final Connection c; private Sessao sessao;
    public ConsoleProcessoAcademico(Scanner entrada,Connection c,Sessao sessao) { this.entrada=entrada; this.c=c; this.sessao=sessao; }
    private String ler(String texto) { System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private long id(String texto) { try { long n=Long.parseLong(ler(texto)); if(n<=0) throw new NumberFormatException(); return n; } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID positivo."); } }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    private char[] senha(String variavel) {
        var console=System.console();
        if(console!=null) return console.readPassword("Senha (oculta): ");
        String valor=System.getenv(variavel);
        if(valor==null) throw new IllegalArgumentException("Sem terminal com senha oculta: configure "+variavel+" localmente na execução, sem capturar seu valor.");
        return valor.toCharArray();
    }
    public Sessao executar() {
        var seguranca=new SegurancaJdbc(c); var repo=new RepositorioProcessoAcademicoJdbc(c);
        var consultaProcessos = new ServicoConsultaProcessosAcademicos(matricula -> repo.listar(sessao, matricula));
        while(true) try {
            System.out.println("\n18 Processos acadêmicos | 1 Inicializar acesso | 2 Entrar | 3 Sair da sessão | 4 Criar conta | 5 Solicitar conciliação | 6 Decidir conciliação | 7 Solicitar revisão | 8 Analisar revisão | 9 Listar processos | 10 Histórico do processo | 11 Vincular conta acadêmica | 12 Minha conta e vínculos | 13 Consultar conta (admin) | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return sessao; }
                case "1" -> {
                    String login=ler("Identificação da primeira conta: ");
                    if(confirmar()) System.out.println("Conta inicial criada: "+seguranca.inicializar(login,senha("SCOA_SENHA_NOVA_CONTA"),"SIM".equals(System.getenv("SCOA_PERMITIR_INICIALIZACAO_SEGURANCA"))));
                }
                case "2" -> {
                    String login=ler("Identificação: ");
                    if(sessao!=null) { sessao.encerrar(); sessao=null; }
                    sessao=seguranca.entrar(login,senha("SCOA_SENHA_SESSAO"));
                    System.out.println("Sessão iniciada; ID da conta de acesso: "+sessao.usuario());
                    exibirConta(seguranca.minhaConta(sessao));
                }
                case "3" -> { if(sessao!=null) sessao.encerrar(); sessao=null; System.out.println("Sessão encerrada."); }
                case "4" -> {
                    String login=ler("Identificação da nova conta: ");
                    System.out.println("Use uma única conta por pessoa. Os papéis acadêmicos serão obtidos dos vínculos em 18.11.");
                    String tecnico=ler("Acesso técnico inicial (SEM_PERMISSOES/ADMIN/SOLICITANTE/ANALISTA/BIBLIOTECARIO; Enter = SEM_PERMISSOES): ").toUpperCase(Locale.ROOT);
                    if(tecnico.isEmpty()) tecnico="SEM_PERMISSOES";
                    if(confirmar()) System.out.println("Conta criada; ID da conta de acesso: "+seguranca.criarConta(sessao,login,senha("SCOA_SENHA_NOVA_CONTA"),tecnico));
                }
                case "5" -> {
                    String tipo=ler("Tipo (PLANO/EQUIVALENCIA): ").toUpperCase(Locale.ROOT);
                    long origem=id("ID do registro legado: "),m=id("ID da matrícula: "),e=id("ID da estrutura: ");
                    String docs=ler("Referências documentais da conciliação: ");
                    if(confirmar()) System.out.println("Conciliação registrada: "+repo.solicitarConciliacao(sessao,tipo,origem,m,e,docs));
                }
                case "6" -> {
                    long processo=id("ID da conciliação: "); var consulta=repo.consultarConciliacao(sessao,processo); System.out.println(consulta);
                    String acao=ler("Decisão (A=aprovar/R=rejeitar): ").toUpperCase(Locale.ROOT);
                    if(!Set.of("A","R").contains(acao)) throw new IllegalArgumentException("Escolha A ou R.");
                    String parecer=ler("Parecer: "); Pedido material=null;
                    if("A".equals(acao)) {
                        if("EQUIVALENCIA".equals(consulta.tipo())) material=material();
                    }
                    System.out.println("Aprovação de plano apenas concilia o contexto; não aprova disciplinas. Material: "+material);
                    if(confirmar()) { repo.decidirConciliacao(sessao,processo,"A".equals(acao),parecer,material); System.out.println("Decisão de conciliação registrada."); }
                }
                case "7" -> {
                    long pedido=id("ID do reconhecimento (pedido deferido): "); String motivo=ler("Justificativa da revisão: ");
                    System.out.println("Abrir revisão suspende novos usos do reconhecimento; inscrições existentes permanecem preservadas.");
                    if(confirmar()) System.out.println("Revisão registrada: "+repo.solicitarRevisao(sessao,pedido,motivo));
                }
                case "8" -> {
                    long revisao=id("ID da revisão: "); var previa=repo.previa(sessao,revisao); System.out.println(previa);
                    System.out.println("POTENCIAL_HISTORICO: relação não comprovada; seleção conservadora da matrícula. Preservar não significa aprovar o aluno.");
                    String acao=ler("Encaminhamento (REJEITAR/COMPLEMENTAR/TRATAR/REVOGAR_PRESERVANDO): ").toUpperCase(Locale.ROOT);
                    String parecer=ler("Parecer e encaminhamento: ");
                    System.out.println("REVOGAR_PRESERVANDO identifica e preserva expressamente todas as inscrições da prévia; não cancela nenhuma.");
                    if(confirmar()) { repo.decidirRevisao(sessao,revisao,acao,parecer,previa.assinatura()); System.out.println("Encaminhamento registrado."); }
                }
                case "9" -> consultaProcessos.listar(id("ID da matrícula: ")).forEach(System.out::println);
                case "10" -> repo.eventos(sessao,ler("Tipo (REVISAO/CONCILIACAO/PEDIDO): ").toUpperCase(Locale.ROOT),id("ID do processo: ")).forEach(System.out::println);
                case "11" -> {
                    long usuario=id("ID da conta de acesso (usuario.id_usuario): ");
                    exibirConta(seguranca.consultarConta(sessao,usuario));
                    String tipo=ler("Tipo de vínculo (ALUNO/PROFESSOR/COORDENADOR): ").toUpperCase(Locale.ROOT);
                    String campo=switch(tipo) {
                        case "ALUNO" -> "ID do cadastro de aluno (aluno.id_aluno): ";
                        case "PROFESSOR" -> "ID do cadastro de professor (professor.id_professor): ";
                        case "COORDENADOR" -> "ID do curso coordenado (curso.id_curso): ";
                        default -> throw new IllegalArgumentException("Informe ALUNO, PROFESSOR ou COORDENADOR.");
                    };
                    long referencia=id(campo);
                    System.out.println("Vincular conta de acesso "+usuario+" a "+tipo+" de referência "+referencia+". Confira se o cadastro pertence à mesma pessoa. Permissões técnicas são mantidas.");
                    if(confirmar()) {
                        seguranca.vincularContaAcademica(sessao,usuario,tipo,referencia);
                        System.out.println("Conta acadêmica vinculada.");
                        exibirConta(seguranca.consultarConta(sessao,usuario));
                    }
                }
                case "12" -> exibirConta(seguranca.minhaConta(sessao));
                case "13" -> exibirConta(seguranca.consultarConta(sessao,id("ID da conta de acesso (usuario.id_usuario): ")));
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return sessao; }
        catch(java.time.DateTimeException e) { System.out.println("Informe uma data válida no formato AAAA-MM-DD."); }
        catch(IllegalArgumentException | PersistenciaException | ArithmeticException e) { System.out.println(e.getMessage()); }
    }
    private void exibirConta(SegurancaJdbc.ContaAcesso conta) {
        System.out.println("Conta de acesso: ID="+conta.idConta()+" | identificação="+conta.identificacao()+" | situação="+conta.situacao());
        System.out.println("Permissões técnicas concedidas: "+conta.permissoesTecnicas()+" (uso sujeito à conta ativa).");
        if(conta.vinculos().isEmpty()) System.out.println("Sem vínculo acadêmico. Vincule a conta existente em 18.11.");
        for(var vinculo:conta.vinculos()) {
            String chave=switch(vinculo.tipo()) {
                case "ALUNO" -> "aluno.id_aluno";
                case "PROFESSOR" -> "professor.id_professor";
                default -> "curso.id_curso";
            };
            System.out.println("Vínculo acadêmico: "+vinculo.tipo()+" | "+chave+"="+vinculo.referencia()+" | "+vinculo.identificacao()+" | situação do cadastro="+vinculo.situacao());
        }
    }
    private Pedido material() {
        long m=id("ID da matrícula: "),d=id("ID da disciplina destino: ");
        String instituicao=ler("Instituição de origem: "),origem=ler("Disciplina de origem: ");
        int horas=Math.toIntExact(id("Carga horária de origem: "));
        String ementa=ler("Ementa de origem: "),docs=ler("Referências de conclusão, ementa e carga: ");
        LocalDate data=LocalDate.parse(ler("Conclusão (AAAA-MM-DD): "));
        return new Pedido(m,d,instituicao,origem,horas,ementa,docs,data,ler("Ementa de destino: "));
    }
}

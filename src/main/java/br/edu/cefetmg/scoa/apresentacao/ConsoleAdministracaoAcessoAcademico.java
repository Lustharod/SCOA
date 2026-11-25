package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoAdministracaoAcessoAcademico;
import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico.*;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.*;

public final class ConsoleAdministracaoAcessoAcademico {
    private final Scanner entrada;
    private final ServicoAdministracaoAcessoAcademico servico;
    public ConsoleAdministracaoAcessoAcademico(Scanner entrada,ServicoAdministracaoAcessoAcademico servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nAdministração técnica de acesso | 1 Perfis | 2 Criar perfil | 3 Atualizar perfil | 4 Contas | 5 Conceder/revogar perfil | 6 Situação da conta | 7 Histórico da conta | 8 Histórico do perfil | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> servico.perfis().forEach(ConsoleAdministracaoAcessoAcademico::perfil);
                case "2" -> {
                    var dados=new NovoPerfil(ler("Nome do perfil técnico: "),ler("Descrição: "),permissoes()); String motivo=ler("Motivo: ");
                    System.out.println(dados); System.out.println("Criar perfil não o concede automaticamente a uma conta.");
                    if(confirmar()) perfil(servico.criarPerfil(dados,motivo));
                }
                case "3" -> {
                    var antes=servico.consultarPerfil(id("ID do perfil técnico: ")); perfil(antes);
                    if(!antes.escopoAcademico()) { System.out.println("Perfil fora do catálogo acadêmico preservado para consulta."); continue; }
                    System.out.println("As contas vinculadas exibidas serão afetadas por esta alteração.");
                    var dados=new AlterarPerfil(antes.id(),antes.versao(),ler("Nome atualizado: "),ler("Descrição atualizada: "),
                            ler("Situação (ATIVO/INATIVO): ").toUpperCase(Locale.ROOT),permissoes(),ler("Motivo: "));
                    System.out.println(dados); if(confirmar()) perfil(servico.alterarPerfil(dados));
                }
                case "4" -> servico.contas().forEach(System.out::println);
                case "5" -> {
                    var conta=servico.consultarConta(id("ID da conta (usuario.id_usuario): ")); System.out.println(conta);
                    var p=servico.consultarPerfil(id("ID do perfil técnico a conceder/revogar: ")); perfil(p);
                    String acao=ler("Ação (C=conceder/R=revogar): ").toUpperCase(Locale.ROOT);
                    if(!Set.of("C","R").contains(acao)) throw new IllegalArgumentException("Escolha C ou R.");
                    var dados=new VincularPerfil(conta.id(),conta.versao(),p.id(),p.versao(),"C".equals(acao),ler("Motivo: "));
                    System.out.println("Credenciais, conta única e vínculos acadêmicos serão preservados.");
                    if(confirmar()) System.out.println(servico.vincular(dados));
                }
                case "6" -> {
                    var conta=servico.consultarConta(id("ID da conta (usuario.id_usuario): ")); System.out.println(conta);
                    var dados=new AlterarConta(conta.id(),conta.versao(),ler("Situação (ATIVO/INATIVO): ").toUpperCase(Locale.ROOT),ler("Motivo: "));
                    System.out.println("Inativação bloqueia o acesso da pessoa; credenciais, concessões e histórico continuam preservados.");
                    if(confirmar()) System.out.println(servico.alterarConta(dados));
                }
                case "7" -> servico.historicoConta(id("ID da conta: ")).forEach(System.out::println);
                case "8" -> servico.historicoPerfil(id("ID do perfil: ")).forEach(System.out::println);
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private List<String> permissoes() {
        var valores=Codigo.values();
        for(int i=0;i<valores.length;i++) System.out.printf("%d | %s | %s%n",i+1,valores[i].codigo(),valores[i].descricao());
        String texto=ler("Números das permissões separados por vírgula (Enter = nenhuma): ");
        if(texto.isBlank()) return List.of(); var codigos=new ArrayList<String>();
        for(String token:texto.split("[,;\\s]+")) try {
            int n=Integer.parseInt(token); if(n<1 || n>valores.length) throw new NumberFormatException(); codigos.add(valores[n-1].codigo());
        } catch(NumberFormatException e) { throw new IllegalArgumentException("Selecione números do catálogo exibido."); }
        return codigos;
    }
    private static void perfil(Perfil p) {
        System.out.printf("Perfil %d | %s | %s | versão %d | %s%nDescrição: %s%nPermissões: %s%nContas vinculadas: %s%n",
                p.id(),p.nome(),p.situacao(),p.versao(),p.escopoAcademico()?"catálogo acadêmico":"somente consulta neste módulo",p.descricao(),p.permissoes(),p.contas());
    }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    private String ler(String texto) { System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private long id(String texto) { try { long n=Long.parseLong(ler(texto)); HorarioAula.validarId(n); return n; } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID positivo válido."); } }
}

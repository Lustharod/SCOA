package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoParametrosAcademicos;
import br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.math.BigDecimal;
import java.util.*;

public final class ConsoleParametrosAcademicos {
    private final Scanner entrada;
    private final ServicoParametrosAcademicos servico;
    public ConsoleParametrosAcademicos(Scanner entrada,ServicoParametrosAcademicos servico) { this.entrada=entrada; this.servico=servico; }
    private String ler(String campo) { System.out.print(campo); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
    private int numero(String campo,int padrao) { var s=ler(campo+" ["+padrao+"]: "); return s.isEmpty()?padrao:Integer.parseInt(s); }
    private BigDecimal decimal(String campo,BigDecimal padrao) { var s=ler(campo+" ["+padrao+"]: "); return s.isEmpty()?padrao:new BigDecimal(s.replace(',','.')); }
    private long id(String campo) { return Long.parseLong(ler(campo)); }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar (S/N): ")); }
    private void mostrar(Periodo p) {
        System.out.printf("Período %d | %s | %s a %s | %s | vínculo v%d | regras %s | %d turmas, %d inscrições%n",p.id(),p.descricao(),p.inicio(),p.fim(),p.situacao(),p.versaoVinculo(),p.versaoParametros()==null?"BASE_APROVADA":p.versaoParametros(),p.turmas(),p.inscricoes());
        System.out.println(p.regras());
        System.out.println("1ª fase "+p.regras().primeiraFase(p.inicio())+" | 2ª fase "+p.regras().segundaFase(p.inicio()));
        if(!p.futuro()) System.out.println("Período já iniciado: seleção preservada e alteração recusada.");
    }
    public void executar() {
        while(true) try {
            System.out.println("\nRegras acadêmicas | 1 Listar versões | 2 Criar versão | 3 Conferir período | 4 Selecionar versão para período futuro | 5 Histórico do período | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> servico.versoes().forEach(System.out::println);
                case "2" -> {
                    var b=Regras.baseAprovada(); System.out.println("Prazos em dias corridos; D+0 é o início. Nova versão não muda nenhum período automaticamente. Enter mantém o valor aprovado exibido.");
                    var r=new Regras(numero("Antecedência da 1ª fase",b.antecedenciaPrimeira()),numero("Duração da 1ª fase",b.duracaoPrimeira()),
                            numero("Início da 2ª fase (D+)",b.inicioSegunda()),numero("Duração da 2ª fase",b.duracaoSegunda()),
                            numero("Cancelamento com liberação de vaga até D+",b.cancelamentoImediatoAte()),numero("Pedido de cancelamento até D+",b.cancelamentoPedidoAte()),
                            numero("Transferência até D+",b.transferenciaAte()),numero("Retificação docente: dias após término",b.retificacaoDocenteDias()),
                            numero("Tolerância de atraso (minutos)",b.toleranciaAtraso()),decimal("Frequência mínima (%)",b.frequenciaPercentual()),
                            decimal("Média inicial que dispensa PF",b.mediaDispensaPf()),decimal("Média mínima após PF",b.mediaFinalMinima()));
                    var pedido=new Criar(r,ler("Motivo: ")); System.out.println(r);
                    if(confirmar()) System.out.println(servico.criar(pedido));
                }
                case "3" -> mostrar(servico.consultarPeriodo(id("ID do período: ")));
                case "4" -> {
                    var p=servico.consultarPeriodo(id("ID do período futuro: ")); mostrar(p);
                    servico.versoes().forEach(System.out::println);
                    String escolha=ler("ID da versão (BASE para base aprovada): "); Long versao="BASE".equalsIgnoreCase(escolha)?null:Long.valueOf(escolha);
                    if(versao!=null) System.out.println(servico.versoes().stream().filter(v->v.id()==versao).findFirst().orElseThrow(()->new IllegalArgumentException("Versão não encontrada.")));
                    var pedido=new Vincular(p.id(),p.versaoVinculo(),versao,p.assinatura(),ler("Motivo da seleção para este período: "));
                    System.out.println("Aplicação explícita somente neste período futuro; não recalcula resultados nem reescreve planos existentes.");
                    if(confirmar()) mostrar(servico.vincular(pedido));
                }
                case "5" -> servico.historico(id("ID do período: ")).forEach(System.out::println);
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
          catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
}

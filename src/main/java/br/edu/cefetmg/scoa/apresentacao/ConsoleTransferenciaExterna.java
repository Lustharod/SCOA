package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoTransferenciaExterna;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.TransferenciaExterna.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.util.*;

public final class ConsoleTransferenciaExterna {
    private final Scanner entrada;
    private final ServicoTransferenciaExterna servico;
    public ConsoleTransferenciaExterna(Scanner entrada,ServicoTransferenciaExterna servico) {
        this.entrada=entrada; this.servico=servico;
    }
    public void executar() {
        while(true) try {
            System.out.println("\nTransferência externa | 1 Registrar entrada | 2 Registrar saída | 3 Consultar protocolo | 4 Listar por aluno | 5 Acompanhamento ou decisão | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> registrar(Tipo.ENTRADA);
                case "2" -> registrar(Tipo.SAIDA);
                case "3" -> {
                    long id=id("Protocolo externo: "); mostrar(servico.consultar(id));
                    servico.historico(id).forEach(System.out::println);
                }
                case "4" -> servico.listar(id("ID do cadastro de aluno (não é o ID da conta): ")).forEach(ConsoleTransferenciaExterna::mostrar);
                case "5" -> atualizar();
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void registrar(Tipo direcao) {
        Long aluno=null,curso=null,estrutura=null,origem=null;
        if(direcao==Tipo.ENTRADA) {
            aluno=id("ID do cadastro de aluno existente (não é o ID da conta): ");
            curso=id("ID do curso de destino no SCOA: ");
            estrutura=id("ID da estrutura curricular do destino: ");
        } else origem=id("ID da matrícula institucional de origem (não é o ID do aluno/conta): ");
        long periodo=id("ID do período da transferência: ");
        String chave=ler("Referência única do processo (vazio gera uma): ");
        if(chave.isBlank()) chave=UUID.randomUUID().toString();
        var pedido=new Pedido(chave,direcao,aluno,curso,estrutura,origem,periodo,
                ler(direcao==Tipo.ENTRADA?"Instituição externa de origem: ":"Instituição externa de destino: "),
                ler("Referências dos documentos conferidos: "),ler("Motivo e referência da autorização acadêmica: "));
        System.out.println(pedido);
        System.out.println("O registro abre a análise; a matrícula só muda após a decisão de efetivar.");
        if(confirmar("Registrar processo?")) mostrar(servico.registrar(pedido));
        else System.out.println("Registro cancelado antes da gravação.");
    }
    private void atualizar() {
        long id=id("Protocolo externo: "); Processo p=servico.consultar(id); mostrar(p);
        System.out.println("Situações: EM_ANALISE (acompanhar), EFETIVADA, INDEFERIDA, CANCELADA.");
        Situacao destino;
        try { destino=Situacao.valueOf(ler("Nova situação: ").toUpperCase(Locale.ROOT)); }
        catch(IllegalArgumentException e) { throw new IllegalArgumentException("Situação inválida."); }
        String docs=ler("Referências dos documentos atualizadas (vazio mantém as exibidas): ");
        var u=new Atualizacao(p.versao(),destino,docs.isBlank()?p.documentos():docs,ler("Observação ou fundamento da decisão: "));
        if(destino==Situacao.EFETIVADA) {
            System.out.println("Efetivação até D+45 e dentro do período; biblioteca deve estar regular.");
            System.out.println(p.tipo()==Tipo.ENTRADA
                    ?"Será aberto vínculo no destino para o mesmo aluno/conta. Aproveitamentos externos exigem processo formal separado."
                    :"A origem será TRANSFERIDO e inscrições ativas serão canceladas, com a regra de vagas vigente. Histórico e conta serão preservados.");
        }
        if(confirmar("Confirmar atualização e os efeitos informados?")) mostrar(servico.atualizar(id,u));
        else System.out.println("Atualização cancelada antes da gravação.");
    }
    private static void mostrar(Processo p) {
        System.out.printf("Protocolo %d | %s | %s | versão %d | referência %s%n",p.id(),p.tipo(),p.situacao(),p.versao(),p.chave());
        System.out.printf("Aluno %d | curso %d | estrutura %s | matrícula de origem %s | período %d%n",p.aluno(),p.curso(),p.estrutura(),p.origem(),p.periodo());
        System.out.printf("Instituição externa: %s | documentos: %s%nMotivo: %s%n",p.instituicao(),p.documentos(),p.motivo());
        System.out.printf("Transferência efetivada: %s | matrícula do efeito: %s | autor inicial %d | UTC %s / %s%n",
                p.transferencia(),p.matriculaEfetivada(),p.autor(),p.registradoEm(),p.atualizadoEm());
    }
    private boolean confirmar(String texto) { return "S".equalsIgnoreCase(ler(texto+" (S/N): ")); }
    private String ler(String texto) {
        System.out.print(texto); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip();
    }
    private long id(String texto) {
        try { long id=Long.parseLong(ler(texto)); HorarioAula.validarId(id); return id; }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID positivo válido."); }
    }
}

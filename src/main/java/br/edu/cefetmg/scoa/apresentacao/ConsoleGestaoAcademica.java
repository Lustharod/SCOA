package br.edu.cefetmg.scoa.apresentacao;

import br.edu.cefetmg.scoa.aplicacao.ServicoGestaoAcademica;
import br.edu.cefetmg.scoa.dominio.GestaoAcademica.*;
import br.edu.cefetmg.scoa.persistencia.PersistenciaException;
import java.time.*;
import java.util.*;

public final class ConsoleGestaoAcademica {
    private final Scanner entrada; private final ServicoGestaoAcademica servico;
    public ConsoleGestaoAcademica(Scanner entrada,ServicoGestaoAcademica servico) { this.entrada=entrada; this.servico=servico; }
    public void executar() {
        while(true) try {
            System.out.println("\nGestão acadêmica | 1 Meus cursos | 2 Docentes do curso | 3 Registrar meta/prazo | 4 Consultar metas | 5 Atualizar meta | 6 Histórico da meta | 7 Avaliações | 8 Conferir e auditar diário | 9 Auditorias anteriores | 10 Monitoramento curricular | 0 Voltar");
            switch(ler("Opção: ")) {
                case "0" -> { return; }
                case "1" -> { var cursos=servico.cursos(); if(cursos.isEmpty()) System.out.println("Nenhum curso sob seu acompanhamento."); cursos.forEach(c->System.out.printf("Curso %d | %s | %s%n",c.id(),c.nome(),c.situacao())); }
                case "2" -> servico.docentes(id("ID do curso: ")).forEach(d->System.out.printf("Professor %d | %s | %s%n",d.id(),d.identificacao(),d.situacao()));
                case "3" -> registrarMeta();
                case "4" -> servico.metas(id("ID do curso: "),opcional("ID do professor (Enter = todos do curso): ")).forEach(ConsoleGestaoAcademica::meta);
                case "5" -> atualizarMeta();
                case "6" -> servico.historicoMeta(id("ID da meta: ")).forEach(h->System.out.printf("Versão %d | %s UTC | autor %d | %s%nAnterior: %s%nRegistrado: %s%n",h.versao(),h.data(),h.autor(),h.motivo(),Objects.toString(h.anterior(),"sem versão anterior"),h.posterior()));
                case "7" -> servico.avaliacoes(filtro(false)).forEach(a->System.out.printf("Turma %d | disciplina %d | período %d | avaliação %d | %s | %s | %s%n",a.turma(),a.disciplina(),a.periodo(),a.avaliacao().id(),Objects.toString(a.avaliacao().tipo(),"tipo legado"),a.avaliacao().data(),a.avaliacao().descricao()));
                case "8" -> auditar();
                case "9" -> servico.auditorias(id("ID do curso: "),id("ID da turma: ")).forEach(a->System.out.printf("Auditoria %d | %s UTC | autor %d | %s%n%s%n",a.id(),a.data(),a.autor(),a.conclusao(),a.observacoes()));
                case "10" -> servico.monitorar(filtro(true)).forEach(m->System.out.printf("Matrícula %d | %s | %s | cumpridos %s/%s | reconhecidos %s | revisão %s | andamento %s | plano %s%n%s%n",
                        m.matricula(),m.identificacao(),m.situacao(),valor(m.cumpridos()),valor(m.componentes()),valor(m.reconhecidos()),valor(m.sobRevisao()),valor(m.emAndamento()),m.plano(),m.aviso()));
                default -> System.out.println("Escolha uma opção do menu.");
            }
        } catch(NoSuchElementException e) { return; }
        catch(DateTimeException e) { System.out.println("Informe data válida no formato AAAA-MM-DD."); }
        catch(IllegalArgumentException | PersistenciaException e) { System.out.println(e.getMessage()); }
    }
    private void registrarMeta() {
        long curso=id("ID do curso: "); servico.docentes(curso).forEach(d->System.out.printf("Professor %d | %s%n",d.id(),d.identificacao()));
        var pedido=new NovaMeta(curso,id("ID do professor: "),opcional("ID do período (Enter = sem período específico): "),
                TipoMeta.valueOf(ler("Tipo (META/PRAZO): ").toUpperCase(Locale.ROOT)),ler("Descrição: "),LocalDate.parse(ler("Prazo (AAAA-MM-DD): ")));
        if(confirmar()) meta(servico.registrarMeta(pedido));
    }
    private void atualizarMeta() {
        long curso=id("ID do curso: "); var metas=servico.metas(curso,null); metas.forEach(ConsoleGestaoAcademica::meta);
        long id=id("ID da meta a atualizar: "); var atual=metas.stream().filter(m->m.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Selecione uma meta do curso exibido."));
        var pedido=new AtualizarMeta(id,atual.versao(),ler("Descrição atualizada: "),LocalDate.parse(ler("Prazo atualizado (AAAA-MM-DD): ")),
                SituacaoMeta.valueOf(ler("Situação (ABERTA/EM_ANDAMENTO/CONCLUIDA/CANCELADA): ").toUpperCase(Locale.ROOT)),ler("Motivo: "));
        if(confirmar()) meta(servico.atualizarMeta(pedido));
    }
    private void auditar() {
        long curso=id("ID do curso: "),turma=id("ID da turma: "); var diario=servico.diario(curso,turma); mostrarDiario(diario);
        String acao=ler("Registrar conclusão da auditoria? (S/N): "); if(!"S".equalsIgnoreCase(acao)) return;
        var conclusao=ConclusaoAuditoria.valueOf(ler("Conclusão (SEM_RESSALVAS/COM_RESSALVAS/REQUER_TRATAMENTO): ").toUpperCase(Locale.ROOT));
        var pedido=new NovaAuditoria(curso,turma,diario.assinatura(),conclusao,ler("Observações e encaminhamento: "));
        System.out.println("A conclusão registra sua análise. Correções de notas/frequência seguem o procedimento acadêmico próprio.");
        if(confirmar()) System.out.println("Auditoria registrada. Protocolo: "+servico.auditar(pedido).id());
    }
    private static void mostrarDiario(Diario d) {
        System.out.printf("Diário da turma %d | alunos do curso %d%n",d.turma(),d.curso());
        d.aulas().forEach(a->System.out.printf("Aula %d | %s | %s%nPauta: %s%nConteúdo: %s%n",a.id(),a.data(),Objects.toString(a.tempo(),"tempo legado"),a.pauta(),a.conteudo()));
        d.avaliacoes().forEach(a->System.out.printf("Avaliação %d | %s | %s | %s%n",a.id(),Objects.toString(a.tipo(),"tipo legado"),a.data(),a.descricao()));
        if(d.alunos().isEmpty()) System.out.println("Nenhum aluno desse curso inscrito na turma.");
        for(var a:d.alunos()) {
            System.out.printf("Matrícula %d | inscrição %d | %s | %s%n",a.matricula(),a.inscricao(),a.identificacao(),a.situacao());
            a.lancamentos().notas().forEach(n->System.out.printf("  %s: %s%n",Objects.toString(n.avaliacao().tipo(),"Avaliação legada"),valor(n.valor())));
            System.out.printf("  Frequência dos lançamentos: %d/%d min considerados; carga da disciplina %d min%n",a.lancamentos().minutosPresentes(),a.lancamentos().minutosConsiderados(),a.lancamentos().minutosPrevistos());
            a.lancamentos().pendencias().forEach(p->System.out.println("  Pendente: "+p));
            if(a.resultado()==null) System.out.println("  Resultado vigente ainda não registrado.");
            else System.out.printf("  Resultado vigente %s | média %s | frequência %d/%d min%n",a.resultado().situacao(),valor(a.resultado().mediaFinal()),a.resultado().presentes(),a.resultado().total());
        }
    }
    private static void meta(Meta m) {
        System.out.printf("Meta %d | curso %d | professor %d | %s | prazo %s | %s | versão %d%n%s%n",m.id(),m.curso(),m.professor(),m.tipo(),m.prazo(),m.situacao(),m.versao(),m.descricao());
        if(m.atrasada(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) System.out.println("Prazo vencido; situação registrada preservada.");
    }
    private Filtro filtro(boolean aluno) {
        return new Filtro(id("ID do curso: "),opcional("ID do período (Enter = todos): "),opcional("ID da disciplina (Enter = todas): "),opcional("ID da turma (Enter = todas): "),aluno?opcional("ID do aluno (Enter = todos do curso): "):null);
    }
    private static String valor(Object valor) { return Objects.toString(valor,"não informado"); }
    private boolean confirmar() { return "S".equalsIgnoreCase(ler("Confirmar? (S/N): ")); }
    private Long opcional(String prompt) {
        String texto=ler(prompt); if(texto.isEmpty()) return null;
        try { return Long.valueOf(texto); } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe ID numérico ou deixe vazio."); }
    }
    private long id(String prompt) {
        try { return Long.parseLong(ler(prompt)); } catch(NumberFormatException e) { throw new IllegalArgumentException("Informe um ID numérico positivo."); }
    }
    private String ler(String prompt) { System.out.print(prompt); if(!entrada.hasNextLine()) throw new NoSuchElementException(); return entrada.nextLine().strip(); }
}

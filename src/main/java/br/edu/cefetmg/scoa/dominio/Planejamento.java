package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.util.*;

/** Simulação determinística; nenhuma sugestão é evidência de cumprimento. */
public final class Planejamento {
    private Planejamento() { }
    public record Periodo(long id, String descricao, LocalDate inicio, LocalDate fim, String situacao) {
        public PeriodoLetivo intervalo() { return new PeriodoLetivo(inicio,fim); }
    }
    public record Disciplina(long id, String nome, int horas, String situacao, String cadastro, List<Long> requisitos) {
        public Disciplina { requisitos=List.copyOf(requisitos); }
    }
    public record Oferta(long turma, long disciplina, Periodo periodo, String situacao, int vagas, List<FaixaHorario> horarios) {
        public Oferta { horarios=List.copyOf(horarios); }
    }
    public record Dados(long matricula, long estrutura, LocalDate hoje, List<Disciplina> disciplinas,
                        List<Oferta> ofertas, List<Oferta> inscricoesAtivas) {
        public Dados { disciplinas=List.copyOf(disciplinas); ofertas=List.copyOf(ofertas); inscricoesAtivas=List.copyOf(inscricoesAtivas); }
    }
    public record Tentativa(Oferta oferta, List<String> impedimentos) {
        public Tentativa { impedimentos=List.copyOf(impedimentos); }
    }
    public record Item(Disciplina disciplina, Oferta sugestao, List<Tentativa> tentativas, String motivo) {
        public Item { tentativas=List.copyOf(tentativas); }
    }
    public record Resultado(long matricula, long estrutura, List<Item> itens, Map<Long,Long> horasSugeridas) {
        public Resultado { itens=List.copyOf(itens); horasSugeridas=Collections.unmodifiableMap(new LinkedHashMap<>(horasSugeridas)); }
    }
    public static boolean conflita(Oferta a, Oferta b) {
        for(var x:a.horarios()) for(var y:b.horarios())
            if(RegraAlocacao.conflita(x,a.periodo().intervalo(),y,b.periodo().intervalo())) return true;
        return false;
    }
    public static Resultado distribuir(Dados dados, List<Long> prioridade) {
        Objects.requireNonNull(dados); Objects.requireNonNull(prioridade);
        var porId=new LinkedHashMap<Long,Disciplina>();
        dados.disciplinas().forEach(d->porId.put(d.id(),d));
        if(new HashSet<>(prioridade).size()!=prioridade.size() || !porId.keySet().containsAll(prioridade))
            throw new IllegalArgumentException("Prioridade contém disciplina repetida ou fora da estrutura.");
        var ordem=new ArrayList<>(prioridade);
        porId.keySet().stream().sorted().filter(id->!ordem.contains(id)).forEach(ordem::add);
        var cumpridas=new HashSet<Long>();
        dados.disciplinas().stream().filter(d->Set.of("CONCLUIDA","RECONHECIDA").contains(d.situacao())).forEach(d->cumpridas.add(d.id()));
        var ocupadas=new ArrayList<>(dados.inscricoesAtivas());
        var itens=new ArrayList<Item>(); var totais=new LinkedHashMap<Long,Long>();
        for(long id:ordem) {
            var d=porId.get(id);
            if(!"PENDENTE".equals(d.situacao())) {
                itens.add(new Item(d,null,List.of(),"Situação preservada: "+d.situacao()+"; não redistribuída.")); continue;
            }
            var ofertas=dados.ofertas().stream().filter(o->o.disciplina()==id)
                    .sorted(Comparator.comparing((Oferta o)->o.periodo().inicio()).thenComparingLong(Oferta::turma)).toList();
            var tentativas=new ArrayList<Tentativa>(); Oferta escolhida=null;
            for(var o:ofertas) {
                var motivos=new ArrayList<String>();
                if(!"ATIVO".equals(d.cadastro())) motivos.add("Disciplina inativa.");
                if(!"ATIVO".equals(o.situacao())) motivos.add("Turma inativa.");
                if(!"ATIVO".equals(o.periodo().situacao())) motivos.add("Período inativo ou encerrado.");
                if(o.periodo().fim().isBefore(dados.hoje())) motivos.add("Período passado.");
                for(long requisito:d.requisitos()) if(!cumpridas.contains(requisito)) motivos.add("Pré-requisito não cumprido formalmente: "+requisito+".");
                if(o.vagas()<=0) motivos.add("Turma sem vagas observadas.");
                if(o.horarios().isEmpty()) motivos.add("Turma sem horários definidos.");
                for(var ativa:ocupadas) {
                    if(ativa.horarios().isEmpty() && !o.periodo().inicio().isAfter(ativa.periodo().fim()) && !ativa.periodo().inicio().isAfter(o.periodo().fim()))
                        motivos.add("Inscrição ativa sem horários: turma "+ativa.turma()+".");
                    if(conflita(o,ativa)) motivos.add("Conflito com turma "+ativa.turma()+" (inscrição ativa ou sugestão anterior).");
                }
                tentativas.add(new Tentativa(o,motivos));
                if(motivos.isEmpty()) { escolhida=o; ocupadas.add(o); totais.merge(o.periodo().id(),(long)d.horas(),Long::sum); break; }
            }
            itens.add(new Item(d,escolhida,tentativas,escolhida!=null?"Sugestão; não efetiva inscrição.":
                    ofertas.isEmpty()?"Sem oferta cadastrada nos períodos selecionados.":"Nenhuma oferta viável; consulte os impedimentos."));
        }
        return new Resultado(dados.matricula(),dados.estrutura(),itens,totais);
    }
}

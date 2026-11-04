package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.Planejamento.*;
import br.edu.cefetmg.scoa.dominio.Planejamento.Disciplina;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanejamentoTest {
    private final LocalDate hoje=LocalDate.of(2026,11,2);
    private Periodo periodo(long id,int dias) { return new Periodo(id,"P"+id,hoje.plusDays(dias),hoje.plusDays(dias+60),"ATIVO"); }
    private Disciplina d(long id,String estado,Long... req) { return new Disciplina(id,"D"+id,60,estado,"ATIVO",List.of(req)); }
    private Oferta o(long turma,long d,Periodo p,int hora) { return new Oferta(turma,d,p,"ATIVO",2,List.of(new FaixaHorario(1,LocalTime.of(hora,0),LocalTime.of(hora+1,0)))); }
    private Resultado sim(List<Disciplina> ds,List<Oferta> os,List<Oferta> ativas,Long... ordem) {
        return Planejamento.distribuir(new Dados(1,2,hoje,ds,os,ativas),List.of(ordem));
    }
    @Test void sugestaoNaoCumpreRequisitoMesmoEmPeriodoPosterior() {
        var r=sim(List.of(d(1,"PENDENTE"),d(2,"PENDENTE",1L)),List.of(o(10,1,periodo(1,10),8),o(20,2,periodo(2,100),8)),List.of());
        assertNotNull(r.itens().getFirst().sugestao()); assertNull(r.itens().getLast().sugestao());
        assertTrue(r.itens().getLast().tentativas().getFirst().impedimentos().getFirst().contains("Pré-requisito"));
    }
    @Test void andamentoNaoCumpreRequisito() {
        var r=sim(List.of(d(1,"EM_ANDAMENTO"),d(2,"PENDENTE",1L)),List.of(o(20,2,periodo(2,100),8)),List.of());
        assertNull(r.itens().getLast().sugestao());
    }
    @Test void conclusaoEReconhecimentoCumpremSemRedistribuir() {
        for(String estado:List.of("CONCLUIDA","RECONHECIDA")) {
            var r=sim(List.of(d(1,estado),d(2,"PENDENTE",1L)),List.of(o(10,1,periodo(1,10),8),o(20,2,periodo(1,10),9)),List.of());
            assertNull(r.itens().getFirst().sugestao()); assertNotNull(r.itens().getLast().sugestao()); assertEquals(60L,r.horasSugeridas().get(1L));
        }
    }
    @Test void prioridadeResolveConflitoSemPrometerOtimo() {
        var p=periodo(1,10); var r=sim(List.of(d(1,"PENDENTE"),d(2,"PENDENTE")),List.of(o(10,1,p,8),o(20,2,p,8)),List.of(),2L);
        assertEquals(2,r.itens().getFirst().disciplina().id()); assertNotNull(r.itens().getFirst().sugestao()); assertNull(r.itens().getLast().sugestao());
    }
    @Test void tentaOutraOfertaDepoisDeConflitoEOrdenaPorData() {
        var p=periodo(1,10); var r=sim(List.of(d(1,"PENDENTE")),List.of(o(30,1,periodo(2,100),8),o(20,1,p,9),o(10,1,p,8)),List.of(o(5,8,p,8)));
        assertEquals(20,r.itens().getFirst().sugestao().turma()); assertEquals(2,r.itens().getFirst().tentativas().size());
    }
    @Test void mesmoHorarioSemSobreposicaoDosPeriodosNaoConflita() {
        var r=sim(List.of(d(1,"PENDENTE")),List.of(o(10,1,periodo(2,100),8)),List.of(o(5,8,periodo(1,10),8)));
        assertNotNull(r.itens().getFirst().sugestao());
    }
    @Test void ausenciaDeOfertaFicaExplicita() { assertTrue(sim(List.of(d(1,"PENDENTE")),List.of(),List.of()).itens().getFirst().motivo().contains("Sem oferta")); }
    @Test void faltaDeHorarioEVagasBloqueia() {
        var o=new Oferta(10,1,periodo(1,10),"ATIVO",0,List.of());
        var i=sim(List.of(d(1,"PENDENTE")),List.of(o),List.of()).itens().getFirst(); assertNull(i.sugestao()); assertEquals(2,i.tentativas().getFirst().impedimentos().size());
    }
    @Test void ofertaInativaEPeriodoPassadoBloqueiam() {
        var o=new Oferta(10,1,periodo(1,-100),"INATIVO",2,List.of());
        var i=sim(List.of(d(1,"PENDENTE")),List.of(o),List.of()).itens().getFirst();
        assertTrue(i.tentativas().getFirst().impedimentos().contains("Período passado.")); assertNull(i.sugestao());
    }
    @Test void prioridadeInvalidaNaoProduzPlano() {
        assertThrows(IllegalArgumentException.class,()->sim(List.of(d(1,"PENDENTE")),List.of(),List.of(),2L));
        assertThrows(IllegalArgumentException.class,()->sim(List.of(d(1,"PENDENTE")),List.of(),List.of(),1L,1L));
    }
}

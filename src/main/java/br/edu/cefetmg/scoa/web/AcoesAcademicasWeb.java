package br.edu.cefetmg.scoa.web;

import java.sql.Connection;
import java.util.*;
import java.util.function.*;
import br.edu.cefetmg.scoa.persistencia.*;

/** Catálogo explícito. Nenhum nome de método recebido do navegador é executado por reflexão. */
final class AcoesAcademicasWeb {
    record Contexto(Connection c,SegurancaJdbc.Sessao sessao,SegurancaJdbc.ContaAcesso conta) { }
    record Campo(String nome,String titulo,String tipo,boolean opcional,String ajuda,List<String> opcoes,String padrao) {
        Campo { opcoes=List.copyOf(opcoes); }
        Campo facultativo() { return new Campo(nome,titulo,tipo,true,ajuda,opcoes,padrao); }
        Campo ajuda(String ajuda) { return new Campo(nome,titulo,tipo,opcional,ajuda,opcoes,padrao); }
        Campo padrao(Object valor) { return new Campo(nome,titulo,tipo,opcional,ajuda,opcoes,Objects.toString(valor,"")); }
    }
    record Preparacao(Object previa,Function<Contexto,Object> confirmar) { boolean grava() { return confirmar!=null; } }
    record Acao(String id,String grupo,String titulo,String descricao,Predicate<SegurancaJdbc.ContaAcesso> permitida,List<Campo> campos,
            boolean grava,BiFunction<Contexto,FormularioWeb,Preparacao> preparar) { Acao { campos=List.copyOf(campos); } }
    private final Map<String,Acao> acoes=new LinkedHashMap<>();
    void adicionar(Acao acao) { if(!acao.id().matches("[a-z][a-z0-9-]{0,60}") || acoes.putIfAbsent(acao.id(),acao)!=null) throw new IllegalArgumentException("Ação duplicada."); }
    Acao buscar(String id) { var a=acoes.get(id); if(a==null) throw new IllegalArgumentException("Página acadêmica não encontrada."); return a; }
    List<Acao> disponiveis(SegurancaJdbc.ContaAcesso conta) { return acoes.values().stream().filter(a->a.permitida().test(conta)).toList(); }
    static Campo campo(String nome,String titulo) { return new Campo(nome,titulo,"text",false,"",List.of(),""); }
    static Campo id(String nome,String titulo) { return new Campo(nome,titulo,"number",false,"Use a referência exibida na consulta correspondente.",List.of(),""); }
    static Campo numero(String nome,String titulo) { return new Campo(nome,titulo,"number",false,"",List.of(),""); }
    static Campo decimal(String nome,String titulo) { return new Campo(nome,titulo,"decimal",false,"Até uma casa decimal; use ponto ou vírgula.",List.of(),""); }
    static Campo texto(String nome,String titulo) { return new Campo(nome,titulo,"textarea",false,"",List.of(),""); }
    static Campo data(String nome,String titulo) { return new Campo(nome,titulo,"date",false,"",List.of(),""); }
    static Campo escolha(String nome,String titulo,String... valores) { return new Campo(nome,titulo,"select",false,"",List.of(valores),""); }
    static Campo escolha(String nome,String titulo,Class<? extends Enum<?>> tipo) { return escolha(nome,titulo,Arrays.stream(tipo.getEnumConstants()).map(Enum::name).toArray(String[]::new)); }
    static boolean papel(SegurancaJdbc.ContaAcesso c,String papel) { return c.vinculos().stream().anyMatch(v->papel.equals(v.tipo())); }
    static boolean tecnico(SegurancaJdbc.ContaAcesso c,String codigo) { return c.permissoesTecnicas().contains(SegurancaJdbc.ADMIN) || c.permissoesTecnicas().contains(codigo); }
    static final Predicate<SegurancaJdbc.ContaAcesso> ALUNO=c->papel(c,"ALUNO"), DOCENTE=c->papel(c,"PROFESSOR"), GESTAO=c->papel(c,"COORDENADOR")||papel(c,"GESTOR"),
            CONSULTAR=c->tecnico(c,AcessoAcademicoJdbc.CONSULTAR), MANTER=c->tecnico(c,AcessoAcademicoJdbc.MANTER), ADMIN=c->c.permissoesTecnicas().contains(SegurancaJdbc.ADMIN);
    void consultar(String id,String grupo,String titulo,String descricao,Predicate<SegurancaJdbc.ContaAcesso> acesso,BiFunction<Contexto,FormularioWeb,Object> consulta,Campo... campos) {
        adicionar(new Acao(id,grupo,titulo,descricao,acesso,List.of(campos),false,(c,f)->new Preparacao(consulta.apply(c,f),null)));
    }
    void alterar(String id,String grupo,String titulo,String descricao,Predicate<SegurancaJdbc.ContaAcesso> acesso,BiFunction<Contexto,FormularioWeb,Preparacao> preparador,Campo... campos) {
        adicionar(new Acao(id,grupo,titulo,descricao,acesso,List.of(campos),true,preparador));
    }
    static Preparacao gravar(Object previa,Function<Contexto,Object> gravacao) { return new Preparacao(previa,gravacao); }
    static Object concluido(Runnable operacao) { operacao.run(); return "Operação concluída. Consulte o histórico e os dados atualizados."; }
    static AcoesAcademicasWeb criar() {
        var catalogo=new AcoesAcademicasWeb();
        AcoesAlunoDocenteWeb.registrar(catalogo);
        AcoesSecretariaWeb.registrar(catalogo);
        AcoesGestaoAdministracaoWeb.registrar(catalogo);
        return catalogo;
    }
}

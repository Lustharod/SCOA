package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import java.time.*;
import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;

/** Mesma coordenação conservadora da inscrição; consultas derivadas não duplicam decisões. */
public final class RepositorioEquivalenciaJdbc implements RepositorioEquivalencia {
    private final Connection c;
    public RepositorioEquivalenciaJdbc(Connection c) { this.c=Objects.requireNonNull(c); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    private <T> List<T> q(String sql, Leitor<T> leitor, Object... valores) throws SQLException {
        try(var s=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            try(var r=s.executeQuery()) {
                var lista=new ArrayList<T>(); while(r.next()) lista.add(leitor.ler(r)); return List.copyOf(lista);
            }
        }
    }
    private long gravar(String sql,Object... valores) throws SQLException {
        try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            s.executeUpdate(); try(var r=s.getGeneratedKeys()) { return r.next()?r.getLong(1):0; }
        }
    }
    private LocalDate hoje() throws SQLException {
        return q("SELECT UTC_TIMESTAMP(6)",r->r.getObject(1,LocalDateTime.class)
                .atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("America/Sao_Paulo")).toLocalDate()).getFirst();
    }
    private record Contexto(long curso,String situacao,long estrutura) { }
    private Contexto contexto(long matricula,boolean escrita) throws SQLException {
        HorarioAula.validarId(matricula);
        if(q("SELECT id FROM controle_inscricao WHERE id=1 FOR UPDATE",r->r.getLong(1)).isEmpty())
            throw new IllegalArgumentException("Confira a migração V007.");
        var previa=q("SELECT id_aluno FROM matricula_academica WHERE id_matricula=?",r->r.getLong(1),matricula);
        if(previa.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
        var aluno=q("SELECT situacao FROM aluno WHERE id_aluno=? FOR UPDATE",r->r.getString(1),previa.getFirst());
        var m=q("SELECT id_aluno,id_curso,situacao FROM matricula_academica WHERE id_matricula=? FOR UPDATE",
                r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3)},matricula).getFirst();
        if((long)m[0]!=previa.getFirst()) throw new IllegalArgumentException("Vínculo alterado; consulte novamente.");
        if(escrita && (!"ATIVO".equals(m[2]) || aluno.isEmpty() || !"ATIVO".equals(aluno.getFirst())))
            throw new IllegalArgumentException("Aluno e matrícula devem estar ativos para registrar pedido ou decisão.");
        var curso=q("SELECT situacao FROM curso WHERE id_curso=? FOR SHARE",r->r.getString(1),m[1]);
        if(escrita && (curso.isEmpty() || !"ATIVO".equals(curso.getFirst())))
            throw new IllegalArgumentException("Curso deve estar ativo.");
        // Ordem compatível com avaliarBloqueada e manutenção do grafo curricular.
        q("SELECT id_disciplina FROM disciplina ORDER BY id_disciplina FOR SHARE",r->r.getLong(1));
        var vinculo=q("SELECT id_estrutura FROM vinculo_curricular_matricula WHERE id_matricula=? FOR SHARE",r->r.getLong(1),matricula);
        long estrutura=vinculo.isEmpty()?0:vinculo.getFirst();
        if(estrutura!=0) {
            var e=q("SELECT id_curso,situacao FROM estrutura_curricular WHERE id_estrutura=? FOR SHARE",
                    r->new Object[]{r.getLong(1),r.getString(2)},estrutura).getFirst();
            if((long)e[0]!=(long)m[1] || (escrita && !"ATIVO".equals(e[1])))
                throw new IllegalArgumentException("Estrutura curricular incompatível ou inativa.");
        }
        return new Contexto((long)m[1],(String)m[2],estrutura);
    }
    private void exigirContextoConciliado(long matricula,Contexto ctx) throws SQLException {
        if(ctx.estrutura()==0) throw new IllegalArgumentException("Vincule explicitamente a estrutura curricular antes deste fluxo.");
        if(!q("SELECT id_plano FROM plano_estudo WHERE id_matricula=? FOR SHARE",r->r.getLong(1),matricula).isEmpty())
            throw new IllegalArgumentException("Plano legado requer conciliação explícita; nenhum item foi alterado.");
        if(!q("SELECT id_aproveitamento FROM aproveitamento_equivalencia WHERE id_matricula=? FOR SHARE",r->r.getLong(1),matricula).isEmpty())
            throw new IllegalArgumentException("Equivalência legada requer conciliação documental explícita; não foi reconhecida automaticamente.");
    }
    private int destino(long estrutura,long disciplina) throws SQLException {
        var d=q("SELECT d.carga_horaria,d.situacao FROM estrutura_disciplina e JOIN disciplina d ON d.id_disciplina=e.id_disciplina "
                +"WHERE e.id_estrutura=? AND e.id_disciplina=? FOR SHARE",r->new Object[]{r.getInt(1),r.getString(2)},estrutura,disciplina);
        if(d.isEmpty() || !"ATIVO".equals(d.getFirst()[1])) throw new IllegalArgumentException("Destino deve estar ativo e pertencer à estrutura vinculada.");
        return (int)d.getFirst()[0];
    }
    private static Registro ler(ResultSet r) throws SQLException {
        return new Registro(r.getLong("id_pedido"),new Pedido(r.getLong("id_matricula"),r.getLong("id_disciplina_destino"),
                r.getString("instituicao_origem"),r.getString("disciplina_origem"),r.getInt("carga_horaria_origem"),
                r.getString("ementa_origem"),r.getString("documentos"),r.getObject("conclusao_origem",LocalDate.class),r.getString("ementa_destino")),
                r.getLong("id_estrutura"),r.getInt("carga_horaria_destino"),r.getString("situacao"),r.getString("parecer"),r.getString("responsavel"),
                r.getObject("registrado_em",LocalDateTime.class),r.getObject("decidido_em",LocalDateTime.class));
    }
    private List<Registro> pedidos(long matricula) throws SQLException {
        return q("SELECT * FROM pedido_equivalencia WHERE id_matricula=? ORDER BY id_pedido FOR SHARE",RepositorioEquivalenciaJdbc::ler,matricula);
    }
    private Registro buscar(long pedido) throws SQLException {
        var lista=q("SELECT * FROM pedido_equivalencia WHERE id_pedido=? FOR UPDATE",RepositorioEquivalenciaJdbc::ler,pedido);
        if(lista.isEmpty()) throw new IllegalArgumentException("Pedido não encontrado."); return lista.getFirst();
    }
    private void semImpacto(long matricula,long destino) throws SQLException {
        var inscricoes=q("SELECT i.situacao,r.situacao FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma "
                +"LEFT JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao WHERE i.id_matricula=? AND t.id_disciplina=? FOR SHARE",
                r->new String[]{r.getString(1),r.getString(2)},matricula,destino);
        for(var i:inscricoes) {
            if("ATIVO".equals(i[0])) throw new IllegalArgumentException("Destino possui inscrição em andamento; análise de impacto necessária.");
            if("APROVADO".equals(i[0]) || "APROVADO".equals(i[1])) throw new IllegalArgumentException("Destino já aprovado; não duplicar cumprimento.");
            if(!Set.of("CANCELADO","REPROVADO").contains(i[0]) || ("REPROVADO".equals(i[0]) && !"REPROVADO".equals(i[1])))
                throw new IllegalArgumentException("Situação histórica sem resultado formal; análise de impacto necessária.");
        }
    }
    @Override public Registro solicitar(Pedido pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            var ctx=contexto(pedido.matricula(),true); exigirContextoConciliado(pedido.matricula(),ctx);
            int carga=destino(ctx.estrutura(),pedido.destino());
            if(pedido.conclusao().isAfter(hoje())) throw new IllegalArgumentException("Conclusão de origem não pode ser futura.");
            semImpacto(pedido.matricula(),pedido.destino());
            if(pedidos(pedido.matricula()).stream().anyMatch(p->p.pedido().destino()==pedido.destino() && !"INDEFERIDO".equals(p.situacao())))
                throw new IllegalArgumentException("Destino já possui pedido em análise ou reconhecimento.");
            long id=gravar("INSERT INTO pedido_equivalencia(id_matricula,id_estrutura,id_disciplina_destino,instituicao_origem,disciplina_origem,"
                    +"carga_horaria_origem,ementa_origem,documentos,conclusao_origem,ementa_destino,carga_horaria_destino) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    pedido.matricula(),ctx.estrutura(),pedido.destino(),pedido.instituicao(),pedido.origem(),pedido.cargaOrigem(),
                    pedido.ementaOrigem(),pedido.documentos(),pedido.conclusao(),pedido.ementaDestino(),carga);
            return buscar(id);
        });
    }
    @Override public Registro decidir(long id,Decisao decisao) {
        HorarioAula.validarId(id); Objects.requireNonNull(decisao);
        return TransacaoJdbc.executar(c,()->{
            var previa=q("SELECT id_matricula FROM pedido_equivalencia WHERE id_pedido=?",r->r.getLong(1),id);
            if(previa.isEmpty()) throw new IllegalArgumentException("Pedido não encontrado.");
            long matricula=previa.getFirst(); var ctx=contexto(matricula,true);
            exigirContextoConciliado(matricula,ctx); var p=buscar(id);
            if(p.pedido().matricula()!=matricula || p.estrutura()!=ctx.estrutura()) throw new IllegalArgumentException("Contexto alterado; análise de impacto necessária.");
            if(!"EM_ANALISE".equals(p.situacao())) throw new IllegalArgumentException("Decisão final preservada; revisão exige análise de impacto em fluxo futuro.");
            int carga=destino(ctx.estrutura(),p.pedido().destino());
            if(carga!=p.cargaDestino()) throw new IllegalArgumentException("Carga horária de destino mudou; análise de impacto necessária.");
            if(decisao.deferir()) semImpacto(matricula,p.pedido().destino());
            gravar("UPDATE pedido_equivalencia SET situacao=?,parecer=?,responsavel=?,decidido_em=CURRENT_TIMESTAMP(6) WHERE id_pedido=? AND situacao='EM_ANALISE'",
                    decisao.deferir()?"DEFERIDO":"INDEFERIDO",decisao.parecer(),decisao.responsavel(),id);
            return buscar(id);
        });
    }
    @Override public List<Registro> listar(long matricula) {
        return TransacaoJdbc.executar(c,()->{ contexto(matricula,false); return pedidos(matricula); });
    }
    @Override public PlanoBasico plano(long matricula) {
        return TransacaoJdbc.executar(c,()->{
            var ctx=contexto(matricula,false); exigirContextoConciliado(matricula,ctx);
            if(!Set.of("ATIVO","TRANCADO","CANCELADO").contains(ctx.situacao()))
                throw new IllegalArgumentException("Situação da matrícula desconhecida; conciliação necessária.");
            var disciplinas=q("SELECT d.id_disciplina,d.nome FROM estrutura_disciplina e JOIN disciplina d ON d.id_disciplina=e.id_disciplina "
                    +"WHERE e.id_estrutura=? ORDER BY d.id_disciplina FOR SHARE",r->new Object[]{r.getLong(1),r.getString(2)},ctx.estrutura());
            var reconhecimentos=pedidos(matricula); var itens=new ArrayList<ItemPlano>(); LocalDate hoje=hoje();
            for(var d:disciplinas) {
                long id=(long)d[0]; var provas=q("SELECT i.id_inscricao,i.situacao,r.situacao,p.data_fim FROM inscricao_disciplina i "
                        +"JOIN turma t ON t.id_turma=i.id_turma JOIN periodo_letivo p ON p.id_periodo=t.id_periodo "
                        +"LEFT JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao WHERE i.id_matricula=? AND t.id_disciplina=? ORDER BY i.id_inscricao FOR SHARE",
                        r->new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getObject(4,LocalDate.class)},matricula,id);
                String estado="PENDENTE",evidencia="Sem conclusão formal ou reconhecimento.";
                for(var prova:provas) {
                    if(!Set.of("ATIVO","CANCELADO","APROVADO","REPROVADO").contains((String)prova[1])
                            || (Set.of("APROVADO","REPROVADO").contains((String)prova[1]) && !prova[1].equals(prova[2])))
                        throw new IllegalArgumentException("Inscrição histórica sem resultado formal coerente; conciliação necessária.");
                    if("ATIVO".equals(prova[1])) { estado="EM_ANDAMENTO"; evidencia="Inscrição "+prova[0]+"; ainda não concluída."; }
                }
                for(var prova:provas) if("APROVADO".equals(prova[2]) && ((LocalDate)prova[3]).isBefore(hoje)) {
                    estado="CONCLUIDA"; evidencia="Resultado formal da inscrição "+prova[0]; break;
                }
                for(var p:reconhecimentos) if(p.pedido().destino()==id && "DEFERIDO".equals(p.situacao())) {
                    if(p.estrutura()!=ctx.estrutura()) throw new IllegalArgumentException("Reconhecimento em estrutura divergente; análise de impacto necessária.");
                    estado="RECONHECIDA"; evidencia="Pedido "+p.id()+"; origem "+p.pedido().origem()+"; decisão "+p.decididoEm();
                }
                itens.add(new ItemPlano(id,(String)d[1],estado,evidencia));
            }
            return new PlanoBasico(matricula,ctx.estrutura(),ctx.situacao(),itens);
        });
    }
    @Override public List<Evento> historico(long matricula) {
        return TransacaoJdbc.executar(c,()->{
            contexto(matricula,false); var eventos=new ArrayList<Evento>();
            eventos.addAll(q("SELECT i.id_inscricao,t.id_disciplina,r.situacao,r.motivo,r.registrado_em FROM resultado_inscricao r "
                    +"JOIN inscricao_disciplina i ON i.id_inscricao=r.id_inscricao JOIN turma t ON t.id_turma=i.id_turma WHERE i.id_matricula=? FOR SHARE",
                    r->new Evento("RESULTADO",r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getObject(5,LocalDateTime.class)),matricula));
            for(var p:pedidos(matricula)) {
                eventos.add(new Evento("PEDIDO",p.id(),p.pedido().destino(),"EM_ANALISE",p.pedido().instituicao()+" / "+p.pedido().origem()+"; documentos: "+p.pedido().documentos(),p.registradoEm()));
                if(p.decididoEm()!=null) eventos.add(new Evento("DECISAO",p.id(),p.pedido().destino(),p.situacao(),
                        "Responsável declarado: "+p.responsavel()+"; parecer: "+p.parecer(),p.decididoEm()));
            }
            var legados=q("SELECT id_aproveitamento,id_disciplina_destino,decisao FROM aproveitamento_equivalencia WHERE id_matricula=? FOR SHARE",
                    r->new Evento("LEGADO_NAO_CONCILIADO",r.getLong(1),r.getLong(2),r.getString(3),"Registro legado sem reconhecimento por este fluxo.",null),matricula);
            eventos.addAll(legados);
            eventos.sort(Comparator.comparing(Evento::registradoEm,Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Evento::tipo).thenComparingLong(Evento::referencia));
            return List.copyOf(eventos);
        });
    }
}

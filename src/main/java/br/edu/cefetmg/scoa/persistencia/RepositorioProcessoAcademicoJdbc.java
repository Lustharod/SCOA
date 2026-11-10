package br.edu.cefetmg.scoa.persistencia;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia.Pedido;
import br.edu.cefetmg.scoa.persistencia.SegurancaJdbc.Sessao;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;

/** Processos aditivos; o controle_inscricao coordena decisão, inscrição e leitura do planejador. */
public final class RepositorioProcessoAcademicoJdbc {
    private final Connection c;
    public RepositorioProcessoAcademicoJdbc(Connection c) { this.c=Objects.requireNonNull(c); }
    public record Impacto(long inscricao,String fundamento,String situacao,long disciplina) { }
    public record Previa(long revisao,String situacao,List<Impacto> impactos,String assinatura) {
        public Previa { impactos=List.copyOf(impactos); }
    }
    public record ConsultaConciliacao(String tipo,long matricula,long estrutura,String situacao,String documentos,List<String> origem) { }
    public ConsultaConciliacao consultarConciliacao(Sessao sessao,long id) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.DECIDIR);
            var registros=q(c,"SELECT * FROM conciliacao_legado WHERE id_conciliacao=? FOR SHARE",
                r->new Object[]{r.getString("tipo"),r.getLong("id_origem"),r.getLong("id_matricula"),r.getLong("id_estrutura"),r.getString("situacao"),r.getString("documentos")},id);
            if(registros.isEmpty()) throw new IllegalArgumentException("Conciliação não encontrada."); var p=registros.getFirst();
            String tabela="PLANO".equals(p[0])?"plano_estudo":"aproveitamento_equivalencia";
            String chave="PLANO".equals(p[0])?"id_plano":"id_aproveitamento";
            var linhas=new ArrayList<>(q(c,"SELECT * FROM "+tabela+" WHERE "+chave+"=? FOR SHARE",RepositorioProcessoAcademicoJdbc::linha,p[1]));
            if("PLANO".equals(p[0])) linhas.addAll(q(c,"SELECT * FROM item_plano_estudo WHERE id_plano=? ORDER BY id_item_plano FOR SHARE",RepositorioProcessoAcademicoJdbc::linha,p[1]));
            return new ConsultaConciliacao((String)p[0],(long)p[2],(long)p[3],(String)p[4],(String)p[5],List.copyOf(linhas));
        });
    }
    private static String linha(ResultSet r) throws SQLException {
        var s=new StringJoiner(" | ");
        for(int i=1;i<=r.getMetaData().getColumnCount();i++) s.add(r.getMetaData().getColumnLabel(i)+"="+r.getString(i));
        return s.toString();
    }
    public List<String> eventos(Sessao sessao,String tipo,long id) {
        if(!Set.of("REVISAO","CONCILIACAO","PEDIDO").contains(tipo)) throw new IllegalArgumentException("Tipo de processo inválido.");
        return TransacaoJdbc.executar(c,()->{
            controle(c); SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.DECIDIR);
            return q(c,"SELECT id_evento,id_usuario,acao,descricao,registrado_em FROM evento_processo_academico WHERE tipo=? AND id_processo=? ORDER BY id_evento FOR SHARE",RepositorioProcessoAcademicoJdbc::linha,tipo,id);
        });
    }
    private static boolean aberta(String s) { return Set.of("ABERTA","COMPLEMENTACAO","TRATAMENTO").contains(s); }
    static String estado(Connection c,long pedido) throws SQLException {
        var estados=q(c,"SELECT situacao FROM revisao_reconhecimento WHERE id_pedido=? ORDER BY id_revisao FOR SHARE",r->r.getString(1),pedido);
        if(estados.contains("REVOGADA")) return "REVOGADA";
        if(estados.stream().anyMatch(RepositorioProcessoAcademicoJdbc::aberta)) return "SOB_REVISAO";
        return "VIGENTE";
    }
    public long solicitarRevisao(Sessao sessao,long pedido,String justificativa) {
        String motivo=Equivalencia.texto(justificativa,"justificativa",2000);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.SOLICITAR);
            var p=q(c,"SELECT situacao FROM pedido_equivalencia WHERE id_pedido=? FOR UPDATE",r->r.getString(1),pedido);
            if(p.isEmpty() || !"DEFERIDO".equals(p.getFirst()) || !"VIGENTE".equals(estado(c,pedido)))
                throw new IllegalArgumentException("Somente reconhecimento deferido vigente admite nova revisão.");
            long id=g(c,"INSERT INTO revisao_reconhecimento(id_pedido,solicitante,justificativa) VALUES (?,?,?)",pedido,autor,motivo);
            evento(c,"REVISAO",id,autor,"ABRIR",motivo+"; novos usos suspensos"); return id;
        });
    }
    private Object[] revisao(long id) throws SQLException {
        var lista=q(c,"SELECT r.id_pedido,r.situacao,p.id_matricula,p.id_disciplina_destino,p.situacao,p.id_estrutura FROM revisao_reconhecimento r JOIN pedido_equivalencia p ON p.id_pedido=r.id_pedido WHERE r.id_revisao=? FOR UPDATE",
            r->new Object[]{r.getLong(1),r.getString(2),r.getLong(3),r.getLong(4),r.getString(5),r.getLong(6)},id);
        if(lista.isEmpty()) throw new IllegalArgumentException("Revisão não encontrada."); return lista.getFirst();
    }
    private Previa previaInterna(long id) throws SQLException {
        var p=revisao(id);
        // Sem prova histórica, todas as inscrições da matrícula são candidatas conservadoras;
        // o grafo atual não comprova quais requisitos existiam no instante da inscrição antiga.
        var impactos=q(c,"SELECT i.id_inscricao,i.situacao,t.id_disciplina, "
            +"EXISTS(SELECT 1 FROM evidencia_requisito_inscricao e WHERE e.id_inscricao=i.id_inscricao AND e.id_pedido=?) AS comprovado, "
            +"EXISTS(SELECT 1 FROM evidencia_requisito_inscricao e WHERE e.id_inscricao=i.id_inscricao) AS rastreada "
            +"FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma WHERE i.id_matricula=? ORDER BY i.id_inscricao FOR SHARE",
            r->new Object[]{r.getLong(1),r.getString(2),r.getLong(3),r.getBoolean(4),r.getBoolean(5)},p[0],p[2]);
        var lista=new ArrayList<Impacto>();
        for(var i:impactos) if((boolean)i[3] || !(boolean)i[4])
            lista.add(new Impacto((long)i[0],(boolean)i[3]?"COMPROVADO":"POTENCIAL_HISTORICO",(String)i[1],(long)i[2]));
        String assinatura=hash(Arrays.toString(p)+lista+q(c,"SELECT atualizado_em FROM revisao_reconhecimento WHERE id_revisao=?",r->r.getString(1),id));
        return new Previa(id,(String)p[1],lista,assinatura);
    }
    public Previa previa(Sessao sessao,long id) {
        return TransacaoJdbc.executar(c,()->{ controle(c); SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.DECIDIR); return previaInterna(id); });
    }
    public void decidirRevisao(Sessao sessao,long id,String acao,String parecer,String assinatura) {
        String texto=Equivalencia.texto(parecer,"parecer",2000);
        if(!Set.of("REJEITAR","COMPLEMENTAR","TRATAR","REVOGAR_PRESERVANDO").contains(acao)) throw new IllegalArgumentException("Encaminhamento inválido.");
        TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.DECIDIR);
            var antes=previaInterna(id); var p=revisao(id);
            if(!aberta(antes.situacao())) throw new IllegalArgumentException("Revisão já encerrada; histórico preservado.");
            if(!antes.assinatura().equals(assinatura)) throw new IllegalArgumentException("Impactos ou processo alterados; consulte a prévia novamente.");
            if(!"DEFERIDO".equals(p[4])) throw new IllegalArgumentException("Decisão original alterada; análise necessária.");
            String estado=switch(acao) { case "REJEITAR"->"REJEITADA"; case "COMPLEMENTAR"->"COMPLEMENTACAO"; case "TRATAR"->"TRATAMENTO"; default->"REVOGADA"; };
            if("REVOGADA".equals(estado)) for(var i:antes.impactos())
                g(c,"INSERT INTO inscricao_preservada_revisao(id_revisao,id_inscricao,fundamento,situacao_observada) VALUES (?,?,?,?)",id,i.inscricao(),i.fundamento(),i.situacao());
            g(c,"UPDATE revisao_reconhecimento SET situacao=?,parecer=?,analista=?,atualizado_em=CURRENT_TIMESTAMP(6) WHERE id_revisao=?",estado,texto,autor,id);
            evento(c,"REVISAO",id,autor,acao,texto+"; inscrições expressamente preservadas/avaliadas: "+antes.impactos()); return null;
        });
    }
    public List<String> listar(Sessao sessao,long matricula) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.DECIDIR);
            var lista=new ArrayList<String>();
            lista.addAll(q(c,"SELECT r.* FROM revisao_reconhecimento r JOIN pedido_equivalencia p ON p.id_pedido=r.id_pedido WHERE p.id_matricula=? ORDER BY r.id_revisao FOR SHARE",
                r->"REVISAO "+r.getLong("id_revisao")+" pedido="+r.getLong("id_pedido")+" situação="+r.getString("situacao")+" parecer="+r.getString("parecer"),matricula));
            lista.addAll(q(c,"SELECT * FROM conciliacao_legado WHERE id_matricula=? ORDER BY id_conciliacao FOR SHARE",
                r->"CONCILIACAO "+r.getLong("id_conciliacao")+" "+r.getString("tipo")+" origem="+r.getLong("id_origem")+" situação="+r.getString("situacao")+" pedido="+r.getString("id_pedido"),matricula));
            return List.copyOf(lista);
        });
    }
    static String origem(Connection c,String tipo,long id,long matricula) throws SQLException {
        if("EQUIVALENCIA".equals(tipo)) {
            var linhas=q(c,"SELECT id_aproveitamento,id_matricula,id_disciplina_origem,id_disciplina_destino,decisao,data,resultado FROM aproveitamento_equivalencia WHERE id_aproveitamento=? AND id_matricula=? FOR SHARE",
                RepositorioProcessoAcademicoJdbc::canonico,id,matricula);
            if(linhas.isEmpty()) throw new IllegalArgumentException("Equivalência legada não encontrada nesta matrícula."); return hash(linhas.toString());
        }
        if(!"PLANO".equals(tipo)) throw new IllegalArgumentException("Tipo deve ser PLANO ou EQUIVALENCIA.");
        var plano=q(c,"SELECT id_plano,id_matricula,id_estrutura,data_atualizacao,situacao FROM plano_estudo WHERE id_plano=? AND id_matricula=? FOR SHARE",
            RepositorioProcessoAcademicoJdbc::canonico,id,matricula);
        if(plano.isEmpty()) throw new IllegalArgumentException("Plano legado não encontrado nesta matrícula.");
        var itens=q(c,"SELECT id_item_plano,id_disciplina,id_periodo_previsto,situacao FROM item_plano_estudo WHERE id_plano=? ORDER BY id_item_plano FOR SHARE",
            RepositorioProcessoAcademicoJdbc::canonico,id);
        return hash(plano.toString()+itens);
    }
    private static String canonico(ResultSet r) throws SQLException {
        var s=new StringBuilder();
        for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
            String v=r.getString(i); s.append(v==null?"N;":v.length()+":"+v+";");
        }
        return s.toString();
    }
    static void exigirLegadosConciliados(Connection c,long matricula) throws SQLException {
        for(String tipo:List.of("PLANO","EQUIVALENCIA")) {
            String tabela="PLANO".equals(tipo)?"plano_estudo":"aproveitamento_equivalencia";
            String chave="PLANO".equals(tipo)?"id_plano":"id_aproveitamento";
            for(long id:q(c,"SELECT "+chave+" FROM "+tabela+" WHERE id_matricula=? FOR SHARE",r->r.getLong(1),matricula)) {
                var conc=q(c,"SELECT origem_hash FROM conciliacao_legado WHERE tipo=? AND id_origem=? AND id_matricula=? AND situacao='APROVADA' FOR SHARE",r->r.getString(1),tipo,id,matricula);
                if(conc.isEmpty() || !conc.getFirst().equals(origem(c,tipo,id,matricula)))
                    throw new IllegalArgumentException("Legado "+tipo+" "+id+" requer conciliação explícita ou foi alterado; nenhum dado acadêmico foi promovido.");
            }
        }
    }
    public long solicitarConciliacao(Sessao sessao,String tipo,long origem,long matricula,long estrutura,String documentos) {
        String docs=Equivalencia.texto(documentos,"referências documentais",1000);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.SOLICITAR);
            contexto(matricula,estrutura); String hash=origem(c,tipo,origem,matricula);
            if(!q(c,"SELECT id_conciliacao FROM conciliacao_legado WHERE tipo=? AND id_origem=? AND situacao IN ('ABERTA','APROVADA') FOR UPDATE",r->r.getLong(1),tipo,origem).isEmpty())
                throw new IllegalArgumentException("Origem já possui processo; consulte-o antes de repetir.");
            long id=g(c,"INSERT INTO conciliacao_legado(tipo,id_origem,id_matricula,id_estrutura,origem_hash,documentos,solicitante) VALUES (?,?,?,?,?,?,?)",tipo,origem,matricula,estrutura,hash,docs,autor);
            evento(c,"CONCILIACAO",id,autor,"ABRIR",docs+"; hash origem="+hash); return id;
        });
    }
    private void contexto(long matricula,long estrutura) throws SQLException {
        var linhas=q(c,"SELECT m.id_curso,m.situacao,e.id_curso,e.situacao,a.situacao,c.situacao FROM matricula_academica m JOIN aluno a ON a.id_aluno=m.id_aluno JOIN curso c ON c.id_curso=m.id_curso JOIN estrutura_curricular e ON e.id_estrutura=? WHERE m.id_matricula=? FOR SHARE",
            r->new Object[]{r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6)},estrutura,matricula);
        if(linhas.isEmpty()) throw new IllegalArgumentException("Matrícula ou estrutura ausente.");
        var l=linhas.getFirst();
        if(!l[0].equals(l[2]) || !"ATIVO".equals(l[1]) || !"ATIVO".equals(l[3]) || !"ATIVO".equals(l[4]) || !"ATIVO".equals(l[5]))
            throw new IllegalArgumentException("Contexto incompatível ou inativo.");
        var vinculos=q(c,"SELECT id_estrutura FROM vinculo_curricular_matricula WHERE id_matricula=? FOR SHARE",r->r.getLong(1),matricula);
        if(!vinculos.isEmpty() && vinculos.getFirst()!=estrutura) throw new IllegalArgumentException("Vínculo divergente; transferência não implementada.");
    }
    public void decidirConciliacao(Sessao sessao,long id,boolean aprovar,String parecer,Pedido material) {
        String texto=Equivalencia.texto(parecer,"parecer",2000);
        TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.DECIDIR);
            var lista=q(c,"SELECT tipo,id_origem,id_matricula,id_estrutura,origem_hash,situacao FROM conciliacao_legado WHERE id_conciliacao=? FOR UPDATE",
                r->new Object[]{r.getString(1),r.getLong(2),r.getLong(3),r.getLong(4),r.getString(5),r.getString(6)},id);
            if(lista.isEmpty()) throw new IllegalArgumentException("Conciliação não encontrada."); var p=lista.getFirst();
            if(!"ABERTA".equals(p[5])) throw new IllegalArgumentException("Conciliação já decidida; histórico preservado.");
            Long pedido=null;
            if(aprovar) {
                contexto((long)p[2],(long)p[3]);
                if(!p[4].equals(origem(c,(String)p[0],(long)p[1],(long)p[2]))) throw new IllegalArgumentException("Origem alterada; aprovação bloqueada.");
                if("PLANO".equals(p[0])) {
                    if(material!=null) throw new IllegalArgumentException("Plano não promove reconhecimento.");
                    var e=q(c,"SELECT id_estrutura FROM plano_estudo WHERE id_plano=? FOR SHARE",r->r.getLong(1),p[1]).getFirst();
                    if(e!=(long)p[3]) throw new IllegalArgumentException("Plano em estrutura divergente; transferência não implementada.");
                } else {
                    if(material==null || material.matricula()!=(long)p[2]) throw new IllegalArgumentException("Informe material documental completo da matrícula.");
                    long destino=q(c,"SELECT id_disciplina_destino FROM aproveitamento_equivalencia WHERE id_aproveitamento=? FOR SHARE",r->r.getLong(1),p[1]).getFirst();
                    if(material.destino()!=destino) throw new IllegalArgumentException("Destino diverge da origem legada.");
                    var carga=q(c,"SELECT d.carga_horaria FROM estrutura_disciplina e JOIN disciplina d ON d.id_disciplina=e.id_disciplina WHERE e.id_estrutura=? AND d.id_disciplina=? AND d.situacao='ATIVO' FOR SHARE",r->r.getInt(1),p[3],destino);
                    if(carga.isEmpty()) throw new IllegalArgumentException("Destino fora da estrutura ou inativo.");
                    LocalDate hoje=q(c,"SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','-03:00'))",r->r.getObject(1,LocalDate.class)).getFirst();
                    if(material.conclusao().isAfter(hoje)) throw new IllegalArgumentException("Conclusão futura não permitida.");
                    if(!q(c,"SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND id_disciplina_destino=? AND situacao IN ('EM_ANALISE','DEFERIDO') FOR SHARE",r->r.getLong(1),p[2],destino).isEmpty()) throw new IllegalArgumentException("Destino já possui pedido/reconhecimento; não duplicar.");
                    if(!q(c,"SELECT i.id_inscricao FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma WHERE i.id_matricula=? AND t.id_disciplina=? AND i.situacao NOT IN ('CANCELADO','REPROVADO') FOR SHARE",r->r.getLong(1),p[2],destino).isEmpty()) throw new IllegalArgumentException("Destino possui inscrição com impacto; análise específica necessária.");
                    pedido=g(c,"INSERT INTO pedido_equivalencia(id_matricula,id_estrutura,id_disciplina_destino,instituicao_origem,disciplina_origem,carga_horaria_origem,ementa_origem,documentos,conclusao_origem,ementa_destino,carga_horaria_destino,situacao,parecer,responsavel,decidido_em) VALUES (?,?,?,?,?,?,?,?,?,?,?,'DEFERIDO',?,?,CURRENT_TIMESTAMP(6))",
                        p[2],p[3],destino,material.instituicao(),material.origem(),material.cargaOrigem(),material.ementaOrigem(),material.documentos(),material.conclusao(),material.ementaDestino(),carga.getFirst(),texto,"Usuário autenticado "+autor);
                }
                if(q(c,"SELECT id_matricula FROM vinculo_curricular_matricula WHERE id_matricula=? FOR SHARE",r->r.getLong(1),p[2]).isEmpty())
                    g(c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",p[2],p[3]);
            }
            g(c,"UPDATE conciliacao_legado SET situacao=?,analista=?,parecer=?,id_pedido=?,decidido_em=CURRENT_TIMESTAMP(6) WHERE id_conciliacao=?",aprovar?"APROVADA":"REJEITADA",autor,texto,pedido,id);
            evento(c,"CONCILIACAO",id,autor,aprovar?"APROVAR":"REJEITAR",texto+"; pedido formal="+pedido); return null;
        });
    }
}

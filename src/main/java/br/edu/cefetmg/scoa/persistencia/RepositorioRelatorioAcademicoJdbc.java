package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import br.edu.cefetmg.scoa.aplicacao.CodecRelatorioAcademico;
import br.edu.cefetmg.scoa.aplicacao.ExportadorRelatorioAcademico;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.security.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Prévia sem gravação; geração arquiva dados e os dois arquivos em uma transação. */
public final class RepositorioRelatorioAcademicoJdbc implements RepositorioRelatorioAcademico {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final FontesRelatorioAcademicoJdbc fontes;
    public RepositorioRelatorioAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; fontes=new FontesRelatorioAcademicoJdbc(c,sessao);
    }
    @Override public Previa preparar(Filtro f) {
        Objects.requireNonNull(f);
        return TransacaoJdbc.executar(c,()->{ controle(c); return prepararInterno(f); });
    }
    private Previa prepararInterno(Filtro f) throws SQLException {
        var problemas=new ArrayList<String>(); var d=fontes.montar(f,problemas);
        problemas.addAll(ExportadorRelatorioAcademico.validar(d));
        String xml=CodecRelatorioAcademico.escrever(d);
        return new Previa(f,d,hash(xml),problemas);
    }
    @Override public Registro gerar(Geracao pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=fontes.autorizar(pedido.filtro(),null); String assinaturaPedido=hash(autor+"\n"+pedido);
            var repetidos=q(c,"SELECT id_relatorio,assinatura_pedido FROM relatorio_academico WHERE chave=? FOR UPDATE",r->new Object[]{r.getLong(1),r.getString(2)},pedido.chave());
            if(!repetidos.isEmpty()) {
                if(!assinaturaPedido.equals(repetidos.getFirst()[1])) throw new IllegalArgumentException("Referência já usada por outra geração; consulte seu protocolo.");
                return consultarInterno((long)repetidos.getFirst()[0]);
            }
            var atual=prepararInterno(pedido.filtro());
            if(!atual.valida()) throw new IllegalArgumentException("Relatório não concluído: "+String.join("; ",atual.inconsistencias()));
            if(!atual.assinatura().equals(pedido.assinatura())) throw new IllegalArgumentException("Os dados mudaram desde a prévia; confira o relatório atual antes de gerar.");
            var agora=agoraUtc(c); String xml=CodecRelatorioAcademico.escrever(atual.documento());
            byte[] pdf=ExportadorRelatorioAcademico.pdf(atual.documento(),pedido.chave(),autor,agora,atual.assinatura());
            byte[] xlsx=ExportadorRelatorioAcademico.xlsx(atual.documento(),pedido.chave(),autor,agora,atual.assinatura());
            var f=pedido.filtro();
            long id=g(c,"""
                    INSERT INTO relatorio_academico(chave,assinatura_pedido,tipo,id_curso,id_periodo,id_turma,id_matricula,
                        id_usuario_autor,gerado_em_utc,assinatura_dados,dados_xml,pdf,pdf_sha256,xlsx,xlsx_sha256)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,pedido.chave(),assinaturaPedido,f.tipo().name(),f.curso(),f.periodo(),f.turma(),f.matricula(),autor,agora,
                    atual.assinatura(),xml,pdf,sha(pdf),xlsx,sha(xlsx));
            g(c,"""
                    INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes)
                    VALUES (?,'ACADEMICO','RELATORIO_ACADEMICO',?,'GERAR',?,?)
                    """,autor,id,agora,"Tipo "+f.tipo()+"; dados SHA-256 "+atual.assinatura()+"; PDF e XLSX preservados no registro.");
            return consultarInterno(id);
        });
    }
    @Override public Registro consultar(long id) {
        HorarioAula.validarId(id);
        return TransacaoJdbc.executar(c,()->{ controle(c); return consultarInterno(id); });
    }
    private Registro consultarInterno(long id) throws SQLException {
        var linhas=q(c,"SELECT id_relatorio,chave,tipo,id_curso,id_periodo,id_turma,id_matricula,id_usuario_autor,gerado_em_utc,assinatura_dados,dados_xml FROM relatorio_academico WHERE id_relatorio=? FOR SHARE",
                r->new Object[]{r.getLong(1),r.getString(2),filtro(r),r.getLong(8),r.getObject(9,LocalDateTime.class),r.getString(10),r.getString(11)},id);
        if(linhas.isEmpty()) throw new IllegalArgumentException("Relatório não encontrado ou não autorizado.");
        var r=linhas.getFirst(); fontes.autorizar((Filtro)r[2],(long)r[3]);
        if(!hash((String)r[6]).equals(r[5])) throw new IllegalArgumentException("Integridade dos dados arquivados divergente; relatório não disponibilizado.");
        return new Registro((long)r[0],(String)r[1],(Filtro)r[2],(long)r[3],(LocalDateTime)r[4],(String)r[5],CodecRelatorioAcademico.ler((String)r[6]));
    }
    private static Filtro filtro(ResultSet r) throws SQLException {
        return new Filtro(Tipo.valueOf(r.getString("tipo")),r.getObject("id_curso",Long.class),r.getObject("id_periodo",Long.class),r.getObject("id_turma",Long.class),r.getObject("id_matricula",Long.class));
    }
    @Override public List<Registro> meusRelatorios() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigirSessao(c,sessao); var lista=new ArrayList<Registro>();
            var rows=q(c,"SELECT id_relatorio,tipo,id_curso,id_periodo,id_turma,id_matricula FROM relatorio_academico WHERE id_usuario_autor=? ORDER BY id_relatorio FOR SHARE",
                    r->new Object[]{r.getLong(1),filtro(r)},autor);
            for(var row:rows) {
                try { fontes.autorizar((Filtro)row[1],autor); }
                catch(IllegalArgumentException e) { continue; } // Revogação não conserva acesso ao arquivo.
                lista.add(consultarInterno((long)row[0]));
            }
            return List.copyOf(lista);
        });
    }
    @Override public Arquivo arquivo(long id,Formato formato) {
        HorarioAula.validarId(id); Objects.requireNonNull(formato);
        return TransacaoJdbc.executar(c,()->{
            controle(c); consultarInterno(id);
            // O nome de coluna vem exclusivamente do enum; não é entrada textual de usuário.
            String coluna=formato==Formato.PDF?"pdf":"xlsx";
            var dados=q(c,"SELECT "+coluna+","+coluna+"_sha256 FROM relatorio_academico WHERE id_relatorio=? FOR SHARE",
                    r->new Object[]{r.getBytes(1),r.getString(2)},id).getFirst();
            byte[] bytes=(byte[])dados[0];
            if(!sha(bytes).equals(dados[1])) throw new IllegalArgumentException("Integridade do arquivo divergente; cópia não disponibilizada.");
            return new Arquivo("SCOA_relatorio_"+id+"."+coluna,formato==Formato.PDF?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",(String)dados[1],bytes);
        });
    }
    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}

package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Uso interno: ausência de seleção sempre significa base aprovada, nunca a última versão. */
final class ParametroPeriodoJdbc {
    private ParametroPeriodoJdbc() { }
    record Contexto(Long versao,String fonte,Regras regras) { }
    static Contexto porPeriodo(Connection c,long periodo) throws SQLException {
        var ids=q(c,"SELECT id_versao_parametros FROM periodo_letivo WHERE id_periodo=? FOR SHARE",r->new Long[]{r.getObject(1,Long.class)},periodo);
        if(ids.isEmpty()) throw new IllegalArgumentException("Período não encontrado.");
        Long id=ids.getFirst()[0];
        return id==null?new Contexto(null,"BASE_APROVADA",Regras.baseAprovada()):new Contexto(id,"VERSAO",versao(c,id).regras());
    }
    static Contexto porTurma(Connection c,long turma) throws SQLException {
        var ids=q(c,"SELECT id_periodo FROM turma WHERE id_turma=? FOR SHARE",r->r.getLong(1),turma);
        if(ids.isEmpty()) throw new IllegalArgumentException("Turma não encontrada.");
        return porPeriodo(c,ids.getFirst());
    }
    static Versao versao(Connection c,long id) throws SQLException {
        var linhas=q(c,"SELECT * FROM versao_parametros_academicos WHERE id_versao=? FOR SHARE",ParametroPeriodoJdbc::ler,id);
        if(linhas.isEmpty()) throw new IllegalArgumentException("Versão de parâmetros não encontrada.");
        return linhas.getFirst();
    }
    static Versao ler(ResultSet r) throws SQLException {
        var regras=new Regras(r.getInt("antecedencia_primeira"),r.getInt("duracao_primeira"),r.getInt("inicio_segunda"),r.getInt("duracao_segunda"),
                r.getInt("cancelamento_imediato_ate"),r.getInt("cancelamento_pedido_ate"),r.getInt("transferencia_ate"),r.getInt("retificacao_docente_dias"),
                r.getInt("tolerancia_atraso"),r.getBigDecimal("frequencia_percentual"),r.getBigDecimal("media_dispensa_pf"),r.getBigDecimal("media_final_minima"));
        if(!hash(regras.canonico()).equals(r.getString("assinatura"))) throw new IllegalArgumentException("Integridade da versão de parâmetros inválida; confira os dados preservados.");
        return new Versao(r.getLong("id_versao"),r.getInt("numero"),regras,r.getString("motivo"),r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",java.time.LocalDateTime.class),r.getString("assinatura"));
    }
}

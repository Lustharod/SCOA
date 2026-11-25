package br.edu.cefetmg.scoa.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

final class ApoioJdbc {
    private ApoioJdbc() { }
    static Connection abrir(boolean affectedRows) throws SQLException {
        Properties propriedades = new Properties();
        for (String[] item : new String[][]{{"user","SCOA_TEST_DB_USUARIO"},{"password","SCOA_TEST_DB_SENHA"}}) {
            String valor = System.getenv(item[1]);
            if (valor == null) throw new IllegalStateException("Configure " + item[1]);
            propriedades.setProperty(item[0], valor);
        }
        propriedades.setProperty("connectTimeout", "5000");
        propriedades.setProperty("socketTimeout", "20000");
        propriedades.setProperty("useAffectedRows", Boolean.toString(affectedRows));
        String url = System.getenv("SCOA_TEST_DB_URL");
        if (url == null) throw new IllegalStateException("Configure SCOA_TEST_DB_URL");
        Connection conexao = DriverManager.getConnection(url, propriedades);
        try {
            if (!"scoa_java_teste".equals(conexao.getCatalog())) throw new IllegalStateException("Use somente scoa_java_teste.");
            try (var consulta = conexao.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('sala','turma','periodo_letivo','horario_aula','disciplina','professor') AND ENGINE='InnoDB'");
                 var resultado = consulta.executeQuery()) {
                resultado.next();
                if (resultado.getInt(1) != 6) throw new IllegalStateException("O schema deve conter o DDL v7.5 com InnoDB.");
            }
            try (var consulta = conexao.prepareStatement("SELECT (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='matricula_academica' AND COLUMN_NAME='id_periodo_ingresso') + (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='historico_matricula' AND ENGINE='InnoDB')");
                 var resultado = consulta.executeQuery()) {
                resultado.next();
                if (resultado.getInt(1) != 2) throw new IllegalStateException("Aplique uma vez a migração V006__matricula_periodo_historico.sql em scoa_java_teste antes dos testes v0.6. Não recrie o DDL.");
            }
            try (var consulta = conexao.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_equivalencia' AND ENGINE='InnoDB'");
                 var resultado = consulta.executeQuery()) {
                resultado.next();
                if (resultado.getInt(1) != 1) throw new IllegalStateException("Aplique uma vez V008__equivalencia_manual.sql na instância de teste selecionada, após V007.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('revisao_reconhecimento','conciliacao_legado','evento_processo_academico','evidencia_requisito_inscricao','inscricao_preservada_revisao') AND engine='InnoDB'")) {
                r.next(); if(r.getInt(1)!=5) throw new IllegalStateException("Aplique uma vez V009__conciliacao_revisao.sql em scoa_java_teste após V008 antes dos testes v0.10.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema=DATABASE() AND engine='InnoDB'
                      AND table_name IN ('coordenacao_curso','cancelamento_inscricao','sucessao_periodo_curso',
                                         'retificacao_resultado','rematricula_academica','controle_calendario')
                    """)) {
                r.next();
                if(r.getInt(1)!=6) throw new IllegalStateException("Aplique V010, V011 e V012 em scoa_java_teste após V009.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='manifestacao_aluno' AND column_name='id_usuario_autor'")) {
                r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Aplique uma vez V013__autoria_manifestacao.sql em scoa_java_teste após V012.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='aproveitamento_transferencia' AND engine='InnoDB'")) {
                r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Aplique uma vez V014__transferencia_interna.sql em scoa_java_teste após V013.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='andamento_documento' AND engine='InnoDB'")) {
                r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Aplique uma vez V015__solicitacao_documento.sql em scoa_java_teste após V014.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='evento_acesso_simulado' AND engine='InnoDB'")) {
                r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Aplique uma vez V016__diario_tempos_avaliacoes.sql em scoa_java_teste após V015.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='plano_institucional' AND engine='InnoDB'")) {
                r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Aplique uma vez V017__plano_institucional_versionado.sql em scoa_java_teste após V016.");
            }
            try(var s=conexao.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='lote_rematricula' AND engine='InnoDB'")) {
                r.next(); if(r.getInt(1)!=1) throw new IllegalStateException("Aplique uma vez V018__rematricula_lote.sql em scoa_java_teste após V017.");
            }
            return conexao;
        } catch (SQLException | RuntimeException erro) {
            conexao.close();
            throw erro;
        }
    }
}

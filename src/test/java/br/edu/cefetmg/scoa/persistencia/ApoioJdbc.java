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
            return conexao;
        } catch (SQLException | RuntimeException erro) {
            conexao.close();
            throw erro;
        }
    }
}

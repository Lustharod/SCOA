package br.edu.cefetmg.scoa.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexaoMySql {
    private ConexaoMySql() { }

    public static Connection abrir() throws SQLException {
        String url = System.getenv("SCOA_DB_URL");
        if (url == null || url.isBlank()) url = "jdbc:mysql://localhost:3306/scoa";
        String usuario = System.getenv("SCOA_DB_USUARIO");
        String senha = System.getenv("SCOA_DB_SENHA");
        if (usuario == null || usuario.isBlank() || senha == null) {
            throw new IllegalStateException("Configure SCOA_DB_USUARIO e SCOA_DB_SENHA na execução do IntelliJ.");
        }
        Properties propriedades = new Properties();
        propriedades.setProperty("user", usuario);
        propriedades.setProperty("password", senha);
        propriedades.setProperty("connectTimeout", "5000");
        propriedades.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(url, propriedades);
    }
}

package br.edu.cefetmg.scoa.persistencia;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;

/** Uma conexão por sessão. Uma transação externa continua pertencendo ao chamador. */
final class TransacaoJdbc {
    private TransacaoJdbc() { }
    @FunctionalInterface interface Operacao<T> { T executar() throws SQLException; }

    static <T> T executar(Connection conexao, Operacao<T> operacao) {
        try {
            boolean propria = conexao.getAutoCommit();
            if (propria) conexao.setAutoCommit(false);
            Savepoint ponto = propria ? null : conexao.setSavepoint();
            try {
                T resultado = operacao.executar();
                if (propria) conexao.commit();
                else conexao.releaseSavepoint(ponto);
                return resultado;
            } catch (SQLException | RuntimeException | Error erro) {
                try {
                    if (propria) conexao.rollback();
                    else conexao.rollback(ponto);
                } catch (SQLException falhaRollback) {
                    erro.addSuppressed(falhaRollback);
                    // Nunca restaurar autocommit sobre uma transação cujo rollback falhou.
                    if (propria) {
                        try { conexao.close(); } catch (SQLException fechamento) { erro.addSuppressed(fechamento); }
                    }
                }
                throw erro;
            } finally {
                if (propria && !conexao.isClosed()) conexao.setAutoCommit(true);
            }
        } catch (SQLException erro) {
            throw new PersistenciaException("Não foi possível concluir a transação. Consulte os dados e tente novamente.", erro);
        }
    }
}

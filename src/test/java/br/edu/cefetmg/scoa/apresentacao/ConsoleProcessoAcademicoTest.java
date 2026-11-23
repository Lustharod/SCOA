package br.edu.cefetmg.scoa.apresentacao;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleProcessoAcademicoTest {
    @Test void criacaoCanceladaPedeSomenteAcessoTecnicoSemLerSenhaOuGravar() {
        String saida=executar("4\npessoa\n\nN\n0\n");
        assertTrue(saida.contains("Acesso técnico inicial (SEM_PERMISSOES/ADMIN/SOLICITANTE/ANALISTA/BIBLIOTECARIO"));
        assertTrue(saida.contains("Os papéis acadêmicos serão obtidos dos vínculos em 18.11."));
        assertFalse(saida.contains("Papel ("));
        assertFalse(saida.contains("Conta criada;"));
    }
    @Test void propriaContaSemSessaoERecusadaAntesDeLerDados() {
        assertTrue(executar("12\n0\n").contains("Entre com uma sessão válida no menu 18."));
    }
    private String executar(String opcoes) {
        var conexao=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,metodo,args)->{
            return switch(metodo.getName()) {
                case "getAutoCommit" -> true;
                case "setAutoCommit", "rollback", "commit" -> null;
                case "isClosed" -> false;
                default -> throw new AssertionError("A operação não deve acessar dados: "+metodo.getName());
            };
        });
        var original=System.out;
        var bytes=new ByteArrayOutputStream();
        try(var saida=new PrintStream(bytes,true,StandardCharsets.UTF_8); var entrada=new Scanner(opcoes)) {
            System.setOut(saida);
            new ConsoleProcessoAcademico(entrada,conexao,null).executar();
        } finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}

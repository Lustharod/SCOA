package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.ServicoSala;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Executado apenas com o perfil mysql-it e com o schema exclusivo scoa_java_teste. */
class SalaJdbcIT {
    private Connection conexao;
    private ServicoSala servico;
    private RepositorioSalaJdbc repositorio;
    private String etiqueta;

    @BeforeEach
    void preparar() throws SQLException {
        Properties propriedades = new Properties();
        propriedades.setProperty("user", exigirVariavel("SCOA_TEST_DB_USUARIO"));
        propriedades.setProperty("password", exigirVariavel("SCOA_TEST_DB_SENHA"));
        propriedades.setProperty("connectTimeout", "5000");
        propriedades.setProperty("socketTimeout", "10000");
        conexao = DriverManager.getConnection(exigirVariavel("SCOA_TEST_DB_URL"), propriedades);
        if (!"scoa_java_teste".equals(conexao.getCatalog())) {
            conexao.close();
            conexao = null;
            fail("Use o schema exclusivo scoa_java_teste para os testes de integração Java.");
        }
        try (var consulta = conexao.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sala'");
             var resultado = consulta.executeQuery()) {
            assertTrue(resultado.next() && "InnoDB".equals(resultado.getString(1)), "A tabela sala deve existir e ser InnoDB.");
        }
        conexao.setAutoCommit(false);
        repositorio = new RepositorioSalaJdbc(conexao);
        servico = new ServicoSala(repositorio);
        etiqueta = "CTJAVA-" + UUID.randomUUID().toString().substring(0, 12);
    }

    @AfterEach
    void desfazer() throws SQLException {
        if (conexao != null) {
            try {
                conexao.rollback();
                if (etiqueta != null) {
                    try (var consulta = conexao.prepareStatement("SELECT COUNT(*) FROM sala WHERE identificacao LIKE ?")) {
                        consulta.setString(1, etiqueta + "%");
                        try (var resultado = consulta.executeQuery()) {
                            resultado.next();
                            assertEquals(0, resultado.getInt(1), "Os registros de teste devem ser desfeitos.");
                        }
                    }
                }
            } finally { conexao.close(); }
        }
    }

    @Test
    void ctJiSala001CadastraERecuperaTodosOsCampos() {
        Sala cadastrada = servico.cadastrar(etiqueta, "Bloco A — laboratório", 30);
        Sala lida = servico.buscar(cadastrada.getId());
        assertNotNull(lida.getId());
        assertEquals(etiqueta, lida.getIdentificacao());
        assertEquals("Bloco A — laboratório", lida.getLocalizacao());
        assertEquals(30, lida.getCapacidade());
        assertEquals("ATIVO", lida.getSituacao());
    }

    @Test
    void ctJiSala002AtualizaSemCriarOutraSala() {
        Sala inicial = servico.cadastrar(etiqueta, "Bloco A", 30);
        servico.atualizar(inicial.getId(), etiqueta + " NOVA", "Bloco B", 20);
        Sala atualizada = servico.buscar(inicial.getId());
        assertEquals("Bloco B", atualizada.getLocalizacao());
        assertEquals(20, atualizada.getCapacidade());
        assertEquals(1, servico.listar().stream().filter(s -> s.getIdentificacao().startsWith(etiqueta)).count());
    }

    @Test
    void ctJiSala003InativaPreservandoRegistro() {
        Sala inicial = servico.cadastrar(etiqueta, "Bloco A", 30);
        servico.inativar(inicial.getId());
        assertEquals("INATIVO", servico.buscar(inicial.getId()).getSituacao());
    }

    @Test
    void ctJiSala004TextoComAspasEComandoSqlPermaneceDado() {
        String identificacao = etiqueta + " O'Connor; --";
        Sala cadastrada = servico.cadastrar(identificacao, "Bloco A", 30);
        assertEquals(identificacao, servico.buscar(cadastrada.getId()).getIdentificacao());
    }

    @Test
    void ctJiSala005SalaRemovidaNaoERecriadaPorAtualizacao() throws SQLException {
        Sala inicial = servico.cadastrar(etiqueta, "Bloco A", 30);
        try (var comando = conexao.prepareStatement("DELETE FROM sala WHERE id_sala = ?")) {
            comando.setLong(1, inicial.getId());
            comando.executeUpdate();
        }
        assertTrue(repositorio.buscarPorId(inicial.getId()).isEmpty());
        assertThrows(SalaNaoEncontradaException.class, () -> repositorio.salvar(inicial));
    }

    @Test
    void ctJiSala006SalvarSemAlteracoesNaoAcusaAusencia() {
        Sala inicial = servico.cadastrar(etiqueta, "Bloco A", 30);
        assertDoesNotThrow(() -> repositorio.salvar(inicial));
        assertEquals(etiqueta, servico.buscar(inicial.getId()).getIdentificacao());
    }

    private static String exigirVariavel(String nome) {
        String valor = System.getenv(nome);
        if (valor == null) throw new IllegalStateException("Configure " + nome + " para executar o perfil mysql-it.");
        return valor;
    }
}

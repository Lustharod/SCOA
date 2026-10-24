package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RepositorioSala;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicoSalaTest {
    private RepositorioEmMemoria repositorio;
    private ServicoSala servico;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEmMemoria();
        servico = new ServicoSala(repositorio);
    }

    @Test
    void ctJuSala011CadastraSalaEDevolveIdentificador() {
        Sala sala = servico.cadastrar("Sala 101", "Bloco A", 30);
        assertNotNull(sala.getId());
        assertEquals("Sala 101", servico.buscar(sala.getId()).getIdentificacao());
        assertEquals(1, repositorio.gravacoes);
    }

    @Test
    void ctJuSala012DadoInvalidoNaoChegaAoRepositorio() {
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar("Sala 101", "Bloco A", 0));
        assertEquals(0, repositorio.gravacoes);
        assertTrue(servico.listar().isEmpty());
    }

    @Test
    void ctJuSala013AtualizaMantendoIdentidadeESituacao() {
        Sala inicial = servico.cadastrar("Sala 101", "Bloco A", 30);
        Sala atualizada = servico.atualizar(inicial.getId(), "Laboratório 1", "Bloco B", 20);
        assertEquals(inicial.getId(), atualizada.getId());
        assertEquals("ATIVO", atualizada.getSituacao());
        assertEquals("Laboratório 1", servico.buscar(inicial.getId()).getIdentificacao());
        assertEquals(1, servico.listar().size());
    }

    @Test
    void ctJuSala014AtualizacaoInvalidaPreservaCadastroAnterior() {
        Sala inicial = servico.cadastrar("Sala 101", "Bloco A", 30);
        assertThrows(IllegalArgumentException.class, () -> servico.atualizar(inicial.getId(), "Sala alterada", "Bloco B", 0));
        Sala mantida = servico.buscar(inicial.getId());
        assertEquals("Sala 101", mantida.getIdentificacao());
        assertEquals(30, mantida.getCapacidade());
        assertEquals(1, repositorio.gravacoes);
    }

    @Test
    void ctJuSala015InativaSemExcluirHistoricoDoCadastro() {
        Sala inicial = servico.cadastrar("Sala 101", "Bloco A", 30);
        Sala inativa = servico.inativar(inicial.getId());
        assertEquals(inicial.getId(), inativa.getId());
        assertEquals("INATIVO", servico.buscar(inicial.getId()).getSituacao());
        assertEquals(1, servico.listar().size());
    }

    @Test
    void ctJuSala016RepetirInativacaoMantemOEstado() {
        Sala inicial = servico.cadastrar("Sala 101", "Bloco A", 30);
        servico.inativar(inicial.getId());
        servico.inativar(inicial.getId());
        assertEquals("INATIVO", servico.buscar(inicial.getId()).getSituacao());
        assertEquals(1, servico.listar().size());
    }

    @Test
    void ctJuSala017OperacoesSobreSalaAusenteSaoRejeitadas() {
        assertAll(
                () -> assertThrows(SalaNaoEncontradaException.class, () -> servico.buscar(1L)),
                () -> assertThrows(SalaNaoEncontradaException.class, () -> servico.atualizar(1L, "Sala 101", "Bloco A", 30)),
                () -> assertThrows(SalaNaoEncontradaException.class, () -> servico.inativar(1L))
        );
        assertEquals(0, repositorio.gravacoes);
    }

    @Test
    void ctJuSala018ListagemNaoPermiteAlterarOsCadastrosPeloRetorno() {
        servico.cadastrar("Sala 101", "Bloco A", 30);
        List<Sala> retorno = servico.listar();
        assertThrows(UnsupportedOperationException.class, retorno::clear);
        assertEquals(1, servico.listar().size());
    }

    @Test
    void ctJuSala019AtualizarSalaInativaNaoReativaImplicitamente() {
        Sala inicial = servico.cadastrar("Sala 101", "Bloco A", 30);
        servico.inativar(inicial.getId());
        Sala atualizada = servico.atualizar(inicial.getId(), "Sala 102", "Bloco B", 40);
        assertEquals("INATIVO", atualizada.getSituacao());
    }

    /** Colaborador em memória: o teste do serviço não depende de servidor ou JDBC. */
    private static final class RepositorioEmMemoria implements RepositorioSala {
        private final Map<Long, Sala> salas = new LinkedHashMap<>();
        private long proximoId = 1;
        private int gravacoes;

        @Override
        public Sala salvar(Sala sala) {
            long id = sala.getId() == null ? proximoId++ : sala.getId();
            Sala salva = new Sala(id, sala.getIdentificacao(), sala.getLocalizacao(), sala.getCapacidade(), sala.getSituacao());
            salas.put(id, salva);
            gravacoes++;
            return salva;
        }

        @Override public Optional<Sala> buscarPorId(long id) { return Optional.ofNullable(salas.get(id)); }
        @Override public List<Sala> listar() { return new ArrayList<>(salas.values()); }
    }
}

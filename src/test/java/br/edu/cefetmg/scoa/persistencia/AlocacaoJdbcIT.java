package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.ServicoAlocacao;
import br.edu.cefetmg.scoa.aplicacao.ServicoSala;
import br.edu.cefetmg.scoa.dominio.Sala;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Dados sintéticos com IDs próprios; somente scoa_java_teste. Concorrência usa conexões distintas. */
class AlocacaoJdbcIT {
    private Connection conexao;
    private ServicoAlocacao servico;
    private ServicoSala salas;
    private long disciplina, professor, turma, sala;
    private final List<Long> turmas = new ArrayList<>(), periodos = new ArrayList<>(), idsSalas = new ArrayList<>();
    private String etiqueta;
    private static final LocalTime OITO = LocalTime.of(8,0), DEZ = LocalTime.of(10,0);
    private static final int ANO = LocalDate.now().getYear() + 1;

    @BeforeEach void preparar() throws Exception {
        conexao = ApoioJdbc.abrir(false);
        conexao.setAutoCommit(false);
        etiqueta = "CTALOC-" + UUID.randomUUID().toString().substring(0,12);
        professor = inserir("INSERT INTO professor (identificacao,situacao) VALUES (?, 'ATIVO')", etiqueta);
        disciplina = inserir("INSERT INTO disciplina (codigo,nome,carga_horaria,situacao) VALUES (?, ?, 60, 'ATIVO')", etiqueta, etiqueta);
        long periodo = periodo(ANO + "-02-01", ANO + "-06-30");
        turma = turma(30, periodo);
        sala = sala(40);
        servico = new ServicoAlocacao(new RepositorioAlocacaoJdbc(conexao));
        salas = new ServicoSala(new RepositorioSalaJdbc(conexao));
    }

    @AfterEach void limpar() throws Exception {
        if (conexao == null) return;
        try {
            if (!conexao.getAutoCommit()) conexao.rollback();
            conexao.setAutoCommit(false);
            // Os testes concorrentes precisam de fixtures confirmadas para outras conexões.
            // Removemos apenas os IDs desta execução, inclusive após uma asserção falhar.
            for (long id : turmas) executar("DELETE FROM horario_aula WHERE id_turma=?", id);
            for (long id : turmas) executar("DELETE FROM turma WHERE id_turma=?", id);
            for (long id : idsSalas) executar("DELETE FROM sala WHERE id_sala=?", id);
            for (long id : periodos) executar("DELETE FROM periodo_letivo WHERE id_periodo=?", id);
            if (disciplina != 0) executar("DELETE FROM disciplina WHERE id_disciplina=?", disciplina);
            if (professor != 0) executar("DELETE FROM professor WHERE id_professor=?", professor);
            if (!conexao.getAutoCommit()) conexao.commit();
        } finally { conexao.close(); }
    }

    @Test void alocaListaEDesaloca() {
        var horario = servico.alocar(turma, sala, 1, OITO, DEZ);
        assertNotNull(horario.id());
        assertEquals(horario, servico.listarHorarios(turma).getFirst());
        assertEquals(turma, servico.listarTurmas().stream().filter(t -> t.id()==turma).findFirst().orElseThrow().id());
        assertEquals(30,servico.buscarTurma(turma).capacidade());
        servico.desalocar(horario.id());
        assertTrue(servico.listarHorarios(turma).isEmpty());
        assertTrue(servico.listarDisponiveis(turma,1,OITO,DEZ).stream().anyMatch(s -> s.getId()==sala));
    }

    @Test void rejeitaCapacidadeInsuficienteESalaInativaSemGravar() throws Exception {
        long pequena = sala(29);
        assertThrows(IllegalArgumentException.class, () -> servico.alocar(turma,pequena,1,OITO,DEZ));
        salas.inativar(sala);
        assertThrows(IllegalArgumentException.class, () -> servico.alocar(turma,sala,1,OITO,DEZ));
        assertTrue(servico.listarHorarios(turma).isEmpty());
    }

    @Test void disponibilidadeFiltraCapacidadeSituacaoEConflitos() throws Exception {
        long pequena = sala(29), inativa = sala(40), livre = sala(30);
        salas.inativar(inativa);
        long outra = turma(30,periodos.getFirst());
        servico.alocar(outra,sala,1,OITO,DEZ);
        var disponiveis = servico.listarDisponiveis(turma,1,OITO,DEZ);
        assertFalse(disponiveis.stream().anyMatch(s -> s.getId()==sala || s.getId()==pequena || s.getId()==inativa));
        assertTrue(disponiveis.stream().anyMatch(s -> s.getId()==livre));
    }

    @Test void rejeitaConflitoDaSalaEntrePeriodosComDatasSobrepostas() throws Exception {
        long outra = turma(30,periodo(ANO + "-05-01",ANO + "-10-31"));
        servico.alocar(turma,sala,1,OITO,DEZ);
        assertThrows(IllegalArgumentException.class, () -> servico.alocar(outra,sala,1,LocalTime.of(9,0),LocalTime.of(11,0)));
        assertTrue(servico.listarHorarios(outra).isEmpty());
    }

    @Test void rejeitaMesmaTurmaEmDuasSalasNoMesmoHorario() throws Exception {
        long outraSala = sala(40);
        servico.alocar(turma,sala,1,OITO,DEZ);
        assertThrows(IllegalArgumentException.class, () -> servico.alocar(turma,outraSala,1,OITO,DEZ));
    }

    @Test void permiteAdjacenciaEDiasDiferentes() {
        servico.alocar(turma,sala,1,OITO,DEZ);
        assertDoesNotThrow(() -> servico.alocar(turma,sala,1,DEZ,LocalTime.NOON));
        assertDoesNotThrow(() -> servico.alocar(turma,sala,2,OITO,DEZ));
        assertEquals(3,servico.listarHorarios(turma).size());
    }

    @Test void permitePeriodosSemSobreposicao() throws Exception {
        long outra = turma(30,periodo((ANO+1) + "-02-01",(ANO+1) + "-06-30"));
        servico.alocar(turma,sala,1,OITO,DEZ);
        assertDoesNotThrow(() -> servico.alocar(outra,sala,1,OITO,DEZ));
    }

    @Test void recusaEntidadesAusentes() {
        assertThrows(IllegalArgumentException.class, () -> servico.alocar(4_294_967_295L,sala,1,OITO,DEZ));
        assertThrows(br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException.class,
                () -> servico.alocar(turma,4_294_967_295L,1,OITO,DEZ));
        assertThrows(IllegalArgumentException.class, () -> servico.desalocar(4_294_967_295L));
    }

    @Test void preservaCapacidadeESituacaoDeSalaComprometida() {
        servico.alocar(turma,sala,1,OITO,DEZ);
        assertThrows(IllegalArgumentException.class, () -> salas.atualizar(sala,"Reduzida","A",29));
        assertThrows(IllegalArgumentException.class, () -> salas.inativar(sala));
        assertEquals(40,salas.buscar(sala).getCapacidade());
        assertEquals("ATIVO",salas.buscar(sala).getSituacao());
    }

    @Test void transacaoExternaContinuaAbertaEAutocommitProprioERestaurado() throws Exception {
        var horario = servico.alocar(turma,sala,1,OITO,DEZ);
        assertFalse(conexao.getAutoCommit());
        conexao.rollback();
        try (var consulta=conexao.prepareStatement("SELECT COUNT(*) FROM horario_aula WHERE id_horario=?")) {
            consulta.setLong(1,horario.id());
            try (var resultado=consulta.executeQuery()) { resultado.next(); assertEquals(0,resultado.getInt(1)); }
        }
        // Fixtures também foram desfeitas pelo rollback; a nova conexão testa o contrato sem depender delas.
        try (var outra=ApoioJdbc.abrir(false)) {
            var repositorio=new RepositorioSalaJdbc(outra);
            assertThrows(br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException.class,
                    () -> repositorio.alterar(4_294_967_295L,s -> s));
            assertTrue(outra.getAutoCommit());
        }
    }

    @Test void atualizacaoSemMudancasComAffectedRowsNaoAcusaAusencia() throws Exception {
        confirmarFixtures();
        try (var outra=ApoioJdbc.abrir(true)) {
            assertDoesNotThrow(() -> new ServicoSala(new RepositorioSalaJdbc(outra)).atualizar(sala,etiqueta,"Bloco teste",40));
            assertTrue(outra.getAutoCommit());
        }
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void alteracoesConcorrentesPreservamInativacaoECapacidade(boolean inativarPrimeiro) throws Exception {
        salas.atualizar(sala,etiqueta,"Bloco teste",30);
        confirmarFixtures();
        CountDownLatch bloqueada=new CountDownLatch(1), liberar=new CountDownLatch(1), iniciouSegunda=new CountDownLatch(1);
        try (var executores=Executors.newFixedThreadPool(2)) {
            var primeira=executores.submit(() -> {
                try (var c=ApoioJdbc.abrir(false)) {
                    return new RepositorioSalaJdbc(c).alterar(sala,atual -> {
                        bloqueada.countDown(); esperar(liberar);
                        return new Sala(sala,atual.getIdentificacao(),atual.getLocalizacao(),inativarPrimeiro?atual.getCapacidade():40,
                                inativarPrimeiro?"INATIVO":atual.getSituacao());
                    });
                }
            });
            java.util.concurrent.Future<Sala> segunda=null;
            try {
                assertTrue(bloqueada.await(5,TimeUnit.SECONDS));
                segunda=executores.submit(() -> {
                    try (var c=ApoioJdbc.abrir(false)) {
                        iniciouSegunda.countDown();
                        var s=new ServicoSala(new RepositorioSalaJdbc(c));
                        return inativarPrimeiro?s.atualizar(sala,etiqueta,"Bloco teste",40):s.inativar(sala);
                    }
                });
                assertTrue(iniciouSegunda.await(5,TimeUnit.SECONDS));
                var aguardando=segunda;
                assertThrows(TimeoutException.class, () -> aguardando.get(200,TimeUnit.MILLISECONDS));
            } finally { liberar.countDown(); }
            primeira.get(10,TimeUnit.SECONDS);
            assertNotNull(segunda);
            segunda.get(10,TimeUnit.SECONDS);
        }
        assertEquals("INATIVO",salas.buscar(sala).getSituacao());
        assertEquals(40,salas.buscar(sala).getCapacidade());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean mesmaTurma) throws Exception {
        long segundaTurma=mesmaTurma?turma:turma(30,periodos.getFirst());
        long segundaSala=mesmaTurma?sala(40):sala;
        confirmarFixtures();
        conexao.setAutoCommit(false);
        servico.alocar(turma,sala,1,OITO,DEZ);
        CountDownLatch iniciou=new CountDownLatch(1);
        try (var executores=Executors.newSingleThreadExecutor()) {
            var segunda=executores.submit(() -> {
                try (var c=ApoioJdbc.abrir(false)) {
                    iniciou.countDown();
                    return new ServicoAlocacao(new RepositorioAlocacaoJdbc(c)).alocar(segundaTurma,segundaSala,1,OITO,DEZ);
                }
            });
            try {
                assertTrue(iniciou.await(5,TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> segunda.get(200,TimeUnit.MILLISECONDS));
            } finally { conexao.commit(); }
            var erro=assertThrows(ExecutionException.class, () -> segunda.get(10,TimeUnit.SECONDS));
            assertInstanceOf(IllegalArgumentException.class,erro.getCause());
        }
        conexao.setAutoCommit(true);
        assertEquals(1,servico.listarHorarios(turma).size());
        if (!mesmaTurma) assertTrue(servico.listarHorarios(segundaTurma).isEmpty());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean inativar) throws Exception {
        confirmarFixtures();
        conexao.setAutoCommit(false);
        servico.alocar(turma,sala,1,OITO,DEZ);
        CountDownLatch iniciou=new CountDownLatch(1);
        try (var executores=Executors.newSingleThreadExecutor()) {
            var inativacao=executores.submit(() -> {
                try (var c=ApoioJdbc.abrir(false)) {
                    iniciou.countDown();
                    var s = new ServicoSala(new RepositorioSalaJdbc(c));
                    return inativar ? s.inativar(sala) : s.atualizar(sala,etiqueta,"Bloco teste",29);
                }
            });
            try {
                assertTrue(iniciou.await(5,TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> inativacao.get(200,TimeUnit.MILLISECONDS));
            } finally { conexao.commit(); }
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(ExecutionException.class, () -> inativacao.get(10,TimeUnit.SECONDS)).getCause());
        }
        conexao.setAutoCommit(true);
        assertEquals("ATIVO",salas.buscar(sala).getSituacao());
        assertEquals(40,salas.buscar(sala).getCapacidade());
    }

    private void confirmarFixtures() throws Exception { conexao.commit(); conexao.setAutoCommit(true); }
    private long periodo(String inicio,String fim) throws Exception {
        long id=inserir("INSERT INTO periodo_letivo (descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",etiqueta,inicio,fim);
        periodos.add(id); return id;
    }
    private long turma(int capacidade,long periodo) throws Exception {
        long id=inserir("INSERT INTO turma (id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,?,'ATIVO')",
                disciplina,professor,periodo,etiqueta,capacidade);
        turmas.add(id); return id;
    }
    private long sala(int capacidade) {
        Sala nova=new RepositorioSalaJdbc(conexao).salvar(Sala.nova(etiqueta,"Bloco teste",capacidade));
        idsSalas.add(nova.getId()); return nova.getId();
    }
    private long inserir(String sql,Object... valores) throws Exception {
        try (var comando=conexao.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for (int i=0;i<valores.length;i++) comando.setObject(i+1,valores[i]);
            comando.executeUpdate();
            try (var chaves=comando.getGeneratedKeys()) { assertTrue(chaves.next()); return chaves.getLong(1); }
        }
    }
    private void executar(String sql,long id) throws Exception {
        try (var comando=conexao.prepareStatement(sql)) { comando.setLong(1,id); comando.executeUpdate(); }
    }
    private static void esperar(CountDownLatch sinal) {
        try { if (!sinal.await(8,TimeUnit.SECONDS)) throw new IllegalStateException("Tempo de espera excedido."); }
        catch (InterruptedException erro) { Thread.currentThread().interrupt(); throw new IllegalStateException(erro); }
    }
}

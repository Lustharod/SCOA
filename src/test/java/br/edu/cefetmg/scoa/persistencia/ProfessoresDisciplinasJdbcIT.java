package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.aplicacao.*;
import br.edu.cefetmg.scoa.apresentacao.ConsoleProfessoresDisciplinas;
import br.edu.cefetmg.scoa.dominio.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Fixtures exclusivas. Evidências do Codex, separadas da validação do autor. */
class ProfessoresDisciplinasJdbcIT {
    private Connection c;
    private ServicoProfessoresDisciplinas servico;
    private RepositorioProfessoresDisciplinasJdbc repositorio;
    private ServicoCadastroAcademico academico;
    private Professor professor;
    private Disciplina a,b,d;
    private String etiqueta;
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); c.setAutoCommit(false);
        etiqueta="CTP04-"+UUID.randomUUID().toString().substring(0,10);
        repositorio=new RepositorioProfessoresDisciplinasJdbc(c);
        servico=new ServicoProfessoresDisciplinas(repositorio);
        academico=new ServicoCadastroAcademico(new RepositorioCadastroAcademicoJdbc(c));
        professor=servico.cadastrarProfessor(etiqueta+" Professor");
        a=servico.cadastrarDisciplina(etiqueta+"A",etiqueta+" Disciplina A",60);
        b=servico.cadastrarDisciplina(etiqueta+"B",etiqueta+" Disciplina B",60);
        d=servico.cadastrarDisciplina(etiqueta+"D",etiqueta+" Disciplina D",60);
    }
    @AfterEach void limpar() throws Exception {
        if(c==null) return;
        try {
            if(!c.getAutoCommit()) c.rollback();
            c.setAutoCommit(false);
            var turmas=ids("SELECT id_turma FROM turma WHERE codigo LIKE ?",etiqueta+"%");
            for(long id:turmas) sql("DELETE FROM horario_aula WHERE id_turma=?",id);
            for(long id:turmas) sql("DELETE FROM turma WHERE id_turma=?",id);
            for(long id:ids("SELECT id_periodo FROM periodo_letivo WHERE descricao LIKE ?",etiqueta+"%")) sql("DELETE FROM periodo_letivo WHERE id_periodo=?",id);
            for(long id:ids("SELECT id_disciplina FROM disciplina WHERE codigo LIKE ?",etiqueta+"%")) {
                sql("DELETE FROM disciplina_prerequisito WHERE id_disciplina=? OR id_disciplina_prerequisito=?",id,id);
                sql("DELETE FROM disciplina WHERE id_disciplina=?",id);
            }
            for(long id:ids("SELECT id_professor FROM professor WHERE identificacao LIKE ?",etiqueta+"%")) sql("DELETE FROM professor WHERE id_professor=?",id);
            for(long id:ids("SELECT id_usuario FROM usuario WHERE identificacao LIKE ?",etiqueta+"%")) sql("DELETE FROM usuario WHERE id_usuario=?",id);
            c.commit();
        } finally { c.close(); }
    }
    @Test void p0402ProfessorMantemContaIdentidadeESituacao() throws Exception {
        long usuario=inserir("INSERT INTO usuario (identificacao,credencial_hash,situacao) VALUES (?,'hash-sintetico','ATIVO')",etiqueta+" Usuario");
        sql("UPDATE professor SET id_usuario=? WHERE id_professor=?",usuario,professor.id());
        var atualizado=servico.atualizarProfessor(professor.id(),etiqueta+" Atualizado");
        assertEquals(professor.id(),atualizado.id());assertEquals(usuario,atualizado.idUsuario());assertEquals("ATIVO",atualizado.situacao());
        assertEquals(atualizado,servico.buscarProfessor(professor.id()));assertTrue(servico.listarProfessores().contains(atualizado));
        assertEquals(1,ids("SELECT id_professor FROM professor WHERE identificacao LIKE ?",etiqueta+"%").size());
    }
    @Test void p0403InativacaoPreservaTurmaAnteriorENaoReativa() {
        var turma=novaTurma(true);
        assertEquals("INATIVO",servico.inativarProfessor(professor.id()).situacao());
        assertEquals("INATIVO",servico.inativarProfessor(professor.id()).situacao());
        assertEquals("INATIVO",servico.atualizarProfessor(professor.id(),etiqueta+" Corrigido").situacao());
        assertEquals(professor.id().longValue(),academico.buscarTurma(turma.id()).idProfessor());
        assertTrue(servico.listarProfessores().stream().anyMatch(p -> p.id().equals(professor.id())));
    }
    @Test void p0404ProfessorComprometidoNaoInativa() {
        novaTurma(false);
        assertThrows(IllegalArgumentException.class,() -> servico.inativarProfessor(professor.id()));
        assertEquals("ATIVO",servico.buscarProfessor(professor.id()).situacao());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void p0404ConcorrenciaProfessorETurmaNasDuasOrdens(boolean inativarPrimeiro) throws Exception {
        var periodo=novoPeriodo(false); c.commit();c.setAutoCommit(false);
        if(inativarPrimeiro) servico.inativarProfessor(professor.id());
        else academico.cadastrarTurma(a.id(),professor.id(),periodo.id(),etiqueta+"Turma",30);
        var iniciou=new CountDownLatch(1);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var futuro=executor.submit(() -> {
                try(var outra=ApoioJdbc.abrir(false)) {
                    iniciou.countDown();
                    if(inativarPrimeiro) return (Object)new ServicoCadastroAcademico(new RepositorioCadastroAcademicoJdbc(outra))
                            .cadastrarTurma(a.id(),professor.id(),periodo.id(),etiqueta+"Turma",30);
                    return new ServicoProfessoresDisciplinas(new RepositorioProfessoresDisciplinasJdbc(outra)).inativarProfessor(professor.id());
                }
            });
            try { assertTrue(iniciou.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,() -> futuro.get(200,TimeUnit.MILLISECONDS)); }
            finally { c.commit(); }
            assertInstanceOf(IllegalArgumentException.class,assertThrows(ExecutionException.class,() -> futuro.get(10,TimeUnit.SECONDS)).getCause());
        }
        c.setAutoCommit(true);
        assertEquals(inativarPrimeiro?"INATIVO":"ATIVO",servico.buscarProfessor(professor.id()).situacao());
        assertEquals(inativarPrimeiro?0:1,ids("SELECT id_turma FROM turma WHERE codigo LIKE ?",etiqueta+"%").size());
    }
    @Test void p0406DisciplinaMantemIdentidadeESituacaoSemMudancas() throws Exception {
        sql("UPDATE disciplina SET situacao='INATIVO' WHERE id_disciplina=?",a.id());
        var atual=servico.atualizarDisciplina(a.id(),etiqueta+"X",etiqueta+" Nome alterado",80);
        assertEquals(a.id(),atual.id());assertEquals("INATIVO",atual.situacao());
        assertEquals(atual,servico.buscarDisciplina(a.id()));assertTrue(servico.listarDisciplinas().contains(atual));
        assertEquals(atual,servico.atualizarDisciplina(a.id(),atual.codigo(),atual.nome(),atual.cargaHoraria()));
        c.commit();
        try(var outra=ApoioJdbc.abrir(true)) {
            var s=new ServicoProfessoresDisciplinas(new RepositorioProfessoresDisciplinasJdbc(outra));
            assertEquals(atual,s.atualizarDisciplina(a.id(),atual.codigo(),atual.nome(),atual.cargaHoraria()));
        }
    }
    @Test void p0407AusenciasNaoCriamRegistrosNemRelacoes() {
        long ausente=4_294_967_295L;
        assertAll(
            () -> assertThrows(IllegalArgumentException.class,() -> servico.buscarProfessor(ausente)),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.atualizarProfessor(ausente,"X")),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.inativarProfessor(ausente)),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.buscarDisciplina(ausente)),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.atualizarDisciplina(ausente,"X","X",60)),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.listarPreRequisitos(ausente)),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.adicionarPreRequisito(a.id(),ausente)),
            () -> assertThrows(IllegalArgumentException.class,() -> servico.adicionarPreRequisito(ausente,a.id())));
        assertTrue(servico.listarPreRequisitos(a.id()).isEmpty());
    }
    @Test void p0408AdicionaConsultaRemoveERecusaDuplicidade() {
        servico.adicionarPreRequisito(a.id(),b.id());assertEquals(List.of(b),servico.listarPreRequisitos(a.id()));
        assertTrue(servico.listarPreRequisitos(b.id()).isEmpty());
        assertThrows(IllegalArgumentException.class,() -> servico.adicionarPreRequisito(a.id(),b.id()));
        servico.removerPreRequisito(a.id(),b.id());assertTrue(servico.listarPreRequisitos(a.id()).isEmpty());
        assertThrows(IllegalArgumentException.class,() -> servico.removerPreRequisito(a.id(),b.id()));
        assertEquals(a,servico.buscarDisciplina(a.id()));assertEquals(b,servico.buscarDisciplina(b.id()));
    }
    @Test void p0409RecusaAutorreferenciaECiclosDiretosEIndiretos() {
        assertThrows(IllegalArgumentException.class,() -> servico.adicionarPreRequisito(a.id(),a.id()));
        servico.adicionarPreRequisito(a.id(),b.id());
        assertThrows(IllegalArgumentException.class,() -> servico.adicionarPreRequisito(b.id(),a.id()));
        servico.adicionarPreRequisito(b.id(),d.id());
        assertThrows(IllegalArgumentException.class,() -> servico.adicionarPreRequisito(d.id(),a.id()));
        assertEquals(List.of(b),servico.listarPreRequisitos(a.id()));assertEquals(List.of(d),servico.listarPreRequisitos(b.id()));
        assertTrue(servico.listarPreRequisitos(d.id()).isEmpty());
    }
    @Test void p0410RelacoesConcorrentesNaoCriamCicloMesmoAposSnapshot() throws Exception {
        c.commit();c.setAutoCommit(false);servico.adicionarPreRequisito(a.id(),b.id());
        var iniciou=new CountDownLatch(1);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var futuro=executor.submit(() -> {
                try(var outra=ApoioJdbc.abrir(false)) {
                    outra.setAutoCommit(false);
                    var s=new ServicoProfessoresDisciplinas(new RepositorioProfessoresDisciplinasJdbc(outra));
                    assertTrue(s.listarPreRequisitos(b.id()).isEmpty()); // Estabelece snapshot antes do commit concorrente.
                    iniciou.countDown();
                    try { s.adicionarPreRequisito(b.id(),a.id());outra.commit(); }
                    finally { outra.rollback(); }
                    return null;
                }
            });
            try { assertTrue(iniciou.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,() -> futuro.get(200,TimeUnit.MILLISECONDS)); }
            finally { c.commit(); }
            assertInstanceOf(IllegalArgumentException.class,assertThrows(ExecutionException.class,() -> futuro.get(10,TimeUnit.SECONDS)).getCause());
        }
        c.setAutoCommit(true);assertEquals(List.of(b),servico.listarPreRequisitos(a.id()));assertTrue(servico.listarPreRequisitos(b.id()).isEmpty());
    }
    @Test void p0411TransacoesERollbackPreservamDados() throws Exception {
        c.commit();
        servico.atualizarProfessor(professor.id(),etiqueta+" Modificado");
        servico.atualizarDisciplina(a.id(),a.codigo(),etiqueta+" Alterada",90);
        servico.adicionarPreRequisito(a.id(),b.id());
        assertThrows(IllegalArgumentException.class,() -> servico.atualizarDisciplina(a.id(),a.codigo()," ",90));
        assertEquals(etiqueta+" Alterada",servico.buscarDisciplina(a.id()).nome());
        assertFalse(c.getAutoCommit());c.rollback();
        assertEquals(professor,servico.buscarProfessor(professor.id()));assertEquals(a,servico.buscarDisciplina(a.id()));assertTrue(servico.listarPreRequisitos(a.id()).isEmpty());
        c.setAutoCommit(true);
        assertThrows(IllegalArgumentException.class,() -> repositorio.alterarProfessor(professor.id(),p -> new Professor(p.id()+100,null,p.identificacao(),p.situacao())));
        assertTrue(c.getAutoCommit());assertEquals(professor,servico.buscarProfessor(professor.id()));
    }
    @Test void p0412NovasReferenciasIntegramTurmas() {
        var turma=novaTurma(false);
        assertEquals(a.id().longValue(),turma.idDisciplina());assertEquals(professor.id().longValue(),turma.idProfessor());
        assertTrue(academico.listarDisciplinas().stream().anyMatch(r -> r.id()==a.id()));
        assertTrue(academico.listarProfessores().stream().anyMatch(r -> r.id()==professor.id()));
        var livre=servico.cadastrarProfessor(etiqueta+" Livre");servico.inativarProfessor(livre.id());
        assertTrue(academico.listarProfessores().stream().noneMatch(r -> r.id()==livre.id()));
        assertThrows(IllegalArgumentException.class,() -> academico.cadastrarTurma(a.id(),livre.id(),turma.idPeriodo(),etiqueta+" Recusada",30));
    }
    @Test void p0413ConsoleConfirmaCadastrosERelacoes() throws Exception {
        String roteiro="2\n"+etiqueta+" Console\nS\n1\n3\n"+professor.id()+"\n4\n"+professor.id()+"\n"+etiqueta+" Corrigido\nS\n5\n"+professor.id()+"\nS\n"
                +"7\n"+etiqueta+"C\n"+etiqueta+" Console Disciplina\n60\nS\n6\n8\n"+a.id()+"\n9\n"+a.id()+"\n"+a.codigo()+"\n"+etiqueta+" Atualizada\n90\nS\n"
                +"11\n"+a.id()+"\n"+b.id()+"\nS\n10\n"+a.id()+"\n12\n"+a.id()+"\n"+b.id()+"\nS\n0\n";
        String saida=console(roteiro);
        assertTrue(saida.contains("Confirmar operação?"));assertTrue(saida.contains("Pré-requisito registrado."));assertTrue(saida.contains("Pré-requisito removido."));
        assertEquals("INATIVO",servico.buscarProfessor(professor.id()).situacao());assertEquals(etiqueta+" Corrigido",servico.buscarProfessor(professor.id()).identificacao());
        assertEquals(etiqueta+" Atualizada",servico.buscarDisciplina(a.id()).nome());assertEquals(90,servico.buscarDisciplina(a.id()).cargaHoraria());
        assertEquals(2,ids("SELECT id_professor FROM professor WHERE identificacao LIKE ?",etiqueta+"%").size());
        assertEquals(4,ids("SELECT id_disciplina FROM disciplina WHERE codigo LIKE ?",etiqueta+"%").size());
        assertTrue(servico.listarPreRequisitos(a.id()).isEmpty());
    }
    @Test void p0414ConsoleCancelaERecusaSemGravar() throws Exception {
        servico.adicionarPreRequisito(a.id(),b.id());
        String roteiro="2\n"+etiqueta+" Cancelado\nN\n4\n"+professor.id()+"\n"+etiqueta+" Cancelado\nN\n5\n"+professor.id()+"\nN\n"
                +"7\n"+etiqueta+"C\n"+etiqueta+" Cancelada\n60\nN\n9\n"+a.id()+"\n"+a.codigo()+"\n"+etiqueta+" Cancelada\n90\nN\n"
                +"11\n"+b.id()+"\n"+d.id()+"\nN\n12\n"+a.id()+"\n"+b.id()+"\nN\n"
                +"2\n   \nS\n7\nX\nX\n0\n11\n"+b.id()+"\n"+a.id()+"\nS\n1\n0\n";
        String saida=console(roteiro);
        assertTrue(saida.contains("Operação cancelada."));assertTrue(saida.contains("Informe a identificação"));assertTrue(saida.contains("Carga horária fora"));assertTrue(saida.contains("ciclo"));
        assertEquals(professor,servico.buscarProfessor(professor.id()));assertEquals(a,servico.buscarDisciplina(a.id()));
        assertEquals(List.of(b),servico.listarPreRequisitos(a.id()));assertTrue(servico.listarPreRequisitos(b.id()).isEmpty());
        assertEquals(1,ids("SELECT id_professor FROM professor WHERE identificacao LIKE ?",etiqueta+"%").size());
        assertEquals(3,ids("SELECT id_disciplina FROM disciplina WHERE codigo LIKE ?",etiqueta+"%").size());
    }
    private String console(String roteiro) throws Exception {
        var anterior=System.out;var bytes=new ByteArrayOutputStream();
        try(var saida=new PrintStream(bytes,true,StandardCharsets.UTF_8);var entrada=new Scanner(roteiro)) {
            System.setOut(saida);new ConsoleProfessoresDisciplinas(entrada,servico).executar();
        } finally { System.setOut(anterior); }
        String texto=bytes.toString(StandardCharsets.UTF_8);
        var pasta=java.nio.file.Path.of("target","evidencias-console-v04");java.nio.file.Files.createDirectories(pasta);
        java.nio.file.Files.writeString(pasta.resolve(etiqueta+".txt"),texto,StandardCharsets.UTF_8);
        return texto;
    }
    private CadastroPeriodoLetivo novoPeriodo(boolean passado) {
        int ano=LocalDate.now().getYear()+(passado?-2:1);
        return academico.cadastrarPeriodo(etiqueta+" Periodo",LocalDate.of(ano,2,1),LocalDate.of(ano,6,30));
    }
    private Turma novaTurma(boolean passado) { var p=novoPeriodo(passado);return academico.cadastrarTurma(a.id(),professor.id(),p.id(),etiqueta+" Turma",30); }
    private List<Long> ids(String query,Object... valores) throws Exception {
        try(var s=c.prepareStatement(query)) { for(int i=0;i<valores.length;i++)s.setObject(i+1,valores[i]);
            try(var r=s.executeQuery()) {var ids=new ArrayList<Long>();while(r.next())ids.add(r.getLong(1));return ids;}
        }
    }
    private void sql(String query,Object... valores) throws Exception {try(var s=c.prepareStatement(query)){for(int i=0;i<valores.length;i++)s.setObject(i+1,valores[i]);s.executeUpdate();}}
    private long inserir(String query,Object... valores) throws Exception {try(var s=c.prepareStatement(query,Statement.RETURN_GENERATED_KEYS)){for(int i=0;i<valores.length;i++)s.setObject(i+1,valores[i]);s.executeUpdate();try(var r=s.getGeneratedKeys()){assertTrue(r.next());return r.getLong(1);}}}
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.Planejamento.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;
import br.edu.cefetmg.scoa.apresentacao.ConsolePlanejamento;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanejamentoJdbcIT {
    private Connection c;
    private RepositorioPlanejamentoJdbc repo;
    private long aluno,curso,matricula,estrutura,a,b,p,t,prof,sala;
    private String tag;
    private long sql(String comando,Object... valores) throws SQLException {
        try(var s=c.prepareStatement(comando,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            s.executeUpdate(); try(var r=s.getGeneratedKeys()) { return r.next()?r.getLong(1):0; }
        }
    }
    private long contar(String tabela) throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+tabela)) { r.next(); return r.getLong(1); }
    }
    private LocalDate hoje() throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery("SELECT DATE(UTC_TIMESTAMP() - INTERVAL 3 HOUR)")) { r.next(); return r.getObject(1,LocalDate.class); }
    }
    private long turma(long disciplina,long periodo,String hora) throws SQLException {
        long id=sql("INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,2,'ATIVO')",disciplina,prof,periodo,tag+UUID.randomUUID().toString().substring(0,4));
        sql("INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim) VALUES (?,?,1,?,?)",id,sala,hora,LocalTime.parse(hora).plusHours(1)); return id;
    }
    @BeforeEach void preparar() throws Exception {
        c=ApoioJdbc.abrir(false); c.setAutoCommit(false); repo=new RepositorioPlanejamentoJdbc(c); tag="P09-"+UUID.randomUUID().toString().substring(0,12);
        aluno=sql("INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        curso=sql("INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
        prof=sql("INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')",tag);
        sala=sql("INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES (?,'fixture',10,'ATIVO')",tag);
        a=sql("INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+"A",tag+"A");
        b=sql("INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",tag+"B",tag+"B");
        p=sql("INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",tag,hoje().plusDays(100),hoje().plusDays(160));
        matricula=sql("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",aluno,curso,p);
        estrutura=sql("INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,tag);
        sql("INSERT INTO estrutura_disciplina(id_estrutura,id_disciplina) VALUES (?,?),(?,?)",estrutura,a,estrutura,b);
        new RepositorioInscricaoJdbc(c).vincularEstrutura(matricula,estrutura);
        sql("INSERT INTO disciplina_prerequisito(id_disciplina,id_disciplina_prerequisito) VALUES (?,?)",b,a);
        t=turma(b,p,"08:00");
    }
    @AfterEach void limpar() throws Exception { if(c!=null) { try { c.rollback(); } finally { c.close(); } } }
    private Resultado sim() { return repo.simular(matricula,List.of(p),List.of()); }
    private Item item(Resultado r,long id) { return r.itens().stream().filter(i->i.disciplina().id()==id).findFirst().orElseThrow(); }
    private void reconhecer() throws Exception {
        var eq=new RepositorioEquivalenciaJdbc(c);
        var pedido=eq.solicitar(new Pedido(matricula,a,"Externa","Origem",60,"Ementa","Referências "+tag,hoje().minusYears(1),"Destino"));
        eq.decidir(pedido.id(),new Decisao(true,"Análise manual de carga e ementa","Responsável declarado"));
    }
    @Test void requisitoPendenteNaoDistribuiMesmoComOferta() { assertNull(item(sim(),b).sugestao()); }
    @Test void reconhecimentoLiberaOfertaFuturaSemAbrirJanelaNemGravar() throws Exception {
        reconhecer(); var tabelas=List.of("plano_estudo","item_plano_estudo","inscricao_disciplina","resultado_inscricao","pedido_equivalencia");
        var antes=new ArrayList<Long>(); for(var nome:tabelas) antes.add(contar(nome));
        assertEquals(t,item(sim(),b).sugestao().turma()); assertFalse(c.getAutoCommit());
        for(int i=0;i<tabelas.size();i++) assertEquals(antes.get(i),contar(tabelas.get(i)));
        assertFalse(new RepositorioInscricaoJdbc(c).avaliar(matricula,t).permite());
    }
    @Test void sugestaoDaOrigemNaoLiberaSeguinte() throws Exception {
        turma(a,p,"09:00"); var r=sim(); assertNotNull(item(r,a).sugestao()); assertNull(item(r,b).sugestao());
    }
    @Test void andamentoNaoLiberaSeguinte() throws Exception {
        long ta=turma(a,p,"09:00"); sql("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",matricula,ta);
        assertEquals("EM_ANDAMENTO",item(sim(),a).disciplina().situacao()); assertNull(item(sim(),b).sugestao());
    }
    @Test void conflitoComOutraMatriculaDoAlunoImpedeSugestao() throws Exception {
        reconhecer(); long outra=sql("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",aluno,curso,p);
        long ta=turma(a,p,"08:00"); sql("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",outra,ta);
        assertNull(item(sim(),b).sugestao()); assertTrue(item(sim(),b).tentativas().getFirst().impedimentos().stream().anyMatch(s->s.contains("Conflito")));
    }
    @Test void cancelamentoNaoOcupaVagaEFinalSemResultadoExigeConciliacao() throws Exception {
        reconhecer(); sql("UPDATE turma SET capacidade=1 WHERE id_turma=?",t);
        sql("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'CANCELADO')",matricula,t);
        assertNotNull(item(sim(),b).sugestao());
        sql("UPDATE inscricao_disciplina SET situacao='REPROVADO' WHERE id_matricula=?",matricula);
        sql("DELETE FROM horario_aula WHERE id_turma=?",t);
        // Situação final sem resultado deve ser diagnosticada antes de sugerir.
        assertThrows(IllegalArgumentException.class,this::sim);
    }
    @Test void lotacaoDeOutroAlunoEHorarioAusenteSaoExplicados() throws Exception {
        reconhecer(); long outroAluno=sql("INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')",tag+"X");
        long outra=sql("INSERT INTO matricula_academica(id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,CURRENT_DATE,?,'ATIVO')",outroAluno,curso,p);
        sql("UPDATE turma SET capacidade=1 WHERE id_turma=?",t);
        sql("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",outra,t);
        var bloqueada=item(sim(),b); assertNull(bloqueada.sugestao()); assertTrue(bloqueada.tentativas().getFirst().impedimentos().contains("Turma sem vagas observadas."));
        sql("UPDATE inscricao_disciplina SET situacao='CANCELADO' WHERE id_matricula=?",outra);
        sql("DELETE FROM horario_aula WHERE id_turma=?",t);
        assertTrue(item(sim(),b).tentativas().getFirst().impedimentos().contains("Turma sem horários definidos."));
    }
    @Test void reconhecimentoComDataFuturaNaoLiberaPlanejamento() throws Exception {
        reconhecer(); sql("UPDATE pedido_equivalencia SET decidido_em=CURRENT_TIMESTAMP(6)+INTERVAL 1 DAY WHERE id_matricula=?",matricula);
        assertThrows(IllegalArgumentException.class,this::sim);
    }
    @Test void ofertaForaDoHorizonteNaoEUsada() throws Exception {
        reconhecer(); long outro=sql("INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",tag+"X",hoje().plusDays(200),hoje().plusDays(260));
        assertTrue(item(repo.simular(matricula,List.of(outro),List.of()),b).tentativas().isEmpty());
    }
    @Test void novaConsultaRefleteInativacaoDaTurma() throws Exception {
        reconhecer(); assertNotNull(item(sim(),b).sugestao()); sql("UPDATE turma SET situacao='INATIVO' WHERE id_turma=?",t); assertNull(item(sim(),b).sugestao());
    }
    @Test void contextoInativoELegadoNaoSaoPlanejados() throws Exception {
        sql("UPDATE matricula_academica SET situacao='TRANCADO' WHERE id_matricula=?",matricula); assertThrows(IllegalArgumentException.class,this::sim);
        sql("UPDATE matricula_academica SET situacao='ATIVO' WHERE id_matricula=?",matricula);
        sql("INSERT INTO plano_estudo(id_matricula,id_estrutura,data_atualizacao,situacao) VALUES (?,?,CURRENT_DATE,'ATIVO')",matricula,estrutura);
        assertThrows(IllegalArgumentException.class,this::sim);
    }
    @Test void erroPreservaTransacaoExternaEPreparacao() throws Exception {
        assertThrows(IllegalArgumentException.class,()->repo.simular(matricula,List.of(p),List.of(-1L)));
        assertFalse(c.getAutoCommit()); assertNotNull(sim());
    }
    @Test void selecaoDePeriodosEValidada() {
        assertThrows(IllegalArgumentException.class,()->repo.simular(matricula,List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->repo.simular(matricula,List.of(p,p),List.of()));
        assertThrows(IllegalArgumentException.class,()->repo.simular(matricula,List.of(4294967295L),List.of()));
    }
    @Test void consoleSimulaEEncerraSemGravar() throws Exception {
        reconhecer(); long antes=contar("inscricao_disciplina");
        var original=System.out; var bytes=new java.io.ByteArrayOutputStream();
        try(var saida=new java.io.PrintStream(bytes,true,java.nio.charset.StandardCharsets.UTF_8)) {
            System.setOut(saida); new ConsolePlanejamento(new Scanner("2\n"+matricula+"\n"+p+"\n\n0\n"),repo).executar();
        } finally { System.setOut(original); }
        assertTrue(bytes.toString(java.nio.charset.StandardCharsets.UTF_8).contains("SUGERIDA")); assertEquals(antes,contar("inscricao_disciplina"));
    }
}

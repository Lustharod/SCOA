package br.edu.cefetmg.scoa.persistencia;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.g;
import static org.junit.jupiter.api.Assertions.*;

class ConsultasAlunoJdbcIT {
    private static final String SENHA = UUID.randomUUID().toString();
    private Connection conexao;
    private String login;
    private long usuario;
    private long matricula;
    private long outraMatricula;
    private long periodo;
    private long disciplina;
    private long professor;

    @BeforeEach
    void preparar() throws SQLException {
        conexao = ApoioJdbc.abrir(false);
        conexao.setAutoCommit(false);
        login = "IT-ALUNO-" + UUID.randomUUID();
        usuario = g(conexao, "INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",
                login, SegurancaJdbc.codificar(SENHA.toCharArray()));
        long aluno = g(conexao, "INSERT INTO aluno(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",
                usuario, login);
        long outroAluno = g(conexao, "INSERT INTO aluno(identificacao,situacao) VALUES (?,'ATIVO')", login + "-outro");
        long curso = g(conexao, "INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')", login);
        periodo = g(conexao, """
                INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao)
                VALUES ('Semestre sintético','2026-08-01','2026-12-31','ATIVO')
                """);
        matricula = matricular(aluno, curso);
        outraMatricula = matricular(outroAluno, curso);
        disciplina = g(conexao, "INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,'Álgebra',60,'ATIVO')",
                UUID.randomUUID().toString().substring(0, 16));
        professor = g(conexao, "INSERT INTO professor(identificacao,situacao) VALUES (?,'ATIVO')", login);
    }

    @AfterEach
    void descartar() throws SQLException {
        if (conexao != null) {
            try (var aberta = conexao) {
                aberta.rollback();
            }
        }
    }

    @Test
    void listaSomenteHorariosDasInscricoesAtivasDoProprioAluno() throws SQLException {
        long comHorario = turma("Com horário");
        inscrever(matricula, comHorario, "ATIVO");
        inscrever(matricula, turma("Sem horário"), "ATIVO");
        inscrever(matricula, turma("Cancelada"), "CANCELADO");
        inscrever(matricula, turma("Concluída"), "APROVADO");
        inscrever(outraMatricula, turma("Turma de outro aluno"), "ATIVO");
        long sala = g(conexao, """
                INSERT INTO sala(identificacao,localizacao,capacidade,situacao) VALUES ('Laboratório','Bloco A',30,'ATIVO')
                """);
        g(conexao, """
                INSERT INTO horario_aula(id_turma,id_sala,dia_semana,hora_inicio,hora_fim)
                VALUES (?,?,3,'08:00','10:00'),(?,?,1,'14:00','15:00')
                """, comHorario, sala, comHorario, sala);

        var horarios = consultas(entrar()).horarios();
        assertEquals(3, horarios.size());
        assertTrue(horarios.stream().allMatch(h -> h.turma().matricula() == matricula && h.turma().periodo() == periodo));
        assertEquals("Sem horário", horarios.get(0).turma().turma());
        assertNull(horarios.get(0).faixa());
        assertEquals(1, horarios.get(1).faixa().diaSemana());
        assertEquals(LocalTime.of(14, 0), horarios.get(1).faixa().inicio());
        assertEquals("Laboratório", horarios.get(2).sala());
        assertFalse(conexao.getAutoCommit());
    }

    @Test
    void exibeNotasEFrequenciaFinalRegistradasDoAlunoSemInventarDadosAusentes() throws SQLException {
        long inscricao = inscrever(matricula, turma("Resultado próprio"), "APROVADO");
        inscrever(matricula, turma("Sem fechamento"), "ATIVO");
        long inscricaoAlheia = inscrever(outraMatricula, turma("Resultado alheio"), "APROVADO");
        for (long id : new long[]{inscricao, inscricaoAlheia}) {
            g(conexao, """
                    INSERT INTO resultado_inscricao
                        (id_inscricao,p1,p2,total_minutos,presentes_minutos,media_inicial,situacao,motivo)
                    VALUES (?,9,7,120,110,8,'APROVADO','Apuração sintética')
                    """, id);
        }
        var resultados = consultas(entrar()).resultadosFinais();
        assertEquals(1, resultados.size());
        var resultado = resultados.getFirst();
        assertEquals("Resultado próprio", resultado.turma().turma());
        assertEquals("9.0", resultado.p1().toPlainString());
        assertNull(resultado.pf());
        assertNull(resultado.mediaFinal());
        assertEquals(110, resultado.minutosPresentes());
        assertEquals(120, resultado.minutosTotais());
    }

    @Test
    void revalidaSessaoEIdentidadeEmCadaConsulta() throws SQLException {
        var sessao = entrar();
        var consultas = consultas(sessao);
        assertTrue(consultas.horarios().isEmpty());
        assertTrue(consultas.resultadosFinais().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> consultas(null).horarios());
        try (var outraConexao = ApoioJdbc.abrir(false)) {
            assertThrows(IllegalArgumentException.class,
                    new RepositorioConsultasAlunoJdbc(outraConexao, sessao)::horarios);
        }
        g(conexao, "UPDATE aluno SET id_usuario=NULL WHERE id_usuario=?", usuario);
        assertThrows(IllegalArgumentException.class, consultas::resultadosFinais);
        g(conexao, "UPDATE usuario SET situacao='INATIVO' WHERE id_usuario=?", usuario);
        assertThrows(IllegalArgumentException.class, consultas::horarios);
        sessao.encerrar();
        g(conexao, "UPDATE usuario SET situacao='ATIVO' WHERE id_usuario=?", usuario);
        assertThrows(IllegalArgumentException.class, consultas::resultadosFinais);
    }

    private long matricular(long aluno, long curso) throws SQLException {
        return g(conexao, """
                INSERT INTO matricula_academica(id_aluno,id_curso,id_periodo_ingresso,data_ingresso,situacao)
                VALUES (?,?,?,CURRENT_DATE,'ATIVO')
                """, aluno, curso, periodo);
    }

    private long turma(String codigo) throws SQLException {
        return g(conexao, """
                INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao)
                VALUES (?,?,?,?,20,'ATIVO')
                """, disciplina, professor, periodo, codigo);
    }

    private long inscrever(long idMatricula, long turma, String situacao) throws SQLException {
        return g(conexao, """
                INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao)
                VALUES (?,?,CURRENT_DATE,?)
                """, idMatricula, turma, situacao);
    }

    private SegurancaJdbc.Sessao entrar() {
        return new SegurancaJdbc(conexao).entrar(login, SENHA.toCharArray());
    }

    private RepositorioConsultasAlunoJdbc consultas(SegurancaJdbc.Sessao sessao) {
        return new RepositorioConsultasAlunoJdbc(conexao, sessao);
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.CadastroPeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.PeriodoLetivo;
import br.edu.cefetmg.scoa.dominio.ReferenciaAcademica;
import br.edu.cefetmg.scoa.dominio.RepositorioCadastroAcademico;
import br.edu.cefetmg.scoa.dominio.Turma;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class RepositorioCadastroAcademicoJdbc implements RepositorioCadastroAcademico {
    private final Connection conexao;
    public RepositorioCadastroAcademicoJdbc(Connection conexao) { this.conexao=Objects.requireNonNull(conexao); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet resultado) throws SQLException; }

    private <T> List<T> consultar(String sql,Leitor<T> leitor,Object... valores) {
        try (var comando=conexao.prepareStatement(sql)) {
            preencher(comando,valores);
            try (var registros=comando.executeQuery()) {
                List<T> lista=new ArrayList<>();
                while (registros.next()) lista.add(leitor.ler(registros));
                return List.copyOf(lista);
            }
        } catch (SQLException erro) { throw new PersistenciaException("Não foi possível consultar o cadastro acadêmico.",erro); }
    }
    private static void preencher(PreparedStatement comando,Object... valores) throws SQLException {
        for (int i=0;i<valores.length;i++) comando.setObject(i+1,valores[i]);
    }
    private long inserir(String sql,Object... valores) throws SQLException {
        try (var comando=conexao.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            preencher(comando,valores); comando.executeUpdate();
            try (var chaves=comando.getGeneratedKeys()) {
                if (!chaves.next()) throw new SQLException("O banco não retornou o identificador.");
                return chaves.getLong(1);
            }
        }
    }
    private void executar(String sql,Object... valores) throws SQLException {
        try (var comando=conexao.prepareStatement(sql)) { preencher(comando,valores); comando.executeUpdate(); }
    }

    @Override public CadastroPeriodoLetivo inserirPeriodo(CadastroPeriodoLetivo p) {
        Objects.requireNonNull(p);
        if (p.id()!=null) throw new IllegalArgumentException("O período novo não deve ter ID.");
        return TransacaoJdbc.executar(conexao,() -> {
            long id=inserir("INSERT INTO periodo_letivo (descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,?)",
                    p.descricao(),Date.valueOf(p.datas().inicio()),Date.valueOf(p.datas().fim()),p.situacao());
            return new CadastroPeriodoLetivo(id,p.descricao(),p.datas(),p.situacao());
        });
    }
    @Override public Optional<CadastroPeriodoLetivo> buscarPeriodo(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM periodo_letivo WHERE id_periodo=?",RepositorioCadastroAcademicoJdbc::lerPeriodo,id).stream().findFirst();
    }
    @Override public List<CadastroPeriodoLetivo> listarPeriodos() {
        return consultar("SELECT * FROM periodo_letivo ORDER BY data_inicio,id_periodo",RepositorioCadastroAcademicoJdbc::lerPeriodo);
    }
    @Override public CadastroPeriodoLetivo alterarPeriodo(long id,UnaryOperator<CadastroPeriodoLetivo> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao,() -> {
            var atual=consultar("SELECT * FROM periodo_letivo WHERE id_periodo=? FOR UPDATE",RepositorioCadastroAcademicoJdbc::lerPeriodo,id)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Período letivo não encontrado."));
            var novo=Objects.requireNonNull(alteracao.apply(atual));
            if (!Long.valueOf(id).equals(novo.id())) throw new IllegalArgumentException("Preserve o ID do período.");
            if (!atual.datas().equals(novo.datas()) || !atual.situacao().equals(novo.situacao())) {
                if (!consultar("SELECT id_turma FROM turma WHERE id_periodo=? LIMIT 1 FOR UPDATE",r -> r.getLong(1),id).isEmpty())
                    throw new IllegalArgumentException("Este período possui turmas. Nesta etapa, apenas sua descrição pode ser alterada.");
            }
            executar("UPDATE periodo_letivo SET descricao=?,data_inicio=?,data_fim=?,situacao=? WHERE id_periodo=?",
                    novo.descricao(),Date.valueOf(novo.datas().inicio()),Date.valueOf(novo.datas().fim()),novo.situacao(),id);
            return novo;
        });
    }
    @Override public Turma inserirTurma(Turma turma) {
        Objects.requireNonNull(turma);
        if (turma.id()!=null) throw new IllegalArgumentException("A turma nova não deve ter ID.");
        return TransacaoJdbc.executar(conexao,() -> {
            validarVinculos(turma);
            long id=inserir("INSERT INTO turma (id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,?,?)",
                    turma.idDisciplina(),turma.idProfessor(),turma.idPeriodo(),turma.codigo(),turma.capacidade(),turma.situacao());
            return new Turma(id,turma.idDisciplina(),turma.idProfessor(),turma.idPeriodo(),turma.codigo(),turma.capacidade(),turma.situacao());
        });
    }
    @Override public Optional<Turma> buscarTurma(long id) {
        HorarioAula.validarId(id);
        return consultar("SELECT * FROM turma WHERE id_turma=?",RepositorioCadastroAcademicoJdbc::lerTurma,id).stream().findFirst();
    }
    @Override public List<Turma> listarTurmas() {
        return consultar("SELECT * FROM turma ORDER BY id_turma",RepositorioCadastroAcademicoJdbc::lerTurma);
    }
    @Override public Turma alterarTurma(long id,UnaryOperator<Turma> alteracao) {
        HorarioAula.validarId(id); Objects.requireNonNull(alteracao);
        return TransacaoJdbc.executar(conexao,() -> {
            var atual=consultar("SELECT * FROM turma WHERE id_turma=? FOR UPDATE",RepositorioCadastroAcademicoJdbc::lerTurma,id)
                    .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Turma não encontrada."));
            var nova=Objects.requireNonNull(alteracao.apply(atual));
            if (!Long.valueOf(id).equals(nova.id())) throw new IllegalArgumentException("Preserve o ID da turma.");
            validarVinculos(nova);
            var horarios=consultar("SELECT s.capacidade,p.data_fim>=CURRENT_DATE AS compromisso FROM horario_aula h "
                    + "JOIN sala s ON s.id_sala=h.id_sala JOIN periodo_letivo p ON p.id_periodo=? "
                    + "WHERE h.id_turma=? FOR UPDATE",r -> new int[]{r.getInt(1),r.getBoolean(2)?1:0},atual.idPeriodo(),id);
            boolean mudouVinculos=atual.idDisciplina()!=nova.idDisciplina() || atual.idProfessor()!=nova.idProfessor() || atual.idPeriodo()!=nova.idPeriodo();
            if (!horarios.isEmpty() && mudouVinculos)
                throw new IllegalArgumentException("A turma possui horários. Remova as alocações antes de mudar seus vínculos.");
            for (int[] horario:horarios) {
                if (horario[1]==1 && nova.capacidade()>horario[0]) throw new IllegalArgumentException("A capacidade da turma supera a de uma sala alocada.");
                if (horario[1]==1 && !"ATIVO".equals(nova.situacao())) throw new IllegalArgumentException("Remova as alocações atuais ou futuras antes de inativar a turma.");
            }
            int inscritos=consultar("SELECT id_inscricao FROM inscricao_disciplina WHERE id_turma=? FOR UPDATE",r -> r.getLong(1),id).size();
            if (nova.capacidade()<inscritos) throw new IllegalArgumentException("A capacidade não pode ser inferior à quantidade de inscrições registradas.");
            if (inscritos>0 && (mudouVinculos || !atual.situacao().equals(nova.situacao())))
                throw new IllegalArgumentException("A turma possui inscrições. Mudanças de vínculos e situação exigem o fluxo acadêmico correspondente.");
            executar("UPDATE turma SET id_disciplina=?,id_professor=?,id_periodo=?,codigo=?,capacidade=?,situacao=? WHERE id_turma=?",
                    nova.idDisciplina(),nova.idProfessor(),nova.idPeriodo(),nova.codigo(),nova.capacidade(),nova.situacao(),id);
            return nova;
        });
    }
    private void validarVinculos(Turma turma) {
        validarReferencia("SELECT situacao FROM periodo_letivo WHERE id_periodo=? FOR SHARE",turma.idPeriodo(),"Período",turma);
        validarReferencia("SELECT situacao FROM disciplina WHERE id_disciplina=? FOR SHARE",turma.idDisciplina(),"Disciplina",turma);
        validarReferencia("SELECT situacao FROM professor WHERE id_professor=? FOR SHARE",turma.idProfessor(),"Professor",turma);
    }
    private void validarReferencia(String sql,long id,String nome,Turma turma) {
        var situacoes=consultar(sql,r -> r.getString(1),id);
        if (situacoes.isEmpty()) throw new IllegalArgumentException(nome + " não encontrado.");
        if ("ATIVO".equals(turma.situacao()) && !"ATIVO".equals(situacoes.getFirst()))
            throw new IllegalArgumentException(nome + " está inativo e não pode ser vinculado a uma turma ativa.");
    }
    @Override public List<ReferenciaAcademica> listarDisciplinas() {
        return consultar("SELECT id_disciplina,CONCAT(codigo,' — ',nome) FROM disciplina WHERE situacao='ATIVO' ORDER BY id_disciplina",
                r -> new ReferenciaAcademica(r.getLong(1),r.getString(2)));
    }
    @Override public List<ReferenciaAcademica> listarProfessores() {
        return consultar("SELECT id_professor,identificacao FROM professor WHERE situacao='ATIVO' ORDER BY id_professor",
                r -> new ReferenciaAcademica(r.getLong(1),r.getString(2)));
    }
    private static CadastroPeriodoLetivo lerPeriodo(ResultSet r) throws SQLException {
        return new CadastroPeriodoLetivo(r.getLong("id_periodo"),r.getString("descricao"),
                new PeriodoLetivo(r.getDate("data_inicio").toLocalDate(),r.getDate("data_fim").toLocalDate()),r.getString("situacao"));
    }
    private static Turma lerTurma(ResultSet r) throws SQLException {
        return new Turma(r.getLong("id_turma"),r.getLong("id_disciplina"),r.getLong("id_professor"),r.getLong("id_periodo"),
                r.getString("codigo"),r.getInt("capacidade"),r.getString("situacao"));
    }
}

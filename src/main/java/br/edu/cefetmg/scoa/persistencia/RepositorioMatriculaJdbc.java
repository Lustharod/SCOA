package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.SituacaoBiblioteca.Estado;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Uma conexão por sessão. Escritores da biblioteca devem bloquear usuario_biblioteca antes das pendências. */
public final class RepositorioMatriculaJdbc implements RepositorioMatricula {
    private final Connection c;
    public RepositorioMatriculaJdbc(Connection c) { this.c = Objects.requireNonNull(c); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    private static void preencher(PreparedStatement s, Object... valores) throws SQLException {
        for (int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
    }
    private <T> List<T> consulta(String sql, Leitor<T> leitor, Object... valores) {
        try (var s=c.prepareStatement(sql)) {
            preencher(s,valores);
            try(var r=s.executeQuery()) {
                List<T> lista=new ArrayList<>(); while(r.next()) lista.add(leitor.ler(r)); return List.copyOf(lista);
            }
        } catch(SQLException e) { throw new PersistenciaException("Não foi possível consultar matrícula/biblioteca. Operação não concluída.",e); }
    }
    private long inserir(String sql, Object... valores) throws SQLException {
        try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            preencher(s,valores); s.executeUpdate();
            try(var r=s.getGeneratedKeys()) { if(!r.next()) throw new SQLException("ID não retornado."); return r.getLong(1); }
        }
    }
    private void executar(String sql,Object... valores) throws SQLException {
        try(var s=c.prepareStatement(sql)) { preencher(s,valores); s.executeUpdate(); }
    }
    private static Long longoNulo(ResultSet r,String coluna) throws SQLException {
        long v=r.getLong(coluna); return r.wasNull()?null:v;
    }
    private static Matricula ler(ResultSet r) throws SQLException {
        return new Matricula(r.getLong("id_matricula"),r.getLong("id_aluno"),r.getLong("id_curso"),
                r.getDate("data_ingresso").toLocalDate(),longoNulo(r,"id_periodo_ingresso"),r.getString("situacao"));
    }
    @Override public Optional<Matricula> buscar(long id) {
        HorarioAula.validarId(id);
        return consulta("SELECT * FROM matricula_academica WHERE id_matricula=?",RepositorioMatriculaJdbc::ler,id).stream().findFirst();
    }
    @Override public List<Matricula> listar(long aluno) {
        HorarioAula.validarId(aluno);
        if(consulta("SELECT id_aluno FROM aluno WHERE id_aluno=?",r->r.getLong(1),aluno).isEmpty())
            throw new IllegalArgumentException("Aluno não encontrado.");
        return consulta("SELECT * FROM matricula_academica WHERE id_aluno=? ORDER BY id_matricula",RepositorioMatriculaJdbc::ler,aluno);
    }
    private void bloquearAluno(long id,boolean ativo) {
        var linhas=consulta("SELECT situacao FROM aluno WHERE id_aluno=? FOR UPDATE",r->r.getString(1),id);
        if(linhas.isEmpty()) throw new IllegalArgumentException("Aluno não encontrado.");
        if(ativo && !"ATIVO".equals(linhas.getFirst())) throw new IllegalArgumentException("Aluno deve estar ativo.");
    }
    private void cursoAtivo(long id) {
        var linhas=consulta("SELECT situacao FROM curso WHERE id_curso=? FOR UPDATE",r->r.getString(1),id);
        if(linhas.isEmpty() || !"ATIVO".equals(linhas.getFirst())) throw new IllegalArgumentException("Curso ausente ou inativo.");
    }
    private void periodoAtivo(long id) {
        var linhas=consulta("SELECT situacao FROM periodo_letivo WHERE id_periodo=? FOR UPDATE",r->r.getString(1),id);
        if(linhas.isEmpty() || !"ATIVO".equals(linhas.getFirst())) throw new IllegalArgumentException("Período ausente ou inativo.");
    }
    private void semOutroVinculo(long aluno,long curso,Long exceto) {
        var lista=consulta("SELECT * FROM matricula_academica WHERE id_aluno=? AND id_curso=? FOR UPDATE",RepositorioMatriculaJdbc::ler,aluno,curso);
        for(var m:lista) {
            if(Objects.equals(m.id(),exceto)) continue;
            if(!Set.of("CANCELADO","CONCLUIDO").contains(m.situacao()))
                throw new IllegalArgumentException("Vínculo já existente: matrícula " + m.id() + ". Consulte-o antes de criar ou retornar.");
        }
    }
    private Matricula bloquear(long id,boolean alunoAtivo) {
        var previa=buscar(id).orElseThrow(()->new IllegalArgumentException("Matrícula não encontrada."));
        bloquearAluno(previa.idAluno(),alunoAtivo);
        var atual=consulta("SELECT * FROM matricula_academica WHERE id_matricula=? FOR UPDATE",RepositorioMatriculaJdbc::ler,id)
                .stream().findFirst().orElseThrow(()->new IllegalArgumentException("Matrícula não encontrada."));
        if(atual.idAluno()!=previa.idAluno() || atual.idCurso()!=previa.idCurso())
            throw new IllegalArgumentException("Vínculo alterado externamente. Consulte novamente.");
        return atual;
    }
    @Override public Matricula criar(long aluno,long curso,LocalDate data,long periodo,String motivo) {
        var dados=new Matricula(null,aluno,curso,data,periodo,"ATIVO");
        String razao=Matricula.validarMotivo(motivo);
        return TransacaoJdbc.executar(c,()->{
            bloquearAluno(aluno,true); cursoAtivo(curso); periodoAtivo(periodo); semOutroVinculo(aluno,curso,null);
            biblioteca(aluno).exigirRegular();
            long id=inserir("INSERT INTO matricula_academica (id_aluno,id_curso,data_ingresso,id_periodo_ingresso,situacao) VALUES (?,?,?,?,'ATIVO')",aluno,curso,data,periodo);
            var nova=new Matricula(id,aluno,curso,dados.dataIngresso(),periodo,"ATIVO");
            registrar(null,nova,"CRIACAO",razao); return nova;
        });
    }
    @Override public Matricula corrigir(long id,LocalDate data,long periodo,String motivo) {
        HorarioAula.validarId(id); HorarioAula.validarId(periodo); Matricula.validarData(data);
        String razao=Matricula.validarMotivo(motivo);
        return TransacaoJdbc.executar(c,()->{
            var atual=bloquear(id,false); periodoAtivo(periodo);
            var nova=new Matricula(id,atual.idAluno(),atual.idCurso(),data,periodo,atual.situacao());
            executar("UPDATE matricula_academica SET data_ingresso=?,id_periodo_ingresso=? WHERE id_matricula=?",data,periodo,id);
            registrar(atual,nova,"CORRECAO",razao); return nova;
        });
    }
    @Override public Matricula transicionar(long id,String destino,String motivo) {
        HorarioAula.validarId(id); String razao=Matricula.validarMotivo(motivo);
        return TransacaoJdbc.executar(c,()->{
            var atual=bloquear(id,"ATIVO".equals(destino));
            Matricula.validarTransicao(atual.situacao(),destino);
            if("ATIVO".equals(destino)) {
                cursoAtivo(atual.idCurso());
                if(atual.idPeriodoIngresso()==null) throw new IllegalArgumentException("Corrija o período de ingresso antes do retorno.");
                // Período de ingresso é histórico; seu encerramento não impede retorno.
                semOutroVinculo(atual.idAluno(),atual.idCurso(),id); biblioteca(atual.idAluno()).exigirRegular();
            }
            var nova=new Matricula(id,atual.idAluno(),atual.idCurso(),atual.dataIngresso(),atual.idPeriodoIngresso(),destino);
            executar("UPDATE matricula_academica SET situacao=? WHERE id_matricula=?",destino,id);
            registrar(atual,nova,"SITUACAO",razao); return nova;
        });
    }
    private void registrar(Matricula antes,Matricula depois,String operacao,String motivo) throws SQLException {
        inserir("INSERT INTO historico_matricula (id_matricula,operacao,motivo,situacao_anterior,situacao_nova,data_anterior,data_nova,periodo_anterior,periodo_novo) VALUES (?,?,?,?,?,?,?,?,?)",
                depois.id(),operacao,motivo,antes==null?null:antes.situacao(),depois.situacao(),antes==null?null:antes.dataIngresso(),depois.dataIngresso(),antes==null?null:antes.idPeriodoIngresso(),depois.idPeriodoIngresso());
    }
    @Override public List<EventoMatricula> historico(long id) {
        HorarioAula.validarId(id);
        return consulta("SELECT * FROM historico_matricula WHERE id_matricula=? ORDER BY id_evento",r->new EventoMatricula(
                r.getLong("id_evento"),id,r.getTimestamp("ocorrido_em").toLocalDateTime(),r.getString("operacao"),r.getString("motivo"),
                r.getString("situacao_anterior"),r.getString("situacao_nova"),r.getDate("data_anterior")==null?null:r.getDate("data_anterior").toLocalDate(),
                r.getDate("data_nova").toLocalDate(),longoNulo(r,"periodo_anterior"),longoNulo(r,"periodo_novo")),id);
    }
    @Override public SituacaoBiblioteca consultarBiblioteca(long aluno) {
        HorarioAula.validarId(aluno);
        return TransacaoJdbc.executar(c,()->{ bloquearAluno(aluno,false); return biblioteca(aluno); });
    }
    private SituacaoBiblioteca biblioteca(long aluno) {
        // FOR UPDATE usa leitura corrente mesmo sob REPEATABLE READ.
        var usuarios=consulta("SELECT b.id_usuario_biblioteca,b.situacao FROM aluno a JOIN usuario_biblioteca b ON b.id_usuario=a.id_usuario WHERE a.id_aluno=? FOR UPDATE",
                r->new Object[]{r.getLong(1),r.getString(2)},aluno);
        if(usuarios.isEmpty() || !"ATIVO".equals(usuarios.getFirst()[1]))
            return new SituacaoBiblioteca(Estado.INDISPONIVEL,"identificação bibliotecária ausente ou inativa.");
        var pendencias=consulta("SELECT p.id_pendencia,p.situacao,p.impeditiva FROM emprestimo e JOIN pendencia_biblioteca p ON p.id_emprestimo=e.id_emprestimo WHERE e.id_usuario_biblioteca=? FOR UPDATE",
                r->new Object[]{r.getLong(1),r.getString(2),r.getBoolean(3)},usuarios.getFirst()[0]);
        for(var p:pendencias) {
            if("ATIVO".equals(p[1]) && Boolean.TRUE.equals(p[2]))
                return new SituacaoBiblioteca(Estado.IMPEDITIVA,"pendência impeditiva " + p[0] + " no Módulo Biblioteca.");
        }
        for(var p:pendencias) if(!Set.of("ATIVO","REGULARIZADO","LIBERADO").contains(p[1]))
            return new SituacaoBiblioteca(Estado.INDISPONIVEL,"situação de pendência não reconhecida.");
        return new SituacaoBiblioteca(Estado.REGULAR,"consulta válida sem pendência impeditiva ativa.");
    }
}

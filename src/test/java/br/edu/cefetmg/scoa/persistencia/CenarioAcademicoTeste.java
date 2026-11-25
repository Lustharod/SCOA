package br.edu.cefetmg.scoa.persistencia;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Dados exclusivamente sintéticos, mantidos na transação e desfeitos ao encerrar cada caso. */
final class CenarioAcademicoTeste implements AutoCloseable {
    private static final String SENHA=UUID.randomUUID().toString();
    private static final String HASH=SegurancaJdbc.codificar(SENHA.toCharArray());
    final Connection c;
    final String tag="IT-V014-"+UUID.randomUUID();
    final long admin,docente,alunoConta,outroConta,outroDocente;
    final long aluno,outroAluno,curso,periodo,professor,disciplina,estrutura,matricula,outraMatricula,turma,inscricao;
    final LocalDate hoje;
    CenarioAcademicoTeste() throws SQLException {
        c=ApoioJdbc.abrir(false); c.setAutoCommit(false);
        try {
            admin=usuario("admin"); docente=usuario("docente"); alunoConta=usuario("aluno");
            outroConta=usuario("outro"); outroDocente=usuario("outro-docente");
            conceder(admin,SegurancaJdbc.ADMIN);
            aluno=g(c,"INSERT INTO aluno(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",alunoConta,tag);
            outroAluno=g(c,"INSERT INTO aluno(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",outroConta,tag+"-outro");
            curso=g(c,"INSERT INTO curso(nome,situacao) VALUES (?,'ATIVO')",tag);
            hoje=dataAcademica(agoraUtc(c));
            periodo=g(c,"INSERT INTO periodo_letivo(descricao,data_inicio,data_fim,situacao) VALUES (?,?,?,'ATIVO')",
                    tag,hoje.minusDays(100),hoje.minusDays(1));
            professor=g(c,"INSERT INTO professor(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",docente,tag);
            g(c,"INSERT INTO professor(id_usuario,identificacao,situacao) VALUES (?,?,'ATIVO')",outroDocente,tag+"-outro-docente");
            disciplina=disciplina("D");
            estrutura=g(c,"INSERT INTO estrutura_curricular(id_curso,identificacao,situacao) VALUES (?,?,'ATIVO')",curso,tag);
            g(c,"INSERT INTO estrutura_disciplina VALUES (?,?)",estrutura,disciplina);
            matricula=matricular(aluno); outraMatricula=matricular(outroAluno);
            turma=g(c,"INSERT INTO turma(id_disciplina,id_professor,id_periodo,codigo,capacidade,situacao) VALUES (?,?,?,?,20,'ATIVO')",
                    disciplina,professor,periodo,"Turma sintética");
            inscricao=inscrever(matricula,"ATIVO");
        } catch(SQLException | RuntimeException e) { c.rollback(); c.close(); throw e; }
    }
    private long usuario(String nome) throws SQLException {
        return g(c,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",tag+"-"+nome,HASH);
    }
    long disciplina(String codigo) throws SQLException {
        return g(c,"INSERT INTO disciplina(codigo,nome,carga_horaria,situacao) VALUES (?,?,60,'ATIVO')",codigo,tag+codigo);
    }
    long matricular(long idAluno) throws SQLException {
        long id=g(c,"INSERT INTO matricula_academica(id_aluno,id_curso,id_periodo_ingresso,data_ingresso,situacao) VALUES (?,?,?,?,'ATIVO')",
                idAluno,curso,periodo,hoje.minusDays(100));
        g(c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",id,estrutura);
        return id;
    }
    long inscrever(long m,String situacao) throws SQLException {
        return g(c,"INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,?)",m,turma,hoje.minusDays(100),situacao);
    }
    SegurancaJdbc.Sessao sessao(long usuario) { return sessao(c,usuario); }
    SegurancaJdbc.Sessao sessao(Connection conexao,long usuario) {
        try {
            String login=q(conexao,"SELECT identificacao FROM usuario WHERE id_usuario=?",r->r.getString(1),usuario).getFirst();
            return new SegurancaJdbc(conexao).entrar(login,SENHA.toCharArray());
        } catch(SQLException e) { throw new AssertionError(e); }
    }
    void conceder(long usuario,String codigo) throws SQLException {
        var existentes=q(c,"SELECT id_permissao FROM permissao WHERE codigo=?",r->r.getLong(1),codigo);
        long permissao=existentes.isEmpty()?g(c,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",codigo,codigo):existentes.getFirst();
        long perfil=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,?,'ATIVO')",UUID.randomUUID().toString(),"Fixture de permissão");
        g(c,"INSERT INTO usuario_perfil VALUES (?,?)",usuario,perfil);
        g(c,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,permissao);
    }
    long reconhecer(long destino,String revisao) throws SQLException {
        g(c,"INSERT INTO estrutura_disciplina VALUES (?,?)",estrutura,destino);
        long id=g(c,"""
                INSERT INTO pedido_equivalencia(id_matricula,id_estrutura,id_disciplina_destino,instituicao_origem,
                disciplina_origem,carga_horaria_origem,ementa_origem,documentos,conclusao_origem,ementa_destino,
                carga_horaria_destino,situacao,parecer,responsavel,decidido_em)
                VALUES (?,?,?,'Instituição sintética','Origem',60,'Ementa','Referência',?,'Destino',60,'DEFERIDO','Parecer','Responsável',CURRENT_TIMESTAMP(6))
                """,matricula,estrutura,destino,hoje.minusDays(200));
        if(revisao!=null) g(c,"INSERT INTO revisao_reconhecimento(id_pedido,solicitante,justificativa,situacao) VALUES (?,?,'Fixture de revisão',?)",id,admin,revisao);
        return id;
    }
    Connection falharAuditoria() {
        return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,metodo,args)->{
            if(metodo.getName().equals("prepareStatement") && ((String)args[0]).stripLeading().startsWith("INSERT INTO auditoria"))
                throw new SQLException("Falha sintética de auditoria");
            try { return metodo.invoke(c,args); }
            catch(java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
        });
    }
    @Override public void close() throws SQLException { try(c) { c.rollback(); } }
}

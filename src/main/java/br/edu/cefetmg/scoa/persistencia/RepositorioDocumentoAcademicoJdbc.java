package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DocumentoAcademico.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** O aluno é derivado da sessão; atendimento depende de concessão técnica específica. */
public final class RepositorioDocumentoAcademicoJdbc implements RepositorioDocumentoAcademico {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioDocumentoAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    private long aluno() throws SQLException {
        long usuario=SegurancaJdbc.exigirSessao(c,sessao);
        var alunos=q(c,"SELECT id_aluno FROM aluno WHERE id_usuario=? FOR SHARE",r->r.getLong(1),usuario);
        if(alunos.size()!=1) throw new IllegalArgumentException("A conta deve estar vinculada a um cadastro de aluno.");
        return alunos.getFirst();
    }
    private void exigirMinhaMatricula(long matricula,long aluno) throws SQLException {
        if(q(c,"SELECT id_matricula FROM matricula_academica WHERE id_matricula=? AND id_aluno=? FOR SHARE",
                r->r.getLong(1),matricula,aluno).isEmpty())
            throw new IllegalArgumentException("Matrícula não encontrada ou não autorizada.");
    }
    @Override public List<AtendimentoAluno.Vinculo> minhasMatriculas() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long aluno=aluno();
            return q(c,"""
                    SELECT m.id_matricula,c.nome,m.situacao,m.data_ingresso FROM matricula_academica m
                    JOIN curso c ON c.id_curso=m.id_curso WHERE m.id_aluno=? ORDER BY m.id_matricula FOR SHARE
                    """,r->new AtendimentoAluno.Vinculo(r.getLong(1),r.getString(2),r.getString(3),r.getObject(4,LocalDate.class)),aluno);
        });
    }
    @Override public Registro solicitar(Novo pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); exigirMinhaMatricula(pedido.matricula(),aluno());
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            long id=g(c,"""
                    INSERT INTO solicitacao_documento(id_matricula,tipo_documento,data_solicitacao,situacao,
                        id_usuario_solicitante,solicitado_em_utc)
                    VALUES (?,?,?,'SOLICITADO',?,?)
                    """,pedido.matricula(),pedido.tipo().name(),dataAcademica(agora),autor,agora);
            g(c,"""
                    INSERT INTO andamento_documento(id_solicitacao_documento,versao,situacao_nova,id_usuario_autor,ocorrido_em_utc)
                    VALUES (?,0,'SOLICITADO',?,?)
                    """,id,autor,agora);
            g(c,"""
                    INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes)
                    VALUES (?,'ACADEMICO','SOLICITACAO_DOCUMENTO',?,'SOLICITAR',?,'Solicitação de documento acadêmico registrada.')
                    """,autor,id,agora);
            return buscar(id);
        });
    }
    @Override public List<Registro> minhasSolicitacoes() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long aluno=aluno();
            return q(c,SELECT+" WHERE m.id_aluno=? ORDER BY s.id_solicitacao_documento FOR SHARE",RepositorioDocumentoAcademicoJdbc::ler,aluno);
        });
    }
    @Override public List<Andamento> meuAndamento(long protocolo) {
        HorarioAula.validarId(protocolo);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long aluno=aluno();
            if(q(c,"""
                    SELECT s.id_solicitacao_documento FROM solicitacao_documento s
                    JOIN matricula_academica m ON m.id_matricula=s.id_matricula
                    WHERE s.id_solicitacao_documento=? AND m.id_aluno=? FOR SHARE
                    """,r->r.getLong(1),protocolo,aluno).isEmpty())
                throw new IllegalArgumentException("Solicitação não encontrada ou não autorizada.");
            return andamentos(protocolo);
        });
    }
    @Override public List<Registro> filaSecretaria() {
        return acesso.consultarDocumentos(()->TransacaoJdbc.executar(c,()->q(c,
                SELECT+" ORDER BY s.data_solicitacao,s.id_solicitacao_documento FOR SHARE",RepositorioDocumentoAcademicoJdbc::ler)));
    }
    @Override public List<Andamento> andamentoSecretaria(long protocolo) {
        HorarioAula.validarId(protocolo);
        return acesso.consultarDocumentos(()->TransacaoJdbc.executar(c,()->{ buscar(protocolo); return andamentos(protocolo); }));
    }
    @Override public Registro atualizar(Atualizacao pedido) {
        Objects.requireNonNull(pedido);
        return acesso.atenderDocumento(pedido.protocolo(),()->TransacaoJdbc.executar(c,()->{
            var anterior=buscar(pedido.protocolo());
            if(anterior.versao()!=pedido.versao()) throw new IllegalArgumentException("Solicitação alterada. Consulte o andamento e tente novamente.");
            if(anterior.solicitante()==null || anterior.solicitadoEm()==null)
                throw new IllegalArgumentException("Solicitação legada preservada; requer tratamento específico antes de atualização.");
            DocumentoAcademico.exigirTransicao(anterior.situacao(),pedido.situacao());
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            int proxima=anterior.versao()+1;
            g(c,"""
                    UPDATE solicitacao_documento SET situacao=?,versao=?,id_usuario_atendente=?,atualizado_em_utc=?,
                        parecer=?,referencia_entrega=? WHERE id_solicitacao_documento=?
                    """,pedido.situacao().name(),proxima,autor,agora,pedido.parecer(),pedido.referenciaEntrega(),pedido.protocolo());
            g(c,"""
                    INSERT INTO andamento_documento(id_solicitacao_documento,versao,situacao_anterior,situacao_nova,
                        id_usuario_autor,ocorrido_em_utc,parecer,referencia_entrega) VALUES (?,?,?,?,?,?,?,?)
                    """,pedido.protocolo(),proxima,anterior.situacao(),pedido.situacao().name(),autor,agora,pedido.parecer(),pedido.referenciaEntrega());
            return buscar(pedido.protocolo());
        }));
    }
    private static final String SELECT="""
            SELECT s.*,c.nome AS curso FROM solicitacao_documento s
            JOIN matricula_academica m ON m.id_matricula=s.id_matricula JOIN curso c ON c.id_curso=m.id_curso
            """;
    private Registro buscar(long protocolo) throws SQLException {
        var lista=q(c,SELECT+" WHERE s.id_solicitacao_documento=? FOR UPDATE",RepositorioDocumentoAcademicoJdbc::ler,protocolo);
        if(lista.isEmpty()) throw new IllegalArgumentException("Solicitação não encontrada.");
        return lista.getFirst();
    }
    private List<Andamento> andamentos(long protocolo) throws SQLException {
        return q(c,"SELECT * FROM andamento_documento WHERE id_solicitacao_documento=? ORDER BY versao FOR SHARE",
                r->new Andamento(r.getLong("id_andamento"),r.getLong("id_solicitacao_documento"),r.getInt("versao"),
                        r.getString("situacao_anterior"),r.getString("situacao_nova"),r.getLong("id_usuario_autor"),
                        r.getObject("ocorrido_em_utc",LocalDateTime.class),r.getString("parecer"),r.getString("referencia_entrega")),protocolo);
    }
    private static Registro ler(ResultSet r) throws SQLException {
        return new Registro(r.getLong("id_solicitacao_documento"),r.getLong("id_matricula"),r.getString("curso"),
                r.getString("tipo_documento"),r.getString("situacao"),r.getObject("data_solicitacao",LocalDate.class),
                r.getObject("id_usuario_solicitante",Long.class),r.getObject("solicitado_em_utc",LocalDateTime.class),
                r.getInt("versao"),r.getObject("id_usuario_atendente",Long.class),r.getObject("atualizado_em_utc",LocalDateTime.class),
                r.getString("parecer"),r.getString("referencia_entrega"));
    }
}

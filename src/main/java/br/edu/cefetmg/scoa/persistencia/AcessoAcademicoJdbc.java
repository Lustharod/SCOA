package br.edu.cefetmg.scoa.persistencia;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import br.edu.cefetmg.scoa.dominio.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Fronteira autenticada dos serviços legados; autoriza e audita na mesma transação. */
public final class AcessoAcademicoJdbc {
    public static final String CONSULTAR="SCOA_ACADEMICO_CONSULTAR";
    public static final String MANTER="SCOA_ACADEMICO_MANTER";
    public static final String DOCUMENTOS_CONSULTAR="SCOA_DOCUMENTOS_CONSULTAR";
    public static final String DOCUMENTOS_ATENDER="SCOA_DOCUMENTOS_ATENDER";
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;

    public AcessoAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao;
    }

    private long usuario() throws SQLException { return SegurancaJdbc.exigirSessao(c,sessao); }
    private boolean tem(long usuario,String... codigos) throws SQLException {
        var permissoes=q(c,"""
                SELECT DISTINCT p.codigo FROM usuario_perfil up
                JOIN perfil f ON f.id_perfil=up.id_perfil
                JOIN perfil_permissao fp ON fp.id_perfil=f.id_perfil
                JOIN permissao p ON p.id_permissao=fp.id_permissao
                WHERE up.id_usuario=? AND f.situacao='ATIVO' FOR SHARE
                """,r->r.getString(1),usuario);
        return java.util.Arrays.stream(codigos).anyMatch(permissoes::contains);
    }
    private void exigirTecnico(long usuario,String permissao) throws SQLException {
        if(!tem(usuario,SegurancaJdbc.ADMIN,permissao))
            throw new IllegalArgumentException("Sem permissão: "+permissao);
    }
    public <T> T consultar(Supplier<T> operacao) {
        return consultarComPermissao(CONSULTAR,operacao);
    }
    public <T> T consultarDocumentos(Supplier<T> operacao) {
        return consultarComPermissao(DOCUMENTOS_CONSULTAR,operacao);
    }
    private <T> T consultarComPermissao(String permissao,Supplier<T> operacao) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); exigirTecnico(usuario(),permissao); return operacao.get();
        });
    }
    public <T> T manter(String entidade,String acao,long referencia,Supplier<T> operacao) {
        return manterComPermissao(MANTER,entidade,acao,referencia,operacao);
    }
    public <T> T atenderDocumento(long protocolo,Supplier<T> operacao) {
        return manterComPermissao(DOCUMENTOS_ATENDER,"SOLICITACAO_DOCUMENTO","ATUALIZAR",protocolo,operacao);
    }
    private <T> T manterComPermissao(String permissao,String entidade,String acao,long referencia,Supplier<T> operacao) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=usuario(); exigirTecnico(autor,permissao);
            T resultado=operacao.get();
            long registro=referencia(resultado);
            auditar(entidade,acao,registro>0?registro:referencia,autor);
            return resultado;
        });
    }
    public <T> T consultarMatricula(long matricula,Supplier<T> operacao) {
        HorarioAula.validarId(matricula);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=usuario();
            if(!tem(autor,SegurancaJdbc.ADMIN,CONSULTAR)) {
                var alunos=q(c,"SELECT id_aluno FROM matricula_academica WHERE id_matricula=? FOR SHARE",r->r.getLong(1),matricula);
                if(alunos.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada ou não autorizada.");
                SegurancaJdbc.exigirAluno(c,autor,alunos.getFirst());
            }
            return operacao.get();
        });
    }
    public <T> T consultarProcesso(Supplier<T> operacao) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=usuario();
            if(!tem(autor,SegurancaJdbc.ADMIN,CONSULTAR,SegurancaJdbc.SOLICITAR,SegurancaJdbc.DECIDIR))
                throw new IllegalArgumentException("Sem permissão para consultar processos acadêmicos.");
            return operacao.get();
        });
    }
    public <T> T fecharResultado(long inscricao,Supplier<T> operacao) {
        HorarioAula.validarId(inscricao);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=usuario();
            var professores=q(c,"""
                    SELECT pr.id_professor FROM inscricao_disciplina i
                    JOIN turma t ON t.id_turma=i.id_turma
                    JOIN professor pr ON pr.id_professor=t.id_professor
                    WHERE i.id_inscricao=? AND pr.id_usuario=? AND pr.situacao='ATIVO' FOR SHARE
                    """,r->r.getLong(1),inscricao,autor);
            if(professores.isEmpty())
                throw new IllegalArgumentException("O primeiro resultado cabe ao professor responsável pela turma.");
            T resultado=operacao.get();
            auditar("RESULTADO_INSCRICAO","PRIMEIRO_FECHAMENTO",inscricao,autor);
            return resultado;
        });
    }
    private void auditar(String entidade,String acao,long referencia,long autor) throws SQLException {
        g(c,"""
                INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes)
                VALUES (?,'ACADEMICO',?,?,?,UTC_TIMESTAMP(),?)
                """,autor,entidade,referencia,acao,"Operação concluída pela fronteira acadêmica autenticada.");
    }
    private static long referencia(Object valor) {
        if(valor==null) return 0;
        return switch(valor) {
            case Sala s -> s.getId();
            case Aluno a -> a.id();
            case Professor p -> p.id();
            case Curso c -> c.id();
            case Disciplina d -> d.id();
            case EstruturaCurricular e -> e.id();
            case CadastroPeriodoLetivo p -> p.id();
            case Turma t -> t.id();
            case HorarioAula h -> h.id();
            case Matricula m -> m.id();
            case Inscricao i -> i.id();
            default -> 0;
        };
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Mantém perfis técnicos e concessões, sem alterar credenciais ou vínculos acadêmicos. */
public final class RepositorioAdministracaoAcessoAcademicoJdbc implements RepositorioAdministracaoAcessoAcademico {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    public RepositorioAdministracaoAcessoAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) { this.c=Objects.requireNonNull(c); this.sessao=sessao; }
    @FunctionalInterface private interface Operacao<T> { T executar(long autor) throws SQLException; }
    private <T> T ler(Operacao<T> op) {
        return TransacaoJdbc.executar(c,()->{ controle(c); return op.executar(SegurancaJdbc.exigir(c,sessao,SegurancaJdbc.ADMIN)); });
    }
    private <T> T manter(String acao,Operacao<T> op) {
        return ler(autor->{
            T resultado=op.executar(autor); preservarAdministrador();
            long id=resultado instanceof Perfil p?p.id():((Conta)resultado).id();
            g(c,"INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes) VALUES (?,'ACADEMICO','ADMINISTRACAO_ACESSO',?,?,UTC_TIMESTAMP(6),?)",
                    autor,id,acao,"Operação técnica com histórico específico; credenciais e vínculos acadêmicos preservados.");
            return resultado;
        });
    }
    @Override public List<Perfil> perfis() {
        return ler(autor->{
            var ids=q(c,"SELECT id_perfil FROM perfil ORDER BY id_perfil FOR SHARE",r->r.getLong(1)); var lista=new ArrayList<Perfil>();
            for(long id:ids) lista.add(perfil(id)); return List.copyOf(lista);
        });
    }
    @Override public Perfil consultarPerfil(long id) { HorarioAula.validarId(id); return ler(autor->perfil(id)); }
    private Perfil perfil(long id) throws SQLException {
        var linhas=q(c,"SELECT nome,descricao,situacao,versao_acesso FROM perfil WHERE id_perfil=? FOR UPDATE",
                r->new Object[]{r.getString(1),r.getString(2),r.getString(3),r.getInt(4)},id);
        if(linhas.isEmpty()) throw new IllegalArgumentException("Perfil não encontrado."); var p=linhas.getFirst();
        var codigos=q(c,"SELECT DISTINCT p.codigo FROM perfil_permissao fp JOIN permissao p ON p.id_permissao=fp.id_permissao WHERE fp.id_perfil=? ORDER BY p.codigo FOR SHARE",r->r.getString(1),id);
        var contas=q(c,"SELECT id_usuario FROM usuario_perfil WHERE id_perfil=? ORDER BY id_usuario FOR SHARE",r->r.getLong(1),id);
        return new Perfil(id,(String)p[0],(String)p[1],(String)p[2],(int)p[3],codigos,contas);
    }
    @Override public Perfil criarPerfil(NovoPerfil dados,String motivo) {
        Objects.requireNonNull(dados); String razao=Matricula.validarMotivo(motivo);
        return manter("CRIAR_PERFIL",autor->{
            long id=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,?,'ATIVO')",dados.nome(),dados.descricao());
            permissoes(id,dados.permissoes()); var p=perfil(id);
            historia(null,id,"CRIAR_PERFIL",0,null,p.toString(),razao,autor); return p;
        });
    }
    @Override public Perfil alterarPerfil(AlterarPerfil dados) {
        Objects.requireNonNull(dados);
        return manter("ALTERAR_PERFIL",autor->{
            var antes=perfil(dados.perfil()); escopo(antes);
            if(antes.versao()!=dados.versao()) throw new IllegalArgumentException("Perfil alterado desde a consulta; confira permissões e contas afetadas.");
            g(c,"UPDATE perfil SET nome=?,descricao=?,situacao=?,versao_acesso=versao_acesso+1 WHERE id_perfil=?",
                    dados.nome(),dados.descricao(),dados.situacao(),dados.perfil());
            g(c,"DELETE FROM perfil_permissao WHERE id_perfil=?",dados.perfil());
            permissoes(dados.perfil(),dados.permissoes()); var depois=perfil(dados.perfil());
            historia(null,dados.perfil(),"ALTERAR_PERFIL",depois.versao(),antes.toString(),depois.toString(),dados.motivo(),autor); return depois;
        });
    }
    private void permissoes(long perfil,List<String> codigos) throws SQLException {
        for(String codigo:codigos) {
            var ids=q(c,"SELECT id_permissao FROM permissao WHERE codigo=? ORDER BY id_permissao FOR UPDATE",r->r.getLong(1),codigo);
            if(ids.size()>1) throw new IllegalArgumentException("Código de permissão duplicado no legado; confira os cadastros antes de conceder.");
            String descricao=Arrays.stream(Codigo.values()).filter(p->p.codigo().equals(codigo)).findFirst().orElseThrow().descricao();
            long id=ids.isEmpty()?g(c,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",codigo,descricao):ids.getFirst();
            g(c,"INSERT INTO perfil_permissao(id_perfil,id_permissao) VALUES (?,?)",perfil,id);
        }
    }
    @Override public Conta consultarConta(long id) { HorarioAula.validarId(id); return ler(autor->conta(id)); }
    @Override public List<Conta> contas() {
        return ler(autor->{ var ids=q(c,"SELECT id_usuario FROM usuario ORDER BY id_usuario FOR SHARE",r->r.getLong(1)); var lista=new ArrayList<Conta>(); for(long id:ids) lista.add(conta(id)); return List.copyOf(lista); });
    }
    private Conta conta(long id) throws SQLException {
        var linhas=q(c,"SELECT identificacao,situacao,versao_acesso FROM usuario WHERE id_usuario=? FOR UPDATE",r->new Object[]{r.getString(1),r.getString(2),r.getInt(3)},id);
        if(linhas.isEmpty()) throw new IllegalArgumentException("Conta não encontrada."); var r=linhas.getFirst();
        var perfis=q(c,"SELECT id_perfil FROM usuario_perfil WHERE id_usuario=? ORDER BY id_perfil FOR SHARE",p->p.getLong(1),id);
        var codigos=q(c,"SELECT DISTINCT p.codigo FROM usuario_perfil up JOIN perfil f ON f.id_perfil=up.id_perfil JOIN perfil_permissao fp ON fp.id_perfil=f.id_perfil JOIN permissao p ON p.id_permissao=fp.id_permissao WHERE up.id_usuario=? AND f.situacao='ATIVO' ORDER BY p.codigo FOR SHARE",p->p.getString(1),id);
        return new Conta(id,(String)r[0],(String)r[1],(int)r[2],perfis,"ATIVO".equals(r[1])?codigos:List.of());
    }
    @Override public Conta vincular(VincularPerfil dados) {
        Objects.requireNonNull(dados);
        return manter(dados.conceder()?"CONCEDER_PERFIL":"REVOGAR_PERFIL",autor->{
            var antes=conta(dados.usuario()); var perfil=perfil(dados.perfil()); escopo(perfil);
            if(antes.versao()!=dados.versaoConta() || perfil.versao()!=dados.versaoPerfil()) throw new IllegalArgumentException("Conta ou perfil alterado desde a consulta; confira os dados atuais.");
            if(dados.conceder() && (!"ATIVO".equals(antes.situacao()) || !"ATIVO".equals(perfil.situacao())))
                throw new IllegalArgumentException("Concessão exige conta e perfil ativos.");
            boolean existe=antes.perfis().contains(dados.perfil());
            if(existe==dados.conceder()) return antes;
            if(dados.conceder()) g(c,"INSERT INTO usuario_perfil(id_usuario,id_perfil) VALUES (?,?)",dados.usuario(),dados.perfil());
            else g(c,"DELETE FROM usuario_perfil WHERE id_usuario=? AND id_perfil=?",dados.usuario(),dados.perfil());
            g(c,"UPDATE usuario SET versao_acesso=versao_acesso+1 WHERE id_usuario=?",dados.usuario()); var depois=conta(dados.usuario());
            historia(dados.usuario(),dados.perfil(),dados.conceder()?"CONCEDER_PERFIL":"REVOGAR_PERFIL",depois.versao(),antes.toString(),depois.toString(),dados.motivo(),autor); return depois;
        });
    }
    @Override public Conta alterarConta(AlterarConta dados) {
        Objects.requireNonNull(dados);
        return manter("SITUACAO_CONTA",autor->{
            var antes=conta(dados.usuario());
            if(antes.versao()!=dados.versao()) throw new IllegalArgumentException("Conta alterada desde a consulta.");
            if(antes.situacao().equals(dados.situacao())) return antes;
            g(c,"UPDATE usuario SET situacao=?,versao_acesso=versao_acesso+1,geracao_sessao=geracao_sessao+1 WHERE id_usuario=?",dados.situacao(),dados.usuario()); var depois=conta(dados.usuario());
            historia(dados.usuario(),null,"SITUACAO_CONTA",depois.versao(),antes.toString(),depois.toString(),dados.motivo(),autor); return depois;
        });
    }
    private static void escopo(Perfil p) {
        if(!p.escopoAcademico()) throw new IllegalArgumentException("Perfil contém permissões de outro módulo/legado fora deste catálogo. Preservado; use um perfil acadêmico específico.");
    }
    private void preservarAdministrador() throws SQLException {
        if(q(c,"""
                SELECT u.id_usuario FROM usuario u JOIN usuario_perfil up ON up.id_usuario=u.id_usuario
                JOIN perfil f ON f.id_perfil=up.id_perfil JOIN perfil_permissao fp ON fp.id_perfil=f.id_perfil
                JOIN permissao p ON p.id_permissao=fp.id_permissao WHERE u.situacao='ATIVO' AND f.situacao='ATIVO'
                AND p.codigo=? AND u.credencial_hash LIKE 'scoa1$600000$%' LIMIT 1 FOR SHARE
                """,r->r.getLong(1),SegurancaJdbc.ADMIN).isEmpty())
            throw new IllegalArgumentException("Preserve ao menos uma conta administrativa ativa com acesso de autenticação reconhecido.");
    }
    private void historia(Long conta,Long perfil,String operacao,int versao,String antes,String depois,String motivo,long autor) throws SQLException {
        g(c,"""
                INSERT INTO historico_acesso_academico(id_usuario_alvo,id_perfil,operacao,versao_referencia,anterior,novo,motivo,id_usuario_autor,registrado_em_utc)
                VALUES (?,?,?,?,?,?,?,?,UTC_TIMESTAMP(6))
                """,conta,perfil,operacao,versao,antes,depois,motivo,autor);
    }
    @Override public List<Evento> historicoConta(long id) { HorarioAula.validarId(id); return ler(autor->{ conta(id); return historico("id_usuario_alvo",id); }); }
    @Override public List<Evento> historicoPerfil(long id) { HorarioAula.validarId(id); return ler(autor->{ perfil(id); return historico("id_perfil",id); }); }
    private List<Evento> historico(String campo,long id) throws SQLException {
        return q(c,"SELECT * FROM historico_acesso_academico WHERE "+campo+"=? ORDER BY id_evento FOR SHARE",r->new Evento(
                r.getLong("id_evento"),r.getObject("id_usuario_alvo",Long.class),r.getObject("id_perfil",Long.class),r.getString("operacao"),
                r.getInt("versao_referencia"),r.getString("anterior"),r.getString("novo"),r.getString("motivo"),r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class)),id);
    }
}

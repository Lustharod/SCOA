package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Provisionamento só de fixture, em conexão de teste validada. Não integra a aplicação. */
final class AcessoTeste {
    private static long usuario,perfil;
    private static final List<Long> permissoesCriadas=new ArrayList<>();
    private static final String login="IT-V010-"+UUID.randomUUID();
    private static final String senha=UUID.randomUUID().toString();
    static synchronized SegurancaJdbc.Sessao sessao(Connection c) throws SQLException {
        if(usuario==0) try(var setup=ApoioJdbc.abrir(false)) {
            usuario=g(setup,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",login,SegurancaJdbc.codificar(senha.toCharArray()));
            perfil=g(setup,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,'Fixture automatizada','ATIVO')",login);
            g(setup,"INSERT INTO usuario_perfil VALUES (?,?)",usuario,perfil);
            for(String codigo:List.of(SegurancaJdbc.ADMIN,SegurancaJdbc.SOLICITAR,SegurancaJdbc.DECIDIR)) {
                var ids=q(setup,"SELECT id_permissao FROM permissao WHERE codigo=?",r->r.getLong(1),codigo);
                long id;
                if(ids.isEmpty()) { id=g(setup,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",codigo,codigo); permissoesCriadas.add(id); }
                else id=ids.getFirst();
                g(setup,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,id);
            }
        }
        return new SegurancaJdbc(c).entrar(login,senha.toCharArray());
    }
    static synchronized void limpar() throws SQLException {
        if(usuario==0) return;
        try(var c=ApoioJdbc.abrir(false)) {
            g(c,"DELETE FROM evento_processo_academico WHERE id_usuario=?",usuario);
            g(c,"DELETE FROM usuario_perfil WHERE id_usuario=?",usuario);
            g(c,"DELETE FROM perfil_permissao WHERE id_perfil=?",perfil);
            g(c,"DELETE FROM perfil WHERE id_perfil=?",perfil);
            g(c,"DELETE FROM usuario WHERE id_usuario=?",usuario);
            for(long id:permissoesCriadas) g(c,"DELETE FROM permissao WHERE id_permissao=? AND NOT EXISTS(SELECT 1 FROM perfil_permissao WHERE id_permissao=?)",id,id);
        }
        usuario=0; permissoesCriadas.clear();
    }
}

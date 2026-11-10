package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

/** Utilitários internos. Identificadores SQL nunca vêm de entradas do console. */
final class JdbcAcademico {
    private JdbcAcademico() { }
    @FunctionalInterface interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    static <T> List<T> q(Connection c,String sql,Leitor<T> leitor,Object... args) throws SQLException {
        try(var s=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) s.setObject(i+1,args[i]);
            try(var r=s.executeQuery()) { var lista=new ArrayList<T>(); while(r.next()) lista.add(leitor.ler(r)); return List.copyOf(lista); }
        }
    }
    static long g(Connection c,String sql,Object... args) throws SQLException {
        try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<args.length;i++) s.setObject(i+1,args[i]);
            s.executeUpdate(); try(var r=s.getGeneratedKeys()) { return r.next()?r.getLong(1):0; }
        }
    }
    static void controle(Connection c) throws SQLException {
        if(q(c,"SELECT id FROM controle_inscricao WHERE id=1 FOR UPDATE",r->r.getInt(1)).isEmpty())
            throw new IllegalArgumentException("Confira a migração V007.");
    }
    static String hash(String texto) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static void evento(Connection c,String tipo,long id,long usuario,String acao,String descricao) throws SQLException {
        g(c,"INSERT INTO evento_processo_academico(tipo,id_processo,id_usuario,acao,descricao) VALUES (?,?,?,?,?)",tipo,id,usuario,acao,descricao);
    }
}

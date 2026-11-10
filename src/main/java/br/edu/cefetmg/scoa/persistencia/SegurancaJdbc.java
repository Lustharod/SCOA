package br.edu.cefetmg.scoa.persistencia;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia;
import java.sql.*;
import java.time.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.util.*;

/** Identidade em memória; permissões e atividade revalidadas a cada operação protegida. */
public final class SegurancaJdbc {
    public static final String SOLICITAR="SCOA_PROCESSO_SOLICITAR", DECIDIR="SCOA_PROCESSO_DECIDIR", ADMIN="SCOA_ACESSO_ADMIN";
    private final Connection c;
    public SegurancaJdbc(Connection c) { this.c=Objects.requireNonNull(c); }
    public static final class Sessao {
        private final long usuario; private final Connection conexao;
        private final Instant fim; private boolean encerrada;
        private Sessao(long id,Connection c) { usuario=id; conexao=c; fim=Instant.now().plus(Duration.ofHours(2)); }
        public long usuario() { return usuario; }
        public void encerrar() { encerrada=true; }
    }
    public static String codificar(char[] senha) {
        if(senha==null || senha.length<12 || senha.length>256) throw new IllegalArgumentException("Senha deve ter de 12 a 256 caracteres.");
        byte[] sal=new byte[16]; new SecureRandom().nextBytes(sal);
        return "scoa1$600000$"+Base64.getEncoder().encodeToString(sal)+"$"+Base64.getEncoder().encodeToString(derivar(senha,sal,600000));
    }
    private static byte[] derivar(char[] senha,byte[] sal,int iteracoes) {
        var spec=new PBEKeySpec(senha,sal,iteracoes,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    public static boolean confere(char[] senha,String hash) {
        if(senha==null || senha.length>256 || hash==null) return false;
        try {
            var p=hash.split("\\$"); if(p.length!=4 || !"scoa1".equals(p[0]) || !"600000".equals(p[1])) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(p[3]),derivar(senha,Base64.getDecoder().decode(p[2]),600000));
        } catch(IllegalArgumentException e) { return false; }
    }
    public Sessao entrar(String login,char[] senha) {
        try { return TransacaoJdbc.executar(c,()->{
            var usuarios=q(c,"SELECT id_usuario,credencial_hash,situacao FROM usuario WHERE identificacao=? FOR SHARE",
                r->new Object[]{r.getLong(1),r.getString(2),r.getString(3)},login);
            if(usuarios.size()!=1 || !"ATIVO".equals(usuarios.getFirst()[2]) || !confere(senha,(String)usuarios.getFirst()[1]))
                throw new IllegalArgumentException("Identificação ou senha inválida, ambígua ou inativa.");
            return new Sessao((long)usuarios.getFirst()[0],c);
        }); } finally { if(senha!=null) Arrays.fill(senha,'\0'); }
    }
    static long exigir(Connection c,Sessao sessao,String permissao) throws SQLException {
        if(sessao==null || sessao.conexao!=c || sessao.encerrada || !Instant.now().isBefore(sessao.fim))
            throw new IllegalArgumentException("Entre com uma sessão válida no menu 18.");
        var u=q(c,"SELECT situacao FROM usuario WHERE id_usuario=? FOR SHARE",r->r.getString(1),sessao.usuario);
        if(u.isEmpty() || !"ATIVO".equals(u.getFirst())) throw new IllegalArgumentException("Usuário inativo.");
        var p=q(c,"SELECT p.codigo FROM usuario_perfil up JOIN perfil f ON f.id_perfil=up.id_perfil JOIN perfil_permissao fp ON fp.id_perfil=f.id_perfil JOIN permissao p ON p.id_permissao=fp.id_permissao WHERE up.id_usuario=? AND f.situacao='ATIVO' AND p.codigo=? FOR SHARE",r->r.getString(1),sessao.usuario,permissao);
        if(p.isEmpty()) throw new IllegalArgumentException("Sem permissão: "+permissao);
        return sessao.usuario;
    }
    /** Inicialização local explícita: não toma posse de contas existentes nem fornece senha padrão. */
    public long inicializar(String login,char[] senha,boolean habilitada) {
        try { return TransacaoJdbc.executar(c,()->{
            controle(c);
            if(!habilitada || !q(c,"SELECT id_usuario FROM usuario WHERE credencial_hash LIKE 'scoa1$%' FOR UPDATE",r->r.getLong(1)).isEmpty())
                throw new IllegalArgumentException("Inicialização indisponível: requer habilitação local e nenhuma conta SCOA inicializada.");
            long id=criar(login,senha,"ADMIN");
            evento(c,"ACESSO",id,id,"INICIALIZAR","Primeira conta técnica criada por inicialização local explícita"); return id;
        }); } finally { if(senha!=null) Arrays.fill(senha,'\0'); }
    }
    public long criarConta(Sessao sessao,String login,char[] senha,String papel) {
        try { return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=exigir(c,sessao,ADMIN); long id=criar(login,senha,papel);
            evento(c,"ACESSO",id,autor,"CRIAR_CONTA",papel); return id;
        }); } finally { if(senha!=null) Arrays.fill(senha,'\0'); }
    }
    private long criar(String login,char[] senha,String papel) throws SQLException {
        login=Equivalencia.texto(login,"identificação",100);
        if(!Set.of("ADMIN","SOLICITANTE","ANALISTA").contains(papel)) throw new IllegalArgumentException("Papel inválido.");
        if(!q(c,"SELECT id_usuario FROM usuario WHERE identificacao=? FOR UPDATE",r->r.getLong(1),login).isEmpty()) throw new IllegalArgumentException("Identificação já existente.");
        long u=g(c,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",login,codificar(senha));
        long perfil=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,?,'ATIVO')","SCOA "+papel+" "+u,"Permissões técnicas do fluxo v0.10");
        g(c,"INSERT INTO usuario_perfil VALUES (?,?)",u,perfil);
        var permissoes="ADMIN".equals(papel)?List.of(ADMIN,SOLICITAR,DECIDIR):List.of("ANALISTA".equals(papel)?DECIDIR:SOLICITAR);
        for(String codigo:permissoes) {
            var ids=q(c,"SELECT id_permissao FROM permissao WHERE codigo=? FOR UPDATE",r->r.getLong(1),codigo);
            long p=ids.isEmpty()?g(c,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",codigo,codigo):ids.getFirst();
            g(c,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,p);
        }
        return u;
    }
}

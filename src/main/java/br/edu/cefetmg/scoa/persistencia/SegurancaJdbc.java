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
    public static final String REGISTRAR_PENDENCIA="SCOA_BIBLIOTECA_PENDENCIA";
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
    static long exigirSessao(Connection c,Sessao sessao) throws SQLException {
        if(sessao==null || sessao.conexao!=c || sessao.encerrada || !Instant.now().isBefore(sessao.fim))
            throw new IllegalArgumentException("Entre com uma sessão válida no menu 18.");
        var u=q(c,"SELECT situacao FROM usuario WHERE id_usuario=? FOR SHARE",r->r.getString(1),sessao.usuario);
        if(u.isEmpty() || !"ATIVO".equals(u.getFirst())) throw new IllegalArgumentException("Usuário inativo.");
        return sessao.usuario;
    }
    static long exigir(Connection c,Sessao sessao,String permissao) throws SQLException {
        long usuario=exigirSessao(c,sessao);
        var p=q(c,"SELECT p.codigo FROM usuario_perfil up JOIN perfil f ON f.id_perfil=up.id_perfil JOIN perfil_permissao fp ON fp.id_perfil=f.id_perfil JOIN permissao p ON p.id_permissao=fp.id_permissao WHERE up.id_usuario=? AND f.situacao='ATIVO' AND p.codigo=? FOR SHARE",r->r.getString(1),sessao.usuario,permissao);
        if(p.isEmpty()) throw new IllegalArgumentException("Sem permissão: "+permissao);
        return usuario;
    }
    static boolean coordena(Connection c,long usuario,long curso) throws SQLException {
        return !q(c,"SELECT id_usuario FROM coordenacao_curso WHERE id_curso=? AND id_usuario=? FOR SHARE",
                r->r.getLong(1),curso,usuario).isEmpty();
    }
    static boolean ehAluno(Connection c,long usuario,long aluno) throws SQLException {
        return !q(c,"SELECT id_aluno FROM aluno WHERE id_aluno=? AND id_usuario=? FOR SHARE",
                r->r.getLong(1),aluno,usuario).isEmpty();
    }
    static void exigirAluno(Connection c,long usuario,long aluno) throws SQLException {
        if(!ehAluno(c,usuario,aluno)) throw new IllegalArgumentException("A operação é restrita ao próprio aluno.");
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
    /** O conjunto técnico inicial é independente dos vínculos acadêmicos. */
    public long criarConta(Sessao sessao,String login,char[] senha,String acessoTecnico) {
        try { return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=exigir(c,sessao,ADMIN); long id=criar(login,senha,acessoTecnico);
            evento(c,"ACESSO",id,autor,"CRIAR_CONTA","Acesso técnico inicial: "+acessoTecnico); return id;
        }); } finally { if(senha!=null) Arrays.fill(senha,'\0'); }
    }
    public record VinculoAcademico(String tipo,long referencia,String identificacao,String situacao) { }
    public record ContaAcesso(long idConta,String identificacao,String situacao,
                              List<VinculoAcademico> vinculos,List<String> permissoesTecnicas) {
        public ContaAcesso {
            vinculos=List.copyOf(vinculos);
            permissoesTecnicas=List.copyOf(permissoesTecnicas);
        }
    }
    public ContaAcesso minhaConta(Sessao sessao) {
        return TransacaoJdbc.executar(c,()->consultar(exigirSessao(c,sessao)));
    }
    public ContaAcesso consultarConta(Sessao sessao,long usuario) {
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(usuario);
        return TransacaoJdbc.executar(c,()->{
            exigir(c,sessao,ADMIN);
            return consultar(usuario);
        });
    }
    private ContaAcesso consultar(long usuario) throws SQLException {
        var contas=q(c,"SELECT identificacao,situacao FROM usuario WHERE id_usuario=? FOR SHARE",
                r->new String[]{r.getString(1),r.getString(2)},usuario);
        if(contas.isEmpty()) throw new IllegalArgumentException("Conta não encontrada.");
        var vinculos=new ArrayList<VinculoAcademico>();
        vinculos.addAll(q(c,"SELECT id_aluno,identificacao,situacao FROM aluno WHERE id_usuario=? ORDER BY id_aluno FOR SHARE",
                r->new VinculoAcademico("ALUNO",r.getLong(1),r.getString(2),r.getString(3)),usuario));
        vinculos.addAll(q(c,"SELECT id_professor,identificacao,situacao FROM professor WHERE id_usuario=? ORDER BY id_professor FOR SHARE",
                r->new VinculoAcademico("PROFESSOR",r.getLong(1),r.getString(2),r.getString(3)),usuario));
        vinculos.addAll(q(c,"SELECT c.id_curso,c.nome,c.situacao FROM coordenacao_curso cc JOIN curso c ON c.id_curso=cc.id_curso WHERE cc.id_usuario=? ORDER BY c.id_curso FOR SHARE",
                r->new VinculoAcademico("COORDENADOR",r.getLong(1),r.getString(2),r.getString(3)),usuario));
        vinculos.addAll(q(c,"SELECT id_gestor,situacao FROM gestao_academica WHERE id_usuario=? FOR SHARE",
                r->new VinculoAcademico("GESTOR",r.getLong(1),"Gestão acadêmica institucional — todos os cursos",r.getString(2)),usuario));
        var permissoes=q(c,"SELECT DISTINCT p.codigo FROM usuario_perfil up JOIN perfil f ON f.id_perfil=up.id_perfil JOIN perfil_permissao fp ON fp.id_perfil=f.id_perfil JOIN permissao p ON p.id_permissao=fp.id_permissao WHERE up.id_usuario=? AND f.situacao='ATIVO' ORDER BY p.codigo FOR SHARE",
                r->r.getString(1),usuario);
        return new ContaAcesso(usuario,contas.getFirst()[0],contas.getFirst()[1],vinculos,permissoes);
    }
    public void vincularContaAcademica(Sessao sessao,long usuario,String tipo,long referencia) {
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(usuario);
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(referencia);
        if(!Set.of("ALUNO","PROFESSOR","COORDENADOR").contains(tipo))
            throw new IllegalArgumentException("Informe ALUNO, PROFESSOR ou COORDENADOR.");
        TransacaoJdbc.executar(c,()->{
            controle(c);
            long autor=exigir(c,sessao,ADMIN);
            if(q(c,"SELECT id_usuario FROM usuario WHERE id_usuario=? AND situacao='ATIVO' FOR UPDATE",
                    r->r.getLong(1),usuario).isEmpty()) throw new IllegalArgumentException("Conta ausente ou inativa.");
            if("COORDENADOR".equals(tipo)) {
                if(q(c,"SELECT id_curso FROM curso WHERE id_curso=? AND situacao='ATIVO' FOR SHARE",
                        r->r.getLong(1),referencia).isEmpty()) throw new IllegalArgumentException("Curso ausente ou inativo.");
                if(!coordena(c,usuario,referencia)) {
                    g(c,"INSERT INTO coordenacao_curso(id_curso,id_usuario) VALUES (?,?)",referencia,usuario);
                    evento(c,"ACESSO",usuario,autor,"VINCULAR_COORDENACAO","Curso "+referencia);
                }
            } else {
                // Os nomes das tabelas e chaves são constantes internas.
                String tabela="ALUNO".equals(tipo)?"aluno":"professor";
                String chave="id_"+tabela;
                var atual=q(c,"SELECT id_usuario FROM "+tabela+" WHERE "+chave+"=? AND situacao='ATIVO' FOR UPDATE",
                        r->Optional.ofNullable(r.getObject(1,Long.class)),referencia);
                if(atual.isEmpty()) throw new IllegalArgumentException("Cadastro ausente ou inativo.");
                if(atual.getFirst().isPresent() && atual.getFirst().get()!=usuario)
                    throw new IllegalArgumentException("O cadastro já está vinculado a outra conta.");
                if(!q(c,"SELECT "+chave+" FROM "+tabela+" WHERE id_usuario=? AND "+chave+"<>? FOR UPDATE",
                        r->r.getLong(1),usuario,referencia).isEmpty())
                    throw new IllegalArgumentException("A conta já está vinculada a outro cadastro.");
                if(atual.getFirst().isEmpty()) {
                    g(c,"UPDATE "+tabela+" SET id_usuario=? WHERE "+chave+"=?",usuario,referencia);
                    evento(c,"ACESSO",usuario,autor,"VINCULAR_"+tipo,"Cadastro "+referencia);
                }
            }
            return null;
        });
    }
    public long vincularGestorAcademico(Sessao sessao,long usuario) {
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(usuario);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=exigir(c,sessao,ADMIN);
            if(q(c,"SELECT id_usuario FROM usuario WHERE id_usuario=? AND situacao='ATIVO' FOR UPDATE",r->r.getLong(1),usuario).isEmpty())
                throw new IllegalArgumentException("Conta ausente ou inativa.");
            var anteriores=q(c,"SELECT id_gestor,situacao FROM gestao_academica WHERE id_usuario=? FOR UPDATE",r->new Object[]{r.getLong(1),r.getString(2)},usuario);
            if(!anteriores.isEmpty()) {
                if(!"ATIVO".equals(anteriores.getFirst()[1])) throw new IllegalArgumentException("Vínculo de gestor inativo. Use a manutenção de situação.");
                return (long)anteriores.getFirst()[0];
            }
            long id=g(c,"INSERT INTO gestao_academica(id_usuario,situacao,id_usuario_autor,registrado_em_utc) VALUES (?,'ATIVO',?,?)",usuario,autor,agoraUtc(c));
            evento(c,"GESTAO_ACESSO",id,autor,"VINCULAR_GESTOR","Conta "+usuario+" vinculada à gestão institucional; permissões técnicas preservadas.");
            g(c,"INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes) VALUES (?,'ACADEMICO','GESTAO_ACADEMICA',?,'VINCULAR',?,'Vínculo acadêmico institucional explícito.')",autor,id,agoraUtc(c));
            return id;
        });
    }
    public void definirSituacaoGestor(Sessao sessao,long usuario,String situacao) {
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(usuario);
        if(!Set.of("ATIVO","INATIVO").contains(Objects.toString(situacao,""))) throw new IllegalArgumentException("Informe ATIVO ou INATIVO.");
        TransacaoJdbc.executar(c,()->{
            controle(c); long autor=exigir(c,sessao,ADMIN);
            var atuais=q(c,"SELECT id_gestor,situacao FROM gestao_academica WHERE id_usuario=? FOR UPDATE",r->new Object[]{r.getLong(1),r.getString(2)},usuario);
            if(atuais.isEmpty()) throw new IllegalArgumentException("Vínculo de gestor não encontrado.");
            var atual=atuais.getFirst();
            if(!situacao.equals(atual[1])) {
                g(c,"UPDATE gestao_academica SET situacao=? WHERE id_gestor=?",situacao,atual[0]);
                evento(c,"GESTAO_ACESSO",(long)atual[0],autor,"SITUACAO_GESTOR",atual[1]+" -> "+situacao);
                g(c,"INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes) VALUES (?,'ACADEMICO','GESTAO_ACADEMICA',?,'ALTERAR_SITUACAO',?,?)",autor,atual[0],agoraUtc(c),atual[1]+" -> "+situacao);
            }
            return null;
        });
    }
    private long criar(String login,char[] senha,String acessoTecnico) throws SQLException {
        login=Equivalencia.texto(login,"identificação",100);
        if(acessoTecnico==null || !Set.of("SEM_PERMISSOES","ADMIN","SOLICITANTE","ANALISTA","BIBLIOTECARIO").contains(acessoTecnico))
            throw new IllegalArgumentException("Acesso técnico inválido. Papéis acadêmicos são obtidos dos vínculos no menu 18.11.");
        if(!q(c,"SELECT id_usuario FROM usuario WHERE identificacao=? FOR UPDATE",r->r.getLong(1),login).isEmpty()) throw new IllegalArgumentException("Identificação já existente.");
        long u=g(c,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",login,codificar(senha));
        if("SEM_PERMISSOES".equals(acessoTecnico)) return u;
        long perfil=g(c,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,?,'ATIVO')","SCOA "+acessoTecnico+" "+u,"Permissões técnicas do SCOA");
        g(c,"INSERT INTO usuario_perfil VALUES (?,?)",u,perfil);
        var permissoes=switch(acessoTecnico) {
            case "ADMIN" -> List.of(ADMIN,SOLICITAR,DECIDIR);
            case "ANALISTA" -> List.of(DECIDIR);
            case "BIBLIOTECARIO" -> List.of(REGISTRAR_PENDENCIA);
            case "SOLICITANTE" -> List.of(SOLICITAR);
            default -> List.<String>of();
        };
        for(String codigo:permissoes) {
            var ids=q(c,"SELECT id_permissao FROM permissao WHERE codigo=? FOR UPDATE",r->r.getLong(1),codigo);
            long p=ids.isEmpty()?g(c,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",codigo,codigo):ids.getFirst();
            g(c,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,p);
        }
        return u;
    }
}

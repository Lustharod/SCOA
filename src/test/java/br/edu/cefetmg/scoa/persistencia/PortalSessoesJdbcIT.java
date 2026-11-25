package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.web.PortalAcademico;
import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;
import static org.junit.jupiter.api.Assertions.*;

/** Sessões HTTP com conexões MySQL realmente distintas e dados sintéticos confirmados.
 * Limpeza explícita somente dos IDs criados neste caso, sem limpar tabelas inteiras. */
class PortalSessoesJdbcIT {
    private Connection controle;
    private PortalAcademico portal;
    private HttpClient cliente;
    private final List<Connection> abertas=new CopyOnWriteArrayList<>();
    private final List<Long> perfis=new ArrayList<>(),permissoesNovas=new ArrayList<>();
    private long admin,leitor;
    private String base,senha,loginAdmin,loginLeitor;
    @BeforeEach void preparar() throws Exception {
        controle=ApoioJdbc.abrir(false);String tag="HTTP-SESS-"+UUID.randomUUID();senha="HTTP-"+UUID.randomUUID();String hash=SegurancaJdbc.codificar(senha.toCharArray());
        loginAdmin=tag+"-admin";loginLeitor=tag+"-leitor";
        admin=g(controle,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",loginAdmin,hash);
        leitor=g(controle,"INSERT INTO usuario(identificacao,credencial_hash,situacao) VALUES (?,?,'ATIVO')",loginLeitor,hash);
        conceder(admin,SegurancaJdbc.ADMIN);conceder(leitor,AcessoAcademicoJdbc.CONSULTAR);
        portal=new PortalAcademico(0,()->{var c=ApoioJdbc.abrir(false);abertas.add(c);return c;});portal.iniciar();base="http://127.0.0.1:"+portal.porta();
        cliente=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private void conceder(long usuario,String codigo) throws Exception {
        var existentes=q(controle,"SELECT id_permissao FROM permissao WHERE codigo=?",r->r.getLong(1),codigo);long permissao;
        if(existentes.isEmpty()){permissao=g(controle,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",codigo,"Permissão de fixture HTTP");permissoesNovas.add(permissao);}else permissao=existentes.getFirst();
        long perfil=g(controle,"INSERT INTO perfil(nome,descricao,situacao) VALUES (?,'Fixture HTTP','ATIVO')","HTTP-"+UUID.randomUUID());perfis.add(perfil);
        g(controle,"INSERT INTO perfil_permissao VALUES (?,?)",perfil,permissao);g(controle,"INSERT INTO usuario_perfil VALUES (?,?)",usuario,perfil);
    }
    @AfterEach void descartar() throws Exception {
        if(portal!=null)portal.close();if(cliente!=null)cliente.close();
        if(controle!=null)try(var conexao=controle) {
            g(controle,"DELETE FROM auditoria WHERE id_usuario IN (?,?)",admin,leitor);
            g(controle,"DELETE FROM historico_acesso_academico WHERE id_usuario_autor IN (?,?)",admin,leitor);
            g(controle,"DELETE FROM usuario_perfil WHERE id_usuario IN (?,?)",admin,leitor);
            for(long perfil:perfis){g(controle,"DELETE FROM perfil_permissao WHERE id_perfil=?",perfil);g(controle,"DELETE FROM perfil WHERE id_perfil=?",perfil);}
            g(controle,"DELETE FROM usuario WHERE id_usuario IN (?,?)",admin,leitor);
            for(long permissao:permissoesNovas)g(controle,"DELETE FROM permissao WHERE id_permissao=?",permissao);
        }
    }
    private record Entrada(String cookie,String csrf) { }
    private String extrair(String html,String nome) {var m=Pattern.compile("name=\""+nome+"\" value=\"([^\"]*)\"").matcher(html);assertTrue(m.find());return m.group(1);}
    private String cookie(HttpResponse<?> r,String nome) {return r.headers().allValues("Set-Cookie").stream().filter(s->s.startsWith(nome+"=")).findFirst().orElseThrow().split(";",2)[0];}
    private HttpResponse<String> requisitar(String caminho,String cookie,Map<String,String> campos) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+caminho)).timeout(Duration.ofSeconds(15));if(cookie!=null)b.header("Cookie",cookie);
        if(campos==null)b.GET();else {String corpo=campos.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).reduce((x,y)->x+"&"+y).orElse("");b.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(corpo));}
        return cliente.send(b.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private Entrada entrar(String login) throws Exception {
        var inicio=requisitar("/",null,null);var r=requisitar("/entrar",cookie(inicio,"SCOA_ENTRADA"),Map.of("csrf",extrair(inicio.body(),"csrf"),"login",login,"senha",senha));assertEquals(303,r.statusCode());
        String cookie=cookie(r,"SCOA_SESSAO");return new Entrada(cookie,extrair(requisitar("/",cookie,null).body(),"csrf"));
    }
    @Test void conexoesDistintasIsolamSessoesELogoutNaoEncerraOutraConta() throws Exception {
        var a=entrar(loginAdmin);var b=entrar(loginLeitor);assertNotEquals(a.cookie(),b.cookie());assertNotEquals(a.csrf(),b.csrf());
        assertEquals(2,abertas.size());assertNotSame(abertas.get(0),abertas.get(1));assertTrue(abertas.get(0).getAutoCommit());assertTrue(abertas.get(1).getAutoCommit());
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var ra=executor.submit(()->requisitar("/acao/secretaria-cursos",a.cookie(),Map.of("csrf",a.csrf())));
            var rb=executor.submit(()->requisitar("/acao/secretaria-cursos",b.cookie(),Map.of("csrf",b.csrf())));
            assertEquals(200,ra.get().statusCode());assertEquals(200,rb.get().statusCode());
        }
        assertEquals(403,requisitar("/acao/admin-contas",b.cookie(),null).statusCode());
        assertEquals(303,requisitar("/sair",a.cookie(),Map.of("csrf",a.csrf())).statusCode());assertTrue(abertas.getFirst().isClosed());assertFalse(abertas.getLast().isClosed());
        assertEquals(200,requisitar("/",b.cookie(),null).statusCode());assertEquals(401,requisitar("/acao/secretaria-cursos",a.cookie(),null).statusCode());
    }
    @Test void inativacaoEmOutraConexaoRevogaCookieMesmoDepoisDeReativacao() throws Exception {
        var a=entrar(loginAdmin);var b=entrar(loginLeitor);
        var repo=new RepositorioAdministracaoAcessoAcademicoJdbc(controle,new SegurancaJdbc(controle).entrar(loginAdmin,senha.toCharArray()));
        var c=repo.consultarConta(leitor);var inativa=repo.alterarConta(new AdministracaoAcessoAcademico.AlterarConta(leitor,c.versao(),"INATIVO","Fixture de revogação"));
        repo.alterarConta(new AdministracaoAcessoAcademico.AlterarConta(leitor,inativa.versao(),"ATIVO","Fixture de reativação"));
        assertEquals(401,requisitar("/",b.cookie(),null).statusCode());assertEquals(200,requisitar("/",a.cookie(),null).statusCode());
        var nova=entrar(loginLeitor);assertNotEquals(b.cookie(),nova.cookie());assertEquals(200,requisitar("/",nova.cookie(),null).statusCode());
    }
}

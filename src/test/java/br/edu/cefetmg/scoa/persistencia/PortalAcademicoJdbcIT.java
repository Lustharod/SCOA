package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.web.PortalAcademico;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** HTTP real e regras JDBC reais; a fixture transacional é desfeita por caso.
 * A conexão emprestada não é fechada pelo portal de teste. Isolamento entre conexões é testado separadamente. */
@Timeout(value=45,threadMode=Timeout.ThreadMode.SEPARATE_THREAD)
class PortalAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    private PortalAcademico portal;
    private HttpClient cliente;
    private String base,senha,cookie,csrf;
    private AtomicInteger fechamentos;
    private void salvarHtml(String nome,String html) throws Exception {var pasta=java.nio.file.Path.of("target/qa-portal");java.nio.file.Files.createDirectories(pasta);java.nio.file.Files.writeString(pasta.resolve(nome+".html"),html,StandardCharsets.UTF_8);}
    @BeforeEach void preparar() throws Exception {
        f=new CenarioAcademicoTeste();senha="HTTP-"+UUID.randomUUID();String hash=SegurancaJdbc.codificar(senha.toCharArray());
        g(f.c,"UPDATE usuario SET credencial_hash=? WHERE id_usuario IN (?,?,?,?,?)",hash,f.admin,f.alunoConta,f.docente,f.outroConta,f.outroDocente);
        fechamentos=new AtomicInteger();
        portal=new PortalAcademico(0,()->(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(obj,metodo,args)->{
            if(metodo.getName().equals("close")){fechamentos.incrementAndGet();return null;}
            try{return metodo.invoke(f.c,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
        }));portal.iniciar();base="http://127.0.0.1:"+portal.porta();
        cliente=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @AfterEach @Timeout(value=20,threadMode=Timeout.ThreadMode.SEPARATE_THREAD) void descartar() throws Exception {if(portal!=null)portal.close();if(cliente!=null)cliente.close();if(f!=null)f.close();}
    private HttpResponse<String> get(String caminho) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+caminho)).timeout(Duration.ofSeconds(15));if(cookie!=null)b.header("Cookie",cookie);
        return cliente.send(b.GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private String codificar(Map<String,String> campos) { return campos.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).reduce((a,b)->a+"&"+b).orElse(""); }
    private HttpResponse<String> post(String caminho,Map<String,String> campos) throws Exception {return post(caminho,campos,base);}
    private HttpResponse<String> post(String caminho,Map<String,String> campos,String origem) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+caminho)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded");if(cookie!=null)b.header("Cookie",cookie);if(origem!=null)b.header("Origin",origem);
        return cliente.send(b.POST(HttpRequest.BodyPublishers.ofString(codificar(campos))).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private String oculto(String html,String nome) {var m=Pattern.compile("name=\""+nome+"\" value=\"([^\"]*)\"").matcher(html);assertTrue(m.find(),"Campo "+nome+" ausente: "+html.substring(0,Math.min(html.length(),600)));return m.group(1);}
    private String obterCookie(HttpResponse<?> r,String nome) {return r.headers().allValues("Set-Cookie").stream().filter(s->s.startsWith(nome+"=")).findFirst().orElseThrow().split(";",2)[0];}
    private void entrar(long usuario) throws Exception {
        var pagina=get("/");cookie=obterCookie(pagina,"SCOA_ENTRADA");String token=oculto(pagina.body(),"csrf");
        String login=q(f.c,"SELECT identificacao FROM usuario WHERE id_usuario=?",r->r.getString(1),usuario).getFirst();
        var resposta=post("/entrar",Map.of("csrf",token,"login",login,"senha",senha));assertEquals(303,resposta.statusCode(),resposta.body());
        cookie=obterCookie(resposta,"SCOA_SESSAO");csrf=oculto(get("/").body(),"csrf");
    }
    private HttpResponse<String> preparar(String acao,Map<String,String> campos) throws Exception {var mapa=new LinkedHashMap<>(campos);mapa.put("csrf",csrf);return post("/acao/"+acao,mapa);}
    private HttpResponse<String> confirmar(HttpResponse<String> previa) throws Exception {assertEquals(200,previa.statusCode(),previa.body());return post("/confirmar",Map.of("csrf",csrf,"chave",oculto(previa.body(),"chave")));}
    @Test void paginaDeEntradaTemProtecoesESemDadosAcademicosAnonimos() throws Exception {
        var r=get("/");assertEquals(200,r.statusCode());assertTrue(r.body().contains("Acesse sua conta"));
        salvarHtml("entrada",r.body());
        assertEquals("no-store",r.headers().firstValue("Cache-Control").orElseThrow());assertEquals("DENY",r.headers().firstValue("X-Frame-Options").orElseThrow());
        assertEquals("same-origin",r.headers().firstValue("Referrer-Policy").orElseThrow(),"Formulários devem preservar Origin local no navegador.");
        assertTrue(r.headers().firstValue("Content-Security-Policy").orElseThrow().contains("form-action 'self'"));
        assertTrue(r.headers().allValues("Set-Cookie").getFirst().contains("HttpOnly; SameSite=Strict"));
        assertEquals(401,get("/acao/aluno-historico").statusCode());assertFalse(get("/acao/aluno-historico").body().contains(f.tag));
    }
    @Test void loginSemConferenciaOuSenhaCorretaNaoAbreSessao() throws Exception {
        assertEquals(403,post("/entrar",Map.of("login","qualquer","senha",senha,"csrf","inventado")).statusCode());
        var p=get("/");cookie=obterCookie(p,"SCOA_ENTRADA");
        var r=post("/entrar",Map.of("csrf",oculto(p.body(),"csrf"),"login","inexistente","senha",senha));assertEquals(401,r.statusCode());assertFalse(r.body().contains(senha));
    }
    @Test void alunoVeSomenteSuasAtividadesEServidorRejeitaRotaAdministrativaDireta() throws Exception {
        entrar(f.alunoConta);String inicio=get("/").body();assertTrue(inicio.contains("aluno-historico"));assertFalse(inicio.contains("admin-concessao"));assertFalse(inicio.contains("docente-turmas"));
        salvarHtml("aluno",inicio);
        assertEquals(403,get("/acao/admin-contas").statusCode());assertEquals(403,preparar("admin-contas",Map.of()).statusCode());
    }
    @Test void adminTecnicoNaoRecebeAreaDocenteOuGestaoSemVinculo() throws Exception {
        entrar(f.admin);String inicio=get("/").body();assertTrue(inicio.contains("admin-contas"));assertFalse(inicio.contains("docente-turmas"));assertFalse(inicio.contains("gestao-cursos"));
        salvarHtml("administracao",inicio);
        assertEquals(403,get("/acao/docente-turmas").statusCode());assertEquals(403,get("/acao/gestao-cursos").statusCode());
    }
    @Test void csrfEOrigemExternaImpedemGravacaoAntesDaPreparacao() throws Exception {
        entrar(f.alunoConta);
        var sem=post("/acao/aluno-manifestacao",Map.of("tipo","SUGESTAO","conteudo","Não gravar"));assertEquals(403,sem.statusCode());
        var fora=post("/acao/aluno-manifestacao",Map.of("csrf",csrf,"tipo","SUGESTAO","conteudo","Não gravar"),"https://exemplo.invalid");assertEquals(403,fora.statusCode());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",r->r.getInt(1),f.aluno).getFirst());
    }
    @Test void origemNulaNaoEContornoParaEntrarENoncePermaneceValidoParaOrigemLocal() throws Exception {
        var p=get("/");cookie=obterCookie(p,"SCOA_ENTRADA");String token=oculto(p.body(),"csrf");
        String login=q(f.c,"SELECT identificacao FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst();
        var campos=Map.of("csrf",token,"login",login,"senha",senha);
        assertEquals(403,post("/entrar",campos,"null").statusCode());
        var aceito=post("/entrar",campos,base);assertEquals(303,aceito.statusCode(),aceito.body());
        cookie=obterCookie(aceito,"SCOA_SESSAO");assertEquals(200,get("/").statusCode());
    }
    @Test void metadadoCrossSiteBloqueiaPostMesmoComOrigemLocalETokenValido() throws Exception {
        entrar(f.alunoConta);var campos=Map.of("csrf",csrf,"tipo","SUGESTAO","conteudo","Não gravar origem cruzada");
        var req=HttpRequest.newBuilder(URI.create(base+"/acao/aluno-manifestacao")).timeout(Duration.ofSeconds(15))
                .header("Content-Type","application/x-www-form-urlencoded").header("Cookie",cookie)
                .header("Origin",base).header("Sec-Fetch-Site","cross-site")
                .POST(HttpRequest.BodyPublishers.ofString(codificar(campos))).build();
        assertEquals(403,cliente.send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",r->r.getInt(1),f.aluno).getFirst());
        assertEquals(200,preparar("aluno-manifestacao",campos).statusCode());
    }
    @Test void preparacaoNaoGravaEConfirmacaoRepetidaNaoDuplicaManifestacao() throws Exception {
        entrar(f.alunoConta);String texto="<script>alert('x')</script> Sugestão sintética";
        var p=preparar("aluno-manifestacao",Map.of("tipo","SUGESTAO","conteudo",texto));assertEquals(200,p.statusCode());assertFalse(p.body().contains("<script>"));assertTrue(p.body().contains("&lt;script&gt;"));
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",r->r.getInt(1),f.aluno).getFirst());
        String chave=oculto(p.body(),"chave");var r=confirmar(p);assertEquals(200,r.statusCode(),r.body());
        assertTrue(post("/confirmar",Map.of("csrf",csrf,"chave",chave)).body().contains("confirmação já foi processada"));
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",x->x.getInt(1),f.aluno).getFirst());
        assertEquals(texto,q(f.c,"SELECT conteudo FROM manifestacao_aluno WHERE id_aluno=?",x->x.getString(1),f.aluno).getFirst());
    }
    @Test void confirmacoesSimultaneasDaMesmaSessaoExecutamSomenteUmaGravacao() throws Exception {
        entrar(f.alunoConta);var p=preparar("aluno-manifestacao",Map.of("tipo","RECLAMACAO","conteudo","Caso simultâneo"));String chave=oculto(p.body(),"chave");
        try(var exec=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var a=exec.submit(()->post("/confirmar",Map.of("csrf",csrf,"chave",chave)));var b=exec.submit(()->post("/confirmar",Map.of("csrf",csrf,"chave",chave)));
            assertEquals(200,a.get().statusCode());assertEquals(200,b.get().statusCode());
        }
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",x->x.getInt(1),f.aluno).getFirst());
    }
    @Test void camposExtrasEEscolhasForjadasSaoRecusados() throws Exception {
        entrar(f.alunoConta);assertEquals(400,preparar("aluno-manifestacao",Map.of("tipo","SUGESTAO","conteudo","Teste","aluno",Long.toString(f.outroAluno))).statusCode());
        assertEquals(400,preparar("aluno-manifestacao",Map.of("tipo","ADMIN","conteudo","Teste")).statusCode());
        assertEquals(409,post("/confirmar",Map.of("csrf",csrf,"chave","inventada")).statusCode());
    }
    @Test void alunoNaoSolicitaDocumentoParaMatriculaDeOutraPessoa() throws Exception {
        entrar(f.alunoConta);var r=preparar("aluno-documento",Map.of("matricula",Long.toString(f.outraMatricula),"tipo","HISTORICO_ESCOLAR"));assertEquals(400,r.statusCode());
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM solicitacao_documento WHERE id_matricula=?",x->x.getInt(1),f.outraMatricula).getFirst());
    }
    @Test void documentoWebPreservaPessoaEMatriculaComAutoria() throws Exception {
        entrar(f.alunoConta);var p=preparar("aluno-documento",Map.of("matricula",Long.toString(f.matricula),"tipo","HISTORICO_ESCOLAR"));var r=confirmar(p);assertEquals(200,r.statusCode(),r.body());
        assertEquals(f.alunoConta,q(f.c,"SELECT id_usuario_solicitante FROM solicitacao_documento WHERE id_matricula=?",x->x.getLong(1),f.matricula).getFirst());
        assertEquals(200,preparar("aluno-documentos",Map.of()).statusCode());
    }
    @Test void inativacaoOuRevogacaoDeSessaoValeNaProximaRequisicaoHttp() throws Exception {
        entrar(f.alunoConta);g(f.c,"UPDATE usuario SET geracao_sessao=geracao_sessao+1 WHERE id_usuario=?",f.alunoConta);
        var r=get("/");assertEquals(401,r.statusCode());assertTrue(r.body().contains("Entre novamente"));assertTrue(fechamentos.get()>0);
        assertEquals(401,preparar("aluno-manifestacao",Map.of("tipo","SUGESTAO","conteudo","Não gravar")).statusCode());
    }
    @Test void permissaoRevogadaEntrePreviaEConfirmacaoImpedeGravacao() throws Exception {
        f.conceder(f.alunoConta,AcessoAcademicoJdbc.PARAMETROS_MANTER);entrar(f.alunoConta);
        var campos=new LinkedHashMap<String,String>();campos.put("antecedencia","14");campos.put("duracaoPrimeira","7");campos.put("inicioSegunda","7");campos.put("duracaoSegunda","7");campos.put("imediato","13");campos.put("pedido","45");campos.put("transferencia","45");campos.put("retificacao","7");campos.put("atraso","15");campos.put("frequencia","75");campos.put("dispensa","7");campos.put("finalMinima","5");campos.put("motivo","Regra sintética");
        var p=preparar("parametros-criar",campos);assertEquals(200,p.statusCode(),p.body());
        g(f.c,"DELETE FROM usuario_perfil WHERE id_usuario=?",f.alunoConta);
        assertEquals(403,confirmar(p).statusCode());assertEquals(0,q(f.c,"SELECT COUNT(*) FROM versao_parametros_academicos",x->x.getInt(1)).getFirst());
    }
    @Test void professorNaoConsultaTurmaDeOutroDocentePelaUrl() throws Exception {
        entrar(f.outroDocente);assertEquals(400,preparar("docente-inscricoes",Map.of("turma",Long.toString(f.turma))).statusCode());
        assertEquals(400,preparar("docente-boletim",Map.of("inscricao",Long.toString(f.inscricao))).statusCode());
    }
    @Test void professorCadastraAvaliacaoPeloFluxoDeConferencia() throws Exception {
        entrar(f.docente);var p=preparar("docente-nova-avaliacao",Map.of("turma",Long.toString(f.turma),"tipo","P1","data",f.hoje.minusDays(20).toString(),"descricao","Prova sintética web"));
        salvarHtml("docente",get("/").body());salvarHtml("formulario-nota",get("/acao/docente-nota").body());salvarHtml("conferencia-avaliacao",p.body());
        assertEquals(200,confirmar(p).statusCode());assertEquals(1,q(f.c,"SELECT COUNT(*) FROM avaliacao WHERE id_turma=? AND tipo_prova='P1'",x->x.getInt(1),f.turma).getFirst());
    }
    @Test void cadastroWebCriaSemIdInventadoEAtualizaComConferenciaConcorrente() throws Exception {
        entrar(f.admin);String nome=f.tag+"-web";var p=preparar("secretaria-curso",Map.of("nome",nome,"situacao","ATIVO"));assertEquals(200,confirmar(p).statusCode());
        long id=q(f.c,"SELECT id_curso FROM curso WHERE nome=?",x->x.getLong(1),nome).getFirst();
        var alterar=preparar("secretaria-curso",Map.of("id",Long.toString(id),"nome",nome+" novo","situacao","ATIVO"));
        g(f.c,"UPDATE curso SET nome=? WHERE id_curso=?",nome+" concorrente",id);
        assertEquals(409,confirmar(alterar).statusCode());assertEquals(nome+" concorrente",q(f.c,"SELECT nome FROM curso WHERE id_curso=?",x->x.getString(1),id).getFirst());
    }
    @Test void exportacaoWebBaixaOsBytesArquivadosERevalidaAcesso() throws Exception {
        entrar(f.alunoConta);var p=preparar("relatorio-historico-proprio",Map.of());var r=confirmar(p);assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("Baixar PDF"));
        long id=q(f.c,"SELECT id_relatorio FROM relatorio_academico WHERE id_usuario_autor=?",x->x.getLong(1),f.alunoConta).getFirst();
        var req=HttpRequest.newBuilder(URI.create(base+"/arquivo?relatorio="+id+"&formato=PDF")).header("Cookie",cookie).GET().build();var pdf=cliente.send(req,HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,pdf.statusCode());assertEquals("application/pdf",pdf.headers().firstValue("Content-Type").orElseThrow());
        assertArrayEquals(q(f.c,"SELECT pdf FROM relatorio_academico WHERE id_relatorio=?",x->x.getBytes(1),id).getFirst(),pdf.body());
        g(f.c,"UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",f.aluno);
        assertNotEquals(200,get("/arquivo?relatorio="+id+"&formato=PDF").statusCode());
    }
    @Test void sairEncerraSessaoEConexaoSemReutilizarCookieAntigo() throws Exception {
        entrar(f.alunoConta);assertEquals(303,post("/sair",Map.of("csrf",csrf)).statusCode());assertEquals(1,fechamentos.get());
        assertEquals(401,get("/acao/aluno-historico").statusCode());
    }
    @Test void calendarioProprioNaoExigeConcessaoTecnicaDeSecretaria() throws Exception {
        entrar(f.alunoConta);var r=preparar("aluno-periodos",Map.of());assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains(f.tag));
        assertEquals(403,get("/acao/secretaria-periodos").statusCode());
    }
    @Test void diarioCompletoDaTurmaEConsultadoSemGerarArquivo() throws Exception {
        entrar(f.docente);var r=preparar("docente-diario",Map.of("turma",Long.toString(f.turma)));assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("Notas originais"));
        assertEquals(0,q(f.c,"SELECT COUNT(*) FROM relatorio_academico WHERE id_usuario_autor=?",x->x.getInt(1),f.docente).getFirst());
    }
    @Test void contaCriadaNaWebPreservaSenhaComEspacosSemExibiLaNaConferencia() throws Exception {
        entrar(f.admin);String login=f.tag+"-web";String segredo=" "+UUID.randomUUID()+" ";
        var p=preparar("admin-criar-conta",Map.of("login",login,"senha",segredo));assertEquals(200,p.statusCode());assertFalse(p.body().contains(segredo.strip()));
        var r=confirmar(p);assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("ID da nova conta de acesso"));
        long id=q(f.c,"SELECT id_usuario FROM usuario WHERE identificacao=?",x->x.getLong(1),login).getFirst();
        assertTrue(q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",x->SegurancaJdbc.confere(segredo.toCharArray(),x.getString(1)),id).getFirst());
        assertEquals(303,post("/sair",Map.of("csrf",csrf)).statusCode());senha=segredo;entrar(id);assertTrue(get("/").body().contains(login));
    }
    @Test void confirmacaoJaProcessadaNaoReexibeDadosDeVinculoPosteriormenteRetirado() throws Exception {
        entrar(f.alunoConta);String conteudo="Conteúdo do vínculo anterior";var p=preparar("aluno-manifestacao",Map.of("tipo","SUGESTAO","conteudo",conteudo));String chave=oculto(p.body(),"chave");assertEquals(200,confirmar(p).statusCode());
        // Preparação SQL simula revisão administrativa do vínculo, sem atribuí-la ao console.
        g(f.c,"UPDATE aluno SET id_usuario=NULL WHERE id_aluno=?",f.aluno);g(f.c,"UPDATE aluno SET id_usuario=? WHERE id_aluno=?",f.alunoConta,f.outroAluno);
        var r=post("/confirmar",Map.of("csrf",csrf,"chave",chave));assertEquals(200,r.statusCode());assertFalse(r.body().contains(conteudo));assertTrue(r.body().contains("não foi repetida"));
        assertEquals(1,q(f.c,"SELECT COUNT(*) FROM manifestacao_aluno WHERE id_aluno=?",x->x.getInt(1),f.aluno).getFirst());
    }
}

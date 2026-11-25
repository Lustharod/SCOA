package br.edu.cefetmg.scoa.web;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import br.edu.cefetmg.scoa.persistencia.*;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico;
import static br.edu.cefetmg.scoa.web.HtmlAcademico.*;
import static br.edu.cefetmg.scoa.web.AcoesAcademicasWeb.*;

/** Portal local do protótipo. Uma conexão por sessão; autorização revalidada em cada requisição. */
public final class PortalAcademico implements AutoCloseable {
    @FunctionalInterface public interface Conexoes { Connection abrir() throws SQLException; }
    private static final String COOKIE="SCOA_SESSAO",PRE_COOKIE="SCOA_ENTRADA";
    private final HttpServer servidor;
    private final Conexoes conexoes;
    private final AcoesAcademicasWeb acoes;
    private final Map<String,SessaoPortal> sessoes=new ConcurrentHashMap<>();
    private final Map<String,Instant> entradas=new ConcurrentHashMap<>();
    private final Deque<Instant> tentativas=new ArrayDeque<>();
    private final Semaphore capacidade=new Semaphore(64);
    private final Semaphore vagasSessoes=new Semaphore(64);
    private final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService limpeza=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"scoa-portal-sessoes");t.setDaemon(true);return t;});
    private static final class SessaoPortal {
        final Connection c; final SegurancaJdbc.Sessao sessao; final Runnable aoFechar; final String csrf=token(); final Instant fim=Instant.now().plus(Duration.ofHours(2));
        final LinkedHashMap<String,Pendente> pendentes=new LinkedHashMap<>(); Instant ultima=Instant.now();
        boolean fechada;
        SessaoPortal(Connection c,SegurancaJdbc.Sessao sessao,Runnable aoFechar) { this.c=c; this.sessao=sessao;this.aoFechar=aoFechar; }
        boolean expirada() { return Instant.now().isAfter(fim) || ultima.plus(Duration.ofMinutes(30)).isBefore(Instant.now()); }
        void fechar() { if(fechada)return;fechada=true;sessao.encerrar(); pendentes.clear(); try { c.close(); } catch(SQLException ignored) { }finally{aoFechar.run();} }
    }
    private static final class Pendente {
        final Acao acao; Preparacao preparacao; final Instant fim=Instant.now().plus(Duration.ofMinutes(10));
        String resultado; int status=200;
        Pendente(Acao acao,Preparacao preparacao) { this.acao=acao; this.preparacao=preparacao; }
    }
    public PortalAcademico(int porta,Conexoes conexoes) throws IOException { this(porta,conexoes,AcoesAcademicasWeb.criar()); }
    PortalAcademico(int porta,Conexoes conexoes,AcoesAcademicasWeb acoes) throws IOException {
        this.conexoes=Objects.requireNonNull(conexoes); this.acoes=Objects.requireNonNull(acoes);
        servidor=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),porta),32);
        servidor.createContext("/",this::atender); servidor.setExecutor(executor);
    }
    public void iniciar() { servidor.start(); limpeza.scheduleAtFixedRate(this::limpar,30,30,TimeUnit.SECONDS); }
    public int porta() { return servidor.getAddress().getPort(); }
    public static void main(String[] args) throws Exception {
        String url=System.getenv("SCOA_DB_URL");
        if(url==null || !url.matches("jdbc:mysql://[^/]+/scoa_java_teste(?:\\?.*)?"))
            throw new IllegalStateException("Configure explicitamente SCOA_DB_URL para a instância preparada de scoa_java_teste antes de iniciar o portal.");
        int porta=Integer.parseInt(System.getenv().getOrDefault("SCOA_HTTP_PORT","8080"));
        if(porta<1 || porta>65535) throw new IllegalArgumentException("Porta HTTP inválida.");
        var portal=new PortalAcademico(porta,ConexaoMySql::abrir);
        Runtime.getRuntime().addShutdownHook(new Thread(portal::close)); portal.iniciar();
        System.out.println("SCOA — Portal acadêmico local: http://127.0.0.1:"+portal.porta()+"/ | Encerre a execução para parar.");
    }
    private void limpar() {
        var agora=Instant.now(); entradas.entrySet().removeIf(e->e.getValue().isBefore(agora));
        sessoes.forEach((id,s)->{synchronized(s){s.pendentes.entrySet().removeIf(e->e.getValue().fim.isBefore(agora));if(s.expirada() && sessoes.remove(id,s))s.fechar();}});
    }
    @Override public void close() { servidor.stop(0); limpeza.shutdownNow(); sessoes.forEach((id,s)->{synchronized(s){s.fechar();}});sessoes.clear();executor.close(); }
    private static String token() { byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private static boolean igual(String a,String b) { return a!=null && b!=null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8)); }
    private String cookie(HttpExchange x,String nome) {
        String resultado=null;
        for(String linha:x.getRequestHeaders().getOrDefault("Cookie",List.of())) for(String parte:linha.split(";")) {
            var p=parte.strip().split("=",2);
            if(p.length==2 && p[0].equals(nome)) { if(resultado!=null)return null; resultado=p[1]; }
        }
        return resultado!=null && resultado.matches("[A-Za-z0-9_-]{43}")?resultado:null;
    }
    private void definirCookie(HttpExchange x,String nome,String valor,boolean apagar) {
        x.getResponseHeaders().add("Set-Cookie",nome+"="+valor+"; Path=/; HttpOnly; SameSite=Strict"+(apagar?"; Max-Age=0":""));
    }
    private boolean origemValida(HttpExchange x) {
        String host=x.getRequestHeaders().getFirst("Host");
        if(!Set.of("127.0.0.1:"+porta(),"localhost:"+porta()).contains(Objects.toString(host,""))) return false;
        String origem=x.getRequestHeaders().getFirst("Origin");
        if(origem!=null && !origem.equals("http://"+host)) return false;
        return !"cross-site".equals(x.getRequestHeaders().getFirst("Sec-Fetch-Site"));
    }
    private void atender(HttpExchange x) throws IOException {
        if(!capacidade.tryAcquire()) { resposta(x,503,pagina("Aguarde um instante","<p>Há muitas operações em andamento. Tente novamente.</p>",""));x.close();return; }
        try {
            if(!origemValida(x)) { resposta(x,403,pagina("Origem não autorizada","<p>Acesse o endereço local do portal.</p>",""));return; }
            if(!Set.of("GET","POST").contains(x.getRequestMethod())) {x.getResponseHeaders().set("Allow","GET, POST");resposta(x,405,pagina("Método não permitido","<a href=/>Voltar ao início</a>",""));return;}
            String caminho=x.getRequestURI().getRawPath();
            if(!caminho.matches("/(?:[a-z0-9-]+/?)*")) {resposta(x,404,pagina("Página não encontrada","<a href=/>Voltar ao início</a>",""));return;}
            if(caminho.equals("/entrar") && x.getRequestMethod().equals("POST")) { entrar(x);return; }
            String sid=cookie(x,COOKIE); var sessao=sid==null?null:sessoes.get(sid);
            if(sessao==null) {if(x.getRequestMethod().equals("GET") && (caminho.equals("/")||caminho.equals("/entrar")))login(x,200,"");else login(x,401,"Sua sessão terminou. Entre para continuar.");return;}
            synchronized(sessao) {
                if(sessao.expirada()) {sessoes.remove(sid,sessao);sessao.fechar();definirCookie(x,COOKIE,"",true);login(x,401,"Sua sessão terminou. Entre para continuar.");return;}
                SegurancaJdbc.ContaAcesso conta;
                try {conta=new SegurancaJdbc(sessao.c).minhaConta(sessao.sessao);} catch(IllegalArgumentException e) {sessoes.remove(sid,sessao);sessao.fechar();definirCookie(x,COOKIE,"",true);login(x,401,"O acesso foi alterado. Entre novamente.");return;}
                sessao.ultima=Instant.now(); var contexto=new Contexto(sessao.c,sessao.sessao,conta);
                FormularioWeb f=x.getRequestMethod().equals("POST")?formulario(x):FormularioWeb.decodificar(Objects.toString(x.getRequestURI().getRawQuery(),"").getBytes(StandardCharsets.UTF_8));
                if(x.getRequestMethod().equals("POST") && !igual(sessao.csrf,f.texto("csrf"))) {resposta(x,403,pagina("Confirmação inválida","<p>Reabra o formulário no portal e tente novamente.</p>",barra(conta,sessao)));return;}
                if(caminho.equals("/sair") && x.getRequestMethod().equals("POST")) {sessoes.remove(sid,sessao);sessao.fechar();definirCookie(x,COOKIE,"",true);redirecionar(x,"/");return;}
                if(caminho.equals("/") && x.getRequestMethod().equals("GET")) {inicio(x,contexto,sessao);return;}
                if(caminho.equals("/arquivo") && x.getRequestMethod().equals("GET")) {baixar(x,contexto,f);return;}
                if(caminho.equals("/confirmar") && x.getRequestMethod().equals("POST")) {confirmar(x,contexto,sessao,f);return;}
                if(caminho.startsWith("/acao/")) {
                    var acao=acoes.buscar(caminho.substring(6));
                    if(!acao.permitida().test(conta)) {resposta(x,403,pagina("Acesso restrito","<p>Esta operação não está disponível para seus vínculos e permissões atuais.</p>",barra(conta,sessao)));return;}
                    if(x.getRequestMethod().equals("GET")) {mostrarFormulario(x,contexto,sessao,acao,Map.of(),"");return;}
                    try {
                        validarCampos(acao,f); var p=acao.preparar().apply(contexto,f);
                        if(acao.grava()!=p.grava()) throw new IllegalStateException("Preparação incompatível com a ação.");
                        if(!p.grava()) {resposta(x,200,pagina(acao.titulo(),"<section class=card>"+dados(p.previa())+"</section>"+voltar(acao),barra(conta,sessao)));return;}
                        while(sessao.pendentes.size()>=40) sessao.pendentes.remove(sessao.pendentes.keySet().iterator().next());
                        String chave=token(); sessao.pendentes.put(chave,new Pendente(acao,p));
                        String corpo="<p class=notice>Confira os dados antes de confirmar. A operação será validada novamente ao gravar.</p><section class=card>"+dados(p.previa())+"</section><form method=post action=/confirmar>"+oculto("csrf",sessao.csrf)+oculto("chave",chave)+"<button type=submit>Confirmar operação</button></form>"+voltar(acao);
                        resposta(x,200,pagina("Conferir: "+acao.titulo(),corpo,barra(conta,sessao)));return;
                    } catch(IllegalArgumentException e) {mostrarFormulario(x,contexto,sessao,acao,f.valores(),e.getMessage());return;}
                }
                resposta(x,404,pagina("Página não encontrada","<a href=/>Voltar ao início</a>",barra(conta,sessao)));
            }
        } catch(IllegalArgumentException e) {resposta(x,400,pagina("Confira a solicitação","<p class=erro>"+escapar(e.getMessage())+"</p><a href=/>Voltar ao início</a>",""));}
          catch(RuntimeException e) {String ref=UUID.randomUUID().toString();System.err.println("Falha no portal SCOA: "+ref+" | "+e.getClass().getSimpleName());resposta(x,503,pagina("Operação indisponível","<p>Não foi possível concluir a operação. Consulte os dados antes de tentar novamente.</p><p>Referência: "+escapar(ref)+"</p><a href=/>Voltar ao início</a>",""));}
        finally {x.close();capacidade.release();}
    }
    private FormularioWeb formulario(HttpExchange x) throws IOException {
        String tipo=x.getRequestHeaders().getFirst("Content-Type");
        if(tipo==null || !tipo.split(";",2)[0].strip().equalsIgnoreCase("application/x-www-form-urlencoded")) throw new IllegalArgumentException("Envie o formulário da página do portal.");
        byte[] bytes=x.getRequestBody().readNBytes(131073);return FormularioWeb.decodificar(bytes);
    }
    private void login(HttpExchange x,int status,String erro) throws IOException {
        entradas.entrySet().removeIf(e->e.getValue().isBefore(Instant.now())); if(entradas.size()>=256) {resposta(x,503,pagina("Aguarde","<p>Tente entrar novamente em alguns minutos.</p>",""));return;}
        String chave=token();entradas.put(chave,Instant.now().plus(Duration.ofMinutes(10)));definirCookie(x,PRE_COOKIE,chave,false);
        String corpo="<section class=\"card login\"><h2>Acesse sua conta</h2><p>Use a mesma conta de acesso. Seus vínculos acadêmicos e permissões definem as opções disponíveis.</p>"+(erro.isEmpty()?"":"<p class=erro role=alert>"+escapar(erro)+"</p>")+"<form method=post action=/entrar>"+oculto("csrf",chave)+"<label for=login>Identificação da conta<input id=login name=login autocomplete=username required maxlength=100></label><label for=senha>Senha<input id=senha name=senha type=password autocomplete=current-password required maxlength=256></label><button type=submit>Entrar</button></form></section>";
        resposta(x,status,pagina("Bem-vindo ao SCOA",corpo,""));
    }
    private void entrar(HttpExchange x) throws IOException {
        var f=formulario(x);String chave=cookie(x,PRE_COOKIE);
        if(chave==null || !igual(chave,f.texto("csrf"))) {login(x,403,"Reabra a página de entrada e tente novamente.");return;}
        var fim=entradas.remove(chave); if(fim==null || fim.isBefore(Instant.now())) {login(x,403,"O formulário de entrada expirou. Tente novamente.");return;}
        synchronized(tentativas) {var limite=Instant.now().minusSeconds(60);while(!tentativas.isEmpty() && tentativas.getFirst().isBefore(limite))tentativas.removeFirst();if(tentativas.size()>=30){login(x,429,"Aguarde um minuto antes de tentar entrar novamente.");return;}tentativas.addLast(Instant.now());}
        if(!vagasSessoes.tryAcquire()) {login(x,503,"O portal atingiu o limite de sessões. Tente novamente depois.");return;}
        Connection c=null;boolean reservada=true;
        try {
            c=conexoes.abrir(); if(!"scoa_java_teste".equals(c.getCatalog())) throw new IllegalStateException("Portal do protótipo exige scoa_java_teste.");
            var s=new SegurancaJdbc(c).entrar(f.obrigatorio("login"),f.valores().getOrDefault("senha","").toCharArray());
            String sid=token();sessoes.put(sid,new SessaoPortal(c,s,vagasSessoes::release));c=null;reservada=false;
            String anterior=cookie(x,COOKIE);if(anterior!=null){var antiga=sessoes.remove(anterior);if(antiga!=null)synchronized(antiga){antiga.fechar();}}
            definirCookie(x,COOKIE,sid,false);definirCookie(x,PRE_COOKIE,"",true);redirecionar(x,"/");
        } catch(IllegalArgumentException e) {login(x,401,"Identificação ou senha inválida, ambígua ou inativa.");}
          catch(SQLException e) {login(x,503,"Não foi possível acessar o serviço acadêmico. Tente novamente depois.");}
        finally {if(c!=null)try{c.close();}catch(SQLException ignored){}if(reservada)vagasSessoes.release();}
    }
    private String barra(SegurancaJdbc.ContaAcesso conta,SessaoPortal s) {
        return "<nav class=barra aria-label=\"Navegação principal\"><a href=/>Início</a><span>"+escapar(conta.identificacao())+" · conta "+conta.idConta()+"</span><form method=post action=/sair>"+oculto("csrf",s.csrf)+"<button type=submit>Sair</button></form></nav>";
    }
    private void inicio(HttpExchange x,Contexto c,SessaoPortal s) throws IOException {
        var grupos=new LinkedHashMap<String,List<Acao>>();acoes.disponiveis(c.conta()).forEach(a->grupos.computeIfAbsent(a.grupo(),k->new ArrayList<>()).add(a));
        StringBuilder b=new StringBuilder("<p>Escolha uma atividade. A mesma conta reúne seus vínculos legítimos; cada operação confere sua autorização atual.</p><details class=card><summary>Minha conta e meus vínculos</summary>"+dados(c.conta())+"</details>");
        if(grupos.isEmpty()) b.append("<p class=notice>Nenhuma atividade acadêmica disponível. Solicite à administração a conferência dos seus vínculos e permissões.</p>");
        grupos.forEach((grupo,lista)->{b.append("<h2>").append(escapar(grupo)).append("</h2><div class=grid>");lista.forEach(a->b.append("<article class=card><h3><a href=/acao/").append(a.id()).append(">").append(escapar(a.titulo())).append("</a></h3><p>").append(escapar(a.descricao())).append("</p></article>"));b.append("</div>");});
        resposta(x,200,pagina("Sua área acadêmica",b.toString(),barra(c.conta(),s)));
    }
    private void validarCampos(Acao a,FormularioWeb f) {
        var nomes=new HashSet<>(List.of("csrf"));a.campos().forEach(c->nomes.add(c.nome()));
        if(!nomes.containsAll(f.valores().keySet())) throw new IllegalArgumentException("O formulário contém campos não previstos. Reabra a página.");
        for(var campo:a.campos()) {
            String valor=f.texto(campo.nome());if(!campo.opcional() && valor.isEmpty())throw new IllegalArgumentException("Preencha "+campo.titulo()+".");
            if(!valor.isEmpty() && !campo.opcoes().isEmpty() && !campo.opcoes().contains(valor)) throw new IllegalArgumentException("Selecione uma opção válida em "+campo.titulo()+".");
        }
    }
    private void mostrarFormulario(HttpExchange x,Contexto c,SessaoPortal s,Acao a,Map<String,String> valores,String erro) throws IOException {
        var b=new StringBuilder("<p>"+escapar(a.descricao())+"</p>"+(erro.isEmpty()?"":"<p class=erro role=alert>"+escapar(erro)+"</p>")+"<section class=formulario><form method=post action=/acao/"+a.id()+">"+oculto("csrf",s.csrf)+"<div class=fields>");
        for(var campo:a.campos()) {
            String valor=campo.tipo().equals("password")?"":valores.getOrDefault(campo.nome(),campo.padrao()),atributos=" id=\""+escapar(campo.nome())+"\" name=\""+escapar(campo.nome())+"\""+(campo.opcional()?"":" required");
            b.append("<label for=\"").append(escapar(campo.nome())).append("\">").append(escapar(campo.titulo())).append(campo.opcional()?" (opcional)":"");
            if(campo.tipo().equals("textarea")) b.append("<textarea").append(atributos).append(" maxlength=40000>").append(escapar(valor)).append("</textarea>");
            else if(campo.tipo().equals("select")) {b.append("<select").append(atributos).append("><option value=\"\">Selecione</option>");for(String o:campo.opcoes())b.append("<option value=\"").append(escapar(o)).append("\"").append(o.equals(valor)?" selected":"").append(">").append(escapar(o.replace('_',' '))).append("</option>");b.append("</select>");}
            else b.append("<input type=\"").append(campo.tipo().equals("decimal")?"text":escapar(campo.tipo())).append("\"").append(atributos).append(" value=\"").append(escapar(valor)).append("\" maxlength=1000").append(campo.tipo().equals("password")?" autocomplete=new-password":"").append(">");
            if(!campo.ajuda().isEmpty())b.append("<small>").append(escapar(campo.ajuda())).append("</small>");b.append("</label>");
        }
        b.append("</div><button type=submit>").append(a.grava()?"Conferir antes de gravar":"Consultar").append("</button></form></section><a href=/>&larr; Voltar ao início</a>");
        resposta(x,erro.isEmpty()?200:400,pagina(a.titulo(),b.toString(),barra(c.conta(),s)));
    }
    private void confirmar(HttpExchange x,Contexto c,SessaoPortal s,FormularioWeb f) throws IOException {
        var p=s.pendentes.get(f.texto("chave"));
        if(p==null || p.fim.isBefore(Instant.now())) {resposta(x,409,pagina("Conferência expirada","<p>Reabra a atividade e confira os dados novamente.</p><a href=/>Voltar ao início</a>",barra(c.conta(),s)));return;}
        if(!p.acao.permitida().test(c.conta())) {resposta(x,403,pagina("Acesso alterado","<p>Suas permissões atuais não permitem esta operação.</p>",barra(c.conta(),s)));return;}
        if(p.resultado!=null) {resposta(x,p.status,pagina(p.acao.titulo(),"<p class=notice>Esta confirmação já foi processada. A operação não foi repetida. Consulte os dados pela atividade para conferir sua situação atual.</p>"+voltar(p.acao),barra(c.conta(),s)));return;}
        if(p.resultado==null) {
            try {p.resultado="<p class=notice>Operação concluída.</p><section class=card>"+dados(p.preparacao.confirmar().apply(c))+"</section>";}
            catch(IllegalArgumentException e) {p.status=409;p.resultado="<p class=erro role=alert>"+escapar(e.getMessage())+"</p><p>Confira os dados atuais antes de preparar uma nova operação.</p>";}
            catch(RuntimeException e) {p.status=503;p.resultado="<p class=erro>Não foi possível concluir a operação. Consulte os registros antes de tentar novamente.</p>";System.err.println("Falha ao confirmar atividade acadêmica: "+p.acao.id()+" | "+e.getClass().getSimpleName());}
            finally {p.preparacao=null;}
        }
        String resultado=p.resultado;p.resultado="";
        resposta(x,p.status,pagina(p.acao.titulo(),resultado+voltar(p.acao),barra(c.conta(),s)));
    }
    private void baixar(HttpExchange x,Contexto c,FormularioWeb f) throws IOException {
        var formato=f.escolha("formato",RelatorioAcademico.Formato.class);
        var arquivo=new RepositorioRelatorioAcademicoJdbc(c.c(),c.sessao()).arquivo(f.id("relatorio"),formato);
        byte[] bytes=arquivo.conteudo();cabecalhos(x);x.getResponseHeaders().set("Content-Type",arquivo.mime());
        x.getResponseHeaders().set("Content-Disposition","attachment; filename=\""+arquivo.nome().replaceAll("[^A-Za-z0-9._-]","_")+"\"");
        x.getResponseHeaders().set("X-Content-SHA256",arquivo.sha256());x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes);
    }
    private static String oculto(String nome,String valor) { return "<input type=hidden name=\""+nome+"\" value=\""+escapar(valor)+"\">"; }
    private static String voltar(Acao a) {return "<p><a href=/acao/"+a.id()+">Voltar à atividade</a> · <a href=/>Início</a></p>";}
    private void cabecalhos(HttpExchange x) {
        var h=x.getResponseHeaders();h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");h.set("Referrer-Policy","no-referrer");h.set("X-Frame-Options","DENY");
        h.set("Content-Security-Policy","default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
    }
    private void resposta(HttpExchange x,int status,String html) throws IOException {cabecalhos(x);x.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");byte[] bytes=html.getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(status,bytes.length);x.getResponseBody().write(bytes);}
    private void redirecionar(HttpExchange x,String destino) throws IOException {cabecalhos(x);x.getResponseHeaders().set("Location",destino);x.sendResponseHeaders(303,-1);}
}

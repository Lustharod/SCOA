package br.edu.cefetmg.scoa.web;

import java.lang.reflect.*;
import java.util.*;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico;

/** Apenas apresentação de DTOs já autorizados; texto de usuário sempre escapado. */
final class HtmlAcademico {
    private HtmlAcademico() { }
    static String escapar(Object valor) { return Objects.toString(valor,"").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    static String rotulo(String nome) {
        return switch(nome) {
            case "idConta","usuario" -> "ID da conta de acesso";
            case "aluno" -> "Referência do aluno";
            case "professor" -> "Referência do professor";
            case "curso" -> "Referência do curso";
            case "matricula" -> "Matrícula institucional";
            case "inscricao" -> "Inscrição na turma";
            case "versao","versaoVinculo" -> "Versão consultada";
            case "registradoEm","solicitadoEm","geradoEm","data" -> "Data / instante registrado";
            case "id" -> "Referência";
            case "minutosPrevistos" -> "Carga prevista (minutos)";
            case "minutosConsiderados" -> "Carga considerada para esta inscrição (minutos)";
            case "minutosPresentes" -> "Presença (minutos)";
            case "assinatura" -> "Referência de integridade";
            default -> { String s=nome.replaceAll("([a-zà-ú])([A-Z])","$1 $2"); yield s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1); }
        };
    }
    static String dados(Object valor) {
        if(valor==null) return "<span class=muted>Não informado</span>";
        if(valor instanceof DiarioAcademico.Boletim b) {
            var s=new StringBuilder("<h2>Notas e frequência</h2><p>Inscrição "+b.inscricao()+" · turma "+b.turma()+"</p><div class=grid>");
            for(var item:List.of(new Object[]{"Carga prevista",b.minutosPrevistos()},new Object[]{"Aulas registradas",b.minutosRegistrados()},new Object[]{"Carga considerada nesta inscrição",b.minutosConsiderados()},new Object[]{"Presença registrada",b.minutosPresentes()}))
                s.append("<article class=card><strong>").append(escapar(item[0])).append("</strong><span>").append(escapar(item[1])).append(" minutos</span></article>");
            s.append("</div>");
            if(b.encerrado())s.append("<p class=notice>Resultado formal encerrado. Esta tela preserva os lançamentos originais; consulte os resultados finais para a situação vigente após eventuais retificações.</p>");
            if(!b.pendencias().isEmpty())s.append("<h3>Pendências de apuração</h3>").append(dados(b.pendencias()));
            if(b.previa()!=null)s.append("<h3>").append(b.encerrado()?"Apuração dos lançamentos originais":"Prévia do resultado").append("</h3>").append(dados(b.previa()));
            var notas=b.notas().stream().map(n->List.of(Objects.toString(n.avaliacao().tipo(),"Não identificado"),Objects.toString(n.avaliacao().data(),"Não informada"),Long.toString(n.avaliacao().id()),n.valor()==null?"Sem lançamento":n.valor().toPlainString(),Objects.toString(n.versao(),"—"))).toList();
            var chamadas=b.frequencias().stream().map(f->List.of(Objects.toString(f.aula().data(),"Não informada"),Objects.toString(f.aula().tempo(),"Não identificado"),Long.toString(f.aula().id()),f.situacao()==null?"Sem lançamento":f.situacao(),f.considerada()?"Sim":"Não — anterior à inscrição",Objects.toString(f.origem(),"—"),Objects.toString(f.versao(),"—"))).toList();
            var d=new RelatorioAcademico.Documento("Lançamentos por avaliação e tempo",List.of(),List.of("Ausência de lançamento não equivale a nota zero ou falta. Tempos anteriores à inscrição ficam fora da frequência individual."),
                    List.of(new RelatorioAcademico.Tabela("Notas",List.of("Prova","Data","ID da avaliação","Nota","Versão"),notas),new RelatorioAcademico.Tabela("Chamadas",List.of("Data","Tempo","ID da aula","Situação","Considerada","Origem","Versão"),chamadas)));
            return s.append(dados(d)).toString();
        }
        if(valor instanceof RelatorioAcademico.Registro r) {
            var campos=new LinkedHashMap<String,Object>();campos.put("Protocolo",r.id());campos.put("Referência",r.chave());campos.put("Filtros",r.filtro());campos.put("Autor",r.autor());campos.put("Gerado em (UTC)",r.geradoEm());
            return dados(campos)+"<p><a class=botao href=\"/arquivo?relatorio="+r.id()+"&amp;formato=PDF\">Baixar PDF</a><a class=botao href=\"/arquivo?relatorio="+r.id()+"&amp;formato=XLSX\">Baixar XLSX</a></p>"+dados(r.documento());
        }
        if(valor instanceof Optional<?> o) return o.map(HtmlAcademico::dados).orElse("<p>Nenhum registro disponível.</p>");
        if(valor instanceof RelatorioAcademico.Documento d) {
            var s=new StringBuilder("<h2>"+escapar(d.titulo())+"</h2>");
            d.parametros().forEach(p->s.append("<p><strong>").append(escapar(p.nome())).append(":</strong> ").append(escapar(p.valor())).append("</p>"));
            d.avisos().forEach(a->s.append("<p class=notice>").append(escapar(a)).append("</p>"));
            d.tabelas().forEach(t->{s.append("<h3>").append(escapar(t.titulo())).append("</h3><div class=scroll><table><thead><tr>");t.colunas().forEach(c->s.append("<th scope=col>").append(escapar(c)).append("</th>"));s.append("</tr></thead><tbody>");t.linhas().forEach(l->{s.append("<tr>");l.forEach(c->s.append("<td>").append(escapar(c)).append("</td>"));s.append("</tr>");});s.append("</tbody></table></div>");});
            return s.toString();
        }
        if(valor instanceof Collection<?> lista) {
            if(lista.isEmpty()) return "<p>Nenhum registro disponível.</p>";
            Object primeiro=lista.iterator().next();
            if(primeiro instanceof RelatorioAcademico.Registro) return lista.stream().map(x->"<section class=card>"+dados(x)+"</section>").reduce("",String::concat);
            if(primeiro!=null && primeiro.getClass().isRecord() && lista.stream().allMatch(x->x!=null && x.getClass()==primeiro.getClass())) {
                var componentes=primeiro.getClass().getRecordComponents(); var s=new StringBuilder("<div class=scroll><table><thead><tr>");
                for(var c:componentes) s.append("<th scope=col>").append(escapar(rotulo(c.getName()))).append("</th>");
                s.append("</tr></thead><tbody>"); for(var item:lista) { s.append("<tr>"); for(var c:componentes) s.append("<td>").append(dados(ler(c,item))).append("</td>"); s.append("</tr>"); }
                return s.append("</tbody></table></div>").toString();
            }
            return "<ul>"+lista.stream().map(x->"<li>"+dados(x)+"</li>").reduce("",String::concat)+"</ul>";
        }
        if(valor instanceof Map<?,?> mapa) { var s=new StringBuilder("<dl>");mapa.forEach((k,v)->s.append("<dt>").append(escapar(k)).append("</dt><dd>").append(dados(v)).append("</dd>"));return s.append("</dl>").toString(); }
        if(valor instanceof Sala s) return dados(new SalaVista(s.getId(),s.getIdentificacao(),s.getLocalizacao(),s.getCapacidade(),s.getSituacao()));
        if(valor.getClass().isRecord()) {
            var s=new StringBuilder("<dl>"); for(var c:valor.getClass().getRecordComponents()) s.append("<dt>").append(escapar(rotulo(c.getName()))).append("</dt><dd>").append(dados(ler(c,valor))).append("</dd>"); return s.append("</dl>").toString();
        }
        if(valor instanceof byte[] || valor instanceof java.sql.Connection) throw new IllegalArgumentException("Conteúdo não destinado à apresentação.");
        return "<span class=valor>"+escapar(valor instanceof Boolean b?(b?"Sim":"Não"):valor)+"</span>";
    }
    private record SalaVista(Long id,String identificacao,String localizacao,int capacidade,String situacao) { }
    private static Object ler(RecordComponent c,Object item) {
        try { return c.getAccessor().invoke(item); } catch(ReflectiveOperationException e) { throw new IllegalStateException("Não foi possível apresentar o registro autorizado.",e); }
    }
    static String pagina(String titulo,String conteudo,String barra) {
        return "<!doctype html><html lang=pt-BR><head><meta charset=utf-8><meta name=viewport content=\"width=device-width, initial-scale=1\"><title>"+escapar(titulo)+" · SCOA</title><style>"+CSS+"</style></head><body><a class=pular href=#conteudo>Ir ao conteúdo</a><header><a class=marca href=/>SCOA<span>Portal acadêmico</span></a>"+barra+"</header><main id=conteudo><p class=eyebrow>MÓDULO ACADÊMICO</p><h1>"+escapar(titulo)+"</h1>"+conteudo+"</main><footer>SCOA · Protótipo acadêmico · CEFET/RJ</footer></body></html>";
    }
    private static final String CSS="""
            :root{color-scheme:light;--ink:#162840;--blue:#123b72;--line:#d8e1eb;--bg:#f3f6fa;--teal:#087d72}*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.5 system-ui,-apple-system,Segoe UI,sans-serif}header{background:var(--blue);color:white;padding:20px max(24px,calc((100% - 1200px)/2));display:flex;justify-content:space-between;gap:24px;align-items:center}header a{color:white}.marca{font-size:28px;font-weight:800;text-decoration:none;letter-spacing:2px}.marca span{display:block;font-size:12px;font-weight:400;letter-spacing:1px}main{max-width:1248px;margin:auto;padding:32px 24px 64px}h1{font-size:clamp(26px,4vw,38px);line-height:1.2;margin:8px 0 24px}h2{font-size:23px;margin:28px 0 12px}h3{font-size:18px}.eyebrow{font-size:12px;letter-spacing:2px;font-weight:750;color:var(--teal)}.card,section.formulario{background:white;border:1px solid var(--line);border-radius:12px;padding:24px;margin:14px 0;box-shadow:0 3px 12px #102a4310}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}.grid .card{margin:0;display:flex;flex-direction:column;gap:8px}.card h3{margin:0}.card a{font-weight:650}.muted,small{color:#50627a}.notice{background:#e5f3f0;border-left:4px solid var(--teal);padding:16px;border-radius:4px}.erro{background:#fff0ef;border-left:4px solid #b32820;padding:16px}.fields{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:20px}label{font-weight:650;display:block}input,select,textarea{display:block;margin-top:6px;padding:11px;width:100%;border:1px solid #8e9eb2;border-radius:6px;font:inherit;background:white;color:var(--ink)}textarea{min-height:120px;resize:vertical}label small{display:block;font-weight:400;margin-top:5px}button,.botao{display:inline-block;padding:11px 20px;border:0;border-radius:6px;background:var(--blue);color:white;font:650 16px system-ui;cursor:pointer;text-decoration:none;margin:18px 8px 0 0}button:hover,.botao:hover{background:#0b2a54}a{color:var(--blue);text-underline-offset:3px}a:focus-visible,button:focus-visible,input:focus-visible,textarea:focus-visible,select:focus-visible{outline:3px solid #dc9b19;outline-offset:3px}.scroll{overflow:auto;max-width:100%;margin:16px 0}table{border-collapse:collapse;width:100%;background:white;font-size:14px}th,td{border:1px solid var(--line);padding:10px 12px;text-align:left;vertical-align:top;min-width:100px}th{background:#e7edf5}td dl{min-width:210px}dl{display:grid;grid-template-columns:minmax(140px,1fr) minmax(120px,3fr);gap:8px 16px}dt{font-weight:650}dd{margin:0;overflow-wrap:anywhere}.valor{white-space:pre-wrap;overflow-wrap:anywhere}.barra{display:flex;align-items:center;gap:18px;font-size:14px}.barra form{margin:0}.barra button{margin:0;background:#ffffff20;border:1px solid #ffffff70;padding:8px 14px}.login{max-width:480px;margin:35px auto}.pular{position:absolute;top:-80px;background:white;padding:12px;z-index:9}.pular:focus{top:5px}footer{padding:24px;text-align:center;color:#50627a;border-top:1px solid var(--line);font-size:13px}@media(max-width:600px){header{padding:16px;align-items:flex-start}main{padding:24px 16px}.barra{flex-wrap:wrap;justify-content:flex-end;gap:8px}dl{display:block}dd{margin:4px 0 14px}.card,section.formulario{padding:18px}}
            """;
}

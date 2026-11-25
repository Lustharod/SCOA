package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Fontes obtidas pelas mesmas fronteiras acadêmicas usadas no console. */
final class FontesRelatorioAcademicoJdbc {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    FontesRelatorioAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) { this.c=c; this.sessao=sessao; }
    long autorizar(Filtro f,Long dono) throws SQLException {
        long autor=SegurancaJdbc.exigirSessao(c,sessao);
        switch(f.tipo()) {
            case CENSO_DEMONSTRATIVO,ENADE_DEMONSTRATIVO -> new AcessoAcademicoJdbc(c,sessao).consultarRelatorios(()->null);
            case DIARIO_DOCENTE -> new RepositorioDiarioAcademicoJdbc(c,sessao).aulas(f.turma());
            case DIARIO_GESTAO -> new AcessoGestaoAcademicaJdbc(c,sessao).turma(f.curso(),f.turma());
            case HISTORICO_PROPRIO -> {
                if(dono!=null && dono!=autor) throw new IllegalArgumentException("Relatório restrito à conta que o gerou.");
                if(q(c,"SELECT id_aluno FROM aluno WHERE id_usuario=? FOR SHARE",r->r.getLong(1),autor).isEmpty())
                    throw new IllegalArgumentException("Conta sem vínculo de aluno.");
            }
            case PLANO_PROPRIO -> {
                if(dono!=null && dono!=autor) throw new IllegalArgumentException("Relatório restrito à conta que o gerou.");
                var alunos=q(c,"SELECT id_aluno FROM matricula_academica WHERE id_matricula=? FOR SHARE",r->r.getLong(1),f.matricula());
                if(alunos.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada ou não autorizada.");
                SegurancaJdbc.exigirAluno(c,autor,alunos.getFirst());
            }
        }
        return autor;
    }
    Documento montar(Filtro f,List<String> problemas) throws SQLException {
        autorizar(f,null);
        return switch(f.tipo()) {
            case CENSO_DEMONSTRATIVO,ENADE_DEMONSTRATIVO -> mec(f,problemas);
            case DIARIO_DOCENTE,DIARIO_GESTAO -> diario(f);
            case HISTORICO_PROPRIO -> historico();
            case PLANO_PROPRIO -> plano(f,problemas);
        };
    }
    private Documento mec(Filtro f,List<String> problemas) throws SQLException {
        var curso=q(c,"SELECT nome,situacao FROM curso WHERE id_curso=? FOR SHARE",r->new String[]{r.getString(1),r.getString(2)},f.curso());
        var periodo=q(c,"SELECT descricao,data_inicio,data_fim,situacao FROM periodo_letivo WHERE id_periodo=? FOR SHARE",
                r->new Object[]{r.getString(1),r.getObject(2,LocalDate.class),r.getObject(3,LocalDate.class),r.getString(4)},f.periodo());
        if(curso.isEmpty() || periodo.isEmpty()) throw new IllegalArgumentException("Curso/período não encontrado.");
        if(curso.getFirst()[0].isBlank() || ((String)periodo.getFirst()[0]).isBlank()) problemas.add("Nome do curso ou descrição do período ausente; complemente a origem antes de gerar.");
        var parametros=new ArrayList<Parametro>();
        parametros.add(new Parametro("Curso",f.curso()+" - "+curso.getFirst()[0]));
        parametros.add(new Parametro("Período",f.periodo()+" - "+periodo.getFirst()[0]+"; "+periodo.getFirst()[1]+" a "+periodo.getFirst()[2]));
        parametros.add(new Parametro("População","Matrículas com ingresso, rematrícula ou inscrição registrada no período selecionado, sem duplicar matrícula."));
        parametros.add(new Parametro("Situações","Situações atuais dos registros na extração; não reconstrução da situação em uma data passada."));
        var avisos=new ArrayList<String>();
        avisos.add("DEMONSTRATIVO DO PROTÓTIPO. Sem formato oficial do MEC e sem envio externo.");
        avisos.add("Não define aptidão ao ENADE, ano de formação, condição de concluinte ou conclusão de curso.");
        var populacao=q(c,"""
                SELECT m.id_matricula,m.id_aluno,a.identificacao,m.situacao,a.situacao,m.data_ingresso,
                    v.id_estrutura FROM matricula_academica m JOIN aluno a ON a.id_aluno=m.id_aluno
                LEFT JOIN vinculo_curricular_matricula v ON v.id_matricula=m.id_matricula
                WHERE m.id_curso=? AND (m.id_periodo_ingresso=? OR EXISTS (
                    SELECT 1 FROM rematricula_academica r WHERE r.id_matricula=m.id_matricula AND r.id_periodo=?)
                    OR EXISTS (SELECT 1 FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma
                        WHERE i.id_matricula=m.id_matricula AND t.id_periodo=?))
                ORDER BY m.id_matricula FOR SHARE
                """,r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getObject(6,LocalDate.class),r.getObject(7,Long.class)},
                f.curso(),f.periodo(),f.periodo(),f.periodo());
        var matriculas=new ArrayList<List<String>>(); var inscricoes=new ArrayList<List<String>>(); var trajetorias=new ArrayList<List<String>>();
        for(var m:populacao) {
            long id=(long)m[0];
            matriculas.add(linha(id,m[1],m[2],m[3],m[4],m[5],m[6]));
            if(((String)m[2]).isBlank()) problemas.add("Matrícula "+id+": identificação de aluno ausente.");
            if(!Set.of("ATIVO","TRANCADO","CANCELADO","CONCLUIDO","TRANSFERIDO").contains(m[3])) problemas.add("Matrícula "+id+": situação institucional desconhecida.");
            var inscritas=q(c,"""
                    SELECT i.id_inscricao,t.id_turma,d.nome,i.situacao,r.situacao,r.media_inicial,r.media_final,
                        r.total_minutos,r.presentes_minutos FROM inscricao_disciplina i
                    JOIN turma t ON t.id_turma=i.id_turma JOIN disciplina d ON d.id_disciplina=t.id_disciplina
                    LEFT JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao
                    WHERE i.id_matricula=? AND t.id_periodo=? ORDER BY i.id_inscricao FOR SHARE
                    """,r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getObject(8,Long.class),r.getObject(9,Long.class)},id,f.periodo());
            for(var i:inscritas) {
                inscricoes.add(linha(id,i[0],i[1],i[2],i[3],i[4]==null?"SEM_RESULTADO":i[4],i[5],i[6],i[7],i[8]));
                boolean finalizada=Set.of("APROVADO","REPROVADO").contains(i[3]);
                if(!Set.of("ATIVO","CANCELADO","APROVADO","REPROVADO").contains(i[3]) ||
                        finalizada && !Objects.equals(i[3],i[4]) || !finalizada && i[4]!=null)
                    problemas.add("Inscrição "+i[0]+": situação e resultado formal incoerentes.");
            }
            if(f.tipo()==Tipo.ENADE_DEMONSTRATIVO) try {
                if(!q(c,"SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND situacao='DEFERIDO' AND (conclusao_origem>? OR decidido_em IS NULL OR decidido_em>CURRENT_TIMESTAMP(6)) FOR SHARE",
                        r->r.getLong(1),id,dataAcademica(agoraUtc(c))).isEmpty()) throw new IllegalArgumentException("Reconhecimento sem datas válidas.");
                var base=new RepositorioEquivalenciaJdbc(c).plano(id);
                if(base.itens().isEmpty()) throw new IllegalArgumentException("Estrutura sem componentes.");
                long cumpridos=base.itens().stream().filter(i->Set.of("CONCLUIDA","RECONHECIDA").contains(i.situacao())).count();
                trajetorias.add(linha(id,m[2],base.estrutura(),base.itens().size(),cumpridos,
                        base.itens().stream().filter(i->"RECONHECIDA".equals(i.situacao())).count(),
                        base.itens().stream().filter(i->"SOB_REVISAO".equals(i.situacao())).count(),base.itens().size()-cumpridos,"SEM_CLASSIFICACAO_OFICIAL"));
            } catch(IllegalArgumentException e) {
                problemas.add("Matrícula "+id+": "+e.getMessage());
                trajetorias.add(linha(id,m[2],m[6],"DADOS_INSUFICIENTES","","","","","SEM_CLASSIFICACAO_OFICIAL"));
            }
        }
        if(populacao.isEmpty()) avisos.add("Nenhum registro integra a população dos filtros selecionados.");
        var tabelas=new ArrayList<Tabela>();
        tabelas.add(new Tabela("Vínculos na população",List.of("Matrícula","Aluno","Identificação","Situação do vínculo atual","Situação do cadastro atual","Ingresso","Estrutura"),matriculas));
        tabelas.add(new Tabela("Inscrições e resultados registrados",List.of("Matrícula","Inscrição","Turma","Disciplina","Situação da inscrição","Resultado formal","Média inicial","Média final","Minutos totais","Minutos presentes"),inscricoes));
        if(f.tipo()==Tipo.ENADE_DEMONSTRATIVO) tabelas.add(new Tabela("Trajetória curricular demonstrativa",List.of("Matrícula","Identificação","Estrutura","Componentes","Cumpridos","Reconhecidos","Sob revisão","Não cumpridos","Classificação"),trajetorias));
        return new Documento(f.tipo()==Tipo.CENSO_DEMONSTRATIVO?"Censo - demonstrativo acadêmico":"ENADE - demonstrativo acadêmico",parametros,avisos,tabelas);
    }
    private Documento diario(Filtro f) throws SQLException {
        List<DiarioAcademico.Aula> aulas; List<DiarioAcademico.Avaliacao> avaliacoes; List<GestaoAcademica.AlunoDiario> alunos;
        if(f.tipo()==Tipo.DIARIO_GESTAO) {
            var d=new RepositorioGestaoAcademicaJdbc(c,sessao).diario(f.curso(),f.turma()); aulas=d.aulas(); avaliacoes=d.avaliacoes(); alunos=d.alunos();
        } else {
            var repo=new RepositorioDiarioAcademicoJdbc(c,sessao); aulas=repo.aulas(f.turma()); avaliacoes=repo.avaliacoes(f.turma());
            alunos=q(c,"""
                    SELECT i.id_inscricao,i.id_matricula,i.situacao,a.identificacao FROM inscricao_disciplina i
                    JOIN matricula_academica m ON m.id_matricula=i.id_matricula JOIN aluno a ON a.id_aluno=m.id_aluno
                    WHERE i.id_turma=? ORDER BY i.id_inscricao FOR SHARE
                    """,r->new GestaoAcademica.AlunoDiario(r.getLong(2),r.getLong(1),r.getString(4),r.getString(3),
                        repo.consultarDocente(r.getLong(1)),resultado(r.getLong(1))),f.turma());
        }
        var meta=q(c,"""
                SELECT t.codigo,d.nome,d.carga_horaria,p.descricao,p.data_inicio,p.data_fim,pr.identificacao
                FROM turma t JOIN disciplina d ON d.id_disciplina=t.id_disciplina JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                JOIN professor pr ON pr.id_professor=t.id_professor WHERE t.id_turma=? FOR SHARE
                """,r->new Object[]{r.getString(1),r.getString(2),r.getInt(3),r.getString(4),r.getObject(5,LocalDate.class),r.getObject(6,LocalDate.class),r.getString(7)},f.turma()).getFirst();
        var parametros=List.of(new Parametro("Turma",f.turma()+" - "+meta[0]),new Parametro("Disciplina",s(meta[1])),
                new Parametro("Carga horária",meta[2]+" horas de 60 minutos"),new Parametro("Período",meta[3]+"; "+meta[4]+" a "+meta[5]),
                new Parametro("Professor",s(meta[6])),new Parametro("Alcance",f.curso()==null?"Turma sob responsabilidade docente":"Alunos do curso "+f.curso()+"; turma compartilhada limitada ao curso"));
        var notas=new ArrayList<List<String>>(); var chamadas=new ArrayList<List<String>>(); var resumo=new ArrayList<List<String>>(); var vigentes=new ArrayList<List<String>>();
        for(var a:alunos) {
            var b=a.lancamentos();
            resumo.add(linha(a.matricula(),a.inscricao(),a.identificacao(),a.situacao(),b.minutosPrevistos(),b.minutosRegistrados(),b.minutosConsiderados(),b.minutosPresentes(),String.join("; ",b.pendencias()),b.encerrado()?"SIM":"NAO"));
            for(var n:b.notas()) notas.add(linha(a.inscricao(),n.avaliacao().id(),n.avaliacao().tipo(),n.id(),n.valor()==null?"SEM_REGISTRO":n.valor(),n.versao(),n.autor()));
            for(var ch:b.frequencias()) chamadas.add(linha(a.inscricao(),ch.aula().id(),ch.aula().data(),ch.aula().tempo(),ch.situacao()==null?"SEM_REGISTRO":ch.situacao(),ch.origem(),ch.considerada()?"SIM":"NAO",ch.versao(),ch.autor()));
            var v=a.resultado();
            if(v!=null) vigentes.add(linha(a.inscricao(),v.p1(),v.p2(),v.pf(),v.mediaInicial(),v.mediaFinal(),v.total(),v.presentes(),v.situacao(),v.motivo()));
        }
        return new Documento("Diário de classe - registros acadêmicos",parametros,
                List.of("Consulta/exportação dos registros existentes; sem assinatura oficial.","Lançamentos originais e resultados vigentes são apresentados separadamente. Ausência de lançamento não equivale a zero ou falta.","Tempos anteriores à inscrição ficam fora da frequência individual; a carga integral continua identificada."),
                List.of(new Tabela("Aulas, pauta e conteúdo registrado",List.of("Aula","Data","Tempo","Pauta","Conteúdo"),aulas.stream().map(a->linha(a.id(),a.data(),a.tempo(),a.pauta(),a.conteudo())).toList()),
                    new Tabela("Avaliações",List.of("Avaliação","Tipo","Data","Descrição"),avaliacoes.stream().map(a->linha(a.id(),a.tipo(),a.data(),a.descricao())).toList()),
                    new Tabela("Resumo por inscrição",List.of("Matrícula","Inscrição","Identificação","Situação","Minutos previstos","Registrados","Considerados","Presentes","Pendências","Encerrado"),resumo),
                    new Tabela("Notas originais",List.of("Inscrição","Avaliação","Tipo","Lançamento","Nota","Versão","Autor"),notas),
                    new Tabela("Frequência original",List.of("Inscrição","Aula","Data","Tempo","Situação","Origem","Considerada","Versão","Autor"),chamadas),
                    new Tabela("Resultados vigentes",List.of("Inscrição","P1","P2","PF","Média inicial","Média final","Minutos totais","Presentes","Situação","Motivo"),vigentes)));
    }
    private GestaoAcademica.ResultadoVigente resultado(long insc) throws SQLException {
        return q(c,"SELECT * FROM resultado_inscricao WHERE id_inscricao=? FOR SHARE",r->new GestaoAcademica.ResultadoVigente(
                r.getBigDecimal("p1"),r.getBigDecimal("p2"),r.getBigDecimal("pf"),r.getBigDecimal("media_inicial"),r.getBigDecimal("media_final"),
                r.getLong("total_minutos"),r.getLong("presentes_minutos"),r.getString("situacao"),r.getString("motivo")),insc).stream().findFirst().orElse(null);
    }
    private Documento historico() {
        var h=new RepositorioAtendimentoAlunoJdbc(c,sessao).historico();
        return new Documento("Histórico escolar - consulta própria",List.of(new Parametro("Aluno",h.aluno()+" - "+h.identificacao())),
                List.of("Consulta dos dados registrados. Não substitui histórico oficial solicitado à secretaria.","Reconhecimentos sob revisão, revogados ou legados não são promovidos automaticamente a aprovação."),
                List.of(new Tabela("Vínculos institucionais",List.of("Matrícula","Curso","Situação","Ingresso"),h.matriculas().stream().map(m->linha(m.matricula(),m.curso(),m.situacao(),m.ingresso())).toList()),
                    new Tabela("Disciplinas e resultados",List.of("Matrícula","Curso","Período","Disciplina","Nome","Carga horária","Inscrição","Situação da inscrição","Resultado formal","Coerente","Média inicial","Média final","Minutos totais","Presentes"),
                        h.disciplinas().stream().map(d->linha(d.matricula(),d.curso(),d.descricaoPeriodo(),d.disciplina(),d.nome(),d.cargaHoraria(),d.inscricao(),d.situacaoInscricao(),d.situacaoResultado(),d.resultadoCoerente()?"SIM":"NAO",d.mediaInicial(),d.mediaFinal(),d.minutosTotais(),d.minutosPresentes())).toList()),
                    new Tabela("Reconhecimentos formais",List.of("Matrícula","Pedido","Disciplina","Nome","Carga horária","Origem","Conclusão","Decisão","Revisão","Vigente"),
                        h.reconhecimentos().stream().map(r->linha(r.matricula(),r.pedido(),r.disciplina(),r.nome(),r.cargaHoraria(),r.instituicao(),r.conclusao(),r.decisao(),r.revisao(),r.vigente()?"SIM":"NAO")).toList()),
                    new Tabela("Legado preservado",List.of("Matrícula","Referência","Disciplina","Decisão registrada"),h.legados().stream().map(l->linha(l.matricula(),l.referencia(),l.disciplina(),l.decisao())).toList())));
    }
    private Documento plano(Filtro f,List<String> problemas) {
        var atual=new RepositorioPlanoInstitucionalJdbc(c,sessao).atualAluno(f.matricula());
        if(atual.isEmpty()) {
            problemas.add("Não há plano institucional registrado. A consulta não gera plano automaticamente.");
            return new Documento("Plano de estudo - consulta própria",List.of(new Parametro("Matrícula",s(f.matricula()))),List.of("SEM_PLANO"),List.of());
        }
        var consulta=atual.orElseThrow(); var v=consulta.versao();
        if(consulta.atualidade()==PlanoInstitucional.Atualidade.INCONSISTENTE) problemas.add(consulta.aviso());
        return new Documento("Plano de estudo - versão registrada",List.of(new Parametro("Matrícula",s(v.matricula())),new Parametro("Estrutura",s(v.estrutura())),
                new Parametro("Versão",s(v.numero())),new Parametro("Autoria e UTC",v.autor()+"; "+v.data()),new Parametro("Atualidade",consulta.atualidade().name())),
                List.of(consulta.aviso(),"Sugestões não reservam vagas nem confirmam inscrição; consulta não atualiza o plano."),
                List.of(new Tabela("Componentes do plano preservado",List.of("Disciplina","Código","Nome","Horas","Cadastro","Situação","Fundamento","Pré-requisitos","Turma sugerida","Período sugerido","Diagnóstico"),
                    v.componentes().stream().map(i->linha(i.disciplina(),i.codigo(),i.nome(),i.horas(),i.cadastro(),i.situacao(),i.evidencia(),i.requisitos(),i.turma(),i.periodo(),i.diagnostico())).toList())));
    }
    private static List<String> linha(Object... valores) { return Arrays.stream(valores).map(FontesRelatorioAcademicoJdbc::s).toList(); }
    private static String s(Object valor) { return valor==null?"":valor instanceof BigDecimal b?b.toPlainString():valor.toString(); }
}

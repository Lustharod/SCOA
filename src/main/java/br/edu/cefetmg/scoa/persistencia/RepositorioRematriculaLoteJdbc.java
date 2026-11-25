package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RematriculaLote.*;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.Atualidade;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Uma prévia confirmada, ocorrências por aluno e nenhum preenchimento automático de turmas. */
public final class RepositorioRematriculaLoteJdbc implements RepositorioRematriculaLote {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioRematriculaLoteJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    private record Calendario(LocalDate inicioOrigem,LocalDate fimOrigem,LocalDate inicioDestino,LocalDate fimDestino) { }
    private record MatriculaLote(long id,long aluno,String nome,String situacao,Long ingresso,LocalDate dataIngresso) { }
    @Override public Previa preparar(Selecao selecao) {
        Objects.requireNonNull(selecao);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->prepararInterno(selecao)));
    }
    private Calendario calendario(Selecao s) throws SQLException {
        if(q(c,"SELECT id_curso FROM curso WHERE id_curso=? AND situacao='ATIVO' FOR SHARE",r->r.getLong(1),s.curso()).isEmpty())
            throw new IllegalArgumentException("Selecione um curso ativo.");
        var datas=q(c,"""
                SELECT o.data_inicio,o.data_fim,d.data_inicio,d.data_fim FROM periodo_letivo o
                JOIN periodo_letivo d ON d.id_periodo=? AND d.situacao='ATIVO' WHERE o.id_periodo=? FOR SHARE
                """,r->new Calendario(r.getObject(1,LocalDate.class),r.getObject(2,LocalDate.class),r.getObject(3,LocalDate.class),r.getObject(4,LocalDate.class)),s.destino(),s.origem());
        var hoje=dataAcademica(agoraUtc(c));
        if(datas.isEmpty() || !datas.getFirst().fimOrigem().isBefore(hoje) || !datas.getFirst().inicioDestino().isAfter(datas.getFirst().fimOrigem()) || datas.getFirst().fimDestino().isBefore(hoje))
            throw new IllegalArgumentException("O lote exige origem encerrada e destino ativo posterior, ainda não encerrado.");
        return datas.getFirst();
    }
    private Previa prepararInterno(Selecao selecao) throws SQLException {
        var calendario=calendario(selecao);
        var curso=q(c,"SELECT m.*,a.identificacao FROM matricula_academica m JOIN aluno a ON a.id_aluno=m.id_aluno WHERE m.id_curso=? ORDER BY m.id_matricula FOR SHARE",
                r->new MatriculaLote(r.getLong("id_matricula"),r.getLong("id_aluno"),r.getString("identificacao"),r.getString("situacao"),
                        r.getObject("id_periodo_ingresso",Long.class),r.getObject("data_ingresso",LocalDate.class)),selecao.curso());
        var ids=new HashSet<Long>(); curso.forEach(m->ids.add(m.id()));
        if(!ids.containsAll(selecao.matriculas())) throw new IllegalArgumentException("Há matrícula não encontrada no curso selecionado.");
        var escolhidos=curso.stream().filter(m->selecao.matriculas().isEmpty()?"ATIVO".equals(m.situacao()):selecao.matriculas().contains(m.id())).toList();
        if(escolhidos.isEmpty()) throw new IllegalArgumentException("Nenhuma matrícula selecionada para esse curso.");
        var candidatos=new ArrayList<Candidato>();
        for(var m:escolhidos) candidatos.add(avaliar(selecao,m,calendario));
        return new Previa(selecao,UUID.randomUUID().toString(),hash(selecao+"\n"+calendario+"\n"+candidatos),candidatos);
    }
    private Candidato avaliar(Selecao s,MatriculaLote m,Calendario calendario) throws SQLException {
        Long plano=null;
        try {
            if(!"ATIVO".equals(m.situacao())) throw new IllegalArgumentException("Vínculo institucional não está ativo.");
            if(!q(c,"SELECT id_periodo FROM rematricula_academica WHERE id_matricula=? AND id_periodo=? FOR SHARE",r->r.getLong(1),m.id(),s.destino()).isEmpty())
                return new Candidato(m.id(),m.nome(),null,"JA_RENOVADA","Renovação desse período já registrada; não será duplicada.",List.of());
            if(m.ingresso()==null || m.dataIngresso().isAfter(calendario.fimOrigem()))
                throw new IllegalArgumentException("Ingresso não definido para o período de origem; encaminhar para análise manual.");
            var ingresso=q(c,"SELECT data_inicio FROM periodo_letivo WHERE id_periodo=? FOR SHARE",r->r.getObject(1,LocalDate.class),m.ingresso());
            if(ingresso.isEmpty() || ingresso.getFirst().isAfter(calendario.inicioOrigem()))
                throw new IllegalArgumentException("A matrícula não pertence à população do período de origem.");
            var resultados=q(c,"""
                    SELECT i.situacao,r.situacao FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma
                    LEFT JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao
                    WHERE i.id_matricula=? AND t.id_periodo=? FOR SHARE
                    """,r->new String[]{r.getString(1),r.getString(2)},m.id(),s.origem());
            if(resultados.isEmpty() || resultados.stream().anyMatch(r->"CANCELADO".equals(r[0])?r[1]!=null:
                    !Set.of("APROVADO","REPROVADO").contains(r[0]) || !r[0].equals(r[1])))
                throw new IllegalArgumentException("Resultados do período anterior ausentes, pendentes ou divergentes.");
            if(!q(c,"""
                    SELECT r.id_pedido FROM retificacao_resultado r JOIN inscricao_disciplina i ON i.id_inscricao=r.id_inscricao
                    JOIN turma t ON t.id_turma=i.id_turma WHERE i.id_matricula=? AND t.id_periodo=? AND r.situacao='PENDENTE' FOR SHARE
                    """,r->r.getLong(1),m.id(),s.origem()).isEmpty())
                throw new IllegalArgumentException("Há retificação pendente no período anterior.");
            var consulta=new RepositorioPlanoInstitucionalJdbc(c,sessao).atualSecretaria(m.id())
                    .orElseThrow(()->new IllegalArgumentException("Plano institucional não registrado."));
            plano=consulta.versao().id();
            if(consulta.atualidade()!=Atualidade.ATUAL || !consulta.versao().periodos().contains(s.destino()))
                throw new IllegalArgumentException("Plano pendente/inconsistente ou sem o período de destino. Atualize-o antes do lote.");
            new RepositorioMatriculaJdbc(c).consultarBiblioteca(m.aluno()).exigirRegular();
            var propostas=new ArrayList<Proposta>();
            for(var componente:consulta.versao().componentes()) {
                if(Set.of("CONCLUIDA","RECONHECIDA").contains(componente.situacao())) continue;
                Long turma=componente.periodo()!=null && componente.periodo()==s.destino()?componente.turma():null;
                if(turma==null) propostas.add(new Proposta(componente.disciplina(),null,"SEM_SUGESTAO",componente.diagnostico()));
                else {
                    var elegibilidade=new RepositorioInscricaoJdbc(c).avaliar(m.id(),turma);
                    propostas.add(new Proposta(componente.disciplina(),turma,elegibilidade.permite()?"DISPONIVEL":"RECUSADA",
                            elegibilidade.permite()?"Depende de confirmação do aluno e revalidação; não reserva vaga.":String.join(" ",elegibilidade.motivos())));
                }
            }
            if(propostas.stream().noneMatch(p->"DISPONIVEL".equals(p.situacao())))
                return new Candidato(m.id(),m.nome(),plano,"MANUAL","Nenhuma turma sugerida disponível. Toda a rematrícula exige análise manual; vínculo não será renovado automaticamente.",propostas);
            return new Candidato(m.id(),m.nome(),plano,"ELEGIVEL","Apto à renovação institucional; propostas exigem confirmação separada.",propostas);
        } catch(IllegalArgumentException e) {
            return new Candidato(m.id(),m.nome(),plano,"MANUAL",e.getMessage(),List.of());
        }
    }
    @Override public Resultado executar(Pedido pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manter("LOTE_REMATRICULA_CURSO","PROCESSAR_LOTE",pedido.selecao().curso(),()->TransacaoJdbc.executar(c,()->{
            var existentes=q(c,"SELECT id_lote FROM lote_rematricula WHERE chave=? FOR UPDATE",r->r.getLong(1),pedido.chave());
            if(!existentes.isEmpty()) {
                var mesmo=q(c,"""
                        SELECT id_lote FROM lote_rematricula WHERE id_lote=? AND id_curso=? AND id_periodo_origem=?
                        AND id_periodo_destino=? AND selecao=CAST(? AS JSON) AND assinatura=? AND motivo=? FOR SHARE
                        """,r->r.getLong(1),existentes.getFirst(),pedido.selecao().curso(),pedido.selecao().origem(),pedido.selecao().destino(),
                        pedido.selecao().matriculas().toString(),pedido.assinatura(),pedido.motivo());
                if(mesmo.isEmpty()) throw new IllegalArgumentException("A chave já identifica outro processamento. Consulte o lote original.");
                return carregar(existentes.getFirst());
            }
            var previa=prepararInterno(pedido.selecao());
            if(!previa.assinatura().equals(pedido.assinatura())) throw new IllegalArgumentException("Dados do lote mudaram. Confira uma nova prévia antes de processar.");
            long autor=SegurancaJdbc.exigirSessao(c,sessao);
            long id=g(c,"""
                    INSERT INTO lote_rematricula(chave,id_curso,id_periodo_origem,id_periodo_destino,selecao,assinatura,
                        id_usuario_autor,registrado_em_utc,motivo) VALUES (?,?,?,?,CAST(? AS JSON),?,?,?,?)
                    """,pedido.chave(),pedido.selecao().curso(),pedido.selecao().origem(),pedido.selecao().destino(),pedido.selecao().matriculas().toString(),
                    pedido.assinatura(),autor,agoraUtc(c),pedido.motivo());
            for(var candidato:previa.candidatos()) {
                Candidato resultado=candidato;
                if("ELEGIVEL".equals(candidato.situacao())) {
                    try {
                        new RepositorioMatriculaJdbc(c).rematricularPelaSecretaria(sessao,candidato.matricula(),pedido.selecao().destino());
                        resultado=new Candidato(candidato.matricula(),candidato.identificacao(),candidato.plano(),"RENOVADA","Vínculo renovado. Inscrição nas turmas depende de confirmação separada.",candidato.propostas());
                    } catch(IllegalArgumentException e) {
                        resultado=new Candidato(candidato.matricula(),candidato.identificacao(),candidato.plano(),"MANUAL",e.getMessage(),candidato.propostas());
                    }
                }
                g(c,"INSERT INTO item_lote_rematricula VALUES (?,?,?,?,?,?)",id,resultado.matricula(),resultado.identificacao(),resultado.plano(),resultado.situacao(),resultado.mensagem());
                for(var p:resultado.propostas()) g(c,"INSERT INTO proposta_lote_rematricula VALUES (?,?,?,?,?,?)",id,resultado.matricula(),p.disciplina(),p.turma(),p.situacao(),p.mensagem());
            }
            evento(c,"LOTE_REMATRICULA",id,autor,"PROCESSAR",pedido.motivo()+"; candidatos "+previa.candidatos().size()+"; sem inscrições automáticas.");
            return carregar(id);
        }));
    }
    @Override public Resultado consultar(long lote) {
        HorarioAula.validarId(lote); return acesso.consultar(()->TransacaoJdbc.executar(c,()->carregar(lote)));
    }
    private Resultado carregar(long id) throws SQLException {
        var header=q(c,"SELECT * FROM lote_rematricula WHERE id_lote=? FOR SHARE",r->new Object[]{r.getString("chave"),r.getLong("id_curso"),r.getLong("id_periodo_origem"),
                r.getLong("id_periodo_destino"),r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class)},id);
        if(header.isEmpty()) throw new IllegalArgumentException("Lote não encontrado.");
        var candidatos=q(c,"SELECT * FROM item_lote_rematricula WHERE id_lote=? ORDER BY id_matricula FOR SHARE",r->{
            long matricula=r.getLong("id_matricula");
            var propostas=q(c,"SELECT * FROM proposta_lote_rematricula WHERE id_lote=? AND id_matricula=? ORDER BY id_disciplina FOR SHARE",
                    p->new Proposta(p.getLong("id_disciplina"),p.getObject("id_turma",Long.class),p.getString("situacao"),p.getString("mensagem")),id,matricula);
            return new Candidato(matricula,r.getString("identificacao_snapshot"),r.getObject("id_plano",Long.class),r.getString("situacao"),r.getString("mensagem"),propostas);
        },id);
        var h=header.getFirst(); return new Resultado(id,(String)h[0],(long)h[1],(long)h[2],(long)h[3],(long)h[4],(LocalDateTime)h[5],candidatos);
    }
}

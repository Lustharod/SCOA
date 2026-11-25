package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Diário por tempo oficial. O professor é obtido da sessão, nunca de uma permissão ADMIN. */
public final class RepositorioDiarioAcademicoJdbc implements RepositorioDiarioAcademico {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    public RepositorioDiarioAcademicoJdbc(Connection c,SegurancaJdbc.Sessao sessao) { this.c=Objects.requireNonNull(c); this.sessao=sessao; }
    private record Contexto(long autor,int horas,LocalDate inicio,LocalDate fim,String situacao) { }
    private Contexto professor(long turma) throws SQLException {
        HorarioAula.validarId(turma); long autor=SegurancaJdbc.exigirSessao(c,sessao);
        var lista=q(c,"""
                SELECT d.carga_horaria,p.data_inicio,p.data_fim,t.situacao FROM turma t
                JOIN professor pr ON pr.id_professor=t.id_professor JOIN disciplina d ON d.id_disciplina=t.id_disciplina
                JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                WHERE t.id_turma=? AND pr.id_usuario=? AND pr.situacao='ATIVO' FOR SHARE
                """,r->new Contexto(autor,r.getInt(1),r.getObject(2,LocalDate.class),r.getObject(3,LocalDate.class),r.getString(4)),turma,autor);
        if(lista.isEmpty()) throw new IllegalArgumentException("O diário é restrito ao professor responsável pela turma.");
        return lista.getFirst();
    }
    private LocalDate hoje() throws SQLException { return dataAcademica(agoraUtc(c)); }
    private void data(Contexto ctx,LocalDate data,boolean realizada) throws SQLException {
        if(!"ATIVO".equals(ctx.situacao()) || data.isBefore(ctx.inicio()) || data.isAfter(ctx.fim()))
            throw new IllegalArgumentException("Use uma turma ativa e uma data dentro de seu período.");
        if(realizada && data.isAfter(hoje())) throw new IllegalArgumentException("Não registre aula ou lançamento realizado em data futura.");
    }
    private RepositorioInscricaoJdbc.ContextoInscricao inscricaoAberta(long id,long turma) throws SQLException {
        var ctx=new RepositorioInscricaoJdbc(c).bloquearInscricao(id);
        if(ctx.inscricao().turma()!=turma) throw new IllegalArgumentException("A inscrição deve pertencer à turma da aula/avaliação.");
        if(!"ATIVO".equals(ctx.inscricao().situacao()) || encerrado(id))
            throw new IllegalArgumentException("Inscrição encerrada ou cancelada. Resultado final só muda pelo processo de retificação.");
        return ctx;
    }
    private boolean encerrado(long inscricao) throws SQLException {
        return !q(c,"SELECT id_inscricao FROM resultado_inscricao WHERE id_inscricao=? FOR SHARE",r->r.getLong(1),inscricao).isEmpty();
    }
    @Override public List<Aula> registrarAulas(NovaAula pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); var ctx=professor(pedido.turma()); data(ctx,pedido.data(),true);
            DiarioAcademico.temposDaCarga(ctx.horas());
            if(!q(c,"SELECT i.id_inscricao FROM inscricao_disciplina i JOIN resultado_inscricao r ON r.id_inscricao=i.id_inscricao WHERE i.id_turma=? FOR SHARE",
                    r->r.getLong(1),pedido.turma()).isEmpty())
                throw new IllegalArgumentException("A turma já tem resultado final. Novas aulas exigem análise de impacto.");
            var horarios=q(c,"SELECT hora_inicio,hora_fim FROM horario_aula WHERE id_turma=? AND ativo=TRUE AND dia_semana=? FOR SHARE",
                    r->new LocalTime[]{r.getObject(1,LocalTime.class),r.getObject(2,LocalTime.class)},pedido.turma(),pedido.data().getDayOfWeek().getValue());
            var criadas=new ArrayList<Aula>();
            for(var tempo:pedido.tempos()) {
                if(horarios.stream().noneMatch(h->!tempo.inicio().isBefore(h[0]) && !tempo.fim().isAfter(h[1])))
                    throw new IllegalArgumentException("O tempo "+tempo+" não está coberto pelo horário da turma nessa data.");
                if(!q(c,"SELECT id_aula FROM aula WHERE id_turma=? AND data=? AND tempo_letivo=? FOR SHARE",r->r.getLong(1),pedido.turma(),pedido.data(),tempo.name()).isEmpty())
                    throw new IllegalArgumentException("Tempo já registrado; consulte o diário.");
                long id=g(c,"INSERT INTO aula(id_turma,data,pauta,conteudo,tempo_letivo,id_usuario_autor,registrado_em_utc) VALUES (?,?,?,?,?,?,?)",
                        pedido.turma(),pedido.data(),pedido.pauta(),pedido.conteudo(),tempo.name(),ctx.autor(),agoraUtc(c));
                registrar("AULA",id,ctx.autor(),"REGISTRAR","Tempo "+tempo+"; turma "+pedido.turma()+"; data "+pedido.data());
                criadas.add(new Aula(id,pedido.turma(),pedido.data(),tempo.name(),pedido.pauta(),pedido.conteudo()));
            }
            return List.copyOf(criadas);
        });
    }
    @Override public List<Aula> aulas(long turma) {
        return TransacaoJdbc.executar(c,()->{ controle(c); professor(turma); return aulasDaTurma(turma); });
    }
    private List<Aula> aulasDaTurma(long turma) throws SQLException {
        return q(c,"SELECT * FROM aula WHERE id_turma=? ORDER BY data,tempo_letivo,id_aula FOR SHARE",RepositorioDiarioAcademicoJdbc::lerAula,turma);
    }
    @Override public Avaliacao registrarAvaliacao(NovaAvaliacao pedido) {
        Objects.requireNonNull(pedido);
        return TransacaoJdbc.executar(c,()->{
            controle(c); var ctx=professor(pedido.turma()); data(ctx,pedido.data(),false);
            if(!q(c,"SELECT id_avaliacao FROM avaliacao WHERE id_turma=? AND tipo_prova=? FOR SHARE",r->r.getLong(1),pedido.turma(),pedido.tipo().name()).isEmpty())
                throw new IllegalArgumentException("Essa prova já está cadastrada para a turma.");
            long id=g(c,"INSERT INTO avaliacao(id_turma,identificacao,data,descricao,tipo_prova,id_usuario_autor,registrado_em_utc) VALUES (?,?,?,?,?,?,?)",
                    pedido.turma(),pedido.tipo().name(),pedido.data(),pedido.descricao(),pedido.tipo().name(),ctx.autor(),agoraUtc(c));
            registrar("AVALIACAO",id,ctx.autor(),"REGISTRAR",pedido.tipo()+"; turma "+pedido.turma());
            return new Avaliacao(id,pedido.turma(),pedido.tipo().name(),pedido.data(),pedido.descricao());
        });
    }
    @Override public List<Avaliacao> avaliacoes(long turma) {
        return TransacaoJdbc.executar(c,()->{ controle(c); professor(turma);
            return q(c,"SELECT * FROM avaliacao WHERE id_turma=? ORDER BY data,id_avaliacao FOR SHARE",RepositorioDiarioAcademicoJdbc::lerAvaliacao,turma);
        });
    }
    @Override public void lancarNota(LancarNota pedido) {
        Objects.requireNonNull(pedido);
        TransacaoJdbc.executar(c,()->{
            controle(c); var a=avaliacao(pedido.avaliacao()); var ctx=professor(a.turma()); data(ctx,a.data(),true);
            inscricaoAberta(pedido.inscricao(),a.turma());
            if(!Set.of("P1","P2","PF").contains(Objects.toString(a.tipo(),""))) throw new IllegalArgumentException("Avaliação legada sem tipo formal; confira antes de lançar.");
            var anteriores=q(c,"SELECT id_nota,valor,versao,id_usuario_autor FROM nota WHERE id_avaliacao=? AND id_inscricao=? FOR UPDATE",
                    r->new Object[]{r.getLong(1),r.getBigDecimal(2),r.getInt(3),r.getObject(4,Long.class)},a.id(),pedido.inscricao());
            long id;
            if(anteriores.isEmpty()) {
                versao(pedido.versao(),-1);
                id=g(c,"INSERT INTO nota(id_avaliacao,id_inscricao,valor,id_usuario_autor,atualizado_em_utc) VALUES (?,?,?,?,?)",
                        a.id(),pedido.inscricao(),pedido.valor(),ctx.autor(),agoraUtc(c));
            } else {
                var anterior=anteriores.getFirst(); if(anterior[3]==null) throw new IllegalArgumentException("Nota legada sem autoria; requer conferência específica.");
                versao(pedido.versao(),(int)anterior[2]); id=(long)anterior[0];
                g(c,"UPDATE nota SET valor=?,desconsiderada=FALSE,versao=versao+1,id_usuario_autor=?,atualizado_em_utc=? WHERE id_nota=?",pedido.valor(),ctx.autor(),agoraUtc(c),id);
            }
            registrar("NOTA",id,ctx.autor(),"LANCAR",pedido.motivo()+"; anterior="+(anteriores.isEmpty()?"ausente":anteriores.getFirst()[1])+"; novo="+pedido.valor()+"; inscrição="+pedido.inscricao());
            return null;
        });
    }
    @Override public void lancarFrequencia(LancarFrequencia pedido) {
        Objects.requireNonNull(pedido);
        TransacaoJdbc.executar(c,()->{ controle(c); frequencia(pedido,"MANUAL"); return null; });
    }
    @Override public void retirarNota(RetirarNota pedido) {
        Objects.requireNonNull(pedido);
        TransacaoJdbc.executar(c,()->{
            controle(c); var a=avaliacao(pedido.avaliacao()); var ctx=professor(a.turma());
            inscricaoAberta(pedido.inscricao(),a.turma());
            var notas=q(c,"SELECT id_nota,valor,versao,id_usuario_autor,desconsiderada FROM nota WHERE id_avaliacao=? AND id_inscricao=? FOR UPDATE",
                    r->new Object[]{r.getLong(1),r.getBigDecimal(2),r.getInt(3),r.getObject(4,Long.class),r.getBoolean(5)},a.id(),pedido.inscricao());
            if(notas.isEmpty() || notas.getFirst()[3]==null || (boolean)notas.getFirst()[4])
                throw new IllegalArgumentException("Não há nota vigente com autoria para retirar. Preserve o legado e confira os lançamentos.");
            var n=notas.getFirst(); versao(pedido.versao(),(int)n[2]);
            g(c,"UPDATE nota SET desconsiderada=TRUE,versao=versao+1,id_usuario_autor=?,atualizado_em_utc=? WHERE id_nota=?",ctx.autor(),agoraUtc(c),n[0]);
            registrar("NOTA",(long)n[0],ctx.autor(),"RETIRAR",pedido.motivo()+"; anterior="+n[1]+"; novo=SEM_NOTA; inscrição="+pedido.inscricao());
            return null;
        });
    }
    private void frequencia(LancarFrequencia pedido,String origem) throws SQLException {
        var a=aula(pedido.aula()); var ctx=professor(a.turma()); data(ctx,a.data(),true);
        tempo(a); inscricaoAberta(pedido.inscricao(),a.turma());
        var anteriores=q(c,"SELECT id_frequencia,situacao,versao,id_usuario_autor FROM registro_frequencia WHERE id_aula=? AND id_inscricao=? FOR UPDATE",
                r->new Object[]{r.getLong(1),r.getString(2),r.getInt(3),r.getObject(4,Long.class)},a.id(),pedido.inscricao());
        long id;
        if(anteriores.isEmpty()) {
            versao(pedido.versao(),-1);
            id=g(c,"INSERT INTO registro_frequencia(id_aula,id_inscricao,situacao,origem,id_usuario_autor,atualizado_em_utc) VALUES (?,?,?,?,?,?)",
                    a.id(),pedido.inscricao(),pedido.presenca().name(),origem,ctx.autor(),agoraUtc(c));
        } else {
            var anterior=anteriores.getFirst(); if(anterior[3]==null) throw new IllegalArgumentException("Frequência legada sem autoria; requer conferência específica.");
            versao(pedido.versao(),(int)anterior[2]); id=(long)anterior[0];
            g(c,"UPDATE registro_frequencia SET situacao=?,origem=?,versao=versao+1,id_usuario_autor=?,atualizado_em_utc=? WHERE id_frequencia=?",
                    pedido.presenca().name(),origem,ctx.autor(),agoraUtc(c),id);
        }
        registrar("FREQUENCIA",id,ctx.autor(),"LANCAR",pedido.motivo()+"; anterior="+(anteriores.isEmpty()?"ausente":anteriores.getFirst()[1])+"; novo="+pedido.presenca()+"; inscrição="+pedido.inscricao());
    }
    private static void versao(int esperada,int atual) {
        if(esperada!=atual) throw new IllegalArgumentException("Lançamento alterado. Consulte os valores atuais antes de reenviar.");
    }
    @Override public Boletim consultarDocente(long inscricao) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); var ctx=new RepositorioInscricaoJdbc(c).bloquearInscricao(inscricao); professor(ctx.inscricao().turma());
            return boletim(ctx.inscricao());
        });
    }
    @Override public Boletim consultarAluno(long inscricao) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigirSessao(c,sessao);
            var ctx=new RepositorioInscricaoJdbc(c).bloquearInscricao(inscricao); SegurancaJdbc.exigirAluno(c,autor,ctx.aluno());
            return boletim(ctx.inscricao());
        });
    }
    private Boletim boletim(Inscricao insc) throws SQLException {
        var notas=q(c,"""
                SELECT a.*,n.id_nota,CASE WHEN n.desconsiderada THEN NULL ELSE n.valor END AS valor,
                    n.versao,n.id_usuario_autor AS autor_nota FROM avaliacao a
                LEFT JOIN nota n ON n.id_avaliacao=a.id_avaliacao AND n.id_inscricao=?
                WHERE a.id_turma=? ORDER BY a.data,a.id_avaliacao FOR SHARE
                """,r->new Nota(lerAvaliacao(r),r.getObject("id_nota",Long.class),r.getBigDecimal("valor"),
                        r.getObject("versao",Integer.class),r.getObject("autor_nota",Long.class)),insc.id(),insc.turma());
        var frequencias=q(c,"""
                SELECT a.*,f.id_frequencia,f.situacao,f.origem,f.versao,f.id_usuario_autor AS autor_frequencia FROM aula a
                LEFT JOIN registro_frequencia f ON f.id_aula=a.id_aula AND f.id_inscricao=?
                WHERE a.id_turma=? ORDER BY a.data,a.tempo_letivo,a.id_aula FOR SHARE
                """,r->new Frequencia(lerAula(r),r.getObject("id_frequencia",Long.class),r.getString("situacao"),r.getString("origem"),
                        r.getObject("versao",Integer.class),r.getObject("autor_frequencia",Long.class),
                        !r.getObject("data",LocalDate.class).isBefore(insc.data())),insc.id(),insc.turma());
        int horas=q(c,"SELECT d.carga_horaria FROM turma t JOIN disciplina d ON d.id_disciplina=t.id_disciplina WHERE t.id_turma=? FOR SHARE",r->r.getInt(1),insc.turma()).getFirst();
        long previstos=DiarioAcademico.minutosDaCarga(horas),registrados=0,considerados=0,presentes=0;
        var pendencias=new ArrayList<String>();
        try { DiarioAcademico.temposDaCarga(horas); } catch(IllegalArgumentException e) { pendencias.add(e.getMessage()); }
        for(var f:frequencias) {
            try { tempo(f.aula()); registrados+=50; }
            catch(IllegalArgumentException e) { pendencias.add("Aula legada/tempo desconhecido: "+f.aula().id()); continue; }
            if(!f.considerada()) continue;
            considerados+=50;
            if(f.autor()==null || !Set.of("PRESENTE","FALTA").contains(Objects.toString(f.situacao(),"")))
                pendencias.add("Frequência pendente ou legada na aula "+f.aula().id());
            else if("PRESENTE".equals(f.situacao())) presentes+=50;
        }
        if(registrados!=previstos) pendencias.add("Carga prevista "+previstos+" min; aulas registradas "+registrados+" min.");
        if(considerados==0) pendencias.add("Nenhum tempo considerado a partir da data da inscrição.");
        var valores=new EnumMap<Prova,BigDecimal>(Prova.class);
        for(var n:notas) {
            if(n.avaliacao().tipo()==null) { pendencias.add("Avaliação legada sem tipo: "+n.avaliacao().id()); continue; }
            try {
                var tipo=Prova.valueOf(n.avaliacao().tipo());
                if(n.valor()!=null && n.autor()!=null) valores.put(tipo,RegraResultado.nota(n.valor()));
                else if(tipo!=Prova.PF || n.valor()!=null) pendencias.add("Nota "+tipo+" pendente ou legada.");
            } catch(IllegalArgumentException e) { pendencias.add("Avaliação/nota inválida: "+n.avaliacao().id()); }
        }
        if(!valores.containsKey(Prova.P1)) pendencias.add("P1 pendente.");
        if(!valores.containsKey(Prova.P2)) pendencias.add("P2 pendente.");
        RegraResultado.Resultado previa=null;
        if(pendencias.isEmpty()) {
            try {
                previa=RegraResultado.calcular(valores.get(Prova.P1),valores.get(Prova.P2),valores.get(Prova.PF),considerados,presentes,
                        ParametroPeriodoJdbc.porTurma(c,insc.turma()).regras());
                if("AGUARDA_PF".equals(previa.situacao())) pendencias.add("PF pendente.");
            } catch(IllegalArgumentException e) { pendencias.add(e.getMessage()); }
        }
        return new Boletim(insc.id(),insc.turma(),previstos,registrados,considerados,presentes,notas,frequencias,pendencias.stream().distinct().toList(),previa,encerrado(insc.id()));
    }
    Boletim consultarGestao(long curso,long inscricao) {
        return TransacaoJdbc.executar(c,()->{
            controle(c); new AcessoGestaoAcademicaJdbc(c,sessao).curso(curso);
            var ctx=new RepositorioInscricaoJdbc(c).bloquearInscricao(inscricao);
            if(ctx.curso()!=curso) throw new IllegalArgumentException("Inscrição fora do curso autorizado.");
            return boletim(ctx.inscricao());
        });
    }
    @Override public RegraResultado.Resultado fechar(long inscricao,String motivo) {
        String razao=Matricula.validarMotivo(motivo);
        return new AcessoAcademicoJdbc(c,sessao).fecharResultado(inscricao,()->TransacaoJdbc.executar(c,()->{
            var ctx=new RepositorioInscricaoJdbc(c).bloquearInscricao(inscricao); professor(ctx.inscricao().turma());
            var b=boletim(ctx.inscricao());
            if(!b.pendencias().isEmpty()) throw new IllegalArgumentException(String.join(" ",b.pendencias()));
            var valores=new EnumMap<Prova,BigDecimal>(Prova.class);
            for(var nota:b.notas()) if(nota.valor()!=null) valores.put(Prova.valueOf(nota.avaliacao().tipo()),nota.valor());
            return new RepositorioInscricaoJdbc(c).registrarResultado(inscricao,valores.get(Prova.P1),valores.get(Prova.P2),valores.get(Prova.PF),
                    b.minutosConsiderados(),b.minutosPresentes(),razao);
        }));
    }
    @Override public List<Processamento> importarSimulados(List<Chegada> eventos) {
        Objects.requireNonNull(eventos); var resultados=new ArrayList<Processamento>();
        for(var e:eventos) {
            Objects.requireNonNull(e);
            try { resultados.add(TransacaoJdbc.executar(c,()->importar(e))); }
            catch(IllegalArgumentException erro) { resultados.add(new Processamento(e.chave(),"RECUSADO",erro.getMessage())); }
        }
        return List.copyOf(resultados);
    }
    private Processamento importar(Chegada e) throws SQLException {
        controle(c); var aula=aula(e.aula()); var ctx=professor(aula.turma());
        var anteriores=q(c,"SELECT id_aula,id_inscricao,chegada_local FROM evento_acesso_simulado WHERE origem=? AND chave=? FOR UPDATE",
                r->new Object[]{r.getLong(1),r.getLong(2),r.getObject(3,LocalDateTime.class)},e.origem(),e.chave());
        if(!anteriores.isEmpty()) {
            var a=anteriores.getFirst();
            if((long)a[0]!=e.aula() || (long)a[1]!=e.inscricao() || !a[2].equals(e.instante()))
                throw new IllegalArgumentException("Chave de evento já usada com outros dados.");
            return new Processamento(e.chave(),"REPETIDO","Evento já processado; não acrescentou presença nem carga.");
        }
        if(!e.instante().toLocalDate().equals(aula.data())) throw new IllegalArgumentException("A chegada deve corresponder à data da aula.");
        var p=tempo(aula).presencaPorChegada(e.instante().toLocalTime(),ParametroPeriodoJdbc.porTurma(c,aula.turma()).regras().toleranciaAtraso());
        // O adaptador não sobrescreve frequência manual ou outro evento anteriormente aceito.
        frequencia(new LancarFrequencia(e.aula(),e.inscricao(),p,-1,"Evento simulado "+e.origem()+"/"+e.chave()),"SIMULADO");
        g(c,"INSERT INTO evento_acesso_simulado VALUES (?,?,?,?,?,?,?)",e.origem(),e.chave(),e.aula(),e.inscricao(),e.instante(),ctx.autor(),agoraUtc(c));
        return new Processamento(e.chave(),"PROCESSADO",p.name());
    }
    private void registrar(String entidade,long id,long autor,String acao,String descricao) throws SQLException {
        evento(c,"DIARIO_"+entidade,id,autor,acao,descricao);
        g(c,"INSERT INTO auditoria(id_usuario,modulo,entidade,id_registro,acao,data_hora,detalhes) VALUES (?,'ACADEMICO',?,?,?,?,?)",
                autor,entidade,id,acao,agoraUtc(c),"Operação do diário concluída pelo docente responsável.");
    }
    private Aula aula(long id) throws SQLException {
        HorarioAula.validarId(id);
        var lista=q(c,"SELECT * FROM aula WHERE id_aula=? FOR SHARE",RepositorioDiarioAcademicoJdbc::lerAula,id);
        if(lista.isEmpty()) throw new IllegalArgumentException("Aula não encontrada."); return lista.getFirst();
    }
    private Avaliacao avaliacao(long id) throws SQLException {
        HorarioAula.validarId(id);
        var lista=q(c,"SELECT * FROM avaliacao WHERE id_avaliacao=? FOR SHARE",RepositorioDiarioAcademicoJdbc::lerAvaliacao,id);
        if(lista.isEmpty()) throw new IllegalArgumentException("Avaliação não encontrada."); return lista.getFirst();
    }
    private static TempoLetivo tempo(Aula aula) {
        try { return TempoLetivo.valueOf(Objects.toString(aula.tempo(),"")); }
        catch(IllegalArgumentException e) { throw new IllegalArgumentException("Aula sem tempo oficial; preserve o legado e confira o diário."); }
    }
    private static Aula lerAula(ResultSet r) throws SQLException {
        return new Aula(r.getLong("id_aula"),r.getLong("id_turma"),r.getObject("data",LocalDate.class),r.getString("tempo_letivo"),r.getString("pauta"),r.getString("conteudo"));
    }
    private static Avaliacao lerAvaliacao(ResultSet r) throws SQLException {
        return new Avaliacao(r.getLong("id_avaliacao"),r.getLong("id_turma"),r.getString("tipo_prova"),r.getObject("data",LocalDate.class),r.getString("descricao"));
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Registra versões explícitas. Consultas nunca criam nem atualizam um plano. */
public final class RepositorioPlanoInstitucionalJdbc implements RepositorioPlanoInstitucional {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioPlanoInstitucionalJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    @Override public Previa preparar(Selecao selecao) {
        Objects.requireNonNull(selecao);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->prepararInterno(selecao)));
    }
    private Previa prepararInterno(Selecao selecao) throws SQLException {
        var simulacao=new RepositorioPlanejamentoJdbc(c).simular(selecao.matricula(),selecao.periodos(),selecao.prioridades());
        var base=new RepositorioEquivalenciaJdbc(c).plano(selecao.matricula());
        if(simulacao.itens().isEmpty()) throw new IllegalArgumentException("A estrutura precisa de componentes para gerar um plano consistente.");
        if(!q(c,"SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND situacao='DEFERIDO' AND conclusao_origem>? FOR SHARE",
                r->r.getLong(1),selecao.matricula(),dataAcademica(agoraUtc(c))).isEmpty())
            throw new IllegalArgumentException("Há reconhecimento com conclusão futura. Confira os dados antes de atualizar o plano.");
        var itens=new ArrayList<Componente>();
        for(var item:simulacao.itens()) {
            var d=item.disciplina();
            String codigo=q(c,"SELECT codigo FROM disciplina WHERE id_disciplina=? FOR SHARE",r->r.getString(1),d.id()).getFirst();
            var evidencia=base.itens().stream().filter(i->i.disciplina()==d.id()).findFirst().orElseThrow();
            var sugestao=item.sugestao();
            itens.add(new Componente(d.id(),codigo,d.nome(),d.horas(),d.cadastro(),d.situacao(),evidencia.evidencia(),d.requisitos(),
                    sugestao==null?null:sugestao.turma(),sugestao==null?null:sugestao.periodo().id(),
                    sugestao==null?null:sugestao.periodo().descricao(),sugestao==null?null:sugestao.periodo().inicio(),
                    sugestao==null?null:sugestao.periodo().fim(),item.motivo()+" Tentativas: "+item.tentativas()));
        }
        int anterior=q(c,"SELECT COALESCE(MAX(versao),0) FROM plano_institucional WHERE id_matricula=?",r->r.getInt(1),selecao.matricula()).getFirst();
        String assinatura=hash(base.toString()+"\n"+simulacao+"\n"+itens+"\n"+selecao);
        return new Previa(selecao,anterior,simulacao.estrutura(),assinatura,itens);
    }
    @Override public Versao registrar(Registro pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manter("PLANO_MATRICULA","REGISTRAR_VERSAO",pedido.selecao().matricula(),()->TransacaoJdbc.executar(c,()->{
            var atual=prepararInterno(pedido.selecao());
            if(atual.versaoAnterior()!=pedido.versaoAnterior() || !atual.assinatura().equals(pedido.assinatura()))
                throw new IllegalArgumentException("O plano ou seus dados mudaram. Confira uma nova prévia antes de registrar.");
            long autor=SegurancaJdbc.exigirSessao(c,sessao);
            var anteriores=q(c,"SELECT id_plano FROM plano_institucional WHERE id_matricula=? ORDER BY versao DESC LIMIT 1 FOR UPDATE",
                    r->r.getLong(1),pedido.selecao().matricula());
            Long anterior=anteriores.isEmpty()?null:anteriores.getFirst();
            long id=g(c,"""
                    INSERT INTO plano_institucional(id_matricula,id_estrutura,versao,id_anterior,id_usuario_autor,
                        registrado_em_utc,motivo,assinatura,periodos,prioridades)
                    VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON))
                    """,pedido.selecao().matricula(),atual.estrutura(),atual.versaoAnterior()+1,anterior,autor,
                    agoraUtc(c),pedido.motivo(),atual.assinatura(),pedido.selecao().periodos().toString(),pedido.selecao().prioridades().toString());
            int ordem=0;
            for(var i:atual.componentes()) g(c,"""
                    INSERT INTO componente_plano_institucional(id_plano,ordem,id_disciplina,codigo_snapshot,nome_snapshot,
                        horas_snapshot,cadastro_snapshot,situacao_snapshot,evidencia_snapshot,requisitos_snapshot,
                        id_turma_sugerida,id_periodo_sugerido,periodo_snapshot,inicio_snapshot,fim_snapshot,diagnostico)
                    VALUES (?,?,?,?,?,?,?,?,?,CAST(? AS JSON),?,?,?,?,?,?)
                    """,id,++ordem,i.disciplina(),i.codigo(),i.nome(),i.horas(),i.cadastro(),i.situacao(),i.evidencia(),i.requisitos().toString(),
                    i.turma(),i.periodo(),i.descricaoPeriodo(),i.inicio(),i.fim(),i.diagnostico());
            evento(c,"PLANO_INSTITUCIONAL",id,autor,"REGISTRAR_VERSAO",pedido.motivo()+"; versão "+(atual.versaoAnterior()+1)+"; anterior "+anterior);
            return carregar(id);
        }));
    }
    @Override public Optional<Consulta> atualSecretaria(long matricula) {
        HorarioAula.validarId(matricula);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->atual(matricula)));
    }
    @Override public Optional<Consulta> atualAluno(long matricula) {
        HorarioAula.validarId(matricula);
        return TransacaoJdbc.executar(c,()->{ controle(c); exigirPropriedade(matricula); return atual(matricula); });
    }
    @Override public List<Versao> historicoSecretaria(long matricula) {
        HorarioAula.validarId(matricula);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->historico(matricula)));
    }
    @Override public List<Versao> historicoAluno(long matricula) {
        HorarioAula.validarId(matricula);
        return TransacaoJdbc.executar(c,()->{ controle(c); exigirPropriedade(matricula); return historico(matricula); });
    }
    private void exigirPropriedade(long matricula) throws SQLException {
        long usuario=SegurancaJdbc.exigirSessao(c,sessao);
        if(q(c,"SELECT m.id_matricula FROM matricula_academica m JOIN aluno a ON a.id_aluno=m.id_aluno WHERE m.id_matricula=? AND a.id_usuario=? FOR SHARE",
                r->r.getLong(1),matricula,usuario).isEmpty())
            throw new IllegalArgumentException("Matrícula não encontrada ou não autorizada.");
    }
    private Optional<Consulta> atual(long matricula) throws SQLException {
        var ids=q(c,"SELECT id_plano FROM plano_institucional WHERE id_matricula=? ORDER BY versao DESC LIMIT 1 FOR SHARE",r->r.getLong(1),matricula);
        if(ids.isEmpty()) return Optional.empty();
        var versao=carregar(ids.getFirst());
        try {
            var previa=prepararInterno(new Selecao(matricula,versao.periodos(),versao.prioridades()));
            boolean igual=previa.assinatura().equals(versao.assinatura());
            return Optional.of(new Consulta(versao,igual?Atualidade.ATUAL:Atualidade.PENDENTE_ATUALIZACAO,
                    igual?"Plano registrado corresponde aos dados consultados. Sugestões não reservam vagas nem efetivam inscrições.":
                    "Há dados posteriores ou alterados. Esta versão permanece preservada; solicite atualização acadêmica."));
        } catch(IllegalArgumentException e) {
            return Optional.of(new Consulta(versao,Atualidade.INCONSISTENTE,"Versão histórica preservada. Não considerar o plano como definitivo: "+e.getMessage()));
        }
    }
    private List<Versao> historico(long matricula) throws SQLException {
        var ids=q(c,"SELECT id_plano FROM plano_institucional WHERE id_matricula=? ORDER BY versao FOR SHARE",r->r.getLong(1),matricula);
        var lista=new ArrayList<Versao>(); for(long id:ids) lista.add(carregar(id)); return List.copyOf(lista);
    }
    private List<Long> idsDoJson(long plano,String campo) throws SQLException {
        if(!Set.of("periodos","prioridades").contains(campo)) throw new IllegalArgumentException("Campo interno inválido.");
        return q(c,"SELECT j.valor FROM plano_institucional p,JSON_TABLE(p."+campo+",'$[*]' COLUMNS(ordem FOR ORDINALITY,valor BIGINT PATH '$')) j WHERE p.id_plano=? ORDER BY j.ordem",
                r->r.getLong(1),plano);
    }
    private Versao carregar(long id) throws SQLException {
        var dados=q(c,"SELECT * FROM plano_institucional WHERE id_plano=? FOR SHARE",r->new Object[]{r.getLong("id_matricula"),r.getLong("id_estrutura"),r.getInt("versao"),
                r.getObject("id_anterior",Long.class),r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class),r.getString("motivo"),r.getString("assinatura")},id).getFirst();
        var itens=q(c,"SELECT * FROM componente_plano_institucional WHERE id_plano=? ORDER BY ordem FOR SHARE",r->{
            long disciplina=r.getLong("id_disciplina");
            var requisitos=q(c,"SELECT j.valor FROM componente_plano_institucional i,JSON_TABLE(i.requisitos_snapshot,'$[*]' COLUMNS(ordem FOR ORDINALITY,valor BIGINT PATH '$')) j WHERE i.id_plano=? AND i.id_disciplina=? ORDER BY j.ordem",
                    x->x.getLong(1),id,disciplina);
            return new Componente(disciplina,r.getString("codigo_snapshot"),r.getString("nome_snapshot"),r.getInt("horas_snapshot"),r.getString("cadastro_snapshot"),r.getString("situacao_snapshot"),
                    r.getString("evidencia_snapshot"),requisitos,r.getObject("id_turma_sugerida",Long.class),r.getObject("id_periodo_sugerido",Long.class),r.getString("periodo_snapshot"),
                    r.getObject("inicio_snapshot",LocalDate.class),r.getObject("fim_snapshot",LocalDate.class),r.getString("diagnostico"));
        },id);
        return new Versao(id,(long)dados[0],(long)dados[1],(int)dados[2],(Long)dados[3],(long)dados[4],(LocalDateTime)dados[5],(String)dados[6],(String)dados[7],idsDoJson(id,"periodos"),idsDoJson(id,"prioridades"),itens);
    }
}

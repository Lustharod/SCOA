package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.TransferenciaExterna.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Processo, efeitos institucionais, histórico e auditoria na mesma transação. */
public final class RepositorioTransferenciaExternaJdbc implements RepositorioTransferenciaExterna {
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    private final AcessoAcademicoJdbc acesso;
    public RepositorioTransferenciaExternaJdbc(Connection c,SegurancaJdbc.Sessao sessao) {
        this.c=Objects.requireNonNull(c); this.sessao=sessao; acesso=new AcessoAcademicoJdbc(c,sessao);
    }
    @Override public Processo registrar(Pedido pedido) {
        Objects.requireNonNull(pedido);
        return acesso.manter("TRANSFERENCIA_EXTERNA","REGISTRAR",0,()->TransacaoJdbc.executar(c,()->{
            String assinatura=hash(pedido.toString());
            var existentes=q(c,"SELECT * FROM processo_transferencia_externa WHERE chave=? FOR UPDATE",RepositorioTransferenciaExternaJdbc::ler,pedido.chave());
            if(!existentes.isEmpty()) {
                if(!q(c,"SELECT id_processo FROM processo_transferencia_externa WHERE chave=? AND assinatura_pedido=?",
                        r->r.getLong(1),pedido.chave(),assinatura).isEmpty()) return existentes.getFirst();
                throw new IllegalArgumentException("Chave já usada por outro pedido; consulte o protocolo existente.");
            }
            long autor=SegurancaJdbc.exigirSessao(c,sessao); var agora=agoraUtc(c);
            long aluno,curso;
            if(pedido.tipo()==Tipo.ENTRADA) {
                aluno=pedido.aluno(); curso=pedido.curso();
                if(q(c,"SELECT id_aluno FROM aluno WHERE id_aluno=? FOR SHARE",r->r.getLong(1),aluno).isEmpty())
                    throw new IllegalArgumentException("Aluno não localizado; identifique o cadastro existente antes do processo.");
                estrutura(curso,pedido.estrutura());
            } else {
                var origem=q(c,"SELECT id_aluno,id_curso FROM matricula_academica WHERE id_matricula=? FOR SHARE",
                        r->new long[]{r.getLong(1),r.getLong(2)},pedido.origem());
                if(origem.isEmpty()) throw new IllegalArgumentException("Matrícula de origem não encontrada.");
                aluno=origem.getFirst()[0]; curso=origem.getFirst()[1];
            }
            if(q(c,"SELECT id_periodo FROM periodo_letivo WHERE id_periodo=? FOR SHARE",r->r.getLong(1),pedido.periodo()).isEmpty())
                throw new IllegalArgumentException("Período não encontrado.");
            long id=g(c,"""
                    INSERT INTO processo_transferencia_externa(chave,assinatura_pedido,tipo,id_aluno,id_curso,id_estrutura,
                        id_matricula_origem,id_periodo,instituicao_externa,documentos,motivo,situacao,id_usuario_autor,
                        registrado_em_utc,atualizado_em_utc)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,'EM_ANALISE',?,?,?)
                    """,pedido.chave(),assinatura,pedido.tipo().name(),aluno,curso,pedido.estrutura(),pedido.origem(),
                    pedido.periodo(),pedido.instituicao(),pedido.documentos(),pedido.motivo(),autor,agora,agora);
            andamento(id,0,null,Situacao.EM_ANALISE,pedido.documentos(),pedido.motivo(),autor,agora);
            evento(c,"TRANSF_EXTERNA",id,autor,"REGISTRAR", "Processo externo registrado para análise; sem alteração de vínculo.");
            return buscar(id);
        }));
    }
    @Override public Processo consultar(long id) {
        HorarioAula.validarId(id);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->buscar(id)));
    }
    @Override public List<Processo> listar(long aluno) {
        HorarioAula.validarId(aluno);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->q(c,
                "SELECT * FROM processo_transferencia_externa WHERE id_aluno=? ORDER BY id_processo FOR SHARE",
                RepositorioTransferenciaExternaJdbc::ler,aluno)));
    }
    @Override public List<Andamento> historico(long id) {
        HorarioAula.validarId(id);
        return acesso.consultar(()->TransacaoJdbc.executar(c,()->{ buscar(id); return andamentos(id); }));
    }
    @Override public Processo atualizar(long id,Atualizacao u) {
        HorarioAula.validarId(id); Objects.requireNonNull(u);
        return acesso.manter("TRANSFERENCIA_EXTERNA","ATUALIZAR",id,()->TransacaoJdbc.executar(c,()->{
            Processo p=buscar(id); long autor=SegurancaJdbc.exigirSessao(c,sessao);
            if(p.situacao()!=Situacao.EM_ANALISE) {
                var ultimo=andamentos(id).getLast();
                if(p.situacao()==u.situacao() && p.versao()==u.versao()+1 && p.documentos().equals(u.documentos())
                        && ultimo.observacao().equals(u.observacao())) return p;
                throw new IllegalArgumentException("Processo concluído. Histórico e efeitos são preservados; nova atualização não permitida.");
            }
            if(p.versao()!=u.versao()) throw new IllegalArgumentException("Processo alterado desde a consulta; confira a versão atual.");
            var agora=agoraUtc(c); Long transferencia=null,matricula=null;
            if(u.situacao()==Situacao.EFETIVADA) {
                LocalDate hoje=dataAcademica(agora); periodoNoPrazo(p.periodo(),hoje);
                var efeito=p.tipo()==Tipo.ENTRADA?entrada(p,autor,agora,hoje):saida(p,autor,agora,hoje);
                transferencia=efeito[0]; matricula=efeito[1];
            }
            g(c,"""
                    UPDATE processo_transferencia_externa SET documentos=?,situacao=?,versao=versao+1,
                        id_transferencia=?,id_matricula_efetivada=?,atualizado_em_utc=? WHERE id_processo=? AND versao=?
                    """,u.documentos(),u.situacao().name(),transferencia,matricula,agora,id,u.versao());
            andamento(id,p.versao()+1,p.situacao().name(),u.situacao(),u.documentos(),u.observacao(),autor,agora);
            evento(c,"TRANSF_EXTERNA",id,autor,u.situacao()==Situacao.EM_ANALISE?"ACOMPANHAR":u.situacao().name(),
                    "Situação "+u.situacao()+"; versão "+(p.versao()+1)+". Fundamento preservado no andamento específico.");
            return buscar(id);
        }));
    }
    private void estrutura(long curso,long estrutura) throws SQLException {
        if(q(c,"""
                SELECT e.id_estrutura FROM estrutura_curricular e JOIN curso c ON c.id_curso=e.id_curso
                WHERE e.id_estrutura=? AND e.id_curso=? AND e.situacao='ATIVO' AND c.situacao='ATIVO' FOR SHARE
                """,r->r.getLong(1),estrutura,curso).isEmpty())
            throw new IllegalArgumentException("A estrutura deve estar ativa e pertencer ao curso de destino ativo.");
    }
    private void periodoNoPrazo(long periodo,LocalDate hoje) throws SQLException {
        var datas=q(c,"SELECT data_inicio,data_fim FROM periodo_letivo WHERE id_periodo=? AND situacao='ATIVO' FOR SHARE",
                r->new LocalDate[]{r.getObject(1,LocalDate.class),r.getObject(2,LocalDate.class)},periodo);
        if(datas.isEmpty() || !TransferenciaInterna.noPrazo(datas.getFirst()[0],datas.getFirst()[1],hoje))
            throw new IllegalArgumentException("Transferência exige período ativo, até D+45 e sem ultrapassar seu fim.");
    }
    private long[] entrada(Processo p,long autor,LocalDateTime agora,LocalDate hoje) throws SQLException {
        estrutura(p.curso(),p.estrutura());
        var m=new RepositorioMatriculaJdbc(c).criar(p.aluno(),p.curso(),hoje,p.periodo(),p.motivo());
        g(c,"INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",m.id(),p.estrutura());
        String historico="Entrada externa pelo processo "+p.id()+"; matrícula "+m.id()+" aberta. Equivalências externas dependem de decisão formal própria.";
        long transferencia=g(c,"""
                INSERT INTO transferencia_academica(id_matricula,tipo,data,situacao,historico,id_matricula_destino,
                    id_curso_destino,id_estrutura_destino,id_periodo_transferencia,id_usuario_autor,motivo,registrado_em)
                VALUES (?,'EXTERNA_ENTRADA',?,'EFETIVADA',?,?,?,?,?,?,?,?)
                """,m.id(),hoje,historico,m.id(),p.curso(),p.estrutura(),p.periodo(),autor,p.motivo(),agora);
        return new long[]{transferencia,m.id()};
    }
    private long[] saida(Processo p,long autor,LocalDateTime agora,LocalDate hoje) throws SQLException {
        if(q(c,"SELECT id_aluno FROM aluno WHERE id_aluno=? AND situacao='ATIVO' FOR UPDATE",r->r.getLong(1),p.aluno()).isEmpty())
            throw new IllegalArgumentException("Aluno deve estar ativo para efetivar a transferência.");
        var origem=q(c,"SELECT id_aluno,id_curso,situacao,data_ingresso FROM matricula_academica WHERE id_matricula=? FOR UPDATE",
                r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getObject(4,LocalDate.class)},p.origem());
        if(origem.isEmpty() || (long)origem.getFirst()[0]!=p.aluno() || (long)origem.getFirst()[1]!=p.curso()
                || !"ATIVO".equals(origem.getFirst()[2]) || ((LocalDate)origem.getFirst()[3]).isAfter(hoje))
            throw new IllegalArgumentException("A matrícula de origem deve estar ativa e coerente com a pessoa e o curso do processo.");
        var biblioteca=new RepositorioMatriculaJdbc(c).consultarBiblioteca(p.aluno());
        if(biblioteca.estado()!=SituacaoBiblioteca.Estado.REGULAR)
            throw new IllegalArgumentException("Transferência não efetivada. Regularize a biblioteca: "+biblioteca.detalhe());
        if(!q(c,"SELECT id_inscricao FROM inscricao_disciplina WHERE id_matricula=? AND situacao NOT IN ('ATIVO','CANCELADO','APROVADO','REPROVADO') FOR SHARE",
                r->r.getLong(1),p.origem()).isEmpty())
            throw new IllegalArgumentException("Inscrição com situação desconhecida exige conferência acadêmica.");
        var ativas=q(c,"""
                SELECT i.id_inscricao,t.id_turma,p.data_inicio,p.data_fim FROM inscricao_disciplina i
                JOIN turma t ON t.id_turma=i.id_turma JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                WHERE i.id_matricula=? AND i.situacao='ATIVO' ORDER BY i.id_inscricao FOR UPDATE
                """,r->new Object[]{r.getLong(1),r.getLong(2),r.getObject(3,LocalDate.class),r.getObject(4,LocalDate.class)},p.origem());
        for(var i:ativas) {
            if(!TransferenciaInterna.noPrazo((LocalDate)i[2],(LocalDate)i[3],hoje))
                throw new IllegalArgumentException("Há inscrição ativa fora do prazo de transferência; resolva a pendência acadêmica.");
            if(!q(c,"SELECT id_inscricao FROM resultado_inscricao WHERE id_inscricao=? FOR SHARE",r->r.getLong(1),i[0]).isEmpty())
                throw new IllegalArgumentException("Inscrição ativa com resultado exige conferência antes da transferência.");
            if(!q(c,"SELECT id_pedido FROM cancelamento_inscricao WHERE id_inscricao=? AND situacao='PENDENTE' FOR SHARE",r->r.getLong(1),i[0]).isEmpty())
                throw new IllegalArgumentException("Conclua o pedido de cancelamento pendente antes da transferência.");
        }
        for(var i:ativas) g(c,"UPDATE inscricao_disciplina SET situacao='CANCELADO',vaga_retida=? WHERE id_inscricao=?",
                !CancelamentoInscricao.imediato((LocalDate)i[2],hoje),i[0]);
        g(c,"UPDATE matricula_academica SET situacao='TRANSFERIDO' WHERE id_matricula=?",p.origem());
        g(c,"""
                INSERT INTO historico_matricula(id_matricula,operacao,motivo,situacao_anterior,situacao_nova,
                    data_anterior,data_nova,periodo_anterior,periodo_novo)
                SELECT id_matricula,'TRANSFERENCIA',?,'ATIVO','TRANSFERIDO',data_ingresso,data_ingresso,
                    id_periodo_ingresso,id_periodo_ingresso FROM matricula_academica WHERE id_matricula=?
                """,p.motivo(),p.origem());
        String historico="Saída externa pelo processo "+p.id()+"; origem "+p.origem()+" preservada como TRANSFERIDO. Inscrições canceladas: "+ativas.stream().map(i->i[0].toString()).toList()+". Sem novo vínculo no SCOA.";
        long transferencia=g(c,"""
                INSERT INTO transferencia_academica(id_matricula,tipo,data,situacao,historico,
                    id_periodo_transferencia,id_usuario_autor,motivo,registrado_em)
                VALUES (?,'EXTERNA_SAIDA',?,'EFETIVADA',?,?,?,?,?)
                """,p.origem(),hoje,historico,p.periodo(),autor,p.motivo(),agora);
        return new long[]{transferencia,p.origem()};
    }
    private Processo buscar(long id) throws SQLException {
        return q(c,"SELECT * FROM processo_transferencia_externa WHERE id_processo=? FOR UPDATE",RepositorioTransferenciaExternaJdbc::ler,id)
                .stream().findFirst().orElseThrow(()->new IllegalArgumentException("Processo externo não encontrado."));
    }
    private void andamento(long id,int versao,String anterior,Situacao nova,String docs,String obs,long autor,LocalDateTime agora) throws SQLException {
        g(c,"""
                INSERT INTO andamento_transferencia_externa(id_processo,versao,situacao_anterior,situacao_nova,
                    documentos,observacao,id_usuario_autor,registrado_em_utc) VALUES (?,?,?,?,?,?,?,?)
                """,id,versao,anterior,nova.name(),docs,obs,autor,agora);
    }
    private List<Andamento> andamentos(long id) throws SQLException {
        return q(c,"SELECT * FROM andamento_transferencia_externa WHERE id_processo=? ORDER BY versao FOR SHARE",
                r->new Andamento(r.getLong("id_andamento"),id,r.getInt("versao"),r.getString("situacao_anterior"),
                    Situacao.valueOf(r.getString("situacao_nova")),r.getString("documentos"),r.getString("observacao"),
                    r.getLong("id_usuario_autor"),r.getObject("registrado_em_utc",LocalDateTime.class)),id);
    }
    private static Processo ler(ResultSet r) throws SQLException {
        return new Processo(r.getLong("id_processo"),r.getString("chave"),Tipo.valueOf(r.getString("tipo")),
                r.getLong("id_aluno"),r.getLong("id_curso"),r.getObject("id_estrutura",Long.class),r.getObject("id_matricula_origem",Long.class),
                r.getLong("id_periodo"),r.getString("instituicao_externa"),r.getString("documentos"),r.getString("motivo"),
                Situacao.valueOf(r.getString("situacao")),r.getInt("versao"),r.getObject("id_transferencia",Long.class),
                r.getObject("id_matricula_efetivada",Long.class),r.getLong("id_usuario_autor"),
                r.getObject("registrado_em_utc",LocalDateTime.class),r.getObject("atualizado_em_utc",LocalDateTime.class));
    }
}

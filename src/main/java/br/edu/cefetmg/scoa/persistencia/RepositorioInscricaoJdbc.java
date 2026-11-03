package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Leituras correntes; aluno serializa inscrições e fechamento, turma serializa vagas e horários. */
public final class RepositorioInscricaoJdbc implements RepositorioInscricao {
    private final Connection c;
    public RepositorioInscricaoJdbc(Connection c) { this.c = Objects.requireNonNull(c); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    private <T> List<T> q(String sql, Leitor<T> leitor, Object... valores) throws SQLException {
        try (var s=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            try(var r=s.executeQuery()) {
                List<T> lista=new ArrayList<>(); while(r.next()) lista.add(leitor.ler(r)); return List.copyOf(lista);
            }
        }
    }
    private long gravar(String sql, Object... valores) throws SQLException {
        try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            s.executeUpdate(); try(var r=s.getGeneratedKeys()) { return r.next()?r.getLong(1):0; }
        }
    }
    private LocalDate hoje() throws SQLException {
        return q("SELECT UTC_TIMESTAMP(6)",r->r.getObject(1,LocalDateTime.class)
                .atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("America/Sao_Paulo")).toLocalDate()).getFirst();
    }
    private record Vinculo(long aluno,long curso,String situacao) { }
    private Vinculo matricula(long id) throws SQLException {
        return matricula(id,true);
    }
    private Vinculo matricula(long id,boolean exigirAlunoAtivo) throws SQLException {
        HorarioAula.validarId(id);
        var previa=q("SELECT id_aluno FROM matricula_academica WHERE id_matricula=?",r->r.getLong(1),id);
        if(previa.isEmpty()) throw new IllegalArgumentException("Matrícula não encontrada.");
        var alunos=q("SELECT situacao FROM aluno WHERE id_aluno=? FOR UPDATE",r->r.getString(1),previa.getFirst());
        var atual=q("SELECT id_aluno,id_curso,situacao FROM matricula_academica WHERE id_matricula=? FOR UPDATE",
                r->new Vinculo(r.getLong(1),r.getLong(2),r.getString(3)),id);
        if(atual.isEmpty() || atual.getFirst().aluno()!=previa.getFirst())
            throw new IllegalArgumentException("Vínculo alterado. Consulte novamente.");
        if(alunos.isEmpty() || (exigirAlunoAtivo && !"ATIVO".equals(alunos.getFirst()))) throw new IllegalArgumentException("Aluno ausente ou inativo.");
        return atual.getFirst();
    }
    private void ativo(String tabela,String chave,long id) throws SQLException {
        // Identificadores são constantes internas, nunca entradas do console.
        var lista=q("SELECT situacao FROM "+tabela+" WHERE "+chave+"=? FOR SHARE",r->r.getString(1),id);
        if(lista.isEmpty() || !"ATIVO".equals(lista.getFirst())) throw new IllegalArgumentException(tabela+" ausente ou inativo.");
    }
    private record Oferta(long disciplina,long periodo,int capacidade,String situacao) { }
    private Oferta turma(long id) throws SQLException {
        var lista=q("SELECT id_disciplina,id_periodo,capacidade,situacao FROM turma WHERE id_turma=? FOR UPDATE",
                r->new Oferta(r.getLong(1),r.getLong(2),r.getInt(3),r.getString(4)),id);
        if(lista.isEmpty()) throw new IllegalArgumentException("Turma não encontrada.");
        return lista.getFirst();
    }
    private record Periodo(LocalDate inicio,LocalDate fim,String situacao) { }
    private Periodo periodo(long id) throws SQLException {
        var lista=q("SELECT data_inicio,data_fim,situacao FROM periodo_letivo WHERE id_periodo=? FOR SHARE",
                r->new Periodo(r.getDate(1).toLocalDate(),r.getDate(2).toLocalDate(),r.getString(3)),id);
        if(lista.isEmpty()) throw new IllegalArgumentException("Período não encontrado.");
        return lista.getFirst();
    }
    private static Inscricao ler(ResultSet r) throws SQLException {
        return new Inscricao(r.getLong("id_inscricao"),r.getLong("id_matricula"),r.getLong("id_turma"),
                r.getDate("data_inscricao").toLocalDate(),r.getString("situacao"));
    }
    @Override public List<Inscricao> listar(long id) {
        HorarioAula.validarId(id);
        return TransacaoJdbc.executar(c,()->q("SELECT * FROM inscricao_disciplina WHERE id_matricula=? ORDER BY id_inscricao",
                RepositorioInscricaoJdbc::ler,id));
    }
    private record Horario(int dia,LocalTime inicio,LocalTime fim) { }
    private List<Horario> horarios(long turma) throws SQLException {
        return q("SELECT dia_semana,hora_inicio,hora_fim FROM horario_aula WHERE id_turma=? ORDER BY id_horario FOR SHARE",
                r->new Horario(r.getInt(1),r.getTime(2).toLocalTime(),r.getTime(3).toLocalTime()),turma);
    }
    private boolean cumpriu(long matricula,long disciplina,LocalDate data) throws SQLException {
        var resultados=q("SELECT r.situacao,p.data_fim FROM resultado_inscricao r JOIN inscricao_disciplina i ON i.id_inscricao=r.id_inscricao "
                +"JOIN turma t ON t.id_turma=i.id_turma JOIN periodo_letivo p ON p.id_periodo=t.id_periodo "
                +"WHERE i.id_matricula=? AND t.id_disciplina=? FOR SHARE",r->new Object[]{r.getString(1),r.getDate(2).toLocalDate()},matricula,disciplina);
        for(var r:resultados) if("APROVADO".equals(r[0]) && ((LocalDate)r[1]).isBefore(data)) return true;
        if(!q("SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND id_disciplina_destino=? AND situacao='DEFERIDO' "
                +"AND decidido_em<=CURRENT_TIMESTAMP(6) AND id_estrutura=(SELECT id_estrutura FROM vinculo_curricular_matricula WHERE id_matricula=?) FOR SHARE",
                r->r.getLong(1),matricula,disciplina,matricula).isEmpty()) return true;
        var decisoes=q("SELECT decisao,data FROM aproveitamento_equivalencia WHERE id_matricula=? AND id_disciplina_destino=? FOR SHARE",
                r->new Object[]{r.getString(1),r.getDate(2).toLocalDate()},matricula,disciplina);
        // Somente o produtor formal v0.8 autoriza reconhecimento; o legado não é promovido.
        if(!decisoes.isEmpty()) throw new IllegalArgumentException("Há processo de equivalência: reconhecimento requer integração formal ainda não implementada.");
        return false;
    }
    private Elegibilidade avaliarBloqueada(long id,long alvo) throws SQLException {
        // Antes de qualquer leitura de intervalos vazios: evita deadlock de gap locks entre alunos.
        if(q("SELECT id FROM controle_inscricao WHERE id=1 FOR UPDATE",r->r.getInt(1)).isEmpty())
            throw new IllegalArgumentException("Controle de inscrição ausente. Confira a migração V007.");
        HorarioAula.validarId(alvo);
        Vinculo m=matricula(id);
        List<String> motivos=new ArrayList<>();
        if(!q("SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND id_disciplina_destino="
                +"(SELECT id_disciplina FROM turma WHERE id_turma=?) AND situacao='DEFERIDO' FOR SHARE",
                r->r.getLong(1),id,alvo).isEmpty()) motivos.add("Disciplina já reconhecida por equivalência; análise de impacto necessária para cursá-la.");
        if(!"ATIVO".equals(m.situacao())) motivos.add("Matrícula deve estar ativa.");
        ativo("curso","id_curso",m.curso());
        // Grafo e disciplinas sob a mesma ordem usada pelo escritor de pré-requisitos.
        q("SELECT id_disciplina FROM disciplina ORDER BY id_disciplina FOR SHARE",r->r.getLong(1));
        var inscricoes=q("SELECT i.* FROM inscricao_disciplina i JOIN matricula_academica m ON m.id_matricula=i.id_matricula "
                +"WHERE m.id_aluno=? ORDER BY i.id_inscricao FOR UPDATE",RepositorioInscricaoJdbc::ler,m.aluno());
        SortedSet<Long> turmas=new TreeSet<>(); turmas.add(alvo);
        for(var i:inscricoes) if("ATIVO".equals(i.situacao())) turmas.add(i.turma());
        Map<Long,Oferta> ofertas=new HashMap<>(); for(long t:turmas) ofertas.put(t,turma(t));
        Oferta o=ofertas.get(alvo); Periodo p=periodo(o.periodo()); LocalDate data=hoje();
        JanelaInscricao janela=JanelaInscricao.doPeriodo(p.inicio());
        if(!"ATIVO".equals(o.situacao()) || !"ATIVO".equals(p.situacao())) motivos.add("Turma e período devem estar ativos.");
        ativo("disciplina","id_disciplina",o.disciplina());
        if(!janela.permite(data)) motivos.add("Fora da janela: "+janela.abertura()+" a "+janela.fimExclusivo().minusDays(1)+".");
        var estruturas=q("SELECT id_estrutura FROM vinculo_curricular_matricula WHERE id_matricula=? FOR SHARE",r->r.getLong(1),id);
        if(estruturas.isEmpty()) motivos.add("Vincule explicitamente a estrutura curricular da matrícula.");
        else {
            long estrutura=estruturas.getFirst(); ativo("estrutura_curricular","id_estrutura",estrutura);
            if(q("SELECT id_disciplina FROM estrutura_disciplina WHERE id_estrutura=? AND id_disciplina=? FOR SHARE",r->r.getLong(1),estrutura,o.disciplina()).isEmpty())
                motivos.add("Disciplina fora da estrutura curricular vinculada.");
        }
        for(var i:inscricoes) {
            if(!Set.of("ATIVO","CANCELADO","APROVADO","REPROVADO").contains(i.situacao())) motivos.add("Situação de inscrição desconhecida: "+i.id()+".");
            if(i.turma()==alvo && "ATIVO".equals(i.situacao())) motivos.add("Inscrição vigente já existente: "+i.id()+".");
        }
        var ocupacoes=q("SELECT situacao FROM inscricao_disciplina WHERE id_turma=? FOR UPDATE",r->r.getString(1),alvo);
        if(ocupacoes.stream().anyMatch(s->!Set.of("ATIVO","CANCELADO","APROVADO","REPROVADO").contains(s))) motivos.add("Turma possui inscrição com situação desconhecida.");
        // Encerramentos continuam ocupando o registro da oferta; só cancelamento libera vaga.
        int vagas=Math.max(0,o.capacidade()-(int)ocupacoes.stream().filter(s->!"CANCELADO".equals(s)).count());
        if(vagas==0) motivos.add("Turma sem vagas.");
        var horarios=horarios(alvo);
        if(horarios.isEmpty()) motivos.add("Turma sem horários definidos.");
        for(var i:inscricoes) if("ATIVO".equals(i.situacao()) && i.turma()!=alvo) {
            Oferta outra=ofertas.get(i.turma()); Periodo op=periodo(outra.periodo());
            if(p.inicio().isAfter(op.fim()) || op.inicio().isAfter(p.fim())) continue;
            var outros=horarios(i.turma());
            if(outros.isEmpty()) motivos.add("Inscrição vigente sem horários: "+i.id()+".");
            for(var h:horarios) for(var b:outros)
                if(RegraAlocacao.conflita(new FaixaHorario(h.dia(),h.inicio(),h.fim()),new PeriodoLetivo(p.inicio(),p.fim()),
                        new FaixaHorario(b.dia(),b.inicio(),b.fim()),new PeriodoLetivo(op.inicio(),op.fim())))
                    motivos.add("Conflito com turma "+i.turma()+".");
        }
        var requisitos=q("SELECT id_disciplina_prerequisito FROM disciplina_prerequisito WHERE id_disciplina=? ORDER BY id_disciplina_prerequisito FOR SHARE",r->r.getLong(1),o.disciplina());
        for(long requisito:requisitos) if(!cumpriu(id,requisito,data)) motivos.add("Pré-requisito não cumprido: "+requisito+".");
        return new Elegibilidade(motivos.stream().distinct().toList(),vagas,janela);
    }
    @Override public Elegibilidade avaliar(long matricula,long turma) {
        return TransacaoJdbc.executar(c,()->avaliarBloqueada(matricula,turma));
    }
    @Override public Inscricao inscrever(long matricula,long turma) {
        return TransacaoJdbc.executar(c,()->{
            var avaliacao=avaliarBloqueada(matricula,turma); avaliacao.exigir(); LocalDate data=hoje();
            if(!avaliacao.janela().permite(data)) throw new IllegalArgumentException("Janela encerrada durante a confirmação.");
            long id=gravar("INSERT INTO inscricao_disciplina(id_matricula,id_turma,data_inscricao,situacao) VALUES (?,?,?,'ATIVO')",matricula,turma,data);
            return new Inscricao(id,matricula,turma,data,"ATIVO");
        });
    }
    @Override public void vincularEstrutura(long id,long estrutura) {
        HorarioAula.validarId(estrutura);
        TransacaoJdbc.executar(c,()->{
            var m=matricula(id);
            if(!"ATIVO".equals(m.situacao())) throw new IllegalArgumentException("Matrícula deve estar ativa.");
            ativo("curso","id_curso",m.curso()); ativo("estrutura_curricular","id_estrutura",estrutura);
            long curso=q("SELECT id_curso FROM estrutura_curricular WHERE id_estrutura=? FOR SHARE",r->r.getLong(1),estrutura).getFirst();
            if(curso!=m.curso()) throw new IllegalArgumentException("Estrutura pertence a outro curso.");
            if(!q("SELECT id_matricula FROM vinculo_curricular_matricula WHERE id_matricula=? FOR UPDATE",r->r.getLong(1),id).isEmpty())
                throw new IllegalArgumentException("Estrutura já vinculada; transferência curricular não implementada.");
            if(!q("SELECT id_plano FROM plano_estudo WHERE id_matricula=? FOR SHARE",r->r.getLong(1),id).isEmpty())
                throw new IllegalArgumentException("Matrícula possui plano legado; concilie seu vínculo antes de usar este fluxo.");
            gravar("INSERT INTO vinculo_curricular_matricula(id_matricula,id_estrutura) VALUES (?,?)",id,estrutura);
            return null;
        });
    }
    @Override public RegraResultado.Resultado registrarResultado(long inscricao,BigDecimal p1,BigDecimal p2,BigDecimal pf,long total,long presentes,String motivo) {
        HorarioAula.validarId(inscricao); String razao=Matricula.validarMotivo(motivo);
        var resultado=RegraResultado.calcular(p1,p2,pf,total,presentes);
        if("AGUARDA_PF".equals(resultado.situacao())) throw new IllegalArgumentException("PF pendente; resultado não encerrado.");
        return TransacaoJdbc.executar(c,()->{
            var previa=q("SELECT id_matricula FROM inscricao_disciplina WHERE id_inscricao=?",r->r.getLong(1),inscricao);
            if(previa.isEmpty()) throw new IllegalArgumentException("Inscrição não encontrada.");
            matricula(previa.getFirst(),false);
            var i=q("SELECT * FROM inscricao_disciplina WHERE id_inscricao=? FOR UPDATE",RepositorioInscricaoJdbc::ler,inscricao).getFirst();
            if(i.matricula()!=previa.getFirst()) throw new IllegalArgumentException("Vínculo alterado. Consulte novamente.");
            if(!"ATIVO".equals(i.situacao())) throw new IllegalArgumentException("Somente inscrição ativa admite primeiro fechamento.");
            var o=turma(i.turma());
            if(!periodo(o.periodo()).fim().isBefore(hoje())) throw new IllegalArgumentException("Aguarde o encerramento do período antes de registrar o resultado final.");
            gravar("INSERT INTO resultado_inscricao(id_inscricao,p1,p2,pf,total_minutos,presentes_minutos,media_inicial,media_final,situacao,motivo) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    inscricao,p1,p2,pf,total,presentes,resultado.mediaInicial(),resultado.mediaFinal(),resultado.situacao(),razao);
            gravar("UPDATE inscricao_disciplina SET situacao=? WHERE id_inscricao=?",resultado.situacao(),inscricao);
            return resultado;
        });
    }
}

package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.Planejamento.*;
import br.edu.cefetmg.scoa.dominio.Planejamento.Disciplina;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Leituras correntes sob o protocolo de coordenação existente; não grava dados acadêmicos. */
public final class RepositorioPlanejamentoJdbc {
    private final Connection c;
    public RepositorioPlanejamentoJdbc(Connection c) { this.c=Objects.requireNonNull(c); }
    @FunctionalInterface private interface Leitor<T> { T ler(ResultSet r) throws SQLException; }
    private <T> List<T> q(String sql, Leitor<T> leitor, Object... valores) throws SQLException {
        try(var s=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            try(var r=s.executeQuery()) { var itens=new ArrayList<T>(); while(r.next()) itens.add(leitor.ler(r)); return List.copyOf(itens); }
        }
    }
    private static Periodo periodo(ResultSet r) throws SQLException {
        return new Periodo(r.getLong("id_periodo"),r.getString("descricao"),r.getObject("data_inicio",LocalDate.class),r.getObject("data_fim",LocalDate.class),r.getString("situacao"));
    }
    public List<Periodo> periodos() {
        return TransacaoJdbc.executar(c,()->q("SELECT * FROM periodo_letivo ORDER BY data_inicio,id_periodo",RepositorioPlanejamentoJdbc::periodo));
    }
    private void ativo(String tabela,String chave,long id) throws SQLException {
        var lista=q("SELECT situacao FROM "+tabela+" WHERE "+chave+"=? FOR SHARE",r->r.getString(1),id);
        if(lista.isEmpty() || !"ATIVO".equals(lista.getFirst())) throw new IllegalArgumentException(tabela+" deve estar ativo para simular.");
    }
    private Oferta oferta(long turma,Map<Long,Periodo> periodos) throws SQLException {
        var linha=q("SELECT id_disciplina,id_periodo,situacao,capacidade FROM turma WHERE id_turma=? FOR SHARE",
                r->new Object[]{r.getLong(1),r.getLong(2),r.getString(3),r.getInt(4)},turma).getFirst();
        var estados=q("SELECT situacao FROM inscricao_disciplina WHERE id_turma=? ORDER BY id_inscricao FOR SHARE",r->r.getString(1),turma);
        if(estados.stream().anyMatch(s->!Set.of("ATIVO","CANCELADO","APROVADO","REPROVADO").contains(s)))
            throw new IllegalArgumentException("Turma "+turma+" com situação de inscrição desconhecida; conciliação necessária.");
        var horarios=q("SELECT dia_semana,hora_inicio,hora_fim FROM horario_aula WHERE id_turma=? ORDER BY id_horario FOR SHARE",
                r->new FaixaHorario(r.getInt(1),r.getTime(2).toLocalTime(),r.getTime(3).toLocalTime()),turma);
        return new Oferta(turma,(long)linha[0],periodos.get((long)linha[1]),(String)linha[2],
                Math.max(0,(int)linha[3]-(int)estados.stream().filter(s->!"CANCELADO".equals(s)).count()),horarios);
    }
    public Resultado simular(long matricula,List<Long> selecionados,List<Long> prioridade) {
        HorarioAula.validarId(matricula); Objects.requireNonNull(selecionados); Objects.requireNonNull(prioridade);
        if(selecionados.isEmpty() || new HashSet<>(selecionados).size()!=selecionados.size())
            throw new IllegalArgumentException("Selecione períodos sem repetir IDs.");
        selecionados.forEach(HorarioAula::validarId);
        return TransacaoJdbc.executar(c,()->{
            // plano adquire controle_inscricao, aluno, matrícula, disciplinas e vínculo nesta ordem.
            var plano=new RepositorioEquivalenciaJdbc(c).plano(matricula);
            var contexto=q("SELECT id_aluno,id_curso FROM matricula_academica WHERE id_matricula=? FOR SHARE",
                    r->new long[]{r.getLong(1),r.getLong(2)},matricula).getFirst();
            ativo("aluno","id_aluno",contexto[0]); ativo("matricula_academica","id_matricula",matricula);
            ativo("curso","id_curso",contexto[1]); ativo("estrutura_curricular","id_estrutura",plano.estrutura());
            if(!q("SELECT id_pedido FROM pedido_equivalencia WHERE id_matricula=? AND situacao='DEFERIDO' AND (decidido_em IS NULL OR decidido_em>CURRENT_TIMESTAMP(6)) FOR SHARE",
                    r->r.getLong(1),matricula).isEmpty()) throw new IllegalArgumentException("Reconhecimento sem data válida; conciliação necessária.");
            // Bloqueia ofertas antes dos períodos, como o fluxo de inscrição. Consulta conservadora.
            var turmas=q("SELECT id_turma FROM turma ORDER BY id_turma FOR SHARE",r->r.getLong(1));
            var periodos=new LinkedHashMap<Long,Periodo>();
            q("SELECT * FROM periodo_letivo ORDER BY id_periodo FOR SHARE",RepositorioPlanejamentoJdbc::periodo).forEach(p->periodos.put(p.id(),p));
            if(!periodos.keySet().containsAll(selecionados)) throw new IllegalArgumentException("Período selecionado não encontrado.");
            var disciplinas=new ArrayList<Disciplina>();
            for(var item:plano.itens()) {
                var cadastro=q("SELECT carga_horaria,situacao FROM disciplina WHERE id_disciplina=? FOR SHARE",
                        r->new Object[]{r.getInt(1),r.getString(2)},item.disciplina()).getFirst();
                var requisitos=q("SELECT id_disciplina_prerequisito FROM disciplina_prerequisito WHERE id_disciplina=? ORDER BY id_disciplina_prerequisito FOR SHARE",r->r.getLong(1),item.disciplina());
                disciplinas.add(new Disciplina(item.disciplina(),item.nome(),(int)cadastro[0],item.situacao(),(String)cadastro[1],requisitos));
            }
            var inscricoes=q("SELECT i.id_turma,i.situacao FROM inscricao_disciplina i JOIN matricula_academica m ON m.id_matricula=i.id_matricula WHERE m.id_aluno=? ORDER BY i.id_inscricao FOR SHARE",
                    r->new Object[]{r.getLong(1),r.getString(2)},contexto[0]);
            if(inscricoes.stream().anyMatch(i->!Set.of("ATIVO","CANCELADO","APROVADO","REPROVADO").contains((String)i[1])))
                throw new IllegalArgumentException("Inscrição do aluno com situação desconhecida; conciliação necessária.");
            var ativas=new ArrayList<Oferta>();
            for(var i:inscricoes) if("ATIVO".equals(i[1])) ativas.add(oferta((long)i[0],periodos));
            var idsDisciplinas=new HashSet<Long>(); disciplinas.forEach(d->idsDisciplinas.add(d.id()));
            var ofertas=new ArrayList<Oferta>();
            for(long id:turmas) {
                var meta=q("SELECT id_disciplina,id_periodo FROM turma WHERE id_turma=? FOR SHARE",r->new long[]{r.getLong(1),r.getLong(2)},id).getFirst();
                if(idsDisciplinas.contains(meta[0]) && selecionados.contains(meta[1])) ofertas.add(oferta(id,periodos));
            }
            LocalDate hoje=q("SELECT UTC_TIMESTAMP(6)",r->r.getObject(1,LocalDateTime.class).atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("America/Sao_Paulo")).toLocalDate()).getFirst();
            return Planejamento.distribuir(new Dados(matricula,plano.estrutura(),hoje,disciplinas,ofertas,ativas),prioridade);
        });
    }
}

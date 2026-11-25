package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Referências para navegação; identidade e alcance vêm da sessão, não de IDs de pessoa enviados. */
public final class RepositorioNavegacaoAcademicaJdbc {
    public record TurmaDocente(long turma,String codigo,String disciplina,long periodo,String descricaoPeriodo,LocalDate inicio,LocalDate fim) { }
    public record InscricaoVisivel(long inscricao,long matricula,long turma,String disciplina,String identificacao,String situacao) { }
    public record OfertaVisivel(long turma,String codigo,String disciplina,long periodo,String descricaoPeriodo,br.edu.cefetmg.scoa.dominio.Elegibilidade elegibilidade) { }
    private final Connection c;
    private final SegurancaJdbc.Sessao sessao;
    public RepositorioNavegacaoAcademicaJdbc(Connection c,SegurancaJdbc.Sessao sessao) { this.c=Objects.requireNonNull(c); this.sessao=sessao; }
    public List<TurmaDocente> turmasDocente() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigirSessao(c,sessao);
            return q(c,"""
                    SELECT t.id_turma,t.codigo,d.nome,p.id_periodo,p.descricao,p.data_inicio,p.data_fim
                    FROM turma t JOIN professor pr ON pr.id_professor=t.id_professor
                    JOIN disciplina d ON d.id_disciplina=t.id_disciplina JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                    WHERE pr.id_usuario=? AND pr.situacao='ATIVO' ORDER BY p.data_inicio DESC,t.codigo,t.id_turma FOR SHARE
                    """,r->new TurmaDocente(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getString(5),r.getObject(6,LocalDate.class),r.getObject(7,LocalDate.class)),autor);
        });
    }
    public List<InscricaoVisivel> inscricoesDocente(long turma) {
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(turma);
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigirSessao(c,sessao);
            if(q(c,"SELECT t.id_turma FROM turma t JOIN professor pr ON pr.id_professor=t.id_professor WHERE t.id_turma=? AND pr.id_usuario=? AND pr.situacao='ATIVO' FOR SHARE",r->r.getLong(1),turma,autor).isEmpty())
                throw new IllegalArgumentException("Turma não encontrada ou fora de sua responsabilidade docente.");
            return q(c,"""
                    SELECT i.id_inscricao,i.id_matricula,t.id_turma,d.nome,a.identificacao,i.situacao
                    FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma JOIN disciplina d ON d.id_disciplina=t.id_disciplina
                    JOIN matricula_academica m ON m.id_matricula=i.id_matricula JOIN aluno a ON a.id_aluno=m.id_aluno
                    WHERE t.id_turma=? ORDER BY a.identificacao,i.id_inscricao FOR SHARE
                    """,RepositorioNavegacaoAcademicaJdbc::inscricao,turma);
        });
    }
    public List<InscricaoVisivel> inscricoesAluno() {
        return TransacaoJdbc.executar(c,()->{
            controle(c); long autor=SegurancaJdbc.exigirSessao(c,sessao);
            return q(c,"""
                    SELECT i.id_inscricao,i.id_matricula,t.id_turma,d.nome,a.identificacao,i.situacao
                    FROM inscricao_disciplina i JOIN turma t ON t.id_turma=i.id_turma JOIN disciplina d ON d.id_disciplina=t.id_disciplina
                    JOIN matricula_academica m ON m.id_matricula=i.id_matricula JOIN aluno a ON a.id_aluno=m.id_aluno
                    WHERE a.id_usuario=? ORDER BY i.id_inscricao DESC FOR SHARE
                    """,RepositorioNavegacaoAcademicaJdbc::inscricao,autor);
        });
    }
    public List<OfertaVisivel> ofertasAluno(long matricula) {
        br.edu.cefetmg.scoa.dominio.HorarioAula.validarId(matricula);
        return TransacaoJdbc.executar(c,()->{
            controle(c);long autor=SegurancaJdbc.exigirSessao(c,sessao);
            if(q(c,"SELECT m.id_matricula FROM matricula_academica m JOIN aluno a ON a.id_aluno=m.id_aluno WHERE m.id_matricula=? AND a.id_usuario=? FOR SHARE",r->r.getLong(1),matricula,autor).isEmpty())
                throw new IllegalArgumentException("Matrícula não encontrada ou não autorizada.");
            var referencias=q(c,"""
                    SELECT t.id_turma,t.codigo,d.nome,p.id_periodo,p.descricao FROM turma t
                    JOIN disciplina d ON d.id_disciplina=t.id_disciplina JOIN periodo_letivo p ON p.id_periodo=t.id_periodo
                    JOIN estrutura_disciplina ed ON ed.id_disciplina=t.id_disciplina
                    JOIN vinculo_curricular_matricula v ON v.id_estrutura=ed.id_estrutura
                    WHERE v.id_matricula=? AND p.data_fim>=? ORDER BY p.data_inicio,t.id_turma FOR SHARE
                    """,r->new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getString(5)},matricula,dataAcademica(agoraUtc(c)));
            var resultado=new ArrayList<OfertaVisivel>();
            for(var t:referencias) resultado.add(new OfertaVisivel((long)t[0],(String)t[1],(String)t[2],(long)t[3],(String)t[4],
                    RepositoriosAcademicosAutorizados.inscricoes(c,sessao).avaliar(matricula,(long)t[0])));
            return List.copyOf(resultado);
        });
    }
    public List<br.edu.cefetmg.scoa.dominio.CadastroPeriodoLetivo> periodosAluno() {
        return TransacaoJdbc.executar(c,()->{
            controle(c);long autor=SegurancaJdbc.exigirSessao(c,sessao);
            if(q(c,"SELECT id_aluno FROM aluno WHERE id_usuario=? FOR SHARE",r->r.getLong(1),autor).isEmpty())
                throw new IllegalArgumentException("A consulta do calendário exige vínculo de aluno.");
            return new RepositorioCadastroAcademicoJdbc(c).listarPeriodos();
        });
    }
    private static InscricaoVisivel inscricao(ResultSet r) throws SQLException { return new InscricaoVisivel(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6)); }
}

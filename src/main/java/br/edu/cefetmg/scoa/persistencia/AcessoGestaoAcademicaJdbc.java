package br.edu.cefetmg.scoa.persistencia;

import java.sql.*;
import java.util.*;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

/** Gestor institucional explícito ou coordenação do curso. ADMIN técnico não substitui esses vínculos. */
final class AcessoGestaoAcademicaJdbc {
    private final Connection c; private final SegurancaJdbc.Sessao sessao;
    AcessoGestaoAcademicaJdbc(Connection c,SegurancaJdbc.Sessao sessao) { this.c=c; this.sessao=sessao; }
    private boolean global(long usuario) throws SQLException {
        return !q(c,"SELECT id_gestor FROM gestao_academica WHERE id_usuario=? AND situacao='ATIVO' FOR SHARE",r->r.getLong(1),usuario).isEmpty();
    }
    long curso(long curso) throws SQLException {
        HorarioAula.validarId(curso); long usuario=SegurancaJdbc.exigirSessao(c,sessao);
        if(q(c,"SELECT id_curso FROM curso WHERE id_curso=? FOR SHARE",r->r.getLong(1),curso).isEmpty() || (!global(usuario) && !SegurancaJdbc.coordena(c,usuario,curso)))
            throw new IllegalArgumentException("Curso não encontrado ou fora do seu acompanhamento acadêmico.");
        return usuario;
    }
    List<Long> cursos() throws SQLException {
        long usuario=SegurancaJdbc.exigirSessao(c,sessao);
        if(global(usuario)) return q(c,"SELECT id_curso FROM curso ORDER BY id_curso FOR SHARE",r->r.getLong(1));
        return q(c,"SELECT id_curso FROM coordenacao_curso WHERE id_usuario=? ORDER BY id_curso FOR SHARE",r->r.getLong(1),usuario);
    }
    static final String TURMA_NO_CURSO="""
            (EXISTS (SELECT 1 FROM estrutura_curricular ec JOIN estrutura_disciplina ed ON ed.id_estrutura=ec.id_estrutura
                WHERE ec.id_curso=? AND ed.id_disciplina=t.id_disciplina)
             OR EXISTS (SELECT 1 FROM inscricao_disciplina ii JOIN matricula_academica mm ON mm.id_matricula=ii.id_matricula
                WHERE ii.id_turma=t.id_turma AND mm.id_curso=?))
            """;
    long turma(long curso,long turma) throws SQLException {
        long autor=curso(curso); HorarioAula.validarId(turma);
        if(q(c,"SELECT t.id_turma FROM turma t WHERE t.id_turma=? AND "+TURMA_NO_CURSO+" FOR SHARE",r->r.getLong(1),turma,curso,curso).isEmpty())
            throw new IllegalArgumentException("Turma não encontrada no curso autorizado.");
        return autor;
    }
    long docente(long curso,long professor) throws SQLException {
        long autor=curso(curso); HorarioAula.validarId(professor);
        if(q(c,"SELECT id_professor FROM professor WHERE id_professor=? FOR SHARE",r->r.getLong(1),professor).isEmpty())
            throw new IllegalArgumentException("Docente não encontrado.");
        if(!global(autor) && q(c,"SELECT t.id_turma FROM turma t WHERE t.id_professor=? AND "+TURMA_NO_CURSO+" LIMIT 1 FOR SHARE",r->r.getLong(1),professor,curso,curso).isEmpty()
                && q(c,"SELECT id_prazo_meta FROM prazo_meta_docente WHERE id_curso=? AND id_professor=? AND id_usuario_autor IS NOT NULL LIMIT 1 FOR SHARE",r->r.getLong(1),curso,professor).isEmpty())
            throw new IllegalArgumentException("Docente fora do acompanhamento do seu curso.");
        return autor;
    }
}

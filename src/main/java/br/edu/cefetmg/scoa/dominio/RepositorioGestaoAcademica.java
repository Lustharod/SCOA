package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.GestaoAcademica.*;

public interface RepositorioGestaoAcademica {
    List<CursoDisponivel> cursos();
    List<Docente> docentes(long curso);
    Meta registrarMeta(NovaMeta pedido);
    Meta atualizarMeta(AtualizarMeta pedido);
    List<Meta> metas(long curso,Long professor);
    List<HistoricoMeta> historicoMeta(long meta);
    List<AvaliacaoGestao> avaliacoes(Filtro filtro);
    Diario diario(long curso,long turma);
    Auditoria auditar(NovaAuditoria pedido);
    List<Auditoria> auditorias(long curso,long turma);
    List<Monitoramento> monitorar(Filtro filtro);
}

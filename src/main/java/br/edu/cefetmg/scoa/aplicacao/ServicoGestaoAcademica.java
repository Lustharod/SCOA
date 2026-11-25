package br.edu.cefetmg.scoa.aplicacao;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.GestaoAcademica.*;

public final class ServicoGestaoAcademica {
    private final RepositorioGestaoAcademica repo;
    public ServicoGestaoAcademica(RepositorioGestaoAcademica repo) { this.repo=Objects.requireNonNull(repo); }
    public List<CursoDisponivel> cursos() { return repo.cursos(); }
    public List<Docente> docentes(long curso) { return repo.docentes(curso); }
    public Meta registrarMeta(NovaMeta p) { return repo.registrarMeta(p); }
    public Meta atualizarMeta(AtualizarMeta p) { return repo.atualizarMeta(p); }
    public List<Meta> metas(long curso,Long professor) { return repo.metas(curso,professor); }
    public List<HistoricoMeta> historicoMeta(long meta) { return repo.historicoMeta(meta); }
    public List<AvaliacaoGestao> avaliacoes(Filtro f) { return repo.avaliacoes(f); }
    public Diario diario(long curso,long turma) { return repo.diario(curso,turma); }
    public Auditoria auditar(NovaAuditoria p) { return repo.auditar(p); }
    public List<Auditoria> auditorias(long curso,long turma) { return repo.auditorias(curso,turma); }
    public List<Monitoramento> monitorar(Filtro f) { return repo.monitorar(f); }
}

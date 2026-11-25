package br.edu.cefetmg.scoa.aplicacao;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;

public final class ServicoParametrosAcademicos {
    private final RepositorioParametrosAcademicos repo;
    public ServicoParametrosAcademicos(RepositorioParametrosAcademicos repo) { this.repo=Objects.requireNonNull(repo); }
    public List<Versao> versoes() { return repo.versoes(); }
    public Versao criar(Criar pedido) { return repo.criar(pedido); }
    public Periodo consultarPeriodo(long periodo) { return repo.consultarPeriodo(periodo); }
    public Periodo vincular(Vincular pedido) { return repo.vincular(pedido); }
    public List<Historico> historico(long periodo) { return repo.historico(periodo); }
}

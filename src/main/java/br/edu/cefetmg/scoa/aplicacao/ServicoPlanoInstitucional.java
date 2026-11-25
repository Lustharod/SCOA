package br.edu.cefetmg.scoa.aplicacao;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;

public final class ServicoPlanoInstitucional {
    private final RepositorioPlanoInstitucional repo;
    public ServicoPlanoInstitucional(RepositorioPlanoInstitucional repo) { this.repo=Objects.requireNonNull(repo); }
    public Previa preparar(Selecao p) { return repo.preparar(p); }
    public Versao registrar(Previa p,String motivo) { return repo.registrar(new Registro(p.selecao(),p.versaoAnterior(),p.assinatura(),motivo)); }
    public Optional<Consulta> atualSecretaria(long matricula) { return repo.atualSecretaria(matricula); }
    public Optional<Consulta> atualAluno(long matricula) { return repo.atualAluno(matricula); }
    public List<Versao> historicoSecretaria(long matricula) { return repo.historicoSecretaria(matricula); }
    public List<Versao> historicoAluno(long matricula) { return repo.historicoAluno(matricula); }
}

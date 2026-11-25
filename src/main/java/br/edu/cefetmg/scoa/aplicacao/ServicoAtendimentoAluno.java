package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RepositorioAtendimentoAluno;
import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import java.util.List;
import java.util.Objects;

public final class ServicoAtendimentoAluno {
    private final RepositorioAtendimentoAluno repo;
    public ServicoAtendimentoAluno(RepositorioAtendimentoAluno repo) { this.repo=Objects.requireNonNull(repo); }
    public void verificarAcesso() { repo.verificarAcesso(); }
    public Historico historico() { return repo.historico(); }
    public Manifestacao registrar(TipoManifestacao tipo,String conteudo) {
        return repo.registrar(new NovaManifestacao(tipo,conteudo));
    }
    public List<Manifestacao> manifestacoes() { return repo.manifestacoes(); }
}

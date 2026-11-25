package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RematriculaLote.*;
import java.util.Objects;

public final class ServicoRematriculaLote {
    private final RepositorioRematriculaLote repo;
    public ServicoRematriculaLote(RepositorioRematriculaLote repo) { this.repo=Objects.requireNonNull(repo); }
    public Previa preparar(Selecao s) { return repo.preparar(s); }
    public Resultado executar(Previa p,String motivo) { return repo.executar(new Pedido(p.selecao(),p.chave(),p.assinatura(),motivo)); }
    public Resultado consultar(long lote) { return repo.consultar(lote); }
}

package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.AtendimentoAluno.*;
import java.util.List;

public interface RepositorioAtendimentoAluno {
    void verificarAcesso();
    Historico historico();
    Manifestacao registrar(NovaManifestacao pedido);
    List<Manifestacao> manifestacoes();
}

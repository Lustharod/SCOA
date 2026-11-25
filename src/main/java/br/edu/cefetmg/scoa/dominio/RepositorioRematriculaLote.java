package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.RematriculaLote.*;
public interface RepositorioRematriculaLote {
    Previa preparar(Selecao selecao);
    Resultado executar(Pedido pedido);
    Resultado consultar(long lote);
}

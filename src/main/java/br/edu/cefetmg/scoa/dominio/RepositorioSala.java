package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface RepositorioSala {
    /** Insere uma sala nova, ainda sem ID. Alterações usam o método atômico abaixo. */
    Sala salvar(Sala sala);
    /** Consulta e altera o registro de forma indivisível em relação às outras alterações. */
    Sala alterar(long id, UnaryOperator<Sala> modificacao);
    Optional<Sala> buscarPorId(long id);
    List<Sala> listar();
}

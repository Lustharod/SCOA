package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import java.util.Optional;

public interface RepositorioSala {
    Sala salvar(Sala sala);
    Optional<Sala> buscarPorId(long id);
    List<Sala> listar();
}

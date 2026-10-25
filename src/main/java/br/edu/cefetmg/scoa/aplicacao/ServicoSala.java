package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RepositorioSala;
import br.edu.cefetmg.scoa.dominio.Sala;
import br.edu.cefetmg.scoa.dominio.SalaNaoEncontradaException;
import java.util.List;
import java.util.Objects;

/** Coordena o cadastro e utiliza alterações atômicas para preservar dados concorrentes. */
public final class ServicoSala {
    private final RepositorioSala repositorio;

    public ServicoSala(RepositorioSala repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "Informe um repositório de salas.");
    }

    public Sala cadastrar(String identificacao, String localizacao, int capacidade) {
        return repositorio.salvar(Sala.nova(identificacao, localizacao, capacidade));
    }

    public Sala buscar(long id) {
        Sala.validarId(id);
        return repositorio.buscarPorId(id).orElseThrow(() -> new SalaNaoEncontradaException(id));
    }

    public List<Sala> listar() {
        return List.copyOf(repositorio.listar());
    }

    public Sala atualizar(long id, String identificacao, String localizacao, int capacidade) {
        Sala.validarId(id);
        return repositorio.alterar(id, atual ->
                new Sala(id, identificacao, localizacao, capacidade, atual.getSituacao()));
    }

    public Sala inativar(long id) {
        Sala.validarId(id);
        return repositorio.alterar(id, atual -> new Sala(id, atual.getIdentificacao(), atual.getLocalizacao(),
                atual.getCapacidade(), "INATIVO"));
    }
}

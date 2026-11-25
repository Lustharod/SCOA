package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RepositorioTransferenciaInterna;
import br.edu.cefetmg.scoa.dominio.TransferenciaInterna.*;
import java.util.List;
import java.util.Objects;

public final class ServicoTransferenciaInterna {
    private final RepositorioTransferenciaInterna repo;
    public ServicoTransferenciaInterna(RepositorioTransferenciaInterna repo) { this.repo=Objects.requireNonNull(repo); }
    public Registro efetivar(long origem,long curso,long estrutura,long periodo,String motivo) {
        return repo.efetivar(new Pedido(origem,curso,estrutura,periodo,motivo));
    }
    public List<Registro> listar(long origem) { return repo.listar(origem); }
}

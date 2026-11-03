package br.edu.cefetmg.scoa.aplicacao;

import java.util.List;
import java.util.Objects;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;

public final class ServicoEquivalencia {
    private final RepositorioEquivalencia repo;
    public ServicoEquivalencia(RepositorioEquivalencia repo) { this.repo=Objects.requireNonNull(repo); }
    public Registro solicitar(Pedido pedido) { return repo.solicitar(Objects.requireNonNull(pedido)); }
    public Registro decidir(long pedido, Decisao decisao) {
        HorarioAula.validarId(pedido); return repo.decidir(pedido,Objects.requireNonNull(decisao));
    }
    public List<Registro> listar(long matricula) { return repo.listar(matricula); }
    public PlanoBasico plano(long matricula) { return repo.plano(matricula); }
    public List<Evento> historico(long matricula) { return repo.historico(matricula); }
}

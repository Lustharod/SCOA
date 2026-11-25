package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.RepositorioTransferenciaExterna;
import br.edu.cefetmg.scoa.dominio.TransferenciaExterna.*;
import java.util.List;
import java.util.Objects;

public final class ServicoTransferenciaExterna {
    private final RepositorioTransferenciaExterna repo;
    public ServicoTransferenciaExterna(RepositorioTransferenciaExterna repo) { this.repo=Objects.requireNonNull(repo); }
    public Processo registrar(Pedido pedido) { return repo.registrar(pedido); }
    public Processo consultar(long protocolo) { return repo.consultar(protocolo); }
    public List<Processo> listar(long aluno) { return repo.listar(aluno); }
    public List<Andamento> historico(long protocolo) { return repo.historico(protocolo); }
    public Processo atualizar(long protocolo,Atualizacao atualizacao) { return repo.atualizar(protocolo,atualizacao); }
}

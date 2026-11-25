package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DocumentoAcademico.*;
import java.util.List;
import java.util.Objects;

public final class ServicoDocumentoAcademico {
    private final RepositorioDocumentoAcademico repo;
    public ServicoDocumentoAcademico(RepositorioDocumentoAcademico repo) { this.repo=Objects.requireNonNull(repo); }
    public List<AtendimentoAluno.Vinculo> minhasMatriculas() { return repo.minhasMatriculas(); }
    public Registro solicitar(long matricula,Tipo tipo) { return repo.solicitar(new Novo(matricula,tipo)); }
    public List<Registro> minhasSolicitacoes() { return repo.minhasSolicitacoes(); }
    public List<Andamento> meuAndamento(long protocolo) { return repo.meuAndamento(protocolo); }
    public List<Registro> filaSecretaria() { return repo.filaSecretaria(); }
    public List<Andamento> andamentoSecretaria(long protocolo) { return repo.andamentoSecretaria(protocolo); }
    public Registro atualizar(Atualizacao pedido) { return repo.atualizar(pedido); }
}

package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class ServicoInscricao {
    private final RepositorioInscricao repo;
    public ServicoInscricao(RepositorioInscricao repo) { this.repo = Objects.requireNonNull(repo); }
    public Elegibilidade propor(long matricula, long turma) { return repo.avaliar(matricula, turma); }
    public Inscricao confirmar(long matricula, long turma) { return repo.inscrever(matricula, turma); }
    public List<Inscricao> listar(long matricula) { return repo.listar(matricula); }
    public void vincularEstrutura(long matricula, long estrutura) { repo.vincularEstrutura(matricula, estrutura); }
    public RegraResultado.Resultado registrarResultado(long inscricao, BigDecimal p1, BigDecimal p2,
            BigDecimal pf, long total, long presentes, String motivo) {
        return repo.registrarResultado(inscricao, p1, p2, pf, total, presentes, motivo);
    }
}

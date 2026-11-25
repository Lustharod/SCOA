package br.edu.cefetmg.scoa.aplicacao;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.DiarioAcademico.*;

public final class ServicoDiarioAcademico {
    private final RepositorioDiarioAcademico repo;
    public ServicoDiarioAcademico(RepositorioDiarioAcademico repo) { this.repo=Objects.requireNonNull(repo); }
    public List<Aula> registrarAulas(NovaAula p) { return repo.registrarAulas(p); }
    public List<Aula> aulas(long turma) { return repo.aulas(turma); }
    public Avaliacao registrarAvaliacao(NovaAvaliacao p) { return repo.registrarAvaliacao(p); }
    public List<Avaliacao> avaliacoes(long turma) { return repo.avaliacoes(turma); }
    public void lancarNota(LancarNota p) { repo.lancarNota(p); }
    public void retirarNota(RetirarNota p) { repo.retirarNota(p); }
    public void lancarFrequencia(LancarFrequencia p) { repo.lancarFrequencia(p); }
    public Boletim consultarDocente(long inscricao) { return repo.consultarDocente(inscricao); }
    public Boletim consultarAluno(long inscricao) { return repo.consultarAluno(inscricao); }
    public RegraResultado.Resultado fechar(long inscricao,String motivo) { return repo.fechar(inscricao,motivo); }
    public List<Processamento> importarSimulados(List<Chegada> eventos) { return repo.importarSimulados(List.copyOf(eventos)); }
}

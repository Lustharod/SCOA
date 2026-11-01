package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import java.time.LocalDate;
import java.util.*;

public final class ServicoMatricula {
    private final RepositorioMatricula repo;
    public ServicoMatricula(RepositorioMatricula repo) { this.repo = Objects.requireNonNull(repo); }
    public Matricula criar(long aluno, long curso, LocalDate data, long periodo, String motivo) {
        return repo.criar(aluno, curso, data, periodo, motivo);
    }
    public Matricula buscar(long id) {
        HorarioAula.validarId(id);
        return repo.buscar(id).orElseThrow(() -> new IllegalArgumentException("Matrícula não encontrada."));
    }
    public List<Matricula> listar(long aluno) { HorarioAula.validarId(aluno); return List.copyOf(repo.listar(aluno)); }
    public Matricula corrigir(long id, LocalDate data, long periodo, String motivo) { return repo.corrigir(id,data,periodo,motivo); }
    public Matricula trancar(long id, String motivo) { return repo.transicionar(id,"TRANCADO",motivo); }
    public Matricula retornar(long id, String motivo) { return repo.transicionar(id,"ATIVO",motivo); }
    public Matricula cancelar(long id, String motivo) { return repo.transicionar(id,"CANCELADO",motivo); }
    public List<EventoMatricula> historico(long id) { buscar(id); return List.copyOf(repo.historico(id)); }
    public SituacaoBiblioteca consultarBiblioteca(long aluno) { HorarioAula.validarId(aluno); return repo.consultarBiblioteca(aluno); }
}

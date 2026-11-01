package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepositorioMatricula {
    Matricula criar(long aluno, long curso, LocalDate data, long periodo, String motivo);
    Optional<Matricula> buscar(long id);
    List<Matricula> listar(long aluno);
    Matricula corrigir(long id, LocalDate data, long periodo, String motivo);
    Matricula transicionar(long id, String destino, String motivo);
    List<EventoMatricula> historico(long id);
    SituacaoBiblioteca consultarBiblioteca(long aluno);
}

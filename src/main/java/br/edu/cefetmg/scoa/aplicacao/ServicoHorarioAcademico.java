package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.HorarioAcademico.*;
import java.util.*;

public final class ServicoHorarioAcademico {
    private final RepositorioHorarioAcademico repo;
    public ServicoHorarioAcademico(RepositorioHorarioAcademico repo) { this.repo=Objects.requireNonNull(repo); }
    public List<Registro> planejar(Planejar pedido) { return repo.planejar(pedido); }
    public List<Registro> listar(long turma) { return repo.listar(turma); }
    public List<Sala> salasDisponiveis(long horario) { return repo.salasDisponiveis(horario); }
    public Registro atribuirSala(long horario,int versao,long sala,String motivo) { return repo.atribuirSala(horario,versao,sala,motivo); }
    public Registro retirarSala(long horario,int versao,String motivo) { return repo.retirarSala(horario,versao,motivo); }
    public Registro cancelar(long horario,int versao,String motivo) { return repo.cancelar(horario,versao,motivo); }
    public List<Evento> historico(long horario) { return repo.historico(horario); }
}

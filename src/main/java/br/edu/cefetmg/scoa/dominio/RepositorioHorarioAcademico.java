package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.HorarioAcademico.*;

public interface RepositorioHorarioAcademico {
    List<Registro> planejar(Planejar pedido);
    List<Registro> listar(long turma);
    List<Sala> salasDisponiveis(long horario);
    Registro atribuirSala(long horario,int versao,long sala,String motivo);
    Registro retirarSala(long horario,int versao,String motivo);
    Registro cancelar(long horario,int versao,String motivo);
    List<Evento> historico(long horario);
}

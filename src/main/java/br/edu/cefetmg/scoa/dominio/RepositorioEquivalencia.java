package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.Equivalencia.*;

public interface RepositorioEquivalencia {
    Registro solicitar(Pedido pedido);
    Registro decidir(long pedido, Decisao decisao);
    List<Registro> listar(long matricula);
    PlanoBasico plano(long matricula);
    List<Evento> historico(long matricula);
}

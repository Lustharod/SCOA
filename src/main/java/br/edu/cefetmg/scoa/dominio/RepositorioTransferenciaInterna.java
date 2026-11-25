package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.TransferenciaInterna.*;
import java.util.List;

public interface RepositorioTransferenciaInterna {
    Registro efetivar(Pedido pedido);
    List<Registro> listar(long matriculaOrigem);
}

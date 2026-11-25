package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.ParametrosAcademicos.*;

public interface RepositorioParametrosAcademicos {
    List<Versao> versoes();
    Versao criar(Criar pedido);
    Periodo consultarPeriodo(long periodo);
    Periodo vincular(Vincular pedido);
    List<Historico> historico(long periodo);
}

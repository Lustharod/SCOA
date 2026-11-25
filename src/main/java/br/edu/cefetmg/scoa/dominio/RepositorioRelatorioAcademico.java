package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.util.List;

public interface RepositorioRelatorioAcademico {
    Previa preparar(Filtro filtro);
    Registro gerar(Geracao pedido);
    Registro consultar(long relatorio);
    List<Registro> meusRelatorios();
    Arquivo arquivo(long relatorio,Formato formato);
}

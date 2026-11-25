package br.edu.cefetmg.scoa.dominio;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.PlanoInstitucional.*;

public interface RepositorioPlanoInstitucional {
    Previa preparar(Selecao selecao);
    Versao registrar(Registro pedido);
    Optional<Consulta> atualSecretaria(long matricula);
    Optional<Consulta> atualAluno(long matricula);
    List<Versao> historicoSecretaria(long matricula);
    List<Versao> historicoAluno(long matricula);
}

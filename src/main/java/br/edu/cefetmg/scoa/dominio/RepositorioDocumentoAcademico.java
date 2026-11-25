package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.DocumentoAcademico.*;

public interface RepositorioDocumentoAcademico {
    List<AtendimentoAluno.Vinculo> minhasMatriculas();
    Registro solicitar(Novo pedido);
    List<Registro> minhasSolicitacoes();
    List<Andamento> meuAndamento(long protocolo);
    List<Registro> filaSecretaria();
    List<Andamento> andamentoSecretaria(long protocolo);
    Registro atualizar(Atualizacao pedido);
}

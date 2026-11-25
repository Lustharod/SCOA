package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.TransferenciaExterna.*;

public interface RepositorioTransferenciaExterna {
    Processo registrar(Pedido pedido);
    Processo consultar(long protocolo);
    List<Processo> listar(long aluno);
    List<Andamento> historico(long protocolo);
    Processo atualizar(long protocolo,Atualizacao atualizacao);
}

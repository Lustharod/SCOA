package br.edu.cefetmg.scoa.dominio;

import java.util.List;
import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico.*;

public interface RepositorioAdministracaoAcessoAcademico {
    List<Perfil> perfis();
    Perfil consultarPerfil(long id);
    Perfil criarPerfil(NovoPerfil dados,String motivo);
    Perfil alterarPerfil(AlterarPerfil dados);
    Conta consultarConta(long id);
    List<Conta> contas();
    Conta vincular(VincularPerfil dados);
    Conta alterarConta(AlterarConta dados);
    List<Evento> historicoConta(long id);
    List<Evento> historicoPerfil(long id);
}

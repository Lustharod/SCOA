package br.edu.cefetmg.scoa.aplicacao;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico.*;

public final class ServicoAdministracaoAcessoAcademico {
    private final RepositorioAdministracaoAcessoAcademico repo;
    public ServicoAdministracaoAcessoAcademico(RepositorioAdministracaoAcessoAcademico repo) { this.repo=Objects.requireNonNull(repo); }
    public List<Perfil> perfis() { return repo.perfis(); }
    public Perfil consultarPerfil(long id) { return repo.consultarPerfil(id); }
    public Perfil criarPerfil(NovoPerfil dados,String motivo) { return repo.criarPerfil(dados,motivo); }
    public Perfil alterarPerfil(AlterarPerfil dados) { return repo.alterarPerfil(dados); }
    public Conta consultarConta(long id) { return repo.consultarConta(id); }
    public List<Conta> contas() { return repo.contas(); }
    public Conta vincular(VincularPerfil dados) { return repo.vincular(dados); }
    public Conta alterarConta(AlterarConta dados) { return repo.alterarConta(dados); }
    public List<Evento> historicoConta(long id) { return repo.historicoConta(id); }
    public List<Evento> historicoPerfil(long id) { return repo.historicoPerfil(id); }
}

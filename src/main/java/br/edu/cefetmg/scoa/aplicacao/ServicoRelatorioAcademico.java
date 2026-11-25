package br.edu.cefetmg.scoa.aplicacao;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.RelatorioAcademico.*;
import java.util.*;

public final class ServicoRelatorioAcademico {
    private final RepositorioRelatorioAcademico repo;
    public ServicoRelatorioAcademico(RepositorioRelatorioAcademico repo) { this.repo=Objects.requireNonNull(repo); }
    public Previa preparar(Filtro filtro) { return repo.preparar(filtro); }
    public Registro gerar(Geracao pedido) { return repo.gerar(pedido); }
    public Registro consultar(long id) { return repo.consultar(id); }
    public List<Registro> meusRelatorios() { return repo.meusRelatorios(); }
    public Arquivo arquivo(long id,Formato formato) { return repo.arquivo(id,formato); }
}

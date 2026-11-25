package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

class AdministracaoAcessoAcademicoJdbcIT {
    private CenarioAcademicoTeste f;
    @BeforeEach void preparar() throws Exception { f=new CenarioAcademicoTeste(); }
    @AfterEach void descartar() throws Exception { if(f!=null) f.close(); }
    private RepositorioAdministracaoAcessoAcademicoJdbc repo() { return new RepositorioAdministracaoAcessoAcademicoJdbc(f.c,f.sessao(f.admin)); }
    private Perfil criar(Codigo... codigos) { return repo().criarPerfil(new NovoPerfil("Perfil sintético", "Atuação técnica acadêmica",Arrays.stream(codigos).map(Codigo::codigo).toList()),"Definição autorizada sintética"); }
    private Conta conceder(long usuario,Perfil p) { var c=repo().consultarConta(usuario); return repo().vincular(new VincularPerfil(usuario,c.versao(),p.id(),p.versao(),true,"Concessão sintética")); }
    private AlterarPerfil alteracao(Perfil p,String situacao,List<String> permissoes) { return new AlterarPerfil(p.id(),p.versao(),p.nome(),p.descricao(),situacao,permissoes,"Alteração sintética conferida"); }
    private int eventos() throws Exception { return q(f.c,"SELECT COUNT(*) FROM historico_acesso_academico",r->r.getInt(1)).getFirst(); }
    @Test void somenteAdminTecnicoAtualPodeConsultarOuManterPerfis() throws Exception {
        f.conceder(f.alunoConta,AcessoAcademicoJdbc.MANTER);
        new SegurancaJdbc(f.c).vincularGestorAcademico(f.sessao(f.admin),f.alunoConta);
        for(var sessao:Arrays.asList(null,f.sessao(f.alunoConta),f.sessao(f.docente))) {
            var repo=new RepositorioAdministracaoAcessoAcademicoJdbc(f.c,sessao);
            assertThrows(IllegalArgumentException.class,repo::perfis);
            assertThrows(IllegalArgumentException.class,()->repo.consultarConta(f.admin));
            assertThrows(IllegalArgumentException.class,()->repo.criarPerfil(new NovoPerfil("Novo","Descrição",List.of()),"Motivo"));
        }
    }
    @Test void novoPerfilTemAutoriaEHistoricoSemConcessaoAutomatica() {
        var p=criar(Codigo.CONSULTAR,Codigo.DOCUMENTOS_ATENDER);
        assertTrue(p.contas().isEmpty()); assertEquals(0,p.versao()); assertEquals(2,p.permissoes().size());
        var h=repo().historicoPerfil(p.id()); assertEquals(1,h.size()); assertNull(h.getFirst().anterior()); assertEquals(f.admin,h.getFirst().autor());
        assertFalse(h.toString().contains("scoa1$")); assertTrue(repo().perfis().contains(p));
    }
    @Test void concessaoERevogacaoAfetamSessaoAtualPreservandoContaCredencialEVinculo() throws Exception {
        String credencial=q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst();
        var sessao=f.sessao(f.alunoConta); var base=RepositoriosAcademicosAutorizados.baseAcademica(f.c,sessao);
        assertThrows(IllegalArgumentException.class,base::listarCursos);
        var perfil=criar(Codigo.CONSULTAR,Codigo.MANTER); var conta=conceder(f.alunoConta,perfil);
        assertFalse(base.listarCursos().isEmpty()); assertEquals(f.alunoConta,conta.id());
        var academica=new SegurancaJdbc(f.c).minhaConta(sessao); assertEquals("ALUNO",academica.vinculos().getFirst().tipo());
        assertFalse(academica.vinculos().stream().anyMatch(v->Set.of("GESTOR","COORDENADOR","PROFESSOR").contains(v.tipo())));
        assertEquals(credencial,q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst());
        assertEquals(f.aluno,q(f.c,"SELECT id_aluno FROM aluno WHERE id_usuario=?",r->r.getLong(1),f.alunoConta).getFirst());
        repo().vincular(new VincularPerfil(f.alunoConta,conta.versao(),perfil.id(),perfil.versao(),false,"Revogação conferida"));
        assertThrows(IllegalArgumentException.class,base::listarCursos);
        assertEquals(2,repo().historicoConta(f.alunoConta).size());
    }
    @Test void editarPerfilCompartilhadoMostraAfetadosEPermissoesSaoRevalidadas() {
        var p=criar(Codigo.CONSULTAR); conceder(f.alunoConta,p); conceder(f.outroConta,p);
        var sessao=f.sessao(f.alunoConta); var base=RepositoriosAcademicosAutorizados.baseAcademica(f.c,sessao);
        assertFalse(base.listarCursos().isEmpty()); var antes=repo().consultarPerfil(p.id());
        assertEquals(List.of(f.alunoConta,f.outroConta),antes.contas());
        var depois=repo().alterarPerfil(alteracao(antes,"ATIVO",List.of(Codigo.MANTER.codigo())));
        assertEquals(1,depois.versao()); assertEquals(antes.contas(),depois.contas());
        assertThrows(IllegalArgumentException.class,base::listarCursos);
        assertNotNull(base.inserirCurso(new Curso(null,"Curso sintético autorizado","ATIVO")).id());
        repo().alterarPerfil(alteracao(depois,"INATIVO",depois.permissoes()));
        assertThrows(IllegalArgumentException.class,()->base.inserirCurso(new Curso(null,"Recusado","ATIVO")));
    }
    @Test void revogarUmPerfilPreservaOutrasConcessoesLegitimas() {
        var leitura=criar(Codigo.CONSULTAR); var escrita=criar(Codigo.MANTER);
        conceder(f.alunoConta,leitura); var conta=conceder(f.alunoConta,escrita);
        var depois=repo().vincular(new VincularPerfil(f.alunoConta,conta.versao(),leitura.id(),leitura.versao(),false,"Apenas leitura revogada"));
        assertEquals(List.of(escrita.id()),depois.perfis()); assertEquals(List.of(Codigo.MANTER.codigo()),depois.permissoesEfetivas());
    }
    @Test void versaoAntigaNaoSobrescrevePerfilContaOuConcessao() {
        var perfil=criar(Codigo.CONSULTAR); var anterior=repo().consultarConta(f.alunoConta);
        var conta=conceder(f.alunoConta,perfil); var alterado=repo().alterarPerfil(alteracao(perfil,"ATIVO",List.of(Codigo.MANTER.codigo())));
        assertThrows(IllegalArgumentException.class,()->repo().alterarPerfil(alteracao(perfil,"INATIVO",List.of())));
        assertThrows(IllegalArgumentException.class,()->repo().vincular(new VincularPerfil(f.alunoConta,anterior.versao(),perfil.id(),alterado.versao(),false,"Tela antiga")));
        assertThrows(IllegalArgumentException.class,()->repo().alterarConta(new AlterarConta(f.alunoConta,anterior.versao(),"INATIVO","Tela antiga")));
        assertEquals(conta.perfis(),repo().consultarConta(f.alunoConta).perfis());
    }
    @ParameterizedTest @ValueSource(strings={"conta","revogar","perfil"})
    void ultimaContaAdministrativaNaoPodeSerDesabilitadaIndiretamente(String acao) throws Exception {
        var conta=repo().consultarConta(f.admin); var perfil=repo().consultarPerfil(conta.perfis().getFirst()); int antes=eventos();
        assertThrows(IllegalArgumentException.class,()->{
            switch(acao) {
                case "conta" -> repo().alterarConta(new AlterarConta(f.admin,conta.versao(),"INATIVO","Tentativa sintética"));
                case "revogar" -> repo().vincular(new VincularPerfil(f.admin,conta.versao(),perfil.id(),perfil.versao(),false,"Tentativa sintética"));
                default -> repo().alterarPerfil(alteracao(perfil,"ATIVO",List.of()));
            }
        });
        assertEquals(conta,repo().consultarConta(f.admin)); assertEquals(perfil,repo().consultarPerfil(perfil.id())); assertEquals(antes,eventos());
    }
    @Test void outraContaAdministrativaAtivaPermiteRevogacaoSemPerderHistorico() {
        var admin=criar(Codigo.ADMIN); conceder(f.outroConta,admin);
        var inicial=repo().consultarConta(f.admin); var p=repo().consultarPerfil(inicial.perfis().getFirst()); var sessaoAntiga=f.sessao(f.admin);
        repo().vincular(new VincularPerfil(f.admin,inicial.versao(),p.id(),p.versao(),false,"Administração assumida pela outra conta"));
        assertThrows(IllegalArgumentException.class,()->new RepositorioAdministracaoAcessoAcademicoJdbc(f.c,sessaoAntiga).perfis());
        var outro=new RepositorioAdministracaoAcessoAcademicoJdbc(f.c,f.sessao(f.outroConta));
        assertFalse(outro.perfis().isEmpty()); assertEquals(1,outro.historicoConta(f.admin).size());
    }
    @Test void inativacaoPreservaCredenciaisEConcessoesERevogaSessaoMesmoAposReativar() throws Exception {
        var p=criar(Codigo.CONSULTAR); var conta=conceder(f.alunoConta,p); var sessao=f.sessao(f.alunoConta);
        String hash=q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst();
        var inativa=repo().alterarConta(new AlterarConta(f.alunoConta,conta.versao(),"INATIVO","Acesso suspenso"));
        assertEquals(conta.perfis(),inativa.perfis()); assertTrue(inativa.permissoesEfetivas().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new SegurancaJdbc(f.c).minhaConta(sessao));
        var ativa=repo().alterarConta(new AlterarConta(f.alunoConta,inativa.versao(),"ATIVO","Acesso restabelecido"));
        assertEquals(conta.perfis(),ativa.perfis()); assertEquals(conta.permissoesEfetivas(),ativa.permissoesEfetivas());
        assertThrows(IllegalArgumentException.class,()->new SegurancaJdbc(f.c).minhaConta(sessao));
        assertEquals(f.alunoConta,new SegurancaJdbc(f.c).minhaConta(f.sessao(f.alunoConta)).idConta());
        assertEquals(hash,q(f.c,"SELECT credencial_hash FROM usuario WHERE id_usuario=?",r->r.getString(1),f.alunoConta).getFirst());
    }
    @Test void perfisDeOutroModuloSaoPreservadosParaConsultaSemEdicaoOuRevogacao() throws Exception {
        f.conceder(f.alunoConta,SegurancaJdbc.REGISTRAR_PENDENCIA);
        var conta=repo().consultarConta(f.alunoConta); var p=repo().consultarPerfil(conta.perfis().getFirst()); assertFalse(p.escopoAcademico());
        assertThrows(IllegalArgumentException.class,()->repo().alterarPerfil(alteracao(p,"INATIVO",List.of())));
        assertThrows(IllegalArgumentException.class,()->repo().vincular(new VincularPerfil(f.alunoConta,conta.versao(),p.id(),p.versao(),false,"Fora do escopo")));
        assertEquals(conta,repo().consultarConta(f.alunoConta));
    }
    @ParameterizedTest @ValueSource(strings={"criar","alterar","conceder","revogar","conta"})
    void falhaNaAuditoriaDesfazAlteracaoEHistoricoSemMudarCredenciais(String acao) throws Exception {
        var p=criar(Codigo.CONSULTAR); if("revogar".equals(acao)) conceder(f.alunoConta,p);
        var sessaoAluno=f.sessao(f.alunoConta);
        var conta=repo().consultarConta(f.alunoConta); int antes=eventos(); var perfil=repo().consultarPerfil(p.id());
        var proxy=f.falharAuditoria(); var repo=new RepositorioAdministracaoAcessoAcademicoJdbc(proxy,f.sessao(proxy,f.admin));
        assertThrows(PersistenciaException.class,()->{
            switch(acao) {
                case "criar" -> repo.criarPerfil(new NovoPerfil("Desfazer","Descrição",List.of()),"Motivo");
                case "alterar" -> repo.alterarPerfil(alteracao(perfil,"INATIVO",List.of()));
                case "conceder" -> repo.vincular(new VincularPerfil(f.alunoConta,conta.versao(),p.id(),p.versao(),true,"Motivo"));
                case "revogar" -> repo.vincular(new VincularPerfil(f.alunoConta,conta.versao(),p.id(),p.versao(),false,"Motivo"));
                default -> repo.alterarConta(new AlterarConta(f.alunoConta,conta.versao(),"INATIVO","Motivo"));
            }
        });
        assertEquals(conta,repo().consultarConta(f.alunoConta)); assertEquals(perfil,repo().consultarPerfil(p.id())); assertEquals(antes,eventos());
        assertEquals(f.alunoConta,new SegurancaJdbc(f.c).minhaConta(sessaoAluno).idConta());
        assertTrue(q(f.c,"SELECT id_perfil FROM perfil WHERE nome='Desfazer'",r->r.getLong(1)).isEmpty());
    }
    @Test void codigoDuplicadoNoLegadoNaoEEscolhidoArbitrariamente() throws Exception {
        g(f.c,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",Codigo.CONSULTAR.codigo(),"Duplicada 1");
        g(f.c,"INSERT INTO permissao(codigo,descricao) VALUES (?,?)",Codigo.CONSULTAR.codigo(),"Duplicada 2");
        assertThrows(IllegalArgumentException.class,()->criar(Codigo.CONSULTAR));
        assertTrue(q(f.c,"SELECT id_perfil FROM perfil WHERE nome='Perfil sintético'",r->r.getLong(1)).isEmpty());
    }
    @Test void legadoNaoRecebeHistoricoInventadoEContaNaoExpoeHash() {
        var conta=repo().consultarConta(f.admin); var perfil=repo().consultarPerfil(conta.perfis().getFirst());
        assertTrue(repo().historicoConta(f.admin).isEmpty()); assertTrue(repo().historicoPerfil(perfil.id()).isEmpty());
        assertFalse(conta.toString().contains("scoa1$")); assertTrue(repo().contas().stream().anyMatch(c->c.id()==f.admin));
    }
    @Test void perfilOuContaInativaNaoRecebemNovaConcessao() {
        var p=criar(Codigo.CONSULTAR); var inativo=repo().alterarPerfil(alteracao(p,"INATIVO",p.permissoes()));
        assertThrows(IllegalArgumentException.class,()->conceder(f.alunoConta,inativo));
        var conta=repo().consultarConta(f.alunoConta); repo().alterarConta(new AlterarConta(f.alunoConta,conta.versao(),"INATIVO","Acesso suspenso"));
        assertThrows(IllegalArgumentException.class,()->conceder(f.alunoConta,criar(Codigo.CONSULTAR)));
    }
}

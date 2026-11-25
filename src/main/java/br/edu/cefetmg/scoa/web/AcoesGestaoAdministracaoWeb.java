package br.edu.cefetmg.scoa.web;

import java.util.*;
import java.util.function.Predicate;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.*;
import static br.edu.cefetmg.scoa.web.AcoesAcademicasWeb.*;

final class AcoesGestaoAdministracaoWeb {
    private AcoesGestaoAdministracaoWeb() { }
    private static RepositorioGestaoAcademicaJdbc gestao(Contexto c) { return new RepositorioGestaoAcademicaJdbc(c.c(),c.sessao()); }
    private static RepositorioAdministracaoAcessoAcademicoJdbc admin(Contexto c) { return new RepositorioAdministracaoAcessoAcademicoJdbc(c.c(),c.sessao()); }
    private static RepositorioParametrosAcademicosJdbc parametros(Contexto c) { return new RepositorioParametrosAcademicosJdbc(c.c(),c.sessao()); }
    private static RepositorioProcessoAcademicoJdbc processos(Contexto c) { return new RepositorioProcessoAcademicoJdbc(c.c()); }
    private static GestaoAcademica.Filtro filtro(FormularioWeb f) { return new GestaoAcademica.Filtro(f.id("curso"),f.opcional("periodo"),f.opcional("disciplina"),f.opcional("turma"),f.opcional("aluno")); }
    private static Campo[] filtros() { return new Campo[]{id("curso","ID do curso acompanhado"),id("periodo","ID do período").facultativo(),id("disciplina","ID da disciplina").facultativo(),id("turma","ID da turma").facultativo(),id("aluno","ID do aluno").facultativo()}; }
    static void registrar(AcoesAcademicasWeb a) {
        String grupo="Coordenação e gestão acadêmica";
        a.consultar("gestao-cursos",grupo,"Cursos sob meu acompanhamento","Gestor acompanha todos os cursos; coordenador acompanha os cursos de seu vínculo.",GESTAO,(c,f)->gestao(c).cursos());
        a.consultar("gestao-docentes",grupo,"Docentes do curso","Professores visíveis no alcance acadêmico do curso selecionado.",GESTAO,(c,f)->gestao(c).docentes(f.id("curso")),id("curso","ID do curso"));
        a.consultar("gestao-metas",grupo,"Metas e prazos docentes","Situação e versões das metas do curso. Prazo vencido não altera automaticamente sua situação.",GESTAO,(c,f)->gestao(c).metas(f.id("curso"),f.opcional("professor")),id("curso","ID do curso"),id("professor","ID do professor").facultativo());
        a.alterar("gestao-nova-meta",grupo,"Registrar meta ou prazo docente","Atribuição explícita de meta ao docente, com curso, prazo e período opcional.",GESTAO,(c,f)->{
            var p=new GestaoAcademica.NovaMeta(f.id("curso"),f.id("professor"),f.opcional("periodo"),f.escolha("tipo",GestaoAcademica.TipoMeta.class),f.obrigatorio("descricao"),f.data("prazo"));gestao(c).docentes(p.curso());return gravar(p,x->gestao(x).registrarMeta(p));
        },id("curso","ID do curso"),id("professor","ID do professor"),id("periodo","ID do período").facultativo(),escolha("tipo","Tipo",GestaoAcademica.TipoMeta.class),texto("descricao","Descrição"),data("prazo","Prazo"));
        a.alterar("gestao-atualizar-meta",grupo,"Atualizar meta ou prazo","Preserva o histórico. Metas concluídas ou canceladas não são reabertas por este fluxo.",GESTAO,(c,f)->{
            var p=new GestaoAcademica.AtualizarMeta(f.id("meta"),f.numero("versao"),f.obrigatorio("descricao"),f.data("prazo"),f.escolha("situacao",GestaoAcademica.SituacaoMeta.class),f.obrigatorio("motivo"));var h=gestao(c).historicoMeta(p.id());return gravar(Map.of("Atualização",p,"Histórico",h),x->gestao(x).atualizarMeta(p));
        },id("meta","ID da meta ou prazo"),numero("versao","Versão consultada"),texto("descricao","Descrição atualizada"),data("prazo","Prazo atualizado"),escolha("situacao","Situação",GestaoAcademica.SituacaoMeta.class),texto("motivo","Motivo da atualização"));
        a.consultar("gestao-historico-meta",grupo,"Histórico de meta","Alterações, motivos e autoria preservados.",GESTAO,(c,f)->gestao(c).historicoMeta(f.id("meta")),id("meta","ID da meta ou prazo"));
        a.consultar("gestao-avaliacoes",grupo,"Acompanhar avaliações","Filtros de curso, período, disciplina, turma e aluno dentro de seu alcance.",GESTAO,(c,f)->gestao(c).avaliacoes(filtro(f)),filtros());
        a.consultar("gestao-diario",grupo,"Consultar diário do curso","Aulas e avaliações da turma; dados de alunos limitados ao curso selecionado. Distingue lançamentos originais e resultado vigente.",GESTAO,(c,f)->gestao(c).diario(f.id("curso"),f.id("turma")),id("curso","ID do curso"),id("turma","ID da turma"));
        a.alterar("gestao-auditar",grupo,"Registrar auditoria do diário","Guarda o retrato conferido, conclusão e observação. Auditoria não altera notas, frequência ou resultado.",GESTAO,(c,f)->{
            long curso=f.id("curso"),turma=f.id("turma");var d=gestao(c).diario(curso,turma);var p=new GestaoAcademica.NovaAuditoria(curso,turma,d.assinatura(),f.escolha("conclusao",GestaoAcademica.ConclusaoAuditoria.class),f.obrigatorio("observacoes"));
            return gravar(Map.of("Diário avaliado",d,"Conclusão",p.conclusao(),"Observações",p.observacoes()),x->gestao(x).auditar(p));
        },id("curso","ID do curso"),id("turma","ID da turma"),escolha("conclusao","Conclusão da auditoria",GestaoAcademica.ConclusaoAuditoria.class),texto("observacoes","Observações"));
        a.consultar("gestao-auditorias",grupo,"Auditorias registradas","Retratos avaliados e conclusões anteriores preservados.",GESTAO,(c,f)->gestao(c).auditorias(f.id("curso"),f.id("turma")),id("curso","ID do curso"),id("turma","ID da turma"));
        a.consultar("gestao-monitorar",grupo,"Monitorar trajetória discente","Situação curricular e atualidade do plano, sem presumir conclusão de curso a partir de dados insuficientes.",GESTAO,(c,f)->gestao(c).monitorar(filtro(f)),filtros());
        Predicate<SegurancaJdbc.ContaAcesso> coord=c->papel(c,"COORDENADOR");
        a.consultar("coordenacao-cancelamentos",grupo,"Pedidos de cancelamento da inscrição","Histórico restrito à coordenação do curso e ao próprio aluno.",coord,(c,f)->new RepositorioCancelamentoJdbc(c.c(),c.sessao()).historico(f.id("inscricao")),id("inscricao","ID da inscrição"));
        a.alterar("coordenacao-decidir-cancelamento",grupo,"Decidir cancelamento solicitado","Deferimento preserva o histórico e a regra da vaga ociosa; exige coordenação do curso.",coord,(c,f)->{
            long p=f.id("protocolo");boolean d=f.sim("deferir");String parecer=f.obrigatorio("parecer");return gravar(f.conferiveis(),x->new RepositorioCancelamentoJdbc(x.c(),x.sessao()).decidir(p,d,parecer));
        },id("protocolo","ID do pedido de cancelamento"),escolha("deferir","Deferir?","SIM","NAO"),texto("parecer","Parecer"));
        a.consultar("coordenacao-retificacoes",grupo,"Retificações da inscrição","Proposta docente, resultado anterior e decisão da coordenação.",coord,(c,f)->new RepositorioRetificacaoJdbc(c.c(),c.sessao()).historico(f.id("inscricao")),id("inscricao","ID da inscrição"));
        a.alterar("coordenacao-decidir-retificacao",grupo,"Decidir retificação docente","Revalida resultado anterior, prazo da próxima inscrição e dependências de resultados usados em outros processos.",coord,(c,f)->{
            long p=f.id("protocolo");boolean d=f.sim("deferir");String parecer=f.obrigatorio("parecer");return gravar(f.conferiveis(),x->concluido(()->new RepositorioRetificacaoJdbc(x.c(),x.sessao()).decidir(p,d,parecer)));
        },id("protocolo","ID do pedido de retificação"),escolha("deferir","Deferir?","SIM","NAO"),texto("parecer","Parecer"));
        a.alterar("coordenacao-sucessor",grupo,"Conferir sucessão dos períodos","Registra o próximo período do calendário do curso, preservando a exigência de encerrar retificações antes das novas inscrições.",coord,(c,f)->{
            long curso=f.id("curso"),p=f.id("periodo"),s=f.id("sucessor");return gravar(f.conferiveis(),x->concluido(()->new RepositorioRetificacaoJdbc(x.c(),x.sessao()).definirSucessor(curso,p,s)));
        },id("curso","ID do curso coordenado"),id("periodo","ID do período de origem"),id("sucessor","ID do próximo período"));
        registrarAcesso(a);registrarParametros(a);registrarRelatorios(a);registrarProcessos(a);
    }
    private static List<String> codigos(FormularioWeb f) {
        if(f.texto("permissoes").isEmpty())return List.of();return Arrays.stream(f.texto("permissoes").split("[,;\\s]+",-1)).toList();
    }
    private static void registrarAcesso(AcoesAcademicasWeb a) {
        String grupo="Administração de acesso";
        a.consultar("admin-contas",grupo,"Consultar contas de acesso","IDs de contas e perfis técnicos. As referências acadêmicas são exibidas na consulta de vínculos.",ADMIN,(c,f)->admin(c).contas());
        a.consultar("admin-conta",grupo,"Conta, vínculos e histórico","Conferência da pessoa, vínculos acadêmicos, concessões e histórico técnico.",ADMIN,(c,f)->Map.of("Conta e vínculos",new SegurancaJdbc(c.c()).consultarConta(c.sessao(),f.id("idConta")),"Concessões",admin(c).consultarConta(f.id("idConta")),"Histórico",admin(c).historicoConta(f.id("idConta"))),id("idConta","ID da conta de acesso"));
        a.alterar("admin-criar-conta",grupo,"Criar conta de acesso","Uma conta por pessoa, sem papel acadêmico selecionado aqui. Vincule aluno/professor/curso e conceda perfis separadamente.",ADMIN,(c,f)->{
            String login=f.obrigatorio("login");String senha=f.valores().getOrDefault("senha","");if(senha.length()<12||senha.length()>256)throw new IllegalArgumentException("Senha deve ter de 12 a 256 caracteres.");
            return gravar(Map.of("Identificação",login,"Acesso inicial","Sem permissões técnicas e sem vínculo acadêmico"),x->{char[] segredo=senha.toCharArray();try{return Map.of("ID da nova conta de acesso",new SegurancaJdbc(x.c()).criarConta(x.sessao(),login,segredo,"SEM_PERMISSOES"),"Identificação",login);}finally{Arrays.fill(segredo,'\0');}});
        },campo("login","Identificação da nova conta"),new Campo("senha","Senha inicial (12 a 256 caracteres)","password",false,"Não aparece na conferência nem no histórico.",List.of(),""));
        a.alterar("admin-vinculo",grupo,"Vincular conta existente ao cadastro acadêmico","ID da conta identifica a pessoa. A referência de vínculo identifica aluno, professor ou curso; GESTOR não exige referência separada.",ADMIN,(c,f)->{
            long conta=f.id("idConta");String tipo=f.obrigatorio("tipo");Long ref=f.opcional("referencia");if(!tipo.equals("GESTOR")&&ref==null)throw new IllegalArgumentException("Informe a referência acadêmica correspondente ao tipo escolhido.");
            var antes=new SegurancaJdbc(c.c()).consultarConta(c.sessao(),conta);
            return gravar(Map.of("Conta atual",antes,"Tipo do vínculo",tipo,"Referência acadêmica",Objects.toString(ref,"Vínculo institucional de gestor")),x->{var s=new SegurancaJdbc(x.c());if(tipo.equals("GESTOR"))return s.vincularGestorAcademico(x.sessao(),conta);s.vincularContaAcademica(x.sessao(),conta,tipo,ref);return s.consultarConta(x.sessao(),conta);});
        },id("idConta","ID da conta de acesso"),escolha("tipo","Tipo de vínculo","ALUNO","PROFESSOR","COORDENADOR","GESTOR"),id("referencia","ID do aluno, professor ou curso coordenado").facultativo().ajuda("Para GESTOR, deixe vazio. Não reutilize automaticamente o ID da conta."));
        a.alterar("admin-gestor",grupo,"Alterar atividade do vínculo de gestor","Preserva a conta, credencial e demais vínculos. A referência solicitada é a conta já vinculada ao gestor.",ADMIN,(c,f)->{
            long conta=f.id("idConta");String situacao=f.obrigatorio("situacao");var antes=new SegurancaJdbc(c.c()).consultarConta(c.sessao(),conta);return gravar(Map.of("Conta atual",antes,"Situação do gestor",situacao),x->concluido(()->new SegurancaJdbc(x.c()).definirSituacaoGestor(x.sessao(),conta,situacao)));
        },id("idConta","ID da conta do gestor"),escolha("situacao","Situação do vínculo de gestor","ATIVO","INATIVO"));
        a.consultar("admin-perfis",grupo,"Perfis técnicos e catálogo","Perfis compartilhados exibem contas afetadas. Códigos de outros módulos são preservados para consulta.",ADMIN,(c,f)->Map.of("Perfis",admin(c).perfis(),"Catálogo acadêmico",Arrays.stream(AdministracaoAcessoAcademico.Codigo.values()).map(v->Map.of("Código",v.codigo(),"Descrição",v.descricao())).toList()));
        a.alterar("admin-perfil",grupo,"Criar ou atualizar perfil técnico","Confira as contas afetadas antes de confirmar. Um perfil criado não é atribuído automaticamente.",ADMIN,(c,f)->{
            Long id=f.opcional("perfil");String motivo=f.obrigatorio("motivo");var dados=new AdministracaoAcessoAcademico.NovoPerfil(f.obrigatorio("nome"),f.obrigatorio("descricao"),codigos(f));
            if(id==null)return gravar(dados,x->admin(x).criarPerfil(dados,motivo));
            var antes=admin(c).consultarPerfil(id);var pedido=new AdministracaoAcessoAcademico.AlterarPerfil(id,antes.versao(),dados.nome(),dados.descricao(),f.obrigatorio("situacao"),dados.permissoes(),motivo);
            return gravar(Map.of("Perfil e contas afetadas",antes,"Atualização",pedido),x->admin(x).alterarPerfil(pedido));
        },id("perfil","ID do perfil existente").facultativo().ajuda("Vazio cria novo perfil; criação usa situação ATIVO."),campo("nome","Nome do perfil"),campo("descricao","Descrição"),escolha("situacao","Situação","ATIVO","INATIVO").padrao("ATIVO"),texto("permissoes","Códigos do catálogo, separados por vírgula").facultativo(),texto("motivo","Motivo"));
        a.alterar("admin-concessao",grupo,"Conceder ou revogar perfil da conta","Preserva outras concessões legítimas e protege a última conta administrativa ativa.",ADMIN,(c,f)->{
            var conta=admin(c).consultarConta(f.id("idConta"));var perfil=admin(c).consultarPerfil(f.id("perfil"));var p=new AdministracaoAcessoAcademico.VincularPerfil(conta.id(),conta.versao(),perfil.id(),perfil.versao(),f.sim("conceder"),f.obrigatorio("motivo"));
            return gravar(Map.of("Conta",conta,"Perfil",perfil,"Conceder",p.conceder(),"Motivo",p.motivo()),x->admin(x).vincular(p));
        },id("idConta","ID da conta de acesso"),id("perfil","ID do perfil técnico"),escolha("conceder","Conceder este perfil?","SIM","NAO"),texto("motivo","Motivo"));
        a.alterar("admin-atividade-conta",grupo,"Ativar ou inativar conta","Inativação revoga as sessões antigas. Reativar preserva credenciais e vínculos, exigindo novo login.",ADMIN,(c,f)->{
            var antes=admin(c).consultarConta(f.id("idConta"));var p=new AdministracaoAcessoAcademico.AlterarConta(antes.id(),antes.versao(),f.obrigatorio("situacao"),f.obrigatorio("motivo"));return gravar(Map.of("Conta atual",antes,"Alteração",p),x->admin(x).alterarConta(p));
        },id("idConta","ID da conta de acesso"),escolha("situacao","Situação da conta","ATIVO","INATIVO"),texto("motivo","Motivo"));
        a.consultar("admin-historico-perfil",grupo,"Histórico do perfil técnico","Valores anteriores e posteriores, motivo e autoria das alterações.",ADMIN,(c,f)->admin(c).historicoPerfil(f.id("perfil")),id("perfil","ID do perfil técnico"));
    }
    private static void registrarParametros(AcoesAcademicasWeb a) {
        String grupo="Regras acadêmicas por período";Predicate<SegurancaJdbc.ContaAcesso> acesso=c->tecnico(c,AcessoAcademicoJdbc.PARAMETROS_MANTER);
        a.consultar("parametros-versoes",grupo,"Consultar versões das regras","Criar uma versão não muda nenhum período automaticamente. Período sem seleção usa a base aprovada.",acesso,(c,f)->parametros(c).versoes());
        a.consultar("parametros-periodo",grupo,"Regras selecionadas no período","Janelas, prazos e percentuais vigentes. Períodos já iniciados preservam sua seleção.",acesso,(c,f)->parametros(c).consultarPeriodo(f.id("periodo")),id("periodo","ID do período letivo"));
        a.alterar("parametros-criar",grupo,"Criar versão das regras","Prazos em dias corridos; D+0 é o início do período. A fórmula P1/P2/PF e tempos de 50 minutos são preservados.",acesso,(c,f)->{
            var r=new ParametrosAcademicos.Regras(f.numero("antecedencia"),f.numero("duracaoPrimeira"),f.numero("inicioSegunda"),f.numero("duracaoSegunda"),f.numero("imediato"),f.numero("pedido"),f.numero("transferencia"),f.numero("retificacao"),f.numero("atraso"),f.decimal("frequencia"),f.decimal("dispensa"),f.decimal("finalMinima"));var p=new ParametrosAcademicos.Criar(r,f.obrigatorio("motivo"));return gravar(p,x->parametros(x).criar(p));
        },numero("antecedencia","Antecedência da primeira fase (dias)").padrao(14),numero("duracaoPrimeira","Duração da primeira fase (dias)").padrao(7),numero("inicioSegunda","Início da segunda fase (D+)").padrao(7),numero("duracaoSegunda","Duração da segunda fase (dias)").padrao(7),numero("imediato","Cancelamento com liberação de vaga até D+").padrao(13),numero("pedido","Pedido de cancelamento até D+").padrao(45),numero("transferencia","Transferência até D+").padrao(45),numero("retificacao","Retificação docente: dias após término").padrao(7),numero("atraso","Tolerância de atraso por tempo (minutos)").padrao(15),decimal("frequencia","Frequência mínima (%)").padrao("75.0"),decimal("dispensa","Média que dispensa PF").padrao("7.0"),decimal("finalMinima","Média mínima após PF").padrao("5.0"),texto("motivo","Motivo da nova versão"));
        a.alterar("parametros-selecionar",grupo,"Selecionar regra para período futuro","Exige período ativo ainda não iniciado. Confira os vínculos e inscrições já existentes; não serão refeitos automaticamente.",acesso,(c,f)->{
            var antes=parametros(c).consultarPeriodo(f.id("periodo"));Long versao=f.opcional("versaoParametros");var p=new ParametrosAcademicos.Vincular(antes.id(),antes.versaoVinculo(),versao,antes.assinatura(),f.obrigatorio("motivo"));
            Object nova=versao==null?ParametrosAcademicos.Regras.baseAprovada():parametros(c).versoes().stream().filter(v->v.id()==versao).findFirst().orElseThrow(()->new IllegalArgumentException("Versão não encontrada."));
            return gravar(Map.of("Período e vínculos atuais",antes,"Regras selecionadas",nova,"Motivo",p.motivo()),x->parametros(x).vincular(p));
        },id("periodo","ID do período futuro"),id("versaoParametros","ID da versão de parâmetros").facultativo().ajuda("Vazio seleciona explicitamente a base aprovada, sem adotar a última versão."),texto("motivo","Motivo da seleção"));
        a.consultar("parametros-historico",grupo,"Histórico de seleção das regras","Preserva seleção anterior, posterior e autoria por período.",acesso,(c,f)->parametros(c).historico(f.id("periodo")),id("periodo","ID do período"));
    }
    private static void registrarRelatorios(AcoesAcademicasWeb a) {
        String grupo="Relatórios e exportações";
        for(var tipo:RelatorioAcademico.Tipo.values()) {
            Predicate<SegurancaJdbc.ContaAcesso> acesso=switch(tipo){case HISTORICO_PROPRIO,PLANO_PROPRIO->ALUNO;case DIARIO_DOCENTE->DOCENTE;case DIARIO_GESTAO->GESTAO;default->c->tecnico(c,AcessoAcademicoJdbc.RELATORIOS_GERAR);};
            var campos=new ArrayList<Campo>();
            switch(tipo){case CENSO_DEMONSTRATIVO,ENADE_DEMONSTRATIVO->{campos.add(id("curso","ID do curso"));campos.add(id("periodo","ID do período"));}case DIARIO_DOCENTE->campos.add(id("turma","ID da sua turma"));case DIARIO_GESTAO->{campos.add(id("curso","ID do curso acompanhado"));campos.add(id("turma","ID da turma"));}case PLANO_PROPRIO->campos.add(id("matricula","ID da sua matrícula institucional"));default->{}}
            campos.add(campo("chave","Referência única da exportação").facultativo().ajuda("Vazio gera uma referência automaticamente. Reutilizar uma referência exige os mesmos dados da geração original."));
            String titulo=switch(tipo){case CENSO_DEMONSTRATIVO->"Censo demonstrativo";case ENADE_DEMONSTRATIVO->"ENADE demonstrativo";case DIARIO_DOCENTE->"Exportar diário da minha turma";case DIARIO_GESTAO->"Exportar diário do curso acompanhado";case HISTORICO_PROPRIO->"Exportar meu histórico";case PLANO_PROPRIO->"Exportar meu plano institucional";};
            a.alterar("relatorio-"+tipo.name().toLowerCase(Locale.ROOT).replace('_','-'),grupo,titulo,"Confira o relatório em tela antes de arquivar PDF e XLSX do mesmo retrato. Demonstrativos do protótipo não substituem documentos oficiais nem envio ao MEC.",acesso,(c,f)->{
                var filtro=new RelatorioAcademico.Filtro(tipo,f.opcional("curso"),f.opcional("periodo"),f.opcional("turma"),f.opcional("matricula"));var p=new RepositorioRelatorioAcademicoJdbc(c.c(),c.sessao()).preparar(filtro);
                if(!p.valida())throw new IllegalArgumentException(String.join(" ",p.inconsistencias()));String chave=f.texto("chave").isEmpty()?"WEB-"+UUID.randomUUID():f.texto("chave");
                return gravar(p.documento(),x->new RepositorioRelatorioAcademicoJdbc(x.c(),x.sessao()).gerar(new RelatorioAcademico.Geracao(chave,filtro,p.assinatura())));
            },campos.toArray(Campo[]::new));
        }
        a.consultar("relatorios-arquivados",grupo,"Minhas exportações arquivadas","Baixe novamente os arquivos originais. O acesso é revalidado; mudanças na origem não reescrevem relatórios anteriores.",c->true,(c,f)->new RepositorioRelatorioAcademicoJdbc(c.c(),c.sessao()).meusRelatorios());
    }
    private static void registrarProcessos(AcoesAcademicasWeb a) {
        String grupo="Conciliação e revisão acadêmica";Predicate<SegurancaJdbc.ContaAcesso> solicitar=c->tecnico(c,SegurancaJdbc.SOLICITAR),decidir=c->tecnico(c,SegurancaJdbc.DECIDIR),consultar=c->CONSULTAR.test(c)||solicitar.test(c)||decidir.test(c);
        a.consultar("processos-matricula",grupo,"Consultar processos da matrícula","Conciliações, revisões e reconhecimentos com seus estados atuais.",consultar,(c,f)->processos(c).listar(c.sessao(),f.id("matricula")),id("matricula","ID da matrícula institucional"));
        a.consultar("processos-eventos",grupo,"Histórico do processo acadêmico","Eventos preservados do processo selecionado.",consultar,(c,f)->processos(c).eventos(c.sessao(),f.obrigatorio("tipo"),f.id("protocolo")),escolha("tipo","Tipo do processo","REVISAO","CONCILIACAO","PEDIDO"),id("protocolo","ID do processo"));
        a.alterar("processos-revisar",grupo,"Solicitar revisão de reconhecimento","Suspende novos usos do reconhecimento enquanto a revisão tramita; inscrições existentes são preservadas.",solicitar,(c,f)->{
            long pedido=f.id("protocolo");String motivo=f.obrigatorio("motivo");return gravar(f.conferiveis(),x->processos(x).solicitarRevisao(x.sessao(),pedido,motivo));
        },id("protocolo","ID do pedido de equivalência deferido"),texto("motivo","Justificativa da revisão"));
        a.alterar("processos-revisao-decidir",grupo,"Analisar revisão e impactos","Confira inscrições e fundamentos. Revogar preservando identifica expressamente as inscrições existentes; não as cancela.",decidir,(c,f)->{
            long id=f.id("protocolo");String acao=f.obrigatorio("encaminhamento"),parecer=f.obrigatorio("parecer");var p=processos(c).previa(c.sessao(),id);
            return gravar(Map.of("Impactos",p,"Encaminhamento",acao,"Parecer",parecer),x->concluido(()->processos(x).decidirRevisao(x.sessao(),id,acao,parecer,p.assinatura())));
        },id("protocolo","ID da revisão"),escolha("encaminhamento","Encaminhamento","REJEITAR","COMPLEMENTAR","TRATAR","REVOGAR_PRESERVANDO"),texto("parecer","Parecer e encaminhamento"));
        a.alterar("processos-conciliar",grupo,"Solicitar conciliação de legado","Relaciona registro anterior à matrícula e estrutura explícitas. Não aprova disciplinas automaticamente.",solicitar,(c,f)->{
            String tipo=f.obrigatorio("tipo"),docs=f.obrigatorio("documentos");long origem=f.id("origem"),m=f.id("matricula"),e=f.id("estrutura");return gravar(f.conferiveis(),x->processos(x).solicitarConciliacao(x.sessao(),tipo,origem,m,e,docs));
        },escolha("tipo","Tipo do registro legado","PLANO","EQUIVALENCIA"),id("origem","ID do registro legado"),id("matricula","ID da matrícula institucional"),id("estrutura","ID da estrutura curricular"),texto("documentos","Referências documentais da conciliação"));
        a.consultar("processos-conciliacao-consultar",grupo,"Consultar conciliação","Contexto e documentos do registro anterior para análise.",consultar,(c,f)->processos(c).consultarConciliacao(c.sessao(),f.id("protocolo")),id("protocolo","ID da conciliação"));
        a.alterar("processos-conciliacao-decidir",grupo,"Decidir conciliação de legado","Aprovar PLANO apenas concilia contexto. Aprovar EQUIVALENCIA exige material documental completo do reconhecimento.",decidir,(c,f)->{
            long id=f.id("protocolo");boolean aprovar=f.sim("aprovar");String parecer=f.obrigatorio("parecer");var antes=processos(c).consultarConciliacao(c.sessao(),id);
            Equivalencia.Pedido material=aprovar&&"EQUIVALENCIA".equals(antes.tipo())?new Equivalencia.Pedido(f.id("matricula"),f.id("disciplina"),f.obrigatorio("instituicao"),f.obrigatorio("origem"),f.numero("horas"),f.obrigatorio("ementaOrigem"),f.obrigatorio("documentos"),f.data("conclusao"),f.obrigatorio("ementaDestino")):null;
            return gravar(Map.of("Contexto atual",antes,"Aprovar",aprovar,"Parecer",parecer,"Material",Objects.toString(material,"Não aplicável a esta decisão")),x->concluido(()->processos(x).decidirConciliacao(x.sessao(),id,aprovar,parecer,material)));
        },id("protocolo","ID da conciliação"),escolha("aprovar","Aprovar?","SIM","NAO"),texto("parecer","Parecer"),id("matricula","Matrícula do material de equivalência").facultativo(),id("disciplina","Disciplina de destino").facultativo(),campo("instituicao","Instituição de origem").facultativo(),campo("origem","Disciplina de origem").facultativo(),numero("horas","Carga de origem (horas)").facultativo(),texto("ementaOrigem","Ementa de origem").facultativo(),texto("documentos","Referências documentais").facultativo(),data("conclusao","Data de conclusão na origem").facultativo(),texto("ementaDestino","Ementa de destino").facultativo());
    }
}

package br.edu.cefetmg.scoa.web;

import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.*;
import static br.edu.cefetmg.scoa.web.AcoesAcademicasWeb.*;

final class AcoesSecretariaWeb {
    private AcoesSecretariaWeb() { }
    private static RepositorioBaseAcademica base(Contexto c) { return RepositoriosAcademicosAutorizados.baseAcademica(c.c(),c.sessao()); }
    private static RepositorioProfessoresDisciplinas pd(Contexto c) { return RepositoriosAcademicosAutorizados.professoresDisciplinas(c.c(),c.sessao()); }
    private static RepositorioCadastroAcademico cadastro(Contexto c) { return RepositoriosAcademicosAutorizados.cadastros(c.c(),c.sessao()); }
    private static RepositorioMatricula matriculas(Contexto c) { return RepositoriosAcademicosAutorizados.matriculas(c.c(),c.sessao()); }
    private static RepositorioInscricao inscricoes(Contexto c) { return RepositoriosAcademicosAutorizados.inscricoes(c.c(),c.sessao()); }
    private static RepositorioEquivalencia equivalencias(Contexto c) { return RepositoriosAcademicosAutorizados.equivalencias(c.c(),c.sessao()); }
    private static RepositorioHorarioAcademicoJdbc horarios(Contexto c) { return new RepositorioHorarioAcademicoJdbc(c.c(),c.sessao()); }
    private static Campo referencia() { return id("id","Referência existente").facultativo().ajuda("Deixe vazio para cadastrar. Para atualizar, use a referência exibida na consulta."); }
    private static Campo estado() { return escolha("situacao","Situação","ATIVO","INATIVO").padrao("ATIVO"); }
    private static <T> T preservar(T atual,T antes,T novo) { if(!Objects.equals(atual,antes)) throw new IllegalArgumentException("O cadastro mudou desde a conferência. Consulte os dados e prepare novamente.");return novo; }
    static void registrar(AcoesAcademicasWeb a) {
        String grupo="Cadastros da secretaria",academico="Operações da secretaria";
        a.consultar("secretaria-alunos",grupo,"Consultar alunos","Referências acadêmicas e vínculo opcional com conta; a conta é administrada separadamente.",CONSULTAR,(c,f)->base(c).listarAlunos());
        a.alterar("secretaria-aluno",grupo,"Cadastrar ou atualizar aluno","Preserva ID e conta vinculada nas alterações. Para vincular uma conta existente, use a administração de vínculos.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:base(c).buscarAluno(id).orElseThrow(()->new IllegalArgumentException("Aluno não encontrado."));
            var novo=new Aluno(id,antes==null?null:antes.idUsuario(),f.obrigatorio("identificacao"),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?base(x).inserirAluno(novo):base(x).alterarAluno(id,atual->preservar(atual,antes,novo)));
        },referencia(),campo("identificacao","Identificação do aluno"),estado());
        a.consultar("secretaria-professores",grupo,"Consultar professores","Cadastros docentes; suas referências são diferentes do ID da conta de acesso.",CONSULTAR,(c,f)->pd(c).listarProfessores());
        a.alterar("secretaria-professor",grupo,"Cadastrar ou atualizar professor","Preserva a conta já vinculada. A atribuição a turmas continua explícita.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:pd(c).buscarProfessor(id).orElseThrow(()->new IllegalArgumentException("Professor não encontrado."));
            var novo=new Professor(id,antes==null?null:antes.idUsuario(),f.obrigatorio("identificacao"),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?pd(x).inserirProfessor(novo):pd(x).alterarProfessor(id,atual->preservar(atual,antes,novo)));
        },referencia(),campo("identificacao","Identificação do professor"),estado());
        a.consultar("secretaria-cursos",grupo,"Consultar cursos","Referências e situação dos cursos da instituição.",CONSULTAR,(c,f)->base(c).listarCursos());
        a.alterar("secretaria-curso",grupo,"Cadastrar ou atualizar curso","Atualiza o cadastro preservando seu ID e os vínculos acadêmicos existentes.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:base(c).buscarCurso(id).orElseThrow(()->new IllegalArgumentException("Curso não encontrado."));
            var novo=new Curso(id,f.obrigatorio("nome"),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?base(x).inserirCurso(novo):base(x).alterarCurso(id,atual->preservar(atual,antes,novo)));
        },referencia(),campo("nome","Nome do curso"),estado());
        a.consultar("secretaria-disciplinas",grupo,"Consultar disciplinas","Código, nome e carga em horas de 60 minutos. Sessenta horas equivalem a 72 tempos de 50 minutos.",CONSULTAR,(c,f)->pd(c).listarDisciplinas());
        a.alterar("secretaria-disciplina",grupo,"Cadastrar ou atualizar disciplina","Carga em horas de 60 minutos. Alterações em cadastros comprometidos continuam sujeitas às regras de preservação.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:pd(c).buscarDisciplina(id).orElseThrow(()->new IllegalArgumentException("Disciplina não encontrada."));
            var novo=new Disciplina(id,f.obrigatorio("codigo"),f.obrigatorio("nome"),f.numero("horas"),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?pd(x).inserirDisciplina(novo):pd(x).alterarDisciplina(id,atual->preservar(atual,antes,novo)));
        },referencia(),campo("codigo","Código da disciplina"),campo("nome","Nome da disciplina"),numero("horas","Carga horária (horas de 60 minutos)"),estado());
        a.consultar("secretaria-requisitos",grupo,"Consultar pré-requisitos","Exigências formais da disciplina.",CONSULTAR,(c,f)->pd(c).listarPreRequisitos(f.id("disciplina")),id("disciplina","ID da disciplina"));
        a.alterar("secretaria-requisito",grupo,"Manter pré-requisito","Acrescenta ou retira uma exigência explícita, com verificação de ciclos e compromissos existentes.",MANTER,(c,f)->{
            long d=f.id("disciplina"),r=f.id("requisito");boolean incluir=f.sim("incluir");var antes=pd(c).listarPreRequisitos(d);
            return gravar(Map.of("Disciplina",d,"Pré-requisito",r,"Incluir",incluir,"Requisitos atuais",antes),x->concluido(()->{if(incluir)pd(x).adicionarPreRequisito(d,r);else pd(x).removerPreRequisito(d,r);}));
        },id("disciplina","ID da disciplina"),id("requisito","ID da disciplina exigida"),escolha("incluir","Incluir este pré-requisito?","SIM","NAO"));
        a.consultar("secretaria-estruturas",grupo,"Consultar estruturas curriculares","Versões de currículo vinculadas ao curso selecionado.",CONSULTAR,(c,f)->base(c).listarEstruturas(f.id("curso")),id("curso","ID do curso"));
        a.alterar("secretaria-estrutura",grupo,"Cadastrar ou atualizar estrutura","Identifica a estrutura curricular do curso. Vínculos já usados não são trocados silenciosamente.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:base(c).buscarEstrutura(id).orElseThrow(()->new IllegalArgumentException("Estrutura não encontrada."));
            var novo=new EstruturaCurricular(id,f.id("curso"),f.obrigatorio("identificacao"),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?base(x).inserirEstrutura(novo):base(x).alterarEstrutura(id,atual->preservar(atual,antes,novo)));
        },referencia(),id("curso","ID do curso"),campo("identificacao","Identificação da estrutura"),estado());
        a.consultar("secretaria-componentes",grupo,"Consultar componentes do currículo","Disciplinas que compõem a estrutura selecionada.",CONSULTAR,(c,f)->base(c).listarComponentes(f.id("estrutura")),id("estrutura","ID da estrutura curricular"));
        a.alterar("secretaria-componente",grupo,"Manter componente curricular","Inclui ou retira disciplina da estrutura, respeitando vínculos já utilizados.",MANTER,(c,f)->{
            long e=f.id("estrutura"),d=f.id("disciplina");boolean incluir=f.sim("incluir");var antes=base(c).listarComponentes(e);
            return gravar(Map.of("Estrutura",e,"Disciplina",d,"Incluir",incluir,"Componentes atuais",antes),x->concluido(()->{if(incluir)base(x).adicionarComponente(e,d);else base(x).removerComponente(e,d);}));
        },id("estrutura","ID da estrutura"),id("disciplina","ID da disciplina"),escolha("incluir","Incluir na estrutura?","SIM","NAO"));
        a.consultar("secretaria-periodos",grupo,"Consultar períodos","Calendário letivo com datas e referências para ofertas e renovação.",CONSULTAR,(c,f)->cadastro(c).listarPeriodos());
        a.alterar("secretaria-periodo",grupo,"Cadastrar ou atualizar período","Não admite sobreposição de períodos. Período comprometido preserva suas datas e situação.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:cadastro(c).buscarPeriodo(id).orElseThrow(()->new IllegalArgumentException("Período não encontrado."));
            var novo=new CadastroPeriodoLetivo(id,f.obrigatorio("descricao"),new PeriodoLetivo(f.data("inicio"),f.data("fim")),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?cadastro(x).inserirPeriodo(novo):cadastro(x).alterarPeriodo(id,atual->preservar(atual,antes,novo)));
        },referencia(),campo("descricao","Descrição do período"),data("inicio","Início"),data("fim","Término"),estado());
        a.consultar("secretaria-turmas",grupo,"Consultar turmas","Ofertas com disciplina, professor, período e capacidade.",CONSULTAR,(c,f)->cadastro(c).listarTurmas());
        a.alterar("secretaria-turma",grupo,"Cadastrar ou atualizar turma","Responsável docente, disciplina e período são referências acadêmicas. Horários e sala são tratados em seguida.",MANTER,(c,f)->{
            Long id=f.opcional("id");var antes=id==null?null:cadastro(c).buscarTurma(id).orElseThrow(()->new IllegalArgumentException("Turma não encontrada."));
            var novo=new Turma(id,f.id("disciplina"),f.id("professor"),f.id("periodo"),f.obrigatorio("codigo"),f.numero("capacidade"),f.obrigatorio("situacao"));
            return gravar(novo,x->id==null?cadastro(x).inserirTurma(novo):cadastro(x).alterarTurma(id,atual->preservar(atual,antes,novo)));
        },referencia(),id("disciplina","ID da disciplina"),id("professor","ID do professor responsável"),id("periodo","ID do período"),campo("codigo","Código da turma"),numero("capacidade","Capacidade"),estado());
        a.consultar("secretaria-salas",grupo,"Consultar salas","Capacidade e situação das salas da instituição.",CONSULTAR,(c,f)->RepositoriosAcademicosAutorizados.salas(c.c(),c.sessao()).listar());
        a.alterar("secretaria-sala",grupo,"Cadastrar ou atualizar sala","Preserva reservas e verifica capacidade ao alterar uma sala já utilizada.",MANTER,(c,f)->{
            Long id=f.opcional("id");var repo=RepositoriosAcademicosAutorizados.salas(c.c(),c.sessao());
            var antes=id==null?null:repo.buscarPorId(id).orElseThrow(()->new IllegalArgumentException("Sala não encontrada."));
            var nova=new Sala(id,f.obrigatorio("identificacao"),f.obrigatorio("localizacao"),f.numero("capacidade"),f.obrigatorio("situacao"));
            return gravar(nova,x->id==null?RepositoriosAcademicosAutorizados.salas(x.c(),x.sessao()).salvar(nova):RepositoriosAcademicosAutorizados.salas(x.c(),x.sessao()).alterar(id,atual->{if(!atual.getIdentificacao().equals(antes.getIdentificacao())||!atual.getLocalizacao().equals(antes.getLocalizacao())||atual.getCapacidade()!=antes.getCapacidade()||!atual.getSituacao().equals(antes.getSituacao()))throw new IllegalArgumentException("A sala mudou; confira novamente.");return nova;}));
        },referencia(),campo("identificacao","Identificação da sala"),campo("localizacao","Localização"),numero("capacidade","Capacidade"),estado());
        a.consultar("secretaria-horarios",grupo,"Consultar horários da turma","Tempos definidos com ou sem sala, situação e versão atual.",CONSULTAR,(c,f)->horarios(c).listar(f.id("turma")),id("turma","ID da turma"));
        a.alterar("secretaria-planejar-horario",grupo,"Definir horários antes da sala","Tempos oficiais por dia. Exige turma sem inscrições/aulas que seriam afetadas.",MANTER,(c,f)->{
            var p=new HorarioAcademico.Planejar(f.id("turma"),f.numero("dia"),AcoesAlunoDocenteWeb.tempos(f),f.obrigatorio("motivo"));var antes=horarios(c).listar(p.turma());
            return gravar(Map.of("Horários atuais",antes,"Tempos a definir",p),x->horarios(x).planejar(p));
        },id("turma","ID da turma"),numero("dia","Dia da semana (1=segunda a 7=domingo)"),campo("tempos","Tempos, como M1,M2 ou N4,N5"),texto("motivo","Motivo"));
        a.consultar("secretaria-salas-disponiveis",grupo,"Salas disponíveis para o horário","Alternativas que respeitam capacidade e ocupação no período.",CONSULTAR,(c,f)->horarios(c).salasDisponiveis(f.id("horario")),id("horario","ID do horário da turma"));
        a.alterar("secretaria-sala-horario",grupo,"Atribuir, retirar sala ou cancelar horário","Retirar sala mantém dias/tempos e registros acadêmicos. Cancelar o horário exige ausência de compromissos acadêmicos.",MANTER,(c,f)->{
            long h=f.id("horario");int v=f.numero("versao");String op=f.obrigatorio("operacao"),motivo=f.obrigatorio("motivo");Long sala=f.opcional("sala");
            if(op.equals("ATRIBUIR") && sala==null)throw new IllegalArgumentException("Selecione a referência da sala.");
            return gravar(Map.of("Horário",h,"Versão",v,"Operação",op,"Sala",Objects.toString(sala,"Sem sala"),"Motivo",motivo),x->switch(op){case "ATRIBUIR"->horarios(x).atribuirSala(h,v,sala,motivo);case "RETIRAR_SALA"->horarios(x).retirarSala(h,v,motivo);case "CANCELAR_HORARIO"->horarios(x).cancelar(h,v,motivo);default->throw new IllegalArgumentException("Operação inválida.");});
        },id("horario","ID do horário"),numero("versao","Versão consultada do horário"),escolha("operacao","Operação","ATRIBUIR","RETIRAR_SALA","CANCELAR_HORARIO"),id("sala","ID da sala para atribuição").facultativo(),texto("motivo","Motivo"));
        a.consultar("secretaria-historico-horario",grupo,"Histórico do horário","Alterações com motivo e autoria, preservando a referência original.",CONSULTAR,(c,f)->horarios(c).historico(f.id("horario")),id("horario","ID do horário"));

        a.consultar("secretaria-matriculas",academico,"Matrículas de um aluno","Vínculos institucionais da pessoa com cursos.",CONSULTAR,(c,f)->matriculas(c).listar(f.id("aluno")),id("aluno","Referência do aluno (não é o ID da conta)"));
        a.alterar("secretaria-nova-matricula",academico,"Abrir matrícula institucional","Cria vínculo para aluno e curso existentes, após conferir biblioteca e duplicidade.",MANTER,(c,f)->{
            long aluno=f.id("aluno"),curso=f.id("curso"),periodo=f.id("periodo");var data=f.data("data");String motivo=f.obrigatorio("motivo");
            return gravar(f.conferiveis(),x->matriculas(x).criar(aluno,curso,data,periodo,motivo));
        },id("aluno","ID do aluno"),id("curso","ID do curso"),id("periodo","ID do período de ingresso"),data("data","Data de ingresso"),texto("motivo","Motivo ou referência do ingresso"));
        a.alterar("secretaria-corrigir-matricula",academico,"Corrigir dados de ingresso","Preserva a matrícula e registra o histórico de correção.",MANTER,(c,f)->{
            long m=f.id("matricula"),p=f.id("periodo");var d=f.data("data");String motivo=f.obrigatorio("motivo");var antes=matriculas(c).buscar(m).orElseThrow(()->new IllegalArgumentException("Matrícula não encontrada."));
            return gravar(Map.of("Vínculo atual",antes,"Nova data",d,"Novo período",p,"Motivo",motivo),x->matriculas(x).corrigir(m,d,p,motivo));
        },id("matricula","ID da matrícula institucional"),id("periodo","ID do período de ingresso correto"),data("data","Data de ingresso correta"),texto("motivo","Motivo da correção"));
        a.alterar("secretaria-situacao-matricula",academico,"Trancar, reativar ou cancelar vínculo","Transição explícita do vínculo institucional, com preservação de registros e análise dos compromissos.",MANTER,(c,f)->{
            long m=f.id("matricula");String destino=f.obrigatorio("situacao"),motivo=f.obrigatorio("motivo");var atual=matriculas(c).buscar(m);
            return gravar(Map.of("Vínculo atual",atual,"Destino",destino,"Motivo",motivo),x->matriculas(x).transicionar(m,destino,motivo));
        },id("matricula","ID da matrícula institucional"),escolha("situacao","Situação de destino","ATIVO","TRANCADO","CANCELADO"),texto("motivo","Motivo da transição"));
        a.consultar("secretaria-historico-matricula",academico,"Histórico da matrícula","Ingresso, correções, transições e renovações do vínculo.",CONSULTAR,(c,f)->matriculas(c).historico(f.id("matricula")),id("matricula","ID da matrícula institucional"));
        a.consultar("secretaria-biblioteca",academico,"Conferir situação bibliotecária","Consulta impeditiva usada pelos fluxos acadêmicos. A consulta não quita nem apaga pendências.",CONSULTAR,(c,f)->matriculas(c).consultarBiblioteca(f.id("aluno")),id("aluno","ID do aluno"));
        a.alterar("secretaria-vincular-curriculo",academico,"Vincular currículo à matrícula","Seleciona estrutura explícita do curso para elegibilidade e plano de estudos.",MANTER,(c,f)->{
            long m=f.id("matricula"),e=f.id("estrutura");return gravar(f.conferiveis(),x->concluido(()->inscricoes(x).vincularEstrutura(m,e)));
        },id("matricula","ID da matrícula institucional"),id("estrutura","ID da estrutura curricular"));
        a.consultar("secretaria-inscricoes",academico,"Consultar inscrições da matrícula","Vínculos do aluno com turmas e suas situações atuais.",CONSULTAR,(c,f)->inscricoes(c).listar(f.id("matricula")),id("matricula","ID da matrícula institucional"));
        a.alterar("secretaria-inscrever",academico,"Inscrever aluno em turma","Confere elegibilidade e vagas antes da confirmação; gravação revalida o contexto.",MANTER,(c,f)->{
            long m=f.id("matricula"),t=f.id("turma");var e=inscricoes(c).avaliar(m,t);if(!e.permite())throw new IllegalArgumentException(String.join(" ",e.motivos()));
            return gravar(Map.of("Matrícula",m,"Turma",t,"Elegibilidade",e),x->inscricoes(x).inscrever(m,t));
        },id("matricula","ID da matrícula institucional"),id("turma","ID da turma"));
        a.consultar("secretaria-simular-plano",academico,"Simular distribuição por ofertas","Sugestões por currículo e prioridades. Não cria plano, não reserva vaga e não libera pré-requisito apenas sugerido.",CONSULTAR,(c,f)->RepositoriosAcademicosAutorizados.planejamento(c.c(),c.sessao()).simular(f.id("matricula"),f.ids("periodos"),f.ids("prioridades")),id("matricula","ID da matrícula institucional"),campo("periodos","IDs dos períodos, separados por vírgula"),campo("prioridades","IDs das disciplinas em ordem de prioridade").facultativo());
        a.alterar("secretaria-plano",academico,"Registrar versão do plano institucional","Confira componentes e ofertas. Uma versão nova preserva todas as anteriores; sugestões não efetivam inscrição.",MANTER,(c,f)->{
            var selecao=new PlanoInstitucional.Selecao(f.id("matricula"),f.ids("periodos"),f.ids("prioridades"));String motivo=f.obrigatorio("motivo");var p=new RepositorioPlanoInstitucionalJdbc(c.c(),c.sessao()).preparar(selecao);
            var pedido=new PlanoInstitucional.Registro(selecao,p.versaoAnterior(),p.assinatura(),motivo);return gravar(p,x->new RepositorioPlanoInstitucionalJdbc(x.c(),x.sessao()).registrar(pedido));
        },id("matricula","ID da matrícula institucional"),campo("periodos","IDs dos períodos, separados por vírgula"),campo("prioridades","IDs das disciplinas em ordem de prioridade").facultativo(),texto("motivo","Motivo desta versão"));
        a.consultar("secretaria-planos",academico,"Consultar plano e suas versões","Histórico institucional; a consulta não atualiza planos automaticamente.",CONSULTAR,(c,f)->Map.of("Plano atual",new RepositorioPlanoInstitucionalJdbc(c.c(),c.sessao()).atualSecretaria(f.id("matricula")),"Versões",new RepositorioPlanoInstitucionalJdbc(c.c(),c.sessao()).historicoSecretaria(f.id("matricula"))),id("matricula","ID da matrícula institucional"));
        a.alterar("secretaria-rematricula-lote",academico,"Processar rematrícula em lote","Confira candidatos, propostas e ocorrências. Sem turma disponível, toda a renovação do candidato segue para análise manual; não são criadas inscrições.",MANTER,(c,f)->{
            var selecao=new RematriculaLote.Selecao(f.id("curso"),f.id("origem"),f.id("destino"),f.ids("matriculas"));String motivo=f.obrigatorio("motivo");var p=new RepositorioRematriculaLoteJdbc(c.c(),c.sessao()).preparar(selecao);
            return gravar(p,x->new RepositorioRematriculaLoteJdbc(x.c(),x.sessao()).executar(new RematriculaLote.Pedido(selecao,p.chave(),p.assinatura(),motivo)));
        },id("curso","ID do curso"),id("origem","ID do período anterior encerrado"),id("destino","ID do período de renovação"),campo("matriculas","IDs das matrículas, separados por vírgula").facultativo().ajuda("Vazio prepara todas as matrículas ativas do curso; confira a lista antes de confirmar."),texto("motivo","Motivo do processamento"));
        a.consultar("secretaria-lote",academico,"Consultar lote de rematrícula","Resultado e ocorrências do lote já processado.",CONSULTAR,(c,f)->new RepositorioRematriculaLoteJdbc(c.c(),c.sessao()).consultar(f.id("lote")),id("lote","Protocolo do lote"));
        registrarTransferencias(a,academico);registrarEquivalencias(a,academico);registrarDocumentos(a);
    }
    private static void registrarTransferencias(AcoesAcademicasWeb a,String grupo) {
        a.alterar("secretaria-transferir-interna",grupo,"Efetivar transferência interna","Encerra origem como transferida, abre destino na mesma pessoa, cancela inscrições no prazo e aproveita disciplinas idênticas aprovadas formalmente.",MANTER,(c,f)->{
            var p=new TransferenciaInterna.Pedido(f.id("origem"),f.id("curso"),f.id("estrutura"),f.id("periodo"),f.obrigatorio("motivo"));var m=matriculas(c).buscar(p.origem());var i=inscricoes(c).listar(p.origem());
            return gravar(Map.of("Transferência",p,"Vínculo de origem",m,"Inscrições na origem",i),x->new AcessoAcademicoJdbc(x.c(),x.sessao()).consultar(()->{
                if(!m.equals(matriculas(x).buscar(p.origem())) || !i.equals(inscricoes(x).listar(p.origem())))throw new IllegalArgumentException("A origem ou suas inscrições mudaram desde a conferência. Prepare a transferência novamente.");
                return new RepositorioTransferenciaInternaJdbc(x.c(),x.sessao()).efetivar(p);
            }));
        },id("origem","ID da matrícula de origem"),id("curso","ID do curso de destino"),id("estrutura","ID da estrutura de destino"),id("periodo","ID do período da transferência"),texto("motivo","Motivo da transferência"));
        a.consultar("secretaria-transferencias-internas",grupo,"Histórico de transferências internas","Processos e referências dos vínculos preservados.",CONSULTAR,(c,f)->new RepositorioTransferenciaInternaJdbc(c.c(),c.sessao()).listar(f.id("matricula")),id("matricula","ID da matrícula de origem"));
        a.alterar("secretaria-transferencia-externa",grupo,"Registrar transferência externa","Entrada usa aluno, curso e estrutura; saída usa somente a matrícula de origem. Registrar o processo não efetiva a mudança.",MANTER,(c,f)->{
            var p=new TransferenciaExterna.Pedido(f.obrigatorio("chave"),f.escolha("tipo",TransferenciaExterna.Tipo.class),f.opcional("aluno"),f.opcional("curso"),f.opcional("estrutura"),f.opcional("origem"),f.id("periodo"),f.obrigatorio("instituicao"),f.obrigatorio("documentos"),f.obrigatorio("motivo"));
            return gravar(p,x->new RepositorioTransferenciaExternaJdbc(x.c(),x.sessao()).registrar(p));
        },campo("chave","Referência única do processo"),escolha("tipo","Tipo",TransferenciaExterna.Tipo.class),id("aluno","ID do aluno (entrada)").facultativo(),id("curso","ID do curso (entrada)").facultativo(),id("estrutura","ID da estrutura (entrada)").facultativo(),id("origem","ID da matrícula de origem (saída)").facultativo(),id("periodo","ID do período"),campo("instituicao","Instituição externa"),texto("documentos","Referências dos documentos"),texto("motivo","Motivo"));
        a.consultar("secretaria-transferencia-externa-consultar",grupo,"Consultar transferência externa","Situação, versão, mudança de vínculo e histórico documental.",CONSULTAR,(c,f)->Map.of("Processo",new RepositorioTransferenciaExternaJdbc(c.c(),c.sessao()).consultar(f.id("protocolo")),"Andamentos",new RepositorioTransferenciaExternaJdbc(c.c(),c.sessao()).historico(f.id("protocolo"))),id("protocolo","Protocolo da transferência externa"));
        a.alterar("secretaria-transferencia-externa-decidir",grupo,"Atualizar ou efetivar transferência externa","Efetivar executa a mudança de vínculo. Saída exige biblioteca regular e prazo válido; documentos e registros anteriores são preservados.",MANTER,(c,f)->{
            long id=f.id("protocolo");var p=new TransferenciaExterna.Atualizacao(f.numero("versao"),f.escolha("situacao",TransferenciaExterna.Situacao.class),f.obrigatorio("documentos"),f.obrigatorio("observacao"));var antes=new RepositorioTransferenciaExternaJdbc(c.c(),c.sessao()).consultar(id);
            return gravar(Map.of("Processo atual",antes,"Atualização",p),x->new RepositorioTransferenciaExternaJdbc(x.c(),x.sessao()).atualizar(id,p));
        },id("protocolo","Protocolo da transferência"),numero("versao","Versão consultada"),escolha("situacao","Nova situação",TransferenciaExterna.Situacao.class),texto("documentos","Referências documentais atualizadas"),texto("observacao","Fundamento ou observação"));
    }
    private static void registrarEquivalencias(AcoesAcademicasWeb a,String grupo) {
        a.alterar("secretaria-equivalencia",grupo,"Solicitar aproveitamento ou equivalência","Disciplinas distintas e estudos externos dependem de análise formal de conteúdo e carga horária.",c->tecnico(c,SegurancaJdbc.SOLICITAR),(c,f)->{
            var p=new Equivalencia.Pedido(f.id("matricula"),f.id("disciplina"),f.obrigatorio("instituicao"),f.obrigatorio("origem"),f.numero("horas"),f.obrigatorio("ementaOrigem"),f.obrigatorio("documentos"),f.data("conclusao"),f.obrigatorio("ementaDestino"));return gravar(p,x->equivalencias(x).solicitar(p));
        },id("matricula","ID da matrícula institucional"),id("disciplina","ID da disciplina de destino"),campo("instituicao","Instituição de origem"),campo("origem","Disciplina de origem"),numero("horas","Carga de origem (horas)"),texto("ementaOrigem","Ementa de origem"),texto("documentos","Referências de conclusão, ementa e carga"),data("conclusao","Data de conclusão na origem"),texto("ementaDestino","Ementa de destino considerada"));
        a.consultar("secretaria-equivalencias",grupo,"Consultar equivalências e plano básico","Pedidos, decisões e evidências formais da matrícula.",c->tecnico(c,AcessoAcademicoJdbc.CONSULTAR)||tecnico(c,SegurancaJdbc.SOLICITAR)||tecnico(c,SegurancaJdbc.DECIDIR),(c,f)->Map.of("Pedidos",equivalencias(c).listar(f.id("matricula")),"Plano básico",equivalencias(c).plano(f.id("matricula")),"Histórico",equivalencias(c).historico(f.id("matricula"))),id("matricula","ID da matrícula institucional"));
        a.alterar("secretaria-equivalencia-decidir",grupo,"Decidir equivalência","Decisão formal com parecer sobre ementa/carga e responsável identificado.",c->tecnico(c,SegurancaJdbc.DECIDIR),(c,f)->{
            long p=f.id("protocolo");var d=new Equivalencia.Decisao(f.sim("deferir"),f.obrigatorio("parecer"),f.obrigatorio("responsavel"));return gravar(Map.of("Pedido",p,"Decisão",d),x->equivalencias(x).decidir(p,d));
        },id("protocolo","Protocolo da equivalência"),escolha("deferir","Deferir?","SIM","NAO"),texto("parecer","Parecer sobre conteúdo e carga"),campo("responsavel","Responsável pela análise"));
    }
    private static void registrarDocumentos(AcoesAcademicasWeb a) {
        String grupo="Atendimento de documentos";
        a.consultar("secretaria-documentos",grupo,"Fila de documentos","Solicitações, situação e versão atual para atendimento.",c->tecnico(c,AcessoAcademicoJdbc.DOCUMENTOS_CONSULTAR),(c,f)->new RepositorioDocumentoAcademicoJdbc(c.c(),c.sessao()).filaSecretaria());
        a.consultar("secretaria-documento-historico",grupo,"Histórico de documento solicitado","Andamentos e referências do atendimento preservados.",c->tecnico(c,AcessoAcademicoJdbc.DOCUMENTOS_CONSULTAR),(c,f)->new RepositorioDocumentoAcademicoJdbc(c.c(),c.sessao()).andamentoSecretaria(f.id("protocolo")),id("protocolo","Protocolo da solicitação"));
        a.alterar("secretaria-documento-atender",grupo,"Atender solicitação de documento","Registre andamento e parecer. Para concluir como atendido, informe a referência do documento entregue.",c->tecnico(c,AcessoAcademicoJdbc.DOCUMENTOS_ATENDER),(c,f)->{
            var p=new DocumentoAcademico.Atualizacao(f.id("protocolo"),f.numero("versao"),f.escolha("situacao",DocumentoAcademico.Situacao.class),f.obrigatorio("parecer"),f.texto("entrega"));return gravar(p,x->new RepositorioDocumentoAcademicoJdbc(x.c(),x.sessao()).atualizar(p));
        },id("protocolo","Protocolo da solicitação"),numero("versao","Versão consultada"),escolha("situacao","Novo andamento","EM_ATENDIMENTO","ATENDIDO","INDEFERIDO"),texto("parecer","Parecer"),campo("entrega","Referência do documento entregue").facultativo());
    }
}

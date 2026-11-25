package br.edu.cefetmg.scoa.web;

import java.time.*;
import java.util.*;
import br.edu.cefetmg.scoa.dominio.*;
import br.edu.cefetmg.scoa.persistencia.*;
import static br.edu.cefetmg.scoa.web.AcoesAcademicasWeb.*;

final class AcoesAlunoDocenteWeb {
    private AcoesAlunoDocenteWeb() { }
    private static RepositorioDiarioAcademicoJdbc diario(Contexto c) { return new RepositorioDiarioAcademicoJdbc(c.c(),c.sessao()); }
    private static RepositorioRetificacaoJdbc retificacao(Contexto c) { return new RepositorioRetificacaoJdbc(c.c(),c.sessao()); }
    private static RepositorioDocumentoAcademicoJdbc documentos(Contexto c) { return new RepositorioDocumentoAcademicoJdbc(c.c(),c.sessao()); }
    static List<TempoLetivo> tempos(FormularioWeb f) { try{return Arrays.stream(f.obrigatorio("tempos").toUpperCase(Locale.ROOT).split("[,;\\s]+",-1)).map(TempoLetivo::valueOf).toList();}catch(IllegalArgumentException e){throw new IllegalArgumentException("Use tempos oficiais separados por vírgula, como M1,M2 ou N4,N5.");} }
    static void registrar(AcoesAcademicasWeb a) {
        String aluno="Vida acadêmica do aluno",docente="Diário do professor";
        a.consultar("aluno-matriculas",aluno,"Minhas matrículas","Vínculos institucionais da sua conta; não confunda matrícula com ID da conta.",ALUNO,(c,f)->documentos(c).minhasMatriculas());
        a.consultar("aluno-periodos",aluno,"Calendário letivo","Datas e referências dos períodos para renovação e consulta de ofertas.",ALUNO,(c,f)->new RepositorioNavegacaoAcademicaJdbc(c.c(),c.sessao()).periodosAluno());
        a.consultar("aluno-historico",aluno,"Meu histórico escolar","Disciplinas cursadas, reconhecimentos e registros anteriores preservados.",ALUNO,(c,f)->new RepositorioAtendimentoAlunoJdbc(c.c(),c.sessao()).historico());
        a.consultar("aluno-horarios",aluno,"Meus horários","Horários das inscrições ativas, incluindo sala ainda a definir.",ALUNO,(c,f)->new RepositorioConsultasAlunoJdbc(c.c(),c.sessao()).horarios());
        a.consultar("aluno-resultados",aluno,"Meus resultados finais","Resultados formais vigentes; ausência de resultado não se transforma em nota zero.",ALUNO,(c,f)->new RepositorioConsultasAlunoJdbc(c.c(),c.sessao()).resultadosFinais());
        a.consultar("aluno-inscricoes",aluno,"Minhas inscrições","Referências para consultar notas e frequência ou solicitar cancelamento.",ALUNO,(c,f)->new RepositorioNavegacaoAcademicaJdbc(c.c(),c.sessao()).inscricoesAluno());
        a.consultar("aluno-boletim",aluno,"Minhas notas e frequência","Lançamentos do diário, tempos considerados e prévia do resultado da sua inscrição.",ALUNO,(c,f)->diario(c).consultarAluno(f.id("inscricao")),id("inscricao","ID da sua inscrição na turma"));
        a.consultar("aluno-ofertas",aluno,"Ofertas para minha matrícula","Confira turmas do currículo, vagas observadas, janelas e impedimentos. A consulta não reserva vaga.",ALUNO,(c,f)->new RepositorioNavegacaoAcademicaJdbc(c.c(),c.sessao()).ofertasAluno(f.id("matricula")),id("matricula","ID da sua matrícula institucional"));
        a.alterar("aluno-inscrever",aluno,"Confirmar inscrição em turma","A inscrição é revalidada na confirmação e usa a mesma pessoa e matrícula existentes.",ALUNO,(c,f)->{
            long m=f.id("matricula"),t=f.id("turma");var e=RepositoriosAcademicosAutorizados.inscricoes(c.c(),c.sessao()).avaliar(m,t);
            if(!e.permite())throw new IllegalArgumentException(String.join(" ",e.motivos()));
            return gravar(Map.of("Matrícula",m,"Turma",t,"Elegibilidade",e),atual->new RepositorioInscricaoJdbc(atual.c()).inscreverComoAluno(atual.sessao(),m,t));
        },id("matricula","ID da sua matrícula institucional"),id("turma","ID da turma escolhida"));
        a.alterar("aluno-cancelar",aluno,"Solicitar cancelamento","No prazo de liberação imediata, o cancelamento é efetivado; depois depende da coordenação, dentro do prazo de pedido.",ALUNO,(c,f)->{
            long i=f.id("inscricao");String motivo=f.obrigatorio("motivo");var antes=new RepositorioCancelamentoJdbc(c.c(),c.sessao()).historico(i);
            return gravar(Map.of("Inscrição",i,"Motivo",motivo,"Pedidos anteriores",antes),x->new RepositorioCancelamentoJdbc(x.c(),x.sessao()).solicitar(i,motivo));
        },id("inscricao","ID da sua inscrição"),texto("motivo","Motivo do cancelamento"));
        a.consultar("aluno-cancelamentos",aluno,"Acompanhar cancelamento","Histórico dos pedidos da sua inscrição.",ALUNO,(c,f)->new RepositorioCancelamentoJdbc(c.c(),c.sessao()).historico(f.id("inscricao")),id("inscricao","ID da sua inscrição"));
        a.alterar("aluno-rematricular",aluno,"Renovar matrícula institucional","Renova o vínculo existente; a inscrição nas turmas continua exigindo confirmação separada.",ALUNO,(c,f)->{
            long m=f.id("matricula"),p=f.id("periodo");var propria=documentos(c).minhasMatriculas().stream().filter(v->v.matricula()==m).findFirst().orElseThrow(()->new IllegalArgumentException("Matrícula não autorizada."));
            return gravar(Map.of("Vínculo",propria,"Período de destino",p),x->concluido(()->new RepositorioMatriculaJdbc(x.c()).rematricular(x.sessao(),m,p)));
        },id("matricula","ID da sua matrícula institucional"),id("periodo","ID do período para renovação"));
        a.consultar("aluno-plano",aluno,"Meu plano institucional","Consulta o plano registrado sem atualizá-lo automaticamente. Observe avisos de desatualização.",ALUNO,(c,f)->new RepositorioPlanoInstitucionalJdbc(c.c(),c.sessao()).atualAluno(f.id("matricula")),id("matricula","ID da sua matrícula institucional"));
        a.consultar("aluno-planos-anteriores",aluno,"Versões do meu plano","Versões anteriores do plano preservadas com autoria e fundamento.",ALUNO,(c,f)->new RepositorioPlanoInstitucionalJdbc(c.c(),c.sessao()).historicoAluno(f.id("matricula")),id("matricula","ID da sua matrícula institucional"));
        a.alterar("aluno-documento",aluno,"Solicitar documento acadêmico","Solicite declaração de matrícula ou histórico escolar para atendimento pela secretaria.",ALUNO,(c,f)->{
            var pedido=new DocumentoAcademico.Novo(f.id("matricula"),f.escolha("tipo",DocumentoAcademico.Tipo.class));
            if(documentos(c).minhasMatriculas().stream().noneMatch(m->m.matricula()==pedido.matricula()))throw new IllegalArgumentException("Matrícula não autorizada.");
            return gravar(pedido,x->documentos(x).solicitar(pedido));
        },id("matricula","ID da sua matrícula institucional"),escolha("tipo","Documento solicitado",DocumentoAcademico.Tipo.class));
        a.consultar("aluno-documentos",aluno,"Meus documentos solicitados","Protocolo, situação e referência de entrega informada pela secretaria.",ALUNO,(c,f)->documentos(c).minhasSolicitacoes());
        a.consultar("aluno-documento-andamento",aluno,"Andamento de documento","Histórico da sua solicitação.",ALUNO,(c,f)->documentos(c).meuAndamento(f.id("protocolo")),id("protocolo","Protocolo da solicitação"));
        a.alterar("aluno-manifestacao",aluno,"Enviar sugestão ou reclamação","Sua manifestação fica vinculada à sua conta e ao seu cadastro acadêmico.",ALUNO,(c,f)->{
            var p=new AtendimentoAluno.NovaManifestacao(f.escolha("tipo",AtendimentoAluno.TipoManifestacao.class),f.obrigatorio("conteudo"));
            new RepositorioAtendimentoAlunoJdbc(c.c(),c.sessao()).verificarAcesso();
            return gravar(p,x->new RepositorioAtendimentoAlunoJdbc(x.c(),x.sessao()).registrar(p));
        },escolha("tipo","Tipo de manifestação",AtendimentoAluno.TipoManifestacao.class),texto("conteudo","Conteúdo"));
        a.consultar("aluno-manifestacoes",aluno,"Minhas manifestações","Sugestões e reclamações enviadas por você.",ALUNO,(c,f)->new RepositorioAtendimentoAlunoJdbc(c.c(),c.sessao()).manifestacoes());

        a.consultar("docente-turmas",docente,"Minhas turmas","Turmas sob sua responsabilidade. Permissão administrativa não substitui vínculo docente.",DOCENTE,(c,f)->new RepositorioNavegacaoAcademicaJdbc(c.c(),c.sessao()).turmasDocente());
        a.consultar("docente-diario",docente,"Diário completo da turma","Consulta aulas, avaliações, notas, chamadas e resultados vigentes sem arquivar uma exportação.",DOCENTE,(c,f)->new RepositorioRelatorioAcademicoJdbc(c.c(),c.sessao()).preparar(new RelatorioAcademico.Filtro(RelatorioAcademico.Tipo.DIARIO_DOCENTE,null,null,f.id("turma"),null)).documento(),id("turma","ID da sua turma"));
        a.consultar("docente-inscricoes",docente,"Alunos inscritos na turma","Referências de matrícula e inscrição dos alunos da sua turma.",DOCENTE,(c,f)->new RepositorioNavegacaoAcademicaJdbc(c.c(),c.sessao()).inscricoesDocente(f.id("turma")),id("turma","ID da sua turma"));
        a.consultar("docente-aulas",docente,"Aulas registradas","Data, tempo letivo, pauta e conteúdo da sua turma.",DOCENTE,(c,f)->diario(c).aulas(f.id("turma")),id("turma","ID da sua turma"));
        a.alterar("docente-nova-aula",docente,"Registrar aulas por tempos","Selecione os tempos oficiais de 50 minutos. Intervalos não contam como aula. 60 horas correspondem a 72 tempos.",DOCENTE,(c,f)->{
            var p=new DiarioAcademico.NovaAula(f.id("turma"),f.data("data"),tempos(f),f.obrigatorio("pauta"),f.obrigatorio("conteudo"));diario(c).aulas(p.turma());
            return gravar(p,x->diario(x).registrarAulas(p));
        },id("turma","ID da sua turma"),data("data","Data das aulas"),campo("tempos","Tempos separados por vírgula").ajuda("Manhã M1–M6; tarde T1–T6; noite N1–N5. Exemplo: M1,M2."),campo("pauta","Pauta"),texto("conteudo","Conteúdo ministrado"));
        a.consultar("docente-avaliacoes",docente,"Avaliações da turma","P1, P2 e PF cadastradas separadamente.",DOCENTE,(c,f)->diario(c).avaliacoes(f.id("turma")),id("turma","ID da sua turma"));
        a.alterar("docente-nova-avaliacao",docente,"Cadastrar avaliação","Registre P1, P2 ou PF sem duplicar o tipo na turma.",DOCENTE,(c,f)->{
            var p=new DiarioAcademico.NovaAvaliacao(f.id("turma"),f.escolha("tipo",DiarioAcademico.Prova.class),f.data("data"),f.obrigatorio("descricao"));diario(c).avaliacoes(p.turma());return gravar(p,x->diario(x).registrarAvaliacao(p));
        },id("turma","ID da sua turma"),escolha("tipo","Prova",DiarioAcademico.Prova.class),data("data","Data da avaliação"),texto("descricao","Descrição"));
        a.consultar("docente-boletim",docente,"Conferir notas e frequência","Consulta completa de uma inscrição da sua turma, com versões atuais dos lançamentos.",DOCENTE,(c,f)->diario(c).consultarDocente(f.id("inscricao")),id("inscricao","ID da inscrição do aluno"));
        a.alterar("docente-nota",docente,"Lançar ou corrigir nota","A versão é obtida do boletim. Use -1 para o primeiro lançamento; correções preservam histórico.",DOCENTE,(c,f)->{
            var p=new DiarioAcademico.LancarNota(f.id("avaliacao"),f.id("inscricao"),f.decimal("valor"),f.numero("versao"),f.obrigatorio("motivo"));var b=diario(c).consultarDocente(p.inscricao());
            return gravar(Map.of("Lançamento",p,"Boletim atual",b),x->concluido(()->diario(x).lancarNota(p)));
        },id("avaliacao","ID da avaliação P1/P2/PF"),id("inscricao","ID da inscrição do aluno"),decimal("valor","Nota de 0 a 10"),numero("versao","Versão da nota (-1 se ainda não lançada)").padrao(-1),texto("motivo","Motivo do lançamento ou correção"));
        a.alterar("docente-retirar-nota",docente,"Desconsiderar nota indevida","Preserva ID, valor anterior e histórico. Use para PF que não se aplica; não substitui retificação após fechamento.",DOCENTE,(c,f)->{
            var p=new DiarioAcademico.RetirarNota(f.id("avaliacao"),f.id("inscricao"),f.numero("versao"),f.obrigatorio("motivo"));var b=diario(c).consultarDocente(p.inscricao());return gravar(Map.of("Retirada",p,"Boletim atual",b),x->concluido(()->diario(x).retirarNota(p)));
        },id("avaliacao","ID da avaliação"),id("inscricao","ID da inscrição do aluno"),numero("versao","Versão da nota consultada"),texto("motivo","Motivo para desconsiderar"));
        a.alterar("docente-frequencia",docente,"Lançar ou corrigir frequência","Presença inteira por tempo. Atraso dentro da tolerância mantém presença; ausência de lançamento não vira falta automaticamente.",DOCENTE,(c,f)->{
            var p=new DiarioAcademico.LancarFrequencia(f.id("aula"),f.id("inscricao"),f.escolha("presenca",TempoLetivo.Presenca.class),f.numero("versao"),f.obrigatorio("motivo"));var b=diario(c).consultarDocente(p.inscricao());return gravar(Map.of("Chamada",p,"Boletim atual",b),x->concluido(()->diario(x).lancarFrequencia(p)));
        },id("aula","ID da aula por tempo"),id("inscricao","ID da inscrição do aluno"),escolha("presenca","Presença","PRESENTE","FALTA"),numero("versao","Versão da chamada (-1 se ainda não lançada)").padrao(-1),texto("motivo","Motivo do lançamento ou correção"));
        a.alterar("docente-importar",docente,"Importar chegadas simuladas","Eventos fictícios para a integração de frequência. Um evento por linha: origem;chave;aula;inscrição;data e hora local (AAAA-MM-DDTHH:mm:ss). Cada linha tem resultado próprio.",DOCENTE,(c,f)->{
            var lista=new ArrayList<DiarioAcademico.Chegada>();int linha=0;
            for(String texto:f.obrigatorio("eventos").lines().toList()) {linha++;if(texto.isBlank())continue;var itens=texto.split(";",-1);if(itens.length!=5)throw new IllegalArgumentException("Confira os cinco campos da linha "+linha+".");try{lista.add(new DiarioAcademico.Chegada(itens[0],itens[1],Long.parseLong(itens[2].strip()),Long.parseLong(itens[3].strip()),LocalDateTime.parse(itens[4].strip())));}catch(IllegalArgumentException|DateTimeException e){throw new IllegalArgumentException("Confira os campos e o instante local da linha "+linha+".");}}
            if(lista.isEmpty() || lista.size()>200)throw new IllegalArgumentException("Informe de 1 a 200 eventos por importação.");
            for(var e:lista)diario(c).consultarDocente(e.inscricao());
            return gravar(lista,x->diario(x).importarSimulados(lista));
        },texto("eventos","Linhas dos eventos simulados"));
        a.alterar("docente-fechar",docente,"Fechar resultado pelo diário","Exige período encerrado, carga apurada, avaliações e chamadas completas. Após o fechamento, correções usam retificação.",DOCENTE,(c,f)->{
            long i=f.id("inscricao");String motivo=f.obrigatorio("motivo");var b=diario(c).consultarDocente(i);
            if(!b.pendencias().isEmpty())throw new IllegalArgumentException(String.join(" ",b.pendencias()));
            return gravar(Map.of("Diário conferido",b,"Motivo",motivo),x->diario(x).fecharConferido(i,b,motivo));
        },id("inscricao","ID da inscrição a encerrar"),texto("motivo","Fundamento do fechamento"));
        a.alterar("docente-retificar",docente,"Solicitar retificação de resultado","Somente o professor da turma propõe a correção de resultado fechado; a coordenação decide. Valem as regras do período de origem.",DOCENTE,(c,f)->{
            long i=f.id("inscricao");var p=new RetificacaoResultado(f.decimal("p1"),f.decimal("p2"),f.decimalOpcional("pf"),f.id("total"),f.numero("presentes"));String motivo=f.obrigatorio("motivo");var r=retificacao(c).preparar(i,p);
            return gravar(Map.of("Inscrição",i,"Proposta",p,"Resultado proposto",r,"Motivo",motivo),x->retificacao(x).solicitar(i,p,motivo));
        },id("inscricao","ID da inscrição encerrada"),decimal("p1","P1 corrigida"),decimal("p2","P2 corrigida"),decimal("pf","PF corrigida, se aplicável").facultativo(),numero("total","Total de minutos considerados"),numero("presentes","Minutos de presença"),texto("motivo","Motivo da retificação"));
        a.consultar("docente-retificacoes",docente,"Histórico de retificações","Propostas e decisões de correção do resultado.",DOCENTE,(c,f)->retificacao(c).historico(f.id("inscricao")),id("inscricao","ID da inscrição"));
    }
}

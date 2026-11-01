# Validação do Codex para SCOA v0.6

Em 01/11/2026 às 14:43:43 UTC−03:00, Maven verify -Pmysql-it terminou com BUILD SUCCESS na pasta do projeto, usando instância MySQL isolada. Resultado: 77 testes unitários e 95 de integração, sem falhas, erros ou ignorados; 172 execuções, incluindo as 140 regressões da v0.5. A validação do autor ainda está pendente. Não há validação manual do autor atribuída a esta execução.

Versão 0.6.0-SNAPSHOT. Base de código anterior dc1a6f3 e fechamento documental 4d75429. Fontes atuais identificadas por SHA-256 em FONTES_SHA256.json; não há commit de implementação atribuído retrospectivamente aos testes.

## Resultados por classe

| Classe | Execuções | Falhas/erros/ignorados |
|---|---:|---|
| ServicoSalaTest | 9 | 0/0/0 |
| AlocacaoTest | 15 | 0/0/0 |
| BaseAcademicaTest | 9 | 0/0/0 |
| CadastroAcademicoTest | 9 | 0/0/0 |
| MatriculaTest | 3 | 0/0/0 |
| ProfessoresDisciplinasTest | 9 | 0/0/0 |
| SalaTest | 23 | 0/0/0 |
| AlocacaoJdbcIT | 17 | 0/0/0 |
| BaseAcademicaJdbcIT | 17 | 0/0/0 |
| CadastroAcademicoJdbcIT | 12 | 0/0/0 |
| MatriculaJdbcIT | 29 | 0/0/0 |
| ProfessoresDisciplinasJdbcIT | 14 | 0/0/0 |
| SalaJdbcIT | 6 | 0/0/0 |

## Rastreabilidade P06

Plano registrado antes da implementação. P06-01: MatriculaTest.p0601 e persistência DATE/Unicode em p0610DatasLimiteEMotivoUnicode. P06-02: p0602CriacaoEHistorico. P06-03: três execuções de p0603PendenciaEncerradaOuImpeditiva e p0603NaoImpeditiva. P06-04: quatro variantes de p0604IdentificacaoOuSituacaoIndisponivel e p0604FalhaSqlBibliotecaNaoLibera. P06-05/06: p0605DuplicidadeEReingresso e p0605LegadoDesconhecidoBloqueia. P06-07: matriz de domínio e p0607TransicoesEHistorico. P06-08: p0608RetornoBloqueadoEPreservacao e p0608RetornoIndisponivelMantemHistorico. P06-09: duas variantes de p0609CorrecaoSemBibliotecaESemMudanca. P06-10: quatro variantes de p0610ReferenciasInvalidas e persistência das datas limite. P06-11: p0611RollbackExterno e duas variantes de p0611FalhaNoHistoricoDesfazMatricula, com preservação de alteração externa anterior ao savepoint. P06-12: p0612CriacoesConcorrentes e p0612BibliotecaEscritorCoordenado. P06-13: p0613PeriodoProtegidoELegado. P06-14: p0614ConsoleConfirmacaoCancelamentoEEof cobre criação, transições, correção, consultas, cancelamento, entradas inválidas e interrupção antes da confirmação. P06-15: regressão completa e conferência independente da migração.

A conferência independente aplicou o DDL v7.5 a uma segunda base descartável no mesmo servidor isolado, inseriu matrícula legada, aplicou V006 e comparou as cinco colunas originais antes/depois: conteúdo idêntico. Período permaneceu NULL, histórico ficou vazio e a estrutura passou a 72 tabelas. Os arquivos de comparação e os XMLs finais permanecem nas evidências privadas.

## Ambiente e preservação

Windows, OpenJDK 25.0.1, Maven 3.9.11, MySQL 8.0.46, Connector/J 26.7.0 e JUnit 6.0.1. Porta temporária exclusiva 33316, bind local; bancos do autor e configurações do IntelliJ não foram alterados. DDL v7.5 original e Word v7.8 foram preservados; o hash do Word coincide com o fechamento anterior. A instância temporária foi encerrada ao concluir a validação.

Uma tentativa preliminar falhou por conexão local bloqueada/instância não persistente (91 erros de preparação); o log foi preservado em separado. A infraestrutura foi corrigida antes das execuções aprovadas. As execuções de desenvolvimento anteriores sobre fontes em preparação não substituem a execução final identificada neste relatório. Nenhum resultado foi atribuído ao autor.

## Limites

RF-MA-09 / UC-MA-09 avançam no recorte de matrícula; UC-MB-10 é integração de leitura, sem produtores completos da biblioteca. Conclusão de curso, inscrição, créditos, planos, autenticação/autorização e demais módulos seguem pendentes. O protocolo de concorrência bibliotecária exige que escritores futuros obtenham a trava do usuário bibliotecário. Escritas externas que ignorem o protocolo não são abrangidas. A migração é aditiva e exige aplicação única após backup; não foi aplicada aos bancos do autor nesta preparação.
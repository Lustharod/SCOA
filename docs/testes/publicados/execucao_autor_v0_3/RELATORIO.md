# Validação do SCOA v0.3

Versão: **0.3.0-SNAPSHOT**. Código executado: **e9efc1b**. Responsável: **Rodrigo Lustosa**. Consolidação: **28/10/2026**.

A execução automatizada terminou em **27/10/2026 às 22:49:00, UTC−03:00**, com **56 testes unitários e 35 testes de integração**, zero falhas, erros ou testes ignorados e **BUILD SUCCESS**. As capturas do roteiro manual e da conferência do banco foram realizadas em 28/10/2026. Datas futuras presentes nos cadastros são dados de teste dos períodos letivos, e não datas da execução.

## Escopo e referência

A entrega contempla períodos letivos e turmas, vinculados a disciplinas e professores existentes, e sua integração com a alocação de salas. Referências: RF-MA-06 / UC-MA-06 e regressão dos fluxos de salas e alocação de RF-MA-07 e RF-MA-08, conforme a documentação v7.5. As restrições iniciais e os limites da implementação estão descritos na [nota técnica da v0.3](../../../SCOA_Implementacao_v0_3.md).

Esta é a consolidação da validação efetivamente realizada. O quadro conceitual abaixo organiza os comportamentos verificados e seus resultados esperados, sem valores de entrada. Não representa um planejamento anterior à execução. O plano geral ligado aos requisitos e a atualização correspondente do documento acadêmico serão tratados em uma etapa própria.

## Comportamentos verificados na v0.3

| ID | Comportamento / resultado esperado | Comprovação da execução | Resultado |
|---|---|---|---|
| V03-01 | Criar período válido e manter sua consulta e listagem | Manual 01 e 02; CadastroAcademicoJdbcIT | Aprovado |
| V03-02 | Criar turma com referências ativas; consultar e atualizar preservando a identidade | Manual 02, 04 e 09; cadastraConsultaListaEAtualizaTurma | Aprovado |
| V03-03 | Rejeitar alteração das datas ou inativação de período vinculado; permitir correção da descrição | Manual 04 e 05 para datas/descrição; periodoVinculadoPreservaDatasESituacaoMasPermiteDescricao também verifica inativação | Aprovado |
| V03-04 | Atualizar e inativar período livre; impedir nova turma em período inativo | Manual 14 e 15 para manutenção; atualizaEInativaPeriodoSemTurmas também verifica a recusa de nova turma | Aprovado |
| V03-05 | Rejeitar referências acadêmicas inexistentes ou inativas sem criar turma | rejeitaVinculosInexistentesOuInativosSemCriarTurma | Aprovado |
| V03-06 | Respeitar campos obrigatórios, limites de texto, identificadores, capacidade e datas do banco | CadastroAcademicoTest; regressões de AlocacaoTest para intervalo invertido | Aprovado |
| V03-07 | Consultar disponibilidade e registrar alocação válida; preservar vínculos enquanto houver horários | Manual 06 e 09; manutencaoDeTurmaPreservaAlocacoes | Aprovado |
| V03-08 | Impedir capacidade de turma incompatível com sala alocada e inativação com compromisso vigente | Manual 10; manutencaoDeTurmaPreservaAlocacoes | Aprovado |
| V03-09 | Remover alocação, inativar turma sem compromissos, preservar a linha e recusar nova alocação | Manual 11 e 12; banco; inativacaoPreservaRegistroEImpedeNovasAlocacoes | Aprovado |
| V03-10 | Cancelar criação sem inserir registro | Manual 15: mensagem de cancelamento e listagem sem o cadastro cancelado | Aprovado |
| V03-11 | Preservar inscrições existentes e capacidade mínima, além da transação do chamador | preservaInscricoesExistentesECapacidadeMinima; alteracaoInvalidaPreservaDadosERollbackDoChamador | Aprovado |
| V03-12 | Coordenar criação de turma e alteração de período; coordenar capacidade e alocação concorrentes | turmaConcorrenteImpedeMudancaDasDatasDoPeriodo; capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala nas duas ordens | Aprovado |

Os nomes dos métodos correspondem a CadastroAcademicoJdbcIT, salvo indicação de outra classe. As 70 execuções da versão anterior foram incluídas na bateria de regressão. Um cenário conceitual pode ter várias verificações e execuções parametrizadas; a quantidade de linhas deste quadro não é a quantidade de testes JUnit.

## Resultados automatizados

| Classe | Tipo | Execuções | Falhas / erros / ignorados |
|---|---|---:|---|
| ServicoSalaTest | Unitário | 9 | 0 / 0 / 0 |
| SalaTest | Unitário | 23 | 0 / 0 / 0 |
| AlocacaoTest | Unitário | 15 | 0 / 0 / 0 |
| CadastroAcademicoTest | Unitário | 9 | 0 / 0 / 0 |
| SalaJdbcIT | Integração | 6 | 0 / 0 / 0 |
| AlocacaoJdbcIT | Integração | 17 | 0 / 0 / 0 |
| CadastroAcademicoJdbcIT | Integração | 12 | 0 / 0 / 0 |
| **Total** | | **91** | **0 / 0 / 0** |

As listagens completas das execuções estão nos [resultados unitários](surefire-reports/RESULTADOS.md) e nos [resultados de integração](failsafe-reports/RESULTADOS.md), extraídos dos XMLs originais. O [ambiente](ambiente.txt) identifica a execução. As capturas automatizadas 01, 02 e 03 mostram, respectivamente, a versão, o total unitário e o total de integração com BUILD SUCCESS.

## Evidências manuais e do banco

Foram selecionadas 17 capturas originais: três automatizadas, onze manuais e três do banco. A numeração original foi mantida. As capturas intermediárias 03, 07, 08 e 13 continuam no arquivo local; foram omitidas da seleção por enquadramento parcial, continuidade ou repetição de verificações já representadas.

| Captura | Conteúdo observado |
|---|---|
| [Automatizada 01](capturas/v0.3%20-%20Testes%20unit%C3%A1rios%20e%20de%20integra%C3%A7%C3%A3o%20-%2001.png) | Versão 0.3.0-SNAPSHOT e identificação da execução no IntelliJ |
| [Automatizada 02](capturas/v0.3%20-%20Testes%20unit%C3%A1rios%20e%20de%20integra%C3%A7%C3%A3o%20-%2002.png) | Total de 56 testes unitários sem falhas, erros ou ignorados |
| [Automatizada 03](capturas/v0.3%20-%20Testes%20unit%C3%A1rios%20e%20de%20integra%C3%A7%C3%A3o%20-%2003.png) | Total de 35 testes de integração e BUILD SUCCESS |
| Manual 01 e 02 | Referências ativas, criação de período e criação de turma |
| Manual 04 e 05 | Turma atualizada; datas do período vinculado recusadas; descrição corrigida |
| Manual 06 e 09 | Consulta de disponibilidade e alocação; mensagem de restrição de vínculos e atualização válida de capacidade |
| Manual 10 | Capacidade acima da sala e inativação com alocação recusadas |
| Manual 11 e 12 | Desalocação, turma inativada e nova alocação recusada |
| Manual 14 e 15 | Período livre criado, atualizado e inativado; criação cancelada sem aparecer na listagem |
| Banco — Ambiente | Schema scoa_java_teste e MySQL 8.0.46, em 28/10/2026 |
| Banco — Turma e período | Turma mantida no banco como INATIVO, com os vínculos e os valores finais preservados |
| Banco — Alocações removidas | Consulta da turma sem horários restantes; a linha NULL exibida pelo Workbench é o espaço de inclusão da grade, não um registro persistido |

Os arquivos estão na pasta [capturas](capturas/). A capacidade da turma foi atualizada para um valor intermediário e posteriormente para o valor final mostrado no banco; a seleção preserva essa sequência. A consulta de estado final complementa as evidências das operações, sem reconstruir as tentativas recusadas.

## Integridade e limites

O [manifesto](MANIFESTO.md) registra os hashes das capturas e dos relatórios originais utilizados. Não foram recriadas telas nem editados resultados. Os dados acadêmicos são sintéticos. A seleção revisada não apresenta credenciais; os XMLs completos, que incluem propriedades e caminhos locais, permanecem no arquivo local junto das capturas não selecionadas e dos guias de aprendizado.

O resultado atesta esta versão e este escopo. Não representa validação de autenticação, autorização, interface web, manutenção completa de disciplinas/professores ou do fluxo de inscrições. A proteção de inscrições existente foi verificada com dados sintéticos e não equivale à entrega do módulo de inscrições. O Word, os diagramas e o DDL v7.5 não foram alterados neste fechamento.

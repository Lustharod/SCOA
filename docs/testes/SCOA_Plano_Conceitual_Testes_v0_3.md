# Plano conceitual de testes do SCOA v0.3

Complemento da documentação v7.5. Consolidação em 28/10/2026.

## 1 Objetivo e origem do plano

Este plano consolida os comportamentos verificáveis dos cadastros de salas, períodos letivos e turmas e da alocação de salas disponíveis no SCOA v0.3. Seu objetivo é ligar requisitos, casos de uso, condições de teste, resultados esperados e evidências, com linguagem conceitual e sem valores concretos de entrada.

A consolidação foi elaborada em 28/10/2026, após o desenvolvimento e a execução da v0.3. Não constitui planejamento realizado antes do código. As condições e os resultados esperados foram confrontados com os testes existentes e com os registros da execução do autor. A organização aqui apresentada servirá de referência para planejar as próximas entregas antes de implementá-las.

A base funcional é a documentação aprovada v7.5. A versão de implementação permanece 0.3.0-SNAPSHOT, com código testado no commit e9efc1b e fechamento documental no commit a3d0282. Este complemento não altera requisitos, regras institucionais, diagramas, DDL ou código.

## 2 Escopo e limites da cobertura

O escopo reúne RF-MA-06 e UC-MA-06, RF-MA-07 e UC-MA-07, RF-MA-08 e UC-MA-08. A manutenção de períodos é suporte à organização de turmas por período. RN-MA-03 fundamenta capacidade e disponibilidade de salas. RN-MA-02 aparece somente na proteção da capacidade ao manter uma turma com inscrições existentes; o fluxo de inscrição não está entregue.

Os casos de uso aprovados incluem condições de acesso e responsabilidades dos atores que a interface de console ainda não implementa. Os resultados deste plano atestam os comportamentos delimitados, e não a aprovação integral dos casos de uso ou requisitos. Autenticação, autorização e interface web permanecem pendentes.

A política inicial impede mudar datas ou situação de período com turma vinculada. A verificação de capacidade da turma conta conservadoramente todos os registros de inscrição. Esses limites da v0.3 não substituem futuras decisões sobre reprogramação de períodos ou inscrições vigentes, canceladas e históricas.

## 3 Organização e critérios de execução

Os identificadores CT-PC-01 a CT-PC-28 pertencem a este plano conceitual e não substituem os identificadores anteriores. Cada caso informa sua origem, condição, ação e resultado esperado. As condições expressam classes válidas, inválidas e de limite, sem nomes, capacidades, datas ou horários de exemplo. Valores concretos permanecem no código dos testes e nas evidências de execução.

Um caso conceitual pode reunir várias verificações e aparecer em mais de um nível. Testes unitários verificam domínio e serviço com colaboradores em memória; testes de integração verificam persistência, transações e concorrência no MySQL real. A execução manual verifica os fluxos de console identificados. A mesma execução pode contribuir para mais de um caso conceitual e não deve ser somada novamente.

Para executar a bateria, usar o código identificado e ambiente compatível com a configuração do projeto, schema exclusivo scoa_java_teste e referências sintéticas. O console de testes deve apontar para esse mesmo schema. Não usar a base de uso corrente como ambiente de testes. A preparação e a limpeza devem preservar registros alheios à execução.

Executar a verificação Maven com o perfil mysql-it, além dos fluxos manuais previstos. Comparar resultados observados e esperados; conferir a persistência e a ausência de gravação nas recusas, nos limites efetivamente exercitados. Preservar os relatórios antes de uma nova execução substituí-los. Credenciais ficam nas configurações locais e não integram o plano ou as evidências públicas.

Um caso só pode ser marcado aprovado quando todas as suas condições previstas tiverem execução rastreável e resultado compatível. Falha exige correção e nova execução do caso afetado e das regressões pertinentes. Caso não executado ou bloqueado não equivale a aprovado. O encerramento exige execução de todos os casos incluídos; um caso não deve ser removido apenas para ocultar falta de execução.

A evidência de encerramento deve identificar versão, commit, responsável, data e hora reais, ambiente, resultado, falhas e limitações. Aplicar docs/testes/PADRAO_EVIDENCIAS.md: separar autor e Codex, manter os originais privados e publicar somente a seleção revisada. A execução registrada para a v0.3 é histórica; esta atualização documental não executou novamente o sistema.

## 4 Casos conceituais de teste

### CT-PC-01 Validar os dados de sala

Origem: RF-MA-07; UC-MA-07.

Condição: Dados de sala submetidos às condições de obrigatoriedade e aos limites definidos no modelo físico.

Ação: Criar representações de sala com capacidades e identificadores válidos e inválidos, textos ausentes, nos limites e além deles, incluindo Unicode e espaços nas extremidades.

Resultado esperado: Aceitar os valores representáveis e normalizar os espaços externos. Rejeitar capacidade, identificador, texto ou situação inválidos. Preservar identificadores válidos na faixa numérica do banco sem truncamento.

### CT-PC-02 Cadastrar consultar e atualizar sala

Origem: RF-MA-07; UC-MA-07.

Condição: Cadastro válido e, para atualização, sala existente.

Ação: Cadastrar, consultar e listar a sala; atualizar seus dados com informações válidas.

Resultado esperado: Atribuir identificador ao novo cadastro, recuperar os campos persistidos e atualizar a mesma sala, preservando identidade e situação, sem criar outro registro.

### CT-PC-03 Recusar gravação inválida de sala

Origem: RF-MA-07; UC-MA-07.

Condição: Tentativa de criação ou alteração de sala com capacidade inválida.

Ação: Enviar a operação ao serviço de cadastro.

Resultado esperado: Recusar a operação antes da gravação. Na criação, não inserir sala; na atualização, preservar o cadastro anterior.

### CT-PC-04 Inativar sala preservando o cadastro

Origem: RF-MA-07; UC-MA-07.

Condição: Sala existente e sem compromisso que impeça a inativação.

Ação: Inativar a sala, repetir a inativação e atualizar dados da sala inativa.

Resultado esperado: Manter o registro consultável e sua identidade. A repetição não cria outro registro; a atualização não reativa a sala implicitamente.

### CT-PC-05 Tratar ausência e consulta de sala

Origem: RF-MA-07; UC-MA-07.

Condição: Identificador sem sala correspondente, sala removida ou listagem retornada pelo serviço.

Ação: Consultar, atualizar ou inativar sala ausente; tentar alterar a coleção retornada; salvar uma sala existente sem mudanças.

Resultado esperado: Sinalizar a ausência sem recriar cadastro removido. Impedir que a coleção retornada modifique o cadastro. Aceitar atualização sem mudanças sem indicar ausência indevida, inclusive na configuração JDBC de linhas alteradas.

### CT-PC-06 Preservar texto como dado

Origem: RF-MA-07; UC-MA-07.

Condição: Identificação válida de sala contendo aspas e caracteres que também podem aparecer em comandos de banco.

Ação: Cadastrar e recuperar a sala.

Resultado esperado: Persistir e recuperar o texto como dado, sem interpretá-lo como comando. Esta verificação isolada não representa aprovação geral da segurança do sistema.

### CT-PC-07 Validar período turma e referências

Origem: RF-MA-06; UC-MA-06.

Condição: Objetos de período e turma novos ou reconstruídos com dados sujeitos aos limites do modelo físico.

Ação: Variar obrigatoriedade, espaços externos, tamanho de texto, Unicode, capacidade, identificadores e datas representáveis; incluir intervalo de datas invertido.

Resultado esperado: Aceitar a construção válida sem identificador antes da persistência e normalizar espaços. Rejeitar campos obrigatórios ausentes, capacidades e identificadores inválidos, textos excessivos, datas não representáveis e intervalo invertido.

### CT-PC-08 Manter turma com referências válidas

Origem: RF-MA-06; UC-MA-06.

Condição: Disciplina, professor e período previamente cadastrados e ativos.

Ação: Cadastrar, consultar, listar e atualizar turma; consultar as referências acadêmicas disponíveis.

Resultado esperado: Disponibilizar a turma com seus vínculos e recuperar a alteração mantendo identidade e situação. Apresentar as referências ativas utilizadas no cadastro.

### CT-PC-09 Recusar referências ausentes ou inativas

Origem: RF-MA-06; UC-MA-06.

Condição: Tentativa de criar turma com disciplina, professor ou período inexistente, ou com referência inativa.

Ação: Solicitar a criação da turma nas condições exercitadas pela implementação.

Resultado esperado: Recusar a criação sem acrescentar turma. A referência inativa de período é exercitada após sua inativação; professores e disciplinas inativos são preparados como dados de teste.

### CT-PC-10 Manter período sem turma vinculada

Origem: RF-MA-06; UC-MA-06; suporte ao período letivo.

Condição: Período cadastrado sem turma vinculada.

Ação: Consultar, listar, alterar descrição e datas válidas e inativar o período.

Resultado esperado: Recuperar os dados alterados e manter o registro inativo consultável. Impedir nova turma nesse período enquanto estiver inativo.

### CT-PC-11 Proteger período com turma vinculada

Origem: RF-MA-06; UC-MA-06; restrição inicial da v0.3.

Condição: Período com turma vinculada.

Ação: Tentar alterar suas datas ou inativá-lo; corrigir somente sua descrição.

Resultado esperado: Recusar mudança de datas e situação, preservando os dados anteriores; permitir a correção da descrição. A regra é uma restrição inicial de implementação, sem aprovar um fluxo de reprogramação de períodos.

### CT-PC-12 Preservar turma diante de operação inválida

Origem: RF-MA-06; UC-MA-06.

Condição: Turma existente e operação inválida; identificadores sem registro correspondente para consultas e inativações.

Ação: Tentar alteração inválida e operações sobre período ou turma ausentes.

Resultado esperado: Recusar a operação, preservar os dados existentes e informar a ausência sem criar registros substitutos.

### CT-PC-13 Preservar alocações na manutenção de turma

Origem: RF-MA-06 e RF-MA-08; UC-MA-06 e UC-MA-08; RN-MA-03.

Condição: Turma com alocação em período atual ou futuro.

Ação: Tentar aumentar capacidade além da sala, inativar a turma ou substituir seus vínculos; atualizar a capacidade até o limite compatível; retirar a alocação antes de mudar o período.

Resultado esperado: Recusar alterações incompatíveis. Aceitar capacidade compatível; exigir desalocação antes da mudança de vínculo exercitada. Preservar a integridade da turma e dos horários.

### CT-PC-14 Preservar inscrições existentes na turma

Origem: RF-MA-06; UC-MA-06; proteção parcial de RN-MA-02.

Condição: Turma com inscrições sintéticas já registradas.

Ação: Tentar reduzir capacidade abaixo da quantidade registrada, inativar ou mudar o período; ajustar capacidade até a quantidade registrada.

Resultado esperado: Recusar capacidade insuficiente e mudanças de vínculo ou situação protegidas; aceitar capacidade suficiente. A contagem conservadora inclui todos os registros e não valida criação, cancelamento ou elegibilidade de inscrições.

### CT-PC-15 Inativar turma sem compromissos

Origem: RF-MA-06 e RF-MA-08; UC-MA-06 e UC-MA-08.

Condição: Turma sem impedimentos para inativação.

Ação: Inativar, consultar, atualizar dados da turma inativa e tentar nova alocação.

Resultado esperado: Preservar o cadastro e a situação inativa, sem reativação implícita; recusar a nova alocação e manter a lista de horários sem a inclusão recusada.

### CT-PC-16 Validar capacidade e situação para alocação

Origem: RF-MA-08; UC-MA-08; RN-MA-03.

Condição: Turma válida e salas com capacidade igual, inferior ou suficiente, em situações ativa e inativa.

Ação: Validar a sala e tentar registrar alocações nas condições de capacidade e situação exercitadas.

Resultado esperado: Aceitar a igualdade de capacidades e a alocação compatível. Rejeitar sala insuficiente ou inativa, sem gravar horário para a tentativa recusada.

### CT-PC-17 Validar dia e intervalo de horário

Origem: RF-MA-08; UC-MA-08; RN-MA-03.

Condição: Solicitação de alocação sujeita ao domínio de dias e à representação de horário utilizada.

Ação: Informar dia inválido, início igual ou posterior ao término, início ausente, precisão não suportada ou dia sem ocorrência no período.

Resultado esperado: Rejeitar as representações inválidas e o dia sem ocorrência. A avaliação de conflitos usa somente intervalos e dias válidos.

### CT-PC-18 Distinguir conflito de uso permitido

Origem: RF-MA-08; UC-MA-08; RN-MA-03.

Condição: Horários válidos que podem se sobrepor ou apenas se tocar; períodos com e sem datas comuns.

Ação: Comparar sobreposição parcial e contenção, adjacência, dias distintos, períodos distintos e períodos com identificadores diferentes e datas comuns; verificar interseção de datas sem ocorrência do dia escolhido.

Resultado esperado: Reconhecer conflito quando dia, horário e ocorrência no intervalo de datas coincidirem. Permitir adjacência, dias diferentes, períodos sem sobreposição e interseção sem ocorrência do dia escolhido.

### CT-PC-19 Consultar disponibilidade alocar e desalocar

Origem: RF-MA-08; UC-MA-08; RN-MA-03.

Condição: Turma existente e conjunto de salas livres, insuficientes, inativas ou ocupadas.

Ação: Consultar disponibilidade, selecionar sala elegível, registrar alocação, consultar horários e retirar a alocação.

Resultado esperado: Listar somente salas elegíveis. Recuperar a associação gravada; após a retirada, não listar o horário removido e voltar a apresentar a sala quando disponível.

### CT-PC-20 Recusar conflito e referências ausentes na gravação

Origem: RF-MA-08; UC-MA-08; RN-MA-03.

Condição: Sala ocupada ou turma já alocada em horário conflitante; tentativa com turma, sala ou horário inexistente.

Ação: Tentar alocar a mesma sala a outra turma em períodos sobrepostos ou a mesma turma em outra sala no mesmo horário; tentar alocar e desalocar com referências ausentes.

Resultado esperado: Recusar os conflitos e as referências ausentes sem gravar a associação recusada. Considerar as datas dos períodos, mesmo quando seus identificadores forem distintos.

### CT-PC-21 Proteger sala comprometida

Origem: RF-MA-07 e RF-MA-08; UC-MA-07 e UC-MA-08; RN-MA-03.

Condição: Sala alocada a turma em período atual ou futuro.

Ação: Tentar reduzir a capacidade abaixo da turma comprometida ou inativar a sala.

Resultado esperado: Recusar ambas as alterações incompatíveis e preservar capacidade e situação anteriores.

### CT-PC-22 Respeitar a transação do chamador

Origem: RF-MA-06 e RF-MA-08; UC-MA-06 e UC-MA-08; consistência da persistência.

Condição: Operações executadas dentro de uma transação aberta pelo chamador e operação com transação gerida pelo repositório.

Ação: Cadastrar ou alterar dados, executar operação inválida e desfazer a transação externa; verificar o estado de confirmação automática na operação própria exercitada.

Resultado esperado: Não confirmar indevidamente a transação externa; permitir desfazer seus registros. Restaurar a confirmação automática quando a operação a gerencia. A recusa não deve ser comunicada como sucesso.

### CT-PC-23 Coordenar operações concorrentes sobre sala

Origem: RF-MA-07 e RF-MA-08; UC-MA-07 e UC-MA-08; RN-MA-03.

Condição: Conexões independentes disputando a mesma sala ou a disponibilidade de uma turma.

Ação: Concorrer alteração de capacidade e inativação nas ordens previstas; disputar sala ou turma com alocações; concorrer alocação com redução de capacidade ou inativação de sala.

Resultado esperado: Coordenar as operações sem perda de situação ou capacidade e sem duplicar sala ou turma em horário conflitante. Após a primeira confirmação, revalidar a operação concorrente e recusar a que se tornou incompatível.

### CT-PC-24 Coordenar operações concorrentes sobre período e turma

Origem: RF-MA-06 e RF-MA-08; UC-MA-06 e UC-MA-08; RN-MA-03.

Condição: Conexões independentes criando turma e alterando período, ou disputando capacidade da turma e alocação.

Ação: Criar turma durante tentativa de alterar datas do período; concorrer aumento de capacidade e alocação nas duas ordens exercitadas.

Resultado esperado: Após o vínculo da turma, preservar as datas do período. Na disputa de capacidade e alocação, manter apenas o resultado compatível com a operação confirmada, sem exceder a capacidade da sala.

### CT-PC-25 Confirmar cadastros e correções pelo console

Origem: RF-MA-06; UC-MA-06; interface de console da v0.3.

Condição: Console conectado ao ambiente de testes e referências acadêmicas ativas disponíveis.

Ação: Criar período e turma, consultar e atualizar a turma; tentar mudar datas de período vinculado e corrigir a descrição.

Resultado esperado: Apresentar os dados e confirmar a gravação válida. Recusar as datas do período vinculado e aceitar a correção de descrição; permitir conferir o resultado nas consultas.

### CT-PC-26 Conferir alocação e proteções pelo console

Origem: RF-MA-06 e RF-MA-08; UC-MA-06 e UC-MA-08; RN-MA-03.

Condição: Turma ativa, sala elegível e alocação preparada no roteiro manual.

Ação: Consultar disponibilidade, alocar, tentar alterar vínculos protegidos, atualizar capacidade compatível, tentar capacidade superior à sala e inativação com compromisso.

Resultado esperado: Registrar a associação válida e a capacidade compatível. Exibir recusa das alterações protegidas, sem aceitar a operação incompatível.

### CT-PC-27 Conferir retirada de horário e inativação

Origem: RF-MA-06 e RF-MA-08; UC-MA-06 e UC-MA-08.

Condição: Turma com alocação registrada no roteiro.

Ação: Retirar a alocação, inativar a turma e tentar nova alocação; conferir o estado final no banco.

Resultado esperado: Remover o horário e preservar o registro da turma inativa com seus vínculos. Recusar nova alocação e confirmar ausência de horários remanescentes para a turma.

### CT-PC-28 Manter período livre e cancelar criação

Origem: RF-MA-06; UC-MA-06; interface de console da v0.3.

Condição: Período sem turma vinculada e nova criação aguardando confirmação.

Ação: Criar, alterar e inativar período livre; cancelar outra criação e consultar a listagem.

Resultado esperado: Persistir a manutenção confirmada. Na criação cancelada, informar o cancelamento e não acrescentar cadastro à listagem.

## 5 Rastreabilidade e comprovação

As referências UT e IT apontam, respectivamente, para os RESULTADOS.md das pastas surefire-reports e failsafe-reports da execução pública do autor v0.3. Todas as invocações parametrizadas listadas nesses resultados integram as referências abaixo.

### CT-PC-01

- SalaTest.aceitaCapacidadeDentroDosLimites
- SalaTest.rejeitaCapacidadeForaDosLimites
- SalaTest.rejeitaIdentificacaoAusente
- SalaTest.rejeitaLocalizacaoAusente
- SalaTest.ctJuSala005RejeitaTextosQueExcedemOsCamposDoBanco
- SalaTest.ctJuSala006AceitaLimitesDeTextoECaracteresUnicode
- SalaTest.ctJuSala007RemoveEspacosDasExtremidades
- SalaTest.rejeitaIdForaDoIntervaloDoBanco
- SalaTest.ctJuSala009PreservaIdAcimaDoLimiteDeIntDoJava
- SalaTest.rejeitaSituacaoAusente

Resultado histórico: aprovado nas condições descritas.

### CT-PC-02

- ServicoSalaTest.ctJuSala011CadastraSalaEDevolveIdentificador
- ServicoSalaTest.ctJuSala013AtualizaMantendoIdentidadeESituacao
- SalaJdbcIT.ctJiSala001CadastraERecuperaTodosOsCampos
- SalaJdbcIT.ctJiSala002AtualizaSemCriarOutraSala

Resultado histórico: aprovado nas condições descritas.

### CT-PC-03

- ServicoSalaTest.ctJuSala012DadoInvalidoNaoChegaAoRepositorio
- ServicoSalaTest.ctJuSala014AtualizacaoInvalidaPreservaCadastroAnterior

Resultado histórico: aprovado nas condições descritas.

### CT-PC-04

- ServicoSalaTest.ctJuSala015InativaSemExcluirHistoricoDoCadastro
- ServicoSalaTest.ctJuSala016RepetirInativacaoMantemOEstado
- ServicoSalaTest.ctJuSala019AtualizarSalaInativaNaoReativaImplicitamente
- SalaJdbcIT.ctJiSala003InativaPreservandoRegistro

Resultado histórico: aprovado nas condições descritas.

### CT-PC-05

- ServicoSalaTest.ctJuSala017OperacoesSobreSalaAusenteSaoRejeitadas
- ServicoSalaTest.ctJuSala018ListagemNaoPermiteAlterarOsCadastrosPeloRetorno
- SalaJdbcIT.ctJiSala005SalaRemovidaNaoERecriadaPorAtualizacao
- SalaJdbcIT.ctJiSala006SalvarSemAlteracoesNaoAcusaAusencia
- AlocacaoJdbcIT.atualizacaoSemMudancasComAffectedRowsNaoAcusaAusencia

Resultado histórico: aprovado nas condições descritas.

### CT-PC-06

- SalaJdbcIT.ctJiSala004TextoComAspasEComandoSqlPermaneceDado

Resultado histórico: aprovado nas condições descritas.

### CT-PC-07

- CadastroAcademicoTest.cadastraPeriodoETurmaSemIdERemoveEspacos
- CadastroAcademicoTest.camposObrigatorios
- CadastroAcademicoTest.limitesDeTextoEUnicode
- CadastroAcademicoTest.capacidadeInvalida
- CadastroAcademicoTest.idsInvalidos
- CadastroAcademicoTest.datasForaDoTipoDateSaoRejeitadas
- AlocacaoTest.datasInvalidasEIdsInvalidosSaoRejeitados

Resultado histórico: aprovado nas condições descritas.

### CT-PC-08

- CadastroAcademicoJdbcIT.cadastraConsultaListaEAtualizaTurma

Resultado histórico: aprovado nas condições descritas.

### CT-PC-09

- CadastroAcademicoJdbcIT.rejeitaVinculosInexistentesOuInativosSemCriarTurma
- CadastroAcademicoJdbcIT.atualizaEInativaPeriodoSemTurmas

Resultado histórico: aprovado nas condições descritas.

### CT-PC-10

- CadastroAcademicoJdbcIT.atualizaEInativaPeriodoSemTurmas

Resultado histórico: aprovado nas condições descritas.

### CT-PC-11

- CadastroAcademicoJdbcIT.periodoVinculadoPreservaDatasESituacaoMasPermiteDescricao

Resultado histórico: aprovado nas condições descritas.

### CT-PC-12

- CadastroAcademicoJdbcIT.alteracaoInvalidaPreservaDadosERollbackDoChamador
- CadastroAcademicoJdbcIT.recusaIdsAusentes

Resultado histórico: aprovado nas condições descritas.

### CT-PC-13

- CadastroAcademicoJdbcIT.manutencaoDeTurmaPreservaAlocacoes

Resultado histórico: aprovado nas condições descritas.

### CT-PC-14

- CadastroAcademicoJdbcIT.preservaInscricoesExistentesECapacidadeMinima

Resultado histórico: aprovado nas condições descritas.

### CT-PC-15

- CadastroAcademicoJdbcIT.inativacaoPreservaRegistroEImpedeNovasAlocacoes

Resultado histórico: aprovado nas condições descritas.

### CT-PC-16

- AlocacaoTest.capacidadeIgualADaTurmaEPermitida
- AlocacaoTest.salaMenorQueTurmaERejeitada
- AlocacaoTest.salaInativaERejeitada
- AlocacaoJdbcIT.rejeitaCapacidadeInsuficienteESalaInativaSemGravar

Resultado histórico: aprovado nas condições descritas.

### CT-PC-17

- AlocacaoTest.diaInvalidoERejeitado
- AlocacaoTest.intervaloInvalidoERejeitado
- AlocacaoTest.diaSemOcorrenciaNoPeriodoERejeitado

Resultado histórico: aprovado nas condições descritas.

### CT-PC-18

- AlocacaoTest.horariosAdjacentesNaoConflitam
- AlocacaoTest.diasDiferentesNaoConflitam
- AlocacaoTest.intersecaoEContencaoDeHorariosConflitam
- AlocacaoTest.periodosDistintosNaoConflitam
- AlocacaoTest.periodosComIdsDiferentesMasDatasSobrepostasConflitam
- AlocacaoTest.intersecaoDeDatasSemODiaEscolhidoNaoConflita
- AlocacaoJdbcIT.permiteAdjacenciaEDiasDiferentes
- AlocacaoJdbcIT.permitePeriodosSemSobreposicao

Resultado histórico: aprovado nas condições descritas.

### CT-PC-19

- AlocacaoJdbcIT.alocaListaEDesaloca
- AlocacaoJdbcIT.disponibilidadeFiltraCapacidadeSituacaoEConflitos

Resultado histórico: aprovado nas condições descritas.

### CT-PC-20

- AlocacaoJdbcIT.rejeitaConflitoDaSalaEntrePeriodosComDatasSobrepostas
- AlocacaoJdbcIT.rejeitaMesmaTurmaEmDuasSalasNoMesmoHorario
- AlocacaoJdbcIT.recusaEntidadesAusentes

Resultado histórico: aprovado nas condições descritas.

### CT-PC-21

- AlocacaoJdbcIT.preservaCapacidadeESituacaoDeSalaComprometida

Resultado histórico: aprovado nas condições descritas.

### CT-PC-22

- CadastroAcademicoJdbcIT.alteracaoInvalidaPreservaDadosERollbackDoChamador
- AlocacaoJdbcIT.transacaoExternaContinuaAbertaEAutocommitProprioERestaurado

Resultado histórico: aprovado nas condições descritas.

### CT-PC-23

- AlocacaoJdbcIT.alteracoesConcorrentesPreservamInativacaoECapacidade
- AlocacaoJdbcIT.alocacoesConcorrentesNaoDuplicamSalaNemTurma
- AlocacaoJdbcIT.alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala

Resultado histórico: aprovado nas condições descritas.

### CT-PC-24

- CadastroAcademicoJdbcIT.turmaConcorrenteImpedeMudancaDasDatasDoPeriodo
- CadastroAcademicoJdbcIT.capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala

Resultado histórico: aprovado nas condições descritas.

### CT-PC-25

Manuais 01, 02, 04 e 05; RELATORIO.md, V03-01 a V03-03.

Resultado histórico: aprovado nas condições descritas.

### CT-PC-26

Manuais 06, 09 e 10; RELATORIO.md, V03-07 e V03-08.

Resultado histórico: aprovado nas condições descritas.

### CT-PC-27

Manuais 11 e 12; Banco Turma e periodo e Alocacoes removidas; RELATORIO.md, V03-09.

Resultado histórico: aprovado nas condições descritas.

### CT-PC-28

Manuais 14 e 15; RELATORIO.md, V03-04 e V03-10.

Resultado histórico: aprovado nas condições descritas.

## 6 Registro da execução consolidada

A execução automatizada do autor terminou em 27/10/2026 às 22:49:00, UTC−03:00, no código e9efc1b. Os relatórios registram 56 execuções unitárias e 35 de integração, sem falhas, erros ou ignorados, com BUILD SUCCESS. As capturas manuais e do banco foram registradas em 28/10/2026.

Os 24 casos conceituais automatizados estão ligados aos métodos das sete classes executadas. Os quatro casos manuais estão ligados às capturas selecionadas do autor. A união das referências cobre as 91 execuções; o número de casos conceituais não é a quantidade de métodos, asserções ou execuções parametrizadas.

O estado consolidado dos 28 casos é aprovado na execução histórica indicada, dentro das condições e limites descritos. Essa conclusão não representa uma nova execução em 28/10/2026 pelo Codex, nem comprova cenários adicionais ausentes dos métodos e capturas identificados. Os documentos de origem e seus resultados permanecem preservados.

## 7 Pendências para as próximas entregas

Os demais requisitos mantêm os critérios iniciais da matriz de rastreabilidade do documento principal. Eles não são casos executáveis incluídos neste plano da v0.3. A cobertura parcial atual não autoriza declarar o sistema inteiro testado.

Antes de implementar professores e disciplinas, derivar casos próprios de RF-MA-02 e UC-MA-02 e de RF-MA-04 e UC-MA-04, incluindo a definição dos efeitos das alterações sobre vínculos existentes. Antes de inscrições, formalizar os estados e a ocupação de vagas de RF-MA-10 e UC-MA-10, com UC-MA-30, RN-MA-01 e RN-MA-02. Esses itens são dependências de planejamento e não recebem aprovação de execução neste documento.

Autenticação e autorização dependem das definições de identificação de acesso e perfis. Interface web exige verificar os fluxos completos e as condições de acesso previstas nos casos de uso. Os demais módulos dependem das pendências de requisitos e parâmetros já registradas na documentação aprovada.

Para cada nova entrega, selecionar requisitos e dependências, esclarecer regras pendentes e registrar os casos conceituais antes do código. Definir condições e resultados esperados, relacionar a implementação executável e executar todos os casos incluídos, mantendo as regressões. Registrar depois os resultados reais. Alterações de requisito, regra ou banco exigem revisar a rastreabilidade e a documentação afetada.

## 8 Documentos de referência

SCOA Sistema de Controle Acadêmico v7.5, seções de requisitos, regras de negócio, especificações de casos de uso e matriz de rastreabilidade. O presente plano é um complemento da documentação aprovada.

docs/SCOA_Implementacao_v0_3.md e docs/SCOA_Implementacao_v0_2.md: implementação e restrições incrementais. src/test/java: sete classes automatizadas identificadas na seção de rastreabilidade.

docs/testes/publicados/execucao_autor_v0_3/RELATORIO.md: comportamentos verificados, resultados e seleção de capturas. surefire-reports/RESULTADOS.md e failsafe-reports/RESULTADOS.md nessa pasta: listagens das execuções extraídas dos XMLs originais.

docs/testes/publicados/execucao_autor_v0_3/MANIFESTO.md e ambiente.txt: integridade e identificação da execução de origem. docs/testes/PADRAO_EVIDENCIAS.md: procedimento local de preservação e publicação.

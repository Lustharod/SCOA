# Resultados extraídos dos relatórios failsafe-reports

Execução do autor em 27/10/2026. Código testado: e9efc1b. Extração em 28/10/2026.

Este documento foi gerado dos XMLs originais, sem executar novamente os testes. Foram omitidas as propriedades do ambiente, caminhos locais e saída de console. Os originais permanecem preservados localmente; o manifesto registra seus hashes. Cada linha abaixo representa uma execução JUnit, inclusive as repetições parametrizadas.

| Classe | Executados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| br.edu.cefetmg.scoa.persistencia.AlocacaoJdbcIT | 17 | 0 | 0 | 0 |
| br.edu.cefetmg.scoa.persistencia.CadastroAcademicoJdbcIT | 12 | 0 | 0 | 0 |
| br.edu.cefetmg.scoa.persistencia.SalaJdbcIT | 6 | 0 | 0 | 0 |
| **Total** | **35** | **0** | **0** | **0** |

## br.edu.cefetmg.scoa.persistencia.AlocacaoJdbcIT

| Execução | Resultado |
|---|---|
| transacaoExternaContinuaAbertaEAutocommitProprioERestaurado | Aprovado |
| disponibilidadeFiltraCapacidadeSituacaoEConflitos | Aprovado |
| permiteAdjacenciaEDiasDiferentes | Aprovado |
| alocaListaEDesaloca | Aprovado |
| atualizacaoSemMudancasComAffectedRowsNaoAcusaAusencia | Aprovado |
| permitePeriodosSemSobreposicao | Aprovado |
| rejeitaMesmaTurmaEmDuasSalasNoMesmoHorario | Aprovado |
| alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean)[1] | Aprovado |
| alocacaoConcorrenteProtegeCapacidadeESituacaoDaSala(boolean)[2] | Aprovado |
| preservaCapacidadeESituacaoDeSalaComprometida | Aprovado |
| rejeitaCapacidadeInsuficienteESalaInativaSemGravar | Aprovado |
| alteracoesConcorrentesPreservamInativacaoECapacidade(boolean)[1] | Aprovado |
| alteracoesConcorrentesPreservamInativacaoECapacidade(boolean)[2] | Aprovado |
| rejeitaConflitoDaSalaEntrePeriodosComDatasSobrepostas | Aprovado |
| alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean)[1] | Aprovado |
| alocacoesConcorrentesNaoDuplicamSalaNemTurma(boolean)[2] | Aprovado |
| recusaEntidadesAusentes | Aprovado |

## br.edu.cefetmg.scoa.persistencia.CadastroAcademicoJdbcIT

| Execução | Resultado |
|---|---|
| periodoVinculadoPreservaDatasESituacaoMasPermiteDescricao | Aprovado |
| cadastraConsultaListaEAtualizaTurma | Aprovado |
| manutencaoDeTurmaPreservaAlocacoes | Aprovado |
| recusaIdsAusentes | Aprovado |
| turmaConcorrenteImpedeMudancaDasDatasDoPeriodo | Aprovado |
| capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean)[1] | Aprovado |
| capacidadeEAlocacaoConcorrentesNaoQuebramLimiteDaSala(boolean)[2] | Aprovado |
| preservaInscricoesExistentesECapacidadeMinima | Aprovado |
| inativacaoPreservaRegistroEImpedeNovasAlocacoes | Aprovado |
| rejeitaVinculosInexistentesOuInativosSemCriarTurma | Aprovado |
| atualizaEInativaPeriodoSemTurmas | Aprovado |
| alteracaoInvalidaPreservaDadosERollbackDoChamador | Aprovado |

## br.edu.cefetmg.scoa.persistencia.SalaJdbcIT

| Execução | Resultado |
|---|---|
| ctJiSala002AtualizaSemCriarOutraSala | Aprovado |
| ctJiSala004TextoComAspasEComandoSqlPermaneceDado | Aprovado |
| ctJiSala005SalaRemovidaNaoERecriadaPorAtualizacao | Aprovado |
| ctJiSala006SalvarSemAlteracoesNaoAcusaAusencia | Aprovado |
| ctJiSala001CadastraERecuperaTodosOsCampos | Aprovado |
| ctJiSala003InativaPreservandoRegistro | Aprovado |
